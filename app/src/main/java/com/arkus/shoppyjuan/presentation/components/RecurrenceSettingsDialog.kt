package com.arkus.shoppyjuan.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.domain.model.DayOfWeek
import com.arkus.shoppyjuan.domain.model.RecurrenceFrequency
import com.arkus.shoppyjuan.domain.model.RecurrenceSettings

@Composable
fun RecurrenceSettingsDialog(
    currentSettings: RecurrenceSettings,
    onSave: (RecurrenceSettings) -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(currentSettings) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Lista Recurrente",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Enable/Disable toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settings = settings.copy(isEnabled = !settings.isEnabled) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Habilitar recurrencia",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Restablecer la lista automaticamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { settings = settings.copy(isEnabled = it) }
                    )
                }

                if (settings.isEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Frequency selector
                    Text(
                        text = "Frecuencia",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        FrequencyOption(
                            title = "Diario",
                            isSelected = settings.frequency == RecurrenceFrequency.DAILY,
                            onClick = { settings = settings.copy(frequency = RecurrenceFrequency.DAILY) }
                        )
                        FrequencyOption(
                            title = "Semanal",
                            isSelected = settings.frequency == RecurrenceFrequency.WEEKLY,
                            onClick = { settings = settings.copy(frequency = RecurrenceFrequency.WEEKLY) }
                        )
                        FrequencyOption(
                            title = "Cada 2 semanas",
                            isSelected = settings.frequency == RecurrenceFrequency.BIWEEKLY,
                            onClick = { settings = settings.copy(frequency = RecurrenceFrequency.BIWEEKLY) }
                        )
                        FrequencyOption(
                            title = "Mensual",
                            isSelected = settings.frequency == RecurrenceFrequency.MONTHLY,
                            onClick = { settings = settings.copy(frequency = RecurrenceFrequency.MONTHLY) }
                        )
                        FrequencyOption(
                            title = "Personalizado",
                            isSelected = settings.frequency == RecurrenceFrequency.CUSTOM,
                            onClick = { settings = settings.copy(frequency = RecurrenceFrequency.CUSTOM) }
                        )
                    }

                    // Day selection for weekly/biweekly
                    if (settings.frequency == RecurrenceFrequency.WEEKLY ||
                        settings.frequency == RecurrenceFrequency.BIWEEKLY
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Dias de la semana",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DayOfWeek.entries.forEach { day ->
                                val isSelected = settings.selectedDays.contains(day)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newDays = if (isSelected) {
                                            if (settings.selectedDays.size > 1) {
                                                settings.selectedDays - day
                                            } else {
                                                settings.selectedDays // Must have at least one day
                                            }
                                        } else {
                                            settings.selectedDays + day
                                        }
                                        settings = settings.copy(selectedDays = newDays)
                                    },
                                    label = {
                                        Text(
                                            text = day.toSpanish().take(2),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.size(width = 44.dp, height = 32.dp)
                                )
                            }
                        }
                    }

                    // Custom interval
                    if (settings.frequency == RecurrenceFrequency.CUSTOM) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cada",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = settings.customIntervalDays.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { days ->
                                        if (days in 1..365) {
                                            settings = settings.copy(customIntervalDays = days)
                                        }
                                    }
                                },
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "dias",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Reset behavior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settings = settings.copy(resetOnRecurrence = !settings.resetOnRecurrence)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restablecer items",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Desmarcar todos los items al repetir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.resetOnRecurrence,
                            onCheckedChange = { settings = settings.copy(resetOnRecurrence = it) }
                        )
                    }

                    // Notification settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                settings = settings.copy(
                                    notifyBeforeRecurrence = !settings.notifyBeforeRecurrence
                                )
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recordatorio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Notificar antes de la recurrencia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.notifyBeforeRecurrence,
                            onCheckedChange = { settings = settings.copy(notifyBeforeRecurrence = it) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(settings) }
            ) {
                Text(if (settings.isEnabled) "Guardar" else "Aceptar")
            }
        },
        dismissButton = {
            Row {
                if (currentSettings.isEnabled) {
                    TextButton(onClick = onDisable) {
                        Text("Deshabilitar")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun FrequencyOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
