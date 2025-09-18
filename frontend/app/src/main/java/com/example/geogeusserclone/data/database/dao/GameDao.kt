/**
 * GameDao.kt
 *
 * Diese Datei definiert das Data Access Object (DAO) für Spiel-Entitäten in der Room-Datenbank.
 * Sie abstrahiert alle datenbankbezogenen Operationen für Spiele und bietet eine typsichere
 * API für CRUD-Operationen.
 *
 * Architektur-Integration:
 * - Data Access Layer: Room-basierte Datenbankoperationen für Spiele
 * - Repository Pattern: Wird von Repositories für lokale Datenpersistierung verwendet
 * - Reactive Programming: Flow-basierte Observables für UI-Updates
 * - Type Safety: Compile-Zeit-Validierung aller SQL-Abfragen
 * - Performance: Optimierte Abfragen mit Indizes und Paging-Support
 */
package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GameEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO für Spiel-Entitäten
 *
 * Definiert alle verfügbaren Datenbankoperationen für Spiele:
 * - CRUD-Operationen (Create, Read, Update, Delete)
 * - Abfragen für Benutzer-spezifische Spiele
 * - Statistik-Abfragen für Leaderboards und Analytics
 * - Reactive Queries mit Flow für automatische UI-Updates
 * - Performance-optimierte Abfragen mit Limiting und Sorting
 */
@Dao
interface GameDao {

