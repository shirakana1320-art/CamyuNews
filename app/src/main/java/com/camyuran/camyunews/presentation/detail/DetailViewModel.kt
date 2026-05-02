package com.camyuran.camyunews.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.domain.repository.FavoriteRepository
import com.camyuran.camyunews.domain.repository.FolderRepository
import com.camyuran.camyunews.domain.model.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val article: Article? = null,
    val relatedArticles: List<Article> = emptyList(),
    val isFavorite: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val favoriteRepository: FavoriteRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val folders: StateFlow<List<Folder>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            val article = articleRepository.getById(articleId)
            if (article != null) {
                articleRepository.markAsRead(articleId)
                _uiState.value = DetailUiState(
                    article = article,
                    isLoading = false
                )
                // お気に入り状態を監視
                launch {
                    favoriteRepository.isFavorite(articleId).collect { isFav ->
                        _uiState.value = _uiState.value.copy(isFavorite = isFav)
                    }
                }
                // 関連記事（同サブジャンル・当日）
                launch {
                    articleRepository.getArticlesByDateCategoryAndSubCategory(
                        article.dateKey, article.category, article.subCategory
                    ).collect { related ->
                        _uiState.value = _uiState.value.copy(
                            relatedArticles = related.filter { it.id != articleId }.take(5)
                        )
                    }
                }
            } else {
                _uiState.value = DetailUiState(isLoading = false)
            }
        }
    }

    fun toggleFavorite(folderId: Long? = null) {
        val articleId = _uiState.value.article?.id ?: return
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                favoriteRepository.removeFavorite(articleId)
            } else {
                favoriteRepository.addFavorite(articleId, folderId)
            }
        }
    }

    fun moveFavoriteToFolder(folderId: Long?) {
        val articleId = _uiState.value.article?.id ?: return
        viewModelScope.launch {
            favoriteRepository.moveToFolder(articleId, folderId)
        }
    }
}
