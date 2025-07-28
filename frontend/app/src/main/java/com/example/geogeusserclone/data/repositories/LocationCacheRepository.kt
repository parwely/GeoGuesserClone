package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationCacheRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao
) {

    suspend fun preloadLocationsInBackground() = withContext(Dispatchers.IO) {
        try {
            // Lade nur wenn weniger als 10 Locations im Cache
            val cachedCount = locationDao.getCachedLocationCount()
            if (cachedCount < 10) {
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

                    // Batch insert für bessere Performance
                    locationDao.insertLocations(locationEntities)

                    // Preload images im Hintergrund
                    preloadImages(locationEntities.take(5)) // Nur erste 5 Bilder
                }
            }
        } catch (e: Exception) {
            // Silent fail - App funktioniert weiter mit Fallback Locations
        }
    }

    private suspend fun preloadImages(locations: List<LocationEntity>) = withContext(Dispatchers.IO) {
        // Implementierung für Image Preloading
        // Dies würde die Bilder in den Cache laden ohne sie anzuzeigen
    }

    suspend fun getNextLocations(count: Int = 3): List<LocationEntity> = withContext(Dispatchers.IO) {
        locationDao.getUnusedLocations(count)
    }
}
