/**
 * GuessDao.kt
 *
 * Diese Datei definiert das Data Access Object (DAO) für Rateversuch-Entitäten in der Room-Datenbank.
 * Sie verwaltet alle datenbankbezogenen Operationen für Benutzer-Guesses und deren Bewertungen.
 *
 * Architektur-Integration:
 * - Data Access Layer: Room-basierte Datenbankoperationen für Rateversuche
 * - Game Analytics: Detaillierte Tracking-Daten für Spielanalysen
 * - Performance Metrics: Basis für Score-Statistiken und Verbesserungs-Tracking
 * - Relational Queries: Verknüpfung zwischen Spielen, Locations und Guesses
 * - Reactive Programming: Flow-basierte Updates für Live-Statistiken
 */
package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GuessEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO für Rateversuch-Entitäten
 *
 * Verwaltet alle Aspekte der Guess-Datenpersistierung:
 * - Speicherung einzelner Rateversuche mit detaillierten Metadaten
 * - Abruf von Guesses pro Spiel für Rundenauswertung
 * - Statistische Analysen für Benutzer-Performance
 * - Cross-Table-Queries für komplexe Analytics
 * - Batch-Operationen für Performance-Optimierung
 */
@Dao
interface GuessDao {

    /**
     * Lädt einen Rateversuch anhand seiner eindeutigen ID
     *
     * @param guessId Eindeutige ID des Rateversuchs
     * @return GuessEntity oder null wenn nicht gefunden
     */
    @Query("SELECT * FROM guesses WHERE id = :guessId")
    suspend fun getGuessById(guessId: String): GuessEntity?

    /**
     * Lädt alle Rateversuche eines Spiels (reaktiv)
     *
     * Gibt einen Flow zurück für automatische UI-Updates bei Änderungen.
     * Sortiert chronologisch nach Eingabe-Zeitpunkt für korrekte Reihenfolge.
     * Ermöglicht Live-Tracking des Spielfortschritts.
     *
     * @param gameId ID des Spiels
     * @return Flow mit chronologisch sortierten Rateversuchen
     */
    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY submittedAt ASC")
    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>>

    /**
     * Lädt alle Rateversuche eines Spiels (synchron)
     *
     * Synchrone Variante für Batch-Operationen und finale Auswertungen.
     * Nützlich für Score-Berechnungen und Spiel-Abschluss-Logik.
     *
     * @param gameId ID des Spiels
     * @return Liste chronologisch sortierter Rateversuche
     */
    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY submittedAt ASC")
    suspend fun getGuessesByGameSync(gameId: String): List<GuessEntity>

    /**
     * Lädt alle Rateversuche für eine spezifische Location
     *
     * Analytics-Funktion um zu analysieren wie verschiedene Benutzer
     * bei derselben Location abschneiden. Basis für Schwierigkeitsgrad-Kalibrierung.
     *
     * @param locationId ID der Location
     * @return Liste aller Rateversuche für diese Location
     */
    @Query("SELECT * FROM guesses WHERE locationId = :locationId")
    suspend fun getGuessesByLocation(locationId: String): List<GuessEntity>

    /**
     * Berechnet den durchschnittlichen Score eines Benutzers
     *
     * Cross-Table-Query die über Spiele und Guesses joinet um
     * benutzer-spezifische Durchschnittswerte zu berechnen.
     * Grundlage für Skill-Rating und Fortschritts-Tracking.
     *
     * @param userId ID des Benutzers
     * @return Durchschnitts-Score über alle Rateversuche oder null
     */
    @Query("SELECT AVG(score) FROM guesses WHERE gameId IN (SELECT id FROM games WHERE userId = :userId)")
    suspend fun getAverageScoreByUser(userId: String): Double?

    /**
     * Ermittelt den besten Score eines Benutzers
     *
     * Findet den höchsten einzelnen Rateversuch-Score eines Benutzers.
     * Basis für persönliche Rekorde und Achievement-System.
     *
     * @param userId ID des Benutzers
     * @return Höchster Score oder null wenn keine Daten vorhanden
     */
    @Query("SELECT MAX(score) FROM guesses WHERE gameId IN (SELECT id FROM games WHERE userId = :userId)")
    suspend fun getBestScoreByUser(userId: String): Int?

