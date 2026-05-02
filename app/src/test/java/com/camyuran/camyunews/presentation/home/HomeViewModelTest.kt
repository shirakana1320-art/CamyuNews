package com.camyuran.camyunews.presentation.home

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel
    private val articleRepository = mockk<ArticleRepository>()
    private val workManager = mockk<WorkManager>(relaxed = true)

    private fun sampleArticle(id: String, category: String, subCategory: String) = Article(
        id = id,
        titleJa = "テスト記事 $id",
        summaryJa = "要約",
        originalUrls = listOf("https://example.com/$id"),
        sourceNames = listOf("Test"),
        publishedAt = 1000L,
        dateKey = "2026-05-03",
        category = category,
        subCategory = subCategory,
        topicalityScore = 7,
        isRead = false,
        groupId = null,
        fetchedAt = 1000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { articleRepository.getAvailableDates() } returns flowOf(listOf("2026-05-03"))
        every { articleRepository.getArticlesByDateAndCategory(any(), any()) } returns
                flowOf(listOf(sampleArticle("1", "ai", "llm")))
        every { articleRepository.getArticlesByDateCategoryAndSubCategory(any(), any(), any()) } returns
                flowOf(emptyList())
        every { workManager.getWorkInfosByTagFlow(any()) } returns flowOf(emptyList<WorkInfo>())
        viewModel = HomeViewModel(articleRepository, workManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `カテゴリ変更でsecurityに切り替わる`() = runTest {
        every { articleRepository.getArticlesByDateAndCategory(any(), "security") } returns
                flowOf(listOf(sampleArticle("2", "security", "cve")))

        val collectJob = launch { viewModel.articles.collect {} }

        viewModel.selectCategory("security")
        advanceUntilIdle()

        assertEquals("security", viewModel.uiState.value.selectedCategory)
        assertNull(viewModel.uiState.value.selectedSubCategory)
        verify { articleRepository.getArticlesByDateAndCategory(any(), "security") }

        collectJob.cancel()
    }

    @Test
    fun `サブカテゴリ選択で正しいクエリが実行される`() = runTest {
        val collectJob = launch { viewModel.articles.collect {} }

        viewModel.selectSubCategory("llm")
        advanceUntilIdle()

        assertEquals("llm", viewModel.uiState.value.selectedSubCategory)
        verify { articleRepository.getArticlesByDateCategoryAndSubCategory(any(), "ai", "llm") }

        collectJob.cancel()
    }

    @Test
    fun `カテゴリ変更でサブカテゴリがリセットされる`() = runTest {
        viewModel.selectSubCategory("llm")
        viewModel.selectCategory("security")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedSubCategory)
    }

    @Test
    fun `日付選択が正しく更新される`() = runTest {
        every { articleRepository.getArticlesByDateAndCategory("2026-05-01", "ai") } returns
                flowOf(emptyList())

        viewModel.selectDate("2026-05-01")
        advanceUntilIdle()

        assertEquals("2026-05-01", viewModel.uiState.value.selectedDateKey)
    }
}
