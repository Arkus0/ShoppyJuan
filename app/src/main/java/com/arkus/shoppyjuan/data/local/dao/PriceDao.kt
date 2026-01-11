package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {
    // ==================== STORES ====================

    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE chain = :chain")
    fun getStoresByChain(chain: String): Flow<List<StoreEntity>>

    @Query("SELECT DISTINCT chain FROM stores ORDER BY chain ASC")
    fun getAllChains(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<StoreEntity>)

    // ==================== PRICE RECORDS ====================

    @Query("""
        SELECT * FROM price_records
        WHERE normalizedName LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchPrices(query: String, limit: Int = 50): List<PriceRecordEntity>

    @Query("""
        SELECT * FROM price_records
        WHERE barcode = :barcode
        ORDER BY updatedAt DESC
    """)
    suspend fun getPricesByBarcode(barcode: String): List<PriceRecordEntity>

    @Query("""
        SELECT * FROM price_records
        WHERE normalizedName = :normalizedName
        ORDER BY price ASC
    """)
    suspend fun getPricesByExactName(normalizedName: String): List<PriceRecordEntity>

    @Query("""
        SELECT * FROM price_records
        WHERE storeChain = :chain
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    suspend fun getPricesByChain(chain: String, limit: Int = 100): List<PriceRecordEntity>

    @Query("""
        SELECT * FROM price_records
        WHERE productName IN (:productNames)
        ORDER BY productName, price ASC
    """)
    suspend fun getPricesForProducts(productNames: List<String>): List<PriceRecordEntity>

    @Query("""
        SELECT * FROM price_records
        WHERE normalizedName IN (:normalizedNames)
        ORDER BY normalizedName, price ASC
    """)
    suspend fun getPricesForNormalizedNames(normalizedNames: List<String>): List<PriceRecordEntity>

    @Query("""
        SELECT pr.* FROM price_records pr
        INNER JOIN (
            SELECT normalizedName, MIN(price) as minPrice
            FROM price_records
            WHERE normalizedName IN (:normalizedNames)
            GROUP BY normalizedName
        ) best ON pr.normalizedName = best.normalizedName AND pr.price = best.minPrice
    """)
    suspend fun getCheapestPricesForProducts(normalizedNames: List<String>): List<PriceRecordEntity>

    @Query("""
        SELECT * FROM price_records
        WHERE updatedAt > :since
        ORDER BY updatedAt DESC
    """)
    suspend fun getRecentPrices(since: Long): List<PriceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: PriceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<PriceRecordEntity>)

    @Query("""
        UPDATE price_records
        SET reportCount = reportCount + 1,
            confidence = MIN(1.0, confidence + 0.1),
            updatedAt = :timestamp
        WHERE id = :priceId
    """)
    suspend fun incrementReportCount(priceId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM price_records WHERE updatedAt < :before")
    suspend fun deleteOldPrices(before: Long)

    // ==================== RECEIPTS ====================

    @Query("SELECT * FROM receipts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReceiptsByUser(userId: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    suspend fun getReceiptById(receiptId: String): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getReceiptsByStatus(status: ReceiptStatus): List<ReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Query("UPDATE receipts SET status = :status, processedAt = :processedAt WHERE id = :receiptId")
    suspend fun updateReceiptStatus(receiptId: String, status: ReceiptStatus, processedAt: Long? = null)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    // ==================== RECEIPT ITEMS ====================

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun getReceiptItems(receiptId: String): List<ReceiptItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<ReceiptItemEntity>)

    @Query("UPDATE receipt_items SET isVerified = 1, matchedPriceId = :priceId WHERE id = :itemId")
    suspend fun verifyReceiptItem(itemId: String, priceId: String?)

    @Query("DELETE FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteReceiptItems(receiptId: String)

    // ==================== CONTRIBUTORS ====================

    @Query("SELECT * FROM price_contributors WHERE userId = :userId")
    suspend fun getContributor(userId: String): PriceContributorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributor(contributor: PriceContributorEntity)

    @Query("""
        UPDATE price_contributors
        SET totalPricesSubmitted = totalPricesSubmitted + 1,
            lastContributionAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun incrementPriceCount(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE price_contributors
        SET totalReceiptsUploaded = totalReceiptsUploaded + 1,
            lastContributionAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun incrementReceiptCount(userId: String, timestamp: Long = System.currentTimeMillis())
}
