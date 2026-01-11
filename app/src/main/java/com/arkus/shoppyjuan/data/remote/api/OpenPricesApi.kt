package com.arkus.shoppyjuan.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open Prices API client
 * Documentation: https://prices.openfoodfacts.org/api/docs
 */
interface OpenPricesApi {

    companion object {
        const val BASE_URL = "https://prices.openfoodfacts.org/"
    }

    /**
     * Get prices by product barcode
     */
    @GET("api/v1/prices")
    suspend fun getPricesByBarcode(
        @Query("product_code") barcode: String,
        @Query("page_size") pageSize: Int = 50
    ): OpenPricesResponse

    /**
     * Get prices by product name (Open Food Facts product search)
     */
    @GET("api/v1/prices")
    suspend fun getPricesByProductId(
        @Query("product_id") productId: Int,
        @Query("page_size") pageSize: Int = 50
    ): OpenPricesResponse

    /**
     * Get prices for a specific location/store
     */
    @GET("api/v1/prices")
    suspend fun getPricesByLocation(
        @Query("location_osm_id") osmId: Long,
        @Query("location_osm_type") osmType: String = "NODE",
        @Query("page_size") pageSize: Int = 100
    ): OpenPricesResponse

    /**
     * Get recent prices
     */
    @GET("api/v1/prices")
    suspend fun getRecentPrices(
        @Query("order_by") orderBy: String = "-date",
        @Query("page_size") pageSize: Int = 100
    ): OpenPricesResponse

    /**
     * Get locations/stores
     */
    @GET("api/v1/locations")
    suspend fun getLocations(
        @Query("osm_name__like") nameLike: String? = null,
        @Query("page_size") pageSize: Int = 50
    ): OpenLocationsResponse

    /**
     * Get products from Open Food Facts
     */
    @GET("api/v1/products")
    suspend fun searchProducts(
        @Query("code") barcode: String? = null,
        @Query("page_size") pageSize: Int = 20
    ): OpenProductsResponse
}

// ==================== Response Models ====================

@Serializable
data class OpenPricesResponse(
    val items: List<OpenPriceItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size")
    val pageSize: Int = 50
)

@Serializable
data class OpenPriceItem(
    val id: Int,
    @SerialName("product_code")
    val productCode: String? = null,
    @SerialName("product_name")
    val productName: String? = null,
    val price: Double,
    @SerialName("price_per")
    val pricePer: String? = null, // "KILOGRAM", "UNIT"
    @SerialName("price_without_discount")
    val priceWithoutDiscount: Double? = null,
    val currency: String = "EUR",
    val date: String, // "2025-01-10"
    @SerialName("location_osm_id")
    val locationOsmId: Long? = null,
    @SerialName("location_osm_type")
    val locationOsmType: String? = null,
    @SerialName("location_id")
    val locationId: Int? = null,
    val proof: OpenProof? = null,
    @SerialName("created")
    val created: String? = null
)

@Serializable
data class OpenProof(
    val id: Int,
    val type: String? = null, // "PRICE_TAG", "RECEIPT"
    @SerialName("file_path")
    val filePath: String? = null
)

@Serializable
data class OpenLocationsResponse(
    val items: List<OpenLocation> = emptyList(),
    val total: Int = 0
)

@Serializable
data class OpenLocation(
    val id: Int,
    @SerialName("osm_id")
    val osmId: Long,
    @SerialName("osm_type")
    val osmType: String,
    @SerialName("osm_name")
    val osmName: String? = null,
    @SerialName("osm_address_city")
    val city: String? = null,
    @SerialName("osm_address_country")
    val country: String? = null,
    @SerialName("osm_lat")
    val latitude: Double? = null,
    @SerialName("osm_lon")
    val longitude: Double? = null,
    @SerialName("price_count")
    val priceCount: Int = 0
)

@Serializable
data class OpenProductsResponse(
    val items: List<OpenProduct> = emptyList(),
    val total: Int = 0
)

@Serializable
data class OpenProduct(
    val id: Int,
    val code: String,
    @SerialName("product_name")
    val productName: String? = null,
    val brands: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("price_count")
    val priceCount: Int = 0
)
