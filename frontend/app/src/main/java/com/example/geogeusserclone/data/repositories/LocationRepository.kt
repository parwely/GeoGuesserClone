package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.ApiRetryHandler
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.MapillaryApiService
import com.example.geogeusserclone.data.network.InteractiveStreetViewResponse
import com.example.geogeusserclone.data.network.StreetViewNavigationResponse
import com.example.geogeusserclone.data.network.StreetViewNavigationRequest
import com.example.geogeusserclone.data.network.InteractiveStreetView
import com.example.geogeusserclone.data.network.StreetViewConfig
import com.example.geogeusserclone.utils.Constants
import com.example.geogeusserclone.utils.DistanceCalculator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

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
                println("LocationRepository: Backend Response: success=${locationsResponse.success}, count=${locationsResponse.count}")

                // KORRIGIERT: Verwende die korrigierte Response-Struktur
                if (locationsResponse.success && locationsResponse.data.isNotEmpty()) {
                    // Versuche ALLE Locations aus der Response, nicht nur die erste
                    for (backendLocation in locationsResponse.data) {
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
                    val backendLocation = locationsResponse.data.first()
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

            if (fallbackResponse?.isSuccessful == true && fallbackResponse.body()?.data?.isNotEmpty() == true) {
                val location = fallbackResponse.body()!!.data.first()
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
        // NEUE: Prüfe geografische Street View-Verfügbarkeit ZUERST
        val isLikelyAvailable = isLocationLikelyToHaveStreetView(
            backendLocation.coordinates.latitude,
            backendLocation.coordinates.longitude,
            backendLocation.city,
            backendLocation.country
        )

        if (!isLikelyAvailable) {
            println("LocationRepository: ⚠️ ${backendLocation.city} wahrscheinlich ohne Street View, verwende sofort Fallback")
            val fallbackUrl = getKnownLocationFallback(backendLocation)
                ?: getRegionalFallbackImage(backendLocation)

            val locationEntity = LocationEntity(
                id = "${prefix}_${backendLocation.id}_${System.currentTimeMillis()}",
                latitude = backendLocation.coordinates.latitude,
                longitude = backendLocation.coordinates.longitude,
                imageUrl = fallbackUrl,
                country = backendLocation.country,
                city = backendLocation.city,
                difficulty = backendLocation.difficulty,
                isCached = true,
                isUsed = false
            )

            println("LocationRepository: ✅ LocationEntity erstellt für ${locationEntity.city} mit URL-Typ: Smart Fallback")
            locationDao.insertLocation(locationEntity)
            addToCache(locationEntity)
            return Result.success(locationEntity)
        }

        // KORRIGIERT: Robuste Street View URL-Validierung mit Backend-Priorität
        val streetViewUrl = try {
            withTimeout(3000L) {
                val streetViewResponse = apiService.getStreetView(
                    locationId = backendLocation.id,
                    heading = (0..359).random(),
                    responsive = true
                )

                if (streetViewResponse.isSuccessful && streetViewResponse.body()?.success == true) {
                    val data = streetViewResponse.body()!!.data
                    println("LocationRepository: ✅ StreetView API Response erfolgreich")

                    // KORRIGIERT: Verwende die tatsächliche Backend-Response-Struktur aus den Logs
                    // Backend Response: {"streetView":{"interactive":"...","static":"..."}}
                    val streetViewData = data.streetView

                    if (streetViewData != null) {
                        // KORRIGIERT: Robuste JSON-Parsing für verschiedene Response-Formate
                        try {
                            // Versuche Gson-Parsing für verschachtelte JSON-Struktur
                            val gson = com.google.gson.Gson()
                            val jsonElement = gson.toJsonTree(streetViewData)

                            when {
                                jsonElement.isJsonObject -> {
                                    val jsonObject = jsonElement.asJsonObject

                                    // Prüfe auf "interactive" und "static" Felder (wie in den Logs)
                                    val interactive = jsonObject.get("interactive")?.asString
                                    val static = jsonObject.get("static")?.asString

                                    when {
                                        interactive != null && isUrlSafeAndValid(interactive) -> {
                                            // KRITISCH: Bereinige auch Backend URLs von unsupported Parametern
                                            val cleanedInteractive = removeUnsupportedEmbedParametersFromBackend(interactive)
                                            println("LocationRepository: ✅ Interactive URL bereinigt: ${cleanedInteractive.take(80)}...")
                                            cleanedInteractive
                                        }
                                        static != null && isUrlSafeAndValid(static) -> {
                                            println("LocationRepository: ✅ Static URL gefunden: ${static.take(80)}...")
                                            static
                                        }
                                        else -> {
                                            println("LocationRepository: ⚠️ Keine gültigen URLs in JSON-Objekt")
                                            null
                                        }
                                    }
                                }
                                jsonElement.isJsonPrimitive -> {
                                    // Direkter String-Wert
                                    val urlString = jsonElement.asString
                                    if (urlString.isNotEmpty() && isUrlSafeAndValid(urlString)) {
                                        println("LocationRepository: ✅ String URL gefunden: ${urlString.take(80)}...")
                                        urlString
                                    } else {
                                        println("LocationRepository: ❌ String URL ungültig: ${urlString.take(50)}")
                                        null
                                    }
                                }
                                else -> {
                                    println("LocationRepository: ❌ Unerwarteter JSON-Typ: ${jsonElement.javaClass.simpleName}")
                                    null
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback: Direkte String-Konvertierung
                            println("LocationRepository: ⚠️ JSON-Parsing fehlgeschlagen, versuche String-Fallback: ${e.message}")
                            val urlString = streetViewData.toString()

                            if (urlString.isNotEmpty() &&
                                !urlString.contains("[object Object]") &&
                                !urlString.contains("@") && // Kotlin Object toString
                                isUrlSafeAndValid(urlString)) {
                                println("LocationRepository: ✅ String-Fallback URL: ${urlString.take(80)}...")
                                urlString
                            } else {
                                println("LocationRepository: ❌ Alle Parsing-Versuche fehlgeschlagen")
                                null
                            }
                        }
                    } else {
                        println("LocationRepository: ❌ streetView Feld ist null")
                        null
                    }
                } else {
                    println("LocationRepository: ❌ StreetView API Response nicht erfolgreich: ${streetViewResponse.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            println("LocationRepository: ❌ StreetView API Exception: ${e.message}")
            null
        }

        // KORRIGIERT: Nur bei Backend-Ausfall verwende Fallbacks
        val finalUrl = streetViewUrl ?: run {
            println("LocationRepository: 🔧 Backend Street View nicht verfügbar für ${backendLocation.city}, verwende Fallback")

            // Intelligente Fallback-Strategie
            val knownLocationUrl = getKnownLocationFallback(backendLocation)
            if (knownLocationUrl != null) {
                println("LocationRepository: 🏛️ Bekannte Location Fallback: ${backendLocation.city}")
                knownLocationUrl
            } else {
                // KORRIGIERT: Prüfe Backend imageUrls mit Null-Safety
                val backendImageUrls = backendLocation.imageUrls
                if (!backendImageUrls.isNullOrEmpty()) {
                    val firstImageUrl = backendImageUrls.first()
                    if (firstImageUrl.isNotBlank() && !firstImageUrl.contains("PLACEHOLDER")) {
                        println("LocationRepository: 🖼️ Verwende Backend imageUrl: ${firstImageUrl.take(50)}...")
                        firstImageUrl
                    } else {
                        println("LocationRepository: 🌍 Verwende regionalen Fallback")
                        getRegionalFallbackImage(backendLocation)
                    }
                } else {
                    println("LocationRepository: 🌍 Verwende regionalen Fallback (imageUrls leer/null)")
                    getRegionalFallbackImage(backendLocation)
                }
            }
        }

        val locationEntity = LocationEntity(
            id = "${prefix}_${backendLocation.id}_${System.currentTimeMillis()}",
            latitude = backendLocation.coordinates.latitude,
            longitude = backendLocation.coordinates.longitude,
            imageUrl = finalUrl,
            country = backendLocation.country,
            city = backendLocation.city,
            difficulty = backendLocation.difficulty,
            isCached = true,
            isUsed = false
        )

        println("LocationRepository: ✅ LocationEntity erstellt für ${locationEntity.city} mit URL-Typ: ${
            when {
                finalUrl.contains("google.com/maps/embed") -> "Interactive Street View"
                finalUrl.contains("maps.googleapis.com/maps/api/streetview") -> "Static Street View"
                finalUrl.contains("unsplash.com") || finalUrl.contains("images.") -> "Fallback Image"
                else -> "Unknown"
            }
        }")

        locationDao.insertLocation(locationEntity)
        addToCache(locationEntity)
        return Result.success(locationEntity)
    }

    private suspend fun getLocationFromMapillaryOptimized(): Result<LocationEntity> {
        return try {
            println("LocationRepository: Mapillary deaktiviert - API-Schlüssel fehlt")
            Result.failure(Exception("Mapillary API nicht verfügbar"))
        } catch (e: Exception) {
            Result.failure(e)
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
        val imageMap = mapOf(
            "Paris" to "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800",
            "London" to "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800",
            "New York" to "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800",
            "Berlin" to "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800",
            "Tokyo" to "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800",
            "Sydney" to "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800",
            "Rome" to "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800",
            "Barcelona" to "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800"
        )

        return imageMap[city.split(" ").first()] ?: "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800"
    }

    suspend fun preloadLocations(count: Int = 10) {
        try {
            repeat(count) {
                val location = getRandomLocation()
                location.getOrNull()?.let { locationEntity ->
                    locationDao.insertLocation(locationEntity.copy(isCached = true))
                }
                delay(200) // Verhindere API Rate Limiting
            }
        } catch (e: Exception) {
            println("LocationRepository: Preload-Fehler: ${e.message}")
        }
    }

    suspend fun clearUnusedLocations() {
        try {
            locationDao.deleteUnusedOldLocations()
            locationCache.clear()
        } catch (e: Exception) {
            println("LocationRepository: Cache-Bereinigung fehlgeschlagen: ${e.message}")
        }
    }

    // Wrapper für neue Location-API-Endpunkte
    suspend fun getLocationsByDifficulty(difficulty: Int, limit: Int = 10): Result<List<LocationEntity>> {
        return try {
            val response = apiService.getLocationsByDifficulty(difficulty, limit)
            // KORRIGIERT: Verwende die korrigierte Response-Struktur
            if (response.isSuccessful && response.body()?.data != null) {
                val locations = response.body()!!.data.map { backendLocation ->
                    LocationEntity(
                        id = backendLocation.id.toString(),
                        latitude = backendLocation.coordinates.latitude,
                        longitude = backendLocation.coordinates.longitude,
                        imageUrl = backendLocation.imageUrls.firstOrNull() ?: "",
                        country = backendLocation.country,
                        city = backendLocation.city,
                        difficulty = backendLocation.difficulty,
                        isCached = true,
                        isUsed = false
                    )
                }
                Result.success(locations)
            } else {
                Result.failure(Exception("Fehler beim Laden der Locations nach Schwierigkeit"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocationsByCategory(category: String, limit: Int = 10): Result<List<LocationEntity>> {
        return try {
            val response = apiService.getLocationsByCategory(category, limit)
            // KORRIGIERT: Verwende die korrigierte Response-Struktur
            if (response.isSuccessful && response.body()?.data != null) {
                val locations = response.body()!!.data.map { backendLocation ->
                    LocationEntity(
                        id = backendLocation.id.toString(),
                        latitude = backendLocation.coordinates.latitude,
                        longitude = backendLocation.coordinates.longitude,
                        imageUrl = backendLocation.imageUrls.firstOrNull() ?: "",
                        country = backendLocation.country,
                        city = backendLocation.city,
                        difficulty = backendLocation.difficulty,
                        isCached = true,
                        isUsed = false
                    )
                }
                Result.success(locations)
            } else {
                Result.failure(Exception("Fehler beim Laden der Locations nach Kategorie"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocationsNear(lat: Double, lng: Double, radius: Int = 100, limit: Int = 10): Result<List<LocationEntity>> {
        return try {
            val response = apiService.getLocationsNear(lat, lng, radius, limit)
            // KORRIGIERT: Verwende die korrigierte Response-Struktur
            if (response.isSuccessful && response.body()?.data != null) {
                val locations = response.body()!!.data.map { backendLocation ->
                    LocationEntity(
                        id = backendLocation.id.toString(),
                        latitude = backendLocation.coordinates.latitude,
                        longitude = backendLocation.coordinates.longitude,
                        imageUrl = backendLocation.imageUrls.firstOrNull() ?: "",
                        country = backendLocation.country,
                        city = backendLocation.city,
                        difficulty = backendLocation.difficulty,
                        isCached = true,
                        isUsed = false
                    )
                }
                Result.success(locations)
            } else {
                Result.failure(Exception("Fehler beim Laden der Locations in der Nähe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkStreetViewAvailability(locationId: Int): Result<Boolean> {
        return try {
            val response = apiService.checkStreetViewAvailability(locationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.streetViewAvailable)
            } else {
                Result.failure(Exception("Fehler beim Prüfen der Street View Verfügbarkeit"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDistanceBetweenLocations(id1: Int, id2: Int): Result<Double> {
        return try {
            val response = apiService.getDistanceBetweenLocations(id1, id2)
            if (response.isSuccessful && response.body()?.distance != null) {
                Result.success(response.body()!!.distance.distanceKm)
            } else {
                Result.failure(Exception("Fehler beim Berechnen der Distanz"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isStreetViewAvailable(locationId: String): Boolean {
        return try {
            val numericId = locationId.toIntOrNull()
            if (numericId == null) return false
            val response = apiService.checkStreetViewAvailability(numericId)
            response.isSuccessful && response.body()?.streetViewAvailable == true
        } catch (e: Exception) {
            false
        }
    }

    // Neue Methoden für interaktive Street View
    suspend fun getInteractiveStreetView(
        locationId: Int,
        quality: String = "high",
        enableNavigation: Boolean = true
    ): Result<InteractiveStreetViewResponse> {
        return try {
            val response = apiService.getInteractiveStreetView(
                locationId = locationId,
                quality = quality,
                enableNavigation = enableNavigation
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Interaktive Street View nicht verfügbar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun navigateStreetView(
        currentLat: Double,
        currentLng: Double,
        direction: String,
        heading: Int,
        stepSize: Double = 25.0
    ): Result<StreetViewNavigationResponse> {
        return try {
            val request = StreetViewNavigationRequest(
                currentLat = currentLat,
                currentLng = currentLng,
                direction = direction,
                heading = heading,
                stepSize = stepSize
            )

            val response = apiService.navigateStreetView(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Navigation fehlgeschlagen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEnhancedRandomLocations(
        count: Int = 5,
        difficulty: Int? = null,
        category: String? = null,
        streetViewQuality: String = "high"
    ): Result<List<LocationEntity>> {
        return try {
            // KORRIGIERT: Enhanced API gibt es nicht - verwende Standard API
            println("LocationRepository: Enhanced API nicht verfügbar, nutze Standard-Fallback")

            // Verwende bestehende optimierte Backend-Suche
            val backendResult = getLocationFromBackendOptimized()
            if (backendResult.isSuccess) {
                val singleLocation = backendResult.getOrNull()!!
                // Generiere weitere Locations durch sequenzielle Calls
                val allLocations = mutableListOf(singleLocation)

                repeat(count - 1) {
                    delay(200) // Rate limiting
                    val additionalResult = getLocationFromBackendOptimized()
                    additionalResult.getOrNull()?.let { location ->
                        allLocations.add(location)
                    }
                }

                println("LocationRepository: ✅ ${allLocations.size} Locations über Standard-API geladen")
                Result.success(allLocations)
            } else {
                // Fallback zu lokalen Locations
                val localLocations = (1..count).map {
                    val location = generateUniqueLocation()
                    locationDao.insertLocation(location)
                    location
                }

                println("LocationRepository: ✅ ${localLocations.size} lokale Fallback-Locations generiert")
                Result.success(localLocations)
            }
        } catch (e: Exception) {
            // Fallback to existing method
            println("LocationRepository: Enhanced locations failed: ${e.message}, using fallback")
            (1..count).map { getRandomLocation() }
                .mapNotNull { it.getOrNull() }
                .let { locations ->
                    if (locations.size >= count) Result.success(locations)
                    else Result.failure(Exception("Nicht genügend Locations verfügbar"))
                }
        }
    }

    suspend fun getBulkStreetView(
        locationIds: List<String>,
        quality: String = "medium",
        interactive: Boolean = true
    ): Result<Map<String, InteractiveStreetView>> {
        return try {
            val response = apiService.getBulkStreetView(
                locationIds = locationIds.joinToString(","),
                quality = quality,
                interactive = interactive
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Bulk Street View laden fehlgeschlagen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ÜBERARBEITETE: Weniger restriktive Street View-Verfügbarkeitsprüfung
    private fun isLocationLikelyToHaveStreetView(
        lat: Double,
        lng: Double,
        city: String,
        country: String
    ): Boolean {
        val cityLower = city.lowercase()
        val countryLower = country.lowercase()

        // 1. NUR absolute No-Go-Zonen (sehr konservativ)
        val absoluteNoStreetView = listOf(
            "antarctica", "arctic ocean", "deep sahara", "deep gobi", "middle of pacific"
        )

        for (noGo in absoluteNoStreetView) {
            if (cityLower.contains(noGo) || countryLower.contains(noGo)) {
                println("LocationRepository: ❌ Absolute No-Go Zone erkannt: $noGo")
                return false
            }
        }

        // 2. Nur extreme geografische Bereiche ausschließen
        when {
            // Nur extreme Antarktis (südlich von -75°)
            lat < -75.0 -> {
                println("LocationRepository: ❌ Extreme Antarktis: $lat")
                return false
            }
            // Nur extreme Arktis (nördlich von 80°)
            lat > 80.0 -> {
                println("LocationRepository: ❌ Extreme Arktis: $lat")
                return false
            }
            // Nur definitiv mitten im tiefen Ozean
            isDefinitelyInDeepOcean(lat, lng) -> {
                println("LocationRepository: ❌ Mitten im tiefen Ozean")
                return false
            }
        }

        // 3. OPTIMISTISCHE STANDARDANNAHME: Fast überall gibt es Street View
        println("LocationRepository: ✅ Location $cityLower wahrscheinlich Street View verfügbar")
        return true
    }

    // VERBESSERTE: Viel weniger restriktive URL-Validierung
    private fun isUrlSafeAndValid(url: String): Boolean {
        try {
            // 1. Nur wirklich kritische Probleme prüfen
            if (url.contains("[object Object]") || url.contains("undefined")) {
                println("LocationRepository: ❌ URL enthält korrupte Daten")
                return false
            }

            if (url.isBlank()) {
                println("LocationRepository: ❌ URL ist leer")
                return false
            }

            // 2. Für Google URLs: Basis API-Key Validierung
            if (url.contains("google.com") || url.contains("googleapis.com")) {
                if (!url.contains("key=AIza")) {
                    println("LocationRepository: ❌ Google URL ohne API-Key")
                    return false
                }
            }

            // 3. Nur extreme URL-Längen ausschließen
            if (url.length > 8192) { // Sehr großzügiges Limit
                println("LocationRepository: ❌ URL extrem lang (${url.length} Zeichen)")
                return false
            }

            // 4. Nur ungültige Koordinatenbereiche prüfen
            if (url.contains("location=") && hasInvalidCoordinates(url)) {
                println("LocationRepository: ❌ Ungültige Koordinaten in URL")
                return false
            }

            // 5. OPTIMISTISCHE STANDARDANNAHME: URL ist gültig
            return true

        } catch (e: Exception) {
            println("LocationRepository: ⚠️ URL-Validierung Fehler (erlaubt trotzdem): ${e.message}")
            return true // Bei Unklarheit: erlauben
        }
    }

    // Hilfsmethode für Koordinatenvalidierung
    private fun hasInvalidCoordinates(url: String): Boolean {
        return try {
            val locationPattern = Regex("location=([^&]+)")
            val match = locationPattern.find(url) ?: return false

            val coordString = match.groupValues[1]
            val coords = coordString.split(",", "%2C")

            if (coords.size >= 2) {
                val lat = coords[0].toDoubleOrNull() ?: return false
                val lng = coords[1].toDoubleOrNull() ?: return false

                // Nur offensichtlich ungültige Koordinaten
                return lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
            }

            false
        } catch (e: Exception) {
            false // Bei Fehlern: als gültig betrachten
        }
    }

    // Sehr konservative Deep Ocean Detection
    private fun isDefinitelyInDeepOcean(lat: Double, lng: Double): Boolean {
        return when {
            // Nur die allertiefsten Ozean-Bereiche, weit weg von Land
            (lat > -5.0 && lat < 5.0 && lng > 160.0 && lng < -150.0) -> true // Mitte des Pazifiks
            (lat > -5.0 && lat < 5.0 && lng > -40.0 && lng < -20.0) -> true // Mitte des Atlantiks
            (lat > -10.0 && lat < 0.0 && lng > 80.0 && lng < 100.0) -> true // Mitte des Indischen Ozeans
            else -> false
        }
    }

    // Fallback-Methoden für bekannte Locations
    private fun getKnownLocationFallback(location: com.example.geogeusserclone.data.network.BackendLocation): String? {
        val cityName = location.city.lowercase()
        val countryName = location.country.lowercase()

        return when {
            cityName.contains("paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            cityName.contains("london") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
            cityName.contains("new york") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
            cityName.contains("tokyo") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
            cityName.contains("berlin") -> "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop"
            cityName.contains("sydney") -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
            cityName.contains("rome") -> "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop"
            cityName.contains("barcelona") -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop"
            cityName.contains("providence") || cityName.contains("provence") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            cityName.contains("aqaba") -> "https://images.unsplash.com/photo-1539650116574-75c0c6d73f6e?w=800&h=600&fit=crop"
            cityName.contains("cork") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
            else -> null
        }
    }

    // Regionale Fallback-Images
    private fun getRegionalFallbackImage(location: com.example.geogeusserclone.data.network.BackendLocation): String {
        val countryName = location.country.lowercase()

        return when {
            countryName.contains("france") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            countryName.contains("united kingdom") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
            countryName.contains("united states") || countryName.contains("usa") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
            countryName.contains("germany") -> "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop"
            countryName.contains("japan") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
            countryName.contains("australia") -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
            countryName.contains("italy") -> "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop"
            countryName.contains("spain") -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop"
            else -> "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
        }
    }

    // NEUE: Fehlende getRegionalFallbackByCoords Funktion
    private fun getRegionalFallbackByCoords(lat: Double, lng: Double): String {
        return when {
            // Europa
            lat in 35.0..70.0 && lng in -10.0..40.0 -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            // Nordamerika
            lat in 25.0..70.0 && lng in -170.0..-50.0 -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
            // Asien
            lat in 10.0..70.0 && lng in 70.0..180.0 -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
            // Australien/Ozeanien
            lat in -50.0..-10.0 && lng in 110.0..180.0 -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
            // Südamerika
            lat in -60.0..15.0 && lng in -85.0..-30.0 -> "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800&h=600&fit=crop"
            // Afrika
            lat in -35.0..35.0 && lng in -20.0..55.0 -> "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=800&h=600&fit=crop"
            // Default
            else -> "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
        }
    }


    // NEUE: getKnownLocationFallbackByCoords Funktion
    private fun getKnownLocationFallbackByCoords(
        lat: Double,
        lng: Double,
        city: String?,
        country: String?
    ): String? {
        val cityLower = city?.lowercase() ?: ""
        val countryLower = country?.lowercase() ?: ""

        return when {
            cityLower.contains("death valley") -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=600&fit=crop"
            cityLower.contains("paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            cityLower.contains("london") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
            cityLower.contains("new york") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
            cityLower.contains("tokyo") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
            cityLower.contains("sydney") -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
            cityLower.contains("berlin") -> "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop"
            cityLower.contains("rome") -> "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop"
            cityLower.contains("barcelona") -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop"
            cityLower.contains("providence") || cityLower.contains("provence") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            cityLower.contains("aqaba") -> "https://images.unsplash.com/photo-1539650116574-75c0c6d73f6e?w=800&h=600&fit=crop"
            cityLower.contains("cork") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
            else -> null
        }
    }

    // NEUE: Backend URL-Bereinigung für unsupported Parameter - KORRIGIERT mit URL-Dekodierung
    private fun removeUnsupportedEmbedParametersFromBackend(url: String): String {
        // KRITISCH: Dekodiere URL zuerst, bevor Parameter entfernt werden
        var cleanedUrl = decodeUrlParametersInRepository(url)

        // KRITISCH: Google Maps Embed API unterstützt diese Parameter NICHT
        val unsupportedParams = listOf(
            "navigation=1", "navigation=true",
            "controls=1", "controls=true",
            "zoom=1", "zoom=true",
            "fullscreen=1", "fullscreen=true",
            // NEUE: Zusätzliche problematische Parameter
            "disableDefaultUI=1", "disableDefaultUI=true",
            "gestureHandling=none", "gestureHandling=greedy",
            "mapTypeControl=false", "streetViewControl=false"
        )

        // Entferne alle unsupported Parameter systematisch
        for (param in unsupportedParams) {
            // Entferne Parameter am Ende der URL
            cleanedUrl = cleanedUrl.replace("&$param", "")
            // Entferne Parameter direkt nach dem ? und ersetze mit nächstem Parameter
            cleanedUrl = cleanedUrl.replace("?$param&", "?")
            // Entferne Parameter wenn es der einzige Parameter ist
            cleanedUrl = cleanedUrl.replace("?$param", "")
        }

        // NEUE: Bereinige mehrfache & Zeichen
        cleanedUrl = cleanedUrl.replace("&&+".toRegex(), "&")

        // NEUE: Entferne & am Ende der URL
        cleanedUrl = cleanedUrl.trimEnd('&')

        println("LocationRepository: 🔧 Backend URL bereinigt von: ${url.take(120)}...")
        println("LocationRepository: 🔧 Backend URL bereinigt zu: ${cleanedUrl.take(120)}...")

        return cleanedUrl
    }

    // NEUE: URL-Parameter Dekodierung für Repository
    private fun decodeUrlParametersInRepository(url: String): String {
        return try {
            // Dekodiere nur die wichtigsten URL-kodierten Zeichen für Google Maps
            url.replace("%2C", ",")  // Komma - KRITISCH für location Parameter
               .replace("%20", " ")  // Leerzeichen
               .replace("%3D", "=")  // Gleichheitszeichen
               .replace("%26", "&")  // Ampersand
               .also { decodedUrl ->
                   println("LocationRepository: 🔄 URL dekodiert von: ${url.take(80)}...")
                   println("LocationRepository: 🔄 URL dekodiert zu: ${decodedUrl.take(80)}...")
               }
        } catch (e: Exception) {
            println("LocationRepository: ⚠️ URL-Dekodierung fehlgeschlagen: ${e.message}")
            url // Returniere Original bei Fehlern
        }
    }
}
