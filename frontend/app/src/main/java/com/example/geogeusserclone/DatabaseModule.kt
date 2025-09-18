/**
 * DatabaseModule.kt
 *
 * Diese Datei konfiguriert die Room-Datenbank für die GeoGuess-App.
 * Sie stellt alle Datenbank-Komponenten über Hilt Dependency Injection bereit
 * und verwaltet Datenbankmigrationen für Schema-Updates.
 *
 * Architektur-Integration:
 * - Database Layer: Room-ORM für lokale Datenpersistierung
 * - Dependency Injection: Hilt-basierte Bereitstellung aller DAOs
 * - Migration Management: Automatische Schema-Updates zwischen App-Versionen
 * - Performance: Optimierte Indizes und Konfigurationen
 * - Threading: Background-Thread-Konfiguration für DB-Operationen
 */
package com.example.geogeusserclone

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.geogeusserclone.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * Hilt-Modul für Datenbank-Konfiguration
 *
 * Stellt die Room-Datenbank und alle zugehörigen DAOs als Singletons bereit.
 * Konfiguriert Migrationen und Performance-Optimierungen.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migration von Datenbank-Version 1 zu 2
     *
     * Fügt Performance-Indizes für häufige Abfragen hinzu.
     * Optimiert Suchen nach gameId, locationId und Zeitstempeln.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Performance-Indizes für Guess-Tabelle
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_gameId` ON `guesses` (`gameId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_locationId` ON `guesses` (`locationId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_submittedAt` ON `guesses` (`submittedAt`)")

            // Performance-Indizes für Game-Tabelle
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId` ON `games` (`userId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_createdAt` ON `games` (`createdAt`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_isCompleted` ON `games` (`isCompleted`)")
        }
    }

    /**
     * Migration von Datenbank-Version 2 zu 3
     *
     * Erweitert User- und Location-Tabellen um fehlende Spalten.
     * Fügt Statistik-Felder und Cache-Management hinzu.
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Erweitere Users-Tabelle um Statistik-Felder
            database.execSQL("ALTER TABLE users ADD COLUMN totalScore INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN gamesPlayed INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN bestScore INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN lastLoginAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")

            // Erweitere Locations-Tabelle um Usage-Tracking
            database.execSQL("ALTER TABLE locations ADD COLUMN isUsed INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Migration von Datenbank-Version 3 zu 4
     *
     * Fügt erweiterte Performance-Indizes und Query-Optimierungen hinzu.
     * Optimiert für häufige App-Operationen wie Location-Suche und Benutzer-Auth.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Composite-Indizes für komplexe Location-Abfragen
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_isUsed_isCached` ON `locations` (`isUsed`, `isCached`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_difficulty` ON `locations` (`difficulty`)")

            // Authentifizierungs-Performance-Index
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_users_authToken` ON `users` (`authToken`)")

            // Game-Performance-Indizes für Statistiken
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId_isCompleted` ON `games` (`userId`, `isCompleted`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_score` ON `games` (`score`)")
        }
    }

    /**
     * Migration von Datenbank-Version 4 zu 5
     *
     * Erweitert Schema für neue Spielmodi (Blitz, Endless) und
     * fügt zusätzliche Metadaten-Felder hinzu.
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Erweitere Games-Tabelle für neue Spielmodi
            database.execSQL("ALTER TABLE games ADD COLUMN gameMode TEXT NOT NULL DEFAULT 'CLASSIC'")
            database.execSQL("ALTER TABLE games ADD COLUMN roundTimeLimit INTEGER")
            database.execSQL("ALTER TABLE games ADD COLUMN streak INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE games ADD COLUMN maxRounds INTEGER NOT NULL DEFAULT 5")

            // Erweitere Locations-Tabelle um Metadaten
            database.execSQL("ALTER TABLE locations ADD COLUMN category TEXT")
            database.execSQL("ALTER TABLE locations ADD COLUMN lastUsedAt INTEGER")

            // Performance-Indizes für neue Felder
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_gameMode` ON `games` (`gameMode`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_streak` ON `games` (`streak`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_category` ON `locations` (`category`)")
        }
    }

    /**
     * Stellt die konfigurierte Room-Datenbank als Singleton bereit
     *
     * Konfiguriert die Datenbank mit allen Migrationen, Performance-Optimierungen
     * und Background-Threading für sichere UI-Operationen.
     *
     * @param context Application Context für Datenbankdatei-Zugriff
     * @return Konfigurierte AppDatabase-Instanz
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "geoguess_database"
        )
        // Alle Migrationen für nahtlose Schema-Updates
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )
        // Performance-Optimierungen
        .setQueryExecutor(Executors.newFixedThreadPool(4)) // 4 Background-Threads für DB-Operationen
        .enableMultiInstanceInvalidation() // Sync zwischen mehreren DB-Instanzen
        // Entwicklungs-Hilfsmittel (nur in Debug-Builds)
        //.setQueryCallback({ sqlQuery, bindArgs ->
        //    println("SQL Query: $sqlQuery")
        //}, Executors.newSingleThreadExecutor())
        // Fallback-Strategien (Vorsicht in Production!)
        // .fallbackToDestructiveMigration() // Nur für Development
        .build()
    }

    /**
     * Stellt UserDao als Singleton bereit
     *
     * @param appDatabase Die konfigurierte Datenbank-Instanz
     * @return UserDao für Benutzer-Operationen
     */
    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): com.example.geogeusserclone.data.database.dao.UserDao {
        return appDatabase.userDao()
    }

    /**
     * Stellt LocationDao als Singleton bereit
     *
     * @param appDatabase Die konfigurierte Datenbank-Instanz
     * @return LocationDao für Location-Operationen
     */
    @Provides
    @Singleton
    fun provideLocationDao(appDatabase: AppDatabase): com.example.geogeusserclone.data.database.dao.LocationDao {
        return appDatabase.locationDao()
    }

    /**
     * Stellt GameDao als Singleton bereit
     *
     * @param appDatabase Die konfigurierte Datenbank-Instanz
     * @return GameDao für Spiel-Operationen
     */
    @Provides
    @Singleton
    fun provideGameDao(appDatabase: AppDatabase): com.example.geogeusserclone.data.database.dao.GameDao {
        return appDatabase.gameDao()
    }

    /**
     * Stellt GuessDao als Singleton bereit
     *
     * @param appDatabase Die konfigurierte Datenbank-Instanz
     * @return GuessDao für Guess-Operationen
     */
    @Provides
    @Singleton
    fun provideGuessDao(appDatabase: AppDatabase): com.example.geogeusserclone.data.database.dao.GuessDao {
        return appDatabase.guessDao()
    }
}
