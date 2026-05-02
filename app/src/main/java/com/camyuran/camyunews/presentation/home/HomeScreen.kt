package com.camyuran.camyunews.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val availableDates by viewModel.availableDates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showCalendar by remember { mutableStateOf(false) }
    var showKeywordFilter by remember { mutableStateOf(false) }
    val displayDates = if (availableDates.isEmpty()) listOf(uiState.selectedDateKey) else availableDates

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("CamyuNews") },
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

        TabRow(selectedTabIndex = if (uiState.selectedCategory == "ai") 0 else 1) {
            Tab(
                selected = uiState.selectedCategory == "ai",
                onClick = { viewModel.selectCategory("ai") },
                text = { Text("AI") }
            )
            Tab(
                selected = uiState.selectedCategory == "security",
                onClick = { viewModel.selectCategory("security") },
                text = { Text("セキュリティ") }
            )
        }

        val subCategories = SubCategory.forCategory(uiState.selectedCategory)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedSubCategory == null,
                    onClick = { viewModel.selectSubCategory(null) },
                    label = { Text("すべて") }
                )
            }
            items(subCategories) { subCat ->
                FilterChip(
                    selected = uiState.selectedSubCategory == subCat.code,
                    onClick = { viewModel.selectSubCategory(subCat.code) },
                    label = { Text(subCat.displayName) }
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
