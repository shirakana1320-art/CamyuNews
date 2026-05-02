package com.camyuran.camyunews.data.repository

import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.data.local.entity.FavoriteEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FavoriteRepositoryImplTest {

    private lateinit var repository: FavoriteRepositoryImpl
    private val favoriteDao = mockk<FavoriteDao>()
    private val articleDao = mockk<ArticleDao>()

    @Before
    fun setUp() {
        repository = FavoriteRepositoryImpl(favoriteDao, articleDao)
    }

    @Test
    fun `お気に入りに追加できる`() = runTest {
        coEvery { favoriteDao.insert(any()) } returns 1L

        repository.addFavorite("article1", folderId = null)

        coVerify {
            favoriteDao.insert(match {
                it.articleId == "article1" && it.folderId == null
            })
        }
    }

    @Test
    fun `お気に入りをフォルダ付きで追加できる`() = runTest {
        coEvery { favoriteDao.insert(any()) } returns 2L

        repository.addFavorite("article2", folderId = 5L)

        coVerify {
            favoriteDao.insert(match {
                it.articleId == "article2" && it.folderId == 5L
            })
        }
    }

    @Test
    fun `お気に入りを解除できる`() = runTest {
        coEvery { favoriteDao.deleteByArticleId("article1") } just Runs

        repository.removeFavorite("article1")

        coVerify { favoriteDao.deleteByArticleId("article1") }
    }

    @Test
    fun `フォルダを移動できる`() = runTest {
        coEvery { favoriteDao.updateFolder("article1", 3L) } just Runs

        repository.moveToFolder("article1", 3L)

        coVerify { favoriteDao.updateFolder("article1", 3L) }
    }

    @Test
    fun `お気に入り判定Flowが正しく動作する`() = runTest {
        every { favoriteDao.isFavorite("article1") } returns flowOf(true)
        every { favoriteDao.isFavorite("article2") } returns flowOf(false)

        assertTrue(repository.isFavorite("article1").first())
        assertFalse(repository.isFavorite("article2").first())
    }
}
