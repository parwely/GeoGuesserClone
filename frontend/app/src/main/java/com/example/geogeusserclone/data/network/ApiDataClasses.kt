package com.example.geogeusserclone.data.network

import kotlinx.serialization.Serializable

@Serializable
data class StreetViewAvailabilityResponse(
    val streetViewAvailable: Boolean,
    val googleMapsAvailable: Boolean,
    val mapillaryAvailable: Boolean,
    val staticFallbackAvailable: Boolean
)

@Serializable
data class LocationDistanceResponse(
    val distance: LocationDistanceData
)

@Serializable
data class LocationDistanceData(
    val location1: LocationDistancePoint,
    val location2: LocationDistancePoint,
    val distanceKm: Double
)

@Serializable
data class LocationDistancePoint(
    val id: Int,
    val lat: Double,
    val lng: Double
)

@Serializable
data class SessionCreateRequest(
    val mode: String,
    val settings: Map<String, String>
)

@Serializable
data class SessionCreateResponse(
    val sessionId: String,
    val settings: Map<String, String>
)

@Serializable
data class SessionJoinRequest(
    val sessionId: String
)

@Serializable
data class SessionJoinResponse(
    val sessionId: String,
    val players: List<PlayerInfo>
)

@Serializable
data class SessionInfoResponse(
    val sessionId: String,
    val players: List<PlayerInfo>,
    val status: String,
    val settings: Map<String, String>
)

@Serializable
data class PlayerInfo(
    val userId: String,
    val username: String
)
