package com.example.geogeusserclone.data.network

import com.example.geogeusserclone.data.network.LocationResponse
import com.example.geogeusserclone.data.network.LocationsResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("locations/random")
    suspend fun getRandomLocation(): Response<LocationResponse>

    @GET("locations")
    suspend fun getLocations(
        @Query("limit") limit: Int = 10
    ): Response<LocationsResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>


    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @POST("games")
    suspend fun createGame(
        @Body request: CreateGameRequest
    ): Response<GameResponse>

    @POST("games/{gameId}/guess")
    suspend fun submitGuess(
        @Path("gameId") gameId: String,
        @Body request: GuessRequest
    ): Response<GuessResponse>
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
