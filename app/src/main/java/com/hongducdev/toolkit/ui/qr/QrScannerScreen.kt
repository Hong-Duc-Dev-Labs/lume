package com.hongducdev.toolkit.ui.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hongducdev.toolkit.qr.QrGalleryDecoder
import com.hongducdev.toolkit.qr.QrHistoryRepository
import com.hongducdev.toolkit.qr.QrImageAnalyzer
import com.hongducdev.toolkit.qr.QrResult
import com.hongducdev.toolkit.ui.theme.EssentialsColorTokens
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrScannerScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember { QrHistoryRepository(context.applicationContext) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "Cần cấp quyền Camera để quét mã trực tiếp", Toast.LENGTH_SHORT).show()
            }
        }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    var scannedResult by remember { mutableStateOf<QrResult?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val analyzer =
        remember {
            QrImageAnalyzer { result ->
                scope.launch {
                    HapticUtil.performHeavyHaptic(view)
                    repository.add(result)
                    scannedResult = result
                }
            }
        }

    // Photo gallery picker
    val galleryPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val decoded =
                        withContext(Dispatchers.IO) {
                            QrGalleryDecoder.decodeFromUri(context, uri)
                        }
                    if (decoded != null) {
                        HapticUtil.performHeavyHaptic(view)
                        repository.add(decoded)
                        scannedResult = decoded
                    } else {
                        Toast.makeText(context, "Không tìm thấy mã QR trong ảnh này", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView =
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview =
                            Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                        val imageAnalysis =
                            ImageAnalysis
                                .Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(Dispatchers.Default.asExecutor(), analyzer)
                                }

                        val cameraSelector =
                            CameraSelector
                                .Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                        try {
                            cameraProvider.unbindAll()
                            camera =
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis,
                                )
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = {
                    // Update camera facing if changed
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(it.context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview =
                            Preview.Builder().build().also { prev ->
                                prev.surfaceProvider = it.surfaceProvider
                            }

                        val imageAnalysis =
                            ImageAnalysis
                                .Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(Dispatchers.Default.asExecutor(), analyzer)
                                }

                        val cameraSelector =
                            CameraSelector
                                .Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                        try {
                            cameraProvider.unbindAll()
                            camera =
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis,
                                )
                            camera?.cameraControl?.enableTorch(isTorchOn)
                            camera?.cameraControl?.setLinearZoom((zoomRatio - 1f) / 2f)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(it.context))
                },
            )

            // Viewfinder Canvas Overlay
            ViewfinderOverlay()
        } else {
            // Permission request placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = "Cần quyền truy cập Camera để quét mã QR",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        shape = RoundedCornerShape(ExpressiveRadius.Button),
                    ) {
                        Text("Cấp quyền Camera")
                    }
                }
            }
        }

        // Top Control Bar (Back, Title, Flash, Flip Camera, History)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledIconButton(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onExit()
                },
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                    ),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Trở về")
            }

            Text(
                text = "Quét mã QR",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash button
                FilledIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        isTorchOn = !isTorchOn
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    },
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isTorchOn) EssentialsColorTokens.VibrantOrange else Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(
                        if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = "Đèn Flash",
                    )
                }

                // Switch Camera button
                FilledIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        lensFacing =
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                    },
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(Icons.Rounded.Cameraswitch, contentDescription = "Đổi Camera")
                }

                // History button
                FilledIconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showHistory = true
                    },
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(Icons.Rounded.History, contentDescription = "Lịch sử")
                }
            }
        }

        // Bottom Controls: Zoom level chips + Gallery photo picker
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Zoom Selector Chips (1x, 2x, 3x)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(1f to "1x", 1.8f to "2x", 3f to "3x").forEach { (ratio, label) ->
                    val isSelected = zoomRatio == ratio
                    Box(
                        modifier =
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) EssentialsColorTokens.VibrantBlue else Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    zoomRatio = ratio
                                    camera?.cameraControl?.setLinearZoom((ratio - 1f) / 2f)
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            // Gallery Button
            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    galleryPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(ExpressiveRadius.Pill),
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chọn ảnh từ thư viện", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Scanned QR Result Bottom Sheet
    scannedResult?.let { result ->
        QrResultSheet(
            result = result,
            onDismissRequest = {
                scannedResult = null
                analyzer.resumeScanning()
            },
            onScanAgain = {
                scannedResult = null
                analyzer.resumeScanning()
            },
        )
    }

    // History Bottom Sheet
    if (showHistory) {
        QrHistorySheet(
            onDismissRequest = { showHistory = false },
            onSelectItem = { itemResult ->
                showHistory = false
                scannedResult = itemResult
            },
        )
    }
}

@Composable
private fun ViewfinderOverlay() {
    val density = LocalDensity.current
    val boxSize = 260.dp
    val cornerRadius = 32.dp

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserYFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "laser_y",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val boxSizePx = with(density) { boxSize.toPx() }
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }

        val left = (width - boxSizePx) / 2f
        val top = (height - boxSizePx) / 2.3f
        val right = left + boxSizePx
        val bottom = top + boxSizePx

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cutoutPath =
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, right, bottom),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        ),
                    )
                }

            // Dim everything except the cutout
            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.65f))
            }

            // Draw laser line moving inside the box
            val currentLaserY = top + (bottom - top) * laserYFraction
            val laserBrush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            EssentialsColorTokens.VibrantBlue,
                            EssentialsColorTokens.VibrantBlue,
                            Color.Transparent,
                        ),
                    startX = left,
                    endX = right,
                )
            drawLine(
                brush = laserBrush,
                start = Offset(left + 16f, currentLaserY),
                end = Offset(right - 16f, currentLaserY),
                strokeWidth = 6f,
            )

            // Draw 4 corner reticles
            val strokeWidth = 8f
            val cornerLength = 36f
            val cornerColor = EssentialsColorTokens.VibrantBlue

            // Top-Left
            drawLine(cornerColor, Offset(left, top + cornerRadiusPx), Offset(left, top + cornerRadiusPx + cornerLength), strokeWidth)
            drawLine(cornerColor, Offset(left + cornerRadiusPx, top), Offset(left + cornerRadiusPx + cornerLength, top), strokeWidth)

            // Top-Right
            drawLine(cornerColor, Offset(right, top + cornerRadiusPx), Offset(right, top + cornerRadiusPx + cornerLength), strokeWidth)
            drawLine(cornerColor, Offset(right - cornerRadiusPx, top), Offset(right - cornerRadiusPx - cornerLength, top), strokeWidth)

            // Bottom-Left
            drawLine(cornerColor, Offset(left, bottom - cornerRadiusPx), Offset(left, bottom - cornerRadiusPx - cornerLength), strokeWidth)
            drawLine(cornerColor, Offset(left + cornerRadiusPx, bottom), Offset(left + cornerRadiusPx + cornerLength, bottom), strokeWidth)

            // Bottom-Right
            drawLine(
                cornerColor,
                Offset(right, bottom - cornerRadiusPx),
                Offset(right, bottom - cornerRadiusPx - cornerLength),
                strokeWidth,
            )
            drawLine(
                cornerColor,
                Offset(right - cornerRadiusPx, bottom),
                Offset(right - cornerRadiusPx - cornerLength, bottom),
                strokeWidth,
            )
        }

        // Subtitle prompt below the box
        Text(
            text = "Căn chỉnh mã QR vào trong khung ngắm",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(top = boxSize + 60.dp),
        )
    }
}
