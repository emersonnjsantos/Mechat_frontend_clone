package com.whatsapp.clone.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.whatsapp.clone.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppCameraScreen(
    onClose: () -> Unit,
    onMediaCaptured: (File, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        if (!hasCameraPermission) {
            Toast.makeText(context, "Permissão da câmera é necessária.", Toast.LENGTH_SHORT).show()
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Aguardando permissão da câmera...", color = Color.White)
        }
        return
    }

    // Camera State
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // Captured Media Review State
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var isCapturedVideo by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Bind Camera Lifecycle
    LaunchedEffect(lensFacing, flashMode) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
        val vidCap = VideoCapture.withOutput(recorder)
        videoCapture = vidCap

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture,
                vidCap
            )
        } catch (e: Exception) {
            Log.e("WhatsAppCamera", "Camera binding failed", e)
        }
    }

    // Timer for video recording
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordingSeconds = 0
            while (isRecordingVideo) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val destFile = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                capturedFile = destFile
                isCapturedVideo = false
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar mídia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (capturedFile == null) {
            // --- 1. ACTIVE VIEWFINDER ---
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Top Camera Bar: Close, Flash, Switch Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                }

                if (isRecordingVideo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val mins = recordingSeconds / 60
                        val secs = recordingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        val icon = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                            else -> Icons.Filled.FlashOff
                        }
                        Icon(icon, contentDescription = "Flash", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Filled.FlipCameraAndroid, contentDescription = "Inverter Câmera", tint = Color.White)
                    }
                }
            }

            // Bottom Bar: Gallery, Shutter Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecordingVideo) "Gravando vídeo... solte para enviar" else "Toque para foto, segure para vídeo",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Shortcut
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Galeria", tint = Color.White)
                    }

                    // Shutter Button (Tap for Photo, Long press for Video)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, if (isRecordingVideo) Color.Red else Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (isRecordingVideo) Color.Red else Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        // Take Photo
                                        val capture = imageCapture ?: return@detectTapGestures
                                        val photoFile = File(context.cacheDir, "wa_photo_${System.currentTimeMillis()}.jpg")
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                        capture.takePicture(
                                            outputOptions,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    capturedFile = photoFile
                                                    isCapturedVideo = false
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    Toast.makeText(context, "Erro ao fotografar: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    },
                                    onLongPress = {
                                        // Start Video Recording
                                        val vid = videoCapture ?: return@detectTapGestures
                                        val videoFile = File(context.cacheDir, "wa_video_${System.currentTimeMillis()}.mp4")
                                        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

                                        isRecordingVideo = true
                                        activeRecording = vid.output
                                            .prepareRecording(context, fileOutputOptions)
                                            .apply {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    withAudioEnabled()
                                                }
                                            }
                                            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                                when (recordEvent) {
                                                    is VideoRecordEvent.Finalize -> {
                                                        isRecordingVideo = false
                                                        if (!recordEvent.hasError()) {
                                                            capturedFile = videoFile
                                                            isCapturedVideo = true
                                                        } else {
                                                            Toast.makeText(context, "Erro na gravação de vídeo", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecordingVideo) {
                            Box(modifier = Modifier.size(24.dp).background(Color.White, RoundedCornerShape(4.dp)))
                        }
                    }

                    // Stop recording or Empty placeholder to balance layout
                    if (isRecordingVideo) {
                        IconButton(
                            onClick = {
                                activeRecording?.stop()
                                activeRecording = null
                                isRecordingVideo = false
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.Red, CircleShape)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Parar Gravação", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(50.dp))
                    }
                }
            }

        } else {
            // --- 2. CAPTURED MEDIA REVIEW & SEND ---
            val file = capturedFile!!
            val bitmap = remember(file) {
                if (!isCapturedVideo && file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Videocam, contentDescription = "Vídeo", tint = Color.White, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Vídeo pronto para envio (${file.length() / 1024} KB)", color = Color.White)
                        }
                    }
                }

                // Top Retake Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            capturedFile?.delete()
                            capturedFile = null
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Descartar", tint = Color.White)
                    }

                    Text(
                        text = if (isCapturedVideo) "Vídeo Gravado" else "Foto Capturada",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Bottom Caption & Send Bar (WhatsApp Official Design)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("Adicione uma legenda...", color = Color.LightGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xCC1F2C34),
                            unfocusedContainerColor = Color(0xCC1F2C34),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            onMediaCaptured(file, isCapturedVideo, captionText)
                            onClose()
                        },
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}
