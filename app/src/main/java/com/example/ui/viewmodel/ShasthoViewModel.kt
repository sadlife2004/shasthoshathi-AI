package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmSoundPlayer
import com.example.alarm.MedicineAlarmScheduler
import com.example.data.ai.ShasthoAiService
import com.example.data.model.AdherenceLog
import com.example.data.model.AiCommandChatMessage
import com.example.data.model.AiCommandResult
import com.example.data.model.DietPlanRecord
import com.example.data.model.DoctorAppointment
import com.example.data.model.DoctorBrief
import com.example.data.model.ExtractedMedication
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.MedicalReport
import com.example.data.model.MedicineReminder
import com.example.data.model.MoodEntry
import com.example.data.model.PrescriptionAnalysisResult
import com.example.data.model.ProfessionalDoctor
import com.example.data.model.SymptomChatMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.HealthRepository
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val VM_TAG = "ShasthoViewModel"

class ShasthoViewModel(
    private val appContext: Context,
    private val repository: HealthRepository,
    private val aiService: ShasthoAiService,
    val currentUserId: String,
    initialDisplayName: String,
    initialLanguage: AppLanguage
) : ViewModel() {

    // --- Real-time Firestore Streams with Two-Tier Error Unwrapping ---

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { emit(unwrapStreamError(it, "profile")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val remindersState: StateFlow<UiState<List<MedicineReminder>>> =
        repository.observeReminders(currentUserId)
            .map<List<MedicineReminder>, UiState<List<MedicineReminder>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "reminders")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adherenceLogsState: StateFlow<UiState<List<AdherenceLog>>> =
        repository.observeAdherenceLogs(currentUserId)
            .map<List<AdherenceLog>, UiState<List<AdherenceLog>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "adherence logs")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val doctorBriefsState: StateFlow<UiState<List<DoctorBrief>>> =
        repository.observeDoctorBriefs(currentUserId)
            .map<List<DoctorBrief>, UiState<List<DoctorBrief>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "doctor briefs")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val dietPlansState: StateFlow<UiState<List<DietPlanRecord>>> =
        repository.observeDietPlans(currentUserId)
            .map<List<DietPlanRecord>, UiState<List<DietPlanRecord>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "diet plans")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val moodEntriesState: StateFlow<UiState<List<MoodEntry>>> =
        repository.observeMoodEntries(currentUserId)
            .map<List<MoodEntry>, UiState<List<MoodEntry>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "mood entries")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val medicalReportsState: StateFlow<UiState<List<MedicalReport>>> =
        repository.observeMedicalReports(currentUserId)
            .map<List<MedicalReport>, UiState<List<MedicalReport>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "medical reports")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val appointmentsState: StateFlow<UiState<List<DoctorAppointment>>> =
        repository.observeAppointments(currentUserId)
            .map<List<DoctorAppointment>, UiState<List<DoctorAppointment>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "doctor appointments")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val healthMetricsState: StateFlow<UiState<List<HealthMetricsRecord>>> =
        repository.observeHealthMetrics(currentUserId)
            .map<List<HealthMetricsRecord>, UiState<List<HealthMetricsRecord>>> { UiState.Success(it) }
            .catch { emit(unwrapStreamError(it, "health metrics")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // --- UI Feedback & Alarm Modal State ---

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activeAlarmReminder = MutableStateFlow<MedicineReminder?>(null)
    val activeAlarmReminder: StateFlow<MedicineReminder?> = _activeAlarmReminder.asStateFlow()
    private val triggeredAlarmKeys = mutableSetOf<String>()

    // --- AI Command Bot State (Controls App Features + Answers Medical Report Questions) ---

    private val _aiChatMessages = MutableStateFlow(
        listOf(
            AiCommandChatMessage(
                id = "bot_welcome",
                fromUser = false,
                text = stringsFor(initialLanguage).aiBotWelcomeMsg,
                timeLabel = currentTimeLabel()
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiCommandChatMessage>> = _aiChatMessages.asStateFlow()

    private val _aiChatLoading = MutableStateFlow(false)
    val aiChatLoading: StateFlow<Boolean> = _aiChatLoading.asStateFlow()

    private val _aiChatError = MutableStateFlow<String?>(null)
    val aiChatError: StateFlow<String?> = _aiChatError.asStateFlow()

    private val _pendingNavigationTarget = MutableStateFlow<String?>(null)
    val pendingNavigationTarget: StateFlow<String?> = _pendingNavigationTarget.asStateFlow()

    private val _pendingLanguageToggle = MutableStateFlow(false)
    val pendingLanguageToggle: StateFlow<Boolean> = _pendingLanguageToggle.asStateFlow()

    // --- Report AI Scanner State ---

    private val _reportScanLoading = MutableStateFlow(false)
    val reportScanLoading: StateFlow<Boolean> = _reportScanLoading.asStateFlow()

    private val _lifeConditionLoading = MutableStateFlow(false)
    val lifeConditionLoading: StateFlow<Boolean> = _lifeConditionLoading.asStateFlow()

    private val _symptomAttachmentLoading = MutableStateFlow(false)
    val symptomAttachmentLoading: StateFlow<Boolean> = _symptomAttachmentLoading.asStateFlow()

    // --- Feature 1: Symptom Organizer State ---

    private val _symptomMessages = MutableStateFlow<List<SymptomChatMessage>>(
        listOf(
            SymptomChatMessage(
                id = "intro",
                fromUser = false,
                text = stringsFor(initialLanguage).symptomIntroAi,
                timeLabel = currentTimeLabel()
            )
        )
    )
    val symptomMessages: StateFlow<List<SymptomChatMessage>> = _symptomMessages.asStateFlow()

    private val _symptomChatLoading = MutableStateFlow(false)
    val symptomChatLoading: StateFlow<Boolean> = _symptomChatLoading.asStateFlow()

    private val _symptomError = MutableStateFlow<String?>(null)
    val symptomError: StateFlow<String?> = _symptomError.asStateFlow()

    private val _briefState = MutableStateFlow<UiState<DoctorBrief>>(UiState.Idle)
    val briefState: StateFlow<UiState<DoctorBrief>> = _briefState.asStateFlow()

    // --- Feature 2: Prescription Reader State ---

    private val _rxPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val rxPreviewBitmap: StateFlow<Bitmap?> = _rxPreviewBitmap.asStateFlow()

    private val _rxState = MutableStateFlow<UiState<PrescriptionAnalysisResult>>(UiState.Idle)
    val rxState: StateFlow<UiState<PrescriptionAnalysisResult>> = _rxState.asStateFlow()
    private var rxAnalysisJob: Job? = null

    // --- Feature 4: Diet Planner State ---

    private val _dietGenState = MutableStateFlow<UiState<DietPlanRecord>>(UiState.Idle)
    val dietGenState: StateFlow<UiState<DietPlanRecord>> = _dietGenState.asStateFlow()

    // --- Feature 5: Mood Journal Analyzer State ---

    private val _moodGenState = MutableStateFlow<UiState<MoodEntry>>(UiState.Idle)
    val moodGenState: StateFlow<UiState<MoodEntry>> = _moodGenState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                defaultName = initialDisplayName,
                languageCode = initialLanguage.code
            )
        }
        // Continuously sync active reminders to Android AlarmManager for background ringtone alarms
        viewModelScope.launch {
            remindersState.collect { state ->
                if (state is UiState.Success) {
                    MedicineAlarmScheduler.syncReminders(appContext, state.data)
                }
            }
        }
        startReminderAlarmWatcher()
    }

    private fun unwrapStreamError(error: Throwable, label: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(VM_TAG, "Error observing $label", rootError)
        return UiState.Error(rootError.message ?: "Failed to load $label")
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun consumeNavigationTarget() {
        _pendingNavigationTarget.value = null
    }

    fun consumeLanguageToggle() {
        _pendingLanguageToggle.value = false
    }

    // --- In-App Reminder Alarm Watcher + Background Alarm Sync ---

    private fun startReminderAlarmWatcher() {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
                val nowFormatted = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                val today = repository.todayDateKey()
                val matching = reminders.firstOrNull { rem ->
                    rem.active &&
                        rem.todayStatus != "TAKEN" &&
                        rem.todayStatus != "SKIPPED" &&
                        rem.scheduledTime.equals(nowFormatted, ignoreCase = true) &&
                        !triggeredAlarmKeys.contains("${rem.id}_${today}_$nowFormatted")
                }
                if (matching != null && _activeAlarmReminder.value == null) {
                    triggeredAlarmKeys.add("${matching.id}_${today}_$nowFormatted")
                    _activeAlarmReminder.value = matching
                }
            }
        }
    }

    fun triggerTestAlarm(language: AppLanguage) {
        val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
        val target = reminders.firstOrNull { it.active && it.todayStatus != "TAKEN" }
            ?: reminders.firstOrNull()
            ?: MedicineReminder(
                id = "demo_alarm",
                userId = currentUserId,
                medicineName = "Napa 500mg (Paracetamol)",
                dosage = "1 Tablet",
                timeSlot = "NOON",
                scheduledTime = currentTimeLabel(),
                mealTiming = "After Meal",
                durationDays = 5,
                sourceType = "AUTO_RX",
                active = true,
                todayStatus = "PENDING",
                notes = if (language == AppLanguage.BN) "খাবারের পরে পানি দিয়ে খান" else "Take with a glass of water after meal",
                ringtoneName = stringsFor(language).defaultRingtoneName
            )
        _activeAlarmReminder.value = target
    }

    fun dismissAlarmModal() {
        AlarmSoundPlayer.stop()
        _activeAlarmReminder.value = null
    }

    // --- AI Health Command Bot ---

    fun resetAiCommandChat(language: AppLanguage) {
        _aiChatMessages.value = listOf(
            AiCommandChatMessage(
                id = UUID.randomUUID().toString(),
                fromUser = false,
                text = stringsFor(language).aiBotWelcomeMsg,
                timeLabel = currentTimeLabel()
            )
        )
        _aiChatError.value = null
    }

    fun sendAiCommand(message: String, language: AppLanguage) {
        val cleaned = message.trim()
        if (cleaned.isBlank() || _aiChatLoading.value) return
        val userMsg = AiCommandChatMessage(
            id = UUID.randomUUID().toString(),
            fromUser = true,
            text = cleaned,
            timeLabel = currentTimeLabel()
        )
        val updatedHistory = _aiChatMessages.value + userMsg
        _aiChatMessages.value = updatedHistory
        _aiChatError.value = null
        _aiChatLoading.value = true

        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data
            val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
            val reports = (medicalReportsState.value as? UiState.Success)?.data.orEmpty()
            val briefs = (doctorBriefsState.value as? UiState.Success)?.data.orEmpty()

            val result = aiService.processAssistantCommand(
                userMessage = cleaned,
                chatHistory = updatedHistory,
                profile = profile,
                reminders = reminders,
                reports = reports,
                briefs = briefs,
                language = language
            )
            _aiChatLoading.value = false

            result.onSuccess { cmd ->
                val badge = executeAiCommand(cmd, language)
                _aiChatMessages.value = _aiChatMessages.value + AiCommandChatMessage(
                    id = UUID.randomUUID().toString(),
                    fromUser = false,
                    text = cmd.reply,
                    timeLabel = currentTimeLabel(),
                    actionBadge = badge
                )
            }.onFailure { err ->
                _aiChatError.value = err.localizedMessage ?: "AI Command Bot could not respond. Please retry."
            }
        }
    }

    private fun executeAiCommand(cmd: AiCommandResult, language: AppLanguage): String {
        val isBn = language == AppLanguage.BN
        return when (cmd.actionType.uppercase(Locale.US)) {
            "NAVIGATE" -> {
                val target = cmd.targetScreen.uppercase(Locale.US)
                if (target.isNotBlank()) {
                    _pendingNavigationTarget.value = target
                    if (isBn) "খোলা হয়েছে: $target" else "Opened: $target"
                } else ""
            }
            "ADD_REMINDER" -> {
                val medName = cmd.medicineName.trim().ifBlank { return "" }
                val time = normalizeClockLabel(cmd.scheduledTime)
                val dosage = cmd.dosage.trim().ifBlank { "1 Tablet" }
                val meal = cmd.mealTiming.trim().ifBlank { "After Meal" }
                addManualReminder(
                    name = medName,
                    dosage = dosage,
                    scheduledTimes = listOf(time),
                    mealTiming = meal,
                    durationDays = cmd.durationDays.coerceIn(1, 365),
                    notes = if (isBn) "এআই কমান্ড বট দ্বারা যোগ করা" else "Added via AI Command Bot",
                    ringtoneUri = "",
                    ringtoneName = stringsFor(language).defaultRingtoneName,
                    language = language
                )
                if (isBn) "রিমাইন্ডার সেট: $medName ($time)" else "Reminder set: $medName ($time)"
            }
            "MARK_REMINDER_TAKEN" -> {
                val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
                val match = findMatchingReminder(reminders, cmd.medicineName)
                if (match != null) {
                    markReminderTaken(match)
                    if (isBn) "খাওয়া সম্পন্ন: ${match.medicineName}" else "Marked Taken: ${match.medicineName}"
                } else ""
            }
            "SNOOZE_REMINDER" -> {
                val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
                val match = findMatchingReminder(reminders, cmd.medicineName)
                if (match != null) {
                    snoozeReminder10Min(match)
                    if (isBn) "১০ মি. স্নুজ: ${match.medicineName}" else "Snoozed 10m: ${match.medicineName}"
                } else ""
            }
            "TOGGLE_LANGUAGE" -> {
                _pendingLanguageToggle.value = true
                if (isBn) "ভাষা পরিবর্তন হয়েছে" else "Language switched"
            }
            "ADD_REPORT" -> {
                val title = cmd.reportTitle.trim().ifBlank { return "" }
                addMedicalReport(
                    title = title,
                    category = cmd.reportCategory.ifBlank { "LAB_TEST" },
                    doctorOrLab = if (isBn) "এআই বট এন্ট্রি" else "AI Bot Entry",
                    reportDate = repository.todayDateKey(),
                    summary = cmd.reportSummary.ifBlank { cmd.reply },
                    details = cmd.reportSummary.ifBlank { cmd.reply },
                    language = language
                )
                if (isBn) "রিপোর্ট সংরক্ষিত: $title" else "Report saved: $title"
            }
            "UPDATE_PROFILE" -> {
                val existing = (profileState.value as? UiState.Success)?.data
                val newAge = if (cmd.profileAge in 1..120) cmd.profileAge.toLong() else (existing?.age ?: 30L)
                val newLocation = cmd.profileLocation.trim().ifBlank { existing?.location.orEmpty() }
                val newEmergency = cmd.profileEmergencyNumber.trim().ifBlank { existing?.emergencyNumber.orEmpty() }
                updateProfile(
                    displayName = existing?.displayName?.ifBlank { "ShasthoSathi User" } ?: "ShasthoSathi User",
                    age = newAge,
                    location = newLocation,
                    emergencyNumber = newEmergency,
                    language = language,
                    conditions = existing?.healthConditions ?: listOf("General Wellness"),
                    preference = existing?.dietaryPreference ?: "Regular"
                )
                if (isBn) "প্রোফাইল আপডেট হয়েছে" else "Profile updated"
            }
            "TEST_ALARM" -> {
                triggerTestAlarm(language)
                if (isBn) "টেস্ট অ্যালার্ম চালু হয়েছে" else "Test alarm triggered"
            }
            else -> ""
        }
    }

    private fun findMatchingReminder(reminders: List<MedicineReminder>, query: String): MedicineReminder? {
        val cleaned = query.trim().lowercase(Locale.US)
        if (cleaned.isNotBlank()) {
            val byName = reminders.firstOrNull {
                it.active && it.medicineName.lowercase(Locale.US).contains(cleaned)
            }
            if (byName != null) return byName
        }
        return reminders.firstOrNull { it.active && it.todayStatus != "TAKEN" } ?: reminders.firstOrNull()
    }

    private fun normalizeClockLabel(raw: String): String {
        val cleaned = raw.trim().uppercase(Locale.US)
        val match = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""").matchEntire(cleaned)
        if (match != null) {
            val h = match.groupValues[1].toInt().coerceIn(1, 12)
            val m = match.groupValues[2].toInt().coerceIn(0, 59)
            return String.format(Locale.US, "%02d:%02d %s", h, m, match.groupValues[3])
        }
        return "09:00 PM"
    }

    // --- Profile & Sample Data ---

    fun updateProfile(
        displayName: String,
        age: Long,
        location: String,
        emergencyNumber: String,
        language: AppLanguage,
        conditions: List<String>,
        preference: String
    ) {
        viewModelScope.launch {
            val res = repository.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                age = age,
                location = location,
                emergencyNumber = emergencyNumber,
                languageCode = language.code,
                healthConditions = conditions,
                dietaryPreference = preference
            )
            res.onSuccess {
                _snackbarMessage.value = stringsFor(language).profileUpdatedToast
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    fun loadStarterSampleData(language: AppLanguage) {
        viewModelScope.launch {
            val res = repository.seedStarterData(currentUserId, isBangla = language == AppLanguage.BN)
            res.onSuccess {
                _snackbarMessage.value = stringsFor(language).sampleDataLoaded
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not load sample data."
            }
        }
    }

    // --- Serial Medical Reports Vault ---

    fun addMedicalReport(
        title: String,
        category: String,
        doctorOrLab: String,
        reportDate: String,
        summary: String,
        details: String,
        language: AppLanguage,
        attachmentType: String = "TEXT",
        attachmentName: String = ""
    ) {
        val existingReports = (medicalReportsState.value as? UiState.Success)?.data.orEmpty()
        val nextSerial = (existingReports.maxOfOrNull { it.serialNumber } ?: 0L) + 1L
        val report = MedicalReport(
            userId = currentUserId,
            serialNumber = nextSerial,
            title = title.trim(),
            category = category,
            doctorOrLab = doctorOrLab.trim(),
            reportDate = reportDate.trim().ifBlank { repository.todayDateKey() },
            summary = summary.trim().ifBlank { title.trim() },
            details = details.trim(),
            attachmentType = attachmentType,
            attachmentName = attachmentName.trim()
        )
        viewModelScope.launch {
            repository.addMedicalReport(currentUserId, report).onSuccess {
                _snackbarMessage.value = "${stringsFor(language).reportSavedToast} (#$nextSerial)"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save medical report."
            }
        }
    }

    fun uploadFileOrAudioToReports(
        bytes: ByteArray?,
        bitmap: Bitmap?,
        mimeType: String,
        fileName: String,
        attachmentType: String,
        language: AppLanguage
    ) {
        if (_reportScanLoading.value) return
        viewModelScope.launch {
            _reportScanLoading.value = true
            val analyzed = aiService.analyzeSymptomAttachment(
                bytes = bytes,
                bitmap = bitmap,
                mimeType = mimeType,
                fileName = fileName,
                attachmentType = attachmentType,
                language = language
            )
            _reportScanLoading.value = false
            analyzed.onSuccess { parsed ->
                addMedicalReport(
                    title = parsed.title.ifBlank { fileName.ifBlank { "Uploaded Medical File" } },
                    category = parsed.category,
                    doctorOrLab = if (language == AppLanguage.BN) "ফাইল / ড্রাইভ আপলোড" else "File / Drive Upload",
                    reportDate = repository.todayDateKey(),
                    summary = parsed.summary,
                    details = parsed.details.ifBlank { parsed.summary },
                    language = language,
                    attachmentType = attachmentType,
                    attachmentName = fileName
                )
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not analyze uploaded file."
            }
        }
    }

    fun scanAndSaveReportBitmap(bitmap: Bitmap, language: AppLanguage) {
        if (_reportScanLoading.value) return
        viewModelScope.launch {
            _reportScanLoading.value = true
            val extracted = aiService.extractMedicalReportFromBitmap(bitmap, language)
            _reportScanLoading.value = false
            extracted.onSuccess { parsed ->
                addMedicalReport(
                    title = parsed.title.ifBlank { "Scanned Medical Report" },
                    category = parsed.category.ifBlank { "LAB_TEST" },
                    doctorOrLab = parsed.doctorOrLab,
                    reportDate = repository.todayDateKey(),
                    summary = parsed.summary,
                    details = parsed.details,
                    language = language
                )
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not scan medical report photo."
            }
        }
    }

    fun saveCurrentRxToReports(language: AppLanguage) {
        val current = (_rxState.value as? UiState.Success)?.data ?: return
        if (current.medicines.isEmpty()) return
        val medNames = current.medicines.joinToString(", ") { it.name }
        val detailsText = current.medicines.joinToString("\n") { m ->
            val sched = "${if (m.morning) 1 else 0}+${if (m.noon) 1 else 0}+${if (m.night) 1 else 0}"
            "• ${m.name} (${m.dosage}) — $sched, ${m.mealTiming}, ${m.durationDays}d ${if (m.specialNotes.isNotBlank()) "- ${m.specialNotes}" else ""}"
        }
        addMedicalReport(
            title = if (language == AppLanguage.BN) "প্রেসক্রিপশন (${repository.todayShortLabel()})" else "Prescription (${repository.todayShortLabel()})",
            category = "PRESCRIPTION",
            doctorOrLab = "AI Rx Reader",
            reportDate = repository.todayDateKey(),
            summary = medNames.take(550),
            details = detailsText,
            language = language
        )
    }

    fun deleteMedicalReport(report: MedicalReport) {
        viewModelScope.launch {
            repository.deleteMedicalReport(currentUserId, report.id).onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to delete report."
            }
        }
    }

    // --- Paid Professional Doctor Online Appointments ---

    fun bookPaidDoctorAppointment(
        doctor: ProfessionalDoctor,
        appointmentDate: String,
        timeSlot: String,
        consultationMode: String,
        paymentMethod: String,
        reasonForVisit: String,
        attachedReportTitle: String,
        language: AppLanguage,
        onBooked: () -> Unit
    ) {
        val refCode = "SS-${(100000..999999).random()}"
        val appointment = DoctorAppointment(
            userId = currentUserId,
            doctorId = doctor.id,
            doctorName = if (language == AppLanguage.BN) doctor.nameBn else doctor.nameEn,
            specialty = if (language == AppLanguage.BN) doctor.specialtyBn else doctor.specialtyEn,
            hospital = if (language == AppLanguage.BN) doctor.hospitalBn else doctor.hospitalEn,
            appointmentDate = appointmentDate,
            timeSlot = timeSlot,
            consultationMode = consultationMode,
            feeBdt = doctor.feeBdt.toLong(),
            paymentMethod = paymentMethod,
            paymentStatus = "PAID",
            bookingStatus = "CONFIRMED",
            bookingRef = refCode,
            reasonForVisit = reasonForVisit.trim(),
            attachedReportTitle = attachedReportTitle.trim()
        )
        viewModelScope.launch {
            repository.createAppointment(currentUserId, appointment).onSuccess {
                _snackbarMessage.value = "${stringsFor(language).appointmentBookedToast} ($refCode)"
                onBooked()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to book appointment."
            }
        }
    }

    fun cancelDoctorAppointment(appointment: DoctorAppointment, language: AppLanguage) {
        viewModelScope.launch {
            repository.cancelAppointment(currentUserId, appointment.id).onSuccess {
                _snackbarMessage.value = stringsFor(language).appointmentCancelledToast
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to cancel appointment."
            }
        }
    }

    // --- Feature 1: Symptom Organizer ---

    fun resetSymptomSession(language: AppLanguage) {
        _symptomMessages.value = listOf(
            SymptomChatMessage(
                id = UUID.randomUUID().toString(),
                fromUser = false,
                text = stringsFor(language).symptomIntroAi,
                timeLabel = currentTimeLabel()
            )
        )
        _briefState.value = UiState.Idle
        _symptomError.value = null
    }

    fun sendSymptomMessage(text: String, language: AppLanguage) {
        val cleaned = text.trim()
        if (cleaned.isBlank() || _symptomChatLoading.value) return
        val userMsg = SymptomChatMessage(
            id = UUID.randomUUID().toString(),
            fromUser = true,
            text = cleaned,
            timeLabel = currentTimeLabel()
        )
        val updatedHistory = _symptomMessages.value + userMsg
        _symptomMessages.value = updatedHistory
        _symptomError.value = null
        _symptomChatLoading.value = true

        viewModelScope.launch {
            val result = aiService.sendSymptomFollowUp(updatedHistory, language)
            _symptomChatLoading.value = false
            result.onSuccess { reply ->
                _symptomMessages.value = _symptomMessages.value + SymptomChatMessage(
                    id = UUID.randomUUID().toString(),
                    fromUser = false,
                    text = reply,
                    timeLabel = currentTimeLabel()
                )
            }.onFailure { error ->
                _symptomError.value = error.localizedMessage ?: "AI follow-up failed. Please retry."
            }
        }
    }

    fun generateAndSaveDoctorBrief(language: AppLanguage) {
        val history = _symptomMessages.value
        if (history.none { it.fromUser }) return
        viewModelScope.launch {
            _briefState.value = UiState.Loading
            val result = aiService.generateDoctorBrief(history, language)
            result.onSuccess { brief ->
                _briefState.value = UiState.Success(brief)
                repository.saveDoctorBrief(currentUserId, brief).onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Generated brief, but failed to save to cloud."
                }
            }.onFailure { error ->
                _briefState.value = UiState.Error(error.localizedMessage ?: "Failed to generate doctor brief.")
            }
        }
    }

    fun uploadSymptomAttachment(
        bytes: ByteArray?,
        bitmap: Bitmap?,
        mimeType: String,
        fileName: String,
        attachmentType: String, // IMAGE, FILE, AUDIO
        saveToReportsVault: Boolean,
        language: AppLanguage
    ) {
        if (_symptomAttachmentLoading.value || _symptomChatLoading.value) return
        val safeName = fileName.trim().ifBlank {
            when (attachmentType) {
                "IMAGE" -> "symptom_image.jpg"
                "AUDIO" -> "voice_symptom.m4a"
                else -> "medical_document.pdf"
            }
        }
        val userText = if (language == AppLanguage.BN) {
            "সংযুক্তি আপলোড করা হয়েছে: $safeName"
        } else {
            "Uploaded attachment: $safeName"
        }
        val userMsg = SymptomChatMessage(
            id = UUID.randomUUID().toString(),
            fromUser = true,
            text = userText,
            timeLabel = currentTimeLabel(),
            attachmentBadge = "$attachmentType • $safeName"
        )
        _symptomMessages.value = _symptomMessages.value + userMsg
        _symptomError.value = null
        _symptomAttachmentLoading.value = true

        viewModelScope.launch {
            val result = aiService.analyzeSymptomAttachment(
                bytes = bytes,
                bitmap = bitmap,
                mimeType = mimeType,
                fileName = safeName,
                attachmentType = attachmentType,
                language = language
            )
            _symptomAttachmentLoading.value = false
            result.onSuccess { analysis ->
                val aiBubbleText = buildString {
                    append(analysis.followUpReply)
                    if (analysis.summary.isNotBlank()) {
                        append("\n\n")
                        append(if (language == AppLanguage.BN) "নোট: " else "Extracted note: ")
                        append(analysis.summary)
                    }
                }
                _symptomMessages.value = _symptomMessages.value + SymptomChatMessage(
                    id = UUID.randomUUID().toString(),
                    fromUser = false,
                    text = aiBubbleText,
                    timeLabel = currentTimeLabel(),
                    attachmentBadge = if (saveToReportsVault) {
                        if (language == AppLanguage.BN) "রিপোর্ট ভল্টে সংরক্ষিত" else "Saved to Medical Reports"
                    } else ""
                )
                if (saveToReportsVault) {
                    addMedicalReport(
                        title = analysis.title.ifBlank { safeName },
                        category = analysis.category,
                        doctorOrLab = if (language == AppLanguage.BN) "উপসর্গ আপলোড ($attachmentType)" else "Symptom Upload ($attachmentType)",
                        reportDate = repository.todayDateKey(),
                        summary = analysis.summary,
                        details = analysis.details.ifBlank { analysis.summary },
                        language = language,
                        attachmentType = attachmentType,
                        attachmentName = safeName
                    )
                }
            }.onFailure { err ->
                _symptomError.value = err.localizedMessage ?: "Failed to analyze attachment."
            }
        }
    }

    // --- Feature 2: AI Prescription Reader ---

    fun analyzePrescriptionBitmap(bitmap: Bitmap, language: AppLanguage) {
        _rxPreviewBitmap.value = bitmap
        rxAnalysisJob?.cancel()
        rxAnalysisJob = viewModelScope.launch {
            _rxState.value = UiState.Loading
            val result = aiService.analyzePrescription(bitmap, language)
            result.onSuccess { analysis ->
                _rxState.value = UiState.Success(analysis)
            }.onFailure { error ->
                _rxState.value = UiState.Error(error.localizedMessage ?: "Could not analyze prescription.")
            }
        }
    }

    fun updateExtractedMedication(updated: ExtractedMedication) {
        val current = (_rxState.value as? UiState.Success)?.data ?: return
        val nextList = current.medicines.map {
            if (it.localId == updated.localId) updated else it
        }
        _rxState.value = UiState.Success(current.copy(medicines = nextList))
    }

    fun removeExtractedMedication(localId: String) {
        val current = (_rxState.value as? UiState.Success)?.data ?: return
        val nextList = current.medicines.filterNot { it.localId == localId }
        _rxState.value = UiState.Success(current.copy(medicines = nextList))
    }

    fun autoScheduleExtractedMedications(language: AppLanguage, onScheduled: () -> Unit) {
        val current = (_rxState.value as? UiState.Success)?.data ?: return
        if (current.medicines.isEmpty()) return
        val today = repository.todayDateKey()
        val remindersToCreate = buildList {
            current.medicines.forEach { med ->
                val slots = mutableListOf<Pair<String, String>>()
                if (med.morning) slots.add("MORNING" to "08:30 AM")
                if (med.noon) slots.add("NOON" to "01:30 PM")
                if (med.night) slots.add("NIGHT" to "09:30 PM")
                if (slots.isEmpty()) slots.add("MORNING" to "08:30 AM")

                slots.forEach { (slot, time) ->
                    add(
                        MedicineReminder(
                            userId = currentUserId,
                            medicineName = med.name,
                            dosage = med.dosage,
                            timeSlot = slot,
                            scheduledTime = time,
                            mealTiming = med.mealTiming,
                            durationDays = med.durationDays.toLong(),
                            sourceType = "AUTO_RX",
                            active = true,
                            todayStatus = "PENDING",
                            lastActionDate = today,
                            notes = med.specialNotes,
                            ringtoneName = stringsFor(language).defaultRingtoneName
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            val res = repository.addRemindersBatch(currentUserId, remindersToCreate)
            res.onSuccess {
                _snackbarMessage.value = stringsFor(language).autoScheduleSuccess
                onScheduled()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to schedule reminders."
            }
        }
    }

    // --- Feature 3: Medicine Reminders ---

    fun addManualReminder(
        name: String,
        dosage: String,
        scheduledTimes: List<String>,
        mealTiming: String,
        durationDays: Int,
        notes: String,
        ringtoneUri: String = "",
        ringtoneName: String = "",
        language: AppLanguage
    ) {
        val today = repository.todayDateKey()
        val normalizedTimes = scheduledTimes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("08:30 AM") }

        val list = normalizedTimes.map { formattedTime ->
            MedicineReminder(
                userId = currentUserId,
                medicineName = name.trim(),
                dosage = dosage.trim().ifBlank { "1 Tablet" },
                timeSlot = inferTimeSlotFromClock(formattedTime),
                scheduledTime = formattedTime,
                mealTiming = mealTiming,
                durationDays = durationDays.coerceIn(1, 365).toLong(),
                sourceType = "MANUAL",
                active = true,
                todayStatus = "PENDING",
                lastActionDate = today,
                notes = notes.trim(),
                ringtoneUri = ringtoneUri.trim(),
                ringtoneName = ringtoneName.trim().ifBlank { stringsFor(language).defaultRingtoneName }
            )
        }
        viewModelScope.launch {
            val res = repository.addRemindersBatch(currentUserId, list)
            res.onSuccess {
                _snackbarMessage.value = stringsFor(language).reminderSavedToast
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to save reminder."
            }
        }
    }

    fun updateReminder(
        existing: MedicineReminder,
        name: String,
        dosage: String,
        scheduledTime: String,
        mealTiming: String,
        durationDays: Int,
        notes: String,
        ringtoneUri: String = existing.ringtoneUri,
        ringtoneName: String = existing.ringtoneName,
        language: AppLanguage
    ) {
        val updated = existing.copy(
            medicineName = name.trim().ifBlank { existing.medicineName },
            dosage = dosage.trim().ifBlank { existing.dosage },
            timeSlot = inferTimeSlotFromClock(scheduledTime),
            scheduledTime = scheduledTime.trim().ifBlank { existing.scheduledTime },
            mealTiming = mealTiming,
            durationDays = durationDays.coerceIn(1, 365).toLong(),
            notes = notes.trim(),
            ringtoneUri = ringtoneUri.trim(),
            ringtoneName = ringtoneName.trim().ifBlank { stringsFor(language).defaultRingtoneName }
        )
        viewModelScope.launch {
            repository.updateReminder(currentUserId, updated).onSuccess {
                _snackbarMessage.value = stringsFor(language).reminderUpdatedToast
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to update reminder."
            }
        }
    }

    private fun inferTimeSlotFromClock(timeLabel: String): String {
        val cleaned = timeLabel.trim().uppercase(Locale.US)
        val match = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""").matchEntire(cleaned) ?: return "MORNING"
        val rawHour = match.groupValues[1].toIntOrNull() ?: return "MORNING"
        val amPm = match.groupValues[3]
        val hour24 = when {
            amPm == "AM" && rawHour == 12 -> 0
            amPm == "PM" && rawHour != 12 -> rawHour + 12
            else -> rawHour
        }
        return when (hour24) {
            in 4..11 -> "MORNING"
            in 12..17 -> "NOON"
            else -> "NIGHT"
        }
    }

    fun toggleReminder(reminder: MedicineReminder, active: Boolean) {
        viewModelScope.launch {
            repository.toggleReminderActive(currentUserId, reminder.id, active).onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to update reminder."
            }
        }
    }

    fun markReminderTaken(reminder: MedicineReminder) {
        AlarmSoundPlayer.stop()
        if (reminder.id == "demo_alarm") {
            _activeAlarmReminder.value = null
            _snackbarMessage.value = "Demo dose marked as Taken!"
            return
        }
        viewModelScope.launch {
            repository.markReminderStatus(currentUserId, reminder, "TAKEN").onSuccess {
                if (_activeAlarmReminder.value?.id == reminder.id) {
                    _activeAlarmReminder.value = null
                }
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to log dose."
            }
        }
    }

    fun snoozeReminder10Min(reminder: MedicineReminder) {
        AlarmSoundPlayer.stop()
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }
        val snoozedTime = SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
        MedicineAlarmScheduler.scheduleSnooze10Min(
            context = appContext,
            reminderId = reminder.id.ifBlank { "med_snooze" },
            medicineName = reminder.medicineName,
            dosage = reminder.dosage,
            mealTiming = reminder.mealTiming,
            scheduledTime = snoozedTime,
            ringtoneUri = reminder.ringtoneUri
        )
        if (reminder.id == "demo_alarm") {
            _activeAlarmReminder.value = null
            _snackbarMessage.value = "Demo reminder snoozed to $snoozedTime"
            return
        }
        viewModelScope.launch {
            repository.markReminderStatus(
                userId = currentUserId,
                reminder = reminder,
                action = "SNOOZED",
                newScheduledTime = snoozedTime
            ).onSuccess {
                if (_activeAlarmReminder.value?.id == reminder.id) {
                    _activeAlarmReminder.value = null
                }
                _snackbarMessage.value = "${reminder.medicineName} snoozed to $snoozedTime"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to snooze reminder."
            }
        }
    }

    fun skipReminder(reminder: MedicineReminder) {
        viewModelScope.launch {
            repository.markReminderStatus(currentUserId, reminder, "SKIPPED").onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to skip reminder."
            }
        }
    }

    fun deleteReminder(reminder: MedicineReminder) {
        viewModelScope.launch {
            repository.deleteReminder(currentUserId, reminder.id).onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to delete reminder."
            }
        }
    }

    // --- Feature 4: Local Food Diet Planner ---

    fun generateLocalDietPlan(
        age: Int,
        conditions: List<String>,
        preference: String,
        language: AppLanguage
    ) {
        viewModelScope.launch {
            _dietGenState.value = UiState.Loading
            val res = aiService.generateDietPlan(age, conditions, preference, language)
            res.onSuccess { plan ->
                _dietGenState.value = UiState.Success(plan)
                repository.saveDietPlan(currentUserId, plan).onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Diet plan created, but cloud save failed."
                }
            }.onFailure { err ->
                _dietGenState.value = UiState.Error(err.localizedMessage ?: "Failed to generate diet plan.")
            }
        }
    }

    // --- Feature 5: Mental Wellness Journal & Mood Analyzer ---

    fun analyzeAndSaveMood(
        selectedMood: String,
        journalText: String,
        language: AppLanguage
    ) {
        val cleaned = journalText.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            _moodGenState.value = UiState.Loading
            val aiRes = aiService.analyzeMoodJournal(selectedMood, cleaned, language)
            aiRes.onSuccess { analysis ->
                val entry = MoodEntry(
                    userId = currentUserId,
                    language = language.code,
                    selectedMood = selectedMood,
                    journalText = cleaned,
                    detectedMoodTag = analysis.detectedMoodTag,
                    moodScore = analysis.moodScore.toLong(),
                    aiReflection = analysis.aiReflection,
                    microAction = analysis.microAction,
                    supportNote = analysis.supportNote,
                    dateLabel = repository.todayShortLabel()
                )
                _moodGenState.value = UiState.Success(entry)
                repository.saveMoodEntry(currentUserId, entry).onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Reflection ready, but cloud save failed."
                }
            }.onFailure { err ->
                _moodGenState.value = UiState.Error(err.localizedMessage ?: "Failed to analyze mood entry.")
            }
        }
    }

    // --- Feature 6: BMI Calculator, Hydration Tracker & AI Life Condition Summary ---

    private fun resolveTodayBaseMetrics(language: AppLanguage): HealthMetricsRecord {
        val records = (healthMetricsState.value as? UiState.Success)?.data.orEmpty()
        val todayKey = repository.todayDateKey()
        val todayRec = records.firstOrNull { it.dateKey == todayKey }
        if (todayRec != null) return todayRec
        val latest = records.firstOrNull()
        return HealthMetricsRecord(
            userId = currentUserId,
            dateKey = todayKey,
            heightCm = latest?.heightCm ?: 0.0,
            weightKg = latest?.weightKg ?: 0.0,
            bmiValue = latest?.bmiValue ?: 0.0,
            bmiCategory = latest?.bmiCategory.orEmpty(),
            waterIntakeMl = 0L,
            waterGoalMl = latest?.waterGoalMl ?: 2500L,
            lifeConditionTitle = latest?.lifeConditionTitle.orEmpty(),
            lifeConditionScore = latest?.lifeConditionScore ?: 0L,
            lifeConditionSummary = latest?.lifeConditionSummary.orEmpty(),
            lifeConditionTips = latest?.lifeConditionTips.orEmpty(),
            language = language.code
        )
    }

    fun saveBmiMeasurement(heightCm: Double, weightKg: Double, language: AppLanguage) {
        if (heightCm <= 30.0 || weightKg <= 2.0) return
        val heightM = heightCm / 100.0
        val rawBmi = weightKg / (heightM * heightM)
        val roundedBmi = kotlin.math.round(rawBmi * 10.0) / 10.0
        val category = when {
            roundedBmi < 18.5 -> "Underweight"
            roundedBmi < 25.0 -> "Normal"
            roundedBmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
        val base = resolveTodayBaseMetrics(language)
        val updated = base.copy(
            heightCm = kotlin.math.round(heightCm * 10.0) / 10.0,
            weightKg = kotlin.math.round(weightKg * 10.0) / 10.0,
            bmiValue = roundedBmi,
            bmiCategory = category,
            language = language.code
        )
        viewModelScope.launch {
            repository.upsertDailyHealthMetrics(currentUserId, updated).onSuccess {
                _snackbarMessage.value = stringsFor(language).bmiSavedToast
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save BMI."
            }
        }
    }

    fun updateWaterIntake(deltaMl: Int, newGoalMl: Int? = null, language: AppLanguage) {
        val base = resolveTodayBaseMetrics(language)
        val nextIntake = (base.waterIntakeMl + deltaMl.toLong()).coerceIn(0L, 15000L)
        val nextGoal = (newGoalMl?.toLong() ?: base.waterGoalMl).coerceIn(500L, 10000L)
        val updated = base.copy(
            waterIntakeMl = nextIntake,
            waterGoalMl = nextGoal,
            language = language.code
        )
        viewModelScope.launch {
            repository.upsertDailyHealthMetrics(currentUserId, updated).onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update hydration."
            }
        }
    }

    fun generateLifeConditionSummary(language: AppLanguage) {
        if (_lifeConditionLoading.value) return
        val base = resolveTodayBaseMetrics(language)
        val profile = (profileState.value as? UiState.Success)?.data
        val reminders = (remindersState.value as? UiState.Success)?.data.orEmpty()
        val activeReminders = reminders.filter { it.active }
        val takenCount = activeReminders.count { it.todayStatus == "TAKEN" }
        val moods = (moodEntriesState.value as? UiState.Success)?.data.orEmpty()
        val reports = (medicalReportsState.value as? UiState.Success)?.data.orEmpty()

        viewModelScope.launch {
            _lifeConditionLoading.value = true
            val result = aiService.generateLifeConditionSummary(
                profile = profile,
                latestMetrics = base,
                takenDoses = takenCount,
                totalDoses = activeReminders.size,
                recentMoods = moods,
                reportCount = reports.size,
                language = language
            )
            _lifeConditionLoading.value = false
            result.onSuccess { summary ->
                val updated = base.copy(
                    lifeConditionTitle = summary.title,
                    lifeConditionScore = summary.score.toLong(),
                    lifeConditionSummary = summary.summary,
                    lifeConditionTips = summary.tips,
                    language = language.code
                )
                repository.upsertDailyHealthMetrics(currentUserId, updated).onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Summary ready, but cloud save failed."
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to generate life condition summary."
            }
        }
    }

    private fun currentTimeLabel(): String =
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
}
