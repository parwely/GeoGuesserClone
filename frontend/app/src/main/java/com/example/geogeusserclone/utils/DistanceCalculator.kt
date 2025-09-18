/**
 * DistanceCalculator.kt
 *
 * Diese Datei enthält Utility-Funktionen für präzise geografische Distanzberechnungen.
 * Sie implementiert die Haversine-Formel für genaue Entfernungsmessungen zwischen
 * Koordinaten auf der Erdkugel.
 *
 * Architektur-Integration:
 * - Utility Layer: Wiederverwendbare mathematische Funktionen
 * - Score Calculation: Basis für Punkteberechnung basierend auf Genauigkeit
 * - Game Logic: Zentrale Komponente für Guess-Bewertung
 * - Performance: Optimierte Berechnungen für häufige Operationen
 */
package com.example.geogeusserclone.utils

import kotlin.math.*

/**
 * Utility-Klasse für präzise geografische Distanzberechnungen
 *
 * Diese Klasse stellt Funktionen zur Berechnung von Entfernungen zwischen
 * geografischen Koordinaten bereit. Sie berücksichtigt die Krümmung der Erde
 * für maximale Genauigkeit.
 *
 * Verwendung:
 * - Bewertung von Benutzer-Guesses im Spiel
 * - Berechnung von Score basierend auf Genauigkeit
 * - Validierung von Location-Daten
 * - Performance-Metriken für Spiel-Analytics
 */
object DistanceCalculator {

    /** Erdradius in Kilometern (WGS84 Ellipsoid mittlerer Radius) */
    private const val EARTH_RADIUS_KM = 6371.0

    /** Erdradius in Metern für höhere Präzision */
    private const val EARTH_RADIUS_M = 6371000.0

