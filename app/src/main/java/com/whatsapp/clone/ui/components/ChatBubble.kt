package com.whatsapp.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsapp.clone.model.Message
import com.whatsapp.clone.model.MessageStatus
import com.whatsapp.clone.model.MessageType
import com.whatsapp.clone.ui.theme.*

@Composable
fun ChatBubble(
    message: Message,
    onDownloadClick: () -> Unit = {},
    isPlayingAudio: Boolean = false,
    audioPlaybackProgress: Float = 0f,
    onPlayAudioClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (message.type == MessageType.SYSTEM_E2EE_NOTICE) {
        SystemE2EERow(text = message.text)
        return
    }

    val isFromMe = message.isFromMe
    val bubbleColor = if (isFromMe) DarkBubbleOutgoing else DarkBubbleIncoming
    val shape = if (isFromMe) {
        RoundedCornerShape(12.dp, 12.dp, 2.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 90.dp, max = 310.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column {
                when (message.type) {
                    MessageType.AUDIO -> {
                        AudioMessageContent(
                            message = message,
                            isPlaying = isPlayingAudio,
                            progress = audioPlaybackProgress,
                            onPlayClick = onPlayAudioClick
                        )
                    }
                    MessageType.DOCUMENT -> {
                        DocumentMessageContent(
                            message = message,
                            onDownloadClick = onDownloadClick
                        )
                    }
                    MessageType.IMAGE -> {
                        ImageMessageContent(
                            message = message,
                            onDownloadClick = onDownloadClick
                        )
                    }
                    MessageType.LOCATION -> {
                        LocationMessageContent(message = message)
                    }
                    MessageType.CONTACT -> {
                        ContactMessageContent(message = message)
                    }
                    else -> {
                        Text(
                            text = message.text,
                            color = DarkTextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Timestamp & Checkmarks
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        color = DarkTextSecondary,
                        fontSize = 11.sp
                    )

                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.PENDING -> {
                                Text(text = "🕒", fontSize = 10.sp)
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Enviado",
                                    tint = TicksGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Entregue",
                                    tint = TicksGray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Lido",
                                    tint = TicksBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentMessageContent(
    message: Message,
    onDownloadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2831))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF8C52FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.icon_file_doc_large),
                contentDescription = "Documento",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.fileName ?: "Arquivo_Documento.pdf",
                color = DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${message.fileSize ?: "2.4 MB"} • PDF",
                color = DarkTextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (message.isDownloaded) Color(0xFF233138) else WhatsAppGreen)
                .clickable { onDownloadClick() },
            contentAlignment = Alignment.Center
        ) {
            if (message.isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (message.isDownloaded) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Baixado",
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_arrow_downward),
                    contentDescription = "Baixar arquivo",
                    tint = DarkBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ImageMessageContent(
    message: Message,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF182229)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Foto",
                tint = Color(0xFF536774),
                modifier = Modifier.size(64.dp)
            )

            if (!message.isDownloaded || message.isDownloading) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC0B141A))
                        .clickable { onDownloadClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (message.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = WhatsAppGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Baixar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.fileSize ?: "1.1 MB",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (message.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.text,
                color = DarkTextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun LocationMessageContent(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2831))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(WhatsAppGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Localização",
                tint = DarkBackground,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Localização em tempo real",
                color = DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message.text.ifBlank { "GPS ativo" },
                color = DarkTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ContactMessageContent(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2831))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A5298)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Contato",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.text.ifBlank { "Contato WhatsApp" },
                color = DarkTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Contato compartilhado",
                color = DarkTextSecondary,
                fontSize = 12.sp
            )
        }

        Button(
            onClick = { /* Message contact */ },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("Conversar", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AudioMessageContent(
    message: Message,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onPlayClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(WhatsAppGreen)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = if (isPlaying) com.whatsapp.clone.R.drawable.ic_pause else com.whatsapp.clone.R.drawable.ic_play_arrow
                ),
                contentDescription = if (isPlaying) "Pausar áudio" else "Tocar áudio gravado",
                tint = DarkBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        val bars = if (message.waveform.isNotEmpty()) message.waveform else listOf(
            0.2f, 0.5f, 0.8f, 0.3f, 0.6f, 0.9f, 0.4f, 0.7f, 0.5f, 0.3f, 0.8f, 0.4f, 0.6f, 0.2f
        )
        val activeBarIndex = (bars.size * progress).toInt()

        Row(
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            bars.forEachIndexed { index, amp ->
                val isPlayed = (isPlaying && index <= activeBarIndex) || (!isPlaying && index < 2)
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((amp * 24).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(if (isPlayed) AudioWaveformPlayed else AudioWaveformUnplayed)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "0:${message.audioDurationSeconds.toString().padStart(2, '0')}",
            color = DarkTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SystemE2EERow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF182229))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Criptografia de ponta a ponta",
                tint = Color(0xFFFFD279),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = Color(0xFFFFD279),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
