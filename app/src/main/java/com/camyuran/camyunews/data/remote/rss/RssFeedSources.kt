package com.camyuran.camyunews.data.remote.rss

data class RssFeedSource(
    val url: String,
    val name: String,
    val defaultCategory: String,
    val defaultSubCategory: String
)

val RSS_FEED_SOURCES = listOf(
    // ── AI カテゴリ ──
    RssFeedSource(
        url = "https://www.theverge.com/ai-artificial-intelligence/rss/index.xml",
        name = "The Verge AI",
        defaultCategory = "ai",
        defaultSubCategory = "llm"
    ),
    RssFeedSource(
        url = "https://www.technologyreview.com/feed/",
        name = "MIT Technology Review",
        defaultCategory = "ai",
        defaultSubCategory = "research"
    ),
    RssFeedSource(
        url = "https://venturebeat.com/ai/feed/",
        name = "VentureBeat AI",
        defaultCategory = "ai",
        defaultSubCategory = "business"
    ),
    RssFeedSource(
        url = "https://www.wired.com/feed/tag/artificial-intelligence/rss",
        name = "Wired AI",
        defaultCategory = "ai",
        defaultSubCategory = "llm"
    ),
    RssFeedSource(
        url = "https://techcrunch.com/tag/artificial-intelligence/feed/",
        name = "TechCrunch AI",
        defaultCategory = "ai",
        defaultSubCategory = "business"
    ),
    RssFeedSource(
        url = "https://blog.google/technology/ai/rss/",
        name = "Google AI Blog",
        defaultCategory = "ai",
        defaultSubCategory = "research"
    ),
    RssFeedSource(
        url = "https://www.artificialintelligence-news.com/feed/",
        name = "AI News",
        defaultCategory = "ai",
        defaultSubCategory = "llm"
    ),
    RssFeedSource(
        url = "https://www.anthropic.com/blog.rss",
        name = "Anthropic Blog",
        defaultCategory = "ai",
        defaultSubCategory = "claude_code"
    ),
    RssFeedSource(
        url = "https://openai.com/news/rss/",
        name = "OpenAI News",
        defaultCategory = "ai",
        defaultSubCategory = "codex"
    ),
    // ── セキュリティ カテゴリ ──
    RssFeedSource(
        url = "https://feeds.feedburner.com/TheHackersNews",
        name = "The Hacker News",
        defaultCategory = "security",
        defaultSubCategory = "incident"
    ),
    RssFeedSource(
        url = "https://krebsonsecurity.com/feed/",
        name = "Krebs on Security",
        defaultCategory = "security",
        defaultSubCategory = "incident"
    ),
    RssFeedSource(
        url = "https://www.bleepingcomputer.com/feed/",
        name = "Bleeping Computer",
        defaultCategory = "security",
        defaultSubCategory = "malware"
    ),
    RssFeedSource(
        url = "https://www.darkreading.com/rss.xml",
        name = "Dark Reading",
        defaultCategory = "security",
        defaultSubCategory = "cve"
    ),
    RssFeedSource(
        url = "https://feeds.feedburner.com/Securityweek",
        name = "SecurityWeek",
        defaultCategory = "security",
        defaultSubCategory = "incident"
    ),
    RssFeedSource(
        url = "https://www.cisa.gov/sites/default/files/feeds/alerts.xml",
        name = "CISA Alerts",
        defaultCategory = "security",
        defaultSubCategory = "cve"
    ),
    RssFeedSource(
        url = "https://threatpost.com/feed/",
        name = "Threatpost",
        defaultCategory = "security",
        defaultSubCategory = "malware"
    )
)
