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

            // Falls keine unbenutzte Location vorhanden, versuche online
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
                // Fallback zu einer zufälligen lokalen Location
                val randomLocation = locationDao.getRandomLocations(1).firstOrNull()
                if (randomLocation != null) {
                    Result.success(randomLocation)
                } else {
                    Result.failure(Exception("Keine Locations verfügbar"))
                }
            }
        } catch (e: Exception) {
            // Offline Fallback
            val randomLocation = locationDao.getRandomLocations(1).firstOrNull()
            if (randomLocation != null) {
                Result.success(randomLocation)
            } else {
                Result.failure(e)
            }
        }
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