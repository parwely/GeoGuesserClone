package com.example.geogeusserclone

import com.example.geogeusserclone.data.database.AppDatabase
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
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
    fun provideUserRepository(
        apiService: ApiService,
        database: AppDatabase,
        authInterceptor: AuthInterceptor
    ): UserRepository {
        return UserRepository(apiService, database.userDao(), authInterceptor)
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        apiService: ApiService,
        database: AppDatabase
    ): GameRepository {
        return GameRepository(apiService, database.gameDao(), database.guessDao())
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        apiService: ApiService,
        database: AppDatabase
    ): LocationRepository {
        return LocationRepository(apiService, database.locationDao())
    }
}