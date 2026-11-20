package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val age: Long = 30L,
    val location: String = "",
    val emergencyNumber: String = "",
    val language: String = "EN",
    val healthConditions: List<String> = emptyList(),
    val dietaryPreference: String = "Regular",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MedicineReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val timeSlot: String = "MORNING", // MORNING, NOON, NIGHT
    val scheduledTime: String = "08:30 AM",
    val mealTiming: String = "After Meal", // Before Meal, After Meal, Anytime
    val durationDays: Long = 7L,
    val sourceType: String = "MANUAL", // AUTO_RX, MANUAL
    val active: Boolean = true,
    val todayStatus: String = "PENDING", // PENDING, TAKEN, SNOOZED, SKIPPED
    val lastActionDate: String = "",
    val notes: String = "",
    val ringtoneUri: String = "",
    val ringtoneName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdherenceLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reminderId: String = "",
    val medicineName: String = "",
    val action: String = "TAKEN", // TAKEN, SNOOZED, SKIPPED
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DoctorBrief(
    @DocumentId val id: String = "",
    val userId: String = "",
    val language: String = "EN",
    val chiefSymptoms: String = "",
    val severityAndContext: String = "",
    val questionsForDoctor: List<String> = emptyList(),
    val redFlagWarning: String = "",
    val urgentCareAdvised: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DietPlanRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val language: String = "EN",
    val age: Long = 30L,
    val conditions: List<String> = emptyList(),
    val preference: String = "Regular",
    val breakfastTitle: String = "",
    val breakfastPortion: String = "",
    val breakfastRationale: String = "",
    val lunchTitle: String = "",
    val lunchPortion: String = "",
    val lunchRationale: String = "",
    val snackTitle: String = "",
    val snackPortion: String = "",
    val snackRationale: String = "",
    val dinnerTitle: String = "",
    val dinnerPortion: String = "",
    val dinnerRationale: String = "",
    val foodsToPrioritize: List<String> = emptyList(),
    val foodsToLimit: List<String> = emptyList(),
    val hydrationTip: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MoodEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val language: String = "EN",
    val selectedMood: String = "CALM", // HAPPY, CALM, TIRED, STRESSED, SAD
    val journalText: String = "",
    val detectedMoodTag: String = "",
    val moodScore: Long = 3L,
    val aiReflection: String = "",
    val microAction: String = "",
    val supportNote: String = "",
    val dateLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MedicalReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val serialNumber: Long = 1L,
    val title: String = "",
    val category: String = "LAB_TEST", // LAB_TEST, PRESCRIPTION, IMAGING, DOCTOR_NOTE, OTHER
    val doctorOrLab: String = "",
    val reportDate: String = "",
    val summary: String = "",
    val details: String = "",
    val attachmentType: String = "TEXT", // TEXT, IMAGE, FILE, AUDIO
    val attachmentName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HealthMetricsRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val bmiValue: Double = 0.0,
    val bmiCategory: String = "",
    val waterIntakeMl: Long = 0L,
    val waterGoalMl: Long = 2500L,
    val lifeConditionTitle: String = "",
    val lifeConditionScore: Long = 0L,
    val lifeConditionSummary: String = "",
    val lifeConditionTips: List<String> = emptyList(),
    val language: String = "EN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DoctorAppointment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val specialty: String = "",
    val hospital: String = "",
    val appointmentDate: String = "",
    val timeSlot: String = "",
    val consultationMode: String = "VIDEO", // VIDEO, AUDIO, CHAT
    val feeBdt: Long = 800L,
    val paymentMethod: String = "BKASH", // BKASH, NAGAD, CARD
    val paymentStatus: String = "PAID", // PAID, REFUNDED
    val bookingStatus: String = "CONFIRMED", // CONFIRMED, COMPLETED, CANCELLED
    val bookingRef: String = "",
    val reasonForVisit: String = "",
    val attachedReportTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Curated Specialist Doctor Directory Model (Telemedicine Consultation)
data class ProfessionalDoctor(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val qualification: String,
    val bmdcRegNo: String,
    val specialtyEn: String,
    val specialtyBn: String,
    val hospitalEn: String,
    val hospitalBn: String,
    val experienceYears: Int,
    val rating: Double,
    val feeBdt: Int,
    val availableSlots: List<String>,
    val languages: String
)

// Transient UI & AI models

data class SymptomChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val timeLabel: String,
    val attachmentBadge: String = ""
)

data class AiCommandChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val timeLabel: String,
    val actionBadge: String = ""
)

data class AiCommandResult(
    val reply: String,
    val actionType: String, // NONE, NAVIGATE, ADD_REMINDER, MARK_REMINDER_TAKEN, SNOOZE_REMINDER, TOGGLE_LANGUAGE, ADD_REPORT, UPDATE_PROFILE, TEST_ALARM
    val targetScreen: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val scheduledTime: String = "",
    val mealTiming: String = "",
    val durationDays: Int = 7,
    val reportTitle: String = "",
    val reportCategory: String = "LAB_TEST",
    val reportSummary: String = "",
    val profileAge: Int = 0,
    val profileLocation: String = "",
    val profileEmergencyNumber: String = ""
)

data class ExtractedMedication(
    val localId: String,
    val name: String,
    val dosage: String,
    val morning: Boolean,
    val noon: Boolean,
    val night: Boolean,
    val mealTiming: String, // "Before Meal" | "After Meal" | "Anytime"
    val durationDays: Int,
    val specialNotes: String,
    val confidence: String // "High" | "Medium" | "Low"
)

data class PrescriptionAnalysisResult(
    val overallClarity: String, // "High" | "Medium" | "Low"
    val warningMessage: String,
    val medicines: List<ExtractedMedication>
)

data class MoodAnalysisResult(
    val detectedMoodTag: String,
    val moodScore: Int,
    val aiReflection: String,
    val microAction: String,
    val supportNote: String
)

data class LifeConditionSummaryResult(
    val title: String,
    val score: Int,
    val summary: String,
    val tips: List<String>
)

data class SymptomAttachmentAnalysis(
    val title: String,
    val category: String,
    val summary: String,
    val details: String,
    val followUpReply: String
)

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
