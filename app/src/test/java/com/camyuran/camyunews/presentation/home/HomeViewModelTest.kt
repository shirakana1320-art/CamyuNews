package com.camyuran.camyunews.presentation.home

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.camyuran.camyunews.data.remote.gemini.ApiKeyProvider
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
    private val apiKeyProvider = mockk<ApiKeyProvider>()

    private fun sampleArticle(id: String, category: String, subCategory: String) = Article(
        id = id,
        titleJa = "テスト記事 $id",
        summaryJa = "要約",
        originalUrls = listOf("https://example.com/$id"),
        sourceNames = listOf("Test"),
        originalTitles = listOf("Test Article $id"),
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
        // 初期 selectedCategory が "all" のため getAllSummarizedArticles() が呼ばれる
        every { articleRepository.getAllSummarizedArticles() } returns
                flowOf(listOf(sampleArticle("1", "ai", "llm")))
        every { workManager.getWorkInfosByTagFlow(any()) } returns flowOf(emptyList<WorkInfo>())
        every { apiKeyProvider.hasApiKey() } returns true
        viewModel = HomeViewModel(articleRepository, workManager, apiKeyProvider)
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

        // selectedCategory が "all" の場合 getAllSummarizedArticles() が呼ばれるため、
        // 先にカテゴリを "ai" に変更してからサブカテゴリを選択する
        viewModel.selectCategory("ai")
        advanceUntilIdle()
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
        // availableDates に "2026-05-01" を含めることで、
        // init ブロックの自動補正（dates に含まれない場合は最新日に戻す）が発動しないようにする
        every { articleRepository.getAvailableDates() } returns
                flowOf(listOf("2026-05-03", "2026-05-01"))
        every { articleRepository.getArticlesByDateAndCategory("2026-05-01", "ai") } returns
                flowOf(emptyList())

        // 自動補正ロジックが availableDates を購読するため、先に collect を開始してから selectDate を呼ぶ
        val collectJob = launch { viewModel.availableDates.collect {} }
        advanceUntilIdle()

        viewModel.selectDate("2026-05-01")
        advanceUntilIdle()

        assertEquals("2026-05-01", viewModel.uiState.value.selectedDateKey)
        collectJob.cancel()
    }

    @Test
    fun `clearFetchError でfetchErrorがnullになる`() = runTest {
        viewModel.setFetchErrorForTest("テストエラー")
        assertEquals("テストエラー", viewModel.uiState.value.fetchError)

        viewModel.clearFetchError()
        assertNull(viewModel.uiState.value.fetchError)
    }

    @Test
    fun `availableDatesに選択日が含まれない場合は最新日付に自動補正される`() = runTest {
        every { articleRepository.getAvailableDates() } returns flowOf(listOf("2026-05-01", "2026-04-30"))
        every { articleRepository.getArticlesByDateAndCategory(any(), any()) } returns flowOf(emptyList())

        val testViewModel = HomeViewModel(articleRepository, workManager, apiKeyProvider)
        val collectJob = launch { testViewModel.availableDates.collect {} }
        advanceUntilIdle()

        assertEquals("2026-05-01", testViewModel.uiState.value.selectedDateKey)
        collectJob.cancel()
    }

    @Test
    fun `availableDatesに選択日が含まれる場合は最新日付に変更されない`() = runTest {
        // 2日分の日付がある場合に、古い方を選択していても最新日に勝手に変わらないことを確認
        every { articleRepository.getAvailableDates() } returns flowOf(listOf("2026-05-03", "2026-05-02"))
        every { articleRepository.getArticlesByDateAndCategory(any(), any()) } returns flowOf(emptyList())

        val testViewModel = HomeViewModel(articleRepository, workManager, apiKeyProvider)
        testViewModel.selectDate("2026-05-02")  // 最新ではない日を明示的に選択
        val collectJob = launch { testViewModel.availableDates.collect {} }
        advanceUntilIdle()

        // "2026-05-02" は availableDates に含まれるので "2026-05-03" に変更されない
        assertEquals("2026-05-02", testViewModel.uiState.value.selectedDateKey)
        collectJob.cancel()
    }
}
