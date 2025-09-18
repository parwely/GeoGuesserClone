package com.example.geogeusserclone.data.network

import com.example.geogeusserclone.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface GameApi {

    @GET("game/newRound")
    suspend fun newRound(
        @Query("difficulty") difficulty: Int? = null,
        @Query("category") category: String? = null
    ): Response<BackendRoundResponse> // Changed to handle direct backend response

    @POST("game/guess")
    suspend fun submitGuess(@Body guess: GuessRequest): Response<ScoreResponse>

    @GET("game/session/{sessionId}")
    suspend fun getGameSession(@Path("sessionId") sessionId: String): Response<GameSession>

    @POST("game/session")
    suspend fun createGameSession(): Response<GameSession>

    // KORRIGIERT: Street View Check API-Pfad angepasst für Backend-Kompatibilität
    @GET("game/streetview/check/{locationId}")
    suspend fun checkStreetViewAvailability(@Path("locationId") locationId: Int): Response<StreetViewAvailabilityResponse>

    @GET("game/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 10): Response<LeaderboardResponse>

    // NEUE: Zusätzliche Street View APIs für bessere Kompatibilität
    @GET("locations/{locationId}/streetview")
    suspend fun getLocationStreetView(@Path("locationId") locationId: Int): Response<StreetViewAvailabilityResponse>

    @GET("locations/{locationId}/streetview/status")
    suspend fun getStreetViewStatus(@Path("locationId") locationId: Int): Response<StreetViewAvailabilityResponse>
}

@kotlinx.serialization.Serializable
data class StreetViewAvailabilityResponse(
    val success: Boolean,
    val streetViewAvailable: Boolean,
    val panoId: String? = null
)

@kotlinx.serialization.Serializable
data class LeaderboardResponse(
    val success: Boolean,
    val leaderboard: List<LeaderboardEntry>
)

@kotlinx.serialization.Serializable
data class LeaderboardEntry(
    val playerName: String,
    val score: Int,
    val timestamp: Long
)
