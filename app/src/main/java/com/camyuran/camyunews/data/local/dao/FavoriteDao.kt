package com.camyuran.camyunews.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.camyuran.camyunews.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE articleId = :articleId")
    suspend fun deleteByArticleId(articleId: String)

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE folderId = :folderId ORDER BY savedAt DESC")
    fun getFavoritesByFolder(folderId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE folderId IS NULL ORDER BY savedAt DESC")
    fun getUncategorizedFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE articleId = :articleId)")
    fun isFavorite(articleId: String): Flow<Boolean>

    @Query("UPDATE favorites SET folderId = :folderId WHERE articleId = :articleId")
    suspend fun updateFolder(articleId: String, folderId: Long?)

    @Query("SELECT articleId FROM favorites")
    suspend fun getAllFavoriteIds(): List<String>

    @Query("SELECT articleId FROM favorites")
    fun getFavoriteIdsFlow(): Flow<List<String>>
}
