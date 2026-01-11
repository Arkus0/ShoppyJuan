package com.arkus.shoppyjuan.domain.price

import com.arkus.shoppyjuan.data.local.entity.PriceRecordEntity
import com.arkus.shoppyjuan.data.repository.PriceRepository
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.util.FuzzySearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Results of price analysis for a shopping list
 */
data class PriceAnalysisResult(
    val items: List<ItemPriceAnalysis>,
    val storeRecommendations: List<StoreRecommendation>,
    val bestSingleStore: StoreRecommendation?,
    val totalSavingsIfOptimal: Double,
    val coveragePercentage: Float, // % of items with price data
    val analyzedAt: Long = System.currentTimeMillis()
)

/**
 * Price analysis for a single item
 */
data class ItemPriceAnalysis(
    val item: ListItem,
    val prices: List<StorePriceInfo>,
    val cheapestStore: String?,
    val cheapestPrice: Double?,
    val mostExpensivePrice: Double?,
    val priceDifference: Double?, // Max - Min
    val averagePrice: Double?,
    val hasPriceData: Boolean
)

/**
 * Price info for a specific store
 */
data class StorePriceInfo(
    val storeChain: String,
    val storeName: String,
    val price: Double,
    val unit: String?,
    val lastUpdated: Long,
    val confidence: Float,
    val source: String
)

/**
 * Recommendation for a store
 */
data class StoreRecommendation(
    val storeChain: String,
    val totalEstimatedCost: Double,
    val itemsCovered: Int,
    val itemsMissing: Int,
    val averageSavings: Double, // vs average price
    val cheapestItemsCount: Int // How many items are cheapest here
)

/**
 * Optimal shopping strategy (split across stores)
 */
data class OptimalShoppingStrategy(
    val stores: List<StoreShoppingList>,
    val totalCost: Double,
    val savingsVsSingleStore: Double
)

data class StoreShoppingList(
    val storeChain: String,
    val items: List<Pair<ListItem, Double>>, // Item to price
    val subtotal: Double
)

