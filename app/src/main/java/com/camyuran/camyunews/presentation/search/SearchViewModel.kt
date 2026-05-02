package com.camyuran.camyunews.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SearchTarget { ALL, FAVORITES }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    val query = MutableStateFlow("")
    val searchTarget = MutableStateFlow(SearchTarget.ALL)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Article>> = combine(
        query.debounce(300).distinctUntilChanged(),
        searchTarget
    ) { q, target -> q to target }
        .flatMapLatest { (q, target) ->
            if (q.isBlank()) flowOf(emptyList())
            else when (target) {
                SearchTarget.ALL -> articleRepository.searchArticles(q)
                SearchTarget.FAVORITES -> favoriteRepository.searchFavorites(q)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { query.value = q }
    fun setTarget(target: SearchTarget) { searchTarget.value = target }
}
