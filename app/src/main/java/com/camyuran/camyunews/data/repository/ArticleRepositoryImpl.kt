package com.camyuran.camyunews.data.repository

import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.util.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val favoriteDao: FavoriteDao
) : ArticleRepository {

    override fun getArticlesByDate(dateKey: String): Flow<List<Article>> =
        articleDao.getArticlesByDate(dateKey).withFavoriteStatus()

    override fun getArticlesByDateAndCategory(
        dateKey: String, category: String
    ): Flow<List<Article>> =
        articleDao.getArticlesByDateAndCategory(dateKey, category).withFavoriteStatus()

    override fun getArticlesByDateCategoryAndSubCategory(
        dateKey: String, category: String, subCategory: String
    ): Flow<List<Article>> =
        articleDao.getArticlesByDateCategoryAndSubCategory(dateKey, category, subCategory)
            .withFavoriteStatus()

    override fun searchArticles(keyword: String): Flow<List<Article>> =
        articleDao.searchArticles(keyword).withFavoriteStatus()

    override fun getAvailableDates(): Flow<List<String>> = articleDao.getAvailableDates()

    override fun getUnreadCountByDate(dateKey: String): Flow<Int> =
        articleDao.getUnreadCountByDate(dateKey)

    override suspend fun getById(id: String): Article? =
        articleDao.getById(id)?.toDomain()

    override suspend fun markAsRead(id: String) {
        val entity = articleDao.getById(id) ?: return
        articleDao.update(entity.copy(isRead = true))
    }

    override suspend fun pruneOldArticles(cutoffMs: Long): Int =
        articleDao.pruneOldArticles(cutoffMs)

    private fun Flow<List<com.camyuran.camyunews.data.local.entity.ArticleEntity>>.withFavoriteStatus(): Flow<List<Article>> =
        combine(favoriteDao.getAllFavorites()) { articles, favorites ->
            val favoriteIds = favorites.map { it.articleId }.toSet()
            articles.map { it.toDomain(isFavorite = it.id in favoriteIds) }
        }
}
