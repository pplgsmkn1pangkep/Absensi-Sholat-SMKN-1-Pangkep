package com.example.ui.components

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ScanGreenBorder
import com.example.ui.theme.ScanGreenPrimary
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Real-time CameraX + ZXing QR/Barcode scanner composable.
 * Renders a live camera preview inside a rounded viewfinder, decodes QR codes from YUV frames,
 * and supports torch toggle and clean lifecycle disposal.
 */
@Composable
fun CameraQrScannerView(
    isScanningActive: Boolean,
    onQrCodeDetected: (String) -> Unit,
    onCameraError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnDetected by rememberUpdatedState(onQrCodeDetected)
    val currentOnError by rememberUpdatedState(onCameraError)
    val currentScanningActive by rememberUpdatedState(isScanningActive)

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Toggle torch whenever state changes
    LaunchedEffect(isTorchOn, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            Log.w("CameraQrScanner", "Torch toggle failed: ${e.message}")
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var boundProvider: ProcessCameraProvider? = null
        var lastScannedValue: String? = null
        var lastScannedTimeMillis = 0L

        val qrAnalyzer = QrImageAnalyzer(
            isActiveProvider = { currentScanningActive },
            onDecoded = { decodedText ->
                val now = System.currentTimeMillis()
                // Debounce identical detections within 1.5s to prevent rapid repeat triggers
                if (decodedText != lastScannedValue || (now - lastScannedTimeMillis) > 1500L) {
                    lastScannedValue = decodedText
                    lastScannedTimeMillis = now
                    mainExecutor.execute {
                        currentOnDetected(decodedText)
                    }
                }
            }
        )

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                boundProvider = cameraProvider

                val preview = Preview.Builder()
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, qrAnalyzer) }

                val cameraSelector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }

                if (cameraSelector == null) {
                    currentOnError("Kamera tidak ditemukan pada perangkat ini.")
                    return@Runnable
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                cameraInstance = camera
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()
            } catch (e: Exception) {
                Log.e("CameraQrScanner", "Camera binding failed", e)
                currentOnError("Gagal membuka kamera: ${e.localizedMessage ?: "Perangkat tidak mendukung"}")
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            try {
                if (isTorchOn) {
                    cameraInstance?.cameraControl?.enableTorch(false)
                }
                boundProvider?.unbindAll()
            } catch (e: Exception) {
                Log.w("CameraQrScanner", "Unbind warning: ${e.message}")
            }
            cameraExecutor.shutdown()
        }
    }

    // Animated horizontal scan laser line
    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(2.5.dp, ScanGreenBorder, RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Subtle animated scan line when actively scanning
        if (isScanningActive) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val travelPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .offset {
                            IntOffset(0, (laserProgress * travelPx).roundToInt())
                        }
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    ScanGreenBorder,
                                    Color.White,
                                    ScanGreenBorder,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Top-left live status badge
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = if (isScanningActive) "● KAMERA AKTIF" else "⏸ TERJEDA",
                color = if (isScanningActive) ScanGreenBorder else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Top-right flash/torch toggle if hardware has flash
        if (hasFlashUnit) {
            IconButton(
                onClick = { isTorchOn = !isTorchOn },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTorchOn) ScanGreenPrimary else Color.Black.copy(alpha = 0.6f)
                    )
                    .testTag("toggle_torch_button")
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (isTorchOn) "Matikan Lampu Senter" else "Nyalakan Lampu Senter",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private class QrImageAnalyzer(
    private val isActiveProvider: () -> Boolean,
    private val onDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (!isActiveProvider()) return

            val plane = imageProxy.planes.firstOrNull() ?: return
            val width = imageProxy.width
            val height = imageProxy.height
            if (width <= 0 || height <= 0) return

            val yBytes = extractContiguousYPlane(plane.buffer, width, height, plane.rowStride)
            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            // Try HybridBinarizer first, then GlobalHistogramBinarizer for tricky projector glare
            val decoded = tryDecode(BinaryBitmap(HybridBinarizer(source)))
                ?: tryDecode(BinaryBitmap(GlobalHistogramBinarizer(source)))

            if (!decoded.isNullOrBlank()) {
                onDecoded(decoded)
            }
        } catch (e: Exception) {
            Log.w("QrImageAnalyzer", "Frame analyze error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun tryDecode(bitmap: BinaryBitmap): String? {
        return try {
            reader.decodeWithState(bitmap).text
        } catch (_: NotFoundException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun extractContiguousYPlane(
        buffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int
    ): ByteArray {
        buffer.rewind()
        if (rowStride == width) {
            val bytes = ByteArray(width * height)
            val available = minOf(bytes.size, buffer.remaining())
            buffer.get(bytes, 0, available)
            return bytes
        }
        val bytes = ByteArray(width * height)
        val rowBuffer = ByteArray(rowStride)
        for (row in 0 until height) {
            val remaining = buffer.remaining()
            if (remaining <= 0) break
            val toRead = minOf(rowStride, remaining)
            buffer.get(rowBuffer, 0, toRead)
            val copyLen = minOf(width, toRead)
            System.arraycopy(rowBuffer, 0, bytes, row * width, copyLen)
        }
        return bytes
    }
}
