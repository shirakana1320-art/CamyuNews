package com.camyuran.camyunews.presentation.detail

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.model.SubCategory
import com.camyuran.camyunews.presentation.shared.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFolderDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(articleId) { viewModel.loadArticle(articleId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    // お気に入りボタン
                    IconButton(onClick = { showFolderDialog = true }) {
                        Icon(
                            if (uiState.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "お気に入り",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 共有ボタン
                    IconButton(onClick = {
                        uiState.article?.originalUrls?.firstOrNull()?.let { url ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${uiState.article?.titleJa}\n$url")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "共有"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "共有")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (uiState.article == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("記事が見つかりません") }
        } else {
            val article = uiState.article!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // サブジャンルチップ
                    SubCategory.fromCode(article.subCategory)?.let {
                        AssistChip(onClick = {}, label = { Text(it.displayName) })
                    }
                }
                item {
                    // タイトル
                    Text(
                        text = article.titleJa,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    // 要約
                    if (article.summaryJa != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "AI要約",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = article.summaryJa,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        OutlinedCard {
                            Box(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "要約を生成中...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    // 引用元リスト
                    Text(
                        "引用元",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(article.originalUrls.zip(article.sourceNames.padEndWith("不明", article.originalUrls.size))) { (url, name) ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val customTabsIntent = CustomTabsIntent.Builder().build()
                                customTabsIntent.launchUrl(context, Uri.parse(url))
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(
                                    url, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
                // 関連記事
                if (uiState.relatedArticles.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("関連記事", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    items(uiState.relatedArticles, key = { it.id }) { related ->
                        ArticleCard(article = related, onClick = { viewModel.loadArticle(related.id) })
                    }
                }
            }
        }
    }

    // フォルダ選択ダイアログ
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(if (uiState.isFavorite) "フォルダを選択" else "お気に入りに追加") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("フォルダなし") },
                        modifier = Modifier.clickable {
                            viewModel.toggleFavorite(null)
                            showFolderDialog = false
                        }
                    )
                    folders.forEach { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            modifier = Modifier.clickable {
                                if (uiState.isFavorite) viewModel.moveFavoriteToFolder(folder.id)
                                else viewModel.toggleFavorite(folder.id)
                                showFolderDialog = false
                            }
                        )
                    }
                    if (uiState.isFavorite) {
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("お気に入りを解除", color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                viewModel.toggleFavorite()
                                showFolderDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("キャンセル") }
            }
        )
    }
}

private fun List<String>.padEndWith(value: String, targetSize: Int): List<String> {
    if (size >= targetSize) return this
    return this + List(targetSize - size) { value }
}
