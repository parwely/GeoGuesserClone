package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Authentication
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body refreshRequest: RefreshTokenRequest): Response<AuthResponse>

    // Game endpoints
    @GET("api/game/location")
    suspend fun getRandomLocation(): Response<LocationResponse>

    @POST("api/game/create")
    suspend fun createGame(@Body gameRequest: CreateGameRequest): Response<GameResponse>

    @POST("api/game/guess")
    suspend fun submitGuess(@Body guessRequest: GuessRequest): Response<GuessResponse>

    @GET("api/game/{gameId}")
    suspend fun getGame(@Path("gameId") gameId: String): Response<GameResponse>

    // User stats
    @GET("api/user/stats")
    suspend fun getUserStats(): Response<UserStatsResponse>

    @GET("api/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 10): Response<LeaderboardResponse>
}

// Request/Response Data Classes
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class RefreshTokenRequest(val refreshToken: String)
data class CreateGameRequest(val gameMode: String, val rounds: Int = 5)
data class GuessRequest(val gameId: String, val locationId: String, val latitude: Double, val longitude: Double)

data class AuthResponse(val token: String, val refreshToken: String, val user: UserDto)
data class LocationResponse(val id: String, val imageUrl: String, val latitude: Double, val longitude: Double)
data class GameResponse(val id: String, val status: String, val currentRound: Int, val totalRounds: Int, val score: Int)
data class GuessResponse(val score: Int, val distance: Double, val points: Int, val totalScore: Int)
data class UserStatsResponse(val gamesPlayed: Int, val totalScore: Int, val bestScore: Int, val averageScore: Double)
data class LeaderboardResponse(val users: List<LeaderboardEntry>)

data class UserDto(val id: String, val username: String, val email: String)
data class LeaderboardEntry(val username: String, val score: Int, val rank: Int)