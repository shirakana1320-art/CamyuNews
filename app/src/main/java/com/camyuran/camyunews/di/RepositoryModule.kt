package com.camyuran.camyunews.di

import com.camyuran.camyunews.data.repository.ArticleRepositoryImpl
import com.camyuran.camyunews.data.repository.FavoriteRepositoryImpl
import com.camyuran.camyunews.data.repository.FolderRepositoryImpl
import com.camyuran.camyunews.domain.repository.ArticleRepository
import com.camyuran.camyunews.domain.repository.FavoriteRepository
import com.camyuran.camyunews.domain.repository.FolderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository
}
