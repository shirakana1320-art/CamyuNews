package com.camyuran.camyunews.domain.repository

import com.camyuran.camyunews.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun getArticlesByDate(dateKey: String): Flow<List<Article>>
    fun getArticlesByDateAndCategory(dateKey: String, category: String): Flow<List<Article>>
    fun getArticlesByDateCategoryAndSubCategory(
        dateKey: String, category: String, subCategory: String
    ): Flow<List<Article>>
    fun searchArticles(keyword: String): Flow<List<Article>>
    fun getAvailableDates(): Flow<List<String>>
    fun getUnreadCountByDate(dateKey: String): Flow<Int>
    suspend fun getById(id: String): Article?
    suspend fun markAsRead(id: String)
    suspend fun pruneOldArticles(cutoffMs: Long): Int
    fun getRecentArticlesBySubCategory(
        category: String, subCategory: String, fromDateKey: String, excludeId: String, limit: Int = 5
    ): Flow<List<Article>>
    fun getAllSummarizedArticles(): Flow<List<Article>>
}
