package com.camyuran.camyunews.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TOKYO = ZoneId.of("Asia/Tokyo")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Long.toDateKey(): String =
    Instant.ofEpochMilli(this)
        .atZone(TOKYO)
        .toLocalDate()
        .format(DATE_FORMATTER)

fun LocalDate.toDateKey(): String = format(DATE_FORMATTER)

fun todayDateKey(): String = LocalDate.now(TOKYO).toDateKey()

fun recentDateKeys(days: Int = 30): List<String> {
    val today = LocalDate.now(TOKYO)
    return (0 until days).map { today.minusDays(it.toLong()).toDateKey() }
}

fun dateKeyToLocalDate(dateKey: String): LocalDate =
    LocalDate.parse(dateKey, DATE_FORMATTER)

fun LocalDate.toDisplayLabel(): String {
    val today = LocalDate.now(TOKYO)
    return when {
        this == today -> "今日"
        this == today.minusDays(1) -> "昨日"
        this == today.minusDays(2) -> "一昨日"
        else -> "${monthValue}/${dayOfMonth}"
    }
}
