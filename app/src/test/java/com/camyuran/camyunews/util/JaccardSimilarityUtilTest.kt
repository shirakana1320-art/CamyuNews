package com.camyuran.camyunews.util

import org.junit.Assert.*
import org.junit.Test

class JaccardSimilarityUtilTest {

    @Test
    fun `同一文字列は類似度1`() {
        val score = JaccardSimilarityUtil.similarity("OpenAI releases GPT-5", "OpenAI releases GPT-5")
        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun `完全に異なる文字列は類似度0`() {
        val score = JaccardSimilarityUtil.similarity("OpenAI releases GPT-5", "量子コンピューター革命")
        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun `英語タイトルの部分一致で閾値を超える`() {
        val a = "OpenAI releases GPT-5 with major improvements"
        val b = "OpenAI announces GPT-5 release with improvements"
        val score = JaccardSimilarityUtil.similarity(a, b)
        assertTrue("英語類似度が閾値0.5を超えること: actual=$score", score >= 0.5)
    }

    @Test
    fun `英語タイトルが異なるトピックの場合は閾値を下回る`() {
        val a = "Google launches new AI model"
        val b = "Major security breach at hospital"
        val score = JaccardSimilarityUtil.similarity(a, b)
        assertTrue("無関係な記事の類似度が0.5未満であること: actual=$score", score < 0.5)
    }

    @Test
    fun `日本語タイトルの類似度計算（bigram）`() {
        val a = "OpenAIがGPT-5を発表した"
        val b = "OpenAIはGPT-5の発表を行った"
        val score = JaccardSimilarityUtil.similarity(a, b)
        assertTrue("日本語類似度が正の値であること: actual=$score", score > 0.0)
    }

    @Test
    fun `片方が空文字の場合は類似度0`() {
        val score = JaccardSimilarityUtil.similarity("", "OpenAI releases GPT-5")
        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun `両方が空文字の場合は類似度1`() {
        val score = JaccardSimilarityUtil.similarity("", "")
        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun `英語トークン化が単語単位で行われる`() {
        val tokens = JaccardSimilarityUtil.tokenize("Hello World Test")
        assertEquals(setOf("hello", "world", "test"), tokens)
    }

    @Test
    fun `日本語トークン化がbigram単位で行われる`() {
        val tokens = JaccardSimilarityUtil.tokenize("AIニュース")
        assertTrue("bigramが含まれること", tokens.contains("aiニ") || tokens.contains("ニュ") || tokens.contains("ュー") || tokens.contains("ース"))
    }

    @Test
    fun `大文字小文字を無視する`() {
        val score = JaccardSimilarityUtil.similarity("OpenAI GPT", "openai gpt")
        assertEquals(1.0, score, 0.001)
    }
}
