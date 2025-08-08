package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.MapillaryApiService
import com.example.geogeusserclone.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreetViewLocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val mapillaryApiService: MapillaryApiService,
    private val locationDao: LocationDao
) : BaseRepository() {

    suspend fun getRandomLocationWithStreetView(): Result<LocationEntity> {
        return try {
            // 1. Versuche zuerst lokale unbenutzte Location
            val unusedLocation = locationDao.getRandomUnusedLocation()
            if (unusedLocation != null) {
                locationDao.markLocationAsUsed(unusedLocation.id)
                return Result.success(unusedLocation)
            }

            // 2. Erstelle Fallback-Locations mit echten 360° StreetView-Bildern
            val streetViewLocations = createStreetViewFallbackLocations()
            locationDao.insertLocations(streetViewLocations)

            val randomLocation = streetViewLocations.random()
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

    private suspend fun createStreetViewFallbackLocations(): List<LocationEntity> {
        val fallbackLocations = mutableListOf<LocationEntity>()

        // Versuche echte Mapillary-Bilder für beliebte Städte zu holen
        val popularCities = listOf(
            Triple(48.8566, 2.3522, "Paris"),      // Paris
            Triple(51.5074, -0.1278, "London"),   // London
            Triple(40.7128, -74.0060, "New York"), // New York
            Triple(35.6762, 139.6503, "Tokyo"),   // Tokyo
            Triple(-33.8688, 151.2093, "Sydney"), // Sydney
            Triple(52.5200, 13.4050, "Berlin"),   // Berlin
            Triple(41.9028, 12.4964, "Rome"),     // Rome
            Triple(41.3851, 2.1734, "Barcelona")  // Barcelona
        )

        popularCities.forEachIndexed { index, (lat, lng, cityName) ->
            try {
                val streetViewImage = fetchMapillaryImageForLocation(lat, lng)
                if (streetViewImage != null) {
                    fallbackLocations.add(
                        LocationEntity(
                            id = "streetview_${cityName.lowercase()}_$index",
                            latitude = streetViewImage.geometry.coordinates[1], // Mapillary coords
                            longitude = streetViewImage.geometry.coordinates[0],
                            imageUrl = streetViewImage.thumb_2048_url ?: streetViewImage.thumb_1024_url ?: "",
                            country = getCountryFromCity(cityName),
                            city = cityName,
                            difficulty = 2,
                            isCached = true,
                            isUsed = false
                        )
                    )
                } else {
                    // Fallback auf hochwertige Unsplash-Bilder
                    fallbackLocations.add(createHighQualityFallbackLocation(lat, lng, cityName, index))
                }
            } catch (e: Exception) {
                // Fallback auf statische Bilder
                fallbackLocations.add(createHighQualityFallbackLocation(lat, lng, cityName, index))
            }
        }

        return fallbackLocations
    }

    private suspend fun fetchMapillaryImageForLocation(lat: Double, lng: Double): com.example.geogeusserclone.data.network.MapillaryImage? {
        return try {
            // Erstelle Bounding Box (ca. 1km Radius)
            val offset = 0.01 // Ungefähr 1km
            val bbox = "${lng - offset},${lat - offset},${lng + offset},${lat + offset}"

            val response = withTimeoutOrNull(3000) {
                mapillaryApiService.getImagesNearby(
                    bbox = bbox,
                    isPano = true,
                    limit = 5,
                    accessToken = Constants.MAPILLARY_ACCESS_TOKEN
                )
            }

            if (response?.isSuccessful == true) {
                response.body()?.data?.firstOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun createHighQualityFallbackLocation(lat: Double, lng: Double, cityName: String, index: Int): LocationEntity {
        val imageUrls = mapOf(
            "Paris" to "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=1200&h=800&fit=crop",
            "London" to "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=1200&h=800&fit=crop",
            "New York" to "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=1200&h=800&fit=crop",
            "Tokyo" to "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=1200&h=800&fit=crop",
            "Sydney" to "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&h=800&fit=crop",
            "Berlin" to "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=1200&h=800&fit=crop",
            "Rome" to "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&h=800&fit=crop",
            "Barcelona" to "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=1200&h=800&fit=crop"
        )

        return LocationEntity(
            id = "fallback_${cityName.lowercase()}_$index",
            latitude = lat,
            longitude = lng,
            imageUrl = imageUrls[cityName] ?: "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=1200&h=800&fit=crop",
            country = getCountryFromCity(cityName),
            city = cityName,
            difficulty = 2,
            isCached = true,
            isUsed = false
        )
    }

    private fun createEmergencyLocation(): LocationEntity {
        return LocationEntity(
            id = "emergency_paris",
            latitude = 48.8566,
            longitude = 2.3522,
            imageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=1200&h=800&fit=crop",
            country = "France",
            city = "Paris",
            difficulty = 2,
            isCached = true,
            isUsed = false
        )
    }

    private fun getCountryFromCity(cityName: String): String {
        return when (cityName) {
            "Paris" -> "France"
            "London" -> "United Kingdom"
            "New York" -> "United States"
            "Tokyo" -> "Japan"
            "Sydney" -> "Australia"
            "Berlin" -> "Germany"
            "Rome" -> "Italy"
            "Barcelona" -> "Spain"
            else -> "Unknown"
        }
    }

    /**
     * Preloade Locations mit echten StreetView-Bildern
     */
    suspend fun preloadStreetViewLocations() {
        try {
            val streetViewLocations = createStreetViewFallbackLocations()
            locationDao.insertLocations(streetViewLocations)
        } catch (e: Exception) {
            // Fallback auf normale Locations
            val normalFallbacks = createStreetViewFallbackLocations()
            locationDao.insertLocations(normalFallbacks)
        }
    }
}
