package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Authentication Endpoints
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    // Location Endpoints
    @GET("locations/random")
    suspend fun getRandomLocations(
        @Query("count") count: Int = 5,
        @Query("difficulty") difficulty: Int? = null,
        @Query("category") category: String? = null
    ): Response<LocationsResponse>

    @GET("locations/{id}")
    suspend fun getLocationById(
        @Path("id") locationId: Int
    ): Response<LocationResponse>

    @GET("locations/{id}/streetview")
    suspend fun getStreetView(
        @Path("id") locationId: Int,
        @Query("heading") heading: Int? = null,
        @Query("multiple") multiple: Boolean = false,
        @Query("responsive") responsive: Boolean = false
    ): Response<StreetViewResponse>

    @GET("locations/stats/overview")
    suspend fun getLocationStats(): Response<StatsResponse>

    // Game Endpoints
    @POST("games/single")
    suspend fun createSinglePlayerGame(
        @Body request: GameCreateRequest
    ): Response<GameCreateResponse>

    @PUT("games/{gameId}/result")
    suspend fun submitGameResult(
        @Path("gameId") gameId: Int,
        @Body request: GameResultRequest
    ): Response<GameResultResponse>

    // Health Check
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>
}

// Auth Models
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val data: AuthData
)

data class AuthData(
    val user: BackendUser,
    val token: String,
    val expiresIn: String = "7d"
)

data class BackendUser(
    val id: Int,
    val username: String,
    val email: String,
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0,
    val bestScore: Int = 0
)

data class TokenResponse(
    val success: Boolean,
    val data: TokenData
)

data class TokenData(
    val token: String,
    val expiresIn: String
)

data class MessageResponse(
    val success: Boolean,
    val message: String
)

// Location Models
data class LocationsResponse(
    val success: Boolean,
    val data: LocationsData
)

data class LocationsData(
    val count: Int,
    val locations: List<BackendLocation>
)

data class LocationResponse(
    val success: Boolean,
    val data: LocationDetailData
)

data class LocationDetailData(
    val location: BackendLocation
)

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
    val hints: Map<String, Any> = emptyMap(),
    val viewCount: Int = 0
)

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class StreetViewResponse(
    val success: Boolean,
    val data: StreetViewData
)

data class StreetViewData(
    val location: StreetViewLocation,
    val streetViewUrl: String? = null,
    val streetViewUrls: Any? = null // Kann Map oder Array sein je nach responsive Parameter
)

data class StreetViewLocation(
    val id: Int,
    val coordinates: Coordinates
)

// Game Models
data class GameCreateRequest(
    val difficulty: Int = 2,
    val category: String = "urban",
    val rounds: Int = 5
)

data class GameCreateResponse(
    val success: Boolean,
    val data: GameCreateData
)

data class GameCreateData(
    val gameId: Int,
    val locations: List<BackendLocation>,
    val settings: GameSettings,
    val createdAt: String
)

data class GameSettings(
    val difficulty: Int,
    val category: String,
    val rounds: Int
)

data class GameResultRequest(
    val totalScore: Int,
    val totalDistance: Double,
    val accuracy: Double,
    val timeTaken: Long,
    val roundsData: List<RoundData>
)

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

data class GameResultResponse(
    val success: Boolean,
    val data: GameResultData
)

data class GameResultData(
    val resultId: Int,
    val gameId: Int,
    val totalScore: Int,
    val submittedAt: String
)

// Stats Models
data class StatsResponse(
    val success: Boolean,
    val data: StatsData
)

data class StatsData(
    val stats: LocationStats
)

data class LocationStats(
    val total: Int,
    val difficulties: Map<String, Int>,
    val categories: Map<String, Int>
)

// Health Models
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val uptime: Long,
    val database: String
)
