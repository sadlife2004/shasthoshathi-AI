package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.ui.theme.TealLight

private const val CAMERAX_TAG = "PrescriptionCameraX"

@Composable
fun PrescriptionCameraScanner(
    language: AppLanguage,
    onBitmapCaptured: (Bitmap) -> Unit,
    onOpenGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasFlash by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider

                    val selector = when {
                        provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                            CameraSelector.DEFAULT_BACK_CAMERA
                        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> null
                    }

                    if (selector == null) {
                        cameraError = strings.cameraUnavailableError
                        return@addListener
                    }

                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        capture
                    )
                    boundCamera = camera
                    imageCapture = capture
                    hasFlash = camera.cameraInfo.hasFlashUnit()
                    cameraError = null
                } catch (e: Exception) {
                    Log.e(CAMERAX_TAG, "Failed to bind CameraX use cases", e)
                    cameraError = e.localizedMessage ?: strings.cameraUnavailableError
                }
            },
            mainExecutor
        )

        onDispose {
            try {
                boundCamera?.cameraControl?.enableTorch(false)
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.w(CAMERAX_TAG, "Error unbinding CameraX on dispose", e)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("camerax_scanner_view")
        ) {
            // 1. Live CameraX Preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Prescription Viewfinder Corner Brackets Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameWidth = size.width * 0.84f
                val frameHeight = size.height * 0.54f
                val left = (size.width - frameWidth) / 2f
                val top = (size.height - frameHeight) / 2f - size.height * 0.03f
                val right = left + frameWidth
                val bottom = top + frameHeight
                val cornerLen = 36.dp.toPx()
                val strokeWidth = 4.dp.toPx()
                val bracketColor = TealLight

                // Top-left
                drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth, StrokeCap.Round)
                drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth, StrokeCap.Round)
                // Top-right
                drawLine(bracketColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth, StrokeCap.Round)
                drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth, StrokeCap.Round)
                // Bottom-left
                drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth, StrokeCap.Round)
                drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth, StrokeCap.Round)
                // Bottom-right
                drawLine(bracketColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth, StrokeCap.Round)
                drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth, StrokeCap.Round)
            }

            // 3. Top Controls Bar (Close, Safety Guidance Pill, Torch Toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("camerax_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = strings.cancel,
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "শুধুমাত্র নির্দেশনা • রোগ নির্ণয় নয়"
                        else "Guidance only — Not diagnostic",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (hasFlash) {
                    IconButton(
                        onClick = {
                            val next = !torchEnabled
                            torchEnabled = next
                            boundCamera?.cameraControl?.enableTorch(next)
                            imageCapture?.flashMode = if (next) {
                                ImageCapture.FLASH_MODE_ON
                            } else {
                                ImageCapture.FLASH_MODE_OFF
                            }
                        },
                        modifier = Modifier.testTag("camerax_flash_toggle")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Toggle flash",
                            tint = if (torchEnabled) Color(0xFFFBBF24) else Color.White
                        )
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }

            // 4. Error Banner if camera failed or capture errored
            if (!cameraError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = cameraError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // 5. Bottom Hint & Shutter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.cameraAlignHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick switch to Gallery Photo Picker
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isCapturing) {
                                onDismiss()
                                onOpenGallery()
                            }
                            .testTag("camerax_gallery_shortcut"),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = strings.choosePhotoBtn,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Primary Circular Shutter Capture Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isCapturing) TealLight else Color.White)
                            .semantics(mergeDescendants = true) {
                                contentDescription = strings.capturePhotoLabel
                            }
                            .clickable(enabled = !isCapturing && imageCapture != null && cameraError == null) {
                                val capture = imageCapture ?: return@clickable
                                isCapturing = true
                                cameraError = null
                                val mainExecutor = ContextCompat.getMainExecutor(context)
                                capture.takePicture(
                                    mainExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val uprightBitmap = try {
                                                image.toUprightBitmap()
                                            } catch (e: Exception) {
                                                Log.e(CAMERAX_TAG, "Failed to decode ImageProxy", e)
                                                null
                                            } finally {
                                                image.close()
                                            }
                                            isCapturing = false
                                            if (uprightBitmap != null) {
                                                onBitmapCaptured(uprightBitmap)
                                            } else {
                                                cameraError = strings.cameraCaptureError
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e(CAMERAX_TAG, "ImageCapture failed", exception)
                                            isCapturing = false
                                            cameraError = exception.localizedMessage ?: strings.cameraCaptureError
                                        }
                                    }
                                )
                            }
                            .testTag("camerax_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    // Symmetrical spacer to keep shutter perfectly centered
                    Spacer(Modifier.size(50.dp))
                }
            }
        }
    }
}

private fun ImageProxy.toUprightBitmap(maxDim: Int = 1440): Bitmap {
    val rawBitmap = this.toBitmap()
    val rotationDegrees = this.imageInfo.rotationDegrees
    val maxSide = maxOf(rawBitmap.width, rawBitmap.height)
    val scale = if (maxSide > maxDim) maxDim.toFloat() / maxSide.toFloat() else 1f
    if (rotationDegrees == 0 && scale >= 1f) return rawBitmap
    val matrix = Matrix().apply {
        if (scale < 1f) postScale(scale, scale)
        if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
    }
    val transformed = Bitmap.createBitmap(
        rawBitmap,
        0,
        0,
        rawBitmap.width,
        rawBitmap.height,
        matrix,
        true
    )
    if (transformed !== rawBitmap) {
        rawBitmap.recycle()
    }
    return transformed
}
