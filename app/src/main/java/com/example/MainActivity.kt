package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.ShasthoAiService
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOutUser
import com.example.data.firebase.configureAppCheck
import com.example.data.model.UiState
import com.example.data.prefs.AppPreferences
import com.example.data.repository.HealthRepository
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.components.AiCommandChatSheet
import com.example.ui.components.MedicineAlarmModal
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DataDashboardScreen
import com.example.ui.screens.DoctorsScreen
import com.example.ui.screens.MedicalReportsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrescriptionReaderScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SymptomOrganizerScreen
import com.example.ui.screens.WellnessScreen
import com.example.ui.screens.WellnessSubTab
import com.example.ui.theme.ShasthoSathiTheme
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TealDeep
import com.example.ui.viewmodel.ShasthoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class MainTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    SYMPTOMS(Icons.Filled.MedicalServices, Icons.Outlined.MedicalServices, "nav_tab_symptoms"),
    SCAN_RX(Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner, "nav_tab_scan_rx"),
    REMINDERS(Icons.Filled.Alarm, Icons.Outlined.Alarm, "nav_tab_reminders"),
    WELLNESS(Icons.Filled.Spa, Icons.Outlined.Spa, "nav_tab_wellness")
}

enum class HomeOverlay {
    REPORTS,
    DOCTORS,
    DATA_DASHBOARD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            ShasthoSathiAppRoot()
        }
    }
}

