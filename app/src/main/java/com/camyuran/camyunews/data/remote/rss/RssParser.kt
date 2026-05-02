package com.camyuran.camyunews.data.remote.rss

import okhttp3.OkHttpClient
import okhttp3.Request
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.inject.Inject

data class RawArticle(
    val title: String,
    val url: String,
    val publishedAt: Long,
    val sourceName: String,
    val defaultCategory: String,
    val defaultSubCategory: String
)

class RssParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    fun fetchAndParse(source: RssFeedSource): List<RawArticle> {
        val request = Request.Builder().url(source.url).build()
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body ?: return emptyList()
                val contentType = response.header("Content-Type") ?: ""
                val charset = extractCharset(contentType) ?: Charsets.UTF_8
                parse(body.byteStream(), charset, source)
            }
        } catch (e: IOException) {
            emptyList()
        } catch (e: XmlPullParserException) {
            emptyList()
        }
    }

    internal fun parse(
        stream: InputStream,
        charset: Charset,
        source: RssFeedSource
    ): List<RawArticle> {
        val articles = mutableListOf<RawArticle>()
        // android.util.Xml / XmlPullParserFactory のスタブを回避し、
        // kxml2 の KXmlParser を直接インスタンス化する（ユニットテスト互換）
        val parser: XmlPullParser = KXmlParser()
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(InputStreamReader(stream, charset))

            var eventType = parser.eventType
            var isAtomFeed = false
            var inEntry = false
            var title = ""
            var link = ""
            var pubDate = 0L

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name?.lowercase()) {
                            "feed" -> isAtomFeed = true
                            "item", "entry" -> {
                                inEntry = true
                                title = ""; link = ""; pubDate = 0L
                            }
                            "title" -> if (inEntry) title = parser.nextText().trim()
                            "link" -> if (inEntry) {
                                if (isAtomFeed) {
                                    link = parser.getAttributeValue(null, "href") ?: parser.nextText().trim()
                                } else {
                                    link = parser.nextText().trim()
                                }
                            }
                            "pubdate", "published", "updated", "dc:date" ->
                                if (inEntry) pubDate = parseDate(parser.nextText().trim())
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name?.lowercase() in listOf("item", "entry") && inEntry) {
                            inEntry = false
                            if (title.isNotBlank() && link.isNotBlank()) {
                                articles.add(
                                    RawArticle(
                                        title = title,
                                        url = link,
                                        publishedAt = if (pubDate == 0L) System.currentTimeMillis() else pubDate,
                                        sourceName = source.name,
                                        defaultCategory = source.defaultCategory,
                                        defaultSubCategory = source.defaultSubCategory
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            // 不正なXMLはスキップ
        }
        return articles
    }

    private fun extractCharset(contentType: String): Charset? {
        val match = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(contentType)
        return try {
            match?.groupValues?.get(1)?.let { Charset.forName(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDate(dateStr: String): Long {
        val formats = listOf(
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
        )
        for (fmt in formats) {
            try {
                return fmt.parse(dateStr)?.time ?: continue
            } catch (e: Exception) { /* 次のフォーマットを試す */ }
        }
        return System.currentTimeMillis()
    }
}
