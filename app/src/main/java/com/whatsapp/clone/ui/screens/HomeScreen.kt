package com.whatsapp.clone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PushPin
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
import com.whatsapp.clone.model.Chat
import com.whatsapp.clone.ui.theme.*

@Composable
fun HomeScreen(
    chats: List<Chat>,
    onChatClick: (Long) -> Unit,
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
            items(chats, key = { it.id }) { chat ->
                ChatItemRow(
                    chat = chat,
                    onClick = { onChatClick(chat.id) }
                )
                HorizontalDivider(
                    color = DarkDivider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp)
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { /* New conversation */ },
            containerColor = WhatsAppGreen,
            contentColor = DarkBackground,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_action_compose),
                contentDescription = "Nova conversa",
                tint = DarkBackground,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ChatItemRow(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(chat.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.avatarLetter,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Chat Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.contactName,
                    color = DarkTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = chat.lastMessageTime,
                    color = if (chat.unreadCount > 0) WhatsAppGreen else DarkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Lido",
                        tint = TicksBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = chat.lastMessage,
                        color = DarkTextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fixado",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${chat.unreadCount}",
                                color = DarkBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
