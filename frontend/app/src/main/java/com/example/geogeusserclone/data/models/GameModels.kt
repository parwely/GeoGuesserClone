package com.example.geogeusserclone.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val id: Int,
    val lat: Double,
    val lng: Double,
    val pano_id: String? = null,
    val country: String? = null,
    val city: String? = null,
    val difficulty: Int = 1
)

@Serializable
data class NewRoundResponse(
    val success: Boolean = true, // Default to true for backward compatibility
    val roundId: String,
    val location: LocationData,
    val message: String? = null
)

// Alternative constructor for direct backend response
@Serializable
data class BackendRoundResponse(
    val id: Int,
    val lat: Double,
    val lng: Double,
    val pano_id: String? = null,
    val location_hint: String? = null
) {
    // Convert to NewRoundResponse
    fun toNewRoundResponse(): NewRoundResponse {
        return NewRoundResponse(
            success = true,
            roundId = id.toString(),
            location = LocationData(
                id = id,
                lat = lat,
                lng = lng,
                pano_id = pano_id,
                country = location_hint,
                city = location_hint,
                difficulty = 2
            )
        )
    }
}

@Serializable
data class GuessRequest(
    val roundId: String,
    val guessLat: Double,
    val guessLng: Double,
    val timeSpentSeconds: Int = 0
)

@Serializable
data class ScoreResponse(
    val success: Boolean,
    val distanceMeters: Double,
    val score: Int,
    val maxScore: Int = 5000,
    val actualLocation: LocationData,
    val message: String? = null
)

@Serializable
data class GameSession(
    val sessionId: String,
    val rounds: List<GameRound> = emptyList(),
    val totalScore: Int = 0,
    val isCompleted: Boolean = false
)

@Serializable
data class GameRound(
    val roundId: String,
    val location: LocationData,
    val guess: GuessLocation? = null,
    val score: Int = 0,
    val distanceMeters: Double = 0.0,
    val isCompleted: Boolean = false
)

@Serializable
data class GuessLocation(
    val lat: Double,
    val lng: Double,
    val timeSpentSeconds: Int
)
