package com.arkus.shoppyjuan.presentation.supermarket

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arkus.shoppyjuan.domain.model.ListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupermarketModeScreen(
    navController: NavController,
    viewModel: SupermarketModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = if (uiState.totalItems > 0) {
        uiState.checkedItems.toFloat() / uiState.totalItems.toFloat()
    } else 0f
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.listName ?: "Modo Supermercado",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.checkedItems}/${uiState.totalItems} completados",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Group items by category
                    val grouped = uiState.items.groupBy { it.category ?: "Otros" }

                    grouped.forEach { (category, items) ->
                        // Category header
                        item(key = "header_$category") {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Items in category
                        val pendingItems = items.filter { !it.checked }
                        items(pendingItems, key = { it.id }) { item ->
                            SupermarketItemCard(
                                item = item,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleItemChecked(item.id, checked)
                                },
                                onMarkUnavailable = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.markUnavailable(item.id)
                                }
                            )
                        }
                    }

                    // Completed items (collapsible)
                    if (uiState.checkedItems > 0) {
                        item(key = "completed_header") {
                            var expanded by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onClick = { expanded = !expanded }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✓ Completados (${uiState.checkedItems})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (expanded) "Contraer" else "Expandir"
                                    )
                                }
                            }

                            if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.items.filter { it.checked }.forEach { item ->
                                    SupermarketItemCard(
                                        item = item,
                                        onCheckedChange = { checked ->
                                            viewModel.toggleItemChecked(item.id, checked)
                                        },
                                        onMarkUnavailable = {}
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Keep screen on during shopping
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun SupermarketItemCard(
    item: ListItem,
    onCheckedChange: (Boolean) -> Unit,
    onMarkUnavailable: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (item.checked) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        }
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
                    text = "${item.emoji ?: "📦"} ${item.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.quantity > 0 || item.unit != null) {
                    Text(
                        text = "${item.quantity}${item.unit ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (!item.checked) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No había") },
                        onClick = {
                            onMarkUnavailable()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) }
                    )
                }
            }
        }
    }
}
