package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a store/supermarket
 */
@Serializable
@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val chain: String, // e.g., "Mercadona", "Carrefour", "DIA"
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val osmId: Long? = null, // OpenStreetMap ID for Open Prices
    val logoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Source of the price data
 */
@Serializable
enum class PriceSource {
    OPEN_PRICES,      // From Open Prices API
    USER_MANUAL,      // User entered manually
    USER_RECEIPT,     // Extracted from receipt/ticket
    CROWDSOURCED      // Verified by multiple users
}

/**
 * Represents a price record for a product at a store
 */
@Serializable
@Entity(
    tableName = "price_records",
    indices = [
        Index(value = ["productName"]),
        Index(value = ["normalizedName"]),
        Index(value = ["barcode"]),
        Index(value = ["storeId"]),
        Index(value = ["updatedAt"])
    ]
)
data class PriceRecordEntity(
    @PrimaryKey
    val id: String,
    val productName: String,
    val normalizedName: String, // Lowercase, no accents for fuzzy matching
    val barcode: String? = null,
    val price: Double,
    val currency: String = "EUR",
    val unit: String? = null, // kg, L, unit, etc.
    val pricePerUnit: Double? = null, // Price per kg/L for comparison
    val storeId: String,
    val storeName: String, // Denormalized for quick access
    val storeChain: String,
    val source: PriceSource,
    val confidence: Float = 1.0f, // 0-1, higher = more reliable
    val reportCount: Int = 1, // How many users reported this price
    val userId: String? = null, // Who submitted it
    val receiptId: String? = null, // If from receipt
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val validUntil: Long? = null, // Price expiration (for offers)
    val contributedToOpenPrices: Boolean = false // If shared to Open Prices API
)

/**
 * Represents an uploaded receipt/ticket
 */
@Serializable
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val storeId: String? = null,
    val storeName: String? = null,
    val imageUri: String,
    val totalAmount: Double? = null,
    val purchaseDate: Long? = null,
    val status: ReceiptStatus = ReceiptStatus.PENDING,
    val extractedItemsCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val contributedToOpenPrices: Boolean = false // If shared to Open Prices API
)

@Serializable
enum class ReceiptStatus {
    PENDING,      // Waiting to be processed
    PROCESSING,   // Currently being analyzed
    COMPLETED,    // Successfully processed
    FAILED,       // Processing failed
    NEEDS_REVIEW  // Needs user verification
}

/**
 * Represents an item extracted from a receipt
 */
@Serializable
@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["receiptId"])]
)
data class ReceiptItemEntity(
    @PrimaryKey
    val id: String,
    val receiptId: String,
    val rawText: String, // Original text from OCR
    val productName: String, // Cleaned/parsed name
    val normalizedName: String,
    val quantity: Double = 1.0,
    val unitPrice: Double? = null,
    val totalPrice: Double,
    val barcode: String? = null,
    val confidence: Float = 0.0f, // OCR confidence
    val isVerified: Boolean = false,
    val matchedPriceId: String? = null // If matched to existing price record
)

/**
 * User's price contribution stats
 */
@Serializable
@Entity(tableName = "price_contributors")
data class PriceContributorEntity(
    @PrimaryKey
    val userId: String,
    val totalPricesSubmitted: Int = 0,
    val totalReceiptsUploaded: Int = 0,
    val totalVerifications: Int = 0,
    val accuracyScore: Float = 1.0f, // Based on community verification
    val lastContributionAt: Long? = null,
    val badges: String? = null // JSON array of earned badges
)
