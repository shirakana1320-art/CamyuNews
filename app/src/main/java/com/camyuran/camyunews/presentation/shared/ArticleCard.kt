package com.camyuran.camyunews.presentation.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.model.SubCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (article.isRead) 0.6f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (article.isRead)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ヘッダー行（ソース名・スコア・お気に入りマーク）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.sourceNames.firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    modifier = Modifier.weight(1f)
                )
                // 話題性バッジ
                if (article.topicalityScore >= 8) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("🔥 Hot", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (article.isFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "お気に入り",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!article.isRead) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge { Text("NEW") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // タイトル
            Text(
                text = article.titleJa,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // 要約プレビュー
            if (article.summaryJa != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.summaryJa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "要約を取得中...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // フッター（サブジャンル・日時）
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubCategory.fromCode(article.subCategory)?.let { subCat ->
                    AssistChip(
                        onClick = {},
                        label = { Text(subCat.displayName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = article.publishedAt.toDisplayTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
    }
}

private fun Long.toDisplayTime(): String = try {
    val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.of("Asia/Tokyo"))
        .format(formatter)
} catch (e: Exception) { "" }
