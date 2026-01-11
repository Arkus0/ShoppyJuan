package com.arkus.shoppyjuan.presentation.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.domain.model.ListItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ReorderableItemList(
    items: List<ListItem>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemCheckedChange: (String, Boolean) -> Unit,
    onItemDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable (() -> Unit)? = null
) {
    val view = LocalView.current
    var localItems by remember(items) { mutableStateOf(items) }
    val lazyListState = rememberLazyListState()

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Update local state immediately for smooth animation
        localItems = localItems.toMutableList().apply {
            add(to.index - (if (header != null) 1 else 0), removeAt(from.index - (if (header != null) 1 else 0)))
        }

        // Haptic feedback
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    // Sync with external items when they change
    LaunchedEffect(items) {
        localItems = items
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Optional header
        header?.let {
            item {
                it()
            }
        }

        items(localItems, key = { it.id }) { item ->
            ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    label = "drag_elevation"
                )

                DraggableItemCard(
                    item = item,
                    isDragging = isDragging,
                    elevation = elevation,
                    onCheckedChange = { checked -> onItemCheckedChange(item.id, checked) },
                    onDelete = { onItemDelete(item.id) },
                    dragModifier = Modifier.draggableHandle(
                        onDragStarted = {
                            view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)
                        },
                        onDragStopped = {
                            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                            // Notify parent of final order
                            val originalIndex = items.indexOfFirst { it.id == item.id }
                            val newIndex = localItems.indexOfFirst { it.id == item.id }
                            if (originalIndex != newIndex && originalIndex >= 0 && newIndex >= 0) {
                                onReorder(originalIndex, newIndex)
                            }
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun DraggableItemCard(
    item: ListItem,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (item.checked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Arrastrar para reordenar",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = dragModifier.padding(8.dp)
            )

            // Checkbox
            Checkbox(
                checked = item.checked,
                onCheckedChange = onCheckedChange
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Item content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        item.emoji?.let { append("$it ") }
                        append(item.name)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.checked) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}
