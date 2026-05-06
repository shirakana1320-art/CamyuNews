package com.camyuran.camyunews.domain.model

data class Article(
    val id: String,
    val titleJa: String,
    val summaryJa: String?,
    val originalUrls: List<String>,
    val sourceNames: List<String>,
    val originalTitles: List<String>,
    val publishedAt: Long,
    val dateKey: String,
    val category: String,
    val subCategory: String,
    val topicalityScore: Int,
    val isRead: Boolean,
    val groupId: String?,
    val fetchedAt: Long,
    val isFavorite: Boolean = false
)

enum class Category(val code: String, val displayName: String) {
    AI("ai", "AI"),
    SECURITY("security", "セキュリティ");

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code } ?: AI
    }
}

enum class SubCategory(
    val code: String,
    val displayName: String,
    val parentCategory: String
) {
    LLM("llm", "生成AI・LLM", "ai"),
    IMAGE_GEN("image_gen", "画像・動画・音声生成", "ai"),
    RESEARCH("research", "AI研究・論文", "ai"),
    BUSINESS("business", "AIビジネス・企業動向", "ai"),
    ETHICS("ethics", "AI規制・倫理・政策", "ai"),
    ROBOTICS("robotics", "ロボット・自律システム", "ai"),
    CLAUDE_CODE("claude_code", "Claude Code", "ai"),
    CODEX("codex", "Codex / OpenAI", "ai"),
    INCIDENT("incident", "サイバー攻撃・インシデント", "security"),
    CVE("cve", "脆弱性・CVE", "security"),
    MALWARE("malware", "マルウェア・ランサムウェア", "security"),
    PRIVACY("privacy", "プライバシー・データ漏洩", "security"),
    PHISHING("phishing", "詐欺・フィッシング", "security"),
    REGULATION("regulation", "法規制・セキュリティツール", "security");

    companion object {
        fun fromCode(code: String) = entries.find { it.code == code }
        fun forCategory(categoryCode: String) = entries.filter { it.parentCategory == categoryCode }
    }
}
