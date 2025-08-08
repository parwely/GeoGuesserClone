package com.example.geogeusserclone

import com.example.geogeusserclone.data.database.AppDatabase
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.repositories.*
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
        return UserRepository(
            apiService = apiService,
            userDao = database.userDao(),
            authInterceptor = authInterceptor
        )
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        apiService: ApiService,
        database: AppDatabase
    ): LocationRepository {
        return LocationRepository(
            apiService = apiService,
            locationDao = database.locationDao()
        )
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        apiService: ApiService,
        database: AppDatabase,
        userRepository: UserRepository
    ): GameRepository {
        return GameRepository(
            apiService = apiService,
            gameDao = database.gameDao(),
            guessDao = database.guessDao(),
            userRepository = userRepository
        )
    }

    @Provides
    @Singleton
    fun provideLocationCacheRepository(
        apiService: ApiService,
        database: AppDatabase
    ): LocationCacheRepository {
        return LocationCacheRepository(
            apiService = apiService,
            locationDao = database.locationDao()
        )
    }
}

