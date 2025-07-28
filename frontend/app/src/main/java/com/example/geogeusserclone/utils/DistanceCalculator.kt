package com.example.geogeusserclone.utils

import kotlin.math.*

object DistanceCalculator {

    /**
     * Berechnet die Entfernung zwischen zwei Koordinaten using Haversine formula
     */
    fun calculateDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val earthRadius = 6371.0 // Earth radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)

        val c = 2 * asin(sqrt(a))

        return earthRadius * c
    }

    /**
     * Formatiert Entfernung für Display
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> "${"%.1f".format(distanceKm)} km"
            else -> "${distanceKm.toInt()} km"
        }
    }
}
