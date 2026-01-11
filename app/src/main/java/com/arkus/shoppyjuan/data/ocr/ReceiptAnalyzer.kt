package com.arkus.shoppyjuan.data.ocr

import android.content.Context
import android.net.Uri
import com.arkus.shoppyjuan.data.local.entity.ReceiptItemEntity
import com.arkus.shoppyjuan.domain.util.FuzzySearch
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Result of receipt analysis
 */
data class ReceiptAnalysisResult(
    val storeChain: String?,
    val storeName: String?,
    val items: List<ReceiptItemEntity>,
    val totalAmount: Double?,
    val purchaseDate: String?,
    val rawText: String,
    val confidence: Float
)

/**
 * Analyzes receipt images using ML Kit OCR
 */
@Singleton
class ReceiptAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Known Spanish supermarket chains
    private val knownChains = mapOf(
        "mercadona" to "Mercadona",
        "carrefour" to "Carrefour",
        "dia" to "DIA",
        "lidl" to "Lidl",
        "aldi" to "Aldi",
        "alcampo" to "Alcampo",
        "eroski" to "Eroski",
        "consum" to "Consum",
        "hipercor" to "Hipercor",
        "el corte ingles" to "El Corte Inglés",
        "bonpreu" to "Bonpreu",
        "condis" to "Condis",
        "ahorramas" to "Ahorramas",
        "mas" to "MAS",
        "simply" to "Simply",
        "supersol" to "Supersol"
    )

    /**
     * Analyze a receipt image
     */
    suspend fun analyzeReceipt(imageUri: Uri, receiptId: String): ReceiptAnalysisResult =
        withContext(Dispatchers.IO) {
            val image = InputImage.fromFilePath(context, imageUri)
            val text = recognizeText(image)

            parseReceiptText(text, receiptId)
        }

    /**
     * Recognize text from image using ML Kit
     */
    private suspend fun recognizeText(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            textRecognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    /**
     * Parse recognized text into structured receipt data
     */
    private fun parseReceiptText(text: String, receiptId: String): ReceiptAnalysisResult {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val normalizedText = text.lowercase()

        // Detect store chain
        val storeChain = detectStoreChain(normalizedText)

        // Parse line items
        val items = mutableListOf<ReceiptItemEntity>()
        var totalAmount: Double? = null

        for (line in lines) {
            // Try to parse as a product line
            val item = parseProductLine(line, receiptId)
            if (item != null) {
                items.add(item)
            }

            // Try to detect total
            val total = parseTotalLine(line)
            if (total != null) {
                totalAmount = total
            }
        }

        // Calculate confidence based on how much we could parse
        val confidence = calculateConfidence(items, totalAmount, storeChain)

        return ReceiptAnalysisResult(
            storeChain = storeChain,
            storeName = storeChain,
            items = items,
            totalAmount = totalAmount,
            purchaseDate = null, // TODO: Parse date
            rawText = text,
            confidence = confidence
        )
    }

    /**
     * Detect store chain from receipt text
     */
    private fun detectStoreChain(text: String): String? {
        for ((keyword, chainName) in knownChains) {
            if (text.contains(keyword)) {
                return chainName
            }
        }
        return null
    }

    /**
     * Parse a product line from receipt
     * Common formats:
     * - "LECHE ENTERA 1L    1,25"
     * - "2 x PAN BARRA    0,90"
     * - "TOMATES     1,500kg x 2,99€/kg    4,49"
     */
    private fun parseProductLine(line: String, receiptId: String): ReceiptItemEntity? {
        // Skip lines that are clearly not products
        if (line.length < 5) return null
        if (line.matches(Regex("^[\\d/\\-:.\\s]+$"))) return null // Date/time lines
        if (line.lowercase().contains("total")) return null
        if (line.lowercase().contains("subtotal")) return null
        if (line.lowercase().contains("iva")) return null
        if (line.lowercase().contains("tarjeta")) return null
        if (line.lowercase().contains("efectivo")) return null

        // Try to extract price (usually at the end)
        val pricePattern = Regex("(\\d+[,.]\\d{2})\\s*€?\\s*$")
        val priceMatch = pricePattern.find(line)

        if (priceMatch == null) return null

        val price = priceMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
        val productPart = line.substring(0, priceMatch.range.first).trim()

        if (productPart.isEmpty()) return null

        // Try to extract quantity
        var quantity = 1.0
        var productName = productPart

        // Pattern: "2 x PRODUCT" or "2x PRODUCT"
        val qtyPattern = Regex("^(\\d+)\\s*[xX]\\s*(.+)")
        val qtyMatch = qtyPattern.find(productPart)
        if (qtyMatch != null) {
            quantity = qtyMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            productName = qtyMatch.groupValues[2].trim()
        }

        // Pattern: "PRODUCT 1,500kg"
        val weightPattern = Regex("(.+)\\s+(\\d+[,.]\\d+)\\s*(kg|g|l|ml)", RegexOption.IGNORE_CASE)
        val weightMatch = weightPattern.find(productPart)
        if (weightMatch != null) {
            productName = weightMatch.groupValues[1].trim()
            quantity = weightMatch.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 1.0
        }

        // Clean up product name
        productName = cleanProductName(productName)

        if (productName.length < 2) return null

        return ReceiptItemEntity(
            id = UUID.randomUUID().toString(),
            receiptId = receiptId,
            rawText = line,
            productName = productName,
            normalizedName = FuzzySearch.normalize(productName),
            quantity = quantity,
            unitPrice = if (quantity > 0) price / quantity else price,
            totalPrice = price,
            confidence = 0.7f
        )
    }

    /**
     * Parse total line
     */
    private fun parseTotalLine(line: String): Double? {
        val lowerLine = line.lowercase()
        if (!lowerLine.contains("total") && !lowerLine.contains("importe")) return null

        val pricePattern = Regex("(\\d+[,.]\\d{2})")
        val match = pricePattern.findAll(line).lastOrNull()

        return match?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
    }

    /**
     * Clean up product name
     */
    private fun cleanProductName(name: String): String {
        return name
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^[\\d.,€\\s]+"), "") // Remove leading numbers/prices
            .replace(Regex("[€$]"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Calculate confidence score for parsing
     */
    private fun calculateConfidence(
        items: List<ReceiptItemEntity>,
        total: Double?,
        storeChain: String?
    ): Float {
        var confidence = 0.3f // Base confidence

        // More items = more confidence
        if (items.size >= 3) confidence += 0.2f
        if (items.size >= 10) confidence += 0.1f

        // Store detected = more confidence
        if (storeChain != null) confidence += 0.1f

        // Total found = more confidence
        if (total != null) confidence += 0.1f

        // Prices sum close to total = more confidence
        if (total != null && items.isNotEmpty()) {
            val itemsTotal = items.sumOf { it.totalPrice }
            val diff = kotlin.math.abs(itemsTotal - total)
            if (diff < 1.0) confidence += 0.2f
            else if (diff < 5.0) confidence += 0.1f
        }

        return minOf(1.0f, confidence)
    }
}