    /**
     * Lädt ein Spiel anhand seiner eindeutigen ID
     *
     * @param gameId Eindeutige ID des Spiels
     * @return GameEntity oder null wenn nicht gefunden
     */
    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: String): GameEntity?

    /**
     * Lädt alle Spiele eines Benutzers (reaktiv)
     *
     * Gibt einen Flow zurück der automatisch aktualisiert wird wenn
     * sich die Spiele-Daten in der Datenbank ändern. Sortiert nach
     * Erstellungsdatum (neueste zuerst).
     *
     * @param userId ID des Benutzers
     * @return Flow mit Liste aller Benutzer-Spiele
     */
    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGamesByUser(userId: String): Flow<List<GameEntity>>

    /**
     * Lädt das aktuelle (nicht abgeschlossene) Spiel eines Benutzers
     *
     * Sucht nach laufenden Spielen die noch nicht abgeschlossen sind.
     * Nützlich für Session-Wiederherstellung nach App-Neustart.
     *
     * @param userId ID des Benutzers
     * @return Aktuelles GameEntity oder null wenn keines läuft
     */
    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentGameForUser(userId: String): GameEntity?

    /**
     * Lädt die besten Spiele aller Benutzer (Leaderboard)
     *
     * Sortiert abgeschlossene Spiele nach Punktestand und gibt die
     * Top-Ergebnisse zurück. Basis für globale Bestenlisten.
     *
     * @param limit Maximale Anzahl Ergebnisse
     * @return Liste der besten Spiele sortiert nach Score
     */
    @Query("SELECT * FROM games WHERE isCompleted = 1 ORDER BY score DESC LIMIT :limit")
    suspend fun getTopGames(limit: Int): List<GameEntity>

    /**
     * Lädt die besten Spiele eines spezifischen Benutzers
     *
     * Persönliche Bestenliste für einen Benutzer. Ermöglicht
     * Fortschritts-Tracking und persönliche Rekorde.
     *
     * @param userId ID des Benutzers
     * @param limit Maximale Anzahl Ergebnisse
     * @return Liste der besten Benutzer-Spiele sortiert nach Score
     */
    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 1 ORDER BY score DESC LIMIT :limit")
    suspend fun getTopGamesByUser(userId: String, limit: Int): List<GameEntity>

    /**
     * Zählt die Gesamtanzahl der Spiele eines Benutzers
     *
     * Einschließlich abgeschlossener und laufender Spiele.
     * Basis für Benutzer-Statistiken.
     *
     * @param userId ID des Benutzers
     * @return Gesamtanzahl der Spiele
     */
    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId")
    suspend fun getGameCountByUser(userId: String): Int

    /**
     * Zählt die abgeschlossenen Spiele eines Benutzers
     *
     * Nur erfolgreich beendete Spiele werden gezählt.
     * Wichtig für Erfolgs-Statistiken und Achievements.
     *
     * @param userId ID des Benutzers
     * @return Anzahl abgeschlossener Spiele
     */
    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedGameCountByUser(userId: String): Int

    /**
     * Berechnet den Durchschnitts-Score eines Benutzers
     *
     * Statistik-Funktion für Performance-Analyse. Berücksichtigt
     * nur abgeschlossene Spiele für genaue Durchschnittswerte.
     *
     * @param userId ID des Benutzers
     * @return Durchschnitts-Score oder null wenn keine Spiele vorhanden
     */
    @Query("SELECT AVG(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getAverageScoreByUser(userId: String): Double?

    /**
     * Ermittelt den besten Score eines Benutzers
     *
     * Persönlicher Rekord für Achievements und Fortschritts-Anzeige.
     * Basis für Bestleistungs-Tracking.
     *
     * @param userId ID des Benutzers
     * @return Höchster Score oder null wenn keine Spiele vorhanden
     */
    @Query("SELECT MAX(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getBestScoreByUser(userId: String): Int?

    /**
     * Lädt Spiele innerhalb eines Zeitraums
     *
     * Ermöglicht zeitbasierte Analysen und Berichte.
     * Nützlich für tägliche/wöchentliche/monatliche Statistiken.
     *
     * @param startTime Start-Zeitstempel (Unix-Zeit)
     * @param endTime End-Zeitstempel (Unix-Zeit)
     * @return Liste der Spiele im angegebenen Zeitraum
     */
    @Query("SELECT * FROM games WHERE createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getGamesByDateRange(startTime: Long, endTime: Long): List<GameEntity>

    /**
     * Fügt ein neues Spiel zur Datenbank hinzu
     *
     * Verwendet REPLACE-Strategie um Updates bei ID-Konflikten zu ermöglichen.
     * Ermöglicht sowohl das Erstellen neuer als auch das Aktualisieren existierender Spiele.
     *
     * @param game Das einzufügende GameEntity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    /**
     * Fügt mehrere Spiele in einer Transaktion hinzu
     *
     * Batch-Operation für bessere Performance bei mehreren Einfügungen.
     * Atomic Operation - entweder alle oder keine Spiele werden eingefügt.
     *
     * @param games Liste der einzufügenden GameEntity-Objekte
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    /**
     * Aktualisiert ein existierendes Spiel
     *
     * Überschreibt alle Felder des Spiels mit den neuen Werten.
     * Die ID muss mit einem existierenden Datensatz übereinstimmen.
     *
     * @param game Das zu aktualisierende GameEntity
     */
    @Update
    suspend fun updateGame(game: GameEntity)

    /**
     * Löscht ein Spiel aus der Datenbank
     *
     * Entfernt das Spiel und alle zugehörigen Daten permanent.
     * Sollte mit Vorsicht verwendet werden, da keine Rückgängig-Option existiert.
     *
     * @param game Das zu löschende GameEntity
     */
    @Delete
    suspend fun deleteGame(game: GameEntity)

    /**
     * Löscht alte Spiele zur Datenbankbereinigung
     *
     * Maintenance-Funktion zur Reduzierung der Datenbankgröße.
     * Entfernt Spiele die älter als der angegebene Zeitpunkt sind.
     *
     * @param cutoffTime Zeitpunkt vor dem alle Spiele gelöscht werden
     * @return Anzahl der gelöschten Spiele
     */
    @Query("DELETE FROM games WHERE createdAt < :cutoffTime")
    suspend fun deleteOldGames(cutoffTime: Long): Int

    /**
     * Löscht unvollständige Spiele
     *
     * Bereinigungsfunktion für abgebrochene oder hängende Spielsitzungen.
     * Entfernt Spiele die nicht ordnungsgemäß abgeschlossen wurden.
     *
     * @return Anzahl der gelöschten unvollständigen Spiele
     */
    @Query("DELETE FROM games WHERE isCompleted = 0 AND createdAt < :cutoffTime")
    suspend fun deleteIncompleteGames(cutoffTime: Long): Int
}
