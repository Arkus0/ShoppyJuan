package com.arkus.shoppyjuan.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Open Prices API client
 * Documentation: https://prices.openfoodfacts.org/api/docs
 *
 * Supports both reading and contributing prices to the crowdsourced database.
 */
interface OpenPricesApi {

    companion object {
        const val BASE_URL = "https://prices.openfoodfacts.org/"
        const val AUTH_URL = "https://world.openfoodfacts.org/"
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

    // ==================== CONTRIBUTION ENDPOINTS ====================

    /**
     * Authenticate with Open Food Facts to get a session token
     * This uses the Open Food Facts authentication endpoint
     */
    @FormUrlEncoded
    @POST("api/v1/auth")
    suspend fun authenticate(
        @Field("username") username: String,
        @Field("password") password: String
    ): OpenPricesAuthResponse

    /**
     * Submit a new price contribution
     * Requires authentication token in header
     */
    @POST("api/v1/prices")
    suspend fun submitPrice(
        @Header("Authorization") authToken: String,
        @Body priceRequest: OpenPriceSubmission
    ): OpenPriceSubmissionResponse

    /**
     * Upload a proof image (receipt or price tag)
     * Returns a proof_id to associate with price submissions
     */
    @Multipart
    @POST("api/v1/proofs/upload")
    suspend fun uploadProof(
        @Header("Authorization") authToken: String,
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody // "RECEIPT" or "PRICE_TAG"
    ): OpenProofUploadResponse

    /**
     * Submit multiple prices in batch (e.g., from a receipt)
     */
    @POST("api/v1/prices")
    suspend fun submitPriceBatch(
        @Header("Authorization") authToken: String,
        @Body prices: List<OpenPriceSubmission>
    ): List<OpenPriceSubmissionResponse>

    /**
     * Get user's contribution stats
     */
    @GET("api/v1/users/{user_id}")
    suspend fun getUserStats(
        @Path("user_id") userId: String
    ): OpenUserStatsResponse
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

// ==================== Contribution Models ====================

@Serializable
data class OpenPricesAuthResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "bearer",
    @SerialName("user_id")
    val userId: String? = null
)

@Serializable
data class OpenPriceSubmission(
    @SerialName("product_code")
    val productCode: String? = null,
    @SerialName("product_name")
    val productName: String? = null,
    val price: Double,
    @SerialName("price_per")
    val pricePer: String? = null, // "KILOGRAM" or "UNIT"
    @SerialName("price_without_discount")
    val priceWithoutDiscount: Double? = null,
    val currency: String = "EUR",
    val date: String, // Format: "2025-01-10"
    @SerialName("location_osm_id")
    val locationOsmId: Long? = null,
    @SerialName("location_osm_type")
    val locationOsmType: String? = null, // "NODE", "WAY", "RELATION"
    @SerialName("proof_id")
    val proofId: Int? = null,
    val source: String = "shoppyjuan" // Attribution for our app
)

@Serializable
data class OpenPriceSubmissionResponse(
    val id: Int,
    @SerialName("product_code")
    val productCode: String? = null,
    val price: Double,
    @SerialName("created")
    val created: String? = null,
    val status: String? = null
)

@Serializable
data class OpenProofUploadResponse(
    val id: Int,
    @SerialName("file_path")
    val filePath: String,
    val type: String,
    @SerialName("created")
    val created: String? = null
)

@Serializable
data class OpenUserStatsResponse(
    @SerialName("user_id")
    val userId: String,
    @SerialName("price_count")
    val priceCount: Int = 0,
    @SerialName("location_count")
    val locationCount: Int = 0,
    @SerialName("product_count")
    val productCount: Int = 0
)

// ==================== Store Chain Mapping ====================

/**
 * Mapping of Spanish supermarket chains to their OSM location IDs
 * This helps users quickly select a known store chain
 */
object SpanishStoreChains {
    data class StoreChainInfo(
        val name: String,
        val displayName: String,
        val osmBrandWikidata: String? = null
    )

    val chains = mapOf(
        "mercadona" to StoreChainInfo("Mercadona", "Mercadona", "Q377705"),
        "carrefour" to StoreChainInfo("Carrefour", "Carrefour", "Q217599"),
        "dia" to StoreChainInfo("DIA", "DIA", "Q925132"),
        "lidl" to StoreChainInfo("Lidl", "Lidl", "Q151954"),
        "aldi" to StoreChainInfo("ALDI", "ALDI", "Q125054"),
        "alcampo" to StoreChainInfo("Alcampo", "Alcampo", "Q2831416"),
        "eroski" to StoreChainInfo("Eroski", "Eroski", "Q1361349"),
        "consum" to StoreChainInfo("Consum", "Consum", "Q8350308"),
        "bonpreu" to StoreChainInfo("Bonpreu", "Bonpreu", "Q11924692"),
        "ahorramas" to StoreChainInfo("Ahorramas", "Ahorramas", "Q11924692"),
        "hipercor" to StoreChainInfo("Hipercor", "Hipercor / El Corte Inglés", "Q841188"),
        "el_corte_ingles" to StoreChainInfo("El Corte Inglés", "El Corte Inglés Supermercado", "Q841188"),
        "simply" to StoreChainInfo("Simply", "Simply (Auchan)", "Q758603"),
        "coviran" to StoreChainInfo("Covirán", "Covirán", "Q11924692"),
        "mas" to StoreChainInfo("MAS", "Supermercados MAS", "Q11924692")
    )

    fun getChainInfo(chainName: String): StoreChainInfo? {
        val normalized = chainName.lowercase().replace(" ", "_")
        return chains[normalized]
    }

    fun getAllChainNames(): List<String> = chains.values.map { it.displayName }
}
