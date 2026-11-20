package com.example.i18n

import androidx.compose.runtime.Immutable

enum class AppLanguage(val code: String) {
    EN("EN"),
    BN("BN");

    fun toggle(): AppLanguage = if (this == EN) BN else EN

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

@Immutable
interface BilingualStrings {
    val appTitle: String
    val appTagline: String
    val getStarted: String
    val languageLabel: String
    val safetyBanner: String
    val welcomeBack: String
    val authSubtitle: String
    val continueWithGoogle: String
    val signingIn: String
    val authPrivacyNote: String
    val greetingMorning: String
    val greetingAfternoon: String
    val greetingEvening: String
    val nextMedicineTitle: String
    val noUpcomingMedicine: String
    val addFirstMedicineHint: String
    val markTaken: String
    val snooze10m: String
    val skip: String
    val quickActions: String
    val symptomOrganizer: String
    val symptomOrganizerSub: String
    val scanPrescription: String
    val scanPrescriptionSub: String
    val localDietPlan: String
    val localDietPlanSub: String
    val moodJournal: String
    val moodJournalSub: String
    val todayAdherence: String
    val latestMoodCheckIn: String
    val noMoodYet: String
    val tabHome: String
    val tabSymptoms: String
    val tabScanRx: String
    val tabReminders: String
    val tabWellness: String

    // Symptom Organizer
    val symptomHeader: String
    val symptomIntroAi: String
    val symptomInputHint: String
    val generateBriefBtn: String
    val generatingBrief: String
    val doctorBriefTitle: String
    val chiefSymptomsLabel: String
    val severityContextLabel: String
    val questionsForDoctorLabel: String
    val redFlagLabel: String
    val copyBrief: String
    val shareBrief: String
    val startNewCheck: String
    val briefCopiedToast: String
    val pastBriefsTitle: String

    // Prescription Reader
    val rxHeader: String
    val rxDropzoneTitle: String
    val rxDropzoneSubtitle: String
    val choosePhotoBtn: String
    val takePhotoBtn: String
    val trySampleRxBtn: String
    val analyzingRx: String
    val lowConfidenceWarning: String
    val activeMedicationsTitle: String
    val autoScheduleAllBtn: String
    val autoScheduleSuccess: String
    val editMedicineTitle: String
    val saveChanges: String
    val cancel: String

    // Reminders
    val remindersHeader: String
    val testAlarmNow: String
    val filterAll: String
    val filterMorning: String
    val filterNoon: String
    val filterNight: String
    val addManualMedicine: String
    val medicineNameLabel: String
    val dosageLabel: String
    val mealTimingLabel: String
    val durationDaysLabel: String
    val notesLabel: String
    val saveMedicineBtn: String
    val autoRxTag: String
    val manualTag: String
    val statusTaken: String
    val statusSnoozed: String
    val statusSkipped: String
    val statusPending: String
    val alarmModalTitle: String
    val alarmModalSubtitle: String
    val dismiss: String

    // Diet Planner
    val dietTabLabel: String
    val moodTabLabel: String
    val ageLabel: String
    val healthConditionsLabel: String
    val dietPreferenceLabel: String
    val generateDietBtn: String
    val regeneratePlanBtn: String
    val generatingDiet: String
    val breakfastLabel: String
    val lunchLabel: String
    val snackLabel: String
    val dinnerLabel: String
    val foodsToPrioritize: String
    val foodsToLimit: String
    val hydrationTipTitle: String

    // Mood Journal
    val moodMeterTitle: String
    val diaryPromptHint: String
    val analyzeSaveBtn: String
    val analyzingMood: String
    val aiReflectionTitle: String
    val microActionTitle: String
    val breathingExerciseTitle: String
    val startBreathing: String
    val stopBreathing: String
    val inhaleText: String
    val holdText: String
    val exhaleText: String
    val weeklyMoodTrend: String
    val journalHistoryTitle: String
    val retryBtn: String
    val profileTitle: String
    val signOut: String
    val loadSampleData: String
    val sampleDataLoaded: String

    // CameraX Prescription Scanner & Permission
    val cameraPermissionTitle: String
    val cameraPermissionRationale: String
    val grantCameraPermission: String
    val openAppSettings: String
    val cameraAlignHint: String
    val cameraCaptureError: String
    val cameraUnavailableError: String
    val capturePhotoLabel: String

    // Manual Reminder Clock Setup & Editing
    val editReminderTitle: String
    val reminderClockTimesLabel: String
    val addDoseTimeBtn: String
    val setClockTimeTitle: String
    val tapTimeToChangeHint: String
    val quickSchedulePresetsLabel: String
    val presetOnceDaily: String
    val presetTwiceDaily: String
    val presetThriceDaily: String
    val switchToKeyboardInput: String
    val switchToClockDial: String
    val confirmTimeBtn: String
    val reminderUpdatedToast: String
    val reminderSavedToast: String

    // AI Command Bot (Home & Sheet)
    val aiBotCardTitle: String
    val aiBotCardSubtitle: String
    val aiBotOpenBtn: String
    val aiBotSheetTitle: String
    val aiBotWelcomeMsg: String
    val aiBotInputHint: String
    val aiBotThinking: String
    val aiBotResetChat: String

    // Serial Medical Reports Vault
    val medicalReportsTitle: String
    val medicalReportsSub: String
    val addReportBtn: String
    val scanReportPhotoBtn: String
    val scanningReportMsg: String
    val reportTitleLabel: String
    val reportCategoryLabel: String
    val reportDoctorLabLabel: String
    val reportDateLabel: String
    val reportSummaryLabel: String
    val reportDetailsLabel: String
    val saveReportBtn: String
    val reportSavedToast: String
    val noReportsYet: String
    val askAiAboutReportBtn: String
    val saveRxToReportsBtn: String

    // Ringtone & Background Alarm
    val alarmRingtoneLabel: String
    val defaultRingtoneName: String
    val chooseRingtoneBtn: String
    val previewRingtoneBtn: String
    val stopPreviewBtn: String
    val backgroundAlarmActiveNote: String
    val enableAlarmNotificationsTitle: String
    val enableAlarmNotificationsSub: String
    val allowNotificationsBtn: String

    // Editable User Profile
    val editProfileTitle: String
    val displayNameLabel: String
    val locationLabel: String
    val emergencyNumberLabel: String
    val callEmergencyBtn: String
    val saveProfileBtn: String
    val profileUpdatedToast: String

