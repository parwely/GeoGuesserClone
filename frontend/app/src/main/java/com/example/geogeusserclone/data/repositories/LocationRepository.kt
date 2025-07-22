package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao
) : BaseRepository() {

    suspend fun getRandomLocation(): Result<LocationEntity> {
        return try {
            // Try to get from API first
            val response = apiService.getRandomLocation()
            if (response.isSuccessful) {
                val locationResponse = response.body()!!
                val location = LocationEntity(
                    id = locationResponse.id,
                    imageUrl = locationResponse.imageUrl,
                    latitude = locationResponse.latitude,
                    longitude = locationResponse.longitude,
                    country = locationResponse.country,
                    city = locationResponse.city,
                    difficulty = locationResponse.difficulty,
                    isUsed = false
                )

                locationDao.insertLocation(location)
                Result.success(location)
            } else {
                // Fallback to local unused location
                getLocalUnusedLocation()
            }
        } catch (e: Exception) {
            // Fallback to local unused location
            getLocalUnusedLocation()
        }
    }

    private suspend fun getLocalUnusedLocation(): Result<LocationEntity> {
        val location = locationDao.getRandomUnusedLocation()
        return if (location != null) {
            Result.success(location)
        } else {
            // Create fallback location if none available
            val fallbackLocation = LocationEntity(
                id = UUID.randomUUID().toString(),
                imageUrl = "file:///android_asset/fallback_location.jpg",
                latitude = 48.8566,
                longitude = 2.3522,
                country = "France",
                city = "Paris",
                difficulty = 2,
                isUsed = false
            )
            locationDao.insertLocation(fallbackLocation)
            Result.success(fallbackLocation)
        }
    }

    suspend fun markLocationAsUsed(locationId: String) {
        locationDao.markLocationAsUsed(locationId)
    }

    suspend fun preloadLocations(count: Int = 20) {
        try {
            val response = apiService.getLocationsBatch(count)
            if (response.isSuccessful) {
                val locations = response.body()!!.locations.map { locationResponse ->
                    LocationEntity(
                        id = locationResponse.id,
                        imageUrl = locationResponse.imageUrl,
                        latitude = locationResponse.latitude,
                        longitude = locationResponse.longitude,
                        country = locationResponse.country,
                        city = locationResponse.city,
                        difficulty = locationResponse.difficulty,
                        isUsed = false
                    )
                }
                locationDao.insertLocations(locations)
            }
        } catch (e: Exception) {
            // Silently fail - fallback will be used
        }
    }

    fun getAllLocations(): Flow<List<LocationEntity>> = locationDao.getAllLocations()
}