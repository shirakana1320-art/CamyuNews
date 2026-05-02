package com.camyuran.camyunews.util

object JaccardSimilarityUtil {

    fun similarity(a: String, b: String): Double {
        val tokensA = tokenize(a)
        val tokensB = tokenize(b)
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size
        return intersection.toDouble() / union.toDouble()
    }

    internal fun tokenize(text: String): Set<String> {
        val normalized = text.lowercase().trim()
        return if (containsJapanese(normalized)) {
            // 日本語: 文字 bigram
            normalized.windowed(2).toSet()
        } else {
            // 英語: 単語単位
            normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        }
    }

    private fun containsJapanese(text: String): Boolean =
        text.any { it.code in 0x3000..0x9FFF }
}
