package com.example.geogeusserclone.data.network

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

// Authentication Request Classes
@Serializable
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

// Authentication Response Classes
@Serializable
data class AuthResponse(
    val success: Boolean,
    val data: AuthData
)

@Serializable
data class AuthData(
    val user: BackendUser,
    val token: String,
    val expiresIn: String = "7d"
)

@Serializable
data class BackendUser(
    val id: Int,
    val username: String,
    val email: String,
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0,
    val bestScore: Int = 0
)

@Serializable
data class TokenResponse(
    val success: Boolean,
    val data: TokenData
)

@Serializable
data class TokenData(
    val token: String,
    val expiresIn: String
)

@Serializable
data class MessageResponse(
    val success: Boolean,
    val message: String
)

// Location Models
@Serializable
data class LocationsResponse(
    val success: Boolean,
    val data: LocationsData
)

@Serializable
data class LocationsData(
    val count: Int,
    val locations: List<BackendLocation>
)

@Serializable
data class LocationResponse(
    val success: Boolean,
    val data: LocationDetailData
)

@Serializable
data class LocationDetailData(
    val location: BackendLocation
)

@Serializable
data class BackendLocation(
    val id: Int,
    val name: String? = null,
    val country: String,
    val city: String,
    val coordinates: Coordinates,
    val difficulty: Int,
    val difficultyName: String,
    val category: String,
    val imageUrls: List<String> = emptyList(),
    val hints: Map<String, String> = emptyMap(), // Changed from Any to String
    val viewCount: Int = 0
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class StreetViewResponse(
    val success: Boolean,
    val data: StreetViewData
)

@Serializable
data class StreetViewData(
    val location: StreetViewLocation,
    val streetViewUrl: String? = null,
    val streetViewUrls: Map<String, String>? = null, // Legacy
    @Contextual val streetViewUrlsRaw: JsonObject? = null // NEU: Rohdaten für gemischte Typen
)

@Serializable
data class StreetViewLocation(
    val id: Int,
    val coordinates: Coordinates
)

// Game Models
@Serializable
data class GameCreateRequest(
    val difficulty: Int = 2,
    val category: String = "urban",
    val rounds: Int = 5
)

@Serializable
data class GameCreateResponse(
    val success: Boolean,
    val data: GameCreateData
)

@Serializable
data class GameCreateData(
    val gameId: Int,
    val locations: List<BackendLocation>,
    val settings: GameSettings,
    val createdAt: String
)

@Serializable
data class GameSettings(
    val difficulty: Int,
    val category: String,
    val rounds: Int
)

@Serializable
data class GameResultRequest(
    val totalScore: Int,
    val totalDistance: Double,
    val accuracy: Double,
    val timeTaken: Long,
    val roundsData: List<RoundData>
)

@Serializable
data class RoundData(
    val locationId: Int,
    val guessLatitude: Double,
    val guessLongitude: Double,
    val actualLatitude: Double,
    val actualLongitude: Double,
    val distance: Double,
    val score: Int,
    val timeSpent: Long
)

@Serializable
data class GameResultResponse(
    val success: Boolean,
    val data: GameResultData? = null
)

@Serializable
data class GameResultData(
    val resultId: Int,
    val gameId: Int,
    val totalScore: Int,
    val submittedAt: String
)

// Stats Models
@Serializable
data class StatsResponse(
    val success: Boolean,
    val data: StatsData
)

@Serializable
data class StatsData(
    val stats: LocationStats
)

@Serializable
data class LocationStats(
    val total: Int,
    val difficulties: Map<String, Int>,
    val categories: Map<String, Int>
)

// Health Models
@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val uptime: Long,
    val database: String
)

// Erweiterte Street View Response für interaktive Features
@Serializable
data class InteractiveStreetViewResponse(
    val success: Boolean,
    val data: InteractiveStreetViewData
)

@Serializable
data class InteractiveStreetViewData(
    val location: StreetViewLocation,
    val streetView: InteractiveStreetView
)

@Serializable
data class InteractiveStreetView(
    val type: String, // "interactive" or "static"
    val embedUrl: String,
    val staticFallback: String? = null,
    val navigationEnabled: Boolean = true,
    val quality: String = "high", // "low", "medium", "high"
    val heading: Int? = null,
    val pitch: Int? = null,
    val zoom: Float? = null
)

// Navigation Request für dynamische Street View-Bewegung
@Serializable
data class StreetViewNavigationRequest(
    val currentLat: Double,
    val currentLng: Double,
    val direction: String, // "forward", "backward", "left", "right"
    val heading: Int,
    val stepSize: Double = 25.0
)

@Serializable
data class StreetViewNavigationResponse(
    val success: Boolean,
    val data: StreetViewNavigationData
)

@Serializable
data class StreetViewNavigationData(
    val newLocation: Coordinates,
    val heading: Int,
    val streetView: InteractiveStreetView,
    val available: Boolean
)

// Enhanced Location Response mit eingebetteter Street View
@Serializable
data class EnhancedLocationResponse(
    val success: Boolean,
    val data: EnhancedLocationData
)

@Serializable
data class EnhancedLocationData(
    val count: Int,
    val locations: List<EnhancedBackendLocation>
)

@Serializable
data class EnhancedBackendLocation(
    val id: Int,
    val name: String? = null,
    val country: String,
    val city: String,
    val coordinates: Coordinates,
    val difficulty: Int,
    val difficultyName: String,
    val category: String,
    val imageUrls: List<String> = emptyList(),
    val hints: Map<String, String> = emptyMap(),
    val viewCount: Int = 0,
    val streetView: InteractiveStreetView? = null // NEW: Embedded Street View
)

