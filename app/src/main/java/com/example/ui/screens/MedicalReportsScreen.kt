package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportableHealthReport
import com.example.data.export.HealthReportExporter
import com.example.data.media.readAttachmentFromUri
import com.example.data.model.MedicalReport
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalReportsScreen(
    language: AppLanguage,
    profile: UserProfile?,
    reports: List<MedicalReport>,
    isScanningPhoto: Boolean,
    onBack: () -> Unit,
    onAddManualReport: (
        title: String,
        category: String,
        doctorOrLab: String,
        reportDate: String,
        summary: String,
        details: String
    ) -> Unit,
    onScanReportBitmap: (Bitmap) -> Unit,
    onUploadDocumentFile: (
        bytes: ByteArray?,
        bitmap: Bitmap?,
        mimeType: String,
        fileName: String,
        attachmentType: String
    ) -> Unit,
    onPostToast: (String) -> Unit,
    onDeleteReport: (MedicalReport) -> Unit,
    onAskAiAboutReport: (MedicalReport) -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = stringsFor(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }
    var pendingPdfExport by remember { mutableStateOf<ExportableHealthReport?>(null) }

    val saveReportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val doc = pendingPdfExport
        pendingPdfExport = null
        if (uri != null && doc != null) {
            scope.launch {
                val res = HealthReportExporter.writePdfToUri(context, uri, doc)
                onPostToast(if (res.isSuccess) strings.pdfExportSuccessToast else strings.pdfExportErrorToast)
            }
        }
    }

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
                onScanReportBitmap(bmp)
            }
        }
    }

    // SAF OpenDocument supporting Local File Manager, Google Drive, Downloads, PDF, Image & Audio
    val documentUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val loaded = readAttachmentFromUri(context, uri, preferredType = null)
                if (loaded == null) {
                    onPostToast("Could not read selected file.")
                } else if (loaded.tooLarge) {
                    onPostToast(strings.fileTooLargeError)
                } else {
                    onUploadDocumentFile(
                        loaded.bytes,
                        loaded.bitmap,
                        loaded.mimeType,
                        loaded.fileName,
                        loaded.attachmentType
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("medical_reports_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("reports_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.medicalReportsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${strings.medicalReportsSub} • ${reports.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TealDeep
                    )
                }
            }
        }

        item { SafetyDisclaimerBanner(language = language) }

        // Action Buttons: Add Manually + AI Photo Scan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAddSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_medical_report_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addReportBtn, style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isScanningPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("scan_medical_report_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null, tint = TealDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.scanReportPhotoBtn, style = MaterialTheme.typography.labelMedium, color = TealDeep)
                }
            }
        }

        // Upload from Local File Manager / Google Drive / Audio + Export All PDF
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        documentUploadLauncher.launch(
                            arrayOf("application/pdf", "image/*", "audio/*", "text/*", "*/*")
                        )
                    },
                    enabled = !isScanningPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("upload_drive_file_report_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, IndigoCalm.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = IndigoCalm, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.uploadFromDriveFilesBtn, style = MaterialTheme.typography.labelSmall, color = IndigoCalm, maxLines = 1)
                }

                if (reports.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val vaultDoc = HealthReportExporter.buildAllMedicalReportsVault(reports, profile, language)
                            pendingPdfExport = vaultDoc
                            saveReportPdfLauncher.launch(vaultDoc.suggestedFileName)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_all_reports_pdf_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = TealDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportAllReportsPdfBtn, style = MaterialTheme.typography.labelSmall, color = TealDeep, maxLines = 1)
                    }
                }
            }
        }

        if (isScanningPhoto) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = TealContainer.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = TealDeep
                        )
                        Text(
                            text = strings.scanningReportMsg,
                            style = MaterialTheme.typography.titleSmall,
                            color = TealDeep
                        )
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = TealDeep,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.noReportsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(reports, key = { it.id }) { report ->
                SerialReportCard(
                    report = report,
                    language = language,
                    onAskAi = { onAskAiAboutReport(report) },
                    onExportPdf = {
                        val singleDoc = HealthReportExporter.buildSingleMedicalReport(report, profile, language)
                        pendingPdfExport = singleDoc
                        saveReportPdfLauncher.launch(singleDoc.suggestedFileName)
                    },
                    onSharePdf = {
                        scope.launch {
                            val singleDoc = HealthReportExporter.buildSingleMedicalReport(report, profile, language)
                            val fileRes = HealthReportExporter.generateShareablePdfFile(context, singleDoc)
                            fileRes.onSuccess { file ->
                                HealthReportExporter.launchSharePdfIntent(context, file, singleDoc.title)
                            }.onFailure {
                                onPostToast(strings.pdfExportErrorToast)
                            }
                        }
                    },
                    onDelete = { onDeleteReport(report) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState
        ) {
            AddMedicalReportForm(
                language = language,
                onSave = { title, category, doctorOrLab, reportDate, summary, details ->
                    onAddManualReport(title, category, doctorOrLab, reportDate, summary, details)
                    showAddSheet = false
                }
            )
        }
    }
}

