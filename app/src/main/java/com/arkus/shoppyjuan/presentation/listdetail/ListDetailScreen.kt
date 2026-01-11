package com.arkus.shoppyjuan.presentation.listdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arkus.shoppyjuan.data.barcode.BarcodeScannerManager
import com.arkus.shoppyjuan.data.speech.VoiceInputManager
import com.arkus.shoppyjuan.data.speech.VoiceInputState
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    navController: NavController,
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showOnlineUsersDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.list?.name ?: "Lista de Compras",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Notes button with badge
                    BadgedBox(
                        badge = {
                            if (uiState.noteCount > 0) {
                                Badge { Text(uiState.noteCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = { showNotesSheet = true }) {
                            Icon(Icons.Default.Comment, contentDescription = "Notas")
                        }
                    }
                    IconButton(onClick = { viewModel.clearCheckedItems() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar marcados")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir artículo")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Lista vacía",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Añade artículos a tu lista",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Presence indicator
                        if (uiState.onlineUsers.any { it.isOnline && it.userId != "current_user_id" }) {
                            item {
                                PresenceChip(
                                    onlineUsers = uiState.onlineUsers,
                                    currentUserId = "current_user_id", // TODO: Get from AuthRepository
                                    onClick = { showOnlineUsersDialog = true },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        if (uiState.uncheckedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Pendientes (${uiState.uncheckedItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(uiState.uncheckedItems) { item ->
                                ItemCard(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleItemChecked(item.id, checked)
                                    },
                                    onDelete = { viewModel.deleteItem(item.id) }
                                )
                            }
                        }

                        if (uiState.checkedItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Completados (${uiState.checkedItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(uiState.checkedItems) { item ->
                                ItemCard(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleItemChecked(item.id, checked)
                                    },
                                    onDelete = { viewModel.deleteItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddItemDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, quantity, unit ->
                    viewModel.addItem(name, quantity, unit)
                    showAddDialog = false
                }
            )
        }

        if (showNotesSheet) {
            NotesBottomSheet(
                notes = uiState.notes,
                currentUserId = "current_user_id", // TODO: Get from AuthRepository
                currentUserName = "Usuario", // TODO: Get from AuthRepository
                onDismiss = { showNotesSheet = false },
                onAddNote = { content ->
                    viewModel.addNote(content, "current_user_id", "Usuario")
                },
                onDeleteNote = { noteId ->
                    viewModel.deleteNote(noteId)
                }
            )
        }

        if (showOnlineUsersDialog) {
            OnlineUsersDialog(
                onlineUsers = uiState.onlineUsers,
                currentUserId = "current_user_id", // TODO: Get from AuthRepository
                onDismiss = { showOnlineUsersDialog = false }
            )
        }
    }
}

@Composable
fun ItemCard(
    item: ListItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "${item.emoji ?: ""} ${item.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.quantity > 0 || item.unit != null) {
                    Text(
                        text = "${item.quantity}${item.unit ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                item.category?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String?) -> Unit,
    voiceInputManager: VoiceInputManager = hiltViewModel<ListDetailViewModel>().voiceInputManager,
    barcodeScannerManager: BarcodeScannerManager = hiltViewModel<ListDetailViewModel>().barcodeScannerManager
) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var voiceInputState by remember { mutableStateOf<VoiceInputState>(VoiceInputState.Idle) }

    if (showBarcodeScanner) {
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
            title = { Text("Añadir Artículo") },
            text = {
                Column {
                    // Input field con iconos de voz y barcode
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Nombre del artículo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                // Voice input button
                                IconButton(
                                    onClick = {
                                        voiceInputState = VoiceInputState.Listening
                                        LaunchedEffect(Unit) {
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
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Barcode scanner button
                                IconButton(onClick = { showBarcodeScanner = true }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Escanear código"
                                    )
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Cantidad") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unidad") },
                            placeholder = { Text("kg, ud, l...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (itemName.isNotBlank()) {
                            val qty = quantity.toDoubleOrNull() ?: 1.0
                            val unitValue = if (unit.isBlank()) null else unit
                            onAdd(itemName, qty, unitValue)
                        }
                    },
                    enabled = itemName.isNotBlank()
                ) {
                    Text("Añadir")
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
