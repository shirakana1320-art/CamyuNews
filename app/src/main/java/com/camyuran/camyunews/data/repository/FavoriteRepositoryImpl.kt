package com.camyuran.camyunews.data.repository

import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.data.local.entity.FavoriteEntity
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.FavoriteRepository
import com.camyuran.camyunews.util.toDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val articleDao: ArticleDao
) : FavoriteRepository {

    override fun getAllFavoriteArticles(): Flow<List<Article>> =
        favoriteDao.getAllFavorites().flatMapLatest { favorites ->
            if (favorites.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val ids = favorites.map { it.articleId }
            val savedAtMap = favorites.associate { it.articleId to it.savedAt }
            articleDao.getByIdsFlow(ids).map { articles ->
                articles.map { it.toDomain(isFavorite = true) }
                    .sortedByDescending { savedAtMap[it.id] }
            }
        }

    override fun getFavoriteArticlesByFolder(folderId: Long): Flow<List<Article>> =
        favoriteDao.getFavoritesByFolder(folderId).flatMapLatest { favorites ->
            if (favorites.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val ids = favorites.map { it.articleId }
            articleDao.getByIdsFlow(ids).map { articles ->
                articles.map { it.toDomain(isFavorite = true) }
            }
        }

    override fun getUncategorizedFavorites(): Flow<List<Article>> =
        favoriteDao.getUncategorizedFavorites().flatMapLatest { favorites ->
            if (favorites.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val ids = favorites.map { it.articleId }
            articleDao.getByIdsFlow(ids).map { articles ->
                articles.map { it.toDomain(isFavorite = true) }
            }
        }

    override fun isFavorite(articleId: String): Flow<Boolean> =
        favoriteDao.isFavorite(articleId)

    override suspend fun addFavorite(articleId: String, folderId: Long?) {
        favoriteDao.insert(
            FavoriteEntity(
                articleId = articleId,
                folderId = folderId,
                savedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeFavorite(articleId: String) {
        favoriteDao.deleteByArticleId(articleId)
    }

    override suspend fun moveToFolder(articleId: String, folderId: Long?) {
        favoriteDao.updateFolder(articleId, folderId)
    }

    override fun searchFavorites(keyword: String): Flow<List<Article>> =
        getAllFavoriteArticles().map { articles ->
            articles.filter { article ->
                article.titleJa.contains(keyword, ignoreCase = true) ||
                        article.summaryJa?.contains(keyword, ignoreCase = true) == true
            }
        }
}
