/**
 * GameApi.kt
 *
 * Diese Datei definiert die Retrofit-API-Schnittstelle für alle spielbezogenen
 * Backend-Operationen der GeoGuess-App. Sie abstrahiert die HTTP-Kommunikation
 * mit dem Backend-Server.
 *
 * Architektur-Integration:
 * - Network Layer: Retrofit-basierte API-Definition für HTTP-Calls
 * - Repository Pattern: Wird von GameRepository verwendet für Datenoperationen
 * - Type Safety: Typisierte Request/Response-Objekte
 * - Asynchronous Operations: Suspend-Funktionen für Coroutine-Integration
 * - Error Handling: Response-Wrapper für explizite Fehlerbehandlung
 */
package com.example.geogeusserclone.data.network

import com.example.geogeusserclone.data.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit-Interface für spielbezogene API-Operationen
 *
 * Definiert alle verfügbaren Endpunkte für Spiellogik und -verwaltung.
 * Alle Methoden sind als Suspend-Funktionen implementiert für asynchrone
 * Ausführung mit Kotlin Coroutines.
 */
interface GameApi {

    /**
     * Startet eine neue Spielrunde
     *
     * Fordert eine neue zufällige Location vom Backend an. Optional können
     * Schwierigkeitsgrad und Kategorie spezifiziert werden um das Spielerlebnis
     * anzupassen.
     *
     * @param difficulty Optional: Schwierigkeitsgrad 1-5 (1=leicht, 5=sehr schwer)
     * @param category Optional: Location-Kategorie (z.B. "urban", "rural", "landmarks")
     * @return Response mit BackendRoundResponse oder HTTP-Fehler
     */
    @GET("game/newRound")
    suspend fun newRound(
        @Query("difficulty") difficulty: Int? = null,
        @Query("category") category: String? = null
    ): Response<BackendRoundResponse>

    /**
     * Übermittelt einen Rateversuch für eine Runde
     *
     * Sendet die Benutzer-Schätzung an das Backend zur Score-Berechnung.
     * Das Backend berechnet die Entfernung zur tatsächlichen Location und
     * den entsprechenden Punktestand.
     *
     * @param guess GuessRequest mit Runden-ID, geschätzten Koordinaten und Zeit
     * @return Response mit BackendScoreResponse oder HTTP-Fehler
     */
    @POST("game/guess")
    suspend fun submitGuess(@Body guess: GuessRequest): Response<BackendScoreResponse> // KORRIGIERT: Verwende BackendScoreResponse

    /**
     * Lädt eine existierende Spielsitzung
     *
     * Ruft alle Daten einer Spielsitzung ab, inklusive aller Runden
     * und des aktuellen Stands. Nützlich für Session-Wiederherstellung.
     *
     * @param sessionId Eindeutige ID der Spielsitzung
     * @return Response mit GameSession-Daten oder HTTP-Fehler
     */
    @GET("game/session/{sessionId}")
    suspend fun getGameSession(@Path("sessionId") sessionId: String): Response<GameSession>

    /**
     * Erstellt eine neue Spielsitzung
     *
     * Initialisiert eine neue Spielsitzung auf dem Backend.
     * Gibt eine Session-ID zurück die für nachfolgende Operationen verwendet wird.
     *
     * @return Response mit neuer GameSession oder HTTP-Fehler
     */
    @POST("game/session")
    suspend fun createGameSession(): Response<GameSession>

    /**
     * Prüft Street View-Verfügbarkeit für eine Location
     *
     * Backend-seitige Validierung ob für eine spezifische Location
     * Google Street View-Daten verfügbar sind. Vermeidet Client-seitige
     * Street View-Ladeprobleme durch Vorab-Prüfung.
     *
     * @param locationId Backend-ID der zu prüfenden Location
     * @return Response mit StreetViewAvailabilityResponse oder HTTP-Fehler
     */
    @GET("game/streetview/check/{locationId}")
    suspend fun checkStreetViewAvailability(@Path("locationId") locationId: Int): Response<StreetViewAvailabilityResponse>

    /**
     * Lädt die Bestenliste
     *
     * Ruft die Top-Spieler-Rangliste vom Backend ab.
     * Die Liste wird nach Punktestand sortiert zurückgegeben.
     *
     * @param limit Maximale Anzahl der zurückzugebenden Einträge (Standard: 10)
     * @return Response mit LeaderboardResponse oder HTTP-Fehler
     */
    @GET("game/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 10): Response<LeaderboardResponse>
}

/**
 * Response-Modell für Street View-Verfügbarkeitsprüfung
 *
 * Enthält die Ergebnisse der Backend-seitigen Street View-Validierung
 * für eine spezifische Location.
 *
 * @property success Ob die Prüfung erfolgreich durchgeführt wurde
 * @property streetViewAvailable Ob Street View für die Location verfügbar ist
 * @property panoId Optional: Google Street View Panorama-ID falls verfügbar
 */
@kotlinx.serialization.Serializable
data class StreetViewAvailabilityResponse(
    val success: Boolean,
    val streetViewAvailable: Boolean,
    val panoId: String? = null
)

/**
 * Response-Modell für die Bestenliste
 *
 * Wrapper-Objekt für die Bestenlisten-Daten vom Backend.
 * Enthält eine Liste der Top-Spieler sortiert nach Punktestand.
 *
 * @property success Ob die Abfrage erfolgreich war
 * @property leaderboard Liste der Bestenlisten-Einträge
 */
@kotlinx.serialization.Serializable
data class LeaderboardResponse(
    val success: Boolean,
    val leaderboard: List<LeaderboardEntry>
)

/**
 * Einzelner Eintrag in der Bestenliste
 *
 * Repräsentiert einen Spieler-Eintrag in der Rangliste mit
 * Name, Punktestand und Zeitstempel der Erreichung.
 *
 * @property playerName Name/Username des Spielers
 * @property score Erreichter Punktestand
 * @property timestamp Unix-Zeitstempel wann der Score erreicht wurde
 */
@kotlinx.serialization.Serializable
data class LeaderboardEntry(
    val playerName: String,
    val score: Int,
    val timestamp: Long
)
