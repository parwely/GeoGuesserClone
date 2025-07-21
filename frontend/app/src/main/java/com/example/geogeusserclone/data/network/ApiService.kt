package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Authentication Endpoints
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body refreshRequest: RefreshTokenRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    // Game Endpoints
    @GET("api/game/location")
    suspend fun getRandomLocation(): Response<LocationResponse>

    @POST("api/game/create")
    suspend fun createGame(@Body gameRequest: CreateGameRequest): Response<GameResponse>

    @POST("api/game/guess")
    suspend fun submitGuess(@Body guessRequest: GuessRequest): Response<GuessResponse>

    @GET("api/game/{gameId}")
    suspend fun getGame(@Path("gameId") gameId: String): Response<GameResponse>

    @PUT("api/game/{gameId}/complete")
    suspend fun completeGame(@Path("gameId") gameId: String): Response<GameResponse>

    // User & Stats Endpoints
    @GET("api/user/stats")
    suspend fun getUserStats(): Response<UserStatsResponse>

    @GET("api/user/profile")
    suspend fun getUserProfile(): Response<UserDto>

    @PUT("api/user/profile")
    suspend fun updateUserProfile(@Body updateRequest: UpdateProfileRequest): Response<UserDto>

    // Leaderboard & Social
    @GET("api/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 10): Response<LeaderboardResponse>

    @GET("api/leaderboard/friends")
    suspend fun getFriendsLeaderboard(): Response<LeaderboardResponse>

    // Battle Royale Endpoints
    @POST("api/battle-royale/join")
    suspend fun joinBattleRoyale(): Response<BattleRoyaleResponse>

    @GET("api/battle-royale/{gameId}")
    suspend fun getBattleRoyaleStatus(@Path("gameId") gameId: String): Response<BattleRoyaleResponse>

    // Location Management
    @GET("api/locations/batch")
    suspend fun getLocationsBatch(@Query("count") count: Int = 10): Response<LocationBatchResponse>

    @POST("api/locations/{locationId}/report")
    suspend fun reportLocation(@Path("locationId") locationId: String, @Body report: LocationReportRequest): Response<Unit>
}

// Request Data Classes
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val username: String, val email: String, val password: String)
data class RefreshTokenRequest(val refreshToken: String)
data class CreateGameRequest(val gameMode: String, val rounds: Int = 5)
data class GuessRequest(val gameId: String, val locationId: String, val latitude: Double, val longitude: Double)
data class UpdateProfileRequest(val username: String?, val email: String?)
data class LocationReportRequest(val reason: String, val description: String)

// Response Data Classes
data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDto,
    val expiresIn: Long
)

data class LocationResponse(
    val id: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val city: String?,
    val difficulty: Int
)

data class GameResponse(
    val id: String,
    val status: String,
    val currentRound: Int,
    val totalRounds: Int,
    val score: Int,
    val gameMode: String,
    val isCompleted: Boolean,
    val duration: Long?
)

data class GuessResponse(
    val score: Int,
    val distance: Double,
    val points: Int,
    val totalScore: Int,
    val isCorrect: Boolean,
    val timeBonus: Int = 0
)

data class UserStatsResponse(
    val gamesPlayed: Int,
    val totalScore: Int,
    val bestScore: Int,
    val averageScore: Double,
    val totalDistance: Double,
    val averageDistance: Double,
    val bestDistance: Double,
    val perfectGuesses: Int,
    val rank: Int
)

data class LeaderboardResponse(
    val users: List<LeaderboardEntry>,
    val userRank: Int?,
    val totalUsers: Int
)

data class BattleRoyaleResponse(
    val gameId: String,
    val status: String,
    val players: List<BattleRoyalePlayer>,
    val currentRound: Int,
    val timeRemaining: Long,
    val isEliminated: Boolean = false
)

data class LocationBatchResponse(
    val locations: List<LocationResponse>
)

// Supporting Data Classes
data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val createdAt: String,
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0
)

data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val score: Int,
    val rank: Int,
    val gamesPlayed: Int,
    val averageScore: Double
)

data class BattleRoyalePlayer(
    val userId: String,
    val username: String,
    val score: Int,
    val isEliminated: Boolean,
    val rank: Int?
)