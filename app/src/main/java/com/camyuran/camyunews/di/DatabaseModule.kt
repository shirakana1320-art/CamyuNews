package com.camyuran.camyunews.di

import android.content.Context
import androidx.room.Room
import com.camyuran.camyunews.data.local.CamyuNewsDatabase
import com.camyuran.camyunews.data.local.dao.ArticleDao
import com.camyuran.camyunews.data.local.dao.FavoriteDao
import com.camyuran.camyunews.data.local.dao.FolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CamyuNewsDatabase =
        Room.databaseBuilder(context, CamyuNewsDatabase::class.java, "camyunews.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideArticleDao(db: CamyuNewsDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideFavoriteDao(db: CamyuNewsDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideFolderDao(db: CamyuNewsDatabase): FolderDao = db.folderDao()
}
