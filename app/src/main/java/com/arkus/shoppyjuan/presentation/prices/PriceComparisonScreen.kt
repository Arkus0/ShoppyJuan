package com.arkus.shoppyjuan.presentation.prices

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.arkus.shoppyjuan.domain.price.*
import java.text.NumberFormat
import java.util.Locale

private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceComparisonScreen(
    uiState: PriceComparisonUiState,
    onViewModeChange: (PriceViewMode) -> Unit,
    onVerifyPrice: (String) -> Unit,
    onShowOpenPricesLogin: (Boolean) -> Unit,
    onShowContributeDialog: (Boolean) -> Unit,
    onLoginToOpenPrices: (String, String) -> Unit,
    onLogoutFromOpenPrices: () -> Unit,
    onContributeAllReceipts: () -> Unit,
    onBack: () -> Unit
) {
    // Show Open Prices login dialog
    if (uiState.showOpenPricesLoginDialog) {
        OpenPricesLoginDialog(
            isLoading = uiState.isContributing,
            error = uiState.error,
            onLogin = onLoginToOpenPrices,
            onDismiss = { onShowOpenPricesLogin(false) }
        )
    }

    // Show contribute dialog
    if (uiState.showContributeDialog) {
        ContributeToOpenPricesDialog(
            isAuthenticated = uiState.isOpenPricesAuthenticated,
            username = uiState.openPricesUsername,
            uncontributedCount = uiState.uncontributedReceiptsCount,
            pricesContributed = uiState.openPricesPricesContributed,
            isContributing = uiState.isContributing,
            progress = uiState.contributionProgress,
            onLogin = { onShowOpenPricesLogin(true) },
            onLogout = onLogoutFromOpenPrices,
            onContributeAll = onContributeAllReceipts,
            onDismiss = { onShowContributeDialog(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Comparador de Precios")
                        if (uiState.listName.isNotEmpty()) {
                            Text(
                                text = uiState.listName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Open Prices contribute button with badge
                    BadgedBox(
                        badge = {
                            if (uiState.uncontributedReceiptsCount > 0) {
                                Badge { Text(uiState.uncontributedReceiptsCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = { onShowContributeDialog(true) }) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = "Compartir con Open Prices",
                                tint = if (uiState.isOpenPricesAuthenticated) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View mode tabs
            TabRow(
                selectedTabIndex = uiState.selectedView.ordinal
            ) {
                Tab(
                    selected = uiState.selectedView == PriceViewMode.BY_ITEM,
                    onClick = { onViewModeChange(PriceViewMode.BY_ITEM) },
                    text = { Text("Por Producto") },
                    icon = { Icon(Icons.Default.ShoppingCart, null) }
                )
                Tab(
                    selected = uiState.selectedView == PriceViewMode.BY_STORE,
                    onClick = { onViewModeChange(PriceViewMode.BY_STORE) },
                    text = { Text("Por Tienda") },
                    icon = { Icon(Icons.Default.Store, null) }
                )
                Tab(
                    selected = uiState.selectedView == PriceViewMode.OPTIMAL,
                    onClick = { onViewModeChange(PriceViewMode.OPTIMAL) },
                    text = { Text("Optimo") },
                    icon = { Icon(Icons.Default.Stars, null) }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cargando lista...")
                        }
                    }
                }

                uiState.isAnalyzing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analizando precios...")
                        }
                    }
                }

                uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Lista vacia",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Anade productos a la lista para comparar precios",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                uiState.analysisResult == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay datos de precios disponibles",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    // Summary card
                    PriceAnalysisSummary(
                        analysis = uiState.analysisResult,
                        optimalStrategy = uiState.optimalStrategy
                    )

                    // Content based on view mode
                    when (uiState.selectedView) {
                        PriceViewMode.BY_ITEM -> ItemPricesList(
                            items = uiState.analysisResult.items,
                            onVerifyPrice = onVerifyPrice
                        )
                        PriceViewMode.BY_STORE -> StoreRecommendationsList(
                            recommendations = uiState.analysisResult.storeRecommendations,
                            bestStore = uiState.analysisResult.bestSingleStore
                        )
                        PriceViewMode.OPTIMAL -> OptimalStrategyView(
                            strategy = uiState.optimalStrategy
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceAnalysisSummary(
    analysis: PriceAnalysisResult,
    optimalStrategy: OptimalShoppingStrategy?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Cobertura de precios",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "${analysis.coveragePercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Ahorro potencial",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = currencyFormatter.format(analysis.totalSavingsIfOptimal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (analysis.totalSavingsIfOptimal > 0) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            if (optimalStrategy != null && optimalStrategy.stores.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Visitando ${optimalStrategy.stores.size} tiendas ahorras ${
                        currencyFormatter.format(optimalStrategy.savingsVsSingleStore)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ItemPricesList(
    items: List<ItemPriceAnalysis>,
    onVerifyPrice: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { itemAnalysis ->
            ItemPriceCard(
                analysis = itemAnalysis,
                onVerifyPrice = onVerifyPrice
            )
        }
    }
}

@Composable
private fun ItemPriceCard(
    analysis: ItemPriceAnalysis,
    onVerifyPrice: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${analysis.item.emoji ?: ""} ${analysis.item.name}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (analysis.hasPriceData) {
                        Text(
                            text = "Mejor: ${analysis.cheapestStore}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text(
                            text = "Sin datos de precios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (analysis.hasPriceData && analysis.cheapestPrice != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = currencyFormatter.format(analysis.cheapestPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        if (analysis.priceDifference != null && analysis.priceDifference > 0) {
                            Text(
                                text = "Dif: ${currencyFormatter.format(analysis.priceDifference)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Colapsar" else "Expandir"
                )
            }

            // Expanded content - show all store prices
            AnimatedVisibility(visible = expanded && analysis.hasPriceData) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    analysis.prices.forEachIndexed { index, priceInfo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (index == 0) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = "Mas barato",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Text(
                                    text = priceInfo.storeChain,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currencyFormatter.format(priceInfo.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                )

                                // Show price difference from cheapest
                                if (index > 0 && analysis.cheapestPrice != null) {
                                    val diff = priceInfo.price - analysis.cheapestPrice
                                    Text(
                                        text = " (+${currencyFormatter.format(diff)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreRecommendationsList(
    recommendations: List<StoreRecommendation>,
    bestStore: StoreRecommendation?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recommendations) { recommendation ->
            StoreRecommendationCard(
                recommendation = recommendation,
                isBest = recommendation.storeChain == bestStore?.storeChain
            )
        }
    }
}

@Composable
private fun StoreRecommendationCard(
    recommendation: StoreRecommendation,
    isBest: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isBest) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isBest) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Recomendado",
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = recommendation.storeChain,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = currencyFormatter.format(recommendation.totalEstimatedCost),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isBest) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${recommendation.itemsCovered} productos disponibles",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (recommendation.itemsMissing > 0) {
                    Text(
                        text = "${recommendation.itemsMissing} no encontrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (recommendation.cheapestItemsCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${recommendation.cheapestItemsCount} productos mas baratos aqui",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun OptimalStrategyView(
    strategy: OptimalShoppingStrategy?
) {
    if (strategy == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay datos suficientes")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Estrategia Optima",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visita ${strategy.stores.size} tienda(s) para el mejor precio",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: ${currencyFormatter.format(strategy.totalCost)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    if (strategy.savingsVsSingleStore > 0) {
                        Text(
                            text = "Ahorras ${currencyFormatter.format(strategy.savingsVsSingleStore)} vs ir a una sola tienda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        items(strategy.stores) { storeList ->
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = storeList.storeChain,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currencyFormatter.format(storeList.subtotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    storeList.items.forEach { (item, price) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.emoji ?: ""} ${item.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currencyFormatter.format(price),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
