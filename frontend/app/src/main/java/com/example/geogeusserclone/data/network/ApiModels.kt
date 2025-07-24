// Neue Datei: app/src/main/java/com/example/geogeusserclone/data/network/ApiModels.kt
package com.example.geogeusserclone.data.network

data class LocationResponse(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String,
    val country: String? = null,
    val city: String? = null,
    val difficulty: Int = 1
)

data class LocationsResponse(
    val locations: List<LocationResponse>
)