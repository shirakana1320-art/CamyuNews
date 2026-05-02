package com.camyuran.camyunews.data.repository

import com.camyuran.camyunews.data.local.dao.FolderDao
import com.camyuran.camyunews.data.local.entity.FolderEntity
import com.camyuran.camyunews.domain.model.Folder
import com.camyuran.camyunews.domain.repository.FolderRepository
import com.camyuran.camyunews.util.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> =
        folderDao.getAllFolders().map { list -> list.map { it.toDomain() } }

    override suspend fun createFolder(name: String): Long =
        folderDao.insert(FolderEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun renameFolder(id: Long, newName: String) {
        val existing = folderDao.getById(id) ?: return
        folderDao.update(existing.copy(name = newName))
    }

    override suspend fun deleteFolder(id: Long) = folderDao.deleteById(id)
}
