package com.camyuran.camyunews.data.remote.gemini

import com.camyuran.camyunews.data.remote.rss.RawArticle
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiSummaryResult(
    val titleJa: String,
    val summaryJa: String,
    val category: String,
    val subCategory: String,
    val topicalityScore: Int
)

sealed class GeminiError {
    data object ApiKeyMissing : GeminiError()
    data object RateLimitExceeded : GeminiError()
    data class Unknown(val message: String) : GeminiError()
}

sealed class SummarizeResult {
    data class Success(val result: GeminiSummaryResult) : SummarizeResult()
    data class Failure(val error: GeminiError) : SummarizeResult()
}

@Singleton
class GeminiService @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun createModel(apiKey: String): GenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.3f
            responseMimeType = "application/json"
        }
    )

    suspend fun summarizeArticleGroup(articles: List<RawArticle>): SummarizeResult {
        val apiKey = apiKeyProvider.getApiKey()
            ?: return SummarizeResult.Failure(GeminiError.ApiKeyMissing)

        val prompt = buildPrompt(articles)
        return try {
            val model = createModel(apiKey)
            val response = model.generateContent(prompt)
            val text = response.text ?: return SummarizeResult.Failure(
                GeminiError.Unknown("空のレスポンス")
            )
            val result = json.decodeFromString<GeminiSummaryResult>(text.trim())
            SummarizeResult.Success(result)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)) {
                SummarizeResult.Failure(GeminiError.RateLimitExceeded)
            } else {
                SummarizeResult.Failure(GeminiError.Unknown(msg))
            }
        }
    }

    private fun buildPrompt(articles: List<RawArticle>): String {
        val articlesText = articles.take(5).joinToString("\n---\n") { article ->
            "タイトル: ${article.title}\nURL: ${article.url}\nソース: ${article.sourceName}"
        }
        return """
以下のニュース記事（英語・日本語が混在する場合あり）を分析して、日本語でまとめてください。
複数の記事が同じトピックを扱っている場合は統合して1つの要約にしてください。

記事:
$articlesText

以下のJSON形式のみで返してください（マークダウン不要）:
{
  "titleJa": "日本語タイトル（50文字以内）",
  "summaryJa": "日本語要約（150〜300文字、箇条書き不要・段落形式）",
  "category": "ai または security のいずれか",
  "subCategory": "llm/image_gen/research/business/ethics/robotics/incident/cve/malware/privacy/phishing/regulation のいずれか",
  "topicalityScore": 話題性スコア1〜10の整数（業界への影響度・注目度で判断）
}
        """.trimIndent()
    }
}
