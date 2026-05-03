package com.camyuran.camyunews.presentation.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.camyuran.camyunews.presentation.shared.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onArticleClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val isShowingAll by viewModel.isShowingAll.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf<Long?>(null) }
    var renameFolderName by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize()) {
        // フォルダサイドバー
        NavigationDrawerSheet(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Text(
                    "お気に入り",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("すべて") },
                    selected = isShowingAll,
                    onClick = { viewModel.showAll() },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                NavigationDrawerItem(
                    label = { Text("未分類") },
                    selected = !isShowingAll && selectedFolderId == null,
                    onClick = { viewModel.selectFolder(null) },
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                folders.forEach { folder ->
                    NavigationDrawerItem(
                        label = { Text(folder.name, maxLines = 1) },
                        selected = !isShowingAll && selectedFolderId == folder.id,
                        onClick = { viewModel.selectFolder(folder.id) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        badge = {
                            IconButton(
                                onClick = {
                                    renameFolderName = folder.name
                                    showRenameDialog = folder.id
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "編集", modifier = Modifier.size(14.dp))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新規フォルダ")
                }
            }
        }

        // 記事リスト
        if (articles.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text("お気に入りがありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(articles, key = { it.id }) { article ->
                    ArticleCard(article = article, onClick = { onArticleClick(article.id) })
                }
            }
        }
    }

    // 新規フォルダ作成ダイアログ
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; newFolderName = "" },
            title = { Text("フォルダを作成") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("フォルダ名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName.trim())
                        newFolderName = ""
                        showCreateFolderDialog = false
                    }
                }) { Text("作成") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false; newFolderName = "" }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // リネームダイアログ
    showRenameDialog?.let { folderId ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("フォルダを編集") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameFolderName,
                        onValueChange = { renameFolderName = it },
                        label = { Text("フォルダ名") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameFolderName.isNotBlank()) {
                        viewModel.renameFolder(folderId, renameFolderName.trim())
                        showRenameDialog = null
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.deleteFolder(folderId)
                            showRenameDialog = null
                        }
                    ) { Text("削除", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { showRenameDialog = null }) { Text("キャンセル") }
                }
            }
        )
    }
}

@Composable
private fun NavigationDrawerSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        content = { Column(content = content) }
    )
}
