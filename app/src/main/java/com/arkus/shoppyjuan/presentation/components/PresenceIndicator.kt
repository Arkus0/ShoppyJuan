package com.arkus.shoppyjuan.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnlineUser(
    val userId: String,
    val userName: String,
    val isOnline: Boolean
)

@Composable
fun PresenceIndicator(
    onlineUsers: List<OnlineUser>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val activeUsers = onlineUsers.filter { it.isOnline && it.userId != currentUserId }

    if (activeUsers.isEmpty()) {
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        activeUsers.take(3).forEach { user ->
            UserAvatar(
                userName = user.userName,
                isOnline = user.isOnline
            )
        }

        if (activeUsers.size > 3) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${activeUsers.size - 3}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun UserAvatar(
    userName: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(getAvatarColor(userName))
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Online indicator
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun PresenceChip(
    onlineUsers: List<OnlineUser>,
    currentUserId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeUsers = onlineUsers.filter { it.isOnline && it.userId != currentUserId }

    if (activeUsers.isEmpty()) {
        return
    }

    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = when (activeUsers.size) {
                    1 -> "${activeUsers[0].userName} está aquí"
                    2 -> "${activeUsers[0].userName} y ${activeUsers[1].userName} están aquí"
                    else -> "${activeUsers.size} personas están aquí"
                },
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            PresenceIndicator(
                onlineUsers = onlineUsers,
                currentUserId = currentUserId
            )
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun OnlineUsersDialog(
    onlineUsers: List<OnlineUser>,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val activeUsers = onlineUsers.filter { it.isOnline && it.userId != currentUserId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "👥 Personas en línea (${activeUsers.size})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeUsers.isEmpty()) {
                    Text(
                        text = "No hay nadie más viendo esta lista",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    activeUsers.forEach { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserAvatar(
                                userName = user.userName,
                                isOnline = user.isOnline,
                                modifier = Modifier.size(40.dp)
                            )
                            Column {
                                Text(
                                    text = user.userName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "● En línea",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF1976D2), // Blue
        Color(0xFFD32F2F), // Red
        Color(0xFF388E3C), // Green
        Color(0xFFF57C00), // Orange
        Color(0xFF7B1FA2), // Purple
        Color(0xFF0097A7), // Cyan
        Color(0xFFC2185B), // Pink
        Color(0xFF5D4037), // Brown
    )
    val index = name.hashCode() % colors.size
    return colors[index.coerceAtLeast(0)]
}
