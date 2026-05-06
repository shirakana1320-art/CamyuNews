package com.camyuran.camyunews.data.remote.gemini

import com.camyuran.camyunews.data.remote.rss.RawArticle
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.3f
            responseMimeType = "application/json"
        }
    )

    suspend fun testConnection(): GeminiError? {
        val apiKey = apiKeyProvider.getApiKey()
            ?: return GeminiError.ApiKeyMissing
        return try {
            withTimeout(10_000) {
                val model = createModel(apiKey)
                model.generateContent("ping")
            }
            null
        } catch (e: TimeoutCancellationException) {
            GeminiError.Unknown("タイムアウト（10秒）: Gemini サーバーに到達できませんでした")
        } catch (e: Exception) {
            classifyException(e)
        }
    }

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
            val result = json.decodeFromString<GeminiSummaryResult>(extractJson(text))
            SummarizeResult.Success(result)
        } catch (e: Exception) {
            SummarizeResult.Failure(classifyException(e))
        }
    }

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        // ```json ... ``` または ``` ... ``` を除去
        val fenceStripped = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```").find(trimmed)
            ?.groupValues?.get(1)
        if (fenceStripped != null) return fenceStripped
        // フェンスなしの場合は { から } までを抽出
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start != -1 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun classifyException(e: Exception): GeminiError {
        val msg = buildString {
            var ex: Throwable? = e
            while (ex != null) {
                if (isNotEmpty()) append(" | ")
                append(ex.message ?: ex.javaClass.simpleName)
                ex = ex.cause
            }
        }
        return when {
            msg.contains("limit: 0", ignoreCase = true) || msg.contains("limit:0", ignoreCase = true)
                || msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                GeminiError.RateLimitExceeded
            msg.contains("API_KEY_INVALID", ignoreCase = true) ->
                GeminiError.ApiKeyMissing
            msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("404") ->
                GeminiError.Unknown("モデルが見つかりません（NOT_FOUND）。生エラー: ${msg.take(200)}")
            else -> GeminiError.Unknown(msg.take(300))
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
  "subCategory": "以下のいずれか1つ: llm/image_gen/research/business/ethics/robotics/claude_code/codex/incident/cve/malware/privacy/phishing/regulation",
  "topicalityScore": 話題性スコア1〜10の整数（業界への影響度・注目度で判断）
}

subCategory の選び方:
- claude_code: Anthropic・Claude・Claude Code・Claude API・Claudeモデルに関する記事
- codex: OpenAI・Codex・ChatGPT・GPTシリーズ・OpenAI APIに関する記事
- llm: 上記以外の大規模言語モデル全般
- image_gen: 画像・動画・音声生成AI
- research: AI研究・学術論文・ベンチマーク
- business: AI企業の事業・投資・製品発表（claude_code/codex に該当しない場合）
- ethics: AI規制・倫理・政策・法律
- robotics: ロボット・自律システム・ドローン
        """.trimIndent()
    }
}