    // Paid Professional Doctor Appointments
    val proDoctorsTitle: String
    val proDoctorsSub: String
    val paidBadgeLabel: String
    val findDoctorsTab: String
    val myAppointmentsTab: String
    val consultationFeeLabel: String
    val bookAppointmentBtn: String
    val checkoutTitle: String
    val selectDateLabel: String
    val selectTimeSlotLabel: String
    val consultationModeLabel: String
    val attachReportLabel: String
    val noReportAttached: String
    val reasonForVisitLabel: String
    val paymentMethodLabel: String
    val confirmAndPayBtn: String
    val appointmentBookedToast: String
    val noAppointmentsYet: String
    val cancelRefundBtn: String
    val appointmentCancelledToast: String

    // BMI, Hydration & AI Life Condition Summary
    val bmiHydrationTabLabel: String
    val bmiCardTitle: String
    val heightCmLabel: String
    val heightFeetLabel: String
    val heightInchesLabel: String
    val weightKgLabel: String
    val calculateSaveBmiBtn: String
    val bmiSavedToast: String
    val healthyRangeLabel: String
    val bmiUnderweight: String
    val bmiNormal: String
    val bmiOverweight: String
    val bmiObese: String
    val hydrationCardTitle: String
    val dailyGoalLabel: String
    val glassesLabel: String
    val waterUpdatedToast: String
    val lifeConditionCardTitle: String
    val lifeConditionCardSub: String
    val generateLifeSummaryBtn: String
    val refreshLifeSummaryBtn: String
    val generatingLifeSummary: String
    val wellnessScoreLabel: String
    val dailyRecommendationsLabel: String
    val dashboardBodyHydrationTitle: String

    // Symptom & Vault File / Image / Audio Uploads
    val attachMediaTitle: String
    val attachMediaSubtitle: String
    val attachImageOption: String
    val attachImageOptionSub: String
    val attachFileOption: String
    val attachFileOptionSub: String
    val attachAudioOption: String
    val attachAudioOptionSub: String
    val recordVoiceOption: String
    val recordingVoiceNow: String
    val stopAndSendVoice: String
    val alsoSaveToReportsToggle: String
    val analyzingAttachmentMsg: String
    val attachmentSavedToVaultToast: String
    val uploadFromDriveFilesBtn: String
    val fileTooLargeError: String
    val micPermissionDeniedError: String