@Singleton
class PriceAnalyzer @Inject constructor(
    private val priceRepository: PriceRepository
) {

    /**
     * Analyze prices for a shopping list
     */
    suspend fun analyzeList(items: List<ListItem>): PriceAnalysisResult = withContext(Dispatchers.IO) {
        val productNames = items.map { it.name }
        val pricesMap = priceRepository.getPricesForProducts(productNames)

        val itemAnalyses = items.map { item ->
            analyzeItem(item, pricesMap[item.name] ?: emptyList())
        }

        val storeRecommendations = calculateStoreRecommendations(itemAnalyses)
        val bestSingleStore = storeRecommendations.maxByOrNull {
            it.itemsCovered - (it.totalEstimatedCost / 100) // Balance coverage and cost
        }

        val itemsWithPrices = itemAnalyses.count { it.hasPriceData }
        val coveragePercentage = if (items.isNotEmpty()) {
            (itemsWithPrices.toFloat() / items.size) * 100
        } else 0f

        // Calculate potential savings
        val optimalTotal = itemAnalyses.mapNotNull { it.cheapestPrice }.sum()
        val averageTotal = itemAnalyses.mapNotNull { it.averagePrice }.sum()
        val totalSavings = averageTotal - optimalTotal

        PriceAnalysisResult(
            items = itemAnalyses,
            storeRecommendations = storeRecommendations.sortedBy { it.totalEstimatedCost },
            bestSingleStore = bestSingleStore,
            totalSavingsIfOptimal = totalSavings,
            coveragePercentage = coveragePercentage
        )
    }

    /**
     * Analyze a single item
     */
    private fun analyzeItem(item: ListItem, prices: List<PriceRecordEntity>): ItemPriceAnalysis {
        if (prices.isEmpty()) {
            return ItemPriceAnalysis(
                item = item,
                prices = emptyList(),
                cheapestStore = null,
                cheapestPrice = null,
                mostExpensivePrice = null,
                priceDifference = null,
                averagePrice = null,
                hasPriceData = false
            )
        }

        // Group by store chain and get best price per chain
        val pricesByChain = prices
            .groupBy { it.storeChain }
            .mapValues { (_, chainPrices) ->
                chainPrices.minByOrNull { it.price }!!
            }
            .values
            .sortedBy { it.price }

        val storePrices = pricesByChain.map { price ->
            StorePriceInfo(
                storeChain = price.storeChain,
                storeName = price.storeName,
                price = price.price * (item.quantity ?: 1).toDouble(),
                unit = price.unit,
                lastUpdated = price.updatedAt,
                confidence = price.confidence,
                source = price.source.name
            )
        }

        val cheapest = storePrices.firstOrNull()
        val mostExpensive = storePrices.lastOrNull()
        val avgPrice = storePrices.map { it.price }.average()

        return ItemPriceAnalysis(
            item = item,
            prices = storePrices,
            cheapestStore = cheapest?.storeChain,
            cheapestPrice = cheapest?.price,
            mostExpensivePrice = mostExpensive?.price,
            priceDifference = if (cheapest != null && mostExpensive != null) {
                mostExpensive.price - cheapest.price
            } else null,
            averagePrice = avgPrice,
            hasPriceData = true
        )
    }

    /**
     * Calculate recommendations per store
     */
    private fun calculateStoreRecommendations(
        itemAnalyses: List<ItemPriceAnalysis>
    ): List<StoreRecommendation> {
        // Get all unique store chains
        val allChains = itemAnalyses
            .flatMap { it.prices }
            .map { it.storeChain }
            .distinct()

        return allChains.map { chain ->
            var totalCost = 0.0
            var itemsCovered = 0
            var cheapestCount = 0
            var totalSavings = 0.0

            itemAnalyses.forEach { analysis ->
                val priceAtChain = analysis.prices.find { it.storeChain == chain }
                if (priceAtChain != null) {
                    totalCost += priceAtChain.price
                    itemsCovered++

                    if (analysis.cheapestStore == chain) {
                        cheapestCount++
                    }

                    analysis.averagePrice?.let { avg ->
                        totalSavings += avg - priceAtChain.price
                    }
                }
            }

            StoreRecommendation(
                storeChain = chain,
                totalEstimatedCost = totalCost,
                itemsCovered = itemsCovered,
                itemsMissing = itemAnalyses.size - itemsCovered,
                averageSavings = totalSavings,
                cheapestItemsCount = cheapestCount
            )
        }
    }

    /**
     * Calculate optimal shopping strategy (splitting across stores)
     */
    suspend fun calculateOptimalStrategy(items: List<ListItem>): OptimalShoppingStrategy =
        withContext(Dispatchers.IO) {
            val analysis = analyzeList(items)

            // Group items by their cheapest store
            val itemsByStore = mutableMapOf<String, MutableList<Pair<ListItem, Double>>>()

            analysis.items.forEach { itemAnalysis ->
                if (itemAnalysis.hasPriceData && itemAnalysis.cheapestStore != null) {
                    val store = itemAnalysis.cheapestStore
                    val price = itemAnalysis.cheapestPrice!!

                    itemsByStore.getOrPut(store) { mutableListOf() }
                        .add(itemAnalysis.item to price)
                }
            }

            val storeShoppingLists = itemsByStore.map { (chain, itemPrices) ->
                StoreShoppingList(
                    storeChain = chain,
                    items = itemPrices,
                    subtotal = itemPrices.sumOf { it.second }
                )
            }.sortedByDescending { it.subtotal }

            val totalOptimalCost = storeShoppingLists.sumOf { it.subtotal }
            val singleStoreCost = analysis.bestSingleStore?.totalEstimatedCost ?: totalOptimalCost

            OptimalShoppingStrategy(
                stores = storeShoppingLists,
                totalCost = totalOptimalCost,
                savingsVsSingleStore = singleStoreCost - totalOptimalCost
            )
        }

    /**
     * Get price suggestions while user types (for quick lookup)
     */
    suspend fun getQuickPriceSuggestion(productName: String): StorePriceInfo? =
        withContext(Dispatchers.IO) {
            val prices = priceRepository.searchPrices(productName)
            prices.minByOrNull { it.price }?.let { cheapest ->
                StorePriceInfo(
                    storeChain = cheapest.storeChain,
                    storeName = cheapest.storeName,
                    price = cheapest.price,
                    unit = cheapest.unit,
                    lastUpdated = cheapest.updatedAt,
                    confidence = cheapest.confidence,
                    source = cheapest.source.name
                )
            }
        }
}
