package com.whatsapp.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsapp.clone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppTopBar(
    title: String = "WhatsApp",
    onCameraClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMenuOptionSelected: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                color = DarkTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        actions = {
            IconButton(onClick = onCameraClick) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_photo_camera),
                    contentDescription = "Câmera",
                    tint = DarkTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_search),
                    contentDescription = "Pesquisar",
                    tint = DarkTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = DarkTextPrimary
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(DarkSurfaceVariant)
                ) {
                    val options = listOf(
                        "Novo grupo",
                        "Nova transmissão",
                        "Aparelhos conectados",
                        "Mensagens favoritas",
                        "Pagamentos",
                        "Configurações"
                    )
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = DarkTextPrimary) },
                            onClick = {
                                expanded = false
                                onMenuOptionSelected(option)
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkSurface
        )
    )
}

enum class NavigationTab {
    CHATS,
    UPDATES,
    COMMUNITIES,
    CALLS
}

@Composable
fun WhatsAppBottomNavigation(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    chatsBadgeCount: Int = 3,
    callsBadgeCount: Int = 1
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Conversas (Chats)
        NavigationBarItem(
            selected = selectedTab == NavigationTab.CHATS,
            onClick = { onTabSelected(NavigationTab.CHATS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (chatsBadgeCount > 0) {
                            Badge(containerColor = WhatsAppGreen, contentColor = DarkBackground) {
                                Text(text = "$chatsBadgeCount", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.whatsapp.clone.R.drawable.ic_home_tab_chats_filled),
                        contentDescription = "Conversas",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = { Text("Conversas", fontWeight = if (selectedTab == NavigationTab.CHATS) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkTextPrimary,
                selectedTextColor = DarkTextPrimary,
                indicatorColor = Color(0xFF103629),
                unselectedIconColor = DarkTextSecondary,
                unselectedTextColor = DarkTextSecondary
            )
        )

        // 2. Atualizações (Updates / Status)
        NavigationBarItem(
            selected = selectedTab == NavigationTab.UPDATES,
            onClick = { onTabSelected(NavigationTab.UPDATES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == NavigationTab.UPDATES) Icons.Filled.DonutLarge else Icons.Outlined.DonutLarge,
                    contentDescription = "Atualizações"
                )
            },
            label = { Text("Atualizações") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkTextPrimary,
                selectedTextColor = DarkTextPrimary,
                indicatorColor = Color(0xFF103629),
                unselectedIconColor = DarkTextSecondary,
                unselectedTextColor = DarkTextSecondary
            )
        )

        // 3. Comunidades (Communities)
        NavigationBarItem(
            selected = selectedTab == NavigationTab.COMMUNITIES,
            onClick = { onTabSelected(NavigationTab.COMMUNITIES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == NavigationTab.COMMUNITIES) Icons.Filled.Groups else Icons.Outlined.Groups,
                    contentDescription = "Comunidades"
                )
            },
            label = { Text("Comunidades") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkTextPrimary,
                selectedTextColor = DarkTextPrimary,
                indicatorColor = Color(0xFF103629),
                unselectedIconColor = DarkTextSecondary,
                unselectedTextColor = DarkTextSecondary
            )
        )

        // 4. Chamadas (Calls)
        NavigationBarItem(
            selected = selectedTab == NavigationTab.CALLS,
            onClick = { onTabSelected(NavigationTab.CALLS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (callsBadgeCount > 0) {
                            Badge(containerColor = MissedCallRed, contentColor = Color.White) {
                                Text(text = "$callsBadgeCount", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (selectedTab == NavigationTab.CALLS) com.whatsapp.clone.R.drawable.ic_home_tab_calls_filled else com.whatsapp.clone.R.drawable.ic_home_tab_calls_unfilled
                        ),
                        contentDescription = "Chamadas",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = { Text("Chamadas") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkTextPrimary,
                selectedTextColor = DarkTextPrimary,
                indicatorColor = Color(0xFF103629),
                unselectedIconColor = DarkTextSecondary,
                unselectedTextColor = DarkTextSecondary
            )
        )
    }
}
