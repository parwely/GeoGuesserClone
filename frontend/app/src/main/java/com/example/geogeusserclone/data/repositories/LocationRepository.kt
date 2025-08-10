package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.MapillaryApiService
import com.example.geogeusserclone.data.network.MapillaryImageDetails
import com.example.geogeusserclone.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
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
                    // Korrekte Verarbeitung der verschachtelten URL-Struktur
                    val urlsData = streetViewResponse.data.streetViewUrls
                    if (urlsData is Map<*, *>) {
                        val url = (urlsData["mobile"] as? String)
                            ?: (urlsData["tablet"] as? String)
                            ?: (urlsData["desktop"] as? String)

                        if (!url.isNullOrEmpty()) {
                            return Result.success(url)
                        }
                    }
                    // Fallback auf die einzelne URL, falls das Objekt-Parsing fehlschlägt
                    val singleUrl = streetViewResponse.data.streetViewUrl
                    if (!singleUrl.isNullOrEmpty()) {
                        Result.success(singleUrl)
                    } else {
                        Result.failure(Exception("Keine Street View URL in der Antwort gefunden"))
                    }
                } else {
                    Result.failure(Exception("Street View Anfrage vom Backend als fehlgeschlagen markiert"))
                }
            } else {
                Result.failure(Exception("Street View API Fehler: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getLocationFromMapillary(): Result<LocationEntity> {
        return try {
            println("LocationRepository: Versuche Mapillary-Fallback...")
            
            // Zufällige bekannte Städte für Mapillary-Suche
            val searchCities = listOf(
                Pair(48.8566, 2.3522),   // Paris
                Pair(51.5074, -0.1278),  // London  
                Pair(40.7128, -74.0060), // New York
                Pair(52.5200, 13.4050),  // Berlin
                Pair(41.9028, 12.4964),  // Rom
                Pair(35.6762, 139.6503)  // Tokyo
            )
            
            val randomCity = searchCities.random()
            val (lat, lng) = randomCity
            
            // Erstelle BoundingBox um die Stadt (ca. 10km Radius)
            val latOffset = 0.1 // ca. 11km
            val lngOffset = 0.1
            val bbox = "${lng - lngOffset},${lat - latOffset},${lng + lngOffset},${lat + latOffset}"
            
            // Versuche Mapillary API-Call
            val response = withTimeoutOrNull(8000L) {
                mapillaryApiService.getImagesNearby(
                    bbox = bbox,
                    isPano = true, // Nur 360° Panoramen
                    limit = 5,
                    accessToken = Constants.MAPILLARY_ACCESS_TOKEN
                )
            }
            
            if (response?.isSuccessful == true) {
                val mapillaryResponse = response.body()!!
                if (mapillaryResponse.data.isNotEmpty()) {
                    val mapillaryImage = mapillaryResponse.data.random()
                    val coords = mapillaryImage.geometry.coordinates
                    
                    // Hole detailed Image info für Download-URL
                    val imageDetails = getMapillaryImageDetails(mapillaryImage.id)
                    val imageUrl = imageDetails.getOrNull()?.thumb_1024_url 
                        ?: mapillaryImage.thumb_1024_url 
                        ?: mapillaryImage.thumb_256_url
                        ?: ""
                    
                    if (imageUrl.isNotEmpty()) {
                        val locationEntity = LocationEntity(
                            id = "mapillary_${mapillaryImage.id}",
                            latitude = coords[1], // Mapillary: [lng, lat]
                            longitude = coords[0],
                            imageUrl = imageUrl,
                            country = getCityName(coords[1], coords[0]).first,
                            city = getCityName(coords[1], coords[0]).second,
                            difficulty = 3, // Mapillary-Locations sind meist schwieriger
                            isCached = true,
                            isUsed = false
                        )
                        
                        locationDao.insertLocation(locationEntity)
                        locationDao.markLocationAsUsed(locationEntity.id)
                        
                        println("LocationRepository: Mapillary-Location erfolgreich geladen: ${locationEntity.city}")
                        return Result.success(locationEntity)
                    }
                }
            }
            
            println("LocationRepository: Mapillary-API nicht verfügbar, verwende statische Fallbacks")
            // Fallback auf statische Locations
            val fallbackLocations = createFallbackLocations()
            if (fallbackLocations.isNotEmpty()) {
                locationDao.insertLocations(fallbackLocations)
                val randomLocation = fallbackLocations.random()
                locationDao.markLocationAsUsed(randomLocation.id)
                println("LocationRepository: Statischer Fallback verwendet: ${randomLocation.city}")
                Result.success(randomLocation)
            } else {
                Result.failure(Exception("Keine Fallback-Locations verfügbar"))
            }
        } catch (e: Exception) {
            println("LocationRepository: Mapillary-Fehler: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun getMapillaryImageDetails(imageId: String): Result<MapillaryImageDetails> {
        return try {
            val response = mapillaryApiService.getImageDetails(
                imageId = imageId,
                accessToken = Constants.MAPILLARY_ACCESS_TOKEN
            )
            
            if (response.isSuccessful) {
                val details = response.body()!!
                Result.success(details)
            } else {
                Result.failure(Exception("Mapillary Image Details API Fehler: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getCityName(lat: Double, lng: Double): Pair<String, String> {
        // Einfache Zuordnung basierend auf Koordinaten
        return when {
            lat in 48.0..49.0 && lng in 2.0..3.0 -> Pair("France", "Paris")
            lat in 51.0..52.0 && lng in -1.0..0.0 -> Pair("United Kingdom", "London")
            lat in 40.0..41.0 && lng in -75.0..-73.0 -> Pair("United States", "New York")
            lat in 52.0..53.0 && lng in 13.0..14.0 -> Pair("Germany", "Berlin")
            lat in 41.0..42.0 && lng in 12.0..13.0 -> Pair("Italy", "Rome")
            lat in 35.0..36.0 && lng in 139.0..140.0 -> Pair("Japan", "Tokyo")
            else -> Pair("Unknown", "Unknown City")
        }
    }

    suspend fun preloadLocations() {
        withContext(Dispatchers.IO) { // Wechsle zum IO-Dispatcher für Netzwerkoperationen
            try {
                val response = withTimeoutOrNull(Constants.BACKEND_FALLBACK_DELAY_MS) {
                    apiService.getRandomLocations(count = 10, difficulty = 2)
                }

                if (response?.isSuccessful == true) {
                    val locationsResponse = response.body()!!
                    if (locationsResponse.success) {
                        // Parallele Ausführung der StreetView-Anfragen
                        val locationEntities = locationsResponse.data.locations.map { backendLocation ->
                            async {
                                val streetViewUrl = getStreetViewForLocation(backendLocation.id).getOrNull()
                                    ?: backendLocation.imageUrls.firstOrNull()
                                    ?: ""

                                if (streetViewUrl.isNotBlank()) {
                                    LocationEntity(
                                        id = backendLocation.id.toString(),
                                        latitude = backendLocation.coordinates.latitude,
                                        longitude = backendLocation.coordinates.longitude,
                                        imageUrl = streetViewUrl,
                                        country = backendLocation.country,
                                        city = backendLocation.city,
                                        difficulty = backendLocation.difficulty,
                                        isCached = true,
                                        isUsed = false
                                    )
                                } else {
                                    null // Null zurückgeben, wenn keine URL gefunden wurde
                                }
                            }
                        }.mapNotNull { it.await() } // Auf alle warten und Null-Werte entfernen

                        locationDao.insertLocations(locationEntities)
                        return@withContext
                    }
                }

                // Fallback: Lade Standard-Locations, wenn das Backend nicht verfügbar ist
                val fallbackLocations = createFallbackLocations()
                locationDao.insertLocations(fallbackLocations)

            } catch (e: Exception) {
                // Stiller Fallback bei Fehlern
                val fallbackLocations = createFallbackLocations()
                locationDao.insertLocations(fallbackLocations)
            }
        }
    }

    private suspend fun createFallbackLocationsWithMapillary(): List<LocationEntity> {
        // Vereinfachte Mapillary-Integration als Fallback
        return createFallbackLocations() // Momentan werden die statischen Fallbacks verwendet
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
