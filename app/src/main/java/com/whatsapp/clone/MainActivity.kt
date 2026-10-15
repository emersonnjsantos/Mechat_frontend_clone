package com.whatsapp.clone

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.whatsapp.clone.ui.camera.WhatsAppCameraScreen
import com.whatsapp.clone.ui.components.NavigationTab
import com.whatsapp.clone.ui.components.WhatsAppBottomNavigation
import com.whatsapp.clone.ui.components.WhatsAppTopBar
import com.whatsapp.clone.ui.screens.*
import com.whatsapp.clone.ui.theme.*
import com.whatsapp.clone.utils.HardwareIntegrations
import com.whatsapp.clone.viewmodel.WhatsAppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WhatsAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WhatsAppTheme(darkTheme = true) {
                val context = LocalContext.current
                val chats by viewModel.chats.collectAsState()
                val messagesMap by viewModel.messages.collectAsState()
                val activeChatId by viewModel.activeChatId.collectAsState()
                val calls by viewModel.calls.collectAsState()
                val isInVoipCall by viewModel.isInVoipCall.collectAsState()
                val webRtcCallStats by viewModel.webRtcCallStats.collectAsState()
                val isCallMuted by viewModel.isCallMuted.collectAsState()
                val isCallSpeakerOn by viewModel.isCallSpeakerOn.collectAsState()
                val activeCallContactName by viewModel.activeCallContactName.collectAsState()
                val activeCallIsVideo by viewModel.activeCallIsVideo.collectAsState()
                val nativeCoreInfo by viewModel.nativeCoreInfo.collectAsState()

                // Real Hardware Audio Recording & Playback States
                val isRecordingAudio by viewModel.isRecordingAudio.collectAsState()
                val recordingDurationSeconds by viewModel.recordingDurationSeconds.collectAsState()
                val recordingWaveform by viewModel.recordingWaveform.collectAsState()
                val currentlyPlayingAudioId by viewModel.currentlyPlayingAudioId.collectAsState()
                val audioPlaybackProgress by viewModel.audioPlaybackProgress.collectAsState()

                var currentTab by remember { mutableStateOf(NavigationTab.CHATS) }

                // Modals for 3-dots Menu
                var showSettingsDialog by remember { mutableStateOf(false) }
                var showLinkedDevicesDialog by remember { mutableStateOf(false) }
                var showPaymentsDialog by remember { mutableStateOf(false) }

                // CameraX In-App Fullscreen Screen
                var showCameraScreen by remember { mutableStateOf(false) }

                when {
                    // 1. VoIP Call Fullscreen Screen
                    isInVoipCall -> {
                        val activeChat = chats.find { it.id == activeChatId }
                        ActiveCallScreen(
                            contactName = if (activeCallContactName.isNotBlank() && activeCallContactName != "WhatsApp Contato") activeCallContactName else (activeChat?.contactName ?: "WhatsApp Contato"),
                            isVideo = activeCallIsVideo,
                            callStats = webRtcCallStats,
                            isMuted = isCallMuted,
                            isSpeakerOn = isCallSpeakerOn,
                            onToggleMute = { viewModel.toggleCallMute() },
                            onToggleSpeaker = { viewModel.toggleCallSpeaker() },
                            onEndCall = { viewModel.endCall() }
                        )
                    }

                    // 2. CameraX In-App Fullscreen Viewfinder & Capture
                    showCameraScreen -> {
                        WhatsAppCameraScreen(
                            onClose = { showCameraScreen = false },
                            onMediaCaptured = { file, isVideo, caption ->
                                val targetChatId = activeChatId ?: 1L
                                viewModel.sendCapturedMedia(targetChatId, file, isVideo, caption)
                                showCameraScreen = false
                            }
                        )
                    }

                    // 3. Chat Conversation Detail Screen
                    activeChatId != null -> {
                        val activeChat = chats.find { it.id == activeChatId }
                        if (activeChat != null) {
                            val activeMessages = messagesMap[activeChat.id] ?: emptyList()
                            ChatDetailScreen(
                                chat = activeChat,
                                messages = activeMessages,
                                isRecordingAudio = isRecordingAudio,
                                recordingDurationSeconds = recordingDurationSeconds,
                                recordingWaveform = recordingWaveform,
                                currentlyPlayingAudioId = currentlyPlayingAudioId,
                                audioPlaybackProgress = audioPlaybackProgress,
                                onBackClick = { viewModel.closeChat() },
                                onSendMessage = { text -> viewModel.sendMessage(activeChat.id, text) },
                                onStartAudioRecording = { viewModel.startRecordingRealAudio() },
                                onStopAndSendAudioRecording = { viewModel.stopAndSendRealAudio(activeChat.id) },
                                onCancelAudioRecording = { viewModel.cancelRealAudioRecording() },
                                onTogglePlayAudio = { message -> viewModel.togglePlayAudio(message) },
                                onSendDocument = { name, size -> viewModel.sendDocument(activeChat.id, name, size) },
                                onSendImage = { caption -> viewModel.sendImage(activeChat.id, caption) },
                                onSendLocation = { addr -> viewModel.sendLocation(activeChat.id, addr) },
                                onSendContact = { name -> viewModel.sendContact(activeChat.id, name) },
                                onClearChat = { viewModel.clearChat(activeChat.id) },
                                onDownloadClick = { msgId -> viewModel.downloadMessageFile(activeChat.id, msgId) },
                                onVoiceCallClick = { viewModel.startCall(activeChat.contactName, false) },
                                onVideoCallClick = { viewModel.startCall(activeChat.contactName, true) },
                                onCameraClick = { showCameraScreen = true }
                            )
                        }
                    }

                    // 4. Main Screen with Top and Bottom Navigation Bars
                    else -> {
                        Scaffold(
                            topBar = {
                                Column {
                                    WhatsAppTopBar(
                                        title = "WhatsApp",
                                        onCameraClick = { showCameraScreen = true },
                                        onSearchClick = {
                                            Toast.makeText(context, "Pesquisar conversas e contatos", Toast.LENGTH_SHORT).show()
                                        },
                                        onMenuOptionSelected = { option ->
                                            when (option) {
                                                "Configurações" -> showSettingsDialog = true
                                                "Aparelhos conectados" -> showLinkedDevicesDialog = true
                                                "Pagamentos" -> showPaymentsDialog = true
                                                "Compartilhar aplicativo" -> {
                                                    HardwareIntegrations.shareWithInstalledApps(
                                                        context,
                                                        "Estou usando o WhatsApp Clone nativo reconstruído com C++, Kotlin e SQLite!"
                                                    )
                                                }
                                                else -> Toast.makeText(context, option, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    // Native Core Hardware Status Banner
                                    if (nativeCoreInfo.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F1B21))
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Core Nativo C++ / C99: $nativeCoreInfo",
                                                color = Color(0xFF00A884),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                WhatsAppBottomNavigation(
                                    selectedTab = currentTab,
                                    onTabSelected = { currentTab = it },
                                    chatsBadgeCount = chats.sumOf { it.unreadCount },
                                    callsBadgeCount = calls.count { it.isMissed }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(DarkBackground)
                            ) {
                                when (currentTab) {
                                    NavigationTab.CHATS -> {
                                        HomeScreen(
                                            chats = chats,
                                            onChatClick = { chatId -> viewModel.openChat(chatId) }
                                        )
                                    }
                                    NavigationTab.UPDATES -> {
                                        StatusScreen()
                                    }
                                    NavigationTab.COMMUNITIES -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.Groups,
                                                    contentDescription = null,
                                                    tint = WhatsAppGreen,
                                                    modifier = Modifier.size(64.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Comunidades do WhatsApp",
                                                    color = DarkTextPrimary,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Reúna grupos relacionados e envie avisos com facilidade.",
                                                    color = DarkTextSecondary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                    NavigationTab.CALLS -> {
                                        CallsScreen(
                                            calls = calls,
                                            onCallClick = { callItem ->
                                                viewModel.startCall(callItem.contactName, callItem.isVideo)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Settings Dialog
                if (showSettingsDialog) {
                    SettingsModal(onDismiss = { showSettingsDialog = false })
                }

                // Linked Devices Dialog
                if (showLinkedDevicesDialog) {
                    LinkedDevicesModal(onDismiss = { showLinkedDevicesDialog = false })
                }

                // Payments Dialog
                if (showPaymentsDialog) {
                    PaymentsModal(onDismiss = { showPaymentsDialog = false })
                }
            }
        }
    }
}

@Composable
fun SettingsModal(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configurações", color = DarkTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Profile header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("V", color = DarkBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Você (Usuário)", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("Disponível", color = DarkTextSecondary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkDivider)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsRow(Icons.Default.VpnKey, "Conta", "Notificações de segurança, mudar número")
                SettingsRow(Icons.Default.Lock, "Privacidade", "Bloquear contatos, mensagens temporárias")
                SettingsRow(Icons.AutoMirrored.Filled.Chat, "Conversas", "Tema, papéis de parede, histórico")
                SettingsRow(Icons.Default.Notifications, "Notificações", "Tons de mensagens e chamadas")
                SettingsRow(Icons.Default.Storage, "Armazenamento e dados", "Uso de rede, download automático")
                SettingsRow(Icons.AutoMirrored.Filled.Help, "Ajuda", "Central de ajuda, fale conosco")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = WhatsAppGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceVariant
    )
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Setting click */ }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = DarkTextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, color = DarkTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = DarkTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun LinkedDevicesModal(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Aparelhos Conectados", color = DarkTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Use o WhatsApp no computador ou em outros aparelhos sem manter seu celular conectado à internet.", color = DarkTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conectar um aparelho (QR Code)", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = WhatsAppGreen)
            }
        },
        containerColor = DarkSurfaceVariant
    )
}

@Composable
fun PaymentsModal(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pagamentos (WhatsApp Pay)", color = DarkTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("PIX & Cartões", color = DarkTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Pague e receba direto no WhatsApp de forma rápida e segura.", color = DarkTextSecondary, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nenhum método cadastrado ainda.", color = DarkTextSecondary, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = WhatsAppGreen)
            }
        },
        containerColor = DarkSurfaceVariant
    )
}
