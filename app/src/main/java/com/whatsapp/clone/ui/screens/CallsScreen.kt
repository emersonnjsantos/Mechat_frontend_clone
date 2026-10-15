package com.whatsapp.clone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsapp.clone.model.CallItem
import com.whatsapp.clone.ui.theme.*

@Composable
fun CallsScreen(
    calls: List<CallItem>,
    onCallClick: (CallItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // "Criar link de chamada" (CallLinkActivity)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Create call link */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link de chamada",
                            tint = DarkBackground,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Criar link de chamada",
                            color = DarkTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Compartilhe um link para sua chamada do WhatsApp",
                            color = DarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = "Recentes",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(calls, key = { it.id }) { call ->
                CallItemRow(call = call, onCallClick = { onCallClick(call) })
                HorizontalDivider(
                    color = DarkDivider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
fun CallItemRow(
    call: CallItem,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCallClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(call.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = call.avatarLetter,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Call details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.contactName,
                color = if (call.isMissed) MissedCallRed else DarkTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                @Suppress("DEPRECATION")
                Icon(
                    imageVector = if (call.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                    contentDescription = if (call.isIncoming) "Recebida" else "Efetuada",
                    tint = if (call.isMissed) MissedCallRed else WhatsAppGreen,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = call.timestamp,
                    color = DarkTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        IconButton(onClick = onCallClick) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(
                    id = if (call.isVideo) com.whatsapp.clone.R.drawable.ic_videocam else com.whatsapp.clone.R.drawable.ic_call
                ),
                contentDescription = if (call.isVideo) "Chamada de vídeo" else "Chamada de voz",
                tint = WhatsAppGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
