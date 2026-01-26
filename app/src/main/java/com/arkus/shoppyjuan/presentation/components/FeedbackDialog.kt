package com.arkus.shoppyjuan.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class FeedbackType(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    BUG(
        title = "Reportar error",
        icon = Icons.Default.BugReport,
        description = "Algo no funciona correctamente"
    ),
    FEATURE(
        title = "Sugerencia",
        icon = Icons.Default.Lightbulb,
        description = "Tengo una idea para mejorar"
    ),
    GENERAL(
        title = "Comentario general",
        icon = Icons.Default.Chat,
        description = "Otros comentarios"
    )
}

enum class FeedbackRating(val emoji: String, val description: String) {
    TERRIBLE("😞", "Muy mal"),
    BAD("😕", "Mal"),
    OKAY("😐", "Regular"),
    GOOD("🙂", "Bien"),
    EXCELLENT("😍", "Excelente")
}

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (FeedbackType, FeedbackRating?, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(FeedbackType.GENERAL) }
    var selectedRating by remember { mutableStateOf<FeedbackRating?>(null) }
    var feedbackText by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) {
                    1 -> "Tipo de feedback"
                    2 -> "Tu opinion"
                    else -> "Feedback"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                when (step) {
                    1 -> FeedbackTypeSelector(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it }
                    )
                    2 -> FeedbackContent(
                        type = selectedType,
                        rating = selectedRating,
                        text = feedbackText,
                        onRatingSelected = { selectedRating = it },
                        onTextChanged = { feedbackText = it }
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(onClick = { step = 2 }) {
                    Text("Siguiente")
                }
            } else {
                Button(
                    onClick = {
                        onSubmit(selectedType, selectedRating, feedbackText)
                    },
                    enabled = feedbackText.isNotBlank()
                ) {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            if (step == 2) {
                TextButton(onClick = { step = 1 }) {
                    Text("Atras")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun FeedbackTypeSelector(
    selectedType: FeedbackType,
    onTypeSelected: (FeedbackType) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        Text(
            text = "Que tipo de feedback quieres enviar?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeedbackType.entries.forEach { type ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) },
                        role = Role.RadioButton
                    ),
                shape = MaterialTheme.shapes.medium,
                color = if (selectedType == type) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = null,
                        tint = if (selectedType == type) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    RadioButton(
                        selected = selectedType == type,
                        onClick = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeedbackContent(
    type: FeedbackType,
    rating: FeedbackRating?,
    text: String,
    onRatingSelected: (FeedbackRating) -> Unit,
    onTextChanged: (String) -> Unit
) {
    Column {
        // Rating selector (for bug and general feedback)
        if (type != FeedbackType.FEATURE) {
            Text(
                text = "Como calificarias tu experiencia?",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeedbackRating.entries.forEach { feedbackRating ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .selectable(
                                selected = rating == feedbackRating,
                                onClick = { onRatingSelected(feedbackRating) }
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = feedbackRating.emoji,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(4.dp)
                        )
                        if (rating == feedbackRating) {
                            Text(
                                text = feedbackRating.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Feedback text
        Text(
            text = when (type) {
                FeedbackType.BUG -> "Describe el problema:"
                FeedbackType.FEATURE -> "Describe tu sugerencia:"
                FeedbackType.GENERAL -> "Tu comentario:"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = {
                Text(
                    text = when (type) {
                        FeedbackType.BUG -> "Describe que esperabas que pasara y que paso en su lugar..."
                        FeedbackType.FEATURE -> "Explica que funcionalidad te gustaria ver..."
                        FeedbackType.GENERAL -> "Comparte tus pensamientos..."
                    }
                )
            },
            maxLines = 6
        )
    }
}
