package com.arkus.shoppyjuan.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.domain.model.ListItem

@Composable
fun ItemCard(
    item: ListItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onAddNote: () -> Unit,
    onCategoryChange: () -> Unit,
    modifier: Modifier = Modifier,
    onAssign: (() -> Unit)? = null
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

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showMenu = true }
            ) {
                // Name with emoji
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

                // Quantity and unit
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

                // Category
                item.category?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (item.checked) 0.5f else 0.8f
                        )
                    )
                }

                // Note indicator
                item.note?.let { note ->
                    if (note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Note,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            onEdit()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.note.isNullOrBlank()) "Añadir nota" else "Editar nota") },
                        onClick = {
                            onAddNote()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Cambiar categoría") },
                        onClick = {
                            onCategoryChange()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                    )

                    if (onAssign != null) {
                        DropdownMenuItem(
                            text = { Text("Asignar") },
                            onClick = {
                                onAssign.invoke()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCardCompact(
    item: ListItem,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = buildString {
                item.emoji?.let { append("$it ") }
                append(item.name)
            },
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.checked) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )

        if (item.quantity > 0) {
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
}
