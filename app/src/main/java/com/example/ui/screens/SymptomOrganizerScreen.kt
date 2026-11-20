package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.media.readAttachmentFromUri
import com.example.data.model.DoctorBrief
import com.example.data.model.SymptomChatMessage
import com.example.data.model.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.ErrorRetryBanner
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val QuickSymptoms = listOf(
    "Fever" to "জ্বর",
    "Cough" to "কাশি",
    "Headache" to "মাথাব্যথা",
    "Gastric" to "গ্যাস্ট্রিক",
    "Fatigue" to "ক্লান্তি"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomOrganizerScreen(
    language: AppLanguage,
    messages: List<SymptomChatMessage>,
    isChatLoading: Boolean,
    isAttachmentLoading: Boolean,
    chatError: String?,
    briefState: UiState<DoctorBrief>,
    savedBriefs: List<DoctorBrief>,
    onSendMessage: (String) -> Unit,
    onUploadAttachment: (
        bytes: ByteArray?,
        bitmap: Bitmap?,
        mimeType: String,
        fileName: String,
        attachmentType: String,
        saveToReportsVault: Boolean
    ) -> Unit,
    onPostToast: (String) -> Unit,
    onGenerateBrief: () -> Unit,
    onStartNewCheck: () -> Unit,
    onCopyToast: () -> Unit
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var saveUploadsToVault by remember { mutableStateOf(true) }

    // Voice Recording State
    var isRecordingVoice by remember { mutableStateOf(false) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                activeRecorder?.stop()
                activeRecorder?.release()
            }
            activeRecorder = null
        }
    }

    fun startVoiceRecording() {
        try {
            val outFile = File(context.cacheDir, "voice_symptom_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recordedAudioFile = outFile
            activeRecorder = recorder
            isRecordingVoice = true
            showAttachSheet = false
        } catch (_: Exception) {
            isRecordingVoice = false
            onPostToast(strings.micPermissionDeniedError)
        }
    }

    fun stopAndSendVoiceRecording() {
        val file = recordedAudioFile
        runCatching {
            activeRecorder?.stop()
            activeRecorder?.release()
        }
        activeRecorder = null
        isRecordingVoice = false
        if (file != null && file.exists() && file.length() > 0L) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                onUploadAttachment(
                    bytes,
                    null,
                    "audio/mp4",
                    file.name,
                    "AUDIO",
                    saveUploadsToVault
                )
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecording()
        } else {
            onPostToast(strings.micPermissionDeniedError)
        }
    }

    fun handlePickedUri(uri: android.net.Uri?, preferredType: String) {
        if (uri == null) return
        showAttachSheet = false
        scope.launch {
            val loaded = readAttachmentFromUri(context, uri, preferredType)
            if (loaded == null) {
                onPostToast("Could not read selected file.")
            } else if (loaded.tooLarge) {
                onPostToast(strings.fileTooLargeError)
            } else {
                onUploadAttachment(
                    loaded.bytes,
                    loaded.bitmap,
                    loaded.mimeType,
                    loaded.fileName,
                    loaded.attachmentType,
                    saveUploadsToVault
                )
            }
        }
    }

    // Zero-permission SAF OpenDocument pickers supporting Local File Manager, Google Drive, OneDrive, Downloads, Gallery
    val imageDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedUri(uri, "IMAGE") }

    val fileDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedUri(uri, "FILE") }

    val audioDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedUri(uri, "AUDIO") }

    val listState = rememberLazyListState()
    val userTurnCount = messages.count { it.fromUser }
    val busy = isChatLoading || isAttachmentLoading

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("symptom_organizer_screen")
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = TealDeep,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.symptomHeader,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (savedBriefs.isNotEmpty()) {
                    IconButton(
                        onClick = { showHistory = !showHistory },
                        modifier = Modifier.testTag("symptom_history_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = strings.pastBriefsTitle,
                            tint = if (showHistory) TealDeep else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onStartNewCheck,
                    modifier = Modifier.testTag("symptom_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = strings.startNewCheck,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SafetyDisclaimerBanner(
            language = language,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(Modifier.height(10.dp))

        if (showHistory) {
            SavedBriefsHistoryList(
                briefs = savedBriefs,
                language = language,
                onCopy = { brief ->
                    copyBriefToClipboard(context, brief)
                    onCopyToast()
                },
                onShare = { brief -> shareBrief(context, brief) }
            )
        } else if (briefState is UiState.Success) {
            DoctorBriefView(
                brief = briefState.data,
                language = language,
                onCopy = {
                    copyBriefToClipboard(context, briefState.data)
                    onCopyToast()
                },
                onShare = { shareBrief(context, briefState.data) },
                onStartNew = onStartNewCheck
            )
        } else {
            // Guided Chat Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                if (isChatLoading || isAttachmentLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TealDeep
                            )
                            Text(
                                text = if (isAttachmentLoading) {
                                    strings.analyzingAttachmentMsg
                                } else if (language == AppLanguage.BN) {
                                    "স্বাস্থ্যসাথী লিখছে..."
                                } else {
                                    "ShasthoSathi is typing..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!chatError.isNullOrBlank()) {
                    item {
                        ErrorRetryBanner(
                            message = chatError,
                            onRetry = {
                                val lastUser = messages.lastOrNull { it.fromUser }?.text
                                if (!lastUser.isNullOrBlank()) onSendMessage(lastUser)
                            },
                            language = language
                        )
                    }
                }

                if (briefState is UiState.Loading) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = TealContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = TealDeep,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = strings.generatingBrief,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TealDeep
                                )
                            }
                        }
                    }
                } else if (briefState is UiState.Error) {
                    item {
                        ErrorRetryBanner(
                            message = briefState.message,
                            onRetry = onGenerateBrief,
                            language = language
                        )
                    }
                }
            }

            // Quick-Tap Symptom Chips + Generate Brief CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (userTurnCount >= 1) {
                    Button(
                        onClick = onGenerateBrief,
                        enabled = briefState !is UiState.Loading && !isChatLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("generate_doctor_brief_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userTurnCount >= 3) TealDeep else AmberAccent
                        )
                    ) {
                        Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.generateBriefBtn, style = MaterialTheme.typography.labelLarge)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuickSymptoms) { (en, bn) ->
                        val chipLabel = if (language == AppLanguage.BN) "$bn ($en)" else "$en / $bn"
                        FilterChip(
                            selected = false,
                            onClick = {
                                val msg = if (language == AppLanguage.BN) "আমার $bn হচ্ছে" else "I have $en"
                                onSendMessage(msg)
                            },
                            label = { Text(chipLabel, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                if (isRecordingVoice) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CoralContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null, tint = CoralHeart)
                                Text(
                                    text = strings.recordingVoiceNow,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = CoralHeart
                                )
                            }
                            Button(
                                onClick = { stopAndSendVoiceRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralHeart),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("symptom_stop_voice_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.stopAndSendVoice, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !busy && !isRecordingVoice) {
                                showAttachSheet = true
                            }
                            .testTag("symptom_attach_button"),
                        color = TealContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = strings.attachMediaTitle,
                                tint = TealDeep,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(strings.symptomInputHint, style = MaterialTheme.typography.bodyMedium)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("symptom_chat_input")
                    )
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable(enabled = inputText.isNotBlank() && !busy) {
                                val toSend = inputText
                                inputText = ""
                                onSendMessage(toSend)
                            }
                            .testTag("symptom_send_button"),
                        color = if (inputText.isNotBlank() && !busy) TealDeep else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send symptom message",
                                tint = if (inputText.isNotBlank() && !busy) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAttachSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("symptom_attach_sheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.attachMediaTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.attachMediaSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TealContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.alsoSaveToReportsToggle,
                            style = MaterialTheme.typography.labelLarge,
                            color = TealDeep,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = saveUploadsToVault,
                            onCheckedChange = { saveUploadsToVault = it },
                            modifier = Modifier.testTag("symptom_save_to_vault_switch")
                        )
                    }
                }

                AttachmentOptionRow(
                    title = strings.attachImageOption,
                    subtitle = strings.attachImageOptionSub,
                    icon = Icons.Filled.Image,
                    iconBg = TealContainer,
                    iconTint = TealDeep,
                    testTag = "attach_image_option",
                    onClick = { imageDocLauncher.launch(arrayOf("image/*")) }
                )

                AttachmentOptionRow(
                    title = strings.attachFileOption,
                    subtitle = strings.attachFileOptionSub,
                    icon = Icons.Filled.Description,
                    iconBg = IndigoContainer,
                    iconTint = IndigoCalm,
                    testTag = "attach_file_option",
                    onClick = {
                        fileDocLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "text/*",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "*/*"
                            )
                        )
                    }
                )

                AttachmentOptionRow(
                    title = strings.attachAudioOption,
                    subtitle = strings.attachAudioOptionSub,
                    icon = Icons.Filled.AudioFile,
                    iconBg = AmberContainer,
                    iconTint = AmberAccent,
                    testTag = "attach_audio_option",
                    onClick = { audioDocLauncher.launch(arrayOf("audio/*")) }
                )

                AttachmentOptionRow(
                    title = strings.recordVoiceOption,
                    subtitle = if (language == AppLanguage.BN) "সরাসরি মাইক্রোফোনে আপনার উপসর্গ বলুন" else "Speak directly into your microphone",
                    icon = Icons.Filled.Mic,
                    iconBg = CoralContainer,
                    iconTint = CoralHeart,
                    testTag = "attach_record_voice_option",
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            startVoiceRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun AttachmentOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: SymptomChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.fromUser) 18.dp else 4.dp,
                bottomEnd = if (message.fromUser) 4.dp else 18.dp
            ),
            color = if (message.fromUser) TealDeep else MaterialTheme.colorScheme.surface,
            border = if (message.fromUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.attachmentBadge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (message.fromUser) Color.White.copy(alpha = 0.2f) else TealContainer,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = null,
                                tint = if (message.fromUser) Color.White else TealDeep,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = message.attachmentBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (message.fromUser) Color.White else TealDeep
                            )
                        }
                    }
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.fromUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.fromUser) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DoctorBriefView(
    brief: DoctorBrief,
    language: AppLanguage,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onStartNew: () -> Unit
) {
    val strings = stringsFor(language)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("doctor_brief_card"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, TealDeep.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Assignment, contentDescription = null, tint = TealDeep)
                        Text(
                            text = strings.doctorBriefTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = TealDeep
                        )
                    }

                    BriefSection(label = strings.chiefSymptomsLabel, body = brief.chiefSymptoms)
                    BriefSection(label = strings.severityContextLabel, body = brief.severityAndContext)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.questionsForDoctorLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        brief.questionsForDoctor.forEach { q ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = TealDeep, fontWeight = FontWeight.Bold)
                                Text(
                                    text = q,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (brief.redFlagWarning.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (brief.urgentCareAdvised) CoralContainer else AmberContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = if (brief.urgentCareAdvised) CoralHeart else AmberOnContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.redFlagLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (brief.urgentCareAdvised) CoralHeart else AmberOnContainer
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = brief.redFlagWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (brief.urgentCareAdvised) Color(0xFF9F1239) else AmberOnContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_brief_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.copyBrief, style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_brief_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.shareBrief, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onStartNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_new_check_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.startNewCheck, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun BriefSection(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SavedBriefsHistoryList(
    briefs: List<DoctorBrief>,
    language: AppLanguage,
    onCopy: (DoctorBrief) -> Unit,
    onShare: (DoctorBrief) -> Unit
) {
    val strings = stringsFor(language)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.pastBriefsTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        items(briefs, key = { it.id.ifBlank { it.chiefSymptoms.hashCode().toString() } }) { brief ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = brief.chiefSymptoms,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = brief.severityAndContext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onCopy(brief) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.copyBrief, style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onShare(brief) }) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.shareBrief, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBriefPlainText(brief: DoctorBrief): String = buildString {
    appendLine("ShasthoSathi AI — Pre-Consultation Doctor Brief")
    appendLine("Disclaimer: Guidance only — Not a medical diagnosis.")
    appendLine()
    appendLine("Chief Symptoms & Duration:")
    appendLine(brief.chiefSymptoms)
    appendLine()
    appendLine("Severity & Key Context:")
    appendLine(brief.severityAndContext)
    appendLine()
    appendLine("Questions to Ask Doctor:")
    brief.questionsForDoctor.forEach { appendLine("- $it") }
    if (brief.redFlagWarning.isNotBlank()) {
        appendLine()
        appendLine("Red-Flag Warning:")
        appendLine(brief.redFlagWarning)
    }
}

private fun copyBriefToClipboard(context: Context, brief: DoctorBrief) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Doctor Brief", formatBriefPlainText(brief)))
}

private fun shareBrief(context: Context, brief: DoctorBrief) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Pre-Consultation Doctor Brief — ShasthoSathi AI")
        putExtra(Intent.EXTRA_TEXT, formatBriefPlainText(brief))
    }
    context.startActivity(Intent.createChooser(intent, "Share Doctor Brief"))
}
