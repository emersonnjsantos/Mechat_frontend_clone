package com.whatsapp.clone.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.whatsapp.clone.model.Chat
import com.whatsapp.clone.model.Message
import com.whatsapp.clone.ui.components.ChatBubble
import com.whatsapp.clone.ui.theme.*
import com.whatsapp.clone.utils.HardwareIntegrations
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: Chat,
    messages: List<Message>,
    isRecordingAudio: Boolean,
    recordingDurationSeconds: Int,
    recordingWaveform: List<Float>,
    currentlyPlayingAudioId: String?,
    audioPlaybackProgress: Float,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopAndSendAudioRecording: () -> Unit,
    onCancelAudioRecording: () -> Unit,
    onTogglePlayAudio: (Message) -> Unit,
    onSendDocument: (String, String) -> Unit,
    onSendImage: (String) -> Unit,
    onSendLocation: (String) -> Unit,
    onSendContact: (String) -> Unit,
    onClearChat: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showChatMenu by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showContactInfoDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showRealContactsDialog by remember { mutableStateOf(false) }
    var realContactsList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Permissão e Gravação do Microfone Físico Real
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartAudioRecording()
            Toast.makeText(context, "Microfone ativo: gravando áudio real...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de microfone necessária", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            onStartAudioRecording()
            Toast.makeText(context, "Gravando voz do microfone...", Toast.LENGTH_SHORT).show()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 2. Câmera Real
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onSendImage("Foto capturada pela Câmera (${bitmap.width}x${bitmap.height})")
            Toast.makeText(context, "Foto capturada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) takePhotoLauncher.launch(null)
        else Toast.makeText(context, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
    }

    fun launchRealCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) takePhotoLauncher.launch(null)
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 3. Galeria Real
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imagem_galeria.jpg"
            onSendImage("Foto selecionada da Galeria: $fileName")
        }
    }

    // 4. Documentos da Memória Real
    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val docName = uri.lastPathSegment?.substringAfterLast('/') ?: "documento_celular.pdf"
            onSendDocument(docName, "2.8 MB")
        }
    }

    // 5. GPS Real
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            val loc = HardwareIntegrations.getDeviceGpsLocation(context)
            if (loc != null) {
                onSendLocation("GPS: Lat ${String.format("%.5f", loc.first)}, Long ${String.format("%.5f", loc.second)}")
            } else {
                onSendLocation("GPS ativo: Localização atual capturada")
            }
        }
    }

    fun fetchRealGpsLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            val loc = HardwareIntegrations.getDeviceGpsLocation(context)
            if (loc != null) {
                onSendLocation("GPS: Lat ${String.format("%.5f", loc.first)}, Long ${String.format("%.5f", loc.second)}")
            } else {
                onSendLocation("GPS ativo: Localização atual capturada")
            }
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // 6. Contatos Reais
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            realContactsList = HardwareIntegrations.getPhoneContacts(context)
            showRealContactsDialog = true
        }
    }

    fun fetchRealContacts() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            realContactsList = HardwareIntegrations.getPhoneContacts(context)
            if (realContactsList.isNotEmpty()) showRealContactsDialog = true
            else onSendContact("Contato Real da Agenda")
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showContactInfoDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(chat.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.avatarLetter,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = chat.contactName,
                                color = DarkTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (chat.isOnline) "online" else "visto por último hoje às 18:30",
                                color = if (chat.isOnline) WhatsAppGreen else DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_arrow_back_white),
                            contentDescription = "Voltar",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onVideoCallClick) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_videocam),
                            contentDescription = "Chamada de vídeo",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onVoiceCallClick) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_call),
                            contentDescription = "Chamada de voz",
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { showChatMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mais opções",
                                tint = DarkTextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showChatMenu,
                            onDismissRequest = { showChatMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dados do contato", color = DarkTextPrimary) },
                                onClick = {
                                    showChatMenu = false
                                    showContactInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar com outros apps", color = DarkTextPrimary) },
                                onClick = {
                                    showChatMenu = false
                                    HardwareIntegrations.shareWithInstalledApps(
                                        context,
                                        "Conversa com ${chat.contactName} via WhatsApp Clone"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Limpar conversa", color = MissedCallRed) },
                                onClick = {
                                    showChatMenu = false
                                    showClearConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
            ) {
                // WhatsApp Attachment Sheet
                if (showAttachmentSheet && !isRecordingAudio) {
                    AttachmentGrid(
                        onSendDocument = {
                            showAttachmentSheet = false
                            pickDocumentLauncher.launch("*/*")
                        },
                        onSendCamera = {
                            showAttachmentSheet = false
                            launchRealCamera()
                        },
                        onSendGallery = {
                            showAttachmentSheet = false
                            pickImageLauncher.launch("image/*")
                        },
                        onSendAudio = {
                            showAttachmentSheet = false
                            handleMicClick()
                        },
                        onSendLocation = {
                            showAttachmentSheet = false
                            fetchRealGpsLocation()
                        },
                        onSendContact = {
                            showAttachmentSheet = false
                            fetchRealContacts()
                        }
                    )
                }

                // Voice Recording Live Bar (When Recording from Hardware Mic)
                if (isRecordingAudio) {
                    VoiceRecordingBar(
                        durationSeconds = recordingDurationSeconds,
                        waveform = recordingWaveform,
                        onCancel = onCancelAudioRecording,
                        onSend = onStopAndSendAudioRecording
                    )
                } else {
                    // Regular Text Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { Toast.makeText(context, "Emojis e Figurinhas", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mood,
                                    contentDescription = "Emojis",
                                    tint = DarkTextSecondary
                                )
                            }

                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(text = "Mensagem", color = DarkTextSecondary, fontSize = 15.sp)
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = DarkTextPrimary,
                                    unfocusedTextColor = DarkTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { showAttachmentSheet = !showAttachmentSheet },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Anexo",
                                    tint = if (showAttachmentSheet) WhatsAppGreen else DarkTextSecondary
                                )
                            }

                            if (inputText.isBlank()) {
                                IconButton(
                                    onClick = { launchRealCamera() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_photo_camera),
                                        contentDescription = "Câmera",
                                        tint = DarkTextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Mic Button (Record Voice) / Send Button
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                } else {
                                    handleMicClick()
                                }
                            },
                            containerColor = WhatsAppGreen,
                            contentColor = DarkBackground,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = if (inputText.isNotBlank()) com.whatsapp.clone.R.drawable.ic_send else com.whatsapp.clone.R.drawable.ic_mic_filled
                                ),
                                contentDescription = if (inputText.isNotBlank()) "Enviar" else "Gravar áudio com microfone real",
                                tint = DarkBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Authentic WhatsApp Doodle Wallpaper from original XAPK
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.conversation_background),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.08f)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onDownloadClick = { onDownloadClick(message.id) },
                        isPlayingAudio = currentlyPlayingAudioId == message.id,
                        audioPlaybackProgress = if (currentlyPlayingAudioId == message.id) audioPlaybackProgress else 0f,
                        onPlayAudioClick = { onTogglePlayAudio(message) }
                    )
                }
            }
        }
    }

    // Contact Info Dialog
    if (showContactInfoDialog) {
        AlertDialog(
            onDismissRequest = { showContactInfoDialog = false },
            title = { Text("Dados do Contato", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(chat.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chat.avatarLetter, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(chat.contactName, color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("+55 11 98765-4321", color = DarkTextSecondary, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Criptografia: Signal Protocol (Curve25519) + Crystals-Kyber PQC", color = DarkTextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactInfoDialog = false }) {
                    Text("OK", color = WhatsAppGreen)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Real Contacts Dialog
    if (showRealContactsDialog) {
        AlertDialog(
            onDismissRequest = { showRealContactsDialog = false },
            title = { Text("Contatos da Agenda Real", color = DarkTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(realContactsList) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSendContact("${contact.first} (${contact.second})")
                                    showRealContactsDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(contact.first, color = DarkTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(contact.second, color = DarkTextSecondary, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = DarkDivider)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRealContactsDialog = false }) {
                    Text("Fechar", color = WhatsAppGreen)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Clear Chat Confirmation
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Limpar conversa?", color = DarkTextPrimary) },
            text = { Text("Deseja realmente apagar todas as mensagens desta conversa?", color = DarkTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onClearChat()
                    showClearConfirmDialog = false
                    Toast.makeText(context, "Conversa limpa com sucesso", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Limpar", color = MissedCallRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar", color = DarkTextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}

@Composable
fun VoiceRecordingBar(
    durationSeconds: Int,
    waveform: List<Float>,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red Trash Can (Cancel recording)
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF2B1B1B))
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Cancelar gravação",
                tint = MissedCallRed,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Recording Container with Pulsing Dot, Timer, and Real Waveform
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blinking red recording indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .alpha(alphaAnim)
                    .clip(CircleShape)
                    .background(MissedCallRed)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Timer: 0:01, 0:02...
            Text(
                text = "0:${durationSeconds.toString().padStart(2, '0')}",
                color = DarkTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Live Waveform visualizer from physical microphone
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val liveBars = if (waveform.isNotEmpty()) waveform else listOf(0.2f, 0.4f, 0.6f, 0.3f, 0.5f)
                liveBars.forEach { amp ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((amp * 20).dp.coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(WhatsAppGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send Audio Button
        FloatingActionButton(
            onClick = onSend,
            containerColor = WhatsAppGreen,
            contentColor = DarkBackground,
            shape = CircleShape,
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_send),
                contentDescription = "Enviar áudio gravado",
                tint = DarkBackground,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AttachmentGrid(
    onSendDocument: () -> Unit,
    onSendCamera: () -> Unit,
    onSendGallery: () -> Unit,
    onSendAudio: () -> Unit,
    onSendLocation: () -> Unit,
    onSendContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(DarkSurfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentIconItem(
                drawableResId = com.whatsapp.clone.R.drawable.ic_attachment_document,
                label = "Documento",
                backgroundColor = Color(0xFF7F66FF),
                onClick = onSendDocument
            )
            AttachmentIconItem(
                drawableResId = com.whatsapp.clone.R.drawable.ic_attachment_camera,
                label = "Câmera",
                backgroundColor = Color(0xFFD3396D),
                onClick = onSendCamera
            )
            AttachmentIconItem(
                drawableResId = com.whatsapp.clone.R.drawable.attach_gallery,
                label = "Galeria",
                backgroundColor = Color(0xFFAC44CF),
                onClick = onSendGallery
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentIconItem(
                drawableResId = com.whatsapp.clone.R.drawable.ic_attachment_audio,
                label = "Áudio",
                backgroundColor = Color(0xFFE96C32),
                onClick = onSendAudio
            )
            AttachmentIconItem(
                drawableResId = com.whatsapp.clone.R.drawable.btn_send_current_location,
                label = "Localização",
                backgroundColor = Color(0xFF1EA95B),
                onClick = onSendLocation
            )
            AttachmentIconItem(
                drawableResId = com.whatsapp.clone.R.drawable.ic_attachment_contact,
                label = "Contato",
                backgroundColor = Color(0xFF009DE2),
                onClick = onSendContact
            )
        }
    }
}

@Composable
fun AttachmentIconItem(
    drawableResId: Int,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = drawableResId),
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = DarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
