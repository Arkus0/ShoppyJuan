package com.arkus.shoppyjuan.data.repository

import android.net.Uri
import com.arkus.shoppyjuan.data.local.dao.PriceDao
import com.arkus.shoppyjuan.data.local.entity.*
import com.arkus.shoppyjuan.data.remote.ContributionSummary
import com.arkus.shoppyjuan.data.remote.OpenPricesContributor
import com.arkus.shoppyjuan.data.remote.api.OpenPricesApi
import com.arkus.shoppyjuan.data.remote.api.OpenPriceSubmissionResponse
import com.arkus.shoppyjuan.data.remote.api.OpenUserStatsResponse
import com.arkus.shoppyjuan.domain.util.FuzzySearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceRepository @Inject constructor(
    private val priceDao: PriceDao,
    private val openPricesApi: OpenPricesApi,
    private val openPricesContributor: OpenPricesContributor
) {
    // ==================== STORES ====================

    fun getAllStores(): Flow<List<StoreEntity>> = priceDao.getAllStores()

    fun getAllChains(): Flow<List<String>> = priceDao.getAllChains()

    suspend fun addStore(store: StoreEntity) = priceDao.insertStore(store)

    // ==================== PRICE SEARCH ====================

    /**
     * Search for prices using fuzzy matching
     */
    suspend fun searchPrices(query: String): List<PriceRecordEntity> = withContext(Dispatchers.IO) {
        val normalizedQuery = FuzzySearch.normalize(query)
        val localResults = priceDao.searchPrices(normalizedQuery)

        // If we have good local results, return them
        if (localResults.size >= 5) {
            return@withContext localResults
        }

        // Otherwise, try to fetch from Open Prices API
        // Note: Open Prices doesn't support text search, so we rely on barcode or local data
        localResults
    }

    /**
     * Get prices by barcode, combining local and API data
     */
    suspend fun getPricesByBarcode(barcode: String): List<PriceRecordEntity> = withContext(Dispatchers.IO) {
        val localPrices = priceDao.getPricesByBarcode(barcode)

        // Try to fetch fresh data from Open Prices
        try {
            val apiResponse = openPricesApi.getPricesByBarcode(barcode)
            val apiPrices = apiResponse.items.map { item ->
                PriceRecordEntity(
                    id = "op_${item.id}",
                    productName = item.productName ?: "Producto",
                    normalizedName = FuzzySearch.normalize(item.productName ?: "producto"),
                    barcode = item.productCode,
                    price = item.price,
                    currency = item.currency,
                    unit = item.pricePer,
                    storeId = "osm_${item.locationOsmId ?: 0}",
                    storeName = "Tienda", // Would need location lookup
                    storeChain = "Desconocido",
                    source = PriceSource.OPEN_PRICES,
                    confidence = 0.8f,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            // Cache API results locally
            if (apiPrices.isNotEmpty()) {
                priceDao.insertPrices(apiPrices)
            }

            // Combine and deduplicate
            (localPrices + apiPrices).distinctBy { "${it.storeChain}_${it.price}" }
        } catch (e: Exception) {
            // Return local data on API failure
            localPrices
        }
    }

    /**
     * Get prices for multiple products (for list analysis)
     */
    suspend fun getPricesForProducts(productNames: List<String>): Map<String, List<PriceRecordEntity>> =
        withContext(Dispatchers.IO) {
            val normalizedNames = productNames.map { FuzzySearch.normalize(it) }
            val allPrices = priceDao.getPricesForNormalizedNames(normalizedNames)

            // Group by normalized name with fuzzy matching
            productNames.associateWith { name ->
                val normalized = FuzzySearch.normalize(name)
                allPrices.filter { price ->
                    FuzzySearch.isMatch(normalized, price.normalizedName, threshold = 0.6)
                }.sortedBy { it.price }
            }
        }

    /**
     * Get cheapest price for each product
     */
    suspend fun getCheapestPrices(productNames: List<String>): List<PriceRecordEntity> =
        withContext(Dispatchers.IO) {
            val normalizedNames = productNames.map { FuzzySearch.normalize(it) }
            priceDao.getCheapestPricesForProducts(normalizedNames)
        }

    // ==================== PRICE SUBMISSION ====================

    /**
     * Submit a new price (user contribution)
     */
    suspend fun submitPrice(
        productName: String,
        price: Double,
        storeChain: String,
        storeName: String,
        userId: String,
        barcode: String? = null,
        unit: String? = null
    ): PriceRecordEntity = withContext(Dispatchers.IO) {
        val priceRecord = PriceRecordEntity(
            id = UUID.randomUUID().toString(),
            productName = productName,
            normalizedName = FuzzySearch.normalize(productName),
            barcode = barcode,
            price = price,
            unit = unit,
            storeId = "${storeChain}_${storeName}".lowercase().replace(" ", "_"),
            storeName = storeName,
            storeChain = storeChain,
            source = PriceSource.USER_MANUAL,
            confidence = 0.7f,
            userId = userId
        )

        priceDao.insertPrice(priceRecord)

        // Update contributor stats
        ensureContributor(userId)
        priceDao.incrementPriceCount(userId)

        priceRecord
    }

    /**
     * Confirm/verify an existing price (increases confidence)
     */
    suspend fun verifyPrice(priceId: String) = withContext(Dispatchers.IO) {
        priceDao.incrementReportCount(priceId)
    }

    // ==================== RECEIPTS ====================

    fun getReceiptsByUser(userId: String): Flow<List<ReceiptEntity>> =
        priceDao.getReceiptsByUser(userId)

    suspend fun uploadReceipt(
        userId: String,
        imageUri: String,
        storeChain: String? = null
    ): ReceiptEntity = withContext(Dispatchers.IO) {
        val receipt = ReceiptEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            storeName = storeChain,
            imageUri = imageUri,
            status = ReceiptStatus.PENDING
        )

        priceDao.insertReceipt(receipt)

        // Update contributor stats
        ensureContributor(userId)
        priceDao.incrementReceiptCount(userId)

        receipt
    }

    suspend fun updateReceiptWithExtractedItems(
        receiptId: String,
        items: List<ReceiptItemEntity>,
        storeChain: String?,
        totalAmount: Double?
    ) = withContext(Dispatchers.IO) {
        priceDao.insertReceiptItems(items)

        val receipt = priceDao.getReceiptById(receiptId)
        if (receipt != null) {
            priceDao.updateReceipt(
                receipt.copy(
                    storeName = storeChain ?: receipt.storeName,
                    totalAmount = totalAmount,
                    extractedItemsCount = items.size,
                    status = ReceiptStatus.NEEDS_REVIEW,
                    processedAt = System.currentTimeMillis()
                )
            )
        }

        // Convert verified receipt items to price records
        items.forEach { item ->
            val priceRecord = PriceRecordEntity(
                id = UUID.randomUUID().toString(),
                productName = item.productName,
                normalizedName = item.normalizedName,
                barcode = item.barcode,
                price = item.totalPrice / item.quantity,
                storeId = storeChain?.lowercase()?.replace(" ", "_") ?: "unknown",
                storeName = storeChain ?: "Desconocido",
                storeChain = storeChain ?: "Desconocido",
                source = PriceSource.USER_RECEIPT,
                confidence = item.confidence,
                userId = receipt?.userId,
                receiptId = receiptId
            )
            priceDao.insertPrice(priceRecord)
        }
    }

    suspend fun getReceiptItems(receiptId: String): List<ReceiptItemEntity> =
        priceDao.getReceiptItems(receiptId)

    // ==================== CONTRIBUTORS ====================

    private suspend fun ensureContributor(userId: String) {
        if (priceDao.getContributor(userId) == null) {
            priceDao.insertContributor(PriceContributorEntity(userId = userId))
        }
    }

    suspend fun getContributorStats(userId: String): PriceContributorEntity? =
        priceDao.getContributor(userId)

    // ==================== MAINTENANCE ====================

    /**
     * Clean up old prices (older than 30 days for low-confidence ones)
     */
    suspend fun cleanupOldPrices() = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        priceDao.deleteOldPrices(thirtyDaysAgo)
    }

    // ==================== OPEN PRICES CONTRIBUTION ====================

    /**
     * Check if user is authenticated with Open Prices
     */
    val isOpenPricesAuthenticated: Flow<Boolean> = openPricesContributor.isAuthenticated

    /**
     * Get Open Prices username
     */
    val openPricesUsername: Flow<String?> = openPricesContributor.username

    /**
     * Authenticate with Open Food Facts to contribute prices
     */
    suspend fun authenticateOpenPrices(username: String, password: String): Result<String> {
        return openPricesContributor.authenticate(username, password)
    }

    /**
     * Logout from Open Prices
     */
    suspend fun logoutOpenPrices() {
        openPricesContributor.logout()
    }

    /**
     * Contribute a single price to Open Prices
     */
    suspend fun contributePrice(
        price: PriceRecordEntity,
        locationOsmId: Long? = null,
        locationOsmType: String? = null
    ): Result<OpenPriceSubmissionResponse> {
        val result = openPricesContributor.submitPrice(price, locationOsmId, locationOsmType)

        // Mark as contributed locally
        if (result.isSuccess) {
            priceDao.updatePrice(price.copy(
                contributedToOpenPrices = true,
                updatedAt = System.currentTimeMillis()
            ))
        }

        return result
    }

    /**
     * Contribute all prices from a receipt to Open Prices
     */
    suspend fun contributeReceiptPrices(
        receiptId: String,
        storeOsmId: Long? = null,
        storeOsmType: String? = "NODE",
        uploadProof: Boolean = true
    ): Result<ContributionSummary> = withContext(Dispatchers.IO) {
        val receipt = priceDao.getReceiptById(receiptId)
            ?: return@withContext Result.failure(IllegalStateException("Receipt not found"))

        val items = priceDao.getReceiptItems(receiptId)

        // Upload receipt image as proof if requested
        var proofId: Int? = null
        if (uploadProof && receipt.imageUri != null) {
            val proofResult = openPricesContributor.uploadReceiptProof(Uri.parse(receipt.imageUri))
            proofId = proofResult.getOrNull()?.id
        }

        // Submit all prices
        val result = openPricesContributor.submitReceiptPrices(
            items = items,
            receipt = receipt,
            storeOsmId = storeOsmId,
            storeOsmType = storeOsmType,
            proofId = proofId
        )

        // Mark receipt as contributed
        if (result.isSuccess) {
            priceDao.updateReceipt(
                receipt.copy(
                    contributedToOpenPrices = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        result
    }

    /**
     * Get user's Open Prices contribution stats
     */
    suspend fun getOpenPricesStats(): Result<OpenUserStatsResponse> {
        return openPricesContributor.getUserStats()
    }

    /**
     * Get receipts that haven't been contributed to Open Prices yet
     */
    suspend fun getUncontributedReceipts(userId: String): List<ReceiptEntity> = withContext(Dispatchers.IO) {
        priceDao.getUncontributedReceipts(userId)
    }

    /**
     * Get price records that haven't been contributed to Open Prices yet
     */
    suspend fun getUncontributedPrices(limit: Int = 50): List<PriceRecordEntity> = withContext(Dispatchers.IO) {
        priceDao.getUncontributedPrices(limit)
    }
}
