package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.example.qr.engine.PayloadKind
import com.example.qr.engine.QrContentParser
import com.example.qr.engine.QrParsedAction
import com.example.qr.engine.QrScannabilityEvaluator
import com.example.ui.StudioViewModel
import com.example.ui.theme.BgDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

data class BatchScanItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val count: Int = 1,
    val thumbnail: Bitmap? = null
)

@Composable
fun ScannerScreen(
    viewModel: StudioViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Modes & Batch Scanning States
    var isBatchMode by remember { mutableStateOf(false) }
    val batchItems = remember { mutableStateListOf<BatchScanItem>() }
    var isBatchPaused by remember { mutableStateOf(false) }
    var showBatchSheet by remember { mutableStateOf(false) }
    var ignoreBatchDuplicates by remember { mutableStateOf(true) }
    var batchFlashPulse by remember { mutableStateOf(false) }
    var lastBatchText by remember { mutableStateOf<String?>(null) }
    var lastBatchTimestamp by remember { mutableStateOf(0L) }
    var lastDuplicateNoticeTime by remember { mutableStateOf(0L) }
    var duplicateNotice by remember { mutableStateOf<String?>(null) }
    var duplicateNoticeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var multiPhotoProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    val isCameraScanningPaused by rememberUpdatedState(isBatchMode && isBatchPaused)

    var scannedResult by remember { mutableStateOf<String?>(null) }
    var scannedPhotoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzingPhoto by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    val vibrator = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
        } catch (_: Exception) { null }
    }

    fun triggerFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(55L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(55L)
            }
        } catch (_: Exception) {}
    }

    fun handleScannedCode(text: String, thumbnail: Bitmap? = null, fromLiveCamera: Boolean = true) {
        if (!isBatchMode) {
            if (text != scannedResult) {
                scannedResult = text
                scannedPhotoThumbnail = thumbnail
                viewModel.recordScannedQr(text)
                triggerFeedback()
            }
        } else {
            // If live camera scanning is paused, ignore incoming frames
            if (fromLiveCamera && isBatchPaused) return

            val now = System.currentTimeMillis()
            val alreadyInBatch = batchItems.any { it.text == text }

            if (ignoreBatchDuplicates && alreadyInBatch) {
                // Completely ignore duplicate: do NOT re-add, do NOT vibrate, do NOT record in history, do NOT flash green!
                if (fromLiveCamera && (now - lastDuplicateNoticeTime > 1500L)) {
                    lastDuplicateNoticeTime = now
                    duplicateNoticeJob?.cancel()
                    duplicateNotice = "Duplicate ignored (Already scanned)"
                    duplicateNoticeJob = coroutineScope.launch {
                        kotlinx.coroutines.delay(1800L)
                        duplicateNotice = null
                    }
                }
                return
            }

            // If deduplication is OFF, avoid continuous rapid buzzing while pointing at the same physical code
            if (fromLiveCamera && text == lastBatchText && (now - lastBatchTimestamp) < 3500L) {
                return
            }

            // Fresh valid QR code scanned
            lastBatchText = text
            lastBatchTimestamp = now
            viewModel.recordScannedQr(text)
            triggerFeedback()

            val existingIdx = batchItems.indexOfFirst { it.text == text }
            if (existingIdx >= 0) {
                val item = batchItems[existingIdx]
                batchItems[existingIdx] = item.copy(count = item.count + 1)
            } else {
                batchItems.add(0, BatchScanItem(text = text, thumbnail = thumbnail))
            }

            batchFlashPulse = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(350L)
                batchFlashPulse = false
            }
        }
    }

    // Pick single image from gallery to scan
    val pickPhotoForScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isAnalyzingPhoto = true
            scanErrorMessage = null
            scannedResult = null
            scannedPhotoThumbnail = null
            coroutineScope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    decodeQrFromGalleryUri(context, uri)
                }
                isAnalyzingPhoto = false
                if (decoded != null) {
                    handleScannedCode(decoded.text, decoded.thumbnail, fromLiveCamera = false)
                    if (isBatchMode) {
                        showBatchSheet = true
                    }
                } else {
                    scanErrorMessage = "No QR code could be found in the selected photo. Please make sure the QR code is clearly visible, in focus, and well-lit."
                }
            }
        }
    }

    // Pick multiple images from gallery for batch scan
    val pickMultiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isAnalyzingPhoto = true
            scanErrorMessage = null
            coroutineScope.launch {
                var foundCount = 0
                uris.forEachIndexed { idx, uri ->
                    multiPhotoProgress = Pair(idx + 1, uris.size)
                    val decoded = withContext(Dispatchers.IO) {
                        decodeQrFromGalleryUri(context, uri)
                    }
                    if (decoded != null) {
                        foundCount++
                        withContext(Dispatchers.Main) {
                            handleScannedCode(decoded.text, decoded.thumbnail, fromLiveCamera = false)
                        }
                    }
                }
                isAnalyzingPhoto = false
                multiPhotoProgress = null
                if (foundCount > 0) {
                    Toast.makeText(context, "Scanned $foundCount QR code(s) from selected photos", Toast.LENGTH_SHORT).show()
                    showBatchSheet = true
                } else {
                    scanErrorMessage = "No QR codes found in the ${uris.size} selected photo(s)."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(BgDark)) {
        if (hasCameraPermission) {
            // Live Camera View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val reader = MultiFormatReader()
                        val hints = mapOf<DecodeHintType, Any>(
                            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                            DecodeHintType.TRY_HARDER to true,
                            DecodeHintType.CHARACTER_SET to "UTF-8"
                        )
                        reader.setHints(hints)
                        var lastAnalysisTime = 0L

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            // When user pauses batch scanning, immediately close frame without CPU heavy processing
                            if (isCameraScanningPaused) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val currentTime = System.currentTimeMillis()
                            // 60ms cadence: fast, responsive scanning matching native camera apps
                            if (currentTime - lastAnalysisTime >= 60L) {
                                lastAnalysisTime = currentTime
                                val buffer = imageProxy.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val origW = imageProxy.width
                                val origH = imageProxy.height
                                val rotation = imageProxy.imageInfo.rotationDegrees

                                // Rotate buffer according to camera sensor rotation
                                val (rotatedBytes, dims) = rotateYuvSensorBuffer(bytes, origW, origH, rotation)
                                val w = dims.first
                                val h = dims.second

                                // Center viewfinder crop (72% center crop where user is pointing)
                                val cropSize = (minOf(w, h) * 0.72f).toInt()
                                val cropX = (w - cropSize) / 2
                                val cropY = (h - cropSize) / 2

                                var decodedText: String? = null

                                // Pass 1: Center Crop with HybridBinarizer (Ultra-fast primary pass)
                                try {
                                    val centerSource = PlanarYUVLuminanceSource(
                                        rotatedBytes, w, h, cropX, cropY, cropSize, cropSize, false
                                    )
                                    val res = reader.decodeWithState(BinaryBitmap(HybridBinarizer(centerSource)))
                                    decodedText = res.text
                                } catch (_: Exception) {
                                    reader.reset()
                                }

                                // Pass 2: Center Crop Inverted (Dark mode / light modules on dark background)
                                if (decodedText.isNullOrEmpty()) {
                                    try {
                                        val centerSource = PlanarYUVLuminanceSource(
                                            rotatedBytes, w, h, cropX, cropY, cropSize, cropSize, false
                                        )
                                        val res = reader.decodeWithState(BinaryBitmap(HybridBinarizer(centerSource.invert())))
                                        decodedText = res.text
                                    } catch (_: Exception) {
                                        reader.reset()
                                    }
                                }

                                // Pass 3: Center Crop GlobalHistogramBinarizer (Soft lighting, shadows, gradients)
                                if (decodedText.isNullOrEmpty()) {
                                    try {
                                        val centerSource = PlanarYUVLuminanceSource(
                                            rotatedBytes, w, h, cropX, cropY, cropSize, cropSize, false
                                        )
                                        val res = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(centerSource)))
                                        decodedText = res.text
                                    } catch (_: Exception) {
                                        reader.reset()
                                    }
                                }

                                // Pass 4: Full Frame HybridBinarizer (Codes held at edges of screen)
                                if (decodedText.isNullOrEmpty()) {
                                    try {
                                        val fullSource = PlanarYUVLuminanceSource(
                                            rotatedBytes, w, h, 0, 0, w, h, false
                                        )
                                        val res = reader.decodeWithState(BinaryBitmap(HybridBinarizer(fullSource)))
                                        decodedText = res.text
                                    } catch (_: Exception) {
                                        reader.reset()
                                    }
                                }

                                // Pass 5: Full Frame Inverted
                                if (decodedText.isNullOrEmpty()) {
                                    try {
                                        val fullSource = PlanarYUVLuminanceSource(
                                            rotatedBytes, w, h, 0, 0, w, h, false
                                        )
                                        val res = reader.decodeWithState(BinaryBitmap(HybridBinarizer(fullSource.invert())))
                                        decodedText = res.text
                                    } catch (_: Exception) {
                                        reader.reset()
                                    }
                                }

                                if (!decodedText.isNullOrEmpty()) {
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        handleScannedCode(decodedText, null, fromLiveCamera = true)
                                    }
                                }
                            }
                            imageProxy.close()
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                ctx as androidx.lifecycle.LifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Viewfinder HUD Reticle Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val reticleBorderColor = when {
                    isBatchMode && isBatchPaused -> Color(0xFFF59E0B)
                    batchFlashPulse -> EmeraldGreen
                    isBatchMode -> NeonViolet
                    else -> ElectricCyan
                }
                val reticleBorderWidth = if (batchFlashPulse) 3.dp else 2.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f)
                        .border(reticleBorderWidth, reticleBorderColor, RoundedCornerShape(24.dp))
                        .background(
                            when {
                                isBatchMode && isBatchPaused -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                                batchFlashPulse -> EmeraldGreen.copy(alpha = 0.15f)
                                else -> Color.Black.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Top tag of reticle for Batch Mode
                    if (isBatchMode) {
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                            color = (if (isBatchPaused) Color(0xFFF59E0B) else if (batchFlashPulse) EmeraldGreen else NeonViolet).copy(alpha = 0.95f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isBatchPaused) Icons.Default.Pause else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBatchPaused) "SCANNER PAUSED" else "${batchItems.size} CODES SCANNED",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Center Resume Card inside Viewfinder when Paused
                    if (isBatchMode && isBatchPaused) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.88f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Scanner Paused",
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { isBatchPaused = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Duplicate Ignored HUD Notification Badge
                    if (duplicateNotice != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.95f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(duplicateNotice!!, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        } else {
            // Permission Fallback Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = ElectricCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera Access Required", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Allow camera access to test-scan QR codes or choose an image from your device photos.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Camera", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Scanner Bar: Mode Switcher + Controls (Pause, Torch, Gallery, Close)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Switcher: Single vs Batch (Compact & Sleek)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Single Mode Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isBatchMode) ElectricCyan else Color.Transparent)
                            .clickable {
                                isBatchMode = false
                                scannedResult = null
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Single",
                            color = if (!isBatchMode) Color.Black else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Batch Mode Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isBatchMode) NeonViolet else Color.Transparent)
                            .clickable {
                                isBatchMode = true
                                scannedResult = null
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isBatchMode) Color.White else TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Batch",
                                color = if (isBatchMode) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (batchItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isBatchMode) Color.White else NeonViolet)
                                        .size(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${batchItems.size}",
                                        color = if (isBatchMode) NeonViolet else Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Action Buttons: Pause/Resume (in Batch Mode), Torch, Gallery, Close
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume in Batch Mode (Compact 34dp Button)
                if (isBatchMode) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isBatchPaused) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, if (isBatchPaused) Color(0xFFF59E0B) else CardBorder, CircleShape)
                            .clickable { isBatchPaused = !isBatchPaused },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBatchPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isBatchPaused) "Resume Scanner" else "Pause Scanner",
                            tint = if (isBatchPaused) Color.Black else Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Torch toggle (Compact 34dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) ElectricCyan else Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, if (isTorchOn) ElectricCyan else CardBorder, CircleShape)
                        .clickable {
                            val nextTorch = !isTorchOn
                            cameraControl?.enableTorch(nextTorch)
                            isTorchOn = nextTorch
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) Color.Black else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Gallery Pick (Compact 34dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, CardBorder, CircleShape)
                        .clickable {
                            if (isBatchMode) {
                                pickMultiplePhotosLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                pickPhotoForScanLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBatchMode) Icons.Default.PhotoLibrary else Icons.Default.Image,
                        contentDescription = if (isBatchMode) "Batch Pick Photos" else "Scan Single Image",
                        tint = if (isBatchMode) NeonViolet else ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Close Camera button (Compact 34dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, CardBorder, CircleShape)
                        .clickable { onNavigateToStudio() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Analyzing Photo from Gallery Overlay (Supports single & batch progress)
        AnimatedVisibility(
            visible = isAnalyzingPhoto,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardDark.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = ElectricCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = if (multiPhotoProgress != null) "Batch Decoding Photos..." else "Analyzing Photo...",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (multiPhotoProgress != null)
                            "Processing photo ${multiPhotoProgress!!.first} of ${multiPhotoProgress!!.second}"
                        else "Searching for QR code in selected image",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Photo Scan Error Message Sheet
        AnimatedVisibility(
            visible = scanErrorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp)),
                color = CardDark
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("No QR Code Found", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick = { scanErrorMessage = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = scanErrorMessage ?: "",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                scanErrorMessage = null
                                pickPhotoForScanLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Another Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Bottom Detection Card Sheet (Single Mode Only)
        AnimatedVisibility(
            visible = !isBatchMode && scannedResult != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val text = scannedResult ?: ""
            val parsedAction = remember(text) { QrContentParser.parse(text) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.dp,
                        if (parsedAction.isLocation) ElectricCyan else EmeraldGreen,
                        RoundedCornerShape(22.dp)
                    ),
                color = CardDark
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header: Status indicator + Badge + Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (parsedAction.isLocation) ElectricCyan else EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (parsedAction.isLocation) "Location QR Detected" else "QR Code Scanned",
                                color = if (parsedAction.isLocation) ElectricCyan else EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (parsedAction.isLocation) ElectricCyan.copy(alpha = 0.18f)
                                        else EmeraldGreen.copy(alpha = 0.18f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = parsedAction.badgeLabel,
                                    color = if (parsedAction.isLocation) ElectricCyan else EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                scannedResult = null
                                scannedPhotoThumbnail = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Body: Thumbnail or Icon + Title + Subtitle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scannedPhotoThumbnail != null) {
                            Image(
                                bitmap = scannedPhotoThumbnail!!.asImageBitmap(),
                                contentDescription = "Scanned Photo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (parsedAction.isLocation) ElectricCyan.copy(alpha = 0.15f)
                                        else SurfaceDark
                                    )
                                    .border(
                                        1.dp,
                                        if (parsedAction.isLocation) ElectricCyan.copy(alpha = 0.35f) else CardBorder,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = parsedAction.primaryButtonIcon,
                                    contentDescription = parsedAction.badgeLabel,
                                    tint = if (parsedAction.isLocation) ElectricCyan else TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parsedAction.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = parsedAction.subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Button (Prominent, High-Contrast)
                    Button(
                        onClick = {
                            QrContentParser.openPrimaryAction(context, text, parsedAction) { toastMsg ->
                                clipboardManager.setText(AnnotatedString(parsedAction.copyableText))
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (parsedAction.isLocation) ElectricCyan else EmeraldGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("scanner_primary_action_button")
                    ) {
                        Icon(
                            imageVector = parsedAction.primaryButtonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = parsedAction.primaryButtonLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Action Buttons Row: Remake in Studio + Copy + Done
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Remake in Studio Button
                        Button(
                            onClick = {
                                viewModel.onScannedFromCamera(text)
                                onNavigateToStudio()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceDark,
                                contentColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Remake", modifier = Modifier.size(13.dp), tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remake", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = ElectricCyan)
                        }

                        // Copy Action Button
                        FilledTonalButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(parsedAction.copyableText))
                                Toast.makeText(context, "Copied: ${parsedAction.copyableText.take(30)}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceDark,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = parsedAction.secondaryButtonIcon,
                                contentDescription = "Copy",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(parsedAction.secondaryButtonLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }

                        // Stop Camera / Done
                        FilledTonalButton(
                            onClick = onNavigateToStudio,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceDark,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(36.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Single Mode Guidance Pill (When waiting to scan)
        AnimatedVisibility(
            visible = !isBatchMode && scannedResult == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 26.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardDark.copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Align QR code inside frame to scan",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Floating Batch Queue Bar (Visible in Batch Mode when sheet is collapsed)
        AnimatedVisibility(
            visible = isBatchMode && !showBatchSheet,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        when {
                            isBatchPaused -> Color(0xFFF59E0B)
                            batchFlashPulse -> EmeraldGreen
                            else -> CardBorder
                        },
                        RoundedCornerShape(18.dp)
                    ),
                color = CardDark.copy(alpha = 0.95f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Queue indicator + text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showBatchSheet = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isBatchPaused -> Color(0xFFF59E0B).copy(alpha = 0.22f)
                                            batchFlashPulse -> EmeraldGreen.copy(alpha = 0.25f)
                                            else -> NeonViolet.copy(alpha = 0.20f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBatchPaused) Icons.Default.Pause else Icons.Default.Layers,
                                    contentDescription = "Batch Queue",
                                    tint = if (isBatchPaused) Color(0xFFF59E0B) else if (batchFlashPulse) EmeraldGreen else NeonViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Batch: ",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${batchItems.size} code${if (batchItems.size == 1) "" else "s"}",
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = if (isBatchPaused) "⏸ Paused" else if (ignoreBatchDuplicates) "Unique only" else "Continuous",
                                    color = if (isBatchPaused) Color(0xFFF59E0B) else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isBatchPaused) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        // Right: Controls (Pause/Resume + Review Button)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Direct Pause/Resume button right on the floating bar!
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBatchPaused) Color(0xFFF59E0B) else SurfaceDark,
                                modifier = Modifier
                                    .border(1.dp, if (isBatchPaused) Color(0xFFF59E0B) else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { isBatchPaused = !isBatchPaused }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isBatchPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isBatchPaused) "Resume" else "Pause",
                                        tint = if (isBatchPaused) Color.Black else ElectricCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (isBatchPaused) "Resume" else "Pause",
                                        color = if (isBatchPaused) Color.Black else TextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Review Drawer Button
                            Button(
                                onClick = { showBatchSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Review (${batchItems.size})", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // Full Batch Review Drawer / Sheet
        AnimatedVisibility(
            visible = isBatchMode && showBatchSheet,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BackHandler {
                showBatchSheet = false
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .border(1.dp, NeonViolet.copy(alpha = 0.4f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = CardDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Drag pill handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(TextMuted.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Batch Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Batch Scanned Codes", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text(
                                    text = "${batchItems.size} code${if (batchItems.size == 1) "" else "s"} • ${batchItems.sumOf { it.count }} captures",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pause / Resume camera scanning
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBatchPaused) Color(0xFFF59E0B) else SurfaceDark,
                                modifier = Modifier
                                    .border(1.dp, if (isBatchPaused) Color(0xFFF59E0B) else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { isBatchPaused = !isBatchPaused }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isBatchPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isBatchPaused) "Resume" else "Pause",
                                        tint = if (isBatchPaused) Color.Black else ElectricCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (isBatchPaused) "Resume" else "Pause",
                                        color = if (isBatchPaused) Color.Black else TextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { showBatchSheet = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Sheet", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Batch Toolbar: Copy All, Share All, Deduplicate, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy All
                        Button(
                            onClick = {
                                if (batchItems.isNotEmpty()) {
                                    val allText = batchItems.joinToString("\n") { it.text }
                                    clipboardManager.setText(AnnotatedString(allText))
                                    Toast.makeText(context, "Copied ${batchItems.size} code(s) to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = batchItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Copy All", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share All
                        FilledTonalButton(
                            onClick = {
                                if (batchItems.isNotEmpty()) {
                                    val allText = batchItems.mapIndexed { idx, it ->
                                        "${idx + 1}. ${it.text}${if (it.count > 1) " (scanned ${it.count}x)" else ""}"
                                    }.joinToString("\n\n")
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, allText)
                                        putExtra(Intent.EXTRA_SUBJECT, "Batch QR Code Scans (${batchItems.size})")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Batch QR Codes"))
                                }
                            },
                            enabled = batchItems.isNotEmpty(),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceDark, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Share", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Deduplicate toggle chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (ignoreBatchDuplicates) NeonViolet.copy(alpha = 0.25f) else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (ignoreBatchDuplicates) NeonViolet else CardBorder),
                            modifier = Modifier
                                .height(30.dp)
                                .clickable {
                                    val nextVal = !ignoreBatchDuplicates
                                    ignoreBatchDuplicates = nextVal
                                    Toast.makeText(
                                        context,
                                        if (nextVal) "Ignore Duplicates: ON" else "Ignore Duplicates: OFF",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ignoreBatchDuplicates) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Text(
                                    text = if (ignoreBatchDuplicates) "Unique: ON" else "All Dups",
                                    color = if (ignoreBatchDuplicates) NeonViolet else TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Clear All
                        IconButton(
                            onClick = { batchItems.clear() },
                            enabled = batchItems.isNotEmpty(),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = if (batchItems.isNotEmpty()) Color(0xFFEF4444) else TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Batch List of Items
                    if (batchItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No QR codes in batch yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Point camera at any QR code, or tap the batch gallery icon to scan multiple saved photos.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(batchItems, key = { it.id }) { item ->
                                val parsed = remember(item.text) { QrContentParser.parse(item.text) }
                                val timeFormat = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
                                val formattedTime = remember(item.timestamp) { timeFormat.format(java.util.Date(item.timestamp)) }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                                    color = SurfaceDark
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header: Badge + Count + Time + Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (parsed.isLocation) ElectricCyan.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = parsed.badgeLabel,
                                                        color = if (parsed.isLocation) ElectricCyan else EmeraldGreen,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (item.count > 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(NeonViolet.copy(alpha = 0.2f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("×${item.count}", color = NeonViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(formattedTime, color = TextMuted, fontSize = 10.sp)
                                            }

                                            IconButton(
                                                onClick = { batchItems.remove(item) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Body: Title & Content
                                        Text(
                                            text = parsed.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.text,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Copy
                                            FilledTonalButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(item.text))
                                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Copy", fontSize = 10.5.sp)
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Remake
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.onScannedFromCamera(item.text)
                                                    onNavigateToStudio()
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Remake", fontSize = 10.5.sp, color = ElectricCyan)
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Open / Primary Action
                                            Button(
                                                onClick = {
                                                    QrContentParser.openPrimaryAction(context, item.text, parsed) { msg ->
                                                        clipboardManager.setText(AnnotatedString(item.text))
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = if (parsed.isLocation) ElectricCyan else EmeraldGreen, contentColor = Color.Black),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Open", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Sheet Action Row: Continue Scanning & Done
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showBatchSheet = false
                                isBatchPaused = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text("Continue Scanning", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = onNavigateToStudio,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text("Finish & Go to Studio", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class DecodedPhotoResult(
    val text: String,
    val thumbnail: Bitmap?
)

/**
 * Robust photo QR decoder that handles software bitmaps, proper scaling,
 * inverted QR codes, multiple rotations, and histogram equalized binarization.
 */
private fun decodeQrFromGalleryUri(context: Context, uri: Uri): DecodedPhotoResult? {
    try {
        // Step 1: Decode a clean software bitmap (max dimension 1600px to avoid memory overflow)
        val bitmap = loadSoftwareBitmapFromUri(context, uri, maxDimension = 1600) ?: return null

        // Create a crisp thumbnail for the UI confirmation badge
        val thumbnail = try {
            val thumbSize = 140
            Bitmap.createScaledBitmap(bitmap, thumbSize, thumbSize, true)
        } catch (_: Exception) {
            null
        }

        // Step 2: Try multi-pass detection on the scaled photo
        val decoded1 = scanQrMultiPass(bitmap)
        if (!decoded1.isNullOrEmpty()) {
            return DecodedPhotoResult(decoded1, thumbnail)
        }

        // Step 3: Try downscaling to 800px if the image was larger (ZXing block thresholding works best at 800px)
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim > 800) {
            val factor = 800f / maxDim
            val targetW = (bitmap.width * factor).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * factor).toInt().coerceAtLeast(1)
            val scaledBmp = try {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } catch (_: Exception) { null }

            if (scaledBmp != null) {
                val decoded2 = scanQrMultiPass(scaledBmp)
                if (!decoded2.isNullOrEmpty()) {
                    return DecodedPhotoResult(decoded2, thumbnail)
                }
            }
        }

        // Step 4: Fallback to QrScannabilityEvaluator which has additional crop passes
        val evaluatorResult = QrScannabilityEvaluator.evaluate(bitmap)
        if (!evaluatorResult.decodedText.isNullOrEmpty()) {
            return DecodedPhotoResult(evaluatorResult.decodedText, thumbnail)
        }

        return null
    } catch (_: Exception) {
        return null
    }
}

/**
 * Loads a software ARGB_8888 bitmap from content Uri, respecting maximum dimensions.
 * Never returns a Config#HARDWARE bitmap.
 */
private fun loadSoftwareBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
                val w = info.size.width
                val h = info.size.height
                val maxDim = maxOf(w, h)
                if (maxDim > maxDimension) {
                    val scale = maxDimension.toFloat() / maxDim
                    decoder.setTargetSize(
                        (w * scale).toInt().coerceAtLeast(1),
                        (h * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { s ->
                BitmapFactory.decodeStream(s, null, bounds)
            }
            val w = bounds.outWidth
            val h = bounds.outHeight
            var sample = 1
            val maxDim = maxOf(w, h)
            while (maxDim / (sample * 2) >= maxDimension) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { s ->
                BitmapFactory.decodeStream(s, null, opts)
            }
        }
    } catch (_: Exception) {
        try {
            context.contentResolver.openInputStream(uri)?.use { s ->
                BitmapFactory.decodeStream(s)
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Multi-pass decoder running Hybrid, Inverted, GlobalHistogram, and rotational scans.
 */
private fun scanQrMultiPass(bmp: Bitmap): String? {
    val safeBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
        bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
    } else {
        bmp
    }

    val width = safeBmp.width
    val height = safeBmp.height
    val pixels = IntArray(width * height)
    safeBmp.getPixels(pixels, 0, width, 0, 0, width, height)

    // Blend transparent pixels onto white background to avoid black alpha blocks
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val a = (pixel ushr 24) and 0xFF
        if (a < 255) {
            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF
            val blendedR = (r * a + 255 * (255 - a)) / 255
            val blendedG = (g * a + 255 * (255 - a)) / 255
            val blendedB = (b * a + 255 * (255 - a)) / 255
            pixels[i] = (0xFF shl 24) or (blendedR shl 16) or (blendedG shl 8) or blendedB
        }
    }

    val source = RGBLuminanceSource(width, height, pixels)
    val reader = MultiFormatReader().apply {
        setHints(mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        ))
    }

    // Pass 1: HybridBinarizer (standard contrast)
    try {
        val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        reader.reset()
        if (!result.text.isNullOrEmpty()) return result.text
    } catch (_: Exception) { reader.reset() }

    // Pass 2: Inverted HybridBinarizer (light QR on dark background / dark mode screenshots)
    try {
        val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert())))
        reader.reset()
        if (!result.text.isNullOrEmpty()) return result.text
    } catch (_: Exception) { reader.reset() }

    // Pass 3: GlobalHistogramBinarizer (smooth gradients, glare, photographic halftone)
    try {
        val result = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
        reader.reset()
        if (!result.text.isNullOrEmpty()) return result.text
    } catch (_: Exception) { reader.reset() }

    // Pass 4: Inverted GlobalHistogramBinarizer
    try {
        val result = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source.invert())))
        reader.reset()
        if (!result.text.isNullOrEmpty()) return result.text
    } catch (_: Exception) { reader.reset() }

    // Pass 5: Rotations (90, 180, 270)
    val matrix = Matrix()
    for (angle in floatArrayOf(90f, 180f, 270f)) {
        try {
            matrix.setRotate(angle)
            val rotatedBmp = Bitmap.createBitmap(safeBmp, 0, 0, width, height, matrix, true)
            val rW = rotatedBmp.width
            val rH = rotatedBmp.height
            val rPixels = IntArray(rW * rH)
            rotatedBmp.getPixels(rPixels, 0, rW, 0, 0, rW, rH)
            val rSource = RGBLuminanceSource(rW, rH, rPixels)
            try {
                val res = reader.decodeWithState(BinaryBitmap(HybridBinarizer(rSource)))
                reader.reset()
                if (!res.text.isNullOrEmpty()) return res.text
            } catch (_: Exception) { reader.reset() }
            try {
                val res = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(rSource)))
                reader.reset()
                if (!res.text.isNullOrEmpty()) return res.text
            } catch (_: Exception) { reader.reset() }
        } catch (_: Exception) {}
    }

    // Pass 6: Center crop (70% center crop to isolate centered QR code from busy photo margins)
    try {
        val cropW = (width * 0.70f).toInt()
        val cropH = (height * 0.70f).toInt()
        val cropX = (width - cropW) / 2
        val cropY = (height - cropH) / 2
        if (cropW > 80 && cropH > 80) {
            val croppedSource = source.crop(cropX, cropY, cropW, cropH)
            try {
                val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(croppedSource)))
                reader.reset()
                if (!result.text.isNullOrEmpty()) return result.text
            } catch (_: Exception) { reader.reset() }
            try {
                val result = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(croppedSource)))
                reader.reset()
                if (!result.text.isNullOrEmpty()) return result.text
            } catch (_: Exception) { reader.reset() }
        }
    } catch (_: Exception) {}

    return null
}

/**
 * Fast rotation of raw camera Y-plane buffer (luminance) to match screen orientation.
 */
private fun rotateYuvSensorBuffer(data: ByteArray, width: Int, height: Int, rotation: Int): Pair<ByteArray, Pair<Int, Int>> {
    if (rotation == 0) return Pair(data, Pair(width, height))
    val rotated = ByteArray(width * height)
    when (rotation) {
        90 -> {
            var i = 0
            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    rotated[i++] = data[y * width + x]
                }
            }
            return Pair(rotated, Pair(height, width))
        }
        180 -> {
            var i = 0
            for (j in width * height - 1 downTo 0) {
                rotated[i++] = data[j]
            }
            return Pair(rotated, Pair(width, height))
        }
        270 -> {
            var i = 0
            for (x in width - 1 downTo 0) {
                for (y in 0 until height) {
                    rotated[i++] = data[y * width + x]
                }
            }
            return Pair(rotated, Pair(height, width))
        }
        else -> return Pair(data, Pair(width, height))
    }
}

