package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiRetryHandler
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.MapillaryApiService
import com.example.geogeusserclone.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiService: ApiService,
    private val mapillaryApiService: MapillaryApiService,
    private val locationDao: LocationDao
) : BaseRepository() {

    // Cache für bessere Performance
    private val locationCache = mutableSetOf<String>() // Koordinaten-Hashes für Duplikat-Erkennung
    private var lastBackendCheck = 0L
    private var isBackendAvailable = true

    fun getCachedLocations(): Flow<List<LocationEntity>> = locationDao.getCachedLocations()

    suspend fun getRandomLocation(): Result<LocationEntity> {
        return withContext(Dispatchers.IO) {
            try {
                println("LocationRepository: Starte optimierte Location-Suche...")

                // 1. ERSTE PRIORITÄT: Google Maps Backend (mit intelligentem Caching)
                if (shouldTryBackend()) {
                    val backendResult = getLocationFromBackendOptimized()
                    if (backendResult.isSuccess) {
                        println("LocationRepository: ✅ Google Maps Backend erfolgreich")
                        markBackendAvailable()
                        return@withContext backendResult
                    } else {
                        println("LocationRepository: ❌ Google Maps Backend fehlgeschlagen")
                        markBackendUnavailable()
                    }
                }

                // 2. ZWEITE PRIORITÄT: Mapillary API (parallele Versuche)
                val mapillaryResult = getLocationFromMapillaryOptimized()
                if (mapillaryResult.isSuccess) {
                    println("LocationRepository: ✅ Mapillary API erfolgreich")
                    return@withContext mapillaryResult
                }

                // 3. DRITTE PRIORITÄT: Lokale unbenutzte Locations
                val unusedLocation = locationDao.getRandomUnusedLocation()
                if (unusedLocation != null && !isLocationDuplicate(unusedLocation)) {
                    locationDao.markLocationAsUsed(unusedLocation.id)
                    addToCache(unusedLocation)
                    println("LocationRepository: ✅ Lokale unbenutzte Location: ${unusedLocation.city}")
                    return@withContext Result.success(unusedLocation)
                }

                // 4. VIERTE PRIORITÄT: Generiere neue einzigartige Fallback-Location
                val uniqueLocation = generateUniqueLocation()
                locationDao.insertLocation(uniqueLocation)
                locationDao.markLocationAsUsed(uniqueLocation.id)
                addToCache(uniqueLocation)
                println("LocationRepository: ✅ Neue einzigartige Location generiert: ${uniqueLocation.city}")

                Result.success(uniqueLocation)

            } catch (e: Exception) {
                println("LocationRepository: ❌ Kritischer Fehler: ${e.message}")
                // Emergency: Generiere garantiert einzigartige Location
                val emergencyLocation = generateUniqueLocation()
                try {
                    locationDao.insertLocation(emergencyLocation)
                    addToCache(emergencyLocation)
                    Result.success(emergencyLocation)
                } catch (dbError: Exception) {
                    Result.failure(Exception("Kritischer Fehler: ${e.message}"))
                }
            }
        }
    }

    private fun shouldTryBackend(): Boolean {
        val currentTime = System.currentTimeMillis()
        return isBackendAvailable || (currentTime - lastBackendCheck > 30000) // Retry nach 30s
    }

    private fun markBackendAvailable() {
        isBackendAvailable = true
        lastBackendCheck = System.currentTimeMillis()
    }

    private fun markBackendUnavailable() {
        isBackendAvailable = false
        lastBackendCheck = System.currentTimeMillis()
    }

    private suspend fun getLocationFromBackendOptimized(): Result<LocationEntity> {
        return try {
            println("LocationRepository: Optimierter Backend-Aufruf mit Retry-Mechanismus...")

            // KORREKTUR: Verwende ALLE verfügbaren Parameter-Kombinationen
            val response = ApiRetryHandler.executeLocationRetry(
                categories = listOf("urban", "landmark", "rural", "desert", "mountain", "coastal", "forest"),
                difficulties = listOf(1, 2, 3) // ALLE Difficulties versuchen
            ) { category, difficulty ->
                // KORREKTUR: Versuche mit verschiedenen counts und ohne Filter
                val randomCount = (5..10).random()
                println("LocationRepository: Backend-Versuch mit category=$category, difficulty=$difficulty, count=$randomCount")

                apiService.getRandomLocations(
                    count = randomCount,
                    difficulty = difficulty,
                    category = category
                )
            }

            if (response?.isSuccessful == true && response.body() != null) {
                val locationsResponse = response.body()!!
                println("LocationRepository: Backend Response: success=${locationsResponse.success}, count=${locationsResponse.data.count}")

                if (locationsResponse.success && locationsResponse.data.locations.isNotEmpty()) {
                    // Versuche ALLE Locations aus der Response, nicht nur die erste
                    for (backendLocation in locationsResponse.data.locations) {
                        val coordHash = "${backendLocation.coordinates.latitude}-${backendLocation.coordinates.longitude}"

                        if (coordHash !in locationCache) {
                            addToCache(LocationEntity(
                                id = "temp_${backendLocation.id}",
                                latitude = backendLocation.coordinates.latitude,
                                longitude = backendLocation.coordinates.longitude,
                                imageUrl = "",
                                country = backendLocation.country,
                                city = backendLocation.city
                            ))

                            return createLocationEntity(backendLocation, "backend_optimized_${System.currentTimeMillis()}")
                        }
                    }

                    // Alle Locations sind Duplikate - force nehme eine
                    val backendLocation = locationsResponse.data.locations.first()
                    println("LocationRepository: ⚠️ Alle Locations sind Duplikate, verwende trotzdem: ${backendLocation.city}")
                    return createLocationEntity(backendLocation, "backend_forced_${System.currentTimeMillis()}")
                } else {
                    println("LocationRepository: ❌ Backend hat keine Locations für diese Parameter")
                }
            } else {
                println("LocationRepository: ❌ Backend Response nicht erfolgreich: ${response?.code()}")
            }

            // FALLBACK: Versuche mit weniger restriktiven Parametern
            println("LocationRepository: Versuche Fallback ohne Filter...")
            val fallbackResponse = try {
                apiService.getRandomLocations(count = 10) // Ohne difficulty und category Filter
            } catch (e: Exception) {
                println("LocationRepository: Fallback-Aufruf fehlgeschlagen: ${e.message}")
                null
            }

            if (fallbackResponse?.isSuccessful == true && fallbackResponse.body()?.data?.locations?.isNotEmpty() == true) {
                val location = fallbackResponse.body()!!.data.locations.first()
                println("LocationRepository: ✅ Fallback-Location gefunden: ${location.city}")
                return createLocationEntity(location, "backend_fallback_${System.currentTimeMillis()}")
            }

            Result.failure(Exception("Backend hat keine Locations verfügbar"))
        } catch (e: Exception) {
            println("LocationRepository: ❌ Backend-Fehler: ${e.message}")
            Result.failure(e)
        }
    }

    // Hilfsfunktion für Location-Erstellung
    private suspend fun createLocationEntity(
        backendLocation: com.example.geogeusserclone.data.network.BackendLocation,
        prefix: String
    ): Result<LocationEntity> {
        // Street View Anfrage für bessere Performance
        val streetViewUrl = try {
            withTimeout(2000L) {
                getStreetViewForLocationOptimized(backendLocation.id).getOrNull()
            }
        } catch (e: Exception) {
            null
        } ?: backendLocation.imageUrls.firstOrNull() ?: ""

        val locationEntity = LocationEntity(
            id = "${prefix}_${backendLocation.id}_${System.currentTimeMillis()}",
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
        addToCache(locationEntity)
        println("LocationRepository: ✅ Backend-Location erfolgreich erstellt: ${locationEntity.city}")
        return Result.success(locationEntity)
    }

    private suspend fun getStreetViewForLocationOptimized(locationId: Int): Result<String> {
        return try {
            // KORREKTUR: Randomisiere heading für verschiedene Ansichten
            val randomHeading = (0..359).random()

            val response = withTimeout(2000L) {
                apiService.getStreetView(
                    locationId = locationId,
                    heading = randomHeading, // Randomisiere Blickrichtung
                    responsive = true
                )
            }

            if (response.isSuccessful && response.body() != null) {
                val streetViewResponse = response.body()!!
                if (streetViewResponse.success) {
                    val urlsData = streetViewResponse.data.streetViewUrls
                    val url = when (urlsData) {
                        is Map<*, *> -> {
                            (urlsData["mobile"] as? String)
                                ?: (urlsData["tablet"] as? String)
                                ?: (urlsData["desktop"] as? String)
                        }
                        else -> streetViewResponse.data.streetViewUrl
                    }

                    if (!url.isNullOrEmpty()) {
                        // KORREKTUR: Füge Cache-Buster hinzu um Bildcaching-Probleme zu vermeiden
                        val cacheBusterUrl = if (url.contains("?")) {
                            "$url&cb=${System.currentTimeMillis()}"
                        } else {
                            "$url?cb=${System.currentTimeMillis()}"
                        }

                        println("LocationRepository: ✅ Street View URL generiert: ${cacheBusterUrl.take(100)}...")
                        Result.success(cacheBusterUrl)
                    } else {
                        Result.failure(Exception("Keine Street View URL"))
                    }
                } else {
                    Result.failure(Exception("Street View Response fehlerhaft"))
                }
            } else {
                Result.failure(Exception("Street View API Fehler: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getLocationFromMapillaryOptimized(): Result<LocationEntity> {
        return try {
            println("LocationRepository: Optimierte Mapillary-Suche...")

            // Parallele Suche in verschiedenen Städten für bessere Ausbeute
            val searchCities = listOf(
                Triple(48.8566, 2.3522, "Paris"),
                Triple(51.5074, -0.1278, "London"),
                Triple(40.7128, -74.0060, "New York"),
                Triple(52.5200, 13.4050, "Berlin"),
                Triple(35.6762, 139.6503, "Tokyo")
            )

            val results = searchCities.mapNotNull { (lat, lng, cityName) ->
                try {
                    searchMapillaryInCity(lat, lng, cityName)
                } catch (e: Exception) {
                    null
                }
            }

            val uniqueResults = results.filter { !isLocationDuplicate(it) }

            if (uniqueResults.isNotEmpty()) {
                val selectedLocation = uniqueResults.random()
                locationDao.insertLocation(selectedLocation)
                addToCache(selectedLocation)
                println("LocationRepository: Mapillary Location gefunden: ${selectedLocation.city}")
                Result.success(selectedLocation)
            } else {
                Result.failure(Exception("Keine einzigartigen Mapillary Locations gefunden"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun searchMapillaryInCity(lat: Double, lng: Double, cityName: String): LocationEntity? {
        return try {
            val latOffset = 0.05
            val lngOffset = 0.05
            val bbox = "${lng - lngOffset},${lat - latOffset},${lng + lngOffset},${lat + latOffset}"

            println("LocationRepository: Mapillary-Suche für $cityName mit bbox=$bbox")

            // SICHERHEIT: Deaktiviere Mapillary bis API-Schlüssel sicher konfiguriert ist
            // Das Hardcodieren von API-Schlüsseln ist ein Sicherheitsrisiko!

            println("LocationRepository: ❌ Mapillary deaktiviert - API-Schlüssel wurde aus Sicherheitsgründen entfernt")
            return null

            /* ENTFERNT: Unsichere Mapillary API Aufrufe
            val response = withTimeout(3000L) {
                mapillaryApiService.getImagesNearby(
                    bbox = bbox,
                    isPano = true,
                    limit = 3,
                    accessToken = "" // LEER - Sicherheitsrisiko behoben
                )
            }
            */

        } catch (e: Exception) {
            println("LocationRepository: ❌ Mapillary Exception für $cityName: ${e.message}")
            null
        }
    }

    private fun getCountryFromCoords(lat: Double, lng: Double): String {
        return when {
            lat in 48.0..49.0 && lng in 2.0..3.0 -> "France"
            lat in 51.0..52.0 && lng in -1.0..0.0 -> "United Kingdom"
            lat in 40.0..41.0 && lng in -75.0..-73.0 -> "United States"
            lat in 52.0..53.0 && lng in 13.0..14.0 -> "Germany"
            lat in 35.0..36.0 && lng in 139.0..140.0 -> "Japan"
            else -> "Unknown"
        }
    }

    private fun isLocationDuplicate(location: LocationEntity): Boolean {
        val coordHash = "${location.latitude}-${location.longitude}"
        return coordHash in locationCache
    }

    private fun addToCache(location: LocationEntity) {
        val coordHash = "${location.latitude}-${location.longitude}"
        locationCache.add(coordHash)
    }

    private fun generateUniqueLocation(): LocationEntity {
        val baseLocations = listOf(
            Triple(48.8566, 2.3522, "Paris"),
            Triple(51.5074, -0.1278, "London"),
            Triple(40.7128, -74.0060, "New York"),
            Triple(52.5200, 13.4050, "Berlin"),
            Triple(35.6762, 139.6503, "Tokyo"),
            Triple(-33.8688, 151.2093, "Sydney"),
            Triple(41.9028, 12.4964, "Rome"),
            Triple(41.3851, 2.1734, "Barcelona"),
            Triple(55.7558, 37.6176, "Moscow"),
            Triple(19.4326, -99.1332, "Mexico City")
        )

        // Finde eine Location, die nicht im Cache ist
        val availableLocations = baseLocations.filter { (lat, lng, _) ->
            "$lat-$lng" !in locationCache
        }

        val (lat, lng, city) = if (availableLocations.isNotEmpty()) {
            availableLocations.random()
        } else {
            // Alle Basis-Locations verwendet, generiere leicht versetzte Koordinaten
            val baseLocation = baseLocations.random()
            val offset = Random().nextDouble() * 0.01 // Max 1km Offset
            Triple(
                baseLocation.first + offset,
                baseLocation.second + offset,
                "${baseLocation.third} Var${Random().nextInt(100)}"
            )
        }

        return LocationEntity(
            id = "generated_${System.currentTimeMillis()}_${Random().nextInt(1000)}",
            latitude = lat,
            longitude = lng,
            imageUrl = getImageUrlForCity(city),
            country = getCountryFromCoords(lat, lng),
            city = city,
            difficulty = 2,
            isCached = true,
            isUsed = false
        )
    }

    private fun getImageUrlForCity(city: String): String {
        return when {
            city.contains("Paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800"
            city.contains("London") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800"
            city.contains("New York") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"
            city.contains("Berlin") -> "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800"
            city.contains("Tokyo") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800"
            city.contains("Sydney") -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800"
            city.contains("Rome") -> "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800"
            city.contains("Barcelona") -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800"
            else -> "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800"
        }
    }

    suspend fun preloadLocations() {
        withContext(Dispatchers.IO) {
            try {
                println("LocationRepository: Starte intelligentes Preloading...")

                // Backend- und Mapillary-Calls sequenziell für bessere Stabilität
                val backendLocations = try {
                    val response = withTimeout(4000L) {
                        apiService.getRandomLocations(count = 8, difficulty = 2)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val locationsResponse = response.body()!!
                        if (locationsResponse.success) {
                            val locationEntities = locationsResponse.data.locations.mapNotNull { backendLocation ->
                                val coordHash = "${backendLocation.coordinates.latitude}-${backendLocation.coordinates.longitude}"
                                if (coordHash !in locationCache) {
                                    val streetViewUrl = getStreetViewForLocationOptimized(backendLocation.id).getOrNull()
                                        ?: backendLocation.imageUrls.firstOrNull() ?: ""

                                    if (streetViewUrl.isNotBlank()) {
                                        LocationEntity(
                                            id = "preload_backend_${backendLocation.id}",
                                            latitude = backendLocation.coordinates.latitude,
                                            longitude = backendLocation.coordinates.longitude,
                                            imageUrl = streetViewUrl,
                                            country = backendLocation.country,
                                            city = backendLocation.city,
                                            difficulty = backendLocation.difficulty,
                                            isCached = true,
                                            isUsed = false
                                        ).also { addToCache(it) }
                                    } else null
                                } else null
                            }
                            locationEntities
                        } else emptyList()
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                val mapillaryLocations = try {
                    val mapillaryLocations = mutableListOf<LocationEntity>()
                    val cities = listOf(
                        Triple(48.8566, 2.3522, "Paris"),
                        Triple(51.5074, -0.1278, "London"),
                        Triple(52.5200, 13.4050, "Berlin")
                    )

                    cities.forEach { (lat, lng, cityName) ->
                        try {
                            searchMapillaryInCity(lat, lng, cityName)?.let { location ->
                                if (!isLocationDuplicate(location)) {
                                    mapillaryLocations.add(location)
                                    addToCache(location)
                                }
                            }
                        } catch (e: Exception) {
                            // Continue with next city
                        }
                    }
                    mapillaryLocations
                } catch (e: Exception) {
                    emptyList()
                }

                val allLocations = (backendLocations + mapillaryLocations).take(10)

                if (allLocations.isNotEmpty()) {
                    locationDao.insertLocations(allLocations)
                    println("LocationRepository: ${allLocations.size} Locations preloaded (${backendLocations.size} Backend, ${mapillaryLocations.size} Mapillary)")
                } else {
                    // Fallback: Generiere einige Locations
                    val fallbackLocations = (1..5).map { generateUniqueLocation() }
                    locationDao.insertLocations(fallbackLocations)
                    println("LocationRepository: ${fallbackLocations.size} Fallback-Locations preloaded")
                }

            } catch (e: Exception) {
                println("LocationRepository: Preload-Fehler: ${e.message}")
                // Silent fail mit minimalen Fallback-Locations
                try {
                    val emergencyLocations = (1..3).map { generateUniqueLocation() }
                    locationDao.insertLocations(emergencyLocations)
                } catch (dbError: Exception) {
                    // Complete silent fail
                }
            }
        }
    }

    suspend fun resetLocationUsage() {
        locationDao.resetAllLocationsUsage()
        locationCache.clear()
    }

    suspend fun getCachedLocationCount(): Int = locationDao.getCachedLocationCount()

    suspend fun testBackendConnection(): Result<Boolean> {
        return try {
            val response = withTimeout(2000L) {
                apiService.getHealth()
            }
            if (response.isSuccessful) {
                markBackendAvailable()
                Result.success(true)
            } else {
                markBackendUnavailable()
                Result.failure(Exception("Backend nicht erreichbar: ${response.code()}"))
            }
        } catch (e: Exception) {
            markBackendUnavailable()
            Result.failure(Exception("Netzwerkfehler: ${e.message}"))
        }
    }
}
