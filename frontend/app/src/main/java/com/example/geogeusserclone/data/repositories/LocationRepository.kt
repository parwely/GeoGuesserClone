package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao
) : BaseRepository() {

    fun getCachedLocations(): Flow<List<LocationEntity>> = locationDao.getCachedLocations()

    suspend fun getRandomLocation(): Result<LocationEntity> {
        return try {
            // Versuche zuerst eine unbenutzte lokale Location zu finden
            val unusedLocation = locationDao.getRandomUnusedLocation()
            if (unusedLocation != null) {
                locationDao.markLocationAsUsed(unusedLocation.id)
                return Result.success(unusedLocation)
            }

            // Falls keine lokalen Locations, erstelle Fallback-Locations
            val fallbackLocations = createFallbackLocations()
            if (fallbackLocations.isNotEmpty()) {
                locationDao.insertLocations(fallbackLocations)
                val randomLocation = fallbackLocations.random()
                locationDao.markLocationAsUsed(randomLocation.id)
                return Result.success(randomLocation)
            }

            // Versuche online
            val response = apiService.getRandomLocation()
            if (response.isSuccessful) {
                val locationResponse = response.body()!!
                val locationEntity = LocationEntity(
                    id = locationResponse.id,
                    latitude = locationResponse.latitude,
                    longitude = locationResponse.longitude,
                    imageUrl = locationResponse.imageUrl,
                    country = locationResponse.country,
                    city = locationResponse.city,
                    difficulty = locationResponse.difficulty,
                    isCached = false,
                    isUsed = true
                )

                locationDao.insertLocation(locationEntity)
                Result.success(locationEntity)
            } else {
                Result.failure(Exception("Keine Locations verfügbar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createFallbackLocations(): List<LocationEntity> {
        return listOf(
            LocationEntity(
                id = "fallback_1",
                latitude = 48.8566,
                longitude = 2.3522,
                imageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52",
                country = "France",
                city = "Paris",
                difficulty = 2,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_2",
                latitude = 51.5074,
                longitude = -0.1278,
                imageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad",
                country = "United Kingdom",
                city = "London",
                difficulty = 2,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_3",
                latitude = 40.7128,
                longitude = -74.0060,
                imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9",
                country = "United States",
                city = "New York",
                difficulty = 3,
                isCached = true,
                isUsed = false
            )
        )
    }

    suspend fun preloadLocations() {
        try {
            val response = apiService.getLocations(50)
            if (response.isSuccessful) {
                val locationsResponse = response.body()!!
                val locationEntities = locationsResponse.locations.map { location ->
                    LocationEntity(
                        id = location.id,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        imageUrl = location.imageUrl,
                        country = location.country,
                        city = location.city,
                        difficulty = location.difficulty,
                        isCached = true,
                        isUsed = false
                    )
                }
                locationDao.insertLocations(locationEntities)
            }
        } catch (e: Exception) {
            // Ignoriere Netzwerkfehler beim Preloading
        }
    }

    suspend fun resetLocationUsage() {
        locationDao.resetAllLocationsUsage()
    }

    suspend fun getCachedLocationCount(): Int = locationDao.getCachedLocationCount()
}