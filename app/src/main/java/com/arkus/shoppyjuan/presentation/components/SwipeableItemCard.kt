package com.arkus.shoppyjuan.presentation.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.domain.model.ListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItemCard(
    item: ListItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right -> toggle checked
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onCheckedChange(!item.checked)
                    false // Don't dismiss, just toggle
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left -> delete
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(dismissState)
        },
        content = {
            ItemCardContent(
                item = item,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color by animateColorAsState(
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green for check
            SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935) // Red for delete
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        },
        label = "swipe_color"
    )

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        else -> Icons.Default.Check
    }

    val scale by animateFloatAsState(
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.scale(scale),
            tint = Color.White
        )
    }
}

@Composable
private fun ItemCardContent(
    item: ListItem,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (item.checked) 0.6f else 1f,
        label = "item_alpha"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (item.checked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "item_background"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = onCheckedChange
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        item.emoji?.let { append("$it ") }
                        append(item.name)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!item.checked) FontWeight.Medium else FontWeight.Normal,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.checked) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (item.quantity > 0 || item.unit != null) {
                    Text(
                        text = buildString {
                            append(item.quantity.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString()
                                else it.toString()
                            })
                            item.unit?.let { append(" $it") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                item.category?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (item.checked) 0.5f else 0.8f
                        )
                    )
                }
            }

            // Swipe hint icon
            Icon(
                Icons.Default.Delete,
                contentDescription = "Desliza para eliminar",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
