package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.export.HealthReportExporter
import com.example.data.model.AdherenceLog
import com.example.data.model.DoctorAppointment
import com.example.data.model.DoctorBrief
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.MedicalReport
import com.example.data.model.MedicineReminder
import com.example.data.model.MoodEntry
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DataDashboardScreen(
    language: AppLanguage,
    profile: UserProfile?,
    reminders: List<MedicineReminder>,
    adherenceLogs: List<AdherenceLog>,
    healthMetrics: List<HealthMetricsRecord>,
    moodEntries: List<MoodEntry>,
    medicalReports: List<MedicalReport>,
    doctorBriefs: List<DoctorBrief>,
    appointments: List<DoctorAppointment>,
    onBack: () -> Unit,
    onPostToast: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = stringsFor(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    val exportableReport = remember(
        profile, reminders, adherenceLogs, healthMetrics, moodEntries, medicalReports, doctorBriefs, appointments, language
    ) {
        HealthReportExporter.buildFullHealthDashboardReport(
            profile = profile,
            reminders = reminders,
            adherenceLogs = adherenceLogs,
            healthMetrics = healthMetrics,
            moodEntries = moodEntries,
            medicalReports = medicalReports,
            doctorBriefs = doctorBriefs,
            appointments = appointments,
            language = language
        )
    }

    // Zero-permission SAF CreateDocument launcher to save PDF to Local File Manager, Downloads, or Google Drive
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val res = HealthReportExporter.writePdfToUri(context, uri, exportableReport)
                isExporting = false
                if (res.isSuccess) {
                    onPostToast(strings.pdfExportSuccessToast)
                } else {
                    onPostToast(strings.pdfExportErrorToast)
                }
            }
        }
    }

    // Analytics Computations
    val activeReminders = reminders.filter { it.active }
    val takenToday = activeReminders.count { it.todayStatus == "TAKEN" }
    val todayAdherencePct = if (activeReminders.isNotEmpty()) (takenToday * 100) / activeReminders.size else 0

    val last7Days = remember {
        val fmtKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fmtShort = SimpleDateFormat("EEE", Locale.US)
        (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            fmtKey.format(cal.time) to fmtShort.format(cal.time)
        }
    }

    val adherenceByDay = last7Days.map { (dateKey, dayLabel) ->
        val takenForDay = adherenceLogs.count { it.dateKey == dateKey && it.action == "TAKEN" }
        dayLabel to takenForDay.toFloat()
    }
    val takenTotalLogs = adherenceLogs.count { it.action == "TAKEN" }
    val snoozedTotalLogs = adherenceLogs.count { it.action == "SNOOZED" }
    val skippedTotalLogs = adherenceLogs.count { it.action == "SKIPPED" }

    val metricsByDate = healthMetrics.associateBy { it.dateKey }
    val waterGoalMl = (healthMetrics.firstOrNull()?.waterGoalMl ?: 2500L).coerceAtLeast(500L)
    val waterByDay = last7Days.map { (dateKey, dayLabel) ->
        val ml = metricsByDate[dateKey]?.waterIntakeMl ?: 0L
        dayLabel to ml.toFloat()
    }
    val avgWaterMl = if (healthMetrics.isNotEmpty()) {
        healthMetrics.take(7).map { it.waterIntakeMl }.average().toLong()
    } else 0L

    val latestBmiRec = healthMetrics.firstOrNull { it.bmiValue > 0.0 }
    val recentMoodPoints = moodEntries.take(7).reversed()
    val avgMoodScore = if (moodEntries.isNotEmpty()) {
        moodEntries.take(7).map { it.moodScore }.average()
    } else 0.0
    val latestWellnessScore = healthMetrics.firstOrNull { it.lifeConditionScore > 0L }?.lifeConditionScore ?: 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("data_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("data_dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(
                            text = strings.dataDashboardTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.dataDashboardSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = TealDeep
                        )
                    }
                }

                Surface(
                    onClick = { savePdfLauncher.launch(exportableReport.suggestedFileName) },
                    shape = RoundedCornerShape(12.dp),
                    color = TealContainer,
                    modifier = Modifier.testTag("data_dashboard_quick_export_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = strings.savePdfToDeviceBtn, tint = TealDeep, modifier = Modifier.size(18.dp))
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TealDeep
                        )
                    }
                }
            }
        }

        item { SafetyDisclaimerBanner(language = language) }

        // 1. 2x2 KPI Summary Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.adherenceRateLabel,
                        value = "$todayAdherencePct%",
                        subtitle = "$takenToday / ${activeReminders.size} today",
                        icon = Icons.Filled.Medication,
                        bg = EmeraldContainer,
                        tint = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.avgHydrationLabel,
                        value = "${avgWaterMl}ml",
                        subtitle = "Goal ${waterGoalMl}ml",
                        icon = Icons.Filled.WaterDrop,
                        bg = IndigoContainer,
                        tint = IndigoCalm,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = strings.latestBmiLabel,
                        value = if (latestBmiRec != null) String.format(Locale.US, "%.1f", latestBmiRec.bmiValue) else "--",
                        subtitle = latestBmiRec?.bmiCategory?.ifBlank { "Not set" } ?: "Not set",
                        icon = Icons.Filled.MonitorWeight,
                        bg = TealContainer,
                        tint = TealDeep,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.avgMoodScoreLabel,
                        value = if (avgMoodScore > 0.0) String.format(Locale.US, "%.1f/5", avgMoodScore) else "--",
                        subtitle = if (latestWellnessScore > 0L) "Life: $latestWellnessScore/100" else "${moodEntries.size} logs",
                        icon = Icons.Filled.SelfImprovement,
                        bg = AmberContainer,
                        tint = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. 7-Day Medication Adherence Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, tint = TealDeep)
                        Text(
                            text = strings.adherenceChartTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    SevenDayBarChart(
                        entries = adherenceByDay,
                        barColor = TealDeep,
                        trackColor = TealContainer,
                        maxReference = (activeReminders.size.coerceAtLeast(3)).toFloat()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniStatusPill("${strings.statusTaken}: $takenTotalLogs", EmeraldContainer, EmeraldSuccess, Modifier.weight(1f))
                        MiniStatusPill("${strings.statusSnoozed}: $snoozedTotalLogs", AmberContainer, AmberAccent, Modifier.weight(1f))
                        MiniStatusPill("${strings.statusSkipped}: $skippedTotalLogs", CoralContainer, CoralHeart, Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. 7-Day Hydration Trend Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = IndigoCalm)
                            Text(
                                text = strings.hydrationChartTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${waterGoalMl}ml",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndigoCalm
                        )
                    }

                    SevenDayBarChart(
                        entries = waterByDay,
                        barColor = IndigoCalm,
                        trackColor = IndigoContainer,
                        maxReference = waterGoalMl.toFloat(),
                        showGoalLine = true
                    )
                }
            }
        }

        // 4. Mood Score Trend Line Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = AmberAccent)
                        Text(
                            text = strings.moodTrendChartTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (recentMoodPoints.isEmpty()) {
                        Text(
                            text = strings.noMoodYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        MoodLineTrendChart(entries = recentMoodPoints)
                    }
                }
            }
        }

        // 5. Vault & Care Summary Counts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.vaultOverviewTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VaultCountChip(
                            label = if (language == AppLanguage.BN) "রিপোর্ট" else "Reports",
                            count = medicalReports.size,
                            icon = Icons.Filled.Description,
                            tint = TealDeep,
                            bg = TealContainer,
                            modifier = Modifier.weight(1f)
                        )
                        VaultCountChip(
                            label = if (language == AppLanguage.BN) "ডাক্তার ব্রিফ" else "Briefs",
                            count = doctorBriefs.size,
                            icon = Icons.Filled.BarChart,
                            tint = IndigoCalm,
                            bg = IndigoContainer,
                            modifier = Modifier.weight(1f)
                        )
                        VaultCountChip(
                            label = if (language == AppLanguage.BN) "ভিজিট" else "Visits",
                            count = appointments.count { it.bookingStatus != "CANCELLED" },
                            icon = Icons.Filled.LocalHospital,
                            tint = CoralHeart,
                            bg = CoralContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 6. Enhanced Health Report Export Card (Save PDF / Share PDF / Share Summary Text)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enhanced_export_report_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = TealDeep)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.exportReportTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = strings.exportReportSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.86f)
                            )
                        }
                    }

                    Button(
                        onClick = { savePdfLauncher.launch(exportableReport.suggestedFileName) },
                        enabled = !isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("export_save_pdf_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = TealDeep
                        )
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.savePdfToDeviceBtn,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    val fileRes = HealthReportExporter.generateShareablePdfFile(context, exportableReport)
                                    isExporting = false
                                    fileRes.onSuccess { file ->
                                        HealthReportExporter.launchSharePdfIntent(context, file, exportableReport.title)
                                    }.onFailure {
                                        onPostToast(strings.pdfExportErrorToast)
                                    }
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_share_pdf_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.sharePdfBtn, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                HealthReportExporter.launchShareTextIntent(context, exportableReport)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_share_text_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.shareTextSummaryBtn, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    bg: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SevenDayBarChart(
    entries: List<Pair<String, Float>>,
    barColor: Color,
    trackColor: Color,
    maxReference: Float,
    showGoalLine: Boolean = false
) {
    val maxVal = maxOf(maxReference, entries.maxOfOrNull { it.second } ?: 1f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = entries.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.46f).coerceIn(14f, 44f)
            val chartBottom = size.height - 6f
            val chartTop = 10f
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

            if (showGoalLine) {
                val goalY = chartBottom - ((maxReference / maxVal).coerceIn(0f, 1f) * chartHeight)
                drawLine(
                    color = barColor.copy(alpha = 0.4f),
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            entries.forEachIndexed { idx, (_, value) ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val left = centerX - barWidth / 2f
                // Track bar
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.55f),
                    topLeft = Offset(left, chartTop),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Active value bar
                val frac = (value / maxVal).coerceIn(0f, 1f)
                val activeHeight = (chartHeight * frac).coerceAtLeast(if (value > 0f) 10f else 0f)
                if (activeHeight > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, chartBottom - activeHeight),
                        size = Size(barWidth, activeHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            entries.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MoodLineTrendChart(entries: List<MoodEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val count = entries.size
            val topPad = 14f
            val bottomPad = 14f
            val usableH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val points = entries.mapIndexed { idx, item ->
                val x = if (count == 1) size.width / 2f else (size.width / (count - 1)) * idx
                val norm = ((item.moodScore.toFloat() - 1f) / 4f).coerceIn(0f, 1f)
                val y = topPad + usableH * (1f - norm)
                Offset(x, y)
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = AmberAccent,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            points.forEach { pt ->
                drawCircle(color = Color.White, radius = 11f, center = pt)
                drawCircle(color = TealDeep, radius = 7f, center = pt)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.dateLabel.take(6),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniStatusPill(label: String, bg: Color, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg.copy(alpha = 0.7f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VaultCountChip(
    label: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
