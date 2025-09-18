package com.example.geogeusserclone

import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.GameApi
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
        userDao: UserDao
    ): UserRepository {
        return UserRepository(apiService, userDao)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        apiService: ApiService,
        locationDao: LocationDao
    ): LocationRepository {
        return LocationRepository(apiService, locationDao)
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        gameApi: GameApi
    ): GameRepository {
        return GameRepository(gameApi)
    }
}
