package com.camyuran.camyunews.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.model.SubCategory
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.util.todayDateKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val articles: List<Article> = emptyList(),
    val selectedDateKey: String = todayDateKey(),
    val selectedCategory: String = "ai",
    val selectedSubCategory: String? = null,
    val keywordFilter: String = "",
    val availableDates: List<String> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val selectedDateKey = MutableStateFlow(todayDateKey())
    private val selectedCategory = MutableStateFlow("ai")
    private val selectedSubCategory = MutableStateFlow<String?>(null)
    private val keywordFilter = MutableStateFlow("")

    val availableDates: StateFlow<List<String>> = articleRepository.getAvailableDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: StateFlow<List<Article>> = combine(
        selectedDateKey, selectedCategory, selectedSubCategory, keywordFilter
    ) { date, category, subCat, keyword -> Triple(date, category to subCat, keyword) }
        .flatMapLatest { (date, catPair, keyword) ->
            val (category, subCat) = catPair
            val baseFlow = when {
                subCat != null -> articleRepository.getArticlesByDateCategoryAndSubCategory(date, category, subCat)
                else -> articleRepository.getArticlesByDateAndCategory(date, category)
            }
            if (keyword.isBlank()) baseFlow
            else baseFlow.map { articles ->
                articles.filter {
                    it.titleJa.contains(keyword, ignoreCase = true) ||
                            it.summaryJa?.contains(keyword, ignoreCase = true) == true
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(dateKey: String) { selectedDateKey.value = dateKey }
    fun selectCategory(category: String) {
        selectedCategory.value = category
        selectedSubCategory.value = null
    }
    fun selectSubCategory(subCategory: String?) { selectedSubCategory.value = subCategory }
    fun setKeywordFilter(keyword: String) { keywordFilter.value = keyword }

    fun getCurrentState() = HomeUiState(
        selectedDateKey = selectedDateKey.value,
        selectedCategory = selectedCategory.value,
        selectedSubCategory = selectedSubCategory.value,
        keywordFilter = keywordFilter.value,
        availableDates = availableDates.value,
        articles = articles.value
    )
}
