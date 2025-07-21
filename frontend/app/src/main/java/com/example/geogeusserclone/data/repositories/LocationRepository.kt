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
) {

    fun getCachedLocations(): Flow<List<LocationEntity>> = locationDao.getCachedLocations()

    suspend fun getRandomLocation(): Result<LocationEntity> {
        return try {
            // Try to get from server first
            val response = apiService.getRandomLocation()

            if (response.isSuccessful) {
                val locationResponse = response.body()!!
                val location = LocationEntity(
                    id = locationResponse.id,
                    latitude = locationResponse.latitude,
                    longitude = locationResponse.longitude,
                    imageUrl = locationResponse.imageUrl,
                    isCached = false
                )

                // Cache the location
                locationDao.insertLocation(location)
                Result.success(location)

            } else {
                // Fallback to cached locations
                getFallbackLocation()
            }

        } catch (e: Exception) {
            // Network error - use cached location
            getFallbackLocation()
        }
    }

    private suspend fun getFallbackLocation(): Result<LocationEntity> {
        val cachedLocations = locationDao.getRandomLocations(1)

        return if (cachedLocations.isNotEmpty()) {
            Result.success(cachedLocations.first())
        } else {
            // Last resort - create a default location
            val defaultLocation = LocationEntity(
                id = UUID.randomUUID().toString(),
                latitude = 40.7589, // Times Square, NYC
                longitude = -73.9851,
                imageUrl = "",
                country = "United States",
                city = "New York",
                difficulty = 3,
                isCached = true
            )
            locationDao.insertLocation(defaultLocation)
            Result.success(defaultLocation)
        }
    }

    suspend fun cacheLocationForOffline(location: LocationEntity) {
        val cachedLocation = location.copy(isCached = true)
        locationDao.insertLocation(cachedLocation)
    }

    suspend fun preloadLocations(locations: List<LocationEntity>) {
        val cachedLocations = locations.map { it.copy(isCached = true) }
        locationDao.insertLocations(cachedLocations)
    }

    suspend fun getCachedLocationCount(): Int = locationDao.getCachedLocationCount()

    suspend fun getLocationById(locationId: String): LocationEntity? = locationDao.getLocationById(locationId)

    suspend fun clearNonCachedLocations() {
        locationDao.deleteNonCachedLocations()
    }
}