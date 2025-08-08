package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.MapillaryApiService
import com.example.geogeusserclone.data.network.NetworkResult
import com.example.geogeusserclone.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val mapillaryApiService: MapillaryApiService,
    private val locationDao: LocationDao
) : BaseRepository() {

    fun getCachedLocations(): Flow<List<LocationEntity>> = locationDao.getCachedLocations()

    suspend fun getRandomLocation(): Result<LocationEntity> {
        return try {
            // 1. Versuche zuerst lokale unbenutzte Location
            val unusedLocation = locationDao.getRandomUnusedLocation()
            if (unusedLocation != null) {
                locationDao.markLocationAsUsed(unusedLocation.id)
                return Result.success(unusedLocation)
            }

            // 2. Versuche dein Google Maps Backend
            val backendResult = getLocationFromBackend()
            if (backendResult.isSuccess) {
                return backendResult
            }

            // 3. Fallback auf Mapillary (nur wenn Backend nicht verfügbar)
            val mapillaryResult = getLocationFromMapillary()
            if (mapillaryResult.isSuccess) {
                return mapillaryResult
            }

            // 4. Ultimate Fallback: Erstelle Offline-Locations
            val fallbackLocations = createFallbackLocations()
            locationDao.insertLocations(fallbackLocations)
            val randomLocation = fallbackLocations.random()
            locationDao.markLocationAsUsed(randomLocation.id)

            return Result.success(randomLocation)

        } catch (e: Exception) {
            // Emergency Fallback
            val emergencyLocation = createEmergencyLocation()
            try {
                locationDao.insertLocation(emergencyLocation)
                locationDao.markLocationAsUsed(emergencyLocation.id)
                Result.success(emergencyLocation)
            } catch (dbError: Exception) {
                Result.failure(Exception("Kritischer Fehler: Kann keine Location laden"))
            }
        }
    }

    private suspend fun getLocationFromBackend(): Result<LocationEntity> {
        return try {
            val response = withTimeoutOrNull(Constants.BACKEND_FALLBACK_DELAY_MS) {
                apiService.getRandomLocations(count = 1, difficulty = 2, category = "urban")
            }

            if (response?.isSuccessful == true) {
                val locationsResponse = response.body()!!
                if (locationsResponse.success && locationsResponse.data.locations.isNotEmpty()) {
                    val backendLocation = locationsResponse.data.locations.first()

                    // Hole Street View URL für diese Location
                    val streetViewResult = getStreetViewForLocation(backendLocation.id)
                    val streetViewUrl = streetViewResult.getOrNull() ?: backendLocation.imageUrls.firstOrNull() ?: ""

                    val locationEntity = LocationEntity(
                        id = backendLocation.id.toString(), // Convert Int to String
                        latitude = backendLocation.coordinates.latitude,
                        longitude = backendLocation.coordinates.longitude,
                        imageUrl = streetViewUrl,
                        country = backendLocation.country,
                        city = backendLocation.city,
                        difficulty = backendLocation.difficulty,
                        isCached = true,
                        isUsed = false
                    )

                    locationDao.insertLocation(locationEntity)
                    locationDao.markLocationAsUsed(locationEntity.id)
                    return Result.success(locationEntity)
                }
            }
            Result.failure(Exception("Backend nicht verfügbar"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getStreetViewForLocation(locationId: Int): Result<String> {
        return try {
            val response = apiService.getStreetView(
                locationId = locationId,
                responsive = true
            )

            if (response.isSuccessful) {
                val streetViewResponse = response.body()!!
                if (streetViewResponse.success) {
                    val streetViewUrl = streetViewResponse.data.streetViewUrl
                    if (!streetViewUrl.isNullOrEmpty()) {
                        Result.success(streetViewUrl)
                    } else {
                        Result.failure(Exception("Keine Street View URL"))
                    }
                } else {
                    Result.failure(Exception("Street View Anfrage fehlgeschlagen"))
                }
            } else {
                Result.failure(Exception("Street View API Fehler"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getLocationFromMapillary(): Result<LocationEntity> {
        return try {
            // Mapillary Fallback Logic (vereinfacht)
            val fallbackLocations = createFallbackLocationsWithMapillary()
            if (fallbackLocations.isNotEmpty()) {
                locationDao.insertLocations(fallbackLocations)
                val randomLocation = fallbackLocations.random()
                locationDao.markLocationAsUsed(randomLocation.id)
                Result.success(randomLocation)
            } else {
                Result.failure(Exception("Mapillary nicht verfügbar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun preloadLocations() {
        try {
            // Preload von deinem Backend (Google Maps)
            val response = withTimeoutOrNull(Constants.BACKEND_FALLBACK_DELAY_MS) {
                apiService.getRandomLocations(count = 10, difficulty = 2)
            }

            if (response?.isSuccessful == true) {
                val locationsResponse = response.body()!!
                if (locationsResponse.success) {
                    val locationEntities = locationsResponse.data.locations.map { backendLocation ->
                        // Hole Street View URL für jede Location
                        val streetViewUrl = getStreetViewForLocation(backendLocation.id).getOrNull()
                            ?: backendLocation.imageUrls.firstOrNull()
                            ?: ""

                        LocationEntity(
                            id = backendLocation.id.toString(), // Convert Int to String
                            latitude = backendLocation.coordinates.latitude,
                            longitude = backendLocation.coordinates.longitude,
                            imageUrl = streetViewUrl,
                            country = backendLocation.country,
                            city = backendLocation.city,
                            difficulty = backendLocation.difficulty,
                            isCached = true,
                            isUsed = false
                        )
                    }
                    locationDao.insertLocations(locationEntities)
                    return
                }
            }

            // Fallback: Lade Standard-Locations falls Backend nicht verfügbar
            val fallbackLocations = createFallbackLocations()
            locationDao.insertLocations(fallbackLocations)

        } catch (e: Exception) {
            // Stille Fallback-Behandlung
            val fallbackLocations = createFallbackLocations()
            locationDao.insertLocations(fallbackLocations)
        }
    }

    private suspend fun createFallbackLocationsWithMapillary(): List<LocationEntity> {
        // Vereinfachte Mapillary Integration als Fallback
        return createFallbackLocations() // Für jetzt verwenden wir die statischen Fallbacks
    }

    private fun createFallbackLocations(): List<LocationEntity> {
        return listOf(
            LocationEntity(
                id = "fallback_paris",
                latitude = 48.8566,
                longitude = 2.3522,
                imageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800",
                country = "France",
                city = "Paris",
                difficulty = 2,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_london",
                latitude = 51.5074,
                longitude = -0.1278,
                imageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800",
                country = "United Kingdom",
                city = "London",
                difficulty = 2,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_newyork",
                latitude = 40.7128,
                longitude = -74.0060,
                imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800",
                country = "United States",
                city = "New York",
                difficulty = 3,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_tokyo",
                latitude = 35.6762,
                longitude = 139.6503,
                imageUrl = "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800",
                country = "Japan",
                city = "Tokyo",
                difficulty = 4,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_sydney",
                latitude = -33.8688,
                longitude = 151.2093,
                imageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800",
                country = "Australia",
                city = "Sydney",
                difficulty = 3,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_berlin",
                latitude = 52.5200,
                longitude = 13.4050,
                imageUrl = "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800",
                country = "Germany",
                city = "Berlin",
                difficulty = 2,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_rome",
                latitude = 41.9028,
                longitude = 12.4964,
                imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800",
                country = "Italy",
                city = "Rome",
                difficulty = 3,
                isCached = true,
                isUsed = false
            ),
            LocationEntity(
                id = "fallback_barcelona",
                latitude = 41.3851,
                longitude = 2.1734,
                imageUrl = "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800",
                country = "Spain",
                city = "Barcelona",
                difficulty = 3,
                isCached = true,
                isUsed = false
            )
        )
    }

    private fun createEmergencyLocation(): LocationEntity {
        return LocationEntity(
            id = "emergency_paris",
            latitude = 48.8566,
            longitude = 2.3522,
            imageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800",
            country = "France",
            city = "Paris",
            difficulty = 2,
            isCached = true,
            isUsed = false
        )
    }

    suspend fun resetLocationUsage() {
        locationDao.resetAllLocationsUsage()
    }

    suspend fun getCachedLocationCount(): Int = locationDao.getCachedLocationCount()

    suspend fun testBackendConnection(): Result<Boolean> {
        return try {
            val response = apiService.getHealth()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Backend nicht erreichbar: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Netzwerkfehler: ${e.message}"))
        }
    }
}
