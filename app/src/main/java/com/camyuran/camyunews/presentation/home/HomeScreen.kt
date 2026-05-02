package com.camyuran.camyunews.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.camyuran.camyunews.domain.model.SubCategory
import com.camyuran.camyunews.presentation.shared.ArticleCard
import com.camyuran.camyunews.presentation.shared.DatePickerDialog
import com.camyuran.camyunews.util.dateKeyToLocalDate
import com.camyuran.camyunews.util.toDisplayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onArticleClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableDates.collectAsStateWithLifecycle()
    val state = viewModel.getCurrentState()
    var showCalendar by remember { mutableStateOf(false) }
    var showKeywordFilter by remember { mutableStateOf(false) }
    val displayDates = if (availableDates.isEmpty()) listOf(state.selectedDateKey) else availableDates

    Column(modifier = Modifier.fillMaxSize()) {
        // トップバー
        TopAppBar(
            title = { Text("CamyuNews") },
            actions = {
                IconButton(onClick = { showKeywordFilter = !showKeywordFilter }) {
                    Icon(Icons.Default.Search, contentDescription = "キーワードフィルタ")
                }
                IconButton(onClick = { showCalendar = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "カレンダー")
                }
            }
        )

        // キーワードフィルタバー
        if (showKeywordFilter) {
            OutlinedTextField(
                value = state.keywordFilter,
                onValueChange = viewModel::setKeywordFilter,
                placeholder = { Text("キーワードで絞り込み") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
                trailingIcon = {
                    if (state.keywordFilter.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setKeywordFilter("") }) {
                            Icon(Icons.Default.Close, contentDescription = "クリア")
                        }
                    }
                }
            )
        }

        // 日付タブ
        val displayDates = if (availableDates.isEmpty()) listOf(state.selectedDateKey) else availableDates
        ScrollableTabRow(
            selectedTabIndex = displayDates.indexOfFirst { it == state.selectedDateKey }.coerceAtLeast(0),
            edgePadding = 0.dp
        ) {
            displayDates.forEach { dateKey ->
                Tab(
                    selected = dateKey == state.selectedDateKey,
                    onClick = { viewModel.selectDate(dateKey) },
                    text = { Text(dateKeyToLocalDate(dateKey).toDisplayLabel()) }
                )
            }
        }

        // カテゴリタブ
        TabRow(selectedTabIndex = if (state.selectedCategory == "ai") 0 else 1) {
            Tab(
                selected = state.selectedCategory == "ai",
                onClick = { viewModel.selectCategory("ai") },
                text = { Text("AI") }
            )
            Tab(
                selected = state.selectedCategory == "security",
                onClick = { viewModel.selectCategory("security") },
                text = { Text("セキュリティ") }
            )
        }

        // サブジャンルチップ
        val subCategories = SubCategory.forCategory(state.selectedCategory)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedSubCategory == null,
                    onClick = { viewModel.selectSubCategory(null) },
                    label = { Text("すべて") }
                )
            }
            items(subCategories) { subCat ->
                FilterChip(
                    selected = state.selectedSubCategory == subCat.code,
                    onClick = { viewModel.selectSubCategory(subCat.code) },
                    label = { Text(subCat.displayName) }
                )
            }
        }

        // 記事一覧
        if (articles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "記事がありません",
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
