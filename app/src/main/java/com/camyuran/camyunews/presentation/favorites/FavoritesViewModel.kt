package com.camyuran.camyunews.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.model.Folder
import com.camyuran.camyunews.domain.repository.FavoriteRepository
import com.camyuran.camyunews.domain.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedFolderId = MutableStateFlow<Long?>(null)
    val isShowingAll = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: StateFlow<List<Article>> = isShowingAll.flatMapLatest { showAll ->
        if (showAll) favoriteRepository.getAllFavoriteArticles()
        else {
            val folderId = selectedFolderId.value
            if (folderId == null) favoriteRepository.getUncategorizedFavorites()
            else favoriteRepository.getFavoriteArticlesByFolder(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFolder(folderId: Long?) {
        selectedFolderId.value = folderId
        isShowingAll.value = false
    }

    fun showAll() { isShowingAll.value = true }

    fun createFolder(name: String) {
        viewModelScope.launch { folderRepository.createFolder(name) }
    }

    fun renameFolder(id: Long, newName: String) {
        viewModelScope.launch { folderRepository.renameFolder(id, newName) }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch { folderRepository.deleteFolder(id) }
    }
}