@Composable
fun ShasthoSathiAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { AppPreferences(context.applicationContext) }

    val language by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
    val onboardingSeen by prefs.onboardingSeenFlow.collectAsStateWithLifecycle(initialValue = false)

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val toggleLanguage: () -> Unit = {
        scope.launch { prefs.setLanguage(language.toggle()) }
    }

    ShasthoSathiTheme(isBangla = language == AppLanguage.BN) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val user = currentUser
            when {
                user == null && !onboardingSeen -> {
                    OnboardingScreen(
                        language = language,
                        onToggleLanguage = toggleLanguage,
                        onGetStarted = {
                            scope.launch { prefs.setOnboardingSeen(true) }
                        }
                    )
                }
                user == null -> {
                    AuthScreen(
                        language = language,
                        onToggleLanguage = toggleLanguage,
                        onAuthSuccess = { /* AuthStateListener triggers transition automatically */ }
                    )
                }
                else -> {
                    // Key composition & ViewModel by authenticated user UID
                    key(user.uid) {
                        AuthenticatedHealthShell(
                            currentUserId = user.uid,
                            fallbackName = user.displayName ?: user.email?.substringBefore("@") ?: "ShasthoSathi User",
                            language = language,
                            onToggleLanguage = toggleLanguage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedHealthShell(
    currentUserId: String,
    fallbackName: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val strings = stringsFor(language)

    val viewModel: ShasthoViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ShasthoViewModel(
                    appContext = app.applicationContext,
                    repository = HealthRepository(db),
                    aiService = ShasthoAiService(),
                    currentUserId = currentUserId,
                    initialDisplayName = fallbackName,
                    initialLanguage = language
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var wellnessSubTab by rememberSaveable { mutableStateOf(WellnessSubTab.DIET) }
    var activeOverlay by rememberSaveable { mutableStateOf<HomeOverlay?>(null) }
    var showAiBotSheet by rememberSaveable { mutableStateOf(false) }

    // Hardware back returns to Home from any secondary tab when no overlay is open
    BackHandler(enabled = activeOverlay == null && currentTab != MainTab.HOME) {
        currentTab = MainTab.HOME
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val adherenceLogsState by viewModel.adherenceLogsState.collectAsStateWithLifecycle()
    val doctorBriefsState by viewModel.doctorBriefsState.collectAsStateWithLifecycle()
    val dietPlansState by viewModel.dietPlansState.collectAsStateWithLifecycle()
    val moodEntriesState by viewModel.moodEntriesState.collectAsStateWithLifecycle()
    val medicalReportsState by viewModel.medicalReportsState.collectAsStateWithLifecycle()
    val appointmentsState by viewModel.appointmentsState.collectAsStateWithLifecycle()
    val healthMetricsState by viewModel.healthMetricsState.collectAsStateWithLifecycle()

    val aiChatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val aiChatLoading by viewModel.aiChatLoading.collectAsStateWithLifecycle()
    val aiChatError by viewModel.aiChatError.collectAsStateWithLifecycle()
    val pendingNavTarget by viewModel.pendingNavigationTarget.collectAsStateWithLifecycle()
    val pendingLangToggle by viewModel.pendingLanguageToggle.collectAsStateWithLifecycle()
    val reportScanLoading by viewModel.reportScanLoading.collectAsStateWithLifecycle()
    val lifeConditionLoading by viewModel.lifeConditionLoading.collectAsStateWithLifecycle()
    val symptomAttachmentLoading by viewModel.symptomAttachmentLoading.collectAsStateWithLifecycle()

    val symptomMessages by viewModel.symptomMessages.collectAsStateWithLifecycle()
    val symptomLoading by viewModel.symptomChatLoading.collectAsStateWithLifecycle()
    val symptomError by viewModel.symptomError.collectAsStateWithLifecycle()
    val briefState by viewModel.briefState.collectAsStateWithLifecycle()

    val rxPreviewBitmap by viewModel.rxPreviewBitmap.collectAsStateWithLifecycle()
    val rxState by viewModel.rxState.collectAsStateWithLifecycle()

    val dietGenState by viewModel.dietGenState.collectAsStateWithLifecycle()
    val moodGenState by viewModel.moodGenState.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val activeAlarmReminder by viewModel.activeAlarmReminder.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(pendingNavTarget) {
        val target = pendingNavTarget
        if (!target.isNullOrBlank()) {
            when (target) {
                "HOME" -> {
                    activeOverlay = null
                    currentTab = MainTab.HOME
                }
                "SYMPTOMS" -> {
                    activeOverlay = null
                    currentTab = MainTab.SYMPTOMS
                }
                "SCAN_RX" -> {
                    activeOverlay = null
                    currentTab = MainTab.SCAN_RX
                }
                "REMINDERS" -> {
                    activeOverlay = null
                    currentTab = MainTab.REMINDERS
                }
                "DIET" -> {
                    activeOverlay = null
                    wellnessSubTab = WellnessSubTab.DIET
                    currentTab = MainTab.WELLNESS
                }
                "MOOD" -> {
                    activeOverlay = null
                    wellnessSubTab = WellnessSubTab.MOOD
                    currentTab = MainTab.WELLNESS
                }
                "BMI" -> {
                    activeOverlay = null
                    wellnessSubTab = WellnessSubTab.BMI_HYDRATION
                    currentTab = MainTab.WELLNESS
                }
                "DATA_DASHBOARD" -> {
                    currentTab = MainTab.HOME
                    activeOverlay = HomeOverlay.DATA_DASHBOARD
                }
                "REPORTS" -> {
                    currentTab = MainTab.HOME
                    activeOverlay = HomeOverlay.REPORTS
                }
                "DOCTORS" -> {
                    currentTab = MainTab.HOME
                    activeOverlay = HomeOverlay.DOCTORS
                }
            }
            showAiBotSheet = false
            viewModel.consumeNavigationTarget()
        }
    }

    LaunchedEffect(pendingLangToggle) {
        if (pendingLangToggle) {
            onToggleLanguage()
            viewModel.consumeLanguageToggle()
        }
    }

    val profile = (profileState as? UiState.Success)?.data
    val reminders = (remindersState as? UiState.Success)?.data.orEmpty()
    val adherenceLogs = (adherenceLogsState as? UiState.Success)?.data.orEmpty()
    val doctorBriefs = (doctorBriefsState as? UiState.Success)?.data.orEmpty()
    val dietPlans = (dietPlansState as? UiState.Success)?.data.orEmpty()
    val moodEntries = (moodEntriesState as? UiState.Success)?.data.orEmpty()
    val medicalReports = (medicalReportsState as? UiState.Success)?.data.orEmpty()
    val appointments = (appointmentsState as? UiState.Success)?.data.orEmpty()
    val healthMetrics = (healthMetricsState as? UiState.Success)?.data.orEmpty()
    val todayDateKey = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val todayMetrics = healthMetrics.firstOrNull { it.dateKey == todayDateKey }
    val latestBmiRecord = healthMetrics.firstOrNull { it.bmiValue > 0.0 } ?: healthMetrics.firstOrNull()

    fun tabLabel(tab: MainTab): String = when (tab) {
        MainTab.HOME -> strings.tabHome
        MainTab.SYMPTOMS -> strings.tabSymptoms
        MainTab.SCAN_RX -> strings.tabScanRx
        MainTab.REMINDERS -> strings.tabReminders
        MainTab.WELLNESS -> strings.tabWellness
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = activeOverlay == null && currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    activeOverlay = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TealDeep,
                                    selectedTextColor = TealDeep,
                                    indicatorColor = TealContainer
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = activeOverlay == null && currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    activeOverlay = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tabLabel(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TealDeep,
                                    selectedTextColor = TealDeep,
                                    indicatorColor = TealContainer
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 640.dp)
                    ) {
                        when {
                            activeOverlay == HomeOverlay.DATA_DASHBOARD -> DataDashboardScreen(
                                language = language,
                                profile = profile,
                                reminders = reminders,
                                adherenceLogs = adherenceLogs,
                                healthMetrics = healthMetrics,
                                moodEntries = moodEntries,
                                medicalReports = medicalReports,
                                doctorBriefs = doctorBriefs,
                                appointments = appointments,
                                onBack = { activeOverlay = null },
                                onPostToast = viewModel::postMessage
                            )

                            activeOverlay == HomeOverlay.REPORTS -> MedicalReportsScreen(
                                language = language,
                                profile = profile,
                                reports = medicalReports,
                                isScanningPhoto = reportScanLoading,
                                onBack = { activeOverlay = null },
                                onAddManualReport = { title, cat, docOrLab, date, summary, details ->
                                    viewModel.addMedicalReport(
                                        title = title,
                                        category = cat,
                                        doctorOrLab = docOrLab,
                                        reportDate = date,
                                        summary = summary,
                                        details = details,
                                        language = language
                                    )
                                },
                                onScanReportBitmap = { bmp ->
                                    viewModel.scanAndSaveReportBitmap(bmp, language)
                                },
                                onUploadDocumentFile = { bytes, bmp, mime, fileName, attachType ->
                                    viewModel.uploadFileOrAudioToReports(
                                        bytes = bytes,
                                        bitmap = bmp,
                                        mimeType = mime,
                                        fileName = fileName,
                                        attachmentType = attachType,
                                        language = language
                                    )
                                },
                                onPostToast = viewModel::postMessage,
                                onDeleteReport = viewModel::deleteMedicalReport,
                                onAskAiAboutReport = { report ->
                                    showAiBotSheet = true
                                    val prompt = if (language == AppLanguage.BN) {
                                        "আমার রিপোর্ট #${report.serialNumber} (${report.title}) এর বিস্তারিত সহজভাবে বুঝিয়ে বলো।"
                                    } else {
                                        "Explain the details of my Report #${report.serialNumber} (${report.title}) clearly."
                                    }
                                    viewModel.sendAiCommand(prompt, language)
                                }
                            )

                            activeOverlay == HomeOverlay.DOCTORS -> DoctorsScreen(
                                language = language,
                                appointments = appointments,
                                reports = medicalReports,
                                onBack = { activeOverlay = null },
                                onBookPaidAppointment = { doctor, date, slot, mode, payment, reason, attachedTitle, onBooked ->
                                    viewModel.bookPaidDoctorAppointment(
                                        doctor = doctor,
                                        appointmentDate = date,
                                        timeSlot = slot,
                                        consultationMode = mode,
                                        paymentMethod = payment,
                                        reasonForVisit = reason,
                                        attachedReportTitle = attachedTitle,
                                        language = language,
                                        onBooked = onBooked
                                    )
                                },
                                onCancelAppointment = { appt ->
                                    viewModel.cancelDoctorAppointment(appt, language)
                                }
                            )

                            currentTab == MainTab.HOME -> DashboardScreen(
                                language = language,
                                onToggleLanguage = onToggleLanguage,
                                profile = profile,
                                fallbackUserName = fallbackName,
                                reminders = reminders,
                                moodEntries = moodEntries,
                                todayMetrics = todayMetrics,
                                latestBmiRecord = latestBmiRecord,
                                reportCount = medicalReports.size,
                                appointmentCount = appointments.count { it.bookingStatus != "CANCELLED" },
                                onMarkTaken = viewModel::markReminderTaken,
                                onSnooze = viewModel::snoozeReminder10Min,
                                onTestAlarm = { viewModel.triggerTestAlarm(language) },
                                onOpenAiBot = { initialPrompt ->
                                    showAiBotSheet = true
                                    if (!initialPrompt.isNullOrBlank()) {
                                        viewModel.sendAiCommand(initialPrompt, language)
                                    }
                                },
                                onOpenSymptoms = { currentTab = MainTab.SYMPTOMS },
                                onOpenScanRx = { currentTab = MainTab.SCAN_RX },
                                onOpenDiet = {
                                    wellnessSubTab = WellnessSubTab.DIET
                                    currentTab = MainTab.WELLNESS
                                },
                                onOpenMood = {
                                    wellnessSubTab = WellnessSubTab.MOOD
                                    currentTab = MainTab.WELLNESS
                                },
                                onOpenBmiHydration = {
                                    wellnessSubTab = WellnessSubTab.BMI_HYDRATION
                                    currentTab = MainTab.WELLNESS
                                },
                                onQuickAddWater = {
                                    viewModel.updateWaterIntake(deltaMl = 250, newGoalMl = null, language = language)
                                },
                                onOpenDataDashboard = { activeOverlay = HomeOverlay.DATA_DASHBOARD },
                                onOpenReports = { activeOverlay = HomeOverlay.REPORTS },
                                onOpenDoctors = { activeOverlay = HomeOverlay.DOCTORS },
                                onUpdateProfile = { name, age, location, emergencyNumber, pref ->
                                    viewModel.updateProfile(
                                        displayName = name,
                                        age = age,
                                        location = location,
                                        emergencyNumber = emergencyNumber,
                                        language = language,
                                        conditions = profile?.healthConditions ?: listOf("General Wellness"),
                                        preference = pref
                                    )
                                },
                                onLoadSampleData = { viewModel.loadStarterSampleData(language) },
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )

                            currentTab == MainTab.SYMPTOMS -> SymptomOrganizerScreen(
                                language = language,
                                messages = symptomMessages,
                                isChatLoading = symptomLoading,
                                isAttachmentLoading = symptomAttachmentLoading,
                                chatError = symptomError,
                                briefState = briefState,
                                savedBriefs = doctorBriefs,
                                onSendMessage = { viewModel.sendSymptomMessage(it, language) },
                                onUploadAttachment = { bytes, bmp, mime, fileName, attachType, saveToVault ->
                                    viewModel.uploadSymptomAttachment(
                                        bytes = bytes,
                                        bitmap = bmp,
                                        mimeType = mime,
                                        fileName = fileName,
                                        attachmentType = attachType,
                                        saveToReportsVault = saveToVault,
                                        language = language
                                    )
                                },
                                onPostToast = viewModel::postMessage,
                                onGenerateBrief = { viewModel.generateAndSaveDoctorBrief(language) },
                                onStartNewCheck = { viewModel.resetSymptomSession(language) },
                                onCopyToast = { viewModel.postMessage(strings.briefCopiedToast) }
                            )

                            currentTab == MainTab.SCAN_RX -> PrescriptionReaderScreen(
                                language = language,
                                previewBitmap = rxPreviewBitmap,
                                rxState = rxState,
                                onAnalyzeBitmap = { bmp -> viewModel.analyzePrescriptionBitmap(bmp, language) },
                                onUpdateMed = viewModel::updateExtractedMedication,
                                onRemoveMed = viewModel::removeExtractedMedication,
                                onAutoScheduleAll = {
                                    viewModel.autoScheduleExtractedMedications(language) {
                                        currentTab = MainTab.REMINDERS
                                    }
                                },
                                onSaveToReports = {
                                    viewModel.saveCurrentRxToReports(language)
                                }
                            )

                            currentTab == MainTab.REMINDERS -> RemindersScreen(
                                language = language,
                                reminders = reminders,
                                onToggleActive = viewModel::toggleReminder,
                                onMarkTaken = viewModel::markReminderTaken,
                                onSnooze = viewModel::snoozeReminder10Min,
                                onSkip = viewModel::skipReminder,
                                onDelete = viewModel::deleteReminder,
                                onTestAlarmNow = { viewModel.triggerTestAlarm(language) },
                                onAddManual = { name, dosage, times, meal, days, notes, ringtoneUri, ringtoneName ->
                                    viewModel.addManualReminder(
                                        name = name,
                                        dosage = dosage,
                                        scheduledTimes = times,
                                        mealTiming = meal,
                                        durationDays = days,
                                        notes = notes,
                                        ringtoneUri = ringtoneUri,
                                        ringtoneName = ringtoneName,
                                        language = language
                                    )
                                },
                                onEditReminder = { existing, name, dosage, time, meal, days, notes, ringtoneUri, ringtoneName ->
                                    viewModel.updateReminder(
                                        existing = existing,
                                        name = name,
                                        dosage = dosage,
                                        scheduledTime = time,
                                        mealTiming = meal,
                                        durationDays = days,
                                        notes = notes,
                                        ringtoneUri = ringtoneUri,
                                        ringtoneName = ringtoneName,
                                        language = language
                                    )
                                }
                            )

                            else -> WellnessScreen(
                                language = language,
                                activeSubTab = wellnessSubTab,
                                onSelectSubTab = { wellnessSubTab = it },
                                userAge = (profile?.age ?: 30L).toInt(),
                                dietGenState = dietGenState,
                                latestSavedDiet = dietPlans.firstOrNull(),
                                onGenerateDiet = { age, conds, pref ->
                                    viewModel.generateLocalDietPlan(age, conds, pref, language)
                                },
                                moodGenState = moodGenState,
                                moodEntries = moodEntries,
                                onAnalyzeMood = { mood, text ->
                                    viewModel.analyzeAndSaveMood(mood, text, language)
                                },
                                todayMetrics = todayMetrics,
                                latestBmiRecord = latestBmiRecord,
                                isGeneratingLifeSummary = lifeConditionLoading,
                                onSaveBmi = { heightCm, weightKg ->
                                    viewModel.saveBmiMeasurement(heightCm, weightKg, language)
                                },
                                onUpdateWater = { deltaMl, newGoalMl ->
                                    viewModel.updateWaterIntake(deltaMl, newGoalMl, language)
                                },
                                onGenerateLifeSummary = {
                                    viewModel.generateLifeConditionSummary(language)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // AI Health Command Bot Modal Sheet (accessible from Home card or Reports Vault)
    if (showAiBotSheet) {
        AiCommandChatSheet(
            language = language,
            messages = aiChatMessages,
            isLoading = aiChatLoading,
            errorMessage = aiChatError,
            reportCount = medicalReports.size,
            onSendMessage = { msg -> viewModel.sendAiCommand(msg, language) },
            onResetChat = { viewModel.resetAiCommandChat(language) },
            onDismiss = { showAiBotSheet = false }
        )
    }

    // In-App Medicine Reminder Alarm Modal Overlay
    activeAlarmReminder?.let { alarmReminder ->
        MedicineAlarmModal(
            reminder = alarmReminder,
            language = language,
            onMarkTaken = { viewModel.markReminderTaken(alarmReminder) },
            onSnooze = { viewModel.snoozeReminder10Min(alarmReminder) },
            onDismiss = viewModel::dismissAlarmModal
        )
    }
}
