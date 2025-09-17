package com.example.geogeusserclone.data.network

import kotlinx.serialization.Serializable

/**
 * Response-Klassen für das neue Street View Diagnostic API
 */
@Serializable
data class StreetViewDiagnosticResponse(
    val success: Boolean,
    val data: StreetViewDiagnosticData
)

@Serializable
data class StreetViewDiagnosticData(
    // Primary: Interaktive URL (kann fehlschlagen)
    val embedUrl: String,

    // Fallback: Statische URL (funktioniert immer)
    val fallback: String,

    // Empfehlung basierend auf Zuverlässigkeit
    val recommended: String, // "static" oder "interactive"

    // Mehrere Optionen für das Frontend
    val alternatives: StreetViewAlternatives
)

@Serializable
data class StreetViewAlternatives(
    val static: String,
    val iframe: String,
    val mapillary: String? = null // Falls verfügbar
)

/**
 * Request für das Diagnostic API
 */
@Serializable
data class StreetViewDiagnosticRequest(
    val latitude: Double,
    val longitude: Double,
    val heading: Int? = null,
    val pitch: Int? = null,
    val fov: Int? = null,
    val responsive: Boolean = true
)

/**
 * Enum für verschiedene Street View-Modi
 */

/**
 * Konfiguration für Street View-Anzeige
 */
@Serializable
data class StreetViewConfig(
    val mode: String, // "interactive", "static", "fallback_image"
    val url: String,
    val isReliable: Boolean,
    val quality: String, // "high", "medium", "low"
    val hasNavigation: Boolean,
    val errorMessage: String? = null
)
