package com.camyuran.camyunews.presentation.home

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.camyuran.camyunews.data.remote.gemini.ApiKeyProvider
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.util.todayDateKey
import com.camyuran.camyunews.worker.NewsFetchWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val selectedDateKey: String = todayDateKey(),
    val selectedCategory: String = "all",
    val selectedSubCategory: String? = null,
    val keywordFilter: String = "",
    val fetchError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val workManager: WorkManager,
    private val apiKeyProvider: ApiKeyProvider
) : ViewModel() {

    private val viewModelCreatedAt = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _hasGeminiKey = MutableStateFlow(apiKeyProvider.hasApiKey())
    val hasGeminiKey: StateFlow<Boolean> = _hasGeminiKey.asStateFlow()

    val availableDates: StateFlow<List<String>> = articleRepository.getAvailableDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 単一購読で isLoading とエラー観測の両方をまかなう
    private val _workInfos: StateFlow<List<WorkInfo>> = workManager
        .getWorkInfosByTagFlow(NewsFetchWorker.TAG_FETCH)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isLoading: StateFlow<Boolean> = _workInfos
        .map { infos ->
            infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fetchProgress: StateFlow<String?> = _workInfos
        .map { infos ->
            infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                ?.progress?.getString(NewsFetchWorker.KEY_PROGRESS_MESSAGE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            availableDates.collect { dates ->
                if (dates.isNotEmpty() && !dates.contains(_uiState.value.selectedDateKey)) {
                    _uiState.update { it.copy(selectedDateKey = dates.first()) }
                }
            }
        }
        viewModelScope.launch {
            val reportedIds = mutableSetOf<UUID>()
            _workInfos.collect { infos ->
                infos
                    .filter { info ->
                        info.state == WorkInfo.State.SUCCEEDED
                            && info.id !in reportedIds
                            && info.outputData.getLong(NewsFetchWorker.KEY_COMPLETED_AT, 0L) >= viewModelCreatedAt
                    }
                    .forEach { info ->
                        reportedIds.add(info.id)
                        buildFetchErrorMessage(
                            info.outputData.getString(NewsFetchWorker.KEY_ERROR_TYPE),
                            info.outputData.getInt(NewsFetchWorker.KEY_PROCESSED_COUNT, 0)
                        )?.let { msg -> _uiState.update { it.copy(fetchError = msg) } }
                    }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: StateFlow<List<Article>> = _uiState
        .flatMapLatest { state ->
            val baseFlow = if (state.selectedCategory == "all") {
                articleRepository.getAllSummarizedArticles()
            } else {
                val flow = when {
                    state.selectedSubCategory != null ->
                        articleRepository.getArticlesByDateCategoryAndSubCategory(
                            state.selectedDateKey, state.selectedCategory, state.selectedSubCategory
                        )
                    else ->
                        articleRepository.getArticlesByDateAndCategory(
                            state.selectedDateKey, state.selectedCategory
                        )
                }
                flow.map { articles -> articles.filter { it.summaryJa != null } }
            }
            if (state.keywordFilter.isBlank()) baseFlow
            else baseFlow.map { articles ->
                articles.filter {
                    it.titleJa.contains(state.keywordFilter, ignoreCase = true) ||
                            it.summaryJa?.contains(state.keywordFilter, ignoreCase = true) == true
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(dateKey: String) = _uiState.update { it.copy(selectedDateKey = dateKey) }
    fun selectCategory(category: String) =
        _uiState.update { it.copy(selectedCategory = category, selectedSubCategory = null) }
    fun selectSubCategory(subCategory: String?) =
        _uiState.update { it.copy(selectedSubCategory = subCategory) }
    fun setKeywordFilter(keyword: String) = _uiState.update { it.copy(keywordFilter = keyword) }

    fun refreshApiKeyStatus() {
        _hasGeminiKey.value = apiKeyProvider.hasApiKey()
    }

    fun clearFetchError() = _uiState.update { it.copy(fetchError = null) }

    fun triggerRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<NewsFetchWorker>()
            .addTag(NewsFetchWorker.TAG_FETCH)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(NewsFetchWorker.WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)
    }

    @VisibleForTesting
    internal fun setFetchErrorForTest(msg: String) = _uiState.update { it.copy(fetchError = msg) }

    private fun buildFetchErrorMessage(errorType: String?, processedCount: Int): String? =
        when (errorType) {
            "api_key_missing" -> "Gemini APIキーが無効です。設定画面で正しいキーを入力してください"
            "rate_limit" -> if (processedCount == 0)
                "APIのレートリミットに達しました。しばらく経ってから更新してください"
            else
                "一部でレートリミットが発生しました（処理済み: ${processedCount}件）"
            "unknown" -> if (processedCount == 0) "記事の要約に失敗しました。接続状態を確認してください" else null
            else -> null
        }
}
