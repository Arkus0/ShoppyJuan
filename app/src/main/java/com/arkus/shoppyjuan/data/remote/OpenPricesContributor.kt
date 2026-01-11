package com.arkus.shoppyjuan.data.remote

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arkus.shoppyjuan.data.local.entity.PriceRecordEntity
import com.arkus.shoppyjuan.data.local.entity.ReceiptEntity
import com.arkus.shoppyjuan.data.local.entity.ReceiptItemEntity
import com.arkus.shoppyjuan.data.remote.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.openPricesDataStore: DataStore<Preferences> by preferencesDataStore(name = "open_prices_auth")

/**
 * Manages authentication and contribution to Open Prices API
 *
 * Users can optionally link their Open Food Facts account to contribute
 * prices extracted from receipts to the global crowdsourced database.
 */
@Singleton
class OpenPricesContributor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openPricesApi: OpenPricesApi
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
    }

    private val dataStore = context.openPricesDataStore

    // ==================== AUTH STATE ====================

    /**
     * Check if user is authenticated with Open Prices
     */
    val isAuthenticated: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACCESS_TOKEN] != null
    }

    /**
     * Get current Open Prices user ID
     */
    val openPricesUserId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    /**
     * Get stored username
     */
    val username: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_USERNAME]
    }

    // ==================== AUTHENTICATION ====================

    /**
     * Authenticate with Open Food Facts credentials
     * These are the same credentials used for openfoodfacts.org
     */
    suspend fun authenticate(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = openPricesApi.authenticate(username, password)

            // Save credentials
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = response.accessToken
                prefs[KEY_USER_ID] = response.userId ?: username
                prefs[KEY_USERNAME] = username
            }

            Result.success(response.accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout and clear stored credentials
     */
    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USERNAME)
        }
    }

    /**
     * Get current auth token
     */
    private suspend fun getAuthToken(): String? {
        return dataStore.data.first()[KEY_ACCESS_TOKEN]
    }

    // ==================== PRICE CONTRIBUTION ====================

    /**
     * Submit a single price to Open Prices
     */
    suspend fun submitPrice(
        price: PriceRecordEntity,
        locationOsmId: Long? = null,
        locationOsmType: String? = null
    ): Result<OpenPriceSubmissionResponse> = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext Result.failure(
            IllegalStateException("No autenticado con Open Prices")
        )

        try {
            val submission = OpenPriceSubmission(
                productCode = price.barcode,
                productName = if (price.barcode == null) price.productName else null,
                price = price.price,
                pricePer = price.unit?.uppercase(),
                currency = price.currency,
                date = formatDate(price.createdAt),
                locationOsmId = locationOsmId,
                locationOsmType = locationOsmType
            )

            val response = openPricesApi.submitPrice("Bearer $token", submission)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submit multiple prices from a receipt
     */
    suspend fun submitReceiptPrices(
        items: List<ReceiptItemEntity>,
        receipt: ReceiptEntity,
        storeOsmId: Long? = null,
        storeOsmType: String? = "NODE",
        proofId: Int? = null
    ): Result<ContributionSummary> = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext Result.failure(
            IllegalStateException("No autenticado con Open Prices")
        )

        val dateStr = formatDate(receipt.createdAt)
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        items.forEach { item ->
            try {
                val submission = OpenPriceSubmission(
                    productCode = item.barcode,
                    productName = if (item.barcode == null) item.productName else null,
                    price = item.unitPrice ?: (item.totalPrice / item.quantity),
                    pricePer = "UNIT",
                    currency = "EUR",
                    date = dateStr,
                    locationOsmId = storeOsmId,
                    locationOsmType = storeOsmType,
                    proofId = proofId
                )

                openPricesApi.submitPrice("Bearer $token", submission)
                successCount++
            } catch (e: Exception) {
                failCount++
                errors.add("${item.productName}: ${e.message}")
            }
        }

        Result.success(ContributionSummary(
            totalItems = items.size,
            successCount = successCount,
            failCount = failCount,
            errors = errors
        ))
    }

    // ==================== PROOF UPLOAD ====================

    /**
     * Upload a receipt image as proof
     */
    suspend fun uploadReceiptProof(imageUri: Uri): Result<OpenProofUploadResponse> = withContext(Dispatchers.IO) {
        val token = getAuthToken() ?: return@withContext Result.failure(
            IllegalStateException("No autenticado con Open Prices")
        )

        try {
            val file = uriToFile(imageUri) ?: return@withContext Result.failure(
                IllegalStateException("No se pudo procesar la imagen")
            )

            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val type = "RECEIPT".toRequestBody("text/plain".toMediaType())

            val response = openPricesApi.uploadProof("Bearer $token", part, type)

            // Clean up temp file
            file.delete()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timestamp))
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "receipt_proof_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get user's contribution statistics from Open Prices
     */
    suspend fun getUserStats(): Result<OpenUserStatsResponse> = withContext(Dispatchers.IO) {
        val userId = dataStore.data.first()[KEY_USER_ID] ?: return@withContext Result.failure(
            IllegalStateException("No autenticado")
        )

        try {
            val stats = openPricesApi.getUserStats(userId)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Summary of a batch contribution
 */
data class ContributionSummary(
    val totalItems: Int,
    val successCount: Int,
    val failCount: Int,
    val errors: List<String> = emptyList()
) {
    val allSucceeded: Boolean get() = failCount == 0
}
