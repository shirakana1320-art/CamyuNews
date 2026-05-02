package com.camyuran.camyunews.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.util.todayDateKey
import com.camyuran.camyunews.worker.NewsFetchWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val selectedDateKey: String = todayDateKey(),
    val selectedCategory: String = "ai",
    val selectedSubCategory: String? = null,
    val keywordFilter: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val availableDates: StateFlow<List<String>> = articleRepository.getAvailableDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = workManager
        .getWorkInfosByTagFlow(MANUAL_FETCH_TAG)
        .map { infos ->
            infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: StateFlow<List<Article>> = _uiState
        .flatMapLatest { state ->
            val baseFlow = when {
                state.selectedSubCategory != null ->
                    articleRepository.getArticlesByDateCategoryAndSubCategory(
                        state.selectedDateKey, state.selectedCategory, state.selectedSubCategory
                    )
                else ->
                    articleRepository.getArticlesByDateAndCategory(
                        state.selectedDateKey, state.selectedCategory
                    )
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

    fun triggerRefresh() {
        val request = OneTimeWorkRequestBuilder<NewsFetchWorker>()
            .addTag(MANUAL_FETCH_TAG)
            .build()
        workManager.enqueueUniqueWork(MANUAL_FETCH_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        private const val MANUAL_FETCH_TAG = "news_fetch_manual"
        private const val MANUAL_FETCH_WORK_NAME = "NewsFetchWorker_manual"
    }
}
