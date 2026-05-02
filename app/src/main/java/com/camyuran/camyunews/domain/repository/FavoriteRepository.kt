package com.camyuran.camyunews.domain.repository

import com.camyuran.camyunews.domain.model.Article
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getAllFavoriteArticles(): Flow<List<Article>>
    fun getFavoriteArticlesByFolder(folderId: Long): Flow<List<Article>>
    fun getUncategorizedFavorites(): Flow<List<Article>>
    fun isFavorite(articleId: String): Flow<Boolean>
    suspend fun addFavorite(articleId: String, folderId: Long?)
    suspend fun removeFavorite(articleId: String)
    suspend fun moveToFolder(articleId: String, folderId: Long?)
    fun searchFavorites(keyword: String): Flow<List<Article>>
}
