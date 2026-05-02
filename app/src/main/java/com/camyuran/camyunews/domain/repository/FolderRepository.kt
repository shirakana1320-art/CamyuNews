package com.camyuran.camyunews.domain.repository

import com.camyuran.camyunews.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun createFolder(name: String): Long
    suspend fun renameFolder(id: Long, newName: String)
    suspend fun deleteFolder(id: Long)
}
