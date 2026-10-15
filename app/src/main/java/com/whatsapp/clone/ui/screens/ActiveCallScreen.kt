package com.whatsapp.clone.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsapp.clone.ui.theme.*
import com.whatsapp.clone.webrtc.WebRtcCallStats

@Composable
fun ActiveCallScreen(
    contactName: String = "Engenharia de Software (Meta / WA)",
    isVideo: Boolean = false,
    callStats: WebRtcCallStats = WebRtcCallStats(),
    isMuted: Boolean = false,
    isSpeakerOn: Boolean = false,
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    onEndCall: () -> Unit
) {
    val isConnected = callStats.callDurationSeconds > 0
    val minutes = callStats.callDurationSeconds / 60
    val seconds = callStats.callDurationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    // Pulse animation for avatar ring during call setup / active stream
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (!isConnected) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B141A),
                        Color(0xFF111B21),
                        Color(0xFF0F1B21)
                    )
                )
            )
            .padding(vertical = 36.dp, horizontal = 24.dp)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // E2EE Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF182229))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "E2EE",
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Criptografia de ponta a ponta (WebRTC DTLS-SRTP)",
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contact Name
            Text(
                text = contactName,
                color = DarkTextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Call State / Live Duration
            Text(
                text = if (isConnected) formattedDuration else "Chamando via WebRTC P2P...",
                color = if (isConnected) WhatsAppGreen else Color(0xFFFFD279),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Codec & Network Stats Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF202C33))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) WhatsAppGreen else Color(0xFFFF9800))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${callStats.codec} • ${if (callStats.isP2pDirect) "P2P Direto" else "TURN"} (${callStats.roundTripTimeMs}ms)",
                    color = DarkTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Center Calling Visual (Avatar or Video Preview)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isVideo) {
                // Video Call Presentation Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E2A30))
                        .border(1.dp, Color(0xFF2A3942), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Feed",
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Transmissão de Vídeo P2P Ativa (VP8 / 720p HD)",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Local PIP Preview in top right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(width = 72.dp, height = 96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF111B21))
                            .border(1.dp, WhatsAppGreen, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Você",
                            color = DarkTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                // Voice Call Animated Avatar Ring
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(WhatsAppGreenDark.copy(alpha = 0.25f))
                )

                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreenDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactName.firstOrNull()?.toString()?.uppercase() ?: "W",
                        color = Color.White,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Controls Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speakerphone Toggle
            IconButton(
                onClick = onToggleSpeaker,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isSpeakerOn) Color.White else Color(0xFF202C33))
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "Alto-falante",
                    tint = if (isSpeakerOn) DarkBackground else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Video Toggle Indicator
            IconButton(
                onClick = { /* Toggle between voice and video */ },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF202C33))
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Câmera",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Microphone Mute Toggle
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) Color.White else Color(0xFF202C33))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mudo",
                    tint = if (isMuted) DarkBackground else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // End Call Button (Red Circle)
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(MissedCallRed)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Desligar chamada",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
