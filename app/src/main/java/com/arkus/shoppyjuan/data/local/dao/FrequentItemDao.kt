package com.arkus.shoppyjuan.data.local.dao

import androidx.room.*
import com.arkus.shoppyjuan.data.local.entity.FrequentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FrequentItemDao {

    @Query("SELECT * FROM frequent_items ORDER BY count DESC, lastUsed DESC LIMIT :limit")
    fun getTopFrequentItems(limit: Int = 10): Flow<List<FrequentItemEntity>>

    @Query("SELECT * FROM frequent_items WHERE normalizedName LIKE '%' || :query || '%' ORDER BY count DESC LIMIT :limit")
    fun searchFrequentItems(query: String, limit: Int = 5): Flow<List<FrequentItemEntity>>

    @Query("SELECT * FROM frequent_items WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): FrequentItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FrequentItemEntity)

    @Query("UPDATE frequent_items SET count = count + 1, lastUsed = :timestamp WHERE normalizedName = :normalizedName")
    suspend fun incrementCount(normalizedName: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM frequent_items WHERE normalizedName = :normalizedName")
    suspend fun delete(normalizedName: String)

    @Query("DELETE FROM frequent_items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM frequent_items")
    suspend fun getCount(): Int
}
