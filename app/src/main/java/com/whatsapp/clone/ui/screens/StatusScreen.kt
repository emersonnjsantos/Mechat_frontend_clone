package com.whatsapp.clone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsapp.clone.ui.theme.*

@Composable
fun StatusScreen(
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
            // My Status Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Update status */ }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(54.dp)
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A5298)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "V",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Green Plus badge
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar status",
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Meu status",
                            color = DarkTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Toque para atualizar seu status",
                            color = DarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = "Atualizações recentes",
                    color = DarkTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Recent Status Updates
            item {
                StatusItemRow(
                    name = "Engenharia de Software (Meta / WA)",
                    time = "Hoje às 18:20",
                    avatarLetter = "E",
                    avatarColor = 0xFF00A884
                )
                StatusItemRow(
                    name = "Família & Amigos",
                    time = "Hoje às 15:05",
                    avatarLetter = "F",
                    avatarColor = 0xFF128C7E
                )
            }
        }

        // Action Buttons for Status (Edit text status + Camera status)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(
                onClick = { /* Write text status */ },
                containerColor = DarkSurfaceVariant,
                contentColor = DarkTextPrimary,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Status de texto",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            FloatingActionButton(
                onClick = { /* Camera status */ },
                containerColor = WhatsAppGreen,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_photo_camera),
                    contentDescription = "Status de foto",
                    tint = DarkBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StatusItemRow(
    name: String,
    time: String,
    avatarLetter: String,
    avatarColor: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* View status */ }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with green status border
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(WhatsAppGreen)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(avatarColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarLetter,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = name,
                color = DarkTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                color = DarkTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
