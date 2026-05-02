package com.camyuran.camyunews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.data.local.dao.FolderDao
import com.camyuran.camyunews.data.local.entity.ArticleEntity
import com.camyuran.camyunews.data.local.entity.FavoriteEntity
import com.camyuran.camyunews.data.local.entity.FolderEntity

@Database(
    entities = [ArticleEntity::class, FavoriteEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CamyuNewsDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun folderDao(): FolderDao
}
