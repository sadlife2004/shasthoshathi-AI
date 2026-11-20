package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExtractedMedication
import com.example.data.model.PrescriptionAnalysisResult
import com.example.data.model.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.ErrorRetryBanner
import com.example.ui.components.PrescriptionCameraScanner
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep

@Composable
fun PrescriptionReaderScreen(
    language: AppLanguage,
    previewBitmap: Bitmap?,
    rxState: UiState<PrescriptionAnalysisResult>,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onUpdateMed: (ExtractedMedication) -> Unit,
    onRemoveMed: (String) -> Unit,
    onAutoScheduleAll: () -> Unit,
    onSaveToReports: () -> Unit = {}
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    var editingMed by remember { mutableStateOf<ExtractedMedication?>(null) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            if (bmp != null) {
                onAnalyzeBitmap(bmp)
            }
        }
    }

    var showCameraScanner by remember { mutableStateOf(false) }
    var showPermissionDeniedCard by remember { mutableStateOf(false) }
    var isPermissionPermanentlyDenied by remember { mutableStateOf(false) }

    // Runtime CAMERA permission request for CameraX prescription scanner
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPermissionDeniedCard = false
            isPermissionPermanentlyDenied = false
            showCameraScanner = true
        } else {
            val activity = context.findActivity()
            isPermissionPermanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            showPermissionDeniedCard = true
        }
    }

    fun openCameraWithPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            showPermissionDeniedCard = false
            isPermissionPermanentlyDenied = false
            showCameraScanner = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prescription_reader_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
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
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = null,
                        tint = TealDeep,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.rxHeader,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item { SafetyDisclaimerBanner(language = language) }

        // Scanner / Upload Dropzone Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, TealDeep.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (previewBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Selected prescription photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (rxState is UiState.Loading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.58f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF0F172A).copy(alpha = 0.9f),
                                        border = BorderStroke(1.dp, TealContainer.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color(0xFF5EEAD4),
                                                strokeWidth = 2.5.dp
                                            )
                                            Text(
                                                text = strings.analyzingRx,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(TealContainer.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DocumentScanner,
                                    contentDescription = null,
                                    tint = TealDeep,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = strings.rxDropzoneTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = strings.rxDropzoneSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = rxState !is UiState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("rx_choose_photo_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.choosePhotoBtn, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { openCameraWithPermission() },
                            enabled = rxState !is UiState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("rx_take_photo_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.takePhotoBtn, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val sampleBitmap = BitmapFactory.decodeResource(
                                context.resources,
                                R.drawable.img_sample_prescription_1795106967495
                            )
                            if (sampleBitmap != null) {
                                onAnalyzeBitmap(sampleBitmap)
                            }
                        },
                        enabled = rxState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rx_try_sample_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealContainer,
                            contentColor = TealDeep
                        )
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.trySampleRxBtn,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Camera Permission Denied Rationale Card
        if (showPermissionDeniedCard) {
            item {
                CameraPermissionRationaleCard(
                    language = language,
                    isPermanentlyDenied = isPermissionPermanentlyDenied,
                    onGrantPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    onChooseFromGallery = {
                        showPermissionDeniedCard = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }

        // Error State
        if (rxState is UiState.Error) {
            item {
                ErrorRetryBanner(
                    message = rxState.message,
                    onRetry = { previewBitmap?.let(onAnalyzeBitmap) },
                    language = language
                )
            }
        }

        // Extracted Medications Results
        if (rxState is UiState.Success) {
            val result = rxState.data
            val hasLowConfidence = result.overallClarity != "High" ||
                result.warningMessage.isNotBlank() ||
                result.medicines.any { it.confidence != "High" }

            if (hasLowConfidence) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rx_low_confidence_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = AmberContainer,
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Verification Warning",
                                tint = AmberOnContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = strings.lowConfidenceWarning,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberOnContainer
                                )
                                if (result.warningMessage.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = result.warningMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AmberOnContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${strings.activeMedicationsTitle} (${result.medicines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(result.medicines, key = { it.localId }) { med ->
                ExtractedMedicineCard(
                    med = med,
                    language = language,
                    onEdit = { editingMed = med },
                    onRemove = { onRemoveMed(med.localId) }
                )
            }

            if (result.medicines.isNotEmpty()) {
                item {
                    Button(
                        onClick = onAutoScheduleAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("rx_auto_schedule_all_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Icon(Icons.Filled.AlarmAdd, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.autoScheduleAllBtn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSaveToReports,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rx_save_to_reports_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = TealDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.saveRxToReportsBtn,
                            style = MaterialTheme.typography.labelLarge,
                            color = TealDeep
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    editingMed?.let { med ->
        EditExtractedMedicineDialog(
            med = med,
            language = language,
            onDismiss = { editingMed = null },
            onSave = { updated ->
                onUpdateMed(updated)
                editingMed = null
            }
        )
    }

    if (showCameraScanner) {
        PrescriptionCameraScanner(
            language = language,
            onBitmapCaptured = { capturedBitmap ->
                showCameraScanner = false
                onAnalyzeBitmap(capturedBitmap)
            },
            onOpenGallery = {
                showCameraScanner = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showCameraScanner = false }
        )
    }
}

@Composable
private fun CameraPermissionRationaleCard(
    language: AppLanguage,
    isPermanentlyDenied: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    val strings = stringsFor(language)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("camera_permission_rationale_card"),
        shape = RoundedCornerShape(18.dp),
        color = AmberContainer,
        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = strings.cameraPermissionTitle,
                    tint = AmberOnContainer,
                    modifier = Modifier.size(22.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.cameraPermissionTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberOnContainer
                    )
                    Text(
                        text = strings.cameraPermissionRationale,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberOnContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPermanentlyDenied) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("camera_open_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.openAppSettings, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("camera_grant_permission_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.grantCameraPermission, style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedButton(
                    onClick = onChooseFromGallery,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("camera_rationale_gallery_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AmberOnContainer.copy(alpha = 0.4f))
                ) {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = AmberOnContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        strings.choosePhotoBtn,
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberOnContainer
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ExtractedMedicineCard(
    med: ExtractedMedication,
    language: AppLanguage,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val confBg = when (med.confidence) {
        "High" -> EmeraldContainer
        "Medium" -> AmberContainer
        else -> CoralContainer
    }
    val confFg = when (med.confidence) {
        "High" -> EmeraldSuccess
        "Medium" -> AmberOnContainer
        else -> CoralHeart
    }

    val scheduleCode = "${if (med.morning) 1 else 0} + ${if (med.noon) 1 else 0} + ${if (med.night) 1 else 0}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Medication, contentDescription = null, tint = TealDeep)
                    }
                    Column {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${med.dosage} • ${med.durationDays} ${if (language == AppLanguage.BN) "দিন" else "days"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_extracted_med_btn")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit medicine", tint = TealDeep)
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = CoralHeart)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = TealContainer) {
                    Text(
                        text = scheduleCode,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TealDeep,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = med.mealTiming,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = confBg) {
                    Text(
                        text = med.confidence,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = confFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (med.specialNotes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = med.specialNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditExtractedMedicineDialog(
    med: ExtractedMedication,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (ExtractedMedication) -> Unit
) {
    val strings = stringsFor(language)
    var name by remember { mutableStateOf(med.name) }
    var dosage by remember { mutableStateOf(med.dosage) }
    var morning by remember { mutableStateOf(med.morning) }
    var noon by remember { mutableStateOf(med.noon) }
    var night by remember { mutableStateOf(med.night) }
    var mealTiming by remember { mutableStateOf(med.mealTiming) }
    var durationStr by remember { mutableStateOf(med.durationDays.toString()) }
    var notes by remember { mutableStateOf(med.specialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editMedicineTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.medicineNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text(strings.dosageLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = morning,
                        onClick = { morning = !morning },
                        label = { Text(strings.filterMorning) }
                    )
                    FilterChip(
                        selected = noon,
                        onClick = { noon = !noon },
                        label = { Text(strings.filterNoon) }
                    )
                    FilterChip(
                        selected = night,
                        onClick = { night = !night },
                        label = { Text(strings.filterNight) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Before Meal", "After Meal", "Anytime").forEach { option ->
                        FilterChip(
                            selected = mealTiming == option,
                            onClick = { mealTiming = option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.durationDaysLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        med.copy(
                            name = name.trim().ifBlank { med.name },
                            dosage = dosage.trim().ifBlank { med.dosage },
                            morning = morning,
                            noon = noon,
                            night = night,
                            mealTiming = mealTiming,
                            durationDays = durationStr.toIntOrNull()?.coerceIn(1, 365) ?: med.durationDays,
                            specialNotes = notes.trim(),
                            confidence = "High"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
