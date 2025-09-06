package com.example.geogeusserclone.data.network

import kotlinx.serialization.Serializable

// Authentication Request Classes
@Serializable
data class LoginRequest(
    val email: String,
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
data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val data: LoginData
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String? = null,
    val data: RegisterData
)

@Serializable
data class LoginData(
    val user: UserResponse,
    val token: String
)

@Serializable
data class RegisterData(
    val user: UserResponse,
    val token: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val totalScore: Int? = null,
    val gamesPlayed: Int? = null,
    val bestScore: Int? = null
)

// Game Request Classes
@Serializable
data class GameCreateRequest(
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

// Game Response Classes
@Serializable
data class GameCreateResponse(
    val success: Boolean,
    val message: String? = null,
    val data: GameData
)

@Serializable
data class GameResultResponse(
    val success: Boolean,
    val message: String? = null,
    val data: GameResultData? = null
)

@Serializable
data class GameData(
    val gameId: Int,
    val locations: List<BackendLocation>
)

@Serializable
data class GameResultData(
    val rank: Int? = null,
    val percentile: Double? = null
)

// Location Classes
@Serializable
data class LocationsResponse(
    val success: Boolean,
    val message: String? = null,
    val data: LocationsData
)

@Serializable
data class LocationsData(
    val count: Int,
    val locations: List<BackendLocation>
)

@Serializable
data class BackendLocation(
    val id: Int,
    val coordinates: Coordinates,
    val country: String,
    val city: String,
    val difficulty: Int = 2,
    val imageUrls: List<String> = emptyList()
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

// Street View Classes
@Serializable
data class StreetViewResponse(
    val success: Boolean,
    val message: String? = null,
    val data: StreetViewData
)

@Serializable
data class StreetViewData(
    val streetViewUrl: String? = null,
    val streetViewUrls: Any? = null // Can be String or Map<String, String>
)
