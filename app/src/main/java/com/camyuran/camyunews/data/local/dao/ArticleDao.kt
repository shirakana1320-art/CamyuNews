package com.camyuran.camyunews.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.camyuran.camyunews.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Update
    suspend fun update(article: ArticleEntity)

    @Query("SELECT * FROM articles WHERE dateKey = :dateKey ORDER BY topicalityScore DESC, fetchedAt DESC")
    fun getArticlesByDate(dateKey: String): Flow<List<ArticleEntity>>

    @Query("""
        SELECT * FROM articles
        WHERE dateKey = :dateKey AND category = :category
        ORDER BY topicalityScore DESC, fetchedAt DESC
    """)
    fun getArticlesByDateAndCategory(dateKey: String, category: String): Flow<List<ArticleEntity>>

    @Query("""
        SELECT * FROM articles
        WHERE dateKey = :dateKey AND category = :category AND subCategory = :subCategory
        ORDER BY topicalityScore DESC, fetchedAt DESC
    """)
    fun getArticlesByDateCategoryAndSubCategory(
        dateKey: String, category: String, subCategory: String
    ): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): ArticleEntity?

    @Query("""
        SELECT * FROM articles
        WHERE titleJa LIKE '%' || :keyword || '%' OR summaryJa LIKE '%' || :keyword || '%'
        ORDER BY topicalityScore DESC, fetchedAt DESC
        LIMIT 100
    """)
    fun searchArticles(keyword: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE summaryJa IS NULL ORDER BY fetchedAt DESC LIMIT 50")
    suspend fun getUnprocessedArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id IN (:ids)")
    fun getByIdsFlow(ids: List<String>): Flow<List<ArticleEntity>>

    @Query("SELECT DISTINCT dateKey FROM articles ORDER BY dateKey DESC LIMIT 30")
    fun getAvailableDates(): Flow<List<String>>

    /** 31日以上経過かつお気に入りでない記事を削除 */
    @Query("""
        DELETE FROM articles
        WHERE fetchedAt < :cutoffMs
        AND id NOT IN (SELECT articleId FROM favorites)
    """)
    suspend fun pruneOldArticles(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM articles WHERE dateKey = :dateKey AND isRead = 0")
    fun getUnreadCountByDate(dateKey: String): Flow<Int>

    /** URLのハッシュ一覧を返す（重複チェック用） */
    @Query("SELECT id FROM articles WHERE fetchedAt >= :since")
    suspend fun getRecentArticleIds(since: Long): List<String>

    /** pubDate なし記事（dateKey=1970-01-01）のうち今日フェッチされたものを正しい日付キーに修正 */
    @Query("UPDATE articles SET dateKey = :newDateKey WHERE dateKey = '1970-01-01' AND fetchedAt >= :since")
    suspend fun fixInvalidDateKeys(newDateKey: String, since: Long): Int

    /** 同じサブジャンルの直近 N 日以内の記事（自分自身を除く） */
    @Query("""
        SELECT * FROM articles
        WHERE category = :category AND subCategory = :subCategory
        AND dateKey >= :fromDateKey
        AND id != :excludeId
        ORDER BY topicalityScore DESC, fetchedAt DESC
        LIMIT :limit
    """)
    fun getRecentArticlesBySubCategory(
        category: String, subCategory: String, fromDateKey: String, excludeId: String, limit: Int
    ): Flow<List<ArticleEntity>>
}
