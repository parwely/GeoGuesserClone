package com.example.geogeusserclone

import com.example.geogeusserclone.data.database.AppDatabase
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.repositories.AuthRepository
import com.example.geogeusserclone.data.repositories.GameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ApiService,
        database: AppDatabase
    ): AuthRepository {
        return AuthRepository(apiService, database.userDao())
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        apiService: ApiService,
        database: AppDatabase
    ): GameRepository {
        return GameRepository(apiService, database.gameDao())
    }
}