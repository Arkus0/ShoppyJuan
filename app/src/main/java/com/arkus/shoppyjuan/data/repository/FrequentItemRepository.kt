package com.arkus.shoppyjuan.data.repository

import com.arkus.shoppyjuan.data.local.dao.FrequentItemDao
import com.arkus.shoppyjuan.data.local.entity.FrequentItemEntity
import com.arkus.shoppyjuan.domain.util.ProductCategory
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrequentItemRepository @Inject constructor(
    private val frequentItemDao: FrequentItemDao
) {
    /**
     * Get top frequently used items
     */
    fun getTopFrequentItems(limit: Int = 10): Flow<List<FrequentItemEntity>> {
        return frequentItemDao.getTopFrequentItems(limit)
    }

    /**
     * Search frequent items by query
     */
    fun searchFrequentItems(query: String, limit: Int = 5): Flow<List<FrequentItemEntity>> {
        val normalizedQuery = normalizeText(query)
        return frequentItemDao.searchFrequentItems(normalizedQuery, limit)
    }

    /**
     * Track item usage - call when an item is added to a list
     */
    suspend fun trackItemUsage(
        name: String,
        category: String? = null,
        emoji: String? = null,
        unit: String? = null
    ) {
        val normalizedName = normalizeText(name)
        val existingItem = frequentItemDao.getByNormalizedName(normalizedName)

        if (existingItem != null) {
            // Increment count for existing item
            frequentItemDao.incrementCount(normalizedName)
        } else {
            // Create new frequent item
            val detectedCategory = ProductCategory.detectCategory(name)
            val newItem = FrequentItemEntity(
                name = name.trim(),
                normalizedName = normalizedName,
                count = 1,
                lastUsed = System.currentTimeMillis(),
                category = category ?: detectedCategory.label,
                emoji = emoji ?: detectedCategory.emoji,
                defaultUnit = unit
            )
            frequentItemDao.insert(newItem)
        }
    }

    /**
     * Delete a frequent item
     */
    suspend fun deleteFrequentItem(name: String) {
        val normalizedName = normalizeText(name)
        frequentItemDao.delete(normalizedName)
    }

    /**
     * Clear all frequent items
     */
    suspend fun clearAll() {
        frequentItemDao.deleteAll()
    }

    /**
     * Normalize text for comparison
     * Removes accents, converts to lowercase, trims
     */
    private fun normalizeText(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .lowercase()
            .trim()
    }
}
