package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val REST_TAG = "GeminiRestClient"
private const val GEMINI_REST_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

/**
 * Lightweight direct Gemini REST client using the official v1beta `generateContent` endpoint
 * with the `X-goog-api-key` header (matching standard Gemini API key integration).
 * Supports plain text, structured JSON responses, images (Bitmap -> JPEG), and binary inline data
 * (PDF, audio, text).
 */
internal object GeminiRestClient {

    suspend fun generateContentText(
        apiKey: String,
        modelName: String,
        systemInstruction: String?,
        promptText: String,
        imageBitmap: Bitmap? = null,
        inlineBytes: ByteArray? = null,
        inlineMimeType: String? = null,
        expectJson: Boolean = false,
        jsonSchemaHint: String? = null,
        timeoutMs: Long = 45_000L
    ): String = withContext(Dispatchers.IO) {
        val endpoint = "$GEMINI_REST_BASE_URL/$modelName:generateContent"
        val effectivePrompt = if (expectJson && !jsonSchemaHint.isNullOrBlank()) {
            "$promptText\n\nReturn ONLY a valid JSON object matching this exact structure (no markdown fences, no extra commentary):\n$jsonSchemaHint"
        } else {
            promptText
        }

        val payload = buildRequestPayload(
            systemInstruction = systemInstruction,
            promptText = effectivePrompt,
            imageBitmap = imageBitmap,
            inlineBytes = inlineBytes,
            inlineMimeType = inlineMimeType,
            expectJson = expectJson
        )

        val rawResponse = postJson(
            endpoint = endpoint,
            apiKey = apiKey,
            payload = payload,
            timeoutMs = timeoutMs
        )

        val extracted = extractResponseText(rawResponse)
        if (expectJson) stripJsonCodeFences(extracted) else extracted.trim()
    }

    private fun buildRequestPayload(
        systemInstruction: String?,
        promptText: String,
        imageBitmap: Bitmap?,
        inlineBytes: ByteArray?,
        inlineMimeType: String?,
        expectJson: Boolean
    ): JSONObject {
        val root = JSONObject()

        if (!systemInstruction.isNullOrBlank()) {
            val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
            root.put("systemInstruction", JSONObject().put("parts", sysParts))
        }

        val userParts = JSONArray()

        if (imageBitmap != null) {
            val jpegStream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, jpegStream)
            val base64Image = Base64.encodeToString(jpegStream.toByteArray(), Base64.NO_WRAP)
            userParts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", base64Image)
                )
            )
        }

        if (inlineBytes != null && !inlineMimeType.isNullOrBlank()) {
            val base64Data = Base64.encodeToString(inlineBytes, Base64.NO_WRAP)
            userParts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", inlineMimeType)
                        .put("data", base64Data)
                )
            )
        }

        userParts.put(JSONObject().put("text", promptText))

        val contentObj = JSONObject()
            .put("role", "user")
            .put("parts", userParts)
        root.put("contents", JSONArray().put(contentObj))

        if (expectJson) {
            root.put(
                "generationConfig",
                JSONObject().put("responseMimeType", "application/json")
            )
        }

        return root
    }

    private fun postJson(
        endpoint: String,
        apiKey: String,
        payload: JSONObject,
        timeoutMs: Long
    ): String {
        val timeoutInt = timeoutMs.coerceIn(5_000L, 120_000L).toInt()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            useCaches = false
            connectTimeout = timeoutInt.coerceAtMost(20_000)
            readTimeout = timeoutInt
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("X-goog-api-key", apiKey)
        }

        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }

            if (statusCode !in 200..299) {
                val apiError = parseErrorMessage(body, statusCode)
                Log.e(REST_TAG, "Gemini REST error HTTP $statusCode: $apiError")
                throw GeminiRestHttpException(statusCode, apiError)
            }

            body
        } finally {
            connection.disconnect()
        }
    }

    private fun extractResponseText(rawJson: String): String {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            val detail = if (blockReason.isNotBlank()) "Blocked ($blockReason)." else "No candidates returned by Gemini API."
            throw IllegalStateException(detail)
        }

        val firstCandidate = candidates.optJSONObject(0)
        val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IllegalStateException("Gemini response contained no content parts.")

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val text = part.optString("text", "")
            if (text.isNotEmpty()) {
                sb.append(text)
            }
        }
        val combined = sb.toString().trim()
        if (combined.isEmpty()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
        return combined
    }

    private fun parseErrorMessage(errorBody: String, statusCode: Int): String {
        val parsed = runCatching {
            JSONObject(errorBody).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return when {
            statusCode == 401 || statusCode == 403 ->
                if (parsed.isNotBlank()) "Gemini API key error ($statusCode): $parsed"
                else "Gemini API key was rejected (HTTP $statusCode). Please verify GEMINI_API_KEY in AI Studio Secrets."
            statusCode == 429 ->
                if (parsed.isNotBlank()) "Gemini API rate limit reached: $parsed"
                else "Gemini API quota/rate limit reached (HTTP 429). Please wait a moment and retry."
            parsed.isNotBlank() -> "Gemini API error ($statusCode): $parsed"
            else -> "Gemini API request failed with HTTP $statusCode."
        }
    }

    private fun stripJsonCodeFences(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            val firstNewline = trimmed.indexOf('\n')
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1)
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.dropLast(3)
            }
            trimmed = trimmed.trim()
        }
        // If any leading/trailing prose slipped around the JSON object, slice cleanly from { to }
        if (!trimmed.startsWith("{")) {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start != -1 && end > start) {
                trimmed = trimmed.substring(start, end + 1)
            }
        }
        return trimmed
    }
}

internal class GeminiRestHttpException(
    val statusCode: Int,
    message: String
) : IllegalStateException(message)
