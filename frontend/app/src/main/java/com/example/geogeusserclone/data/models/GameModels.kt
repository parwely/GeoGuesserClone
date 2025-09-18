/**
 * GameModels.kt
 *
 * Diese Datei enthält alle zentralen Datenmodelle für die Spiellogik der GeoGuess-App.
 * Sie definiert die Strukturen für Locations, Runden, Scores und API-Responses.
 *
 * Architektur-Integration:
 * - Data Layer: Zentrale Datenstrukturen für die gesamte App
 * - API Integration: Mapping zwischen Backend-Responses und App-Modellen
 * - Serialization: Kotlinx.serialization für JSON-Konvertierung
 * - Type Safety: Typsichere Datenübertragung zwischen Schichten
 * - Backwards Compatibility: Unterstützung für verschiedene API-Versionen
 */
package com.example.geogeusserclone.data.models

import kotlinx.serialization.Serializable

/**
 * Datenmodell für eine geografische Location
 *
 * Repräsentiert eine spielbare Location mit allen notwendigen Metadaten.
 * Wird sowohl für Backend-Responses als auch interne Datenverarbeitung verwendet.
 *
 * @property id Eindeutige ID der Location im Backend
 * @property lat Latitude (Breitengrad) der Location
 * @property lng Longitude (Längengrad) der Location
 * @property pano_id Google Street View Panorama-ID (optional)
 * @property country Land der Location (für Hinweise und Kategorisierung)
 * @property city Stadt/Ort der Location (für detaillierte Beschreibung)
 * @property difficulty Schwierigkeitsgrad 1-5 (1=leicht, 5=sehr schwer)
 */
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

/**
 * Response-Modell für eine neue Spielrunde
 *
 * Standardisierte Antwort des Backends beim Start einer neuen Runde.
 * Enthält alle Informationen die für die Darstellung einer Runde benötigt werden.
 *
 * @property success Ob die Runden-Erstellung erfolgreich war
 * @property roundId Eindeutige ID dieser Runde für spätere Score-Submission
 * @property location Vollständige Location-Daten für diese Runde
 * @property message Optional: Zusätzliche Nachrichten vom Backend
 */
@Serializable
data class NewRoundResponse(
    val success: Boolean = true, // Default to true for backward compatibility
    val roundId: String,
    val location: LocationData,
    val message: String? = null
)

/**
 * Backend-spezifische Runden-Response
 *
 * Direktes Mapping der Backend-API-Antwort ohne Wrapper-Struktur.
 * Wird intern zu NewRoundResponse konvertiert für einheitliche Verarbeitung.
 *
 * @property id Backend-ID der Location
 * @property lat Latitude der Location
 * @property lng Longitude der Location
 * @property pano_id Optional: Google Street View Panorama-ID
 * @property location_hint Textuelle Beschreibung der Location
 */
