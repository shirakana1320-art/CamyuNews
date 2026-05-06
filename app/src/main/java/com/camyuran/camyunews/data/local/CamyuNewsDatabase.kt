package com.camyuran.camyunews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.data.local.dao.FolderDao
import com.camyuran.camyunews.data.local.entity.ArticleEntity
import com.camyuran.camyunews.data.local.entity.FavoriteEntity
import com.camyuran.camyunews.data.local.entity.FolderEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE articles ADD COLUMN originalTitles TEXT NOT NULL DEFAULT '[]'")
    }
}

@Database(
    entities = [ArticleEntity::class, FavoriteEntity::class, FolderEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CamyuNewsDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun folderDao(): FolderDao
}
