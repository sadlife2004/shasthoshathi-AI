package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdherenceLog
import com.example.data.model.DietPlanRecord
import com.example.data.model.DoctorAppointment
import com.example.data.model.DoctorBrief
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.MedicalReport
import com.example.data.model.MedicineReminder
import com.example.data.model.MoodEntry
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireOwnerUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user mismatch; refusing cross-user operation." }
        return uid
    }

    fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun todayShortLabel(): String =
        SimpleDateFormat("MMM dd", Locale.US).format(Date())

    // --- Real-time Observation Flows ---

    fun observeProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logStreamError(error, OperationType.GET, "users/$userId", userId)
                throw error
            }

    fun observeReminders(userId: String): Flow<List<MedicineReminder>> =
        db.collection("users").document(userId).collection("reminders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MedicineReminder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareBy<MedicineReminder> { parseMinutesSinceMidnight(it.scheduledTime) }
                        .thenBy { slotOrder(it.timeSlot) }
                )
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/reminders", userId)
                throw error
            }

    fun observeAdherenceLogs(userId: String): Flow<List<AdherenceLog>> =
        db.collection("users").document(userId).collection("adherence_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AdherenceLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/adherence_logs", userId)
                throw error
            }

    fun observeDoctorBriefs(userId: String): Flow<List<DoctorBrief>> =
        db.collection("users").document(userId).collection("doctor_briefs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DoctorBrief::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/doctor_briefs", userId)
                throw error
            }

    fun observeDietPlans(userId: String): Flow<List<DietPlanRecord>> =
        db.collection("users").document(userId).collection("diet_plans")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DietPlanRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/diet_plans", userId)
                throw error
            }

    fun observeMoodEntries(userId: String): Flow<List<MoodEntry>> =
        db.collection("users").document(userId).collection("mood_entries")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MoodEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/mood_entries", userId)
                throw error
            }

    fun observeMedicalReports(userId: String): Flow<List<MedicalReport>> =
        db.collection("users").document(userId).collection("medical_reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MedicalReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.serialNumber }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/medical_reports", userId)
                throw error
            }

    fun observeAppointments(userId: String): Flow<List<DoctorAppointment>> =
        db.collection("users").document(userId).collection("doctor_appointments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DoctorAppointment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/doctor_appointments", userId)
                throw error
            }

    fun observeHealthMetrics(userId: String): Flow<List<HealthMetricsRecord>> =
        db.collection("users").document(userId).collection("health_metrics")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    HealthMetricsRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateKey }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "users/$userId/health_metrics", userId)
                throw error
            }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private fun slotOrder(slot: String): Int = when (slot) {
        "MORNING" -> 0
        "NOON" -> 1
        "NIGHT" -> 2
        else -> 3
    }

    private fun parseMinutesSinceMidnight(timeLabel: String): Int {
        val cleaned = timeLabel.trim().uppercase(Locale.US)
        val regex = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""")
        val match = regex.matchEntire(cleaned) ?: return Int.MAX_VALUE
        val rawHour = match.groupValues[1].toIntOrNull() ?: return Int.MAX_VALUE
        val minute = match.groupValues[2].toIntOrNull() ?: return Int.MAX_VALUE
        val amPm = match.groupValues[3]
        val hour24 = when {
            amPm == "AM" && rawHour == 12 -> 0
            amPm == "PM" && rawHour != 12 -> rawHour + 12
            else -> rawHour
        }
        return hour24 * 60 + minute
    }

    // --- Profile Mutations ---

    suspend fun ensureUserProfile(
        userId: String,
        defaultName: String,
        languageCode: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = defaultName.trim().ifBlank { "ShasthoSathi User" }.take(80)
                val safeLang = if (languageCode == "BN") "BN" else "EN"
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "age" to 30L,
                    "location" to "",
                    "emergencyNumber" to "",
                    "language" to safeLang,
                    "healthConditions" to listOf("General Wellness"),
                    "dietaryPreference" to "Regular",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        age: Long,
        location: String,
        emergencyNumber: String,
        languageCode: String,
        healthConditions: List<String>,
        dietaryPreference: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireOwnerUid(userId)
            val safeName = displayName.trim().ifBlank { "ShasthoSathi User" }.take(80)
            val safeAge = age.coerceIn(1L, 120L)
            val safeLocation = location.trim().take(120)
            val safeEmergency = emergencyNumber.trim().take(30)
            val safeLang = if (languageCode == "BN") "BN" else "EN"
            val safeConditions = healthConditions.take(10)
            val safePref = dietaryPreference.trim().ifBlank { "Regular" }.take(60)
            val docRef = db.collection("users").document(userId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "age" to safeAge,
                        "location" to safeLocation,
                        "emergencyNumber" to safeEmergency,
                        "language" to safeLang,
                        "healthConditions" to safeConditions,
                        "dietaryPreference" to safePref,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "displayName" to safeName,
                        "age" to safeAge,
                        "location" to safeLocation,
                        "emergencyNumber" to safeEmergency,
                        "language" to safeLang,
                        "healthConditions" to safeConditions,
                        "dietaryPreference" to safePref,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Medicine Reminder Mutations ---

    suspend fun addReminder(userId: String, reminder: MedicineReminder): Result<String> {
        var path = "users/$userId/reminders"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId).collection("reminders").document()
            path = docRef.path
            val payload = buildReminderCreateMap(userId, reminder)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addRemindersBatch(userId: String, reminders: List<MedicineReminder>): Result<Unit> {
        val path = "users/$userId/reminders"
        return try {
            requireOwnerUid(userId)
            if (reminders.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            val collection = db.collection("users").document(userId).collection("reminders")
            reminders.forEach { rem ->
                val docRef = collection.document()
                batch.set(docRef, buildReminderCreateMap(userId, rem))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun buildReminderCreateMap(userId: String, rem: MedicineReminder): Map<String, Any> {
        val validSlot = if (rem.timeSlot in listOf("MORNING", "NOON", "NIGHT")) rem.timeSlot else "MORNING"
        val validMeal = if (rem.mealTiming in listOf("Before Meal", "After Meal", "Anytime")) {
            rem.mealTiming
        } else {
            "After Meal"
        }
        val validSource = if (rem.sourceType in listOf("AUTO_RX", "MANUAL")) rem.sourceType else "MANUAL"
        val validStatus = if (rem.todayStatus in listOf("PENDING", "TAKEN", "SNOOZED", "SKIPPED")) {
            rem.todayStatus
        } else {
            "PENDING"
        }
        return mapOf(
            "userId" to userId,
            "medicineName" to rem.medicineName.trim().ifBlank { "Medicine" }.take(120),
            "dosage" to rem.dosage.trim().ifBlank { "1 Dose" }.take(80),
            "timeSlot" to validSlot,
            "scheduledTime" to rem.scheduledTime.trim().ifBlank { "08:30 AM" }.take(20),
            "mealTiming" to validMeal,
            "durationDays" to rem.durationDays.coerceIn(1L, 365L),
            "sourceType" to validSource,
            "active" to rem.active,
            "todayStatus" to validStatus,
            "lastActionDate" to rem.lastActionDate.ifBlank { todayDateKey() }.take(20),
            "notes" to rem.notes.trim().take(300),
            "ringtoneUri" to rem.ringtoneUri.trim().take(400),
            "ringtoneName" to rem.ringtoneName.trim().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun toggleReminderActive(
        userId: String,
        reminderId: String,
        active: Boolean
    ): Result<Unit> {
        val path = "users/$userId/reminders/$reminderId"
        return try {
            requireOwnerUid(userId)
            require(reminderId.isNotBlank()) { "Reminder ID cannot be blank" }
            db.collection("users").document(userId)
                .collection("reminders").document(reminderId)
                .update(
                    mapOf(
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateReminder(
        userId: String,
        reminder: MedicineReminder
    ): Result<Unit> {
        val path = "users/$userId/reminders/${reminder.id}"
        return try {
            requireOwnerUid(userId)
            require(reminder.id.isNotBlank()) { "Reminder ID cannot be blank" }
            val validSlot = if (reminder.timeSlot in listOf("MORNING", "NOON", "NIGHT")) {
                reminder.timeSlot
            } else {
                "MORNING"
            }
            val validMeal = if (reminder.mealTiming in listOf("Before Meal", "After Meal", "Anytime")) {
                reminder.mealTiming
            } else {
                "After Meal"
            }
            db.collection("users").document(userId)
                .collection("reminders").document(reminder.id)
                .update(
                    mapOf(
                        "medicineName" to reminder.medicineName.trim().ifBlank { "Medicine" }.take(120),
                        "dosage" to reminder.dosage.trim().ifBlank { "1 Dose" }.take(80),
                        "timeSlot" to validSlot,
                        "scheduledTime" to reminder.scheduledTime.trim().ifBlank { "08:30 AM" }.take(20),
                        "mealTiming" to validMeal,
                        "durationDays" to reminder.durationDays.coerceIn(1L, 365L),
                        "notes" to reminder.notes.trim().take(300),
                        "ringtoneUri" to reminder.ringtoneUri.trim().take(400),
                        "ringtoneName" to reminder.ringtoneName.trim().take(100),
                        "active" to reminder.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markReminderStatus(
        userId: String,
        reminder: MedicineReminder,
        action: String, // TAKEN, SNOOZED, SKIPPED
        newScheduledTime: String? = null
    ): Result<Unit> {
        val path = "users/$userId/reminders/${reminder.id}"
        return try {
            requireOwnerUid(userId)
            require(reminder.id.isNotBlank()) { "Reminder ID cannot be blank" }
            val safeAction = if (action in listOf("TAKEN", "SNOOZED", "SKIPPED")) action else "TAKEN"
            val dateKey = todayDateKey()
            val remRef = db.collection("users").document(userId)
                .collection("reminders").document(reminder.id)
            val logRef = db.collection("users").document(userId)
                .collection("adherence_logs").document()

            val updateFields = mutableMapOf<String, Any>(
                "todayStatus" to safeAction,
                "lastActionDate" to dateKey,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!newScheduledTime.isNullOrBlank()) {
                updateFields["scheduledTime"] = newScheduledTime.take(20)
            }

            val logPayload = mapOf(
                "userId" to userId,
                "reminderId" to reminder.id.take(128),
                "medicineName" to reminder.medicineName.ifBlank { "Medicine" }.take(120),
                "action" to safeAction,
                "dateKey" to dateKey,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(remRef, updateFields)
            batch.set(logRef, logPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(userId: String, reminderId: String): Result<Unit> {
        val path = "users/$userId/reminders/$reminderId"
        return try {
            requireOwnerUid(userId)
            require(reminderId.isNotBlank()) { "Reminder ID cannot be blank" }
            db.collection("users").document(userId)
                .collection("reminders").document(reminderId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Doctor Briefs ---

    suspend fun saveDoctorBrief(userId: String, brief: DoctorBrief): Result<String> {
        var path = "users/$userId/doctor_briefs"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId)
                .collection("doctor_briefs").document()
            path = docRef.path
            val safeLang = if (brief.language == "BN") "BN" else "EN"
            val payload = mapOf(
                "userId" to userId,
                "language" to safeLang,
                "chiefSymptoms" to brief.chiefSymptoms.trim().ifBlank { "General symptoms" }.take(1000),
                "severityAndContext" to brief.severityAndContext.trim().ifBlank { "Not specified" }.take(1000),
                "questionsForDoctor" to brief.questionsForDoctor.take(15),
                "redFlagWarning" to brief.redFlagWarning.trim().take(600),
                "urgentCareAdvised" to brief.urgentCareAdvised,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Diet Plans ---

    suspend fun saveDietPlan(userId: String, plan: DietPlanRecord): Result<String> {
        var path = "users/$userId/diet_plans"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId)
                .collection("diet_plans").document()
            path = docRef.path
            val safeLang = if (plan.language == "BN") "BN" else "EN"
            val payload = mapOf(
                "userId" to userId,
                "language" to safeLang,
                "age" to plan.age.coerceIn(1L, 120L),
                "conditions" to plan.conditions.take(10),
                "preference" to plan.preference.trim().ifBlank { "Regular" }.take(60),
                "breakfastTitle" to plan.breakfastTitle.trim().ifBlank { "Whole Wheat Roti & Dal" }.take(200),
                "breakfastPortion" to plan.breakfastPortion.trim().ifBlank { "2 rotis, 1 bowl dal" }.take(300),
                "breakfastRationale" to plan.breakfastRationale.trim().ifBlank { "Balanced fiber & protein" }.take(400),
                "lunchTitle" to plan.lunchTitle.trim().ifBlank { "Measured Rice & Fish Curry" }.take(200),
                "lunchPortion" to plan.lunchPortion.trim().ifBlank { "1 cup rice, 1 piece fish, veggies" }.take(300),
                "lunchRationale" to plan.lunchRationale.trim().ifBlank { "Steady afternoon energy" }.take(400),
                "snackTitle" to plan.snackTitle.trim().ifBlank { "Seasonal Fruit & Roasted Chickpeas" }.take(200),
                "snackPortion" to plan.snackPortion.trim().ifBlank { "1 guava, handful of chola" }.take(300),
                "snackRationale" to plan.snackRationale.trim().ifBlank { "Low glycemic snack" }.take(400),
                "dinnerTitle" to plan.dinnerTitle.trim().ifBlank { "Atta Roti & Mixed Shak" }.take(200),
                "dinnerPortion" to plan.dinnerPortion.trim().ifBlank { "2 thin rotis, light vegetable curry" }.take(300),
                "dinnerRationale" to plan.dinnerRationale.trim().ifBlank { "Easy evening digestion" }.take(400),
                "foodsToPrioritize" to plan.foodsToPrioritize.take(15),
                "foodsToLimit" to plan.foodsToLimit.take(15),
                "hydrationTip" to plan.hydrationTip.trim().ifBlank { "Drink 8 glasses of safe water daily." }.take(400),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Mood Journal Entries ---

    suspend fun saveMoodEntry(userId: String, entry: MoodEntry): Result<String> {
        var path = "users/$userId/mood_entries"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId)
                .collection("mood_entries").document()
            path = docRef.path
            val safeLang = if (entry.language == "BN") "BN" else "EN"
            val validMood = if (entry.selectedMood in listOf("HAPPY", "CALM", "TIRED", "STRESSED", "SAD")) {
                entry.selectedMood
            } else {
                "CALM"
            }
            val payload = mapOf(
                "userId" to userId,
                "language" to safeLang,
                "selectedMood" to validMood,
                "journalText" to entry.journalText.trim().ifBlank { "Mood check-in" }.take(2000),
                "detectedMoodTag" to entry.detectedMoodTag.trim().ifBlank { validMood }.take(60),
                "moodScore" to entry.moodScore.coerceIn(1L, 5L),
                "aiReflection" to entry.aiReflection.trim().ifBlank { "Thank you for checking in today." }.take(1200),
                "microAction" to entry.microAction.trim().ifBlank { "Take 3 slow deep breaths." }.take(400),
                "supportNote" to entry.supportNote.trim().take(500),
                "dateLabel" to entry.dateLabel.trim().ifBlank { todayShortLabel() }.take(30),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Optional Realistic Starter Data Seeding (User-triggered or Empty Dashboard) ---

    suspend fun seedStarterData(userId: String, isBangla: Boolean): Result<Unit> {
        val path = "users/$userId/reminders"
        return try {
            requireOwnerUid(userId)
            val today = todayDateKey()
            val batch = db.batch()
            val remCol = db.collection("users").document(userId).collection("reminders")
            val starterReminders = listOf(
                MedicineReminder(
                    medicineName = "Seclo 20mg (Omeprazole)",
                    dosage = "1 Capsule",
                    timeSlot = "MORNING",
                    scheduledTime = "08:00 AM",
                    mealTiming = "Before Meal",
                    durationDays = 14,
                    sourceType = "AUTO_RX",
                    active = true,
                    todayStatus = "TAKEN",
                    lastActionDate = today,
                    notes = "30 mins before breakfast"
                ),
                MedicineReminder(
                    medicineName = "Napa 500mg (Paracetamol)",
                    dosage = "1 Tablet",
                    timeSlot = "NOON",
                    scheduledTime = "01:30 PM",
                    mealTiming = "After Meal",
                    durationDays = 5,
                    sourceType = "AUTO_RX",
                    active = true,
                    todayStatus = "PENDING",
                    lastActionDate = today,
                    notes = "Take with water after lunch"
                ),
                MedicineReminder(
                    medicineName = "Fexo 120mg (Fexofenadine)",
                    dosage = "1 Tablet",
                    timeSlot = "NIGHT",
                    scheduledTime = "09:30 PM",
                    mealTiming = "After Meal",
                    durationDays = 7,
                    sourceType = "MANUAL",
                    active = true,
                    todayStatus = "PENDING",
                    lastActionDate = today,
                    notes = "Before bedtime"
                )
            )
            starterReminders.forEach { rem ->
                batch.set(remCol.document(), buildReminderCreateMap(userId, rem))
            }

            val moodCol = db.collection("users").document(userId).collection("mood_entries")
            val starterMoods = listOf(
                Triple("TIRED", 3L, "Nov 15"),
                Triple("STRESSED", 2L, "Nov 16"),
                Triple("CALM", 4L, "Nov 17"),
                Triple("HAPPY", 5L, "Nov 18")
            )
            starterMoods.forEach { (mood, score, label) ->
                batch.set(
                    moodCol.document(),
                    mapOf(
                        "userId" to userId,
                        "language" to if (isBangla) "BN" else "EN",
                        "selectedMood" to mood,
                        "journalText" to if (isBangla) "প্রতিদিনের স্বাস্থ্য ও মন ভালো রাখার চেষ্টা করছি।" else "Checking in on my daily energy and health routine.",
                        "detectedMoodTag" to mood.lowercase().replaceFirstChar { it.uppercase() },
                        "moodScore" to score,
                        "aiReflection" to if (isBangla) "নিজের যত্ন নেওয়ার জন্য ধন্যবাদ। প্রতিদিন একটু বিশ্রাম আপনাকে আরও সুস্থ রাখবে।" else "Thank you for pausing to check in with yourself. Small consistent habits build lasting resilience.",
                        "microAction" to if (isBangla) "আজ ২ মিনিট শান্ত হয়ে গভীর শ্বাস নিন।" else "Take 2 quiet minutes for gentle 4-7-8 breathing.",
                        "supportNote" to "",
                        "dateLabel" to label,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Serial Medical Reports Vault ---

    suspend fun addMedicalReport(userId: String, report: MedicalReport): Result<String> {
        var path = "users/$userId/medical_reports"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId)
                .collection("medical_reports").document()
            path = docRef.path
            val validCategory = if (report.category in listOf("LAB_TEST", "PRESCRIPTION", "IMAGING", "DOCTOR_NOTE", "OTHER")) {
                report.category
            } else {
                "OTHER"
            }
            val validAttachmentType = if (report.attachmentType in listOf("TEXT", "IMAGE", "FILE", "AUDIO")) {
                report.attachmentType
            } else {
                "TEXT"
            }
            val payload = mapOf(
                "userId" to userId,
                "serialNumber" to report.serialNumber.coerceIn(1L, 100000L),
                "title" to report.title.trim().ifBlank { "Medical Report" }.take(160),
                "category" to validCategory,
                "doctorOrLab" to report.doctorOrLab.trim().take(120),
                "reportDate" to report.reportDate.trim().ifBlank { todayDateKey() }.take(30),
                "summary" to report.summary.trim().ifBlank { "Saved health report" }.take(600),
                "details" to report.details.trim().take(4000),
                "attachmentType" to validAttachmentType,
                "attachmentName" to report.attachmentName.trim().take(160),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMedicalReport(userId: String, reportId: String): Result<Unit> {
        val path = "users/$userId/medical_reports/$reportId"
        return try {
            requireOwnerUid(userId)
            require(reportId.isNotBlank()) { "Report ID cannot be blank" }
            db.collection("users").document(userId)
                .collection("medical_reports").document(reportId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Paid Professional Doctor Online Appointments ---

    suspend fun createAppointment(userId: String, appointment: DoctorAppointment): Result<String> {
        var path = "users/$userId/doctor_appointments"
        return try {
            requireOwnerUid(userId)
            val docRef = db.collection("users").document(userId)
                .collection("doctor_appointments").document()
            path = docRef.path
            val validMode = if (appointment.consultationMode in listOf("VIDEO", "AUDIO", "CHAT")) {
                appointment.consultationMode
            } else {
                "VIDEO"
            }
            val validMethod = if (appointment.paymentMethod in listOf("BKASH", "NAGAD", "CARD")) {
                appointment.paymentMethod
            } else {
                "BKASH"
            }
            val payload = mapOf(
                "userId" to userId,
                "doctorId" to appointment.doctorId.trim().ifBlank { "doc_general" }.take(64),
                "doctorName" to appointment.doctorName.trim().ifBlank { "Specialist Doctor" }.take(120),
                "specialty" to appointment.specialty.trim().ifBlank { "General Medicine" }.take(100),
                "hospital" to appointment.hospital.trim().ifBlank { "Online Clinic" }.take(140),
                "appointmentDate" to appointment.appointmentDate.trim().ifBlank { todayDateKey() }.take(30),
                "timeSlot" to appointment.timeSlot.trim().ifBlank { "07:00 PM" }.take(30),
                "consultationMode" to validMode,
                "feeBdt" to appointment.feeBdt.coerceIn(100L, 50000L),
                "paymentMethod" to validMethod,
                "paymentStatus" to "PAID",
                "bookingStatus" to "CONFIRMED",
                "bookingRef" to appointment.bookingRef.trim().ifBlank { "SS-${System.currentTimeMillis() % 100000}" }.take(40),
                "reasonForVisit" to appointment.reasonForVisit.trim().take(500),
                "attachedReportTitle" to appointment.attachedReportTitle.trim().take(160),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelAppointment(userId: String, appointmentId: String): Result<Unit> {
        val path = "users/$userId/doctor_appointments/$appointmentId"
        return try {
            requireOwnerUid(userId)
            require(appointmentId.isNotBlank()) { "Appointment ID cannot be blank" }
            db.collection("users").document(userId)
                .collection("doctor_appointments").document(appointmentId)
                .update(
                    mapOf(
                        "bookingStatus" to "CANCELLED",
                        "paymentStatus" to "REFUNDED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Daily BMI, Hydration & AI Life Condition Summary ---

    suspend fun upsertDailyHealthMetrics(
        userId: String,
        record: HealthMetricsRecord
    ): Result<Unit> {
        val safeDateKey = record.dateKey.trim().ifBlank { todayDateKey() }.take(20)
        val path = "users/$userId/health_metrics/$safeDateKey"
        return try {
            requireOwnerUid(userId)
            val safeLang = if (record.language == "BN") "BN" else "EN"
            val safeHeight = record.heightCm.coerceIn(0.0, 300.0)
            val safeWeight = record.weightKg.coerceIn(0.0, 500.0)
            val safeBmi = record.bmiValue.coerceIn(0.0, 100.0)
            val safeCategory = record.bmiCategory.trim().take(40)
            val safeWater = record.waterIntakeMl.coerceIn(0L, 20000L)
            val safeGoal = record.waterGoalMl.coerceIn(500L, 10000L)
            val safeTitle = record.lifeConditionTitle.trim().take(120)
            val safeScore = record.lifeConditionScore.coerceIn(0L, 100L)
            val safeSummary = record.lifeConditionSummary.trim().take(1500)
            val safeTips = record.lifeConditionTips.map { it.trim().take(240) }.filter { it.isNotBlank() }.take(8)

            val docRef = db.collection("users").document(userId)
                .collection("health_metrics").document(safeDateKey)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "heightCm" to safeHeight,
                        "weightKg" to safeWeight,
                        "bmiValue" to safeBmi,
                        "bmiCategory" to safeCategory,
                        "waterIntakeMl" to safeWater,
                        "waterGoalMl" to safeGoal,
                        "lifeConditionTitle" to safeTitle,
                        "lifeConditionScore" to safeScore,
                        "lifeConditionSummary" to safeSummary,
                        "lifeConditionTips" to safeTips,
                        "language" to safeLang,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "dateKey" to safeDateKey,
                        "heightCm" to safeHeight,
                        "weightKg" to safeWeight,
                        "bmiValue" to safeBmi,
                        "bmiCategory" to safeCategory,
                        "waterIntakeMl" to safeWater,
                        "waterGoalMl" to safeGoal,
                        "lifeConditionTitle" to safeTitle,
                        "lifeConditionScore" to safeScore,
                        "lifeConditionSummary" to safeSummary,
                        "lifeConditionTips" to safeTips,
                        "language" to safeLang,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
