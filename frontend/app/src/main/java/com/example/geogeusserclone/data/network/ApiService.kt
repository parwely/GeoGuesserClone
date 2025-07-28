package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Authentication endpoints
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    // Location endpoints
    @GET("locations/random")
    suspend fun getRandomLocation(): Response<LocationResponse>

    @GET("locations")
    suspend fun getLocations(@Query("limit") limit: Int): Response<LocationsResponse>

    // User stats
    @PUT("users/{userId}/stats")
    suspend fun updateUserStats(
        @Path("userId") userId: String,
        @Query("totalScore") totalScore: Int,
        @Query("gamesPlayed") gamesPlayed: Int,
        @Query("bestScore") bestScore: Int
    ): Response<Unit>
}

// Request/Response models
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class LoginResponse(
    val user: UserResponse,
    val token: String,
    val refreshToken: String
)

data class RegisterResponse(
    val user: UserResponse,
    val token: String,
    val refreshToken: String
)

data class TokenResponse(
    val token: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val totalScore: Int? = 0,
    val gamesPlayed: Int? = 0
)