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

    // Location Endpoints (Erweiterungen)
    @GET("locations/difficulty/{difficulty}")
    suspend fun getLocationsByDifficulty(
        @Path("difficulty") difficulty: Int,
        @Query("limit") limit: Int = 10
    ): Response<LocationsResponse>

    @GET("locations/category/{category}")
    suspend fun getLocationsByCategory(
        @Path("category") category: String,
        @Query("limit") limit: Int = 10
    ): Response<LocationsResponse>

    @GET("locations/near/{lat}/{lng}")
    suspend fun getLocationsNear(
        @Path("lat") lat: Double,
        @Path("lng") lng: Double,
        @Query("radius") radius: Int = 100,
        @Query("limit") limit: Int = 10
    ): Response<LocationsResponse>

    @GET("locations/{id}/streetview/check")
    suspend fun checkStreetViewAvailability(
        @Path("id") locationId: Int
    ): Response<StreetViewAvailabilityResponse>

    @GET("locations/distance/{id1}/{id2}")
    suspend fun getDistanceBetweenLocations(
        @Path("id1") id1: Int,
        @Path("id2") id2: Int
    ): Response<LocationDistanceResponse>

    // Session Endpoints
    @POST("session")
    suspend fun createSession(
        @Body request: SessionCreateRequest
    ): Response<SessionCreateResponse>

    @POST("session/join")
    suspend fun joinSession(
        @Body request: SessionJoinRequest
    ): Response<SessionJoinResponse>

    @GET("session/{sessionId}")
    suspend fun getSessionInfo(
        @Path("sessionId") sessionId: String
    ): Response<SessionInfoResponse>

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

    // Enhanced Street View Endpoints für interaktive Features
    @GET("locations/{id}/streetview/interactive")
    suspend fun getInteractiveStreetView(
        @Path("id") locationId: Int,
        @Query("quality") quality: String = "high",
        @Query("enableNavigation") enableNavigation: Boolean = true,
        @Query("heading") heading: Int? = null,
        @Query("pitch") pitch: Int? = null
    ): Response<InteractiveStreetViewResponse>

    @POST("locations/streetview/navigate")
    suspend fun navigateStreetView(
        @Body request: StreetViewNavigationRequest
    ): Response<StreetViewNavigationResponse>

    // Enhanced Random Locations mit eingebetteter Street View
    @GET("locations/random/enhanced")
    suspend fun getEnhancedRandomLocations(
        @Query("count") count: Int = 5,
        @Query("difficulty") difficulty: Int? = null,
        @Query("category") category: String? = null,
        @Query("includeStreetView") includeStreetView: Boolean = true,
        @Query("streetViewQuality") streetViewQuality: String = "high"
    ): Response<EnhancedLocationResponse>

    // Bulk Street View für mehrere Locations
    @GET("locations/streetview/bulk")
    suspend fun getBulkStreetView(
        @Query("locationIds") locationIds: String, // Comma-separated IDs
        @Query("quality") quality: String = "medium",
        @Query("interactive") interactive: Boolean = true
    ): Response<Map<String, InteractiveStreetView>>
}