@Serializable
data class BackendRoundResponse(
    val id: Int,
    val lat: Double,
    val lng: Double,
    val pano_id: String? = null,
    val location_hint: String? = null
) {
    /**
     * Konvertiert Backend-Response zu standardisiertem NewRoundResponse
     *
     * Mapping-Funktion die die Backend-spezifische Struktur in das
     * App-interne Format übersetzt. Extrahiert Land/Stadt-Informationen
     * aus location_hint und setzt sinnvolle Defaults.
     *
     * @return Konvertierte NewRoundResponse mit mappedten Daten
     */
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

/**
 * Anfrage-Modell für einen Rateversuch
 *
 * Repräsentiert die Daten die vom Client gesendet werden um einen Rateversuch
 * für eine bestimmte Runde einzureichen. Enthält die geschätzten Koordinaten
 * sowie die benötigte Zeit.
 *
 * @property roundId Eindeutige ID der Runde die geraten wird
 * @property guessLat Geschätzte Breite der Location
 * @property guessLng Geschätzte Länge der Location
 * @property timeSpentSeconds Benötigte Zeit für den Rateversuch in Sekunden
 */
@Serializable
data class GuessRequest(
    val roundId: String,
    val guessLat: Double,
    val guessLng: Double,
    val timeSpentSeconds: Int = 0
)

/**
 * Response-Modell für das Ergebnis eines Rateversuchs
 *
 * Standardisierte Antwort des Backends nach einem Rateversuch.
 * Enthält die tatsächliche Entfernung zur gesuchten Location sowie den Score.
 *
 * @property success Ob die Score-Berechnung erfolgreich war
 * @property distanceMeters Tatsächliche Entfernung zur gesuchten Location in Metern
 * @property score Erzielter Punktestand für den Rateversuch
 * @property maxScore Maximaler möglicher Score (Alias für maxPossibleScore)
 * @property actualLocation Tatsächliche Location-Daten für die gesuchte Location
 * @property message Optional: Zusätzliche Nachrichten vom Backend
 */
@Serializable
data class ScoreResponse(
    val success: Boolean = true, // KORRIGIERT: Default auf true für Backend-Kompatibilität
    val distanceMeters: Double,
    val score: Int,
    val maxScore: Int = 5000, // KORRIGIERT: Alias für maxPossibleScore
    val actualLocation: LocationData,
    val message: String? = null
)

/**
 * Backend-spezifische Response für die Score-Berechnung
 *
 * Direktes Mapping der Backend-API-Antwort für die Score-Berechnung.
 * Enthält die tatsächliche Location in einem separaten Objekt.
 *
 * @property distanceMeters Tatsächliche Entfernung zur gesuchten Location in Metern
 * @property score Erzielter Punktestand für den Rateversuch
 * @property actual Tatsächliche Location-Daten für die gesuchte Location
 * @property maxPossibleScore Maximaler möglicher Score
 */
@Serializable
data class BackendScoreResponse(
    val distanceMeters: Double,
    val score: Int,
    val actual: BackendActualLocation,
    val maxPossibleScore: Int = 5000
) {
    /**
     * Konvertiert Backend-Score-Response zu erwarteten ScoreResponse Format
     *
     * Mapping-Funktion die die Backend-spezifische Score-Response in das
     * App-interne Format übersetzt. Setzt sinnvolle Defaults für fehlende
     * Informationen und konvertiert die tatsächliche Location.
     *
     * @return Konvertierte ScoreResponse mit mappedten Daten
     */
    fun toScoreResponse(): ScoreResponse {
        return ScoreResponse(
            success = true,
            distanceMeters = distanceMeters,
            score = score,
            maxScore = maxPossibleScore,
            actualLocation = LocationData(
                id = 0, // Backend doesn't provide ID in score response
                lat = actual.lat,
                lng = actual.lng,
                country = actual.country,
                city = actual.name, // Use name as city
                difficulty = 2 // Default difficulty
            )
        )
    }
}

/**
 * Datenmodell für die tatsächliche Location in der Backend-Score-Response
 *
 * Enthält die Koordinaten und zusätzliche Informationen der tatsächlichen Location
 * die für die Score-Berechnung verwendet wird.
 *
 * @property lat Latitude (Breitengrad) der tatsächlichen Location
 * @property lng Longitude (Längengrad) der tatsächlichen Location
 * @property name Name der Stadt/Ort der tatsächlichen Location
 * @property country Land der tatsächlichen Location
 */
@Serializable
data class BackendActualLocation(
    val lat: Double,
    val lng: Double,
    val name: String,
    val country: String
)

/**
 * Datenmodell für eine Spielsitzung
 *
 * Repräsentiert eine gesamte Spielsitzung mit mehreren Runden.
 * Enthält den aktuellen Punktestand und den Abschlussstatus.
 *
 * @property sessionId Eindeutige ID der Spielsitzung
 * @property rounds Liste der Spielrunden in dieser Sitzung
 * @property totalScore Gesamtpunktestand der Sitzung
 * @property isCompleted Ob die Sitzung abgeschlossen ist
 */
@Serializable
data class GameSession(
    val sessionId: String,
    val rounds: List<GameRound> = emptyList(),
    val totalScore: Int = 0,
    val isCompleted: Boolean = false
)

/**
 * Datenmodell für eine Spielrunde in der Spielsitzung
 *
 * Enthält alle relevanten Daten für eine Runde innerhalb einer Spielsitzung.
 * Dazu gehören die Location-Daten, die Benutzereingaben und das Ergebnis.
 *
 * @property roundId Eindeutige ID der Runde
 * @property location Location-Daten für die Runde
 * @property guess Benutzergeratene Location-Daten (falls vorhanden)
 * @property score Erzielter Punktestand für die Runde
 * @property distanceMeters Entfernung zur tatsächlichen Location in Metern
 * @property isCompleted Ob die Runde abgeschlossen ist
 */
@Serializable
data class GameRound(
    val roundId: String,
    val location: LocationData,
    val guess: GuessLocation? = null,
    val score: Int = 0,
    val distanceMeters: Double = 0.0,
    val isCompleted: Boolean = false
)

/**
 * Datenmodell für die Benutzereingabe in einer Spielrunde
 *
 * Enthält die geschätzten Koordinaten und die benötigte Zeit für einen Rateversuch.
 *
 * @property lat Geschätzte Breite der Location
 * @property lng Geschätzte Länge der Location
 * @property timeSpentSeconds Benötigte Zeit für den Rateversuch in Sekunden
 */
@Serializable
data class GuessLocation(
    val lat: Double,
    val lng: Double,
    val timeSpentSeconds: Int
)
