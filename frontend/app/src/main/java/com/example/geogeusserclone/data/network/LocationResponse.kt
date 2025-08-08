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
    val locations: List<LocationResponse>,
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 10
)
