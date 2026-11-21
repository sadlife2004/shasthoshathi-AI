package com.example.data.ai

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.example.data.model.AiCommandChatMessage
import com.example.data.model.AiCommandResult
import com.example.data.model.DietPlanRecord
import com.example.data.model.DoctorBrief
import com.example.data.model.ExtractedMedication
import com.example.data.model.HealthMetricsRecord
import com.example.data.model.LifeConditionSummaryResult
import com.example.data.model.MedicalReport
import com.example.data.model.MedicineReminder
import com.example.data.model.MoodAnalysisResult
import com.example.data.model.MoodEntry
import com.example.data.model.PrescriptionAnalysisResult
import com.example.data.model.SymptomAttachmentAnalysis
import com.example.data.model.SymptomChatMessage
import com.example.data.model.UserProfile
import com.example.data.firebase.AppCheckDiagnostics
import com.example.data.firebase.forceRefreshAppCheckToken
import com.example.i18n.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val AI_TAG = "ShasthoAiService"
private const val PRIMARY_MODEL = "gemini-3.8-flash"
private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
private const val TEXT_PRIMARY_TIMEOUT_MS = 45_000L
private const val TEXT_FALLBACK_TIMEOUT_MS = 35_000L
private const val VISION_PRIMARY_TIMEOUT_MS = 50_000L
private const val VISION_FALLBACK_TIMEOUT_MS = 40_000L

class ShasthoAiService {

    // 1. Symptom Organizer Conversational Model (non-diagnostic, empathetic, 3-4 concise follow-ups)
    private val symptomSystemInstruction = content {
        text(
            """
            You are ShasthoSathi AI, an empathetic, safety-hardened pre-consultation health companion for users in Bangladesh and globally.
            STRICT RULES:
            1. NEVER diagnose any disease or prescribe any medication. Always frame guidance as preparation for a licensed doctor.
            2. Ask ONLY ONE short, targeted, warm follow-up question per turn (max 2-3 short sentences total).
            3. Over 3-4 turns, gently gather: (a) duration of symptoms, (b) severity 1-10, (c) existing conditions or allergies, (d) medicines already taken.
            4. After 3-4 user turns, briefly let the user know they can tap 'Generate Doctor Brief' to get their structured summary.
            5. If severe red-flag symptoms appear (chest pain, difficulty breathing, severe bleeding, fainting, high persistent fever >103F), immediately advise urgent medical care.
            6. Match the requested language strictly (natural English or natural Bengali/বাংলা).
            """.trimIndent()
        )
    }

