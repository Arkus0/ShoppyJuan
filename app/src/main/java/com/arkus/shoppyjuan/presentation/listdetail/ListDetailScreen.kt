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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.navigation.Screen
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
    var showShareDialog by remember { mutableStateOf(false) }

    // States for Item Context Actions
    var itemToEdit by remember { mutableStateOf<ListItem?>(null) }
    var itemToAddNote by remember { mutableStateOf<ListItem?>(null) }
    var itemToChangeCategory by remember { mutableStateOf<ListItem?>(null) }
    var itemToAssign by remember { mutableStateOf<ListItem?>(null) }

    // Get current user info from ViewModel
    val currentUserId = viewModel.currentUserId
    val currentUserName = viewModel.currentUserName

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.list?.name ?: "Lista de Compras",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.items.isNotEmpty()) {
                            Text(
                                text = "${uiState.uncheckedItems.size} pendientes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Price comparison button
                    uiState.list?.id?.let { listId ->
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.PriceComparison.createRoute(listId))
                            }
                        ) {
                            Icon(
                                Icons.Default.Euro,
                                contentDescription = "Comparar precios"
                            )
                        }
                    }

                    // Supermarket mode button
                    uiState.list?.id?.let { listId ->
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.SupermarketMode.createRoute(listId))
                            }
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Modo supermercado"
                            )
                        }
                    }

                    // Share button
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }

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

                    // Clear checked items
                    if (uiState.checkedItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCheckedItems() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar completados")
                        }
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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Anadir articulo")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.items.isEmpty() -> {
                    EmptyListContent(onAddClick = { showAddDialog = true })
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Presence indicator - show when there are other online users
                        val otherOnlineUsers = uiState.onlineUsers.filter {
                            it.isOnline && it.userId != currentUserId
                        }
                        if (otherOnlineUsers.isNotEmpty()) {
                            item {
                                PresenceChip(
                                    onlineUsers = uiState.onlineUsers,
                                    currentUserId = currentUserId,
                                    onClick = { showOnlineUsersDialog = true },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Progress indicator
                        if (uiState.items.isNotEmpty()) {
                            item {
                                val progress = uiState.checkedItems.size.toFloat() / uiState.items.size
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                        }

                        // Unchecked items section
                        if (uiState.uncheckedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Pendientes (${uiState.uncheckedItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(
                                items = uiState.uncheckedItems,
                                key = { it.id }
                            ) { item ->
                                ItemCard(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleItemChecked(item.id, checked)
                                    },
                                    onDelete = { viewModel.deleteItem(item.id) },
                                    onEdit = { itemToEdit = item },
                                    onAddNote = { itemToAddNote = item },
                                    onCategoryChange = { itemToChangeCategory = item },
                                    onAssign = { itemToAssign = item }
                                )
                            }
                        }

                        // Checked items section
                        if (uiState.checkedItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Completados (${uiState.checkedItems.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            items(
                                items = uiState.checkedItems,
                                key = { it.id }
                            ) { item ->
                                ItemCard(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleItemChecked(item.id, checked)
                                    },
                                    onDelete = { viewModel.deleteItem(item.id) },
                                    onEdit = { itemToEdit = item },
                                    onAddNote = { itemToAddNote = item },
                                    onCategoryChange = { itemToChangeCategory = item },
                                    onAssign = { itemToAssign = item }
                                )
                            }
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // Add item dialog
        if (showAddDialog) {
            AddItemDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, quantity, unit ->
                    viewModel.addItem(name, quantity, unit)
                    showAddDialog = false
                },
                voiceInputManager = viewModel.voiceInputManager,
                barcodeScannerManager = viewModel.barcodeScannerManager
            )
        }

        // Edit Item Dialog
        itemToEdit?.let { item ->
            EditItemDialog(
                item = item,
                onDismiss = { itemToEdit = null },
                onConfirm = { name, quantity, unit ->
                    viewModel.updateItem(item.copy(name = name, quantity = quantity, unit = unit))
                    itemToEdit = null
                }
            )
        }

        // Item Note Dialog
        itemToAddNote?.let { item ->
            ItemNoteDialog(
                currentNote = item.note,
                onDismiss = { itemToAddNote = null },
                onConfirm = { note ->
                    viewModel.updateItem(item.copy(note = note))
                    itemToAddNote = null
                }
            )
        }

        // Category Selection Dialog
        itemToChangeCategory?.let { item ->
            CategorySelectionDialog(
                currentCategory = item.category,
                onDismiss = { itemToChangeCategory = null },
                onCategorySelected = { category ->
                    viewModel.updateItem(item.copy(category = category.label, emoji = category.emoji))
                    itemToChangeCategory = null
                }
            )
        }

        // Assign Dialog
        itemToAssign?.let { item ->
            val otherUsers = uiState.onlineUsers.filter { it.userId != currentUserId }
            AssignItemDialog(
                users = otherUsers,
                onDismiss = { itemToAssign = null },
                onUserSelected = { userId ->
                    viewModel.assignItem(item, userId)
                    itemToAssign = null
                }
            )
        }

        // Notes bottom sheet
        if (showNotesSheet) {
            NotesBottomSheet(
                notes = uiState.notes,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                onDismiss = { showNotesSheet = false },
                onAddNote = { content ->
                    viewModel.addNote(content)
                },
                onDeleteNote = { noteId ->
                    viewModel.deleteNote(noteId)
                }
            )
        }

        // Online users dialog
        if (showOnlineUsersDialog) {
            OnlineUsersDialog(
                onlineUsers = uiState.onlineUsers,
                currentUserId = currentUserId,
                onDismiss = { showOnlineUsersDialog = false }
            )
        }

        // Share dialog
        if (showShareDialog) {
            uiState.list?.let { list ->
                ShareListDialog(
                    list = list,
                    onDismiss = { showShareDialog = false }
                )
            }
        }
    }
}

@Composable
private fun EmptyListContent(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ShoppingBag,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Lista vacia",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Anade articulos a tu lista de compras",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anadir primer articulo")
        }
    }
}
