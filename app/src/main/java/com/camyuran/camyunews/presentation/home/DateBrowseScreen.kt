package com.camyuran.camyunews.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.camyuran.camyunews.presentation.shared.ArticleCard
import com.camyuran.camyunews.presentation.shared.DatePickerDialog
import com.camyuran.camyunews.util.dateKeyToLocalDate
import com.camyuran.camyunews.util.toDisplayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateBrowseScreen(
    onArticleClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val articles by viewModel.dateArticles.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableDates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val fetchProgress by viewModel.fetchProgress.collectAsStateWithLifecycle()
    var showCalendar by remember { mutableStateOf(false) }
    var showKeywordFilter by remember { mutableStateOf(false) }
    val displayDates = if (availableDates.isEmpty()) listOf(uiState.selectedDateKey) else availableDates
    val clipboardManager = LocalClipboardManager.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.clearFetchError()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("日別") },
                windowInsets = WindowInsets(0),
                actions = {
                    IconButton(onClick = { showKeywordFilter = !showKeywordFilter }) {
                        Icon(Icons.Default.Search, contentDescription = "キーワードフィルタ")
                    }
                    IconButton(onClick = { showCalendar = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "カレンダー")
                    }
                    IconButton(onClick = { viewModel.triggerRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "更新")
                    }
                }
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                fetchProgress?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (showKeywordFilter) {
                OutlinedTextField(
                    value = uiState.keywordFilter,
                    onValueChange = viewModel::setKeywordFilter,
                    placeholder = { Text("キーワードで絞り込み") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.keywordFilter.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setKeywordFilter("") }) {
                                Icon(Icons.Default.Close, contentDescription = "クリア")
                            }
                        }
                    }
                )
            }

            ScrollableTabRow(
                selectedTabIndex = displayDates.indexOfFirst { it == uiState.selectedDateKey }.coerceAtLeast(0),
                edgePadding = 0.dp
            ) {
                displayDates.forEach { dateKey ->
                    Tab(
                        selected = dateKey == uiState.selectedDateKey,
                        onClick = { viewModel.selectDate(dateKey) },
                        text = { Text(dateKeyToLocalDate(dateKey).toDisplayLabel()) }
                    )
                }
            }

            if (articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isLoading) "ニュースを取得中..." else "記事がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        ArticleCard(article = article, onClick = { onArticleClick(article.id) })
                    }
                }
            }
        }

        uiState.fetchError?.let { error ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(error)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "コピー",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(
                        onClick = viewModel::clearFetchError,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "閉じる",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    if (showCalendar) {
        DatePickerDialog(
            onDateSelected = { dateKey ->
                viewModel.selectDate(dateKey)
                showCalendar = false
            },
            onDismiss = { showCalendar = false },
            availableDateKeys = displayDates
        )
    }
}
