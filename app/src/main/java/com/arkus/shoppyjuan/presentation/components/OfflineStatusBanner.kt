package com.arkus.shoppyjuan.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.data.sync.SyncStatus

@Composable
fun OfflineStatusBanner(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null
) {
    val backgroundColor = when (syncStatus) {
        is SyncStatus.Offline -> MaterialTheme.colorScheme.errorContainer
        is SyncStatus.Pending -> MaterialTheme.colorScheme.tertiaryContainer
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.secondaryContainer
        is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer
        SyncStatus.Synced -> Color.Transparent
    }

    val contentColor = when (syncStatus) {
        is SyncStatus.Offline -> MaterialTheme.colorScheme.onErrorContainer
        is SyncStatus.Pending -> MaterialTheme.colorScheme.onTertiaryContainer
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.onSecondaryContainer
        is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        SyncStatus.Synced -> Color.Transparent
    }

    val icon = when (syncStatus) {
        is SyncStatus.Offline -> Icons.Default.CloudOff
        is SyncStatus.Pending -> Icons.Default.CloudQueue
        is SyncStatus.Syncing -> Icons.Default.CloudSync
        is SyncStatus.Error -> Icons.Default.ErrorOutline
        SyncStatus.Synced -> Icons.Default.CloudDone
    }

    val text = when (syncStatus) {
        is SyncStatus.Offline -> "Sin conexion - Los cambios se guardaran localmente"
        is SyncStatus.Pending -> "Sincronizando ${syncStatus.count} cambio(s)..."
        is SyncStatus.Syncing -> "Sincronizando..."
        is SyncStatus.Error -> "Error de sincronizacion: ${syncStatus.message}"
        SyncStatus.Synced -> null
    }

    AnimatedVisibility(
        visible = syncStatus != SyncStatus.Synced,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (syncStatus is SyncStatus.Syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = text ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                if (syncStatus is SyncStatus.Error && onRetryClick != null) {
                    TextButton(
                        onClick = onRetryClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = contentColor
                        )
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusChip(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    if (syncStatus == SyncStatus.Synced) return

    val backgroundColor = when (syncStatus) {
        is SyncStatus.Offline -> MaterialTheme.colorScheme.errorContainer
        is SyncStatus.Pending -> MaterialTheme.colorScheme.tertiaryContainer
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.secondaryContainer
        is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer
        SyncStatus.Synced -> return
    }

    val icon = when (syncStatus) {
        is SyncStatus.Offline -> Icons.Default.CloudOff
        is SyncStatus.Pending -> Icons.Default.CloudQueue
        is SyncStatus.Syncing -> Icons.Default.CloudSync
        is SyncStatus.Error -> Icons.Default.ErrorOutline
        SyncStatus.Synced -> return
    }

    val label = when (syncStatus) {
        is SyncStatus.Offline -> "Offline"
        is SyncStatus.Pending -> "${syncStatus.count} pendiente(s)"
        is SyncStatus.Syncing -> "Sincronizando"
        is SyncStatus.Error -> "Error"
        SyncStatus.Synced -> return
    }

    SuggestionChip(
        onClick = { },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        icon = {
            if (syncStatus is SyncStatus.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = backgroundColor
        ),
        modifier = modifier
    )
}
