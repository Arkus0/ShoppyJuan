package com.arkus.shoppyjuan.presentation.prices

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for submitting a price manually
 */
@Composable
fun SubmitPriceDialog(
    productName: String = "",
    onSubmit: (productName: String, price: Double, storeChain: String, barcode: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(productName) }
    var priceText by remember { mutableStateOf("") }
    var selectedChain by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var chainDropdownExpanded by remember { mutableStateOf(false) }

    val storeChains = listOf(
        "Mercadona",
        "Carrefour",
        "DIA",
        "Lidl",
        "Aldi",
        "Alcampo",
        "Eroski",
        "Consum",
        "Hipercor",
        "El Corte Inglés",
        "Bonpreu",
        "Condis",
        "Ahorramas",
        "Otro"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Euro, contentDescription = null) },
        title = { Text("Aportar Precio") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ayuda a la comunidad compartiendo precios que encuentres en las tiendas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Producto") },
                    placeholder = { Text("Ej: Leche entera 1L") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Precio (€)") },
                    placeholder = { Text("Ej: 1,25") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Default.Euro, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Store chain dropdown
                ExposedDropdownMenuBox(
                    expanded = chainDropdownExpanded,
                    onExpandedChange = { chainDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedChain,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supermercado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chainDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = chainDropdownExpanded,
                        onDismissRequest = { chainDropdownExpanded = false }
                    ) {
                        storeChains.forEach { chain ->
                            DropdownMenuItem(
                                text = { Text(chain) },
                                onClick = {
                                    selectedChain = chain
                                    chainDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it.filter { c -> c.isDigit() } },
                    label = { Text("Codigo de barras (opcional)") },
                    placeholder = { Text("Ej: 8480000123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val price = priceText.replace(",", ".").toDoubleOrNull()
            Button(
                onClick = {
                    if (price != null && name.isNotBlank() && selectedChain.isNotBlank()) {
                        onSubmit(name, price, selectedChain, barcode.takeIf { it.isNotBlank() })
                    }
                },
                enabled = name.isNotBlank() && price != null && selectedChain.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Dialog for uploading a receipt
 */
@Composable
fun UploadReceiptDialog(
    isUploading: Boolean,
    progress: String?,
    onUpload: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // Handle camera result
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
        title = { Text("Subir Ticket") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator()
                    Text(
                        text = progress ?: "Procesando...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Sube una foto de tu ticket de compra y extraeremos los precios automaticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Camera button
                        FilledTonalButton(
                            onClick = {
                                // TODO: Implement camera capture with temp file
                                imagePickerLauncher.launch("image/*")
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Camara")
                        }

                        // Gallery button
                        FilledTonalButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galeria")
                        }
                    }

                    if (selectedImageUri != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Imagen seleccionada")
                            }
                        }
                    }

                    // Tips
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Consejos:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Asegurate de que el ticket este bien iluminado\n" +
                                        "• Captura todo el ticket, incluyendo el total\n" +
                                        "• Evita sombras y reflejos",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isUploading) {
                Button(
                    onClick = {
                        selectedImageUri?.let { onUpload(it) }
                    },
                    enabled = selectedImageUri != null
                ) {
                    Text("Analizar")
                }
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

/**
 * Bottom sheet for price actions on a list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceActionsBottomSheet(
    onAnalyzePrices: () -> Unit,
    onSubmitPrice: () -> Unit,
    onUploadReceipt: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Comparador de Precios",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Analizar precios de la lista") },
                supportingContent = { Text("Ver donde comprar mas barato") },
                leadingContent = {
                    Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable { onAnalyzePrices(); onDismiss() }
            )

            ListItem(
                headlineContent = { Text("Aportar un precio") },
                supportingContent = { Text("Comparte precios que encuentres") },
                leadingContent = {
                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.secondary)
                },
                modifier = Modifier.clickable { onSubmitPrice(); onDismiss() }
            )

            ListItem(
                headlineContent = { Text("Subir ticket de compra") },
                supportingContent = { Text("Extraemos los precios automaticamente") },
                leadingContent = {
                    Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.tertiary)
                },
                modifier = Modifier.clickable { onUploadReceipt(); onDismiss() }
            )
        }
    }
}
