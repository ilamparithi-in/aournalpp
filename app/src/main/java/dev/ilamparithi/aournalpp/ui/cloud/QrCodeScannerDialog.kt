package dev.ilamparithi.aournalpp.ui.cloud

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import java.util.concurrent.Executors

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

@Composable
fun QrCodeScannerDialog(
    title: String = "Scan Nextcloud QR Code",
    description: String = "Align the QR code from Nextcloud Security Settings inside the frame",
    onQrCodeScanned: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onDismissRequest()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            AppDialogDefaults.makeDialogFullscreenEdgeToEdge(view)
            onDispose {}
        }

        BackHandler(onBack = onDismissRequest)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            var cameraInstance by remember { mutableStateOf<Camera?>(null) }
            var isTorchOn by remember { mutableStateOf(false) }

            // Fullscreen Camera Preview
            CameraPreviewScanner(
                onQrCodeDetected = { rawText ->
                    onQrCodeScanned(rawText)
                    onDismissRequest()
                },
                onCameraBound = { camera -> cameraInstance = camera }
            )

            // Semi-transparent scrim with transparent cutout hole over viewport
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                val frameDimension = minOf(size.width * 0.75f, 290.dp.toPx(), size.height * 0.45f)
                val left = (size.width - frameDimension) / 2f
                val top = (size.height - frameDimension) / 2f

                // Translucent dark scrim
                drawRect(color = Color.Black.copy(alpha = 0.58f))

                // Transparent viewfinder punchout
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(frameDimension, frameDimension),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }

            // Target Scanning Viewfinder Frame & Animated Laser
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(290.dp)
                    .border(2.5.dp, primaryColor.copy(alpha = 0.85f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Animated Scanning Line
                val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
                val scanProgress by infiniteTransition.animateFloat(
                    initialValue = 0.05f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scanProgress"
                )

                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    val y = size.height * scanProgress
                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3.5f
                    )
                }
            }

            // Floating Top App Bar Overlay
            val torchTitle = stringResource(R.string.cd_toggle_torch)
            val stateEnabled = stringResource(R.string.state_enabled)
            val stateDisabled = stringResource(R.string.state_disabled)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.a11yHeading()
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White
                ) {
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraInstance?.cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .minTouchTarget()
                            .semantics {
                                role = Role.Switch
                                stateDescription = if (isTorchOn) stateEnabled else stateDisabled
                                this.contentDescription = torchTitle
                            }
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = null,
                            tint = if (isTorchOn) primaryColor else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Floating Bottom Instruction Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.70f),
                contentColor = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                            .union(WindowInsets.systemGestures)
                            .only(WindowInsetsSides.Bottom)
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.nextcloud_qr_settings_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreviewScanner(
    onQrCodeDetected: (String) -> Unit,
    onCameraBound: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var isDetected = false
                val reader = MultiFormatReader()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isDetected) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val rawResult = processImageProxy(imageProxy, reader)
                    if (rawResult != null && !isDetected) {
                        isDetected = true
                        previewView.post {
                            onQrCodeDetected(rawResult)
                        }
                    }
                    imageProxy.close()
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraBound(camera)
                } catch (e: Exception) {
                    android.util.Log.e("QrCodeScanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun processImageProxy(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    val planes = imageProxy.planes
    if (planes.isEmpty()) return null

    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = imageProxy.width
    val height = imageProxy.height

    val source = PlanarYUVLuminanceSource(
        bytes,
        width,
        height,
        0,
        0,
        width,
        height,
        false
    )
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    return try {
        val result = reader.decodeWithState(binaryBitmap)
        reader.reset()
        result.text
    } catch (_: Exception) {
        reader.reset()
        null
    }
}
