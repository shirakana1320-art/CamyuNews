package com.camyuran.camyunews.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.camyuran.camyunews.MainActivity
import com.camyuran.camyunews.R
import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.entity.ArticleEntity
import com.camyuran.camyunews.data.remote.gemini.GeminiError
import com.camyuran.camyunews.data.remote.gemini.GeminiService
import com.camyuran.camyunews.data.remote.gemini.SummarizeResult
import com.camyuran.camyunews.data.remote.rss.RSS_FEED_SOURCES
import com.camyuran.camyunews.data.remote.rss.RawArticle
import com.camyuran.camyunews.data.remote.rss.RssParser
import com.camyuran.camyunews.util.JaccardSimilarityUtil
import com.camyuran.camyunews.util.toDateKey
import com.camyuran.camyunews.widget.NewsWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.work.workDataOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class NewsFetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val rssParser: RssParser,
    private val geminiService: GeminiService,
    private val articleDao: ArticleDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "NewsFetchWorker"
        const val WORK_NAME_MANUAL = "NewsFetchWorker_manual"
        const val TAG_FETCH = "news_fetch"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_PROCESSED_COUNT = "processed_count"
        const val KEY_COMPLETED_AT = "completed_at"
        private const val CHANNEL_ID = "news_updates"
        private const val NOTIFICATION_ID = 1001
        private const val JACCARD_THRESHOLD = 0.5
        private val TOKYO = ZoneId.of("Asia/Tokyo")
    }

    override suspend fun doWork(): Result {
        return try {
            // Step 1: 古い記事を削除（31日超 & 非お気に入り）
            val cutoff = LocalDate.now(TOKYO).minusDays(31)
                .atStartOfDay(TOKYO).toInstant().toEpochMilli()
            val pruned = articleDao.pruneOldArticles(cutoff)

            // Step 1.5: 今日フェッチされた pubDate なし記事の dateKey を今日の日付に修正
            val todayKey = LocalDate.now(TOKYO).toDateKey()
            val todayStartMs = LocalDate.now(TOKYO).atStartOfDay(TOKYO).toInstant().toEpochMilli()
            articleDao.fixInvalidDateKeys(todayKey, todayStartMs)

            // Step 2: RSS 並行取得
            val rawArticles = fetchAllFeeds()
            if (rawArticles.isEmpty()) return Result.success()

            // Step 3: 既存記事との重複排除
            val since = LocalDate.now(TOKYO).minusDays(3)
                .atStartOfDay(TOKYO).toInstant().toEpochMilli()
            val existingIds = articleDao.getRecentArticleIds(since).toSet()
            val newArticles = rawArticles.filter { article ->
                val id = article.url.toArticleId()
                id !in existingIds
            }

            // Step 3.5: summaryJa=null の未処理記事を再処理対象として取得
            val unprocessedEntities = articleDao.getUnprocessedArticles()
            val unprocessedAsRaw = unprocessedEntities.map { entity ->
                val urls = try {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.originalUrls)
                } catch (e: Exception) { listOf(entity.originalUrls) }
                val names = try {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.sourceNames)
                } catch (e: Exception) { listOf(entity.sourceNames) }
                com.camyuran.camyunews.data.remote.rss.RawArticle(
                    title = entity.titleJa,
                    url = urls.firstOrNull() ?: "",
                    publishedAt = entity.publishedAt,
                    sourceName = names.firstOrNull() ?: "",
                    defaultCategory = entity.category,
                    defaultSubCategory = entity.subCategory
                )
            }

            if (newArticles.isEmpty() && unprocessedEntities.isEmpty()) {
                updateWidget()
                return Result.success()
            }

            // Step 4: タイトル類似度グループ化（新規記事のみ）
            val groups = groupByTitleSimilarity(newArticles)

            // Step 5: Gemini AI 処理（5秒間隔で1件ずつ、レートリミット時は65秒待機して継続）
            var hadRateLimit = false
            var apiKeyMissing = false
            var processedCount = 0
            val entitiesToInsert = mutableListOf<ArticleEntity>()
            var interRequestDelay = 5_000L

            for ((groupId, group) in groups) {
                if (apiKeyMissing || hadRateLimit) {
                    entitiesToInsert.addAll(group.map { it.toEntityWithoutSummary(groupId) })
                    continue
                }
                delay(interRequestDelay)

                when (val result = geminiService.summarizeArticleGroup(group)) {
                    is SummarizeResult.Success -> {
                        interRequestDelay = 5_000L
                        val summary = result.result
                        val representativeId = group.first().url.toArticleId()
                        entitiesToInsert.add(
                            ArticleEntity(
                                id = representativeId,
                                titleJa = summary.titleJa,
                                summaryJa = summary.summaryJa,
                                originalUrls = Json.encodeToString(group.map { it.url }),
                                sourceNames = Json.encodeToString(group.map { it.sourceName }.distinct()),
                                publishedAt = group.maxOf { it.publishedAt },
                                dateKey = group.maxOf { it.publishedAt }.toDateKey(),
                                category = summary.category,
                                subCategory = summary.subCategory,
                                topicalityScore = summary.topicalityScore.coerceIn(1, 10),
                                groupId = if (group.size > 1) groupId else null,
                                fetchedAt = System.currentTimeMillis()
                            )
                        )
                        processedCount++
                    }
                    is SummarizeResult.Failure -> {
                        when (result.error) {
                            is GeminiError.RateLimitExceeded -> {
                                hadRateLimit = true
                                entitiesToInsert.addAll(group.map { it.toEntityWithoutSummary(groupId) })
                            }
                            is GeminiError.ApiKeyMissing -> {
                                apiKeyMissing = true
                                entitiesToInsert.addAll(group.map { it.toEntityWithoutSummary(groupId) })
                            }
                            else -> {
                                entitiesToInsert.addAll(group.map { it.toEntityWithoutSummary(groupId) })
                            }
                        }
                    }
                }
            }

            // Step 5.5: summaryJa=null の未処理記事を再処理
            if (!apiKeyMissing && !hadRateLimit && unprocessedEntities.isNotEmpty()) {
                for (entity in unprocessedEntities) {
                    if (apiKeyMissing || hadRateLimit) break
                    val rawForRetry = unprocessedAsRaw.find { it.url == try {
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.originalUrls).firstOrNull() ?: ""
                    } catch (e: Exception) { entity.originalUrls } } ?: continue

                    delay(interRequestDelay)
                    when (val result = geminiService.summarizeArticleGroup(listOf(rawForRetry))) {
                        is SummarizeResult.Success -> {
                            interRequestDelay = 5_000L
                            val summary = result.result
                            articleDao.update(
                                entity.copy(
                                    titleJa = summary.titleJa,
                                    summaryJa = summary.summaryJa,
                                    category = summary.category,
                                    subCategory = summary.subCategory,
                                    topicalityScore = summary.topicalityScore.coerceIn(1, 10)
                                )
                            )
                            processedCount++
                        }
                        is SummarizeResult.Failure -> {
                            when (result.error) {
                                is GeminiError.RateLimitExceeded -> {
                                    hadRateLimit = true
                                    interRequestDelay = 65_000L
                                }
                                is GeminiError.ApiKeyMissing -> apiKeyMissing = true
                                else -> Unit
                            }
                        }
                    }
                }
            }

            // Step 6: Room 保存
            articleDao.insertAll(entitiesToInsert)

            // Step 7: 通知
            if (processedCount > 0) {
                sendNotification(processedCount)
            }

            // Step 8: Widget 更新
            updateWidget()

            val errorType = when {
                apiKeyMissing -> "api_key_missing"
                hadRateLimit -> "rate_limit"
                else -> "none"
            }
            Result.success(
                workDataOf(
                    KEY_ERROR_TYPE to errorType,
                    KEY_PROCESSED_COUNT to processedCount,
                    KEY_COMPLETED_AT to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private suspend fun fetchAllFeeds(): List<RawArticle> = coroutineScope {
        RSS_FEED_SOURCES.map { source ->
            async(Dispatchers.IO) { rssParser.fetchAndParse(source) }
        }.awaitAll().flatten()
    }

    private fun groupByTitleSimilarity(articles: List<RawArticle>): Map<String, List<RawArticle>> {
        val groups = mutableListOf<MutableList<RawArticle>>()
        for (article in articles) {
            val existingGroup = groups.find { group ->
                group.any { existing ->
                    JaccardSimilarityUtil.similarity(existing.title, article.title) >= JACCARD_THRESHOLD
                }
            }
            if (existingGroup != null) {
                existingGroup.add(article)
            } else {
                groups.add(mutableListOf(article))
            }
        }
        return groups.associate { group ->
            val groupId = group.first().url.toArticleId()
            groupId to group
        }
    }

    private fun RawArticle.toEntityWithoutSummary(groupId: String): ArticleEntity {
        val id = url.toArticleId()
        return ArticleEntity(
            id = id,
            titleJa = title,
            summaryJa = null,
            originalUrls = Json.encodeToString(listOf(url)),
            sourceNames = Json.encodeToString(listOf(sourceName)),
            publishedAt = publishedAt,
            dateKey = publishedAt.toDateKey(),
            category = defaultCategory,
            subCategory = defaultSubCategory,
            topicalityScore = 5,
            groupId = groupId.takeIf { it != id },
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun String.toArticleId(): String {
        val normalized = this
            .replace(Regex("[?#].*$"), "")
            .trimEnd('/')
            .lowercase()
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    private fun sendNotification(count: Int) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.notification_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText("${count}件の新しいニュースが届きました")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private suspend fun updateWidget() = withContext(Dispatchers.Main) {
        try {
            NewsWidget.updateAll(applicationContext)
        } catch (e: Exception) {
            // ウィジェット未配置時は無視
        }
    }
}