    /**
     * Berechnet die Großkreisentfernung zwischen zwei Koordinaten mittels Haversine-Formel
     *
     * Die Haversine-Formel ist ein spezieller Fall der allgemeinen sphärischen
     * Trigonometrie und berechnet die kürzeste Entfernung zwischen zwei Punkten
     * auf einer Kugel (in diesem Fall der Erde).
     *
     * Genauigkeit: ±0.3% für die meisten Anwendungsfälle
     * Performance: Optimiert für häufige Berechnungen
     *
     * @param lat1 Latitude des ersten Punkts in Dezimalgrad
     * @param lng1 Longitude des ersten Punkts in Dezimalgrad
     * @param lat2 Latitude des zweiten Punkts in Dezimalgrad
     * @param lng2 Longitude des zweiten Punkts in Dezimalgrad
     * @return Distanz in Kilometern als Double
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        // Validiere Eingabeparameter
        require(lat1 in -90.0..90.0) { "Latitude 1 muss zwischen -90 und 90 Grad liegen" }
        require(lat2 in -90.0..90.0) { "Latitude 2 muss zwischen -90 und 90 Grad liegen" }
        require(lng1 in -180.0..180.0) { "Longitude 1 muss zwischen -180 und 180 Grad liegen" }
        require(lng2 in -180.0..180.0) { "Longitude 2 muss zwischen -180 und 180 Grad liegen" }

        // Konvertiere Grad zu Radiant für trigonometrische Funktionen
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        // Haversine-Formel: a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(radLat1) * cos(radLat2) *
                sin(dLng / 2) * sin(dLng / 2)

        // Berechne den Winkel: c = 2 ⋅ atan2(√a, √(1−a))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Distanz = Radius × Winkel
        return EARTH_RADIUS_KM * c
    }

    /**
     * Berechnet die Distanz in Metern für höhere Präzision
     *
     * Diese Variante gibt das Ergebnis in Metern zurück, was für
     * Score-Berechnungen und detaillierte Distanz-Anzeigen nützlich ist.
     *
     * @param lat1 Latitude des ersten Punkts in Dezimalgrad
     * @param lng1 Longitude des ersten Punkts in Dezimalgrad
     * @param lat2 Latitude des zweiten Punkts in Dezimalgrad
     * @param lng2 Longitude des zweiten Punkts in Dezimalgrad
     * @return Distanz in Metern als Double
     */
    fun calculateDistanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        return calculateDistance(lat1, lng1, lat2, lng2) * 1000.0
    }

    /**
     * Berechnet den Bearing (Kompassrichtung) vom ersten zum zweiten Punkt
     *
     * Der Bearing ist der Winkel (in Grad) vom Norden im Uhrzeigersinn
     * zur Richtung des Zielpunkts.
     *
     * @param lat1 Latitude des Startpunkts in Dezimalgrad
     * @param lng1 Longitude des Startpunkts in Dezimalgrad
     * @param lat2 Latitude des Zielpunkts in Dezimalgrad
     * @param lng2 Longitude des Zielpunkts in Dezimalgrad
     * @return Bearing in Grad (0-360°), wobei 0° = Norden
     */
    fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLng = Math.toRadians(lng2 - lng1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val y = sin(dLng) * cos(radLat2)
        val x = cos(radLat1) * sin(radLat2) - sin(radLat1) * cos(radLat2) * cos(dLng)

        val bearing = Math.toDegrees(atan2(y, x))

        // Normalisiere auf 0-360°
        return (bearing + 360) % 360
    }

    /**
     * Prüft ob ein Punkt innerhalb eines Kreises um einen anderen Punkt liegt
     *
     * Nützlich für:
     * - Prüfung ob Guess innerhalb eines "perfekten" Radius liegt
     * - Validierung von Location-Clustern
     * - Performance-Optimierungen (Quick-Check vor genauer Berechnung)
     *
     * @param centerLat Latitude des Kreismittelpunkts
     * @param centerLng Longitude des Kreismittelpunkts
     * @param pointLat Latitude des zu prüfenden Punkts
     * @param pointLng Longitude des zu prüfenden Punkts
     * @param radiusKm Radius des Kreises in Kilometern
     * @return true wenn der Punkt innerhalb des Kreises liegt
     */
    fun isWithinRadius(
        centerLat: Double, centerLng: Double,
        pointLat: Double, pointLng: Double,
        radiusKm: Double
    ): Boolean {
        val distance = calculateDistance(centerLat, centerLng, pointLat, pointLng)
        return distance <= radiusKm
    }

    /**
     * Formatiert eine Distanz für benutzerfreundliche Anzeige
     *
     * Wählt automatisch die beste Einheit (m, km) basierend auf der Größe
     * und rundet sinnvoll für eine gute Benutzererfahrung.
     *
     * @param distanceKm Distanz in Kilometern
     * @return Formatierter String mit Einheit (z.B. "1.2 km", "350 m")
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 0.001 -> "< 1 m"
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> "${"%.1f".format(distanceKm)} km"
            distanceKm < 1000.0 -> "${distanceKm.toInt()} km"
            else -> "${"%.0f".format(distanceKm)} km"
        }
    }

    /**
     * Berechnet die mittlere Position zwischen mehreren Koordinaten
     *
     * Nützlich für:
     * - Berechnung von Karten-Zentren bei mehreren Punkten
     * - Cluster-Algorithmen für Location-Gruppen
     * - Statistische Analysen von Guess-Patterns
     *
     * @param coordinates Liste von Koordinaten-Paaren (Lat, Lng)
     * @return Pair mit mittlerer Latitude und Longitude
     */
    fun calculateCentroid(coordinates: List<Pair<Double, Double>>): Pair<Double, Double> {
        require(coordinates.isNotEmpty()) { "Koordinaten-Liste darf nicht leer sein" }

        if (coordinates.size == 1) {
            return coordinates.first()
        }

        // Konvertiere zu kartesischen Koordinaten für genauere Mittelwertberechnung
        var x = 0.0
        var y = 0.0
        var z = 0.0

        coordinates.forEach { (lat, lng) ->
            val radLat = Math.toRadians(lat)
            val radLng = Math.toRadians(lng)

            x += cos(radLat) * cos(radLng)
            y += cos(radLat) * sin(radLng)
            z += sin(radLat)
        }

        val count = coordinates.size
        x /= count
        y /= count
        z /= count

        // Konvertiere zurück zu geografischen Koordinaten
        val centralLng = atan2(y, x)
        val centralSquareRoot = sqrt(x * x + y * y)
        val centralLat = atan2(z, centralSquareRoot)

        return Pair(
            Math.toDegrees(centralLat),
            Math.toDegrees(centralLng)
        )
    }

    /**
     * Berechnet die Standard-Abweichung von Distanzen zu einem Mittelpunkt
     *
     * Nützlich für:
     * - Analyse der Guess-Genauigkeit über mehrere Runden
     * - Schwierigkeitsgrad-Bestimmung für Locations
     * - Qualitäts-Metriken für Location-Sets
     *
     * @param center Mittelpunkt-Koordinaten (Lat, Lng)
     * @param points Liste von Punkt-Koordinaten
     * @return Standard-Abweichung der Distanzen in Kilometern
     */
    fun calculateDistanceStandardDeviation(
        center: Pair<Double, Double>,
        points: List<Pair<Double, Double>>
    ): Double {
        if (points.isEmpty()) return 0.0

        val distances = points.map { (lat, lng) ->
            calculateDistance(center.first, center.second, lat, lng)
        }

        val mean = distances.average()
        val variance = distances.map { (it - mean).pow(2) }.average()

        return sqrt(variance)
    }
}
