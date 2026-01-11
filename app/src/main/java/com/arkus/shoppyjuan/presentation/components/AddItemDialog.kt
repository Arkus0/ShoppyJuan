package com.arkus.shoppyjuan.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.data.barcode.BarcodeScannerManager
import com.arkus.shoppyjuan.data.speech.VoiceInputManager
import com.arkus.shoppyjuan.data.speech.VoiceInputState
import kotlinx.coroutines.launch

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, quantity: Double, unit: String?) -> Unit,
    voiceInputManager: VoiceInputManager? = null,
    barcodeScannerManager: BarcodeScannerManager? = null
) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var voiceInputState by remember { mutableStateOf<VoiceInputState>(VoiceInputState.Idle) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (showBarcodeScanner && barcodeScannerManager != null) {
        BarcodeScannerScreen(
            scannerManager = barcodeScannerManager,
            onBarcodeScanned = { barcode ->
                itemName = barcode
                showBarcodeScanner = false
            },
            onClose = { showBarcodeScanner = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Anadir Articulo") },
            text = {
                Column {
                    // Name input with voice and barcode buttons
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Nombre del articulo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            Row {
                                // Voice input button
                                if (voiceInputManager != null) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                voiceInputState = VoiceInputState.Listening
                                                voiceInputManager.startListening().collect { state ->
                                                    voiceInputState = state
                                                    if (state is VoiceInputState.Result) {
                                                        itemName = state.text
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = "Entrada por voz",
                                            tint = if (voiceInputState is VoiceInputState.Listening)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Barcode scanner button
                                if (barcodeScannerManager != null) {
                                    IconButton(onClick = { showBarcodeScanner = true }) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "Escanear codigo",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    if (voiceInputState is VoiceInputState.Listening) {
                        Text(
                            text = "Escuchando...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (voiceInputState is VoiceInputState.Error) {
                        Text(
                            text = (voiceInputState as VoiceInputState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quantity and unit row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { newValue ->
                                // Allow only valid number input
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    quantity = newValue
                                }
                            },
                            label = { Text("Cantidad") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unidad") },
                            placeholder = { Text("kg, ud, l...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (itemName.isNotBlank()) {
                                        val qty = quantity.toDoubleOrNull() ?: 1.0
                                        val unitValue = unit.ifBlank { null }
                                        onAdd(itemName.trim(), qty, unitValue)
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        )
                    }

                    // Quick unit buttons
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ud", "kg", "g", "l", "ml").forEach { quickUnit ->
                            FilterChip(
                                selected = unit == quickUnit,
                                onClick = { unit = if (unit == quickUnit) "" else quickUnit },
                                label = { Text(quickUnit) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (itemName.isNotBlank()) {
                            val qty = quantity.toDoubleOrNull() ?: 1.0
                            val unitValue = unit.ifBlank { null }
                            onAdd(itemName.trim(), qty, unitValue)
                        }
                    },
                    enabled = itemName.isNotBlank()
                ) {
                    Text("Anadir")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun QuickAddItemField(
    onAdd: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Anadir articulo rapido..."
) {
    var itemName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = itemName,
        onValueChange = { itemName = it },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (itemName.isNotBlank()) {
                    onAdd(itemName.trim())
                    itemName = ""
                    keyboardController?.hide()
                }
            }
        )
    )
}
