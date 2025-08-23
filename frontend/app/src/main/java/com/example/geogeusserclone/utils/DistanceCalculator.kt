package com.example.geogeusserclone.utils

import kotlin.math.*

/**
 * Utility-Klasse für präzise Distanzberechnungen zwischen Koordinaten
 */
object DistanceCalculator {

    /**
     * Berechnet die Distanz zwischen zwei Koordinaten mittels Haversine-Formel
     * @param lat1 Latitude des ersten Punkts
     * @param lng1 Longitude des ersten Punkts
     * @param lat2 Latitude des zweiten Punkts
     * @param lng2 Longitude des zweiten Punkts
     * @return Distanz in Kilometern
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // Erdradius in Kilometern

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Berechnet die Genauigkeit basierend auf der Distanz
     */
    fun calculateAccuracy(distance: Double): String {
        return when {
            distance <= 1.0 -> "Perfekt"
            distance <= 10.0 -> "Ausgezeichnet"
            distance <= 50.0 -> "Gut"
            distance <= 200.0 -> "Okay"
            distance <= 1000.0 -> "Schlecht"
            else -> "Sehr schlecht"
        }
    }

    /**
     * Formatiert Distanz für die Anzeige
     */
    fun formatDistance(distance: Double): String {
        return when {
            distance < 1.0 -> "${(distance * 1000).toInt()} m"
            distance < 10.0 -> "${"%.1f".format(distance)} km"
            else -> "${distance.toInt()} km"
        }
    }
}