@Composable
private fun SerialReportCard(
    report: MedicalReport,
    language: AppLanguage,
    onAskAi: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = stringsFor(language)
    var expanded by remember(report.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("medical_report_card_${report.serialNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TealDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${report.serialNumber}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IndigoContainer
                        ) {
                            Text(
                                text = reportCategoryLabel(report.category, language),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = IndigoCalm,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = report.reportDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (report.doctorOrLab.isNotBlank()) {
                        Text(
                            text = report.doctorOrLab,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expand report details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            if (report.attachmentName.isNotBlank() || report.attachmentType != "TEXT") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TealContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = null,
                            tint = TealDeep,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${report.attachmentType}${if (report.attachmentName.isNotBlank()) " • ${report.attachmentName}" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TealDeep
                        )
                    }
                }
            }

            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (expanded && report.details.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = report.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAskAi,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("ask_ai_report_${report.serialNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = TealDeep,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = strings.askAiAboutReportBtn,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TealDeep
                        )
                    }

                    OutlinedButton(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, IndigoCalm.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("export_pdf_report_${report.serialNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = strings.exportSingleReportBtn,
                            tint = IndigoCalm,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.exportSingleReportBtn,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndigoCalm
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSharePdf,
                        modifier = Modifier.testTag("share_pdf_report_${report.serialNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = strings.sharePdfBtn,
                            tint = TealDeep
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete medical report",
                            tint = CoralHeart
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMedicalReportForm(
    language: AppLanguage,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    val strings = stringsFor(language)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("LAB_TEST") }
    var doctorOrLab by remember { mutableStateOf("") }
    var reportDate by remember { mutableStateOf(today) }
    var summary by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    val categories = listOf("LAB_TEST", "PRESCRIPTION", "IMAGING", "DOCTOR_NOTE", "OTHER")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.addReportBtn,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(strings.reportTitleLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_title_input")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(strings.reportCategoryLabel, style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(reportCategoryLabel(cat, language), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealDeep,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = doctorOrLab,
                onValueChange = { doctorOrLab = it },
                label = { Text(strings.reportDoctorLabLabel) },
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = reportDate,
                onValueChange = { reportDate = it },
                label = { Text(strings.reportDateLabel) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text(strings.reportSummaryLabel) },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("report_summary_input")
        )

        OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text(strings.reportDetailsLabel) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (title.isNotBlank() && summary.isNotBlank()) {
                    onSave(title, category, doctorOrLab, reportDate, summary, details)
                }
            },
            enabled = title.isNotBlank() && summary.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_medical_report_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
        ) {
            Text(strings.saveReportBtn, style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun reportCategoryLabel(category: String, language: AppLanguage): String {
    val isBn = language == AppLanguage.BN
    return when (category) {
        "LAB_TEST" -> if (isBn) "ল্যাব টেস্ট" else "Lab Test"
        "PRESCRIPTION" -> if (isBn) "প্রেসক্রিপশন" else "Prescription"
        "IMAGING" -> if (isBn) "ইমেজিং/এক্স-রে" else "Imaging"
        "DOCTOR_NOTE" -> if (isBn) "ডাক্তার নোট" else "Doctor Note"
        else -> if (isBn) "অন্যান্য" else "Other"
    }
}
