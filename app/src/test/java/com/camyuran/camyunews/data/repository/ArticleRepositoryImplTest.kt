package com.camyuran.camyunews.data.repository

import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.data.local.entity.ArticleEntity
import com.camyuran.camyunews.data.local.entity.FavoriteEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArticleRepositoryImplTest {

    private lateinit var repository: ArticleRepositoryImpl
    private val articleDao = mockk<ArticleDao>()
    private val favoriteDao = mockk<FavoriteDao>()

    private fun sampleEntity(id: String = "abc", dateKey: String = "2026-05-03") = ArticleEntity(
        id = id,
        titleJa = "テスト記事",
        summaryJa = "要約",
        originalUrls = """["https://example.com"]""",
        sourceNames = """["Test Source"]""",
        publishedAt = 1000L,
        dateKey = dateKey,
        category = "ai",
        subCategory = "llm",
        topicalityScore = 7,
        isRead = false,
        groupId = null,
        fetchedAt = 1000L
    )

    @Before
    fun setUp() {
        every { favoriteDao.getAllFavorites() } returns flowOf(emptyList())
        repository = ArticleRepositoryImpl(articleDao, favoriteDao)
    }

    @Test
    fun `日付キーでDBから記事を取得できる`() = runTest {
        every { articleDao.getArticlesByDate("2026-05-03") } returns
                flowOf(listOf(sampleEntity(dateKey = "2026-05-03")))

        val articles = repository.getArticlesByDate("2026-05-03").first()

        assertEquals(1, articles.size)
        assertEquals("テスト記事", articles[0].titleJa)
        verify { articleDao.getArticlesByDate("2026-05-03") }
    }

    @Test
    fun `お気に入りフラグが正しく設定される`() = runTest {
        val entity = sampleEntity("article1")
        val favorite = FavoriteEntity(id = 1, articleId = "article1", folderId = null, savedAt = 100L)

        every { articleDao.getArticlesByDate("2026-05-03") } returns flowOf(listOf(entity))
        every { favoriteDao.getAllFavorites() } returns flowOf(listOf(favorite))

        val repo = ArticleRepositoryImpl(articleDao, favoriteDao)
        val articles = repo.getArticlesByDate("2026-05-03").first()

        assertTrue("お気に入り記事のisFavoriteがtrueであること", articles[0].isFavorite)
    }

    @Test
    fun `31日以上経過した記事を削除できる`() = runTest {
        coEvery { articleDao.pruneOldArticles(any()) } returns 5

        val pruned = repository.pruneOldArticles(1000L)

        assertEquals(5, pruned)
        coVerify { articleDao.pruneOldArticles(1000L) }
    }

    @Test
    fun `IDで記事を取得し既読にできる`() = runTest {
        val entity = sampleEntity("abc")
        coEvery { articleDao.getById("abc") } returns entity
        coEvery { articleDao.update(any()) } just Runs

        repository.markAsRead("abc")

        coVerify { articleDao.update(match { it.isRead }) }
    }
}
