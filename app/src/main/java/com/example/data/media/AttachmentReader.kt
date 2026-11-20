package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

const val MAX_ATTACHMENT_BYTES = 8 * 1024 * 1024 // 8 MB safety cap

data class LoadedAttachment(
    val fileName: String,
    val mimeType: String,
    val attachmentType: String, // IMAGE, FILE, AUDIO
    val bytes: ByteArray?,
    val bitmap: Bitmap?,
    val tooLarge: Boolean = false
)

suspend fun readAttachmentFromUri(
    context: Context,
    uri: Uri,
    preferredType: String? = null
): LoadedAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val displayName = resolveDisplayName(context, uri)
    val mimeType = resolver.getType(uri)
        ?: inferMimeFromFileName(displayName)
        ?: when (preferredType) {
            "IMAGE" -> "image/jpeg"
            "AUDIO" -> "audio/mp4"
            else -> "application/octet-stream"
        }

    val resolvedType = preferredType ?: when {
        mimeType.startsWith("image/") -> "IMAGE"
        mimeType.startsWith("audio/") -> "AUDIO"
        else -> "FILE"
    }

    if (resolvedType == "IMAGE") {
        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(resolver, uri)
            }
        } catch (_: Exception) {
            null
        }
        return@withContext if (bitmap != null) {
            LoadedAttachment(
                fileName = displayName,
                mimeType = mimeType,
                attachmentType = "IMAGE",
                bytes = null,
                bitmap = bitmap
            )
        } else {
            null
        }
    }

    // Read raw bytes for PDF, text, audio, or other documents with an 8 MB cap
    val stream = resolver.openInputStream(uri) ?: return@withContext null
    stream.use { input ->
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(chunk)
            if (read <= 0) break
            total += read
            if (total > MAX_ATTACHMENT_BYTES) {
                return@withContext LoadedAttachment(
                    fileName = displayName,
                    mimeType = mimeType,
                    attachmentType = resolvedType,
                    bytes = null,
                    bitmap = null,
                    tooLarge = true
                )
            }
            buffer.write(chunk, 0, read)
        }
        LoadedAttachment(
            fileName = displayName,
            mimeType = mimeType,
            attachmentType = resolvedType,
            bytes = buffer.toByteArray(),
            bitmap = null
        )
    }
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
    try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
    } catch (_: Exception) {
        // Fall through to lastPathSegment
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "uploaded_file" } ?: "uploaded_file"
}

private fun inferMimeFromFileName(name: String): String? {
    val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
    return when (ext) {
        "pdf" -> "application/pdf"
        "txt", "csv", "md" -> "text/plain"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "aac" -> "audio/aac"
        "amr" -> "audio/amr"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> null
    }
}