    // Data Dashboard & Enhanced Report Export
    val dataDashboardTitle: String
    val dataDashboardSub: String
    val adherenceRateLabel: String
    val avgHydrationLabel: String
    val latestBmiLabel: String
    val avgMoodScoreLabel: String
    val adherenceChartTitle: String
    val hydrationChartTitle: String
    val moodTrendChartTitle: String
    val vaultOverviewTitle: String
    val exportReportTitle: String
    val exportReportSub: String
    val savePdfToDeviceBtn: String
    val sharePdfBtn: String
    val shareTextSummaryBtn: String
    val exportAllReportsPdfBtn: String
    val exportSingleReportBtn: String
    val pdfExportSuccessToast: String
    val pdfExportErrorToast: String
}

private object EnglishStrings : BilingualStrings {
    override val appTitle = "ShasthoSathi AI"
    override val appTagline = "Your AI Health Companion\nআপনার স্মার্ট স্বাস্থ্যসাথী"
    override val getStarted = "Get Started"
    override val languageLabel = "Language / ভাষা"
    override val safetyBanner = "Guidance only — Not a medical diagnosis. Always consult a licensed doctor."
    override val welcomeBack = "Welcome to ShasthoSathi"
    override val authSubtitle = "Sign in with Google to sync your medicine schedule, doctor briefs, local diet plans, and wellness journal securely across devices."
    override val continueWithGoogle = "Continue with Google"
    override val signingIn = "Signing in..."
    override val authPrivacyNote = "Your health data stays private and encrypted in your personal cloud vault."
    override val greetingMorning = "Good Morning"
    override val greetingAfternoon = "Good Afternoon"
    override val greetingEvening = "Good Evening"
    override val nextMedicineTitle = "Next Medicine Reminder"
    override val noUpcomingMedicine = "All caught up for today!"
    override val addFirstMedicineHint = "Scan a prescription or add a medicine to set smart reminders."
    override val markTaken = "Mark Taken"
    override val snooze10m = "Snooze 10m"
    override val skip = "Skip"
    override val quickActions = "Quick Actions"
    override val symptomOrganizer = "Symptom Organizer"
    override val symptomOrganizerSub = "Prep for Doctor"
    override val scanPrescription = "Scan Prescription"
    override val scanPrescriptionSub = "AI Rx Reader"
    override val localDietPlan = "Local Diet Plan"
    override val localDietPlanSub = "Bangladeshi Meals"
    override val moodJournal = "Mood Journal"
    override val moodJournalSub = "Mind & Breathing"
    override val todayAdherence = "Today's Medication Adherence"
    override val latestMoodCheckIn = "Latest Mood Check-in"
    override val noMoodYet = "Share how you feel today in Wellness to track your mood trend."
    override val tabHome = "Home"
    override val tabSymptoms = "Symptoms"
    override val tabScanRx = "Scan Rx"
    override val tabReminders = "Reminders"
    override val tabWellness = "Wellness"
    override val symptomHeader = "AI Symptom Organizer"
    override val symptomIntroAi = "Hello! I'm ShasthoSathi. Tell me what symptoms you're experiencing so I can help organize a clear brief for your doctor. (I do not diagnose conditions.)"
    override val symptomInputHint = "Describe your symptom..."
    override val generateBriefBtn = "Generate Doctor Brief"
    override val generatingBrief = "Organizing your doctor brief..."
    override val doctorBriefTitle = "Structured Doctor Brief"
    override val chiefSymptomsLabel = "Chief Symptoms & Duration"
    override val severityContextLabel = "Severity & Key Context"
    override val questionsForDoctorLabel = "Questions to Ask Your Doctor"
    override val redFlagLabel = "Red-Flag Warning"
    override val copyBrief = "Copy Brief"
    override val shareBrief = "Share / Print"
    override val startNewCheck = "New Check"
    override val briefCopiedToast = "Doctor brief copied to clipboard"
    override val pastBriefsTitle = "Saved Doctor Briefs"
    override val rxHeader = "AI Prescription Reader"
    override val rxDropzoneTitle = "Upload or Capture Prescription"
    override val rxDropzoneSubtitle = "Supports printed or handwritten Rx photos (JPEG/PNG). Always verify extracted doses."
    override val choosePhotoBtn = "Choose Photo"
    override val takePhotoBtn = "Camera"
    override val trySampleRxBtn = "Try Sample Prescription"
    override val analyzingRx = "Analyzing prescription with AI..."
    override val lowConfidenceWarning = "Some handwriting appears unclear — please verify manually before saving."
    override val activeMedicationsTitle = "Extracted Active Medications"
    override val autoScheduleAllBtn = "Auto-Schedule All Reminders"
    override val autoScheduleSuccess = "All medicines scheduled in Reminders!"
    override val editMedicineTitle = "Verify & Edit Medicine"
    override val saveChanges = "Save Changes"
    override val cancel = "Cancel"
    override val remindersHeader = "Medicine Scheduler"
    override val testAlarmNow = "Test Alarm"
    override val filterAll = "All"
    override val filterMorning = "Morning"
    override val filterNoon = "Noon"
    override val filterNight = "Night"
    override val addManualMedicine = "Add Manual Medicine"
    override val medicineNameLabel = "Medicine Name (e.g. Napa 500mg)"
    override val dosageLabel = "Dosage (e.g. 1 Tablet)"
    override val mealTimingLabel = "Meal Relation"
    override val durationDaysLabel = "Duration (Days)"
    override val notesLabel = "Special Notes (optional)"
    override val saveMedicineBtn = "Save Reminder"
    override val autoRxTag = "Auto Rx"
    override val manualTag = "Manual"
    override val statusTaken = "Taken"
    override val statusSnoozed = "Snoozed"
    override val statusSkipped = "Skipped"
    override val statusPending = "Pending"
    override val alarmModalTitle = "Time for Your Medicine"
    override val alarmModalSubtitle = "Please follow your doctor's meal timing instruction."
    override val dismiss = "Dismiss"
    override val dietTabLabel = "Local Diet"
    override val moodTabLabel = "Mood Journal"
    override val ageLabel = "Age"
    override val healthConditionsLabel = "Health Focus"
    override val dietPreferenceLabel = "Diet Style"
    override val generateDietBtn = "Generate Bangladeshi Diet Plan"
    override val regeneratePlanBtn = "Regenerate Plan"
    override val generatingDiet = "Crafting your Bangladeshi meal plan..."
    override val breakfastLabel = "Breakfast (সকাল)"
    override val lunchLabel = "Lunch (দুপুর)"
    override val snackLabel = "Evening Snack (বিকেল)"
    override val dinnerLabel = "Dinner (রাত)"
    override val foodsToPrioritize = "Foods to Prioritize"
    override val foodsToLimit = "Foods to Limit"
    override val hydrationTipTitle = "Daily Hydration Tip"
    override val moodMeterTitle = "How are you feeling today?"
    override val diaryPromptHint = "Write what's on your mind in English or Bangla..."
    override val analyzeSaveBtn = "Analyze & Save Entry"
    override val analyzingMood = "Reflecting with empathy..."
    override val aiReflectionTitle = "AI Mind Reflection"
    override val microActionTitle = "Gentle Micro-Action"
    override val breathingExerciseTitle = "Guided 4-7-8 Breathing"
    override val startBreathing = "Start 4-7-8 Timer"
    override val stopBreathing = "Stop Timer"
    override val inhaleText = "Inhale slowly..."
    override val holdText = "Hold gently..."
    override val exhaleText = "Exhale completely..."
    override val weeklyMoodTrend = "Mood Trend (Recent Entries)"
    override val journalHistoryTitle = "Journal History"
    override val retryBtn = "Try Again"
    override val profileTitle = "Health Profile"
    override val signOut = "Sign Out"
    override val loadSampleData = "Load Sample Health Data"
    override val sampleDataLoaded = "Sample reminders & mood check-ins added!"
    override val cameraPermissionTitle = "Camera Permission Required"
    override val cameraPermissionRationale = "Allow camera access so ShasthoSathi AI can capture crisp photos of your medical prescriptions directly inside the app."
    override val grantCameraPermission = "Allow Camera"
    override val openAppSettings = "Open Settings"
    override val cameraAlignHint = "Align your prescription neatly inside the teal frame"
    override val cameraCaptureError = "Could not capture photo. Please hold steady and try again."
    override val cameraUnavailableError = "No camera hardware was detected on this device. You can still upload a photo or try the sample prescription."
    override val capturePhotoLabel = "Capture Prescription"
    override val editReminderTitle = "Edit Medicine Reminder"
    override val reminderClockTimesLabel = "Alarm Clock Times"
    override val addDoseTimeBtn = "Add Time"
    override val setClockTimeTitle = "Set Reminder Clock"
    override val tapTimeToChangeHint = "Tap any clock time to customize hour, minute & AM/PM"
    override val quickSchedulePresetsLabel = "Quick Presets"
    override val presetOnceDaily = "1x / Day"
    override val presetTwiceDaily = "1+0+1"
    override val presetThriceDaily = "1+1+1"
    override val switchToKeyboardInput = "Type time"
    override val switchToClockDial = "Clock dial"
    override val confirmTimeBtn = "Set Time"
    override val reminderUpdatedToast = "Reminder schedule updated!"
    override val reminderSavedToast = "Manual medicine reminder saved!"
    override val aiBotCardTitle = "ShasthoSathi AI Command Bot"
    override val aiBotCardSubtitle = "Control every feature by chat & ask anytime about your saved medical reports."
    override val aiBotOpenBtn = "Chat & Control"
    override val aiBotSheetTitle = "AI Health Command Bot"
    override val aiBotWelcomeMsg = "Hi! I can control ShasthoSathi features for you (add/snooze reminders, open Paid Doctors, update profile, switch language) and answer questions about your saved medical reports anytime."
    override val aiBotInputHint = "Command or ask about your reports..."
    override val aiBotThinking = "Processing command..."
    override val aiBotResetChat = "New Chat"
    override val medicalReportsTitle = "My Medical Reports"
    override val medicalReportsSub = "Serial Cloud Vault"
    override val addReportBtn = "Add Report"
    override val scanReportPhotoBtn = "AI Scan Report"
    override val scanningReportMsg = "Reading report photo with AI..."
    override val reportTitleLabel = "Report Title (e.g. CBC Blood Test)"
    override val reportCategoryLabel = "Report Type"
    override val reportDoctorLabLabel = "Doctor / Diagnostic Lab"
    override val reportDateLabel = "Report Date (YYYY-MM-DD)"
    override val reportSummaryLabel = "Key Findings Summary"
    override val reportDetailsLabel = "Full Values & Notes"
    override val saveReportBtn = "Save to Vault"
    override val reportSavedToast = "Medical report saved serially!"
    override val noReportsYet = "No medical reports saved yet. Add manually or scan a lab report photo so you can review them serially and ask AI anytime."
    override val askAiAboutReportBtn = "Ask AI Bot"
    override val saveRxToReportsBtn = "Save Prescription to Reports Vault"
    override val alarmRingtoneLabel = "Alarm Ringtone"
    override val defaultRingtoneName = "Default Alarm Tone"
    override val chooseRingtoneBtn = "Choose Ringtone"
    override val previewRingtoneBtn = "Test Tone"
    override val stopPreviewBtn = "Stop"
    override val backgroundAlarmActiveNote = "Background alarm enabled — rings with your chosen tone even when the app is closed."
    override val enableAlarmNotificationsTitle = "Enable Background Alarm Alerts"
    override val enableAlarmNotificationsSub = "Allow notifications so medicine alarms ring and show Stop / Snooze buttons in the background."
    override val allowNotificationsBtn = "Allow Alerts"
    override val editProfileTitle = "Edit Health Profile"
    override val displayNameLabel = "Full Name"
    override val locationLabel = "Location (City / Area)"
    override val emergencyNumberLabel = "Emergency Contact Number"
    override val callEmergencyBtn = "Call Emergency"
    override val saveProfileBtn = "Save Profile"
    override val profileUpdatedToast = "Health profile updated!"
    override val proDoctorsTitle = "Pro Doctors"
    override val proDoctorsSub = "Paid Online Visit"
    override val paidBadgeLabel = "PAID"
    override val findDoctorsTab = "Specialists"
    override val myAppointmentsTab = "My Bookings"
    override val consultationFeeLabel = "Consultation Fee"
    override val bookAppointmentBtn = "Book Online Visit"
    override val checkoutTitle = "Paid Online Appointment"
    override val selectDateLabel = "Select Date"
    override val selectTimeSlotLabel = "Select Time Slot"
    override val consultationModeLabel = "Consultation Mode"
    override val attachReportLabel = "Attach Medical Report (Optional)"
    override val noReportAttached = "None"
    override val reasonForVisitLabel = "Reason / Symptoms for Doctor"
    override val paymentMethodLabel = "Select Payment Method"
    override val confirmAndPayBtn = "Pay & Confirm Booking"
    override val appointmentBookedToast = "Paid online appointment confirmed!"
    override val noAppointmentsYet = "No online doctor appointments booked yet. Browse verified specialists to schedule a paid video, audio, or chat consultation."
    override val cancelRefundBtn = "Cancel & Refund"
    override val appointmentCancelledToast = "Appointment cancelled & marked refunded."
    override val bmiHydrationTabLabel = "BMI & Water"
    override val bmiCardTitle = "BMI Calculator"
    override val heightCmLabel = "Height (cm)"
    override val heightFeetLabel = "Feet"
    override val heightInchesLabel = "Inches"
    override val weightKgLabel = "Weight (kg)"
    override val calculateSaveBmiBtn = "Calculate & Save BMI"
    override val bmiSavedToast = "BMI saved to your daily health log!"
    override val healthyRangeLabel = "Healthy BMI range: 18.5 – 24.9"
    override val bmiUnderweight = "Underweight"
    override val bmiNormal = "Healthy Weight"
    override val bmiOverweight = "Overweight"
    override val bmiObese = "Obese"
    override val hydrationCardTitle = "Daily Hydration Tracker"
    override val dailyGoalLabel = "Daily Goal"
    override val glassesLabel = "glasses"
    override val waterUpdatedToast = "Water intake updated!"
    override val lifeConditionCardTitle = "My Life Condition Summary"
    override val lifeConditionCardSub = "AI wellness overview from your BMI, hydration, adherence & mood."
    override val generateLifeSummaryBtn = "Generate Life Condition Summary"
    override val refreshLifeSummaryBtn = "Refresh AI Summary"
    override val generatingLifeSummary = "Synthesizing your life condition summary..."
    override val wellnessScoreLabel = "Wellness Score"
    override val dailyRecommendationsLabel = "Daily Action Tips"
    override val dashboardBodyHydrationTitle = "BMI, Hydration & Life Condition"
    override val attachMediaTitle = "Attach File, Image or Audio"
    override val attachMediaSubtitle = "Pick from Local File Manager, Google Drive, Downloads, Gallery, or record a voice note."
    override val attachImageOption = "Upload Image"
    override val attachImageOptionSub = "Gallery, Local Files & Google Drive"
    override val attachFileOption = "Upload Document / File"
    override val attachFileOptionSub = "PDF, TXT, DOC from File Manager & Google Drive"
    override val attachAudioOption = "Upload Audio File"
    override val attachAudioOptionSub = "Voice memo or MP3/M4A from Drive & Local Files"
    override val recordVoiceOption = "Record Voice Symptom"
    override val recordingVoiceNow = "Recording voice note..."
    override val stopAndSendVoice = "Stop & Analyze"
    override val alsoSaveToReportsToggle = "Also upload to My Medical Reports Vault"
    override val analyzingAttachmentMsg = "Analyzing uploaded attachment with AI..."
    override val attachmentSavedToVaultToast = "Analyzed & saved to Medical Reports Vault!"
    override val uploadFromDriveFilesBtn = "Upload File / Drive / Audio"
    override val fileTooLargeError = "File is too large (max 8 MB). Please choose a smaller file."
    override val micPermissionDeniedError = "Microphone permission is needed to record voice symptoms."
    override val dataDashboardTitle = "Data Dashboard"
    override val dataDashboardSub = "Analytics & Export"
    override val adherenceRateLabel = "Adherence"
    override val avgHydrationLabel = "7d Avg Water"
    override val latestBmiLabel = "Latest BMI"
    override val avgMoodScoreLabel = "Avg Mood"
    override val adherenceChartTitle = "Medication Adherence (Last 7 Days)"
    override val hydrationChartTitle = "Hydration Trend (Last 7 Days)"
    override val moodTrendChartTitle = "Mood Score Trend"
    override val vaultOverviewTitle = "Health Vault & Care Summary"
    override val exportReportTitle = "Enhanced Report Export"
    override val exportReportSub = "Export a clean printable PDF or shareable health summary to Files, Google Drive, Email, or Doctor."
    override val savePdfToDeviceBtn = "Save PDF (Files / Drive)"
    override val sharePdfBtn = "Share / Print PDF"
    override val shareTextSummaryBtn = "Share Text"
    override val exportAllReportsPdfBtn = "Export All Reports (PDF)"
    override val exportSingleReportBtn = "Export PDF"
    override val pdfExportSuccessToast = "PDF Health Report saved successfully!"
    override val pdfExportErrorToast = "Could not export PDF report. Please try again."
}

private object BanglaStrings : BilingualStrings {
    override val appTitle = "স্বাস্থ্যসাথী এআই"
    override val appTagline = "আপনার স্মার্ট স্বাস্থ্যসাথী\nYour AI Health Companion"
    override val getStarted = "শুরু করুন"
    override val languageLabel = "ভাষা / Language"
    override val safetyBanner = "শুধুমাত্র নির্দেশনা — এটি কোনো রোগ নির্ণয় নয়। সর্বদা নিবন্ধিত ডাক্তারের পরামর্শ নিন।"
    override val welcomeBack = "স্বাস্থ্যসাথী-তে স্বাগতম"
    override val authSubtitle = "আপনার ওষুধের রিমাইন্ডার, ডাক্তারের ব্রিফ, দেশি ডায়েট প্ল্যান ও মন-জার্নাল নিরাপদে সংরক্ষণ করতে গুগল দিয়ে সাইন ইন করুন।"
    override val continueWithGoogle = "গুগল দিয়ে চালিয়ে যান"
    override val signingIn = "সাইন ইন হচ্ছে..."
    override val authPrivacyNote = "আপনার স্বাস্থ্য তথ্য আপনার ব্যক্তিগত ক্লাউড অ্যাকাউন্টে সম্পূর্ণ সুরক্ষিত থাকে।"
    override val greetingMorning = "শুভ সকাল"
    override val greetingAfternoon = "শুভ দুপুর"
    override val greetingEvening = "শুভ সন্ধ্যা"
    override val nextMedicineTitle = "পরবর্তী ওষুধের রিমাইন্ডার"
    override val noUpcomingMedicine = "আজকের সব ওষুধ নেওয়া সম্পন্ন!"
    override val addFirstMedicineHint = "রিমাইন্ডার সেট করতে প্রেসক্রিপশন স্ক্যান করুন অথবা ম্যানুয়ালি ওষুধ যোগ করুন।"
    override val markTaken = "খেয়েছি"
    override val snooze10m = "১০ মি. পরে"
    override val skip = "বাদ দিন"
    override val quickActions = "দ্রুত সেবা"
    override val symptomOrganizer = "উপসর্গ গোছানো"
    override val symptomOrganizerSub = "ডাক্তারের প্রস্তুতি"
    override val scanPrescription = "প্রেসক্রিপশন স্ক্যান"
    override val scanPrescriptionSub = "এআই প্রেসক্রিপশন রিডার"
    override val localDietPlan = "দেশি ডায়েট প্ল্যান"
    override val localDietPlanSub = "বাংলাদেশি খাবার"
    override val moodJournal = "মুড জার্নাল"
    override val moodJournalSub = "মানসিক প্রশান্তি"
    override val todayAdherence = "আজকের ওষুধ গ্রহণের অগ্রগতি"
    override val latestMoodCheckIn = "সর্বশেষ মুড চেক-ইন"
    override val noMoodYet = "আপনার মনের অবস্থা জানাতে ওয়েলনেস ট্যাবে একটি এন্ট্রি লিখুন।"
    override val tabHome = "হোম"
    override val tabSymptoms = "উপসর্গ"
    override val tabScanRx = "স্ক্যান Rx"
    override val tabReminders = "রিমাইন্ডার"
    override val tabWellness = "ওয়েলনেস"
    override val symptomHeader = "এআই উপসর্গ অর্গানাইজার"
    override val symptomIntroAi = "নমস্কার / আসসালামু আলাইকুম! আমি স্বাস্থ্যসাথী। আপনার কী কী উপসর্গ হচ্ছে তা বলুন, আমি ডাক্তারের জন্য একটি গুছানো ব্রিফ তৈরি করে দেবো। (আমি রোগ নির্ণয় করি না।)"
    override val symptomInputHint = "আপনার উপসর্গ লিখুন..."
    override val generateBriefBtn = "ডাক্তারের ব্রিফ তৈরি করুন"
    override val generatingBrief = "ডাক্তারের জন্য ব্রিফ সাজানো হচ্ছে..."
    override val doctorBriefTitle = "ডাক্তারের জন্য গুছানো ব্রিফ"
    override val chiefSymptomsLabel = "প্রধান উপসর্গ ও সময়কাল"
    override val severityContextLabel = "তীব্রতা ও প্রাসঙ্গিক তথ্য"
    override val questionsForDoctorLabel = "ডাক্তারকে জিজ্ঞাসা করার প্রশ্নসমূহ"
    override val redFlagLabel = "জরুরি সতর্কবার্তা"
    override val copyBrief = "কপি করুন"
    override val shareBrief = "শেয়ার / প্রিন্ট"
    override val startNewCheck = "নতুন চেক"
    override val briefCopiedToast = "ডাক্তারের ব্রিফ ক্লিপবোর্ডে কপি হয়েছে"
    override val pastBriefsTitle = "পূর্ববর্তী সংরক্ষিত ব্রিফ"
    override val rxHeader = "এআই প্রেসক্রিপশন রিডার"
    override val rxDropzoneTitle = "প্রেসক্রিপশনের ছবি আপলোড বা তুলুন"
    override val rxDropzoneSubtitle = "ছাপানো বা হাতে লেখা প্রেসক্রিপশন (JPEG/PNG)। সংরক্ষণের আগে ডোজ মিলিয়ে নিন।"
    override val choosePhotoBtn = "গ্যালারি থেকে"
    override val takePhotoBtn = "ক্যামেরা"
    override val trySampleRxBtn = "নমুনা প্রেসক্রিপশন দেখুন"
    override val analyzingRx = "এআই দিয়ে প্রেসক্রিপশন বিশ্লেষণ হচ্ছে..."
    override val lowConfidenceWarning = "কিছু হাতের লেখা অস্পষ্ট মনে হচ্ছে — সংরক্ষণের আগে নিজে যাচাই করে নিন।"
    override val activeMedicationsTitle = "শনাক্তকৃত ওষুধের তালিকা"
    override val autoScheduleAllBtn = "সব রিমাইন্ডার অটো-শিডিউল করুন"
    override val autoScheduleSuccess = "সব ওষুধ রিমাইন্ডারে যুক্ত হয়েছে!"
    override val editMedicineTitle = "ওষুধ যাচাই ও সম্পাদনা"
    override val saveChanges = "সংরক্ষণ করুন"
    override val cancel = "বাতিল"
    override val remindersHeader = "ওষুধের সময়সূচী"
    override val testAlarmNow = "টেস্ট অ্যালার্ম"
    override val filterAll = "সব"
    override val filterMorning = "সকাল"
    override val filterNoon = "দুপুর"
    override val filterNight = "রাত"
    override val addManualMedicine = "নতুন ওষুধ যোগ করুন"
    override val medicineNameLabel = "ওষুধের নাম (যেমন: Napa 500mg)"
    override val dosageLabel = "মাত্রা (যেমন: ১টি ট্যাবলেট)"
    override val mealTimingLabel = "খাবারের সম্পর্ক"
    override val durationDaysLabel = "মেয়াদ (দিন)"
    override val notesLabel = "বিশেষ নোট (ঐচ্ছিক)"
    override val saveMedicineBtn = "রিমাইন্ডার সংরক্ষণ করুন"
    override val autoRxTag = "অটো Rx"
    override val manualTag = "ম্যানুয়াল"
    override val statusTaken = "খাওয়া হয়েছে"
    override val statusSnoozed = "স্নুজড"
    override val statusSkipped = "বাদ দেওয়া"
    override val statusPending = "বাকি আছে"
    override val alarmModalTitle = "ওষুধ খাওয়ার সময় হয়েছে"
    override val alarmModalSubtitle = "ডাক্তারের নির্দেশনা অনুযায়ী খাবার আগে বা পরে ওষুধ খান।"
    override val dismiss = "বন্ধ করুন"
    override val dietTabLabel = "দেশি ডায়েট"
    override val moodTabLabel = "মুড জার্নাল"
    override val ageLabel = "বয়স"
    override val healthConditionsLabel = "স্বাস্থ্য অবস্থা"
    override val dietPreferenceLabel = "খাবারের ধরন"
    override val generateDietBtn = "বাংলাদেশি ডায়েট প্ল্যান তৈরি করুন"
    override val regeneratePlanBtn = "নতুন প্ল্যান তৈরি করুন"
    override val generatingDiet = "আপনার দেশি খাবার তালিকা তৈরি হচ্ছে..."
    override val breakfastLabel = "সকালের নাস্তা"
    override val lunchLabel = "দুপুরের খাবার"
    override val snackLabel = "বিকেলের নাস্তা"
    override val dinnerLabel = "রাতের খাবার"
    override val foodsToPrioritize = "যে খাবারগুলো খাবেন"
    override val foodsToLimit = "যে খাবারগুলো কম খাবেন"
    override val hydrationTipTitle = "পানি পানের পরামর্শ"
    override val moodMeterTitle = "আজ আপনার মন কেমন লাগছে?"
    override val diaryPromptHint = "বাংলা বা ইংরেজিতে আপনার মনের কথা লিখুন..."
    override val analyzeSaveBtn = "বিশ্লেষণ ও সংরক্ষণ করুন"
    override val analyzingMood = "সহানুভূতির সাথে বিশ্লেষণ হচ্ছে..."
    override val aiReflectionTitle = "এআই মানসিক প্রতিফলন"
    override val microActionTitle = "ছোট্ট প্রশান্তির কাজ"
    override val breathingExerciseTitle = "৪-৭-৮ শ্বাস-প্রশ্বাস ব্যায়াম"
    override val startBreathing = "৪-৭-৮ টাইমার শুরু করুন"
    override val stopBreathing = "টাইমার বন্ধ করুন"
    override val inhaleText = "ধীরে শ্বাস নিন..."
    override val holdText = "আলতো করে ধরে রাখুন..."
    override val exhaleText = "সম্পূর্ণ শ্বাস ছাড়ুন..."
    override val weeklyMoodTrend = "সাপ্তাহিক মুড গ্রাফ"
    override val journalHistoryTitle = "জার্নাল ইতিহাস"
    override val retryBtn = "আবার চেষ্টা করুন"
    override val profileTitle = "স্বাস্থ্য প্রোফাইল"
    override val signOut = "সাইন আউট"
    override val loadSampleData = "নমুনা স্বাস্থ্য তথ্য যোগ করুন"
    override val sampleDataLoaded = "নমুনা রিমাইন্ডার ও মুড এন্ট্রি যোগ হয়েছে!"
    override val cameraPermissionTitle = "ক্যামেরার অনুমতি প্রয়োজন"
    override val cameraPermissionRationale = "অ্যাপের ভেতর থেকে সরাসরি প্রেসক্রিপশনের স্পষ্ট ছবি তোলার জন্য ক্যামেরা ব্যবহারের অনুমতি দিন।"
    override val grantCameraPermission = "ক্যামেরার অনুমতি দিন"
    override val openAppSettings = "সেটিংস খুলুন"
    override val cameraAlignHint = "প্রেসক্রিপশনটি সবুজ ফ্রেমের ভেতরে সোজা করে ধরুন"
    override val cameraCaptureError = "ছবি তোলা যায়নি। ফোনটি স্থির রেখে আবার চেষ্টা করুন।"
    override val cameraUnavailableError = "এই ডিভাইসে ক্যামেরা পাওয়া যায়নি। আপনি গ্যালারি থেকে ছবি বা নমুনা প্রেসক্রিপশন ব্যবহার করতে পারেন।"
    override val capturePhotoLabel = "প্রেসক্রিপশনের ছবি তুলুন"
    override val editReminderTitle = "রিমাইন্ডার সম্পাদনা করুন"
    override val reminderClockTimesLabel = "অ্যালার্মের সময় (ঘড়ি)"
    override val addDoseTimeBtn = "সময় যোগ করুন"
    override val setClockTimeTitle = "রিমাইন্ডার ঘড়ি সেট করুন"
    override val tapTimeToChangeHint = "ঘণ্টা, মিনিট ও AM/PM পরিবর্তন করতে সময়ের উপর ট্যাপ করুন"
    override val quickSchedulePresetsLabel = "দ্রুত সময়সূচী"
    override val presetOnceDaily = "দিনে ১ বার"
    override val presetTwiceDaily = "১+০+১"
    override val presetThriceDaily = "১+১+১"
    override val switchToKeyboardInput = "কীবোর্ডে লিখুন"
    override val switchToClockDial = "ঘড়ির ডায়াল"
    override val confirmTimeBtn = "সময় সেট করুন"
    override val reminderUpdatedToast = "রিমাইন্ডারের সময় আপডেট হয়েছে!"
    override val reminderSavedToast = "ম্যানুয়াল ওষুধের রিমাইন্ডার সংরক্ষিত হয়েছে!"
    override val aiBotCardTitle = "স্বাস্থ্যসাথী এআই কমান্ড বট"
    override val aiBotCardSubtitle = "চ্যাট করে সব ফিচার নিয়ন্ত্রণ করুন এবং যেকোনো সময় আপনার সংরক্ষিত রিপোর্ট সম্পর্কে জানুন।"
    override val aiBotOpenBtn = "চ্যাট ও কমান্ড"
    override val aiBotSheetTitle = "এআই হেলথ কমান্ড বট"
    override val aiBotWelcomeMsg = "আসসালামু আলাইকুম / নমস্কার! আমি চ্যাটের মাধ্যমে অ্যাপের সব ফিচার নিয়ন্ত্রণ করতে পারি (রিমাইন্ডার যোগ/স্নুজ, পেইড ডাক্তার বুকিং, প্রোফাইল আপডেট, ভাষা পরিবর্তন) এবং আপনার সংরক্ষিত মেডিকেল রিপোর্ট সম্পর্কে জানাতে পারি।"
    override val aiBotInputHint = "কমান্ড লিখুন বা রিপোর্ট সম্পর্কে জিজ্ঞাসা করুন..."
    override val aiBotThinking = "কমান্ড প্রসেস হচ্ছে..."
    override val aiBotResetChat = "নতুন চ্যাট"
    override val medicalReportsTitle = "আমার মেডিকেল রিপোর্ট"
    override val medicalReportsSub = "সিরিয়াল ক্লাউড ভল্ট"
    override val addReportBtn = "রিপোর্ট যোগ"
    override val scanReportPhotoBtn = "এআই স্ক্যান রিপোর্ট"
    override val scanningReportMsg = "এআই দিয়ে রিপোর্টের ছবি পড়া হচ্ছে..."
    override val reportTitleLabel = "রিপোর্টের নাম (যেমন: CBC ব্লাড টেস্ট)"
    override val reportCategoryLabel = "রিপোর্টের ধরন"
    override val reportDoctorLabLabel = "ডাক্তার / ডায়াগনস্টিক ল্যাব"
    override val reportDateLabel = "রিপোর্টের তারিখ (YYYY-MM-DD)"
    override val reportSummaryLabel = "মূল ফলাফলের সারসংক্ষেপ"
    override val reportDetailsLabel = "বিস্তারিত মান ও নোট"
    override val saveReportBtn = "ভল্টে সংরক্ষণ করুন"
    override val reportSavedToast = "মেডিকেল রিপোর্ট সিরিয়াল অনুযায়ী সংরক্ষিত হয়েছে!"
    override val noReportsYet = "এখনো কোনো মেডিকেল রিপোর্ট সংরক্ষিত নেই। ম্যানুয়ালি যোগ করুন বা রিপোর্টের ছবি স্ক্যান করুন যাতে লগইন করলেই সিরিয়াল অনুযায়ী দেখতে ও এআই বটকে জিজ্ঞাসা করতে পারেন।"
    override val askAiAboutReportBtn = "এআই বটকে জিজ্ঞাসা"
    override val saveRxToReportsBtn = "প্রেসক্রিপশন রিপোর্ট ভল্টে সংরক্ষণ করুন"
    override val alarmRingtoneLabel = "অ্যালার্মের রিংটোন"
    override val defaultRingtoneName = "ডিফল্ট অ্যালার্ম টোন"
    override val chooseRingtoneBtn = "রিংটোন বাছাই করুন"
    override val previewRingtoneBtn = "শুনুন"
    override val stopPreviewBtn = "থামান"
    override val backgroundAlarmActiveNote = "ব্যাকগ্রাউন্ড অ্যালার্ম চালু আছে — অ্যাপ বন্ধ থাকলেও নির্বাচিত রিংটোনে অ্যালার্ম বাজবে।"
    override val enableAlarmNotificationsTitle = "ব্যাকগ্রাউন্ড অ্যালার্ম নোটিফিকেশন চালু করুন"
    override val enableAlarmNotificationsSub = "অ্যাপ ব্যাকগ্রাউন্ডে থাকলেও ওষুধের রিমাইন্ডার রিংটোন ও বন্ধ/স্নুজ বাটন দেখাতে অনুমতি দিন।"
    override val allowNotificationsBtn = "অনুমতি দিন"
    override val editProfileTitle = "স্বাস্থ্য প্রোফাইল সম্পাদনা"
    override val displayNameLabel = "পূর্ণ নাম"
    override val locationLabel = "অবস্থান (শহর / এলাকা)"
    override val emergencyNumberLabel = "জরুরি যোগাযোগ নম্বর"
    override val callEmergencyBtn = "জরুরি কল করুন"
    override val saveProfileBtn = "প্রোফাইল সংরক্ষণ"
    override val profileUpdatedToast = "স্বাস্থ্য প্রোফাইল আপডেট হয়েছে!"
    override val proDoctorsTitle = "বিশেষজ্ঞ ডাক্তার"
    override val proDoctorsSub = "পেইড অনলাইন ভিজিট"
    override val paidBadgeLabel = "পেইড"
    override val findDoctorsTab = "বিশেষজ্ঞ তালিকা"
    override val myAppointmentsTab = "আমার অ্যাপয়েন্টমেন্ট"
    override val consultationFeeLabel = "ভিজিট ফি"
    override val bookAppointmentBtn = "অনলাইন অ্যাপয়েন্টমেন্ট নিন"
    override val checkoutTitle = "পেইড অনলাইন অ্যাপয়েন্টমেন্ট"
    override val selectDateLabel = "তারিখ নির্বাচন করুন"
    override val selectTimeSlotLabel = "সময় স্লট নির্বাচন করুন"
    override val consultationModeLabel = "পরামর্শের মাধ্যম"
    override val attachReportLabel = "মেডিকেল রিপোর্ট সংযুক্ত করুন (ঐচ্ছিক)"
    override val noReportAttached = "কোনোটি নয়"
    override val reasonForVisitLabel = "সমস্যা / ভিজিটের কারণ"
    override val paymentMethodLabel = "পেমেন্ট মাধ্যম নির্বাচন করুন"
    override val confirmAndPayBtn = "পেমেন্ট ও বুকিং নিশ্চিত করুন"
    override val appointmentBookedToast = "পেইড অনলাইন অ্যাপয়েন্টমেন্ট নিশ্চিত হয়েছে!"
    override val noAppointmentsYet = "এখনো কোনো অনলাইন ডাক্তার অ্যাপয়েন্টমেন্ট বুক করা হয়নি। বিশেষজ্ঞ ডাক্তারদের তালিকা থেকে ভিডিও, অডিও বা চ্যাট কনসালটেশন বুক করুন।"
    override val cancelRefundBtn = "বাতিল ও রিফান্ড"
    override val appointmentCancelledToast = "অ্যাপয়েন্টমেন্ট বাতিল ও রিফান্ড হিসেবে চিহ্নিত হয়েছে।"
    override val bmiHydrationTabLabel = "বিএমআই ও পানি"
    override val bmiCardTitle = "বিএমআই (BMI) ক্যালকুলেটর"
    override val heightCmLabel = "উচ্চতা (সেমি)"
    override val heightFeetLabel = "ফুট"
    override val heightInchesLabel = "ইঞ্চি"
    override val weightKgLabel = "ওজন (কেজি)"
    override val calculateSaveBmiBtn = "বিএমআই হিসাব ও সংরক্ষণ"
    override val bmiSavedToast = "আপনার বিএমআই সংরক্ষিত হয়েছে!"
    override val healthyRangeLabel = "সুস্থ বিএমআই সীমা: ১৮.৫ – ২৪.৯"
    override val bmiUnderweight = "কম ওজন"
    override val bmiNormal = "স্বাভাবিক ওজন"
    override val bmiOverweight = "অতিরিক্ত ওজন"
    override val bmiObese = "স্থূলতা"
    override val hydrationCardTitle = "দৈনিক পানি পান ট্র্যাকার"
    override val dailyGoalLabel = "দৈনিক লক্ষ্য"
    override val glassesLabel = "গ্লাস"
    override val waterUpdatedToast = "পানি পানের হিসাব আপডেট হয়েছে!"
    override val lifeConditionCardTitle = "আমার জীবন ও স্বাস্থ্য অবস্থার সারসংক্ষেপ"
    override val lifeConditionCardSub = "বিএমআই, পানি পান, ওষুধ ও মুডের সমন্বিত এআই বিশ্লেষণ।"
    override val generateLifeSummaryBtn = "স্বাস্থ্য অবস্থার সারসংক্ষেপ তৈরি করুন"
    override val refreshLifeSummaryBtn = "সারসংক্ষেপ আপডেট করুন"
    override val generatingLifeSummary = "আপনার জীবন ও স্বাস্থ্য অবস্থা বিশ্লেষণ হচ্ছে..."
    override val wellnessScoreLabel = "ওয়েলনেস স্কোর"
    override val dailyRecommendationsLabel = "প্রতিদিনের করণীয়"
    override val dashboardBodyHydrationTitle = "বিএমআই, পানি পান ও জীবন অবস্থা"
    override val attachMediaTitle = "ফাইল, ছবি বা অডিও সংযুক্ত করুন"
    override val attachMediaSubtitle = "ফোন ফাইল ম্যানেজার, গুগল ড্রাইভ, ডাউনলোড বা গ্যালারি থেকে বেছে নিন অথবা ভয়েস রেকর্ড করুন।"
    override val attachImageOption = "ছবি আপলোড করুন"
    override val attachImageOptionSub = "গ্যালারি, লোকাল ফাইল ও গুগল ড্রাইভ"
    override val attachFileOption = "ডকুমেন্ট / ফাইল আপলোড"
    override val attachFileOptionSub = "ফাইল ম্যানেজার ও গুগল ড্রাইভ থেকে PDF, TXT, DOC"
    override val attachAudioOption = "অডিও ফাইল আপলোড"
    override val attachAudioOptionSub = "ড্রাইভ বা ফাইল ম্যানেজার থেকে অডিও / ভয়েস নোট"
    override val recordVoiceOption = "ভয়েস উপসর্গ রেকর্ড করুন"
    override val recordingVoiceNow = "ভয়েস রেকর্ড হচ্ছে..."
    override val stopAndSendVoice = "থামান ও বিশ্লেষণ করুন"
    override val alsoSaveToReportsToggle = "আমার মেডিকেল রিপোর্ট ভল্টেও সংরক্ষণ করুন"
    override val analyzingAttachmentMsg = "সংযুক্ত ফাইল এআই দিয়ে বিশ্লেষণ হচ্ছে..."
    override val attachmentSavedToVaultToast = "বিশ্লেষণ সম্পন্ন ও মেডিকেল রিপোর্ট ভল্টে সংরক্ষিত!"
    override val uploadFromDriveFilesBtn = "ফাইল / ড্রাইভ / অডিও আপলোড"
    override val fileTooLargeError = "ফাইলটি ৮ মেগাবাইটের বেশি। অনুগ্রহ করে ছোট ফাইল বেছে নিন।"
    override val micPermissionDeniedError = "ভয়েস রেকর্ড করতে মাইক্রোফোনের অনুমতি প্রয়োজন।"
    override val dataDashboardTitle = "ডেটা ড্যাশবোর্ড"
    override val dataDashboardSub = "বিশ্লেষণ ও রিপোর্ট এক্সপোর্ট"
    override val adherenceRateLabel = "ওষুধ গ্রহণ"
    override val avgHydrationLabel = "৭ দিনের গড় পানি"
    override val latestBmiLabel = "সর্বশেষ বিএমআই"
    override val avgMoodScoreLabel = "গড় মুড স্কোর"
    override val adherenceChartTitle = "ওষুধ গ্রহণের চার্ট (গত ৭ দিন)"
    override val hydrationChartTitle = "পানি পানের ধারা (গত ৭ দিন)"
    override val moodTrendChartTitle = "মুড স্কোরের ধারা"
    override val vaultOverviewTitle = "স্বাস্থ্য ভল্ট ও সেবার সারাংশ"
    override val exportReportTitle = "উন্নত রিপোর্ট এক্সপোর্ট"
    override val exportReportSub = "ফাইল ম্যানেজার, গুগল ড্রাইভ, ইমেইল বা ডাক্তারের জন্য প্রিন্টযোগ্য PDF বা সারাংশ এক্সপোর্ট করুন।"
    override val savePdfToDeviceBtn = "PDF সেভ (ফাইল / ড্রাইভ)"
    override val sharePdfBtn = "PDF শেয়ার / প্রিন্ট"
    override val shareTextSummaryBtn = "টেক্সট শেয়ার"
    override val exportAllReportsPdfBtn = "সব রিপোর্ট PDF এক্সপোর্ট"
    override val exportSingleReportBtn = "PDF এক্সপোর্ট"
    override val pdfExportSuccessToast = "PDF স্বাস্থ্য রিপোর্ট সফলভাবে সংরক্ষিত হয়েছে!"
    override val pdfExportErrorToast = "PDF রিপোর্ট এক্সপোর্ট করা যায়নি। আবার চেষ্টা করুন।"
}

fun stringsFor(language: AppLanguage): BilingualStrings =
    if (language == AppLanguage.BN) BanglaStrings else EnglishStrings