    private val symptomChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            systemInstruction = symptomSystemInstruction
        )
    }

    private val symptomChatFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            systemInstruction = symptomSystemInstruction
        )
    }

    // 2. Structured Doctor Brief JSON Model
    private val doctorBriefSchema = Schema.obj(
        properties = mapOf(
            "chiefSymptoms" to Schema.string(description = "Summary of chief symptoms and their duration"),
            "severityAndContext" to Schema.string(description = "Reported severity (1-10), existing conditions, allergies, triggers, and prior medicines taken"),
            "questionsForDoctor" to Schema.array(
                items = Schema.string(),
                description = "3 to 5 specific, helpful questions the patient should ask their doctor"
            ),
            "redFlagWarning" to Schema.string(description = "Clear warning signs that warrant immediate emergency medical care"),
            "urgentCareAdvised" to Schema.boolean(description = "True if the user described urgent red-flag symptoms")
        )
    )

    private val doctorBriefConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = doctorBriefSchema
    }

    private val doctorBriefSystemInstruction = content {
        text(
            "You organize patient-reported symptoms into a clear, non-diagnostic pre-consultation brief for a licensed physician. Never diagnose diseases."
        )
    }

    private val doctorBriefModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = doctorBriefConfig,
            systemInstruction = doctorBriefSystemInstruction
        )
    }

    private val doctorBriefFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = doctorBriefConfig,
            systemInstruction = doctorBriefSystemInstruction
        )
    }

    // 3. Structured Prescription Reader Vision JSON Model (anti-hallucination guardrails)
    private val prescriptionSchema = Schema.obj(
        properties = mapOf(
            "overallClarity" to Schema.enumeration(
                values = listOf("High", "Medium", "Low"),
                description = "Overall readability of the prescription image"
            ),
            "warningMessage" to Schema.string(
                description = "Empty string if everything is crystal clear, otherwise a clear caution explaining which handwritten parts need manual verification"
            ),
            "medicines" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Exact medicine brand/generic name visible on prescription, or 'Unclear - Verify' if unreadable"),
                        "dosage" to Schema.string(description = "Strength and dose per intake e.g. '500mg - 1 Tablet'"),
                        "morning" to Schema.boolean(description = "True if prescribed in morning (e.g. 1+x+x)"),
                        "noon" to Schema.boolean(description = "True if prescribed at noon (e.g. x+1+x)"),
                        "night" to Schema.boolean(description = "True if prescribed at night (e.g. x+x+1)"),
                        "mealTiming" to Schema.enumeration(
                            values = listOf("Before Meal", "After Meal", "Anytime"),
                            description = "Relation to meals"
                        ),
                        "durationDays" to Schema.integer(description = "Course duration in days, default 7 if unspecified"),
                        "specialNotes" to Schema.string(description = "Any extra instruction like 'with water' or 'complete full course'"),
                        "confidence" to Schema.enumeration(
                            values = listOf("High", "Medium", "Low"),
                            description = "Extraction confidence for this specific medicine"
                        )
                    )
                ),
                description = "List of extracted medications from the prescription image"
            )
        )
    )

    private val prescriptionConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = prescriptionSchema
    }

    private val prescriptionSystemInstruction = content {
        text(
            """
            You are a safety-first medical prescription OCR assistant familiar with Bangladeshi and international prescriptions (including 1+0+1 / 1+1+1 notation, Napa, Seclo, Fexo, etc.).
            ANTI-HALLUCINATION RULE: Never guess unreadable handwriting. If a medicine name or dose is blurry or uncertain, set confidence to "Low", set overallClarity to "Low" or "Medium", and explain clearly in warningMessage.
            """.trimIndent()
        )
    }

    private val prescriptionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = prescriptionConfig,
            systemInstruction = prescriptionSystemInstruction
        )
    }

    private val prescriptionFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = prescriptionConfig,
            systemInstruction = prescriptionSystemInstruction
        )
    }

    // 4. Structured Bangladeshi Local Food Diet Planner JSON Model
    private val dietPlanSchema = Schema.obj(
        properties = mapOf(
            "breakfastTitle" to Schema.string(description = "Bangladeshi breakfast dish title"),
            "breakfastPortion" to Schema.string(description = "Practical household portion guide"),
            "breakfastRationale" to Schema.string(description = "Brief nutritional rationale"),
            "lunchTitle" to Schema.string(description = "Bangladeshi lunch dish title"),
            "lunchPortion" to Schema.string(description = "Practical household portion guide"),
            "lunchRationale" to Schema.string(description = "Brief nutritional rationale"),
            "snackTitle" to Schema.string(description = "Bangladeshi evening snack title"),
            "snackPortion" to Schema.string(description = "Practical portion guide"),
            "snackRationale" to Schema.string(description = "Brief nutritional rationale"),
            "dinnerTitle" to Schema.string(description = "Bangladeshi dinner dish title"),
            "dinnerPortion" to Schema.string(description = "Practical portion guide"),
            "dinnerRationale" to Schema.string(description = "Brief nutritional rationale"),
            "foodsToPrioritize" to Schema.array(
                items = Schema.string(),
                description = "5 culturally familiar Bangladeshi foods to prioritize"
            ),
            "foodsToLimit" to Schema.array(
                items = Schema.string(),
                description = "5 foods to limit or avoid for this health profile"
            ),
            "hydrationTip" to Schema.string(description = "Short practical daily hydration tip")
        )
    )

    private val dietPlanConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = dietPlanSchema
    }

    private val dietPlanSystemInstruction = content {
        text(
            "You are a culturally grounded nutrition guide creating everyday Bangladeshi home-cooked meal plans (e.g., লাল আটার রুটি, ডাল, শাকসবজি, মাছের ঝোল, পরিমিত ভাত, সিদ্ধ ডিম, পেয়ারা, ছোলা). Provide general wellness guidance only, not medical prescriptions."
        )
    }

    private val dietPlanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = dietPlanConfig,
            systemInstruction = dietPlanSystemInstruction
        )
    }

    private val dietPlanFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = dietPlanConfig,
            systemInstruction = dietPlanSystemInstruction
        )
    }

    // 5. Structured Mood Journal Analyzer JSON Model
    private val moodSchema = Schema.obj(
        properties = mapOf(
            "detectedMoodTag" to Schema.string(description = "2-3 word gentle emotional theme e.g. 'Calm & Grounded' or 'Overwhelmed & Tired'"),
            "moodScore" to Schema.integer(description = "Emotional wellness score from 1 (very distressed) to 5 (very positive)"),
            "aiReflection" to Schema.string(description = "Warm, empathetic 2-3 sentence reflection acknowledging the user's feelings without diagnosing"),
            "microAction" to Schema.string(description = "One tiny, realistic 2-minute soothing action the user can try right now"),
            "supportNote" to Schema.string(description = "Empty string unless persistent distress or crisis cues appear, in which case gently suggest speaking with a trusted loved one or mental health professional")
        )
    )

    private val moodConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = moodSchema
    }

    private val moodSystemInstruction = content {
        text(
            "You are a compassionate mental wellness journal companion. Validate feelings warmly, offer one small practical micro-action, never diagnose mental health disorders, and gently encourage professional support if distress is significant."
        )
    }

    private val moodModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = moodConfig,
            systemInstruction = moodSystemInstruction
        )
    }

    private val moodFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = moodConfig,
            systemInstruction = moodSystemInstruction
        )
    }

    // 6. AI Health Command Bot (Controls App Features + Answers Saved Serial Medical Reports)
    private val assistantCommandSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(description = "Warm, concise, non-diagnostic answer in the requested language explaining the action taken or answering the user's question about their saved medical reports"),
            "actionType" to Schema.enumeration(
                values = listOf(
                    "NONE",
                    "NAVIGATE",
                    "ADD_REMINDER",
                    "MARK_REMINDER_TAKEN",
                    "SNOOZE_REMINDER",
                    "TOGGLE_LANGUAGE",
                    "ADD_REPORT",
                    "UPDATE_PROFILE",
                    "TEST_ALARM"
                ),
                description = "App command to execute based on the user's request, or NONE for Q&A / report lookup"
            ),
            "targetScreen" to Schema.enumeration(
                values = listOf("", "HOME", "SYMPTOMS", "SCAN_RX", "REMINDERS", "DIET", "MOOD", "BMI", "DATA_DASHBOARD", "REPORTS", "DOCTORS"),
                description = "Destination screen when actionType is NAVIGATE"
            ),
            "medicineName" to Schema.string(description = "Medicine name for ADD_REMINDER / MARK_REMINDER_TAKEN / SNOOZE_REMINDER, else empty string"),
            "dosage" to Schema.string(description = "Dosage e.g. '1 Tablet' for ADD_REMINDER, else empty string"),
            "scheduledTime" to Schema.string(description = "Exact 12-hour clock time e.g. '08:30 AM' or '09:00 PM' for ADD_REMINDER, else empty string"),
            "mealTiming" to Schema.enumeration(
                values = listOf("Before Meal", "After Meal", "Anytime"),
                description = "Meal relation for ADD_REMINDER"
            ),
            "durationDays" to Schema.integer(description = "Duration in days for ADD_REMINDER (default 7)"),
            "reportTitle" to Schema.string(description = "Title for ADD_REPORT, else empty string"),
            "reportCategory" to Schema.enumeration(
                values = listOf("LAB_TEST", "PRESCRIPTION", "IMAGING", "DOCTOR_NOTE", "OTHER"),
                description = "Category for ADD_REPORT"
            ),
            "reportSummary" to Schema.string(description = "Summary findings for ADD_REPORT, else empty string"),
            "profileAge" to Schema.integer(description = "New age integer for UPDATE_PROFILE, or 0 if unchanged"),
            "profileLocation" to Schema.string(description = "New city/location for UPDATE_PROFILE, else empty string"),
            "profileEmergencyNumber" to Schema.string(description = "New emergency phone number for UPDATE_PROFILE, else empty string")
        )
    )

    private val assistantCommandConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = assistantCommandSchema
    }

    private val assistantCommandInstruction = content {
        text(
            """
            You are ShasthoSathi AI Command Bot — a bilingual (English & Bangla) personal health assistant that can BOTH:
            1. Answer detailed questions about the user's saved Serial Medical Reports (#1, #2, #3...), Doctor Briefs, active medicines, and profile. Always reference the exact serial number (#1, #2...) and dates when discussing reports.
            2. Control ALL features of the ShasthoSathi app via structured JSON commands:
               - NAVIGATE: open SYMPTOMS, SCAN_RX, REMINDERS, DIET, MOOD, BMI (BMI & Hydration / Life Condition Summary), DATA_DASHBOARD (Health Analytics & PDF Export), REPORTS (My Reports Vault), or DOCTORS (Paid Professional Doctor Appointments).
               - ADD_REMINDER: create a medicine reminder with exact 12-hour clock format "hh:mm AM/PM" (e.g. "09:00 PM", "07:30 AM").
               - MARK_REMINDER_TAKEN / SNOOZE_REMINDER: mark or snooze a medicine by name.
               - ADD_REPORT: save a new medical report entry into the user's serial reports vault.
               - UPDATE_PROFILE: update user's age, location, or emergency contact number.
               - TOGGLE_LANGUAGE: switch between English and Bangla.
               - TEST_ALARM: trigger a test medicine alarm right now.
            SAFETY RULE: Never diagnose diseases or prescribe new medications. Always remind the user to consult a licensed physician for clinical decisions.
            """.trimIndent()
        )
    }

    private val assistantCommandModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = assistantCommandConfig,
            systemInstruction = assistantCommandInstruction
        )
    }

    private val assistantCommandFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = assistantCommandConfig,
            systemInstruction = assistantCommandInstruction
        )
    }

    // 7. Medical Report Document Vision Extractor
    private val reportExtractSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Concise report title e.g. 'Complete Blood Count (CBC)' or 'Lipid Profile'"),
            "category" to Schema.enumeration(
                values = listOf("LAB_TEST", "PRESCRIPTION", "IMAGING", "DOCTOR_NOTE", "OTHER"),
                description = "Category of the medical document"
            ),
            "doctorOrLab" to Schema.string(description = "Hospital, diagnostic lab, or doctor name visible on the report"),
            "summary" to Schema.string(description = "Concise 2-3 sentence key findings summary without diagnosing"),
            "details" to Schema.string(description = "Key values, test parameters, or instructions extracted cleanly from the photo")
        )
    )

    private val reportExtractConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = reportExtractSchema
    }

    private val reportExtractInstruction = content {
        text(
            "You extract structured key findings from medical lab reports, prescriptions, or imaging summaries for the patient's personal health archive. Never fabricate unreadable numbers; mark unclear items for doctor verification."
        )
    }

    private val reportExtractModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = reportExtractConfig,
            systemInstruction = reportExtractInstruction
        )
    }

    private val reportExtractFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = reportExtractConfig,
            systemInstruction = reportExtractInstruction
        )
    }

    // 8. Holistic "My Life Condition Summary" JSON Model (BMI + Hydration + Adherence + Mood)
    private val lifeConditionSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short encouraging 3-5 word status headline e.g. 'Balanced & Hydrating Well' or 'Needs More Water & Rest'"),
            "score" to Schema.integer(description = "Overall daily lifestyle wellness score from 0 to 100 based on BMI, hydration, medication consistency, and mood"),
            "summary" to Schema.string(description = "Warm, holistic 3-4 sentence lifestyle condition summary combining BMI, daily hydration progress, medication adherence, and recent mood without diagnosing any disease"),
            "tips" to Schema.array(
                items = Schema.string(),
                description = "Exactly 3 practical, culturally relevant daily wellness actions for Bangladesh & everyday life"
            )
        )
    )

    private val lifeConditionConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = lifeConditionSchema
    }

    private val lifeConditionInstruction = content {
        text(
            "You are ShasthoSathi AI, a compassionate preventive wellness coach. Synthesize the user's BMI, daily water intake, medication adherence, and mood trend into a clear, encouraging 'Life Condition Summary'. Never diagnose diseases; give practical everyday hydration, movement, nutrition, and rest guidance."
        )
    }

    private val lifeConditionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = lifeConditionConfig,
            systemInstruction = lifeConditionInstruction
        )
    }

    private val lifeConditionFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = lifeConditionConfig,
            systemInstruction = lifeConditionInstruction
        )
    }

    // 9. Multimodal Symptom Attachment (Image / PDF File / Audio Note) Analyzer
    private val symptomAttachmentSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Concise medical archive title for this uploaded photo, document, or voice note"),
            "category" to Schema.enumeration(
                values = listOf("LAB_TEST", "PRESCRIPTION", "IMAGING", "DOCTOR_NOTE", "OTHER"),
                description = "Best matching category for saving in the user's Serial Medical Reports Vault"
            ),
            "summary" to Schema.string(description = "Concise 2-3 sentence summary of key observations, symptoms, or document findings (non-diagnostic)"),
            "details" to Schema.string(description = "Extracted text, test readings, audio transcript highlights, or visual observations to store in the report archive"),
            "followUpReply" to Schema.string(description = "Warm, empathetic 2-3 sentence response in the symptom chat acknowledging the attachment and asking ONE short pre-consultation follow-up question without diagnosing")
        )
    )

    private val symptomAttachmentConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = symptomAttachmentSchema
    }

    private val symptomAttachmentInstruction = content {
        text(
            "You analyze patient-uploaded symptom photos, PDF/text medical documents, and voice/audio recordings to help organize information for a licensed doctor and archive it in the patient's personal Medical Reports Vault. NEVER diagnose diseases or prescribe treatments. If urgent red-flag symptoms appear, advise immediate medical care."
        )
    }

    private val symptomAttachmentModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = symptomAttachmentConfig,
            systemInstruction = symptomAttachmentInstruction
        )
    }

    private val symptomAttachmentFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = symptomAttachmentConfig,
            systemInstruction = symptomAttachmentInstruction
        )
    }

    // --- Resilient Model Execution Helper ---

    private fun isAppCheckError(t: Throwable): Boolean {
        val msg = (t.message ?: t.localizedMessage ?: "").lowercase()
        return msg.contains("app check") || msg.contains("appcheck") || msg.contains("token is invalid") || msg.contains("403")
    }

    private suspend fun generateWithFallback(
        label: String,
        primaryModel: GenerativeModel,
        fallbackModel: GenerativeModel,
        primaryTimeoutMs: Long,
        fallbackTimeoutMs: Long,
        requestContent: Content
    ): GenerateContentResponse {
        return try {
            withTimeout(primaryTimeoutMs) {
                primaryModel.generateContent(requestContent)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(AI_TAG, "$label timed out on $PRIMARY_MODEL after ${primaryTimeoutMs}ms; retrying with $FALLBACK_MODEL", e)
            withTimeout(fallbackTimeoutMs) {
                fallbackModel.generateContent(requestContent)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isAppCheckError(e)) {
                Log.w(AI_TAG, "$label hit App Check error on $PRIMARY_MODEL; forcing App Check token refresh...", e)
                val refreshErr = forceRefreshAppCheckToken()
                if (refreshErr != null) {
                    Log.e(AI_TAG, "App Check refresh failed (${AppCheckDiagnostics.activeProvider}): $refreshErr")
                }
                return try {
                    withTimeout(primaryTimeoutMs) {
                        primaryModel.generateContent(requestContent)
                    }
                } catch (retryErr: CancellationException) {
                    throw retryErr
                } catch (retryErr: Exception) {
                    val detail = AppCheckDiagnostics.lastTokenError
                    if (isAppCheckError(retryErr) && !detail.isNullOrBlank()) {
                        throw IllegalStateException(
                            "Firebase App Check (${AppCheckDiagnostics.activeProvider}) failed: $detail",
                            retryErr
                        )
                    }
                    throw retryErr
                }
            }
            Log.w(AI_TAG, "$label failed on $PRIMARY_MODEL (${e.localizedMessage}); retrying with $FALLBACK_MODEL", e)
            withTimeout(fallbackTimeoutMs) {
                fallbackModel.generateContent(requestContent)
            }
        }
    }

    // --- Public AI Operations ---

    suspend fun sendSymptomFollowUp(
        history: List<SymptomChatMessage>,
        language: AppLanguage
    ): Result<String> {
        return try {
            val recentWindow = history.takeLast(8)
            val langInstruction = if (language == AppLanguage.BN) {
                "Respond ONLY in natural, empathetic Bengali (বাংলা)."
            } else {
                "Respond ONLY in clear, empathetic English."
            }
            val transcript = recentWindow.joinToString("\n") { msg ->
                val role = if (msg.fromUser) "Patient" else "ShasthoSathi"
                "$role: ${msg.text}"
            }
            val userTurnCount = history.count { it.fromUser }
            val prompt = """
                $langInstruction
                User turn count so far: $userTurnCount of 4.
                Conversation so far:
                $transcript

                Reply with ONE short empathetic follow-up question (or if 3+ turns are done, invite them to tap 'Generate Doctor Brief'). Keep it concise and non-diagnostic.
            """.trimIndent()

            val response = generateWithFallback(
                label = "Symptom follow-up",
                primaryModel = symptomChatModel,
                fallbackModel = symptomChatFallbackModel,
                primaryTimeoutMs = TEXT_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = TEXT_FALLBACK_TIMEOUT_MS,
                requestContent = content { text(prompt) }
            )
            val reply = response.text?.trim()
            if (!reply.isNullOrBlank()) {
                Result.success(reply)
            } else {
                Result.failure(IllegalStateException("AI returned an empty reply."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Symptom follow-up timed out", e)
            Result.failure(Exception("Request timed out. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Symptom follow-up failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateDoctorBrief(
        history: List<SymptomChatMessage>,
        language: AppLanguage
    ): Result<DoctorBrief> {
        return try {
            val transcript = history.takeLast(10).joinToString("\n") { msg ->
                val role = if (msg.fromUser) "Patient" else "Assistant"
                "$role: ${msg.text}"
            }
            val langInstruction = if (language == AppLanguage.BN) {
                "Write ALL JSON field values in natural Bengali (বাংলা)."
            } else {
                "Write ALL JSON field values in clear English."
            }
            val prompt = """
                $langInstruction
                Convert this pre-consultation symptom conversation into a structured Doctor Brief JSON.
                Do NOT diagnose any condition.

                Transcript:
                $transcript
            """.trimIndent()

            val response = generateWithFallback(
                label = "Doctor brief",
                primaryModel = doctorBriefModel,
                fallbackModel = doctorBriefFallbackModel,
                primaryTimeoutMs = TEXT_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = TEXT_FALLBACK_TIMEOUT_MS,
                requestContent = content { text(prompt) }
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty doctor brief response.")
            val json = JSONObject(jsonText)
            val qArray = json.optJSONArray("questionsForDoctor")
            val questions = buildList {
                if (qArray != null) {
                    for (i in 0 until qArray.length()) {
                        val item = qArray.optString(i).trim()
                        if (item.isNotBlank()) add(item)
                    }
                }
            }
            Result.success(
                DoctorBrief(
                    language = language.code,
                    chiefSymptoms = json.optString("chiefSymptoms", "Symptoms reported by patient"),
                    severityAndContext = json.optString("severityAndContext", "Context shared during check-in"),
                    questionsForDoctor = questions.ifEmpty {
                        listOf(
                            if (language == AppLanguage.BN) "এই উপসর্গগুলোর সম্ভাব্য কারণ কী হতে পারে?"
                            else "What diagnostic checks would you recommend for these symptoms?"
                        )
                    },
                    redFlagWarning = json.optString("redFlagWarning", ""),
                    urgentCareAdvised = json.optBoolean("urgentCareAdvised", false)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Doctor brief timed out", e)
            Result.failure(Exception("Brief generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Doctor brief generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzePrescription(
        bitmap: Bitmap,
        language: AppLanguage
    ): Result<PrescriptionAnalysisResult> {
        var preparedBitmap: Bitmap? = null
        return try {
            // Prepare compact software-backed bitmap off the main thread for reliable fast upload
            val scaledBitmap = withContext(Dispatchers.Default) {
                prepareBitmapForVision(bitmap, maxDim = 800)
            }
            if (scaledBitmap !== bitmap) {
                preparedBitmap = scaledBitmap
            }

            val langNote = if (language == AppLanguage.BN) {
                "Write warningMessage and specialNotes in Bengali (বাংলা), while keeping medicine names accurate as printed/written (e.g., Napa 500mg) and keeping mealTiming/confidence/overallClarity strictly in the allowed English enum values."
            } else {
                "Write warningMessage and specialNotes in concise English."
            }
            val requestContent = content {
                image(scaledBitmap)
                text(
                    """
                    Analyze this medical prescription carefully and extract all visible medicines into JSON.
                    Remember: In Bangladesh, 1+0+1 means Morning=true, Noon=false, Night=true. 0+0+1 means Night=true only. 1+1+1 means Morning, Noon, Night.
                    Never hallucinate unreadable medicines — mark confidence Low if uncertain.
                    $langNote
                    """.trimIndent()
                )
            }

            val response = generateWithFallback(
                label = "Prescription analysis",
                primaryModel = prescriptionModel,
                fallbackModel = prescriptionFallbackModel,
                primaryTimeoutMs = VISION_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = VISION_FALLBACK_TIMEOUT_MS,
                requestContent = requestContent
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty prescription response.")
            val json = JSONObject(jsonText)
            val overallClarity = json.optString("overallClarity", "Medium")
            val warningMessage = json.optString("warningMessage", "")
            val medsArray = json.optJSONArray("medicines")
            val meds = buildList {
                if (medsArray != null) {
                    for (i in 0 until medsArray.length()) {
                        val obj = medsArray.getJSONObject(i)
                        val mealRaw = obj.optString("mealTiming", "After Meal")
                        val safeMeal = if (mealRaw in listOf("Before Meal", "After Meal", "Anytime")) {
                            mealRaw
                        } else {
                            "After Meal"
                        }
                        val confRaw = obj.optString("confidence", "Medium")
                        val safeConf = if (confRaw in listOf("High", "Medium", "Low")) confRaw else "Medium"
                        add(
                            ExtractedMedication(
                                localId = UUID.randomUUID().toString(),
                                name = obj.optString("name", "Unclear Medicine").trim(),
                                dosage = obj.optString("dosage", "1 Dose").trim(),
                                morning = obj.optBoolean("morning", false),
                                noon = obj.optBoolean("noon", false),
                                night = obj.optBoolean("night", false),
                                mealTiming = safeMeal,
                                durationDays = obj.optInt("durationDays", 7).coerceIn(1, 365),
                                specialNotes = obj.optString("specialNotes", "").trim(),
                                confidence = safeConf
                            )
                        )
                    }
                }
            }
            Result.success(
                PrescriptionAnalysisResult(
                    overallClarity = overallClarity,
                    warningMessage = warningMessage,
                    medicines = meds
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Prescription analysis timed out", e)
            Result.failure(Exception("Prescription analysis timed out. Please retry with a clearer photo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Prescription analysis failed", e)
            Result.failure(e)
        } finally {
            preparedBitmap?.let { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    suspend fun generateDietPlan(
        age: Int,
        conditions: List<String>,
        preference: String,
        language: AppLanguage
    ): Result<DietPlanRecord> {
        return try {
            val langInstruction = if (language == AppLanguage.BN) {
                "Write ALL dish names, portions, rationales, food lists, and hydration tip in natural Bengali (বাংলা)."
            } else {
                "Write ALL fields in clear English while referencing authentic Bangladeshi foods (e.g. Whole wheat atta roti, Masoor dal, Macher jhol, Mixed shak, Measured rice, Guava)."
            }
            val condText = conditions.ifEmpty { listOf("General Wellness") }.joinToString(", ")
            val prompt = """
                $langInstruction
                Create a realistic 1-day Bangladeshi local food diet plan for:
                - Age: $age years
                - Health Focus: $condText
                - Dietary Style: $preference
                Keep portions practical, affordable, and everyday Bangladeshi home-cooked meals. General wellness guidance only.
            """.trimIndent()

            val response = generateWithFallback(
                label = "Diet plan",
                primaryModel = dietPlanModel,
                fallbackModel = dietPlanFallbackModel,
                primaryTimeoutMs = TEXT_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = TEXT_FALLBACK_TIMEOUT_MS,
                requestContent = content { text(prompt) }
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty diet plan response.")
            val json = JSONObject(jsonText)
            val prioritize = jsonArrayToList(json, "foodsToPrioritize")
            val limit = jsonArrayToList(json, "foodsToLimit")

            Result.success(
                DietPlanRecord(
                    language = language.code,
                    age = age.toLong(),
                    conditions = conditions.ifEmpty { listOf("General Wellness") },
                    preference = preference,
                    breakfastTitle = json.optString("breakfastTitle"),
                    breakfastPortion = json.optString("breakfastPortion"),
                    breakfastRationale = json.optString("breakfastRationale"),
                    lunchTitle = json.optString("lunchTitle"),
                    lunchPortion = json.optString("lunchPortion"),
                    lunchRationale = json.optString("lunchRationale"),
                    snackTitle = json.optString("snackTitle"),
                    snackPortion = json.optString("snackPortion"),
                    snackRationale = json.optString("snackRationale"),
                    dinnerTitle = json.optString("dinnerTitle"),
                    dinnerPortion = json.optString("dinnerPortion"),
                    dinnerRationale = json.optString("dinnerRationale"),
                    foodsToPrioritize = prioritize,
                    foodsToLimit = limit,
                    hydrationTip = json.optString("hydrationTip")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Diet plan timed out", e)
            Result.failure(Exception("Diet plan generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Diet plan generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeMoodJournal(
        selectedMood: String,
        journalText: String,
        language: AppLanguage
    ): Result<MoodAnalysisResult> {
        return try {
            val langInstruction = if (language == AppLanguage.BN) {
                "Respond in warm, natural Bengali (বাংলা) for all text fields."
            } else {
                "Respond in warm, natural English for all text fields."
            }
            val prompt = """
                $langInstruction
                User selected mood: $selectedMood
                Diary entry: "$journalText"
                Provide an empathetic reflection, a 1-5 moodScore, one practical micro-action, and a gentle supportNote ONLY if significant distress is expressed (otherwise empty string "").
            """.trimIndent()

            val response = generateWithFallback(
                label = "Mood analysis",
                primaryModel = moodModel,
                fallbackModel = moodFallbackModel,
                primaryTimeoutMs = TEXT_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = TEXT_FALLBACK_TIMEOUT_MS,
                requestContent = content { text(prompt) }
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty mood reflection response.")
            val json = JSONObject(jsonText)
            Result.success(
                MoodAnalysisResult(
                    detectedMoodTag = json.optString("detectedMoodTag", selectedMood),
                    moodScore = json.optInt("moodScore", 3).coerceIn(1, 5),
                    aiReflection = json.optString("aiReflection"),
                    microAction = json.optString("microAction"),
                    supportNote = json.optString("supportNote", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Mood analysis timed out", e)
            Result.failure(Exception("Mood reflection timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Mood analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun processAssistantCommand(
        userMessage: String,
        chatHistory: List<AiCommandChatMessage>,
        profile: UserProfile?,
        reminders: List<MedicineReminder>,
        reports: List<MedicalReport>,
        briefs: List<DoctorBrief>,
        language: AppLanguage
    ): Result<AiCommandResult> {
        return try {
            val langInstruction = if (language == AppLanguage.BN) {
                "Respond in warm, natural Bengali (বাংলা) inside the 'reply' field. Keep enum fields strictly in English."
            } else {
                "Respond in warm, clear English inside the 'reply' field."
            }

            val profileSummary = if (profile != null) {
                "Name=${profile.displayName}, Age=${profile.age}, Location=${profile.location.ifBlank { "Not set" }}, Emergency=${profile.emergencyNumber.ifBlank { "Not set" }}, Diet=${profile.dietaryPreference}"
            } else {
                "Not configured yet"
            }

            val remindersSummary = if (reminders.isEmpty()) {
                "None"
            } else {
                reminders.take(12).joinToString("\n") { rem ->
                    "- ${rem.medicineName} (${rem.dosage}) at ${rem.scheduledTime} [${rem.mealTiming}, status=${rem.todayStatus}, active=${rem.active}]"
                }
            }

            val reportsSummary = if (reports.isEmpty()) {
                "No saved medical reports yet."
            } else {
                reports.takeLast(15).joinToString("\n") { rep ->
                    "Report #${rep.serialNumber} (${rep.reportDate}) [${rep.category}] \"${rep.title}\" by ${rep.doctorOrLab.ifBlank { "Self/Clinic" }}: Summary: ${rep.summary} | Details: ${rep.details.take(300)}"
                }
            }

            val briefsSummary = if (briefs.isEmpty()) {
                "None"
            } else {
                briefs.take(3).joinToString("\n") { b ->
                    "- Symptoms: ${b.chiefSymptoms}; Context: ${b.severityAndContext}"
                }
            }

            val recentChat = chatHistory.takeLast(6).joinToString("\n") { m ->
                "${if (m.fromUser) "User" else "Bot"}: ${m.text}"
            }

            val prompt = """
                $langInstruction
                === USER PROFILE ===
                $profileSummary

                === ACTIVE MEDICINE REMINDERS ===
                $remindersSummary

                === SAVED SERIAL MEDICAL REPORTS ===
                $reportsSummary

                === RECENT DOCTOR BRIEFS ===
                $briefsSummary

                === RECENT CHAT ===
                $recentChat

                === NEW USER MESSAGE / COMMAND ===
                $userMessage
            """.trimIndent()

            val response = generateWithFallback(
                label = "AI Command Bot",
                primaryModel = assistantCommandModel,
                fallbackModel = assistantCommandFallbackModel,
                primaryTimeoutMs = TEXT_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = TEXT_FALLBACK_TIMEOUT_MS,
                requestContent = content { text(prompt) }
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty assistant response.")
            val json = JSONObject(jsonText)
            Result.success(
                AiCommandResult(
                    reply = json.optString("reply", "Done!"),
                    actionType = json.optString("actionType", "NONE"),
                    targetScreen = json.optString("targetScreen", ""),
                    medicineName = json.optString("medicineName", ""),
                    dosage = json.optString("dosage", ""),
                    scheduledTime = json.optString("scheduledTime", ""),
                    mealTiming = json.optString("mealTiming", "After Meal"),
                    durationDays = json.optInt("durationDays", 7).coerceIn(1, 365),
                    reportTitle = json.optString("reportTitle", ""),
                    reportCategory = json.optString("reportCategory", "LAB_TEST"),
                    reportSummary = json.optString("reportSummary", ""),
                    profileAge = json.optInt("profileAge", 0),
                    profileLocation = json.optString("profileLocation", ""),
                    profileEmergencyNumber = json.optString("profileEmergencyNumber", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "AI Command Bot timed out", e)
            Result.failure(Exception("AI assistant timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "AI Command Bot failed", e)
            Result.failure(e)
        }
    }

    suspend fun extractMedicalReportFromBitmap(
        bitmap: Bitmap,
        language: AppLanguage
    ): Result<MedicalReport> {
        var preparedBitmap: Bitmap? = null
        return try {
            val scaledBitmap = withContext(Dispatchers.Default) {
                prepareBitmapForVision(bitmap, maxDim = 800)
            }
            if (scaledBitmap !== bitmap) preparedBitmap = scaledBitmap

            val langNote = if (language == AppLanguage.BN) {
                "Write title, summary, and details in clear Bengali (বাংলা), keeping test parameter names and numbers accurate, and keeping category strictly in English enum values."
            } else {
                "Write title, summary, and details in concise English."
            }

            val response = generateWithFallback(
                label = "Medical report OCR",
                primaryModel = reportExtractModel,
                fallbackModel = reportExtractFallbackModel,
                primaryTimeoutMs = VISION_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = VISION_FALLBACK_TIMEOUT_MS,
                requestContent = content {
                    image(scaledBitmap)
                    text("Extract the medical report metadata, key findings summary, and readable values into JSON. $langNote")
                }
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty report extraction response.")
            val json = JSONObject(jsonText)
            Result.success(
                MedicalReport(
                    title = json.optString("title", "Medical Report").trim(),
                    category = json.optString("category", "LAB_TEST"),
                    doctorOrLab = json.optString("doctorOrLab", "").trim(),
                    summary = json.optString("summary", "Extracted from photo").trim(),
                    details = json.optString("details", "").trim()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Report OCR timed out", e)
            Result.failure(Exception("Report scan timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Report OCR failed", e)
            Result.failure(e)
        } finally {
            preparedBitmap?.let { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    suspend fun generateLifeConditionSummary(
        profile: UserProfile?,
        latestMetrics: HealthMetricsRecord?,
        takenDoses: Int,
        totalDoses: Int,
        recentMoods: List<MoodEntry>,
        reportCount: Int,
        language: AppLanguage
    ): Result<LifeConditionSummaryResult> {
        return try {
            val langInstruction = if (language == AppLanguage.BN) {
                "Write title, summary, and all 3 tips in warm, natural Bengali (বাংলা)."
            } else {
                "Write title, summary, and all 3 tips in warm, clear English."
            }
            val bmiText = if (latestMetrics != null && latestMetrics.bmiValue > 0.0) {
                String.format(
                    java.util.Locale.US,
                    "BMI %.1f (%s), Height %.1f cm, Weight %.1f kg",
                    latestMetrics.bmiValue,
                    latestMetrics.bmiCategory.ifBlank { "Recorded" },
                    latestMetrics.heightCm,
                    latestMetrics.weightKg
                )
            } else {
                "BMI not recorded yet"
            }
            val waterText = if (latestMetrics != null) {
                "${latestMetrics.waterIntakeMl} ml / ${latestMetrics.waterGoalMl} ml daily goal"
            } else {
                "0 ml / 2500 ml daily goal"
            }
            val moodText = if (recentMoods.isEmpty()) {
                "No mood check-ins logged yet"
            } else {
                recentMoods.take(3).joinToString("; ") { "${it.selectedMood} (${it.moodScore}/5)" }
            }
            val ageText = profile?.age?.toString() ?: "30"
            val conditionsText = profile?.healthConditions?.ifEmpty { listOf("General Wellness") }?.joinToString(", ")
                ?: "General Wellness"

            val prompt = """
                $langInstruction
                Generate a holistic, non-diagnostic "My Life Condition Summary" JSON for this user:
                - Age: $ageText, Focus: $conditionsText
                - BMI & Body Metrics: $bmiText
                - Today's Hydration: $waterText
                - Today's Medicine Adherence: $takenDoses of $totalDoses active doses taken
                - Recent Mood Trend: $moodText
                - Saved Medical Reports in Vault: $reportCount

                Provide an encouraging title, a 0-100 wellness score reflecting their current habits, a clear 3-4 sentence summary of their overall life condition, and exactly 3 practical daily tips.
            """.trimIndent()

            val response = generateWithFallback(
                label = "Life condition summary",
                primaryModel = lifeConditionModel,
                fallbackModel = lifeConditionFallbackModel,
                primaryTimeoutMs = TEXT_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = TEXT_FALLBACK_TIMEOUT_MS,
                requestContent = content { text(prompt) }
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty life condition summary response.")
            val json = JSONObject(jsonText)
            val tips = jsonArrayToList(json, "tips").take(5)
            Result.success(
                LifeConditionSummaryResult(
                    title = json.optString("title", if (language == AppLanguage.BN) "স্বাস্থ্য অবস্থা সারসংক্ষেপ" else "Daily Life Condition").trim(),
                    score = json.optInt("score", 70).coerceIn(0, 100),
                    summary = json.optString("summary", "").trim(),
                    tips = tips
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Life condition summary timed out", e)
            Result.failure(Exception("Summary generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Life condition summary failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeSymptomAttachment(
        bytes: ByteArray?,
        bitmap: Bitmap?,
        mimeType: String,
        fileName: String,
        attachmentType: String, // IMAGE, FILE, AUDIO
        language: AppLanguage
    ): Result<SymptomAttachmentAnalysis> {
        var preparedBitmap: Bitmap? = null
        return try {
            val langInstruction = if (language == AppLanguage.BN) {
                "Write title, summary, details, and followUpReply in warm, natural Bengali (বাংলা), while keeping category strictly in the allowed English enum values."
            } else {
                "Write title, summary, details, and followUpReply in clear, empathetic English."
            }
            val safeMime = mimeType.trim().lowercase(java.util.Locale.US)
            val requestContent = when {
                bitmap != null -> {
                    val scaled = withContext(Dispatchers.Default) {
                        prepareBitmapForVision(bitmap, maxDim = 800)
                    }
                    if (scaled !== bitmap) preparedBitmap = scaled
                    content {
                        image(scaled)
                        text(
                            "The patient uploaded an image file named '$fileName' ($attachmentType) during symptom check-in. Extract key visual/text details for their Medical Reports Vault and write a gentle non-diagnostic follow-up reply. $langInstruction"
                        )
                    }
                }
                bytes != null && isDirectlySupportedInlineMime(safeMime) -> {
                    content {
                        inlineData(bytes, safeMime)
                        text(
                            "The patient uploaded a $attachmentType attachment named '$fileName' (MIME: $safeMime) during symptom check-in. Listen/read it carefully, extract key symptom notes or medical report values for their Medical Reports Vault, and write an empathetic non-diagnostic follow-up reply. $langInstruction"
                        )
                    }
                }
                bytes != null -> {
                    val textExcerpt = runCatching {
                        bytes.toString(Charsets.UTF_8)
                            .replace(Regex("[^\\p{Print}\\p{L}\\p{N}\\s.,:;!?\\-_/()]"), " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                            .take(3000)
                    }.getOrDefault("")
                    content {
                        text(
                            """
                            The patient uploaded a file named '$fileName' (type: $attachmentType, MIME: $safeMime).
                            Extracted readable text preview (may be partial for binary office docs):
                            ${textExcerpt.ifBlank { "(Binary document attached — ask patient to describe key details for their doctor.)" }}

                            Summarize what is available for the patient's Medical Reports Vault without inventing any unseen values, and ask one gentle follow-up question.
                            $langInstruction
                            """.trimIndent()
                        )
                    }
                }
                else -> throw IllegalArgumentException("Attachment data is empty.")
            }

            val response = generateWithFallback(
                label = "Symptom attachment analysis ($attachmentType)",
                primaryModel = symptomAttachmentModel,
                fallbackModel = symptomAttachmentFallbackModel,
                primaryTimeoutMs = VISION_PRIMARY_TIMEOUT_MS,
                fallbackTimeoutMs = VISION_FALLBACK_TIMEOUT_MS,
                requestContent = requestContent
            )
            val jsonText = response.text?.trim()
                ?: throw IllegalStateException("Empty attachment analysis response.")
            val json = JSONObject(jsonText)
            val rawCategory = json.optString("category", "DOCTOR_NOTE")
            val safeCategory = if (rawCategory in listOf("LAB_TEST", "PRESCRIPTION", "IMAGING", "DOCTOR_NOTE", "OTHER")) {
                rawCategory
            } else {
                "OTHER"
            }
            Result.success(
                SymptomAttachmentAnalysis(
                    title = json.optString("title", fileName.ifBlank { "Uploaded Attachment" }).trim(),
                    category = safeCategory,
                    summary = json.optString("summary", "Uploaded $attachmentType attachment ($fileName).").trim(),
                    details = json.optString("details", "").trim(),
                    followUpReply = json.optString(
                        "followUpReply",
                        if (language == AppLanguage.BN) "আপনার ফাইলটি পেয়েছি। এই উপসর্গগুলো কতদিন ধরে হচ্ছে?"
                        else "Thank you for sharing this file. How long have you been experiencing these symptoms?"
                    ).trim()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Symptom attachment analysis timed out", e)
            Result.failure(Exception("File analysis timed out. Please try a smaller file or retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Symptom attachment analysis failed", e)
            Result.failure(e)
        } finally {
            preparedBitmap?.let { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    private fun isDirectlySupportedInlineMime(mime: String): Boolean {
        return mime == "application/pdf" ||
            mime.startsWith("text/") ||
            mime.startsWith("audio/") ||
            mime.startsWith("image/")
    }

    private fun jsonArrayToList(json: JSONObject, key: String): List<String> {
        val arr = json.optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotBlank()) add(item)
            }
        }
    }

    private fun prepareBitmapForVision(source: Bitmap, maxDim: Int): Bitmap {
        val softwareSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            source.config == Bitmap.Config.HARDWARE
        ) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }

        val width = softwareSource.width
        val height = softwareSource.height
        if (width <= maxDim && height <= maxDim) return softwareSource

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width >= height) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        val scaled = Bitmap.createScaledBitmap(softwareSource, targetWidth, targetHeight, true)
        if (softwareSource !== source && softwareSource !== scaled) {
            softwareSource.recycle()
        }
        return scaled
    }
}
