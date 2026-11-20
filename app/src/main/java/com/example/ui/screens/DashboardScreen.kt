package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.MedicineReminder
import com.example.data.model.MoodEntry
import com.example.data.model.UserProfile
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.SafetyDisclaimerBanner
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoCalm
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealPrimary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    profile: UserProfile?,
    fallbackUserName: String,
    reminders: List<MedicineReminder>,
    moodEntries: List<MoodEntry>,
    todayMetrics: HealthMetricsRecord?,
    latestBmiRecord: HealthMetricsRecord?,
    reportCount: Int,
    appointmentCount: Int,
    onMarkTaken: (MedicineReminder) -> Unit,
    onSnooze: (MedicineReminder) -> Unit,
    onTestAlarm: () -> Unit,
    onOpenAiBot: (String?) -> Unit,
    onOpenSymptoms: () -> Unit,
    onOpenScanRx: () -> Unit,
    onOpenDiet: () -> Unit,
    onOpenMood: () -> Unit,
    onOpenBmiHydration: () -> Unit,
    onQuickAddWater: () -> Unit,
    onOpenDataDashboard: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenDoctors: () -> Unit,
    onUpdateProfile: (
        displayName: String,
        age: Long,
        location: String,
        emergencyNumber: String,
        dietaryPreference: String
    ) -> Unit,
    onLoadSampleData: () -> Unit,
    onSignOut: () -> Unit
) {
    val strings = stringsFor(language)
    var showProfileDialog by remember { mutableStateOf(false) }

    val greeting = remember(language) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> strings.greetingMorning
            hour < 17 -> strings.greetingAfternoon
            else -> strings.greetingEvening
        }
    }

    val displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: fallbackUserName
    val activeReminders = reminders.filter { it.active }
    val nextReminder = activeReminders.firstOrNull {
        it.todayStatus == "PENDING" || it.todayStatus == "SNOOZED"
    }
    val takenCount = activeReminders.count { it.todayStatus == "TAKEN" }
    val totalCount = activeReminders.size
    val adherenceProgress = if (totalCount > 0) takenCount.toFloat() / totalCount.toFloat() else 0f
    val latestMood = moodEntries.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LanguageTogglePill(language = language, onToggle = onToggleLanguage)

                    IconButton(
                        onClick = onTestAlarm,
                        modifier = Modifier.testTag("dashboard_alarm_bell_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Test medicine reminder notification",
                            tint = TealDeep
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showProfileDialog = true }
                            .testTag("dashboard_profile_avatar"),
                        color = TealContainer,
                        border = BorderStroke(1.5.dp, TealDeep.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "S",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TealDeep
                            )
                        }
                    }
                }
            }
        }

        // 2. Persistent Non-Diagnostic Safety Banner
        item {
            SafetyDisclaimerBanner(language = language)
        }

        // 3. Next Medicine Reminder Hero Card
        item {
            NextMedicineHeroCard(
                reminder = nextReminder,
                hasAnyReminders = reminders.isNotEmpty(),
                language = language,
                onMarkTaken = { nextReminder?.let(onMarkTaken) },
                onSnooze = { nextReminder?.let(onSnooze) },
                onOpenScanRx = onOpenScanRx,
                onLoadSampleData = onLoadSampleData
            )
        }

        // 4. Quick Actions 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = strings.symptomOrganizer,
                        subtitle = strings.symptomOrganizerSub,
                        icon = Icons.Filled.MedicalServices,
                        iconBg = CoralContainer,
                        iconTint = CoralHeart,
                        testTag = "quick_action_symptoms",
                        onClick = onOpenSymptoms,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = strings.scanPrescription,
                        subtitle = strings.scanPrescriptionSub,
                        icon = Icons.Filled.DocumentScanner,
                        iconBg = TealContainer,
                        iconTint = TealDeep,
                        testTag = "quick_action_scan_rx",
                        onClick = onOpenScanRx,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = strings.localDietPlan,
                        subtitle = strings.localDietPlanSub,
                        icon = Icons.Filled.Restaurant,
                        iconBg = AmberContainer,
                        iconTint = AmberAccent,
                        testTag = "quick_action_diet",
                        onClick = onOpenDiet,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = strings.moodJournal,
                        subtitle = strings.moodJournalSub,
                        icon = Icons.Filled.SelfImprovement,
                        iconBg = IndigoContainer,
                        iconTint = IndigoCalm,
                        testTag = "quick_action_mood",
                        onClick = onOpenMood,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = strings.medicalReportsTitle,
                        subtitle = "${strings.medicalReportsSub} ($reportCount)",
                        icon = Icons.Filled.Description,
                        iconBg = EmeraldContainer,
                        iconTint = EmeraldSuccess,
                        testTag = "quick_action_reports",
                        onClick = onOpenReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = "${strings.proDoctorsTitle} (${strings.paidBadgeLabel})",
                        subtitle = if (appointmentCount > 0) "${strings.myAppointmentsTab}: $appointmentCount" else strings.proDoctorsSub,
                        icon = Icons.Filled.LocalHospital,
                        iconBg = CoralContainer,
                        iconTint = CoralHeart,
                        testTag = "quick_action_pro_doctors",
                        onClick = onOpenDoctors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Full-width Health Data Dashboard & Enhanced Report Export Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenDataDashboard)
                        .testTag("quick_action_data_dashboard"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, IndigoCalm.copy(alpha = 0.3f))
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
                                .clip(RoundedCornerShape(13.dp))
                                .background(IndigoCalm),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = strings.dataDashboardTitle,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dataDashboardTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = IndigoCalm
                            )
                            Text(
                                text = strings.dataDashboardSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = TealDeep, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "PDF",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealDeep
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Today's Medication Adherence Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.todayAdherence,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = "$takenCount / $totalCount",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { adherenceProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = TealDeep,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (language == AppLanguage.BN) {
                            "আজকের $totalCount টি ডোজের মধ্যে $takenCount টি গ্রহণ করা হয়েছে"
                        } else {
                            "$takenCount of $totalCount active doses taken today"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5.5 AI Health Command Bot Card (Positioned directly above Wellness / Latest Mood Card)
        item {
            AiCommandBotHomeCard(
                language = language,
                reportCount = reportCount,
                onOpenAiBot = onOpenAiBot
            )
        }

        // 5.8 BMI, Hydration & AI Life Condition Summary Card
        item {
            val bmiVal = todayMetrics?.bmiValue?.takeIf { it > 0 } ?: latestBmiRecord?.bmiValue ?: 0.0
            val bmiCat = todayMetrics?.bmiCategory?.ifBlank { null } ?: latestBmiRecord?.bmiCategory.orEmpty()
            val waterMl = todayMetrics?.waterIntakeMl ?: 0L
            val waterGoal = (todayMetrics?.waterGoalMl ?: latestBmiRecord?.waterGoalMl ?: 2500L).coerceAtLeast(500L)
            val lifeScore = todayMetrics?.lifeConditionScore?.takeIf { it > 0 } ?: latestBmiRecord?.lifeConditionScore ?: 0L
            val lifeTitle = todayMetrics?.lifeConditionTitle?.ifBlank { null } ?: latestBmiRecord?.lifeConditionTitle.orEmpty()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBmiHydration)
                    .testTag("dashboard_bmi_hydration_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = TealDeep, modifier = Modifier.size(20.dp))
                            Text(
                                text = strings.dashboardBodyHydrationTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (lifeScore > 0L) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = "$lifeScore/100",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = TealContainer.copy(alpha = 0.55f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("BMI", style = MaterialTheme.typography.labelSmall, color = TealDeep)
                                Text(
                                    text = if (bmiVal > 0.0) String.format(java.util.Locale.US, "%.1f", bmiVal) else "--",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TealDeep
                                )
                                Text(
                                    text = bmiCat.ifBlank { if (language == AppLanguage.BN) "হিসাব করুন" else "Tap to calculate" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            color = IndigoContainer.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = IndigoCalm, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (language == AppLanguage.BN) "পানি পান" else "Water",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IndigoCalm
                                        )
                                    }
                                    Surface(
                                        onClick = onQuickAddWater,
                                        shape = RoundedCornerShape(8.dp),
                                        color = IndigoCalm,
                                        modifier = Modifier.testTag("dashboard_quick_add_water_button")
                                    ) {
                                        Text(
                                            text = "+250ml",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "$waterMl / $waterGoal ml",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IndigoCalm
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (waterMl.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = IndigoCalm,
                                    trackColor = Color.White.copy(alpha = 0.7f),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    if (lifeTitle.isNotBlank()) {
                        Text(
                            text = "${strings.lifeConditionCardTitle}: $lifeTitle",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TealDeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 6. Latest Mood Check-in Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenMood)
                    .testTag("dashboard_latest_mood_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SelfImprovement,
                                contentDescription = null,
                                tint = IndigoCalm,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.latestMoodCheckIn,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (latestMood != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = IndigoContainer
                            ) {
                                Text(
                                    text = "${latestMood.detectedMoodTag} • ${latestMood.moodScore}/5",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IndigoCalm,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (latestMood != null) {
                        Text(
                            text = latestMood.aiReflection,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = strings.noMoodYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showProfileDialog) {
        val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showProfileDialog = false },
            sheetState = profileSheetState
        ) {
            EditableProfileSheetContent(
                language = language,
                displayNameInit = displayName,
                profile = profile,
                showSampleButton = reminders.isEmpty() && moodEntries.isEmpty(),
                onSaveProfile = { name, age, location, emergency, pref ->
                    onUpdateProfile(name, age, location, emergency, pref)
                    showProfileDialog = false
                },
                onLoadSampleData = {
                    showProfileDialog = false
                    onLoadSampleData()
                },
                onSignOut = {
                    showProfileDialog = false
                    onSignOut()
                }
            )
        }
    }
}

@Composable
private fun AiCommandBotHomeCard(
    language: AppLanguage,
    reportCount: Int,
    onOpenAiBot: (String?) -> Unit
) {
    val strings = stringsFor(language)
    val isBn = language == AppLanguage.BN
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAiBot(null) }
            .testTag("dashboard_ai_command_bot_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = TealDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = strings.aiBotCardTitle,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.aiBotCardTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = strings.aiBotCardSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = {
                        onOpenAiBot(
                            if (isBn) "আমার সংরক্ষিত রিপোর্টগুলো বিস্তারিত দেখাও"
                            else "Check details of my saved medical reports"
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isBn) "রিপোর্ট চেক ($reportCount)" else "Ask Reports ($reportCount)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { onOpenAiBot(null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TealDeep
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("dashboard_open_ai_bot_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.aiBotOpenBtn,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableProfileSheetContent(
    language: AppLanguage,
    displayNameInit: String,
    profile: UserProfile?,
    showSampleButton: Boolean,
    onSaveProfile: (String, Long, String, String, String) -> Unit,
    onLoadSampleData: () -> Unit,
    onSignOut: () -> Unit
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    var name by remember(profile?.displayName, displayNameInit) { mutableStateOf(displayNameInit) }
    var ageStr by remember(profile?.age) { mutableStateOf((profile?.age ?: 30L).toString()) }
    var location by remember(profile?.location) { mutableStateOf(profile?.location.orEmpty()) }
    var emergencyNumber by remember(profile?.emergencyNumber) { mutableStateOf(profile?.emergencyNumber.orEmpty()) }
    var preference by remember(profile?.dietaryPreference) { mutableStateOf(profile?.dietaryPreference ?: "Regular") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("editable_profile_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = TealDeep)
            Text(
                text = strings.editProfileTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(strings.displayNameLabel) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_name_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = ageStr,
                onValueChange = { ageStr = it.filter { ch -> ch.isDigit() }.take(3) },
                label = { Text(strings.ageLabel) },
                singleLine = true,
                modifier = Modifier
                    .weight(0.8f)
                    .testTag("profile_age_input")
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(strings.locationLabel) },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("profile_location_input")
            )
        }

        OutlinedTextField(
            value = emergencyNumber,
            onValueChange = { emergencyNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' || ch == ' ' }.take(24) },
            label = { Text(strings.emergencyNumberLabel) },
            leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_emergency_input")
        )

        if (emergencyNumber.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emergencyNumber.trim()}"))
                    context.startActivity(dialIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_call_emergency_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CoralHeart.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, tint = CoralHeart, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${strings.callEmergencyBtn}: ${emergencyNumber.trim()}", color = CoralHeart)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.dietPreferenceLabel, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Regular", "Low Carb", "Vegetarian", "Low Sodium").forEach { opt ->
                    FilterChip(
                        selected = preference == opt,
                        onClick = { preference = opt },
                        label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        Button(
            onClick = {
                val parsedAge = ageStr.toLongOrNull()?.coerceIn(1L, 120L) ?: (profile?.age ?: 30L)
                onSaveProfile(name.trim().ifBlank { displayNameInit }, parsedAge, location.trim(), emergencyNumber.trim(), preference)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_profile_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.saveProfileBtn, style = MaterialTheme.typography.labelLarge)
        }

        if (showSampleButton) {
            OutlinedButton(
                onClick = onLoadSampleData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.loadSampleData)
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_sign_out_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CoralHeart.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, tint = CoralHeart, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.signOut, color = CoralHeart)
        }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun NextMedicineHeroCard(
    reminder: MedicineReminder?,
    hasAnyReminders: Boolean,
    language: AppLanguage,
    onMarkTaken: () -> Unit,
    onSnooze: () -> Unit,
    onOpenScanRx: () -> Unit,
    onLoadSampleData: () -> Unit
) {
    val strings = stringsFor(language)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("next_medicine_hero_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = TealDeep,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.nextMedicineTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = TealDeep
                    )
                }
                if (reminder != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AmberContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = AmberOnContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = reminder.scheduledTime,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberOnContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (reminder != null) {
                Text(
                    text = reminder.medicineName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealContainer
                    ) {
                        Text(
                            text = reminder.dosage,
                            style = MaterialTheme.typography.labelMedium,
                            color = TealDeep,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = reminder.mealTiming,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onMarkTaken,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_mark_taken_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.markTaken, style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_snooze_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.snooze10m, style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasAnyReminders) strings.noUpcomingMedicine else strings.addFirstMedicineHint,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!hasAnyReminders) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenScanRx,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealDeep)
                        ) {
                            Text(strings.tabScanRx, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onLoadSampleData,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("load_sample_data_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "নমুনা ডেটা" else "Sample Data",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
