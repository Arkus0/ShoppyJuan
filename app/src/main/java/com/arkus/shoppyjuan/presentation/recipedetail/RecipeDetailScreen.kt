package com.arkus.shoppyjuan.presentation.recipedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arkus.shoppyjuan.domain.model.ShoppingList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    navController: NavController,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exportSuccess) {
        if (uiState.exportSuccess) {
            viewModel.clearExportSuccess()
            // Show success message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.recipe?.name ?: "Receta") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (uiState.recipe?.isFavorite == true) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            "Favorito"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showExportDialog = true },
                icon = { Icon(Icons.Default.PlaylistAdd, null) },
                text = { Text("Exportar a lista") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.recipe?.let { recipe ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Image
                    recipe.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = recipe.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(Modifier.padding(16.dp)) {
                        // Category & Area
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recipe.category?.let {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(it) }
                                )
                            }
                            recipe.area?.let {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(it) }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Ingredients
                        Text(
                            "Ingredientes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        recipe.ingredients.forEachIndexed { index, ingredient ->
                            val measure = recipe.measures.getOrNull(index) ?: ""
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Text("• ")
                                Text("$measure $ingredient")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Instructions
                        recipe.instructions?.let {
                            Text(
                                "Instrucciones",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(it)
                        }

                        Spacer(Modifier.height(80.dp)) // FAB space
                    }
                }
            }
        }

        if (showExportDialog) {
            ExportToListDialog(
                lists = uiState.lists,
                onDismiss = { showExportDialog = false },
                onExport = { listId, multiplier ->
                    viewModel.exportToList(listId, multiplier)
                    showExportDialog = false
                }
            )
        }
    }
}

@Composable
fun ExportToListDialog(
    lists: List<ShoppingList>,
    onDismiss: () -> Unit,
    onExport: (String, Int) -> Unit
) {
    var selectedListId by remember { mutableStateOf(lists.firstOrNull()?.id ?: "") }
    var multiplier by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar ingredientes") },
        text = {
            Column {
                Text("Selecciona la lista:")
                Spacer(Modifier.height(8.dp))
                lists.forEach { list ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedListId == list.id,
                            onClick = { selectedListId = list.id }
                        )
                        Text(list.name)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = multiplier,
                    onValueChange = { multiplier = it },
                    label = { Text("Multiplicador") },
                    placeholder = { Text("1") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mult = multiplier.toIntOrNull() ?: 1
                    onExport(selectedListId, mult)
                }
            ) {
                Text("Exportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
