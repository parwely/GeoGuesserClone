package com.example.geogeusserclone

import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.MapillaryApiService
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
        mapillaryApiService: MapillaryApiService,
        locationDao: LocationDao
    ): LocationRepository {
        return LocationRepository(apiService, mapillaryApiService, locationDao)
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        apiService: ApiService,
        gameDao: GameDao,
        guessDao: GuessDao,
        locationDao: LocationDao,
        userRepository: UserRepository
    ): GameRepository {
        return GameRepository(apiService, gameDao, guessDao, locationDao, userRepository)
    }
}
