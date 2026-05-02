package com.camyuran.camyunews.data.remote

import com.camyuran.camyunews.data.remote.rss.RssFeedSource
import com.camyuran.camyunews.data.remote.rss.RssParser
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

class RssParserTest {

    private lateinit var parser: RssParser
    private val dummySource = RssFeedSource(
        url = "https://example.com/feed",
        name = "Test Source",
        defaultCategory = "ai",
        defaultSubCategory = "llm"
    )

    @Before
    fun setUp() {
        // RssParser は KXmlParser を直接インスタンス化するため
        // android.util.Xml / XmlPullParserFactory のモック不要
        parser = RssParser(mockk(relaxed = true))
    }

    private fun parseXml(xml: String, charset: Charset = Charsets.UTF_8) =
        parser.parse(ByteArrayInputStream(xml.toByteArray(charset)), charset, dummySource)

    @Test
    fun `RSS20_有効な記事を解析できる`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <item>
                  <title>OpenAI releases GPT-5</title>
                  <link>https://example.com/gpt5</link>
                  <pubDate>Mon, 01 Jan 2026 06:00:00 GMT</pubDate>
                </item>
                <item>
                  <title>Security breach at hospital</title>
                  <link>https://example.com/breach</link>
                  <pubDate>Mon, 01 Jan 2026 07:00:00 GMT</pubDate>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val articles = parseXml(xml)

        assertEquals(2, articles.size)
        assertEquals("OpenAI releases GPT-5", articles[0].title)
        assertEquals("https://example.com/gpt5", articles[0].url)
        assertEquals("Test Source", articles[0].sourceName)
        assertEquals("ai", articles[0].defaultCategory)
    }

    @Test
    fun `Atom_有効な記事を解析できる`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Atom article title</title>
                <link href="https://example.com/atom-article"/>
                <published>2026-01-01T06:00:00Z</published>
              </entry>
            </feed>
        """.trimIndent()

        val articles = parseXml(xml)

        assertEquals(1, articles.size)
        assertEquals("Atom article title", articles[0].title)
        assertEquals("https://example.com/atom-article", articles[0].url)
    }

    @Test
    fun `不正なXMLで例外を投げずに空リストを返す`() {
        val xml = "<<invalid xml>>"
        val articles = parseXml(xml)
        assertEquals(0, articles.size)
    }

    @Test
    fun `タイトルまたはURLが空の記事はスキップされる`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title></title>
                  <link>https://example.com/missing-title</link>
                </item>
                <item>
                  <title>Missing URL article</title>
                  <link></link>
                </item>
                <item>
                  <title>Valid article</title>
                  <link>https://example.com/valid</link>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val articles = parseXml(xml)

        assertEquals(1, articles.size)
        assertEquals("Valid article", articles[0].title)
    }

    @Test
    fun `空のフィードは空リストを返す`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel><title>Empty</title></channel>
            </rss>
        """.trimIndent()

        val articles = parseXml(xml)
        assertTrue(articles.isEmpty())
    }
}
