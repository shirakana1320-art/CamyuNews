package com.camyuran.camyunews.util

import com.camyuran.camyunews.data.local.entity.ArticleEntity
import com.camyuran.camyunews.data.local.entity.FolderEntity
import com.camyuran.camyunews.domain.model.Article
import com.camyuran.camyunews.domain.model.Folder
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun ArticleEntity.toDomain(isFavorite: Boolean = false): Article = Article(
    id = id,
    titleJa = titleJa,
    summaryJa = summaryJa,
    originalUrls = try {
        json.decodeFromString<List<String>>(originalUrls)
    } catch (e: Exception) {
        listOf(originalUrls)
    },
    sourceNames = try {
        json.decodeFromString<List<String>>(sourceNames)
    } catch (e: Exception) {
        listOf(sourceNames)
    },
    originalTitles = try {
        json.decodeFromString<List<String>>(originalTitles)
    } catch (e: Exception) {
        emptyList()
    },
    publishedAt = publishedAt,
    dateKey = dateKey,
    category = category,
    subCategory = subCategory,
    topicalityScore = topicalityScore,
    isRead = isRead,
    groupId = groupId,
    fetchedAt = fetchedAt,
    isFavorite = isFavorite
)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    createdAt = createdAt
)
