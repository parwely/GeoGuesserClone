package com.example.geogeusserclone.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface MapillaryApiService {

    /**
     * Sucht 360° Panorama-Bilder in der Nähe einer Koordinate
     */
    @GET("images")
    suspend fun getImagesNearby(
        @Query("bbox") bbox: String, // Format: "lng1,lat1,lng2,lat2"
        @Query("is_pano") isPano: Boolean = true, // Nur 360° Panoramen
        @Query("limit") limit: Int = 10,
        @Query("access_token") accessToken: String
    ): Response<MapillaryImagesResponse>

    /**
     * Holt ein spezifisches Bild mit Download-URL
     */
    @GET("images/{image_id}")
    suspend fun getImageDetails(
        @Path("image_id") imageId: String,
        @Query("access_token") accessToken: String
    ): Response<MapillaryImageDetails>
}

data class MapillaryImagesResponse(
    val data: List<MapillaryImage>,
    val links: MapillaryLinks? = null
)

data class MapillaryImage(
    val id: String,
    val geometry: MapillaryGeometry,
    val is_pano: Boolean,
    val captured_at: String,
    val compass_angle: Double? = null,
    val thumb_256_url: String? = null,
    val thumb_1024_url: String? = null,
    val thumb_2048_url: String? = null
)

data class MapillaryImageDetails(
    val id: String,
    val geometry: MapillaryGeometry,
    val is_pano: Boolean,
    val captured_at: String,
    val compass_angle: Double? = null,
    val thumb_256_url: String? = null,
    val thumb_1024_url: String? = null,
    val thumb_2048_url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

data class MapillaryGeometry(
    val type: String = "Point",
    val coordinates: List<Double> // [lng, lat]
)

data class MapillaryLinks(
    val prev: String? = null,
    val next: String? = null
)
