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
