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
        if (!containsJapanese(normalized)) {
            return normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
        }
        // 混在テキスト: 日本語文字はbigram、ASCII単語はword単位に分離してマージ
        val tokens = mutableSetOf<String>()
        val segment = StringBuilder()
        var inJapanese = false

        fun flush() {
            val s = segment.toString()
            if (inJapanese) {
                if (s.length >= 2) tokens.addAll(s.windowed(2))
            } else {
                tokens.addAll(s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() })
            }
            segment.clear()
        }

        for (char in normalized) {
            val isJp = char.code in 0x3000..0x9FFF
            if (isJp != inJapanese) { flush(); inJapanese = isJp }
            segment.append(char)
        }
        flush()
        return tokens
    }

    private fun containsJapanese(text: String): Boolean =
        text.any { it.code in 0x3000..0x9FFF }
}
