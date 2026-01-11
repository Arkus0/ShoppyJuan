package com.arkus.shoppyjuan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frequent_items")
data class FrequentItemEntity(
    @PrimaryKey
    val name: String,
    val normalizedName: String,
    val count: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val category: String? = null,
    val emoji: String? = null,
    val defaultUnit: String? = null,
    val userId: String? = null
)
