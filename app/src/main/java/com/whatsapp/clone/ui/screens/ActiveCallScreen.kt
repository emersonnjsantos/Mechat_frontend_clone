package com.whatsapp.clone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsapp.clone.ui.theme.*

@Composable
fun ActiveCallScreen(
    contactName: String = "Engenharia de Software (Meta / WA)",
    isVideo: Boolean = false,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1B21))
            .padding(vertical = 36.dp, horizontal = 24.dp)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "E2EE",
                    tint = DarkTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Criptografia de ponta a ponta (Signal / Kyber)",
                    color = DarkTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = contactName,
                color = DarkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "00:42",
                color = WhatsAppGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Large Avatar Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(WhatsAppGreenDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contactName.firstOrNull()?.toString() ?: "W",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker
            IconButton(
                onClick = { isSpeakerOn = !isSpeakerOn },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isSpeakerOn) Color.White else Color(0xFF202C33))
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = "Alto-falante",
                    tint = if (isSpeakerOn) DarkBackground else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Video Toggle
            IconButton(
                onClick = { /* Toggle video */ },
                modifier = Modifier
                    .size(54.dp)
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

            // Mute
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .size(54.dp)
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

            // End Call Button (Red)
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MissedCallRed)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Desligar chamada",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}