    /**
     * Fügt einen neuen Rateversuch zur Datenbank hinzu
     *
     * Speichert alle Details eines Rateversuchs inklusive Koordinaten,
     * Zeitstempel, Score und Metadaten. Verwendet REPLACE-Strategie
     * für idempotente Operationen.
     *
     * @param guess Das einzufügende GuessEntity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuess(guess: GuessEntity)

    /**
     * Fügt mehrere Rateversuche in einer Transaktion hinzu
     *
     * Batch-Operation für bessere Performance bei Bulk-Einfügungen.
     * Atomic Operation - alle oder keine Guesses werden eingefügt.
     * Nützlich für Offline-Synchronisation und Daten-Import.
     *
     * @param guesses Liste der einzufügenden GuessEntity-Objekte
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuesses(guesses: List<GuessEntity>)

    /**
     * Aktualisiert einen existierenden Rateversuch
     *
     * Ermöglicht nachträgliche Korrekturen oder Anreicherung
     * mit zusätzlichen Metadaten nach Backend-Synchronisation.
     *
     * @param guess Das zu aktualisierende GuessEntity
     */
    @Update
    suspend fun updateGuess(guess: GuessEntity)

    /**
     * Löscht einen Rateversuch aus der Datenbank
     *
     * Entfernt den Guess permanent aus der lokalen Datenbank.
     * Sollte mit Vorsicht verwendet werden da Spiel-Integrität betroffen sein kann.
     *
     * @param guess Das zu löschende GuessEntity
     */
    @Delete
    suspend fun deleteGuess(guess: GuessEntity)

    /**
     * Löscht alle Rateversuche eines Spiels
     *
     * Bereinigungsfunktion die beim Löschen eines Spiels aufgerufen wird.
     * Stellt referentielle Integrität sicher durch Kaskadierung.
     *
     * @param gameId ID des Spiels dessen Guesses gelöscht werden sollen
     */
    @Query("DELETE FROM guesses WHERE gameId = :gameId")
    suspend fun deleteGuessesByGame(gameId: String)

    /**
     * Zählt die Anzahl der Rateversuche in einem Spiel
     *
     * Utility-Funktion zur Validierung der Spiel-Vollständigkeit.
     * Hilft bei der Erkennung von unvollständigen Spielsitzungen.
     *
     * @param gameId ID des Spiels
     * @return Anzahl der Rateversuche in diesem Spiel
     */
    @Query("SELECT COUNT(*) FROM guesses WHERE gameId = :gameId")
    suspend fun getGuessCountByGame(gameId: String): Int

    /**
     * Lädt die detailliertesten Rateversuche für Analytics
     *
     * Erweiterte Abfrage mit Zeit- und Genauigkeits-Metriken.
     * Basis für fortgeschrittene Spieler-Analytics und KI-Training.
     *
     * @param userId ID des Benutzers
     * @param limit Maximale Anzahl Ergebnisse
     * @return Liste detaillierter Guess-Daten für Analyse
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT g.*, 
               ABS(g.distance) as accuracy,
               (g.score * 1.0 / g.timeSpent) as efficiency
        FROM guesses g 
        INNER JOIN games gm ON g.gameId = gm.id 
        WHERE gm.userId = :userId 
        ORDER BY g.submittedAt DESC 
        LIMIT :limit
    """)
    suspend fun getDetailedGuessesByUser(userId: String, limit: Int = 100): List<GuessEntity>

    /**
     * Berechnet Accuracy-Trends für einen Benutzer (vereinfacht)
     *
     * Zeitbasierte Analyse der Verbesserung in der Genauigkeit.
     * Gibt nur die GuessEntity-Daten zurück für weitere Verarbeitung.
     *
     * @param userId ID des Benutzers
     * @param startTime Startzeit für die Trend-Analyse
     * @return Liste der Guesses für Trend-Analyse
     */
    @Query("""
        SELECT g.*
        FROM guesses g
        INNER JOIN games gm ON g.gameId = gm.id
        WHERE gm.userId = :userId 
        AND g.submittedAt >= :startTime
        ORDER BY g.submittedAt ASC
    """)
    suspend fun getGuessesForAccuracyTrend(userId: String, startTime: Long): List<GuessEntity>
}
