package com.camyuran.camyunews.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SearchTarget { ALL, FAVORITES }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchTarget = MutableStateFlow(SearchTarget.ALL)
    val searchTarget: StateFlow<SearchTarget> = _searchTarget.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Article>> = combine(
        _query.debounce(300).distinctUntilChanged(),
        _searchTarget
    ) { q, target -> q to target }
        .flatMapLatest { (q, target) ->
            if (q.isBlank()) flowOf(emptyList())
            else when (target) {
                SearchTarget.ALL -> articleRepository.searchArticles(q)
                SearchTarget.FAVORITES -> favoriteRepository.searchFavorites(q)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setTarget(target: SearchTarget) { _searchTarget.value = target }
}
