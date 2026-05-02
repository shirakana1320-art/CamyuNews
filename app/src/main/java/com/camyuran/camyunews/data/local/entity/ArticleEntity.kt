package com.camyuran.camyunews.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val titleJa: String,
    val summaryJa: String?,
    /** JSON配列文字列 e.g. ["https://...", "https://..."] */
    val originalUrls: String,
    /** JSON配列文字列 e.g. ["The Hacker News", "Bleeping Computer"] */
    val sourceNames: String,
    val publishedAt: Long,
    /** Asia/Tokyo 基準の日付キー "2026-05-03" */
    val dateKey: String,
    val category: String,
    val subCategory: String,
    val topicalityScore: Int,
    val isRead: Boolean = false,
    val groupId: String?,
    val fetchedAt: Long
)
