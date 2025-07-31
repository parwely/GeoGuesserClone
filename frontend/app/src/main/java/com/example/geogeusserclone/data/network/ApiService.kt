package com.example.geogeusserclone.data.network

import com.example.geogeusserclone.data.network.LocationResponse
import com.example.geogeusserclone.data.network.LocationsResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Updated endpoints for production backend
    @GET("api/locations/random")
    suspend fun getRandomLocation(
        @Query("count") count: Int = 1
    ): Response<LocationsResponse>

    @GET("api/locations")
    suspend fun getLocations(
        @Query("limit") limit: Int = 10
    ): Response<LocationsResponse>

    @GET("api/locations/{id}/streetview")
    suspend fun getStreetViewImage(
        @Path("id") locationId: String,
        @Query("angle") angle: Int? = null,
        @Query("multiple") multiple: Boolean = false
    ): Response<okhttp3.ResponseBody>

    // Updated auth endpoints
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Updated game endpoints for single player
    @POST("api/games/single")
    suspend fun createSinglePlayerGame(
        @Body request: CreateSinglePlayerGameRequest
    ): Response<GameResponse>

    @PUT("api/games/{id}/result")
    suspend fun submitGameResult(
        @Path("id") gameId: String,
        @Body request: GameResultRequest
    ): Response<GameResultResponse>
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0,
    val bestScore: Int = 0
)

data class CreateGameRequest(
    val gameMode: String,
    val rounds: Int
)

data class GameResponse(
    val id: String,
    val gameMode: String,
    val totalRounds: Int,
    val currentRound: Int,
    val score: Int,
    val isCompleted: Boolean,
    val createdAt: Long
)

data class GuessRequest(
    val locationId: String,
    val guessLat: Double,
    val guessLng: Double,
    val timeSpent: Long
)

data class GuessResponse(
    val id: String,
    val distance: Double,
    val score: Int,
    val actualLat: Double,
    val actualLng: Double
)

// New request/response models for backend integration
data class CreateSinglePlayerGameRequest(
    val rounds: Int = 5,
    val gameMode: String = "single"
)

data class GameResultRequest(
    val guesses: List<GuessResultData>,
    val totalScore: Int,
    val completedAt: Long
)

data class GuessResultData(
    val locationId: String,
    val guessLat: Double,
    val guessLng: Double,
    val actualLat: Double,
    val actualLng: Double,
    val distance: Double,
    val score: Int,
    val timeSpent: Long
)

data class GameResultResponse(
    val gameId: String,
    val finalScore: Int,
    val rank: Int?,
    val achievements: List<String> = emptyList()
)
