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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add indices
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_gameId` ON `guesses` (`gameId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_locationId` ON `guesses` (`locationId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_submittedAt` ON `guesses` (`submittedAt`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId` ON `games` (`userId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_createdAt` ON `games` (`createdAt`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_isCompleted` ON `games` (`isCompleted`)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add missing columns to users table
            database.execSQL("ALTER TABLE users ADD COLUMN totalScore INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN gamesPlayed INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN bestScore INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN lastLoginAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE users ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE locations ADD COLUMN isUsed INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Performance-optimierte Indizes hinzufügen
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_isUsed_isCached` ON `locations` (`isUsed`, `isCached`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_difficulty` ON `locations` (`difficulty`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_users_authToken` ON `users` (`authToken`)")

            // Game Performance Indizes
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId_isCompleted` ON `games` (`userId`, `isCompleted`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_gameId_submittedAt` ON `guesses` (`gameId`, `submittedAt`)")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Version 5: Optimierung für bessere Performance und Memory Management

            // Neue Game Spalten für erweiterte Statistiken
            database.execSQL("ALTER TABLE games ADD COLUMN startedAt INTEGER")
            database.execSQL("ALTER TABLE games ADD COLUMN completedAt INTEGER")
            database.execSQL("ALTER TABLE games ADD COLUMN duration INTEGER")

            // Location Cache Optimierungen
            database.execSQL("ALTER TABLE locations ADD COLUMN cachedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")

            // Performance-kritische Indizes
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_cachedAt` ON `locations` (`cachedAt`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_completedAt` ON `games` (`completedAt`)")

            // Cleanup alter Daten
            database.execSQL("DELETE FROM locations WHERE isCached = 0 AND isUsed = 1 AND cachedAt < ${System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)}")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Version 6: Foreign Key und Offline Mode Fixes

            // Prüfe ob games Tabelle existiert bevor wir sie bearbeiten
            val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='games'")
            val gamesTableExists = cursor.moveToFirst()
            cursor.close()

            if (gamesTableExists) {
                // Temporäre Tabellen erstellen ohne Foreign Key Constraints
                database.execSQL("""
                    CREATE TABLE games_temp (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        gameMode TEXT NOT NULL DEFAULT 'classic',
                        totalRounds INTEGER NOT NULL DEFAULT 5,
                        currentRound INTEGER NOT NULL DEFAULT 1,
                        score INTEGER NOT NULL DEFAULT 0,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        startedAt INTEGER,
                        completedAt INTEGER,
                        duration INTEGER
                    )
                """)

                // Daten kopieren falls Tabelle Daten hat
                try {
                    database.execSQL("""
                        INSERT INTO games_temp 
                        SELECT id, userId, gameMode, totalRounds, currentRound, score, isCompleted, createdAt, startedAt, completedAt, duration 
                        FROM games
                    """)
                } catch (e: Exception) {
                    // Falls Spalten fehlen, erstelle nur Basis-Daten
                    database.execSQL("""
                        INSERT INTO games_temp (id, userId, gameMode, totalRounds, currentRound, score, isCompleted, createdAt)
                        SELECT id, userId, 
                               COALESCE(gameMode, 'classic') as gameMode,
                               COALESCE(totalRounds, 5) as totalRounds,
                               COALESCE(currentRound, 1) as currentRound,
                               COALESCE(score, 0) as score,
                               COALESCE(isCompleted, 0) as isCompleted,
                               COALESCE(createdAt, ${System.currentTimeMillis()}) as createdAt
                        FROM games
                    """)
                }

                // Alte Tabelle löschen und neue umbenennen
                database.execSQL("DROP TABLE games")
                database.execSQL("ALTER TABLE games_temp RENAME TO games")
            } else {
                // Falls games Tabelle nicht existiert, erstelle sie neu
                database.execSQL("""
                    CREATE TABLE games (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        gameMode TEXT NOT NULL DEFAULT 'classic',
                        totalRounds INTEGER NOT NULL DEFAULT 5,
                        currentRound INTEGER NOT NULL DEFAULT 1,
                        score INTEGER NOT NULL DEFAULT 0,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        startedAt INTEGER,
                        completedAt INTEGER,
                        duration INTEGER
                    )
                """)
            }

            // Indizes neu erstellen ohne Foreign Key Constraints
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId` ON `games` (`userId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_isCompleted` ON `games` (`isCompleted`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_createdAt` ON `games` (`createdAt`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_completedAt` ON `games` (`completedAt`)")

            // Prüfe ob users Tabelle existiert
            val usersCursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
            val usersTableExists = usersCursor.moveToFirst()
            usersCursor.close()

            if (!usersTableExists) {
                // Erstelle users Tabelle falls sie nicht existiert
                database.execSQL("""
                    CREATE TABLE users (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        authToken TEXT,
                        totalScore INTEGER NOT NULL DEFAULT 0,
                        gamesPlayed INTEGER NOT NULL DEFAULT 0,
                        bestScore INTEGER NOT NULL DEFAULT 0,
                        lastLoginAt INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                    )
                """)
            }

            // Emergency User erstellen falls nicht vorhanden
            database.execSQL("""
                INSERT OR IGNORE INTO users (id, username, email, authToken, totalScore, gamesPlayed, bestScore, lastLoginAt, createdAt)
                VALUES ('emergency_user', 'Emergency User', 'emergency@local.com', NULL, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """)
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Version 7: Sanfte Bereinigung der Database-Probleme ohne komplettes Löschen

            // Prüfe welche Tabellen existieren
            val gamesCursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='games'")
            val gamesTableExists = gamesCursor.moveToFirst()
            gamesCursor.close()

            val locationsCursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='locations'")
            val locationsTableExists = locationsCursor.moveToFirst()
            locationsCursor.close()

            val guessesCursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='guesses'")
            val guessesTableExists = guessesCursor.moveToFirst()
            guessesCursor.close()

            // Nur problematische games Tabelle neu erstellen (wegen "duration" Spalten-Problem)
            if (gamesTableExists) {
                database.execSQL("DROP TABLE IF EXISTS games")
            }

            // Erstelle games Tabelle neu mit korrekter Struktur
            database.execSQL("""
                CREATE TABLE games (
                    id TEXT PRIMARY KEY NOT NULL,
                    userId TEXT NOT NULL,
                    gameMode TEXT NOT NULL DEFAULT 'classic',
                    totalRounds INTEGER NOT NULL DEFAULT 5,
                    currentRound INTEGER NOT NULL DEFAULT 1,
                    score INTEGER NOT NULL DEFAULT 0,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    startedAt INTEGER,
                    completedAt INTEGER,
                    duration INTEGER
                )
            """)

            // Erstelle locations Tabelle nur wenn sie nicht existiert
            if (!locationsTableExists) {
                database.execSQL("""
                    CREATE TABLE locations (
                        id TEXT PRIMARY KEY NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        imageUrl TEXT NOT NULL,
                        country TEXT,
                        city TEXT,
                        difficulty INTEGER NOT NULL DEFAULT 1,
                        isCached INTEGER NOT NULL DEFAULT 0,
                        isUsed INTEGER NOT NULL DEFAULT 0,
                        cachedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                    )
                """)
            }

            // Erstelle guesses Tabelle nur wenn sie nicht existiert
            if (!guessesTableExists) {
                database.execSQL("""
                    CREATE TABLE guesses (
                        id TEXT PRIMARY KEY NOT NULL,
                        gameId TEXT NOT NULL,
                        locationId TEXT NOT NULL,
                        guessLat REAL NOT NULL,
                        guessLng REAL NOT NULL,
                        actualLat REAL NOT NULL,
                        actualLng REAL NOT NULL,
                        distance REAL NOT NULL,
                        score INTEGER NOT NULL,
                        timeSpent INTEGER NOT NULL,
                        submittedAt INTEGER NOT NULL
                    )
                """)
            }

            // Erstelle alle Indizes neu (falls sie nicht existieren)
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId` ON `games` (`userId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_isCompleted` ON `games` (`isCompleted`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_createdAt` ON `games` (`createdAt`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_gameId` ON `guesses` (`gameId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_locationId` ON `guesses` (`locationId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_isUsed_isCached` ON `locations` (`isUsed`, `isCached`)")

            // Stelle sicher, dass users Tabelle existiert
            val usersCursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
            val usersTableExists = usersCursor.moveToFirst()
            usersCursor.close()

            if (!usersTableExists) {
                database.execSQL("""
                    CREATE TABLE users (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        authToken TEXT,
                        totalScore INTEGER NOT NULL DEFAULT 0,
                        gamesPlayed INTEGER NOT NULL DEFAULT 0,
                        bestScore INTEGER NOT NULL DEFAULT 0,
                        lastLoginAt INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                    )
                """)
            }

            // Emergency User erstellen
            database.execSQL("""
                INSERT OR IGNORE INTO users (id, username, email, authToken, totalScore, gamesPlayed, bestScore, lastLoginAt, createdAt)
                VALUES ('emergency_user', 'Emergency User', 'emergency@local.com', NULL, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """)

            // Füge Standard-Fallback-Locations hinzu falls locations Tabelle leer ist
            try {
                val countCursor = database.query("SELECT COUNT(*) FROM locations")
                val hasLocations = if (countCursor.moveToFirst()) {
                    countCursor.getInt(0) > 0
                } else {
                    false
                }
                countCursor.close()

                if (!hasLocations) {
                    // Füge eine Fallback-Location hinzu
                    database.execSQL("""
                        INSERT INTO locations (id, latitude, longitude, imageUrl, country, city, difficulty, isCached, isUsed, cachedAt)
                        VALUES ('fallback_paris', 48.8566, 2.3522, 'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800', 'France', 'Paris', 2, 1, 0, ${System.currentTimeMillis()})
                    """)
                }
            } catch (e: Exception) {
                // Silent fail - Fallback-Location konnte nicht eingefügt werden
            }
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Version 8: Radikaler Clean-Slate. Löscht alle Tabellen und erstellt sie neu.
            // Dies behebt alle vorherigen Migrationsprobleme garantiert.

            // 1. Alle alten Tabellen löschen
            database.execSQL("DROP TABLE IF EXISTS `games`")
            database.execSQL("DROP TABLE IF EXISTS `guesses`")
            database.execSQL("DROP TABLE IF EXISTS `locations`")
            database.execSQL("DROP TABLE IF EXISTS `users`")

            // 2. Alle Tabellen mit der korrekten, aktuellen Struktur neu erstellen
            database.execSQL("""
                CREATE TABLE `users` (
                    `id` TEXT NOT NULL, 
                    `username` TEXT NOT NULL, 
                    `email` TEXT NOT NULL, 
                    `authToken` TEXT, 
                    `totalScore` INTEGER NOT NULL DEFAULT 0, 
                    `gamesPlayed` INTEGER NOT NULL DEFAULT 0, 
                    `bestScore` INTEGER NOT NULL DEFAULT 0, 
                    `lastLoginAt` INTEGER NOT NULL DEFAULT 0, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """)

            database.execSQL("""
                CREATE TABLE `locations` (
                    `id` TEXT NOT NULL, 
                    `latitude` REAL NOT NULL, 
                    `longitude` REAL NOT NULL, 
                    `imageUrl` TEXT NOT NULL, 
                    `country` TEXT, 
                    `city` TEXT, 
                    `difficulty` INTEGER NOT NULL DEFAULT 1, 
                    `isCached` INTEGER NOT NULL DEFAULT 0, 
                    `isUsed` INTEGER NOT NULL DEFAULT 0, 
                    `cachedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """)

            database.execSQL("""
                CREATE TABLE `games` (
                    `id` TEXT NOT NULL, 
                    `userId` TEXT NOT NULL, 
                    `gameMode` TEXT NOT NULL, 
                    `totalRounds` INTEGER NOT NULL, 
                    `currentRound` INTEGER NOT NULL, 
                    `score` INTEGER NOT NULL, 
                    `isCompleted` INTEGER NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    `startedAt` INTEGER, 
                    `completedAt` INTEGER, 
                    `duration` INTEGER, 
                    PRIMARY KEY(`id`)
                )
            """)
            
            database.execSQL("""
                CREATE TABLE `guesses` (
                    `id` TEXT NOT NULL, 
                    `gameId` TEXT NOT NULL, 
                    `locationId` TEXT NOT NULL, 
                    `guessLat` REAL NOT NULL, 
                    `guessLng` REAL NOT NULL, 
                    `actualLat` REAL NOT NULL, 
                    `actualLng` REAL NOT NULL, 
                    `distance` REAL NOT NULL, 
                    `score` INTEGER NOT NULL, 
                    `timeSpent` INTEGER NOT NULL, 
                    `submittedAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`)
                )
            """)

            // 3. Alle Indizes neu erstellen
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId` ON `games` (`userId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_locations_isUsed_isCached` ON `locations` (`isUsed`, `isCached`)")

            // 4. Notwendige Start-Daten einfügen
            database.execSQL("""
                INSERT INTO users (id, username, email, createdAt) 
                VALUES ('emergency_user', 'Emergency User', 'emergency@local.com', ${System.currentTimeMillis()})
            """)
            database.execSQL("""
                INSERT INTO locations (id, latitude, longitude, imageUrl, country, city, cachedAt)
                VALUES ('fallback_paris', 48.8566, 2.3522, 'https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800', 'France', 'Paris', ${System.currentTimeMillis()})
            """)
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "geoguesser_database"
        )
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8 // Neue, saubere Migration
        )
        .setQueryExecutor(Executors.newFixedThreadPool(4))
        .setTransactionExecutor(Executors.newFixedThreadPool(2))
        // Fallback Strategy für Development
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase) = database.userDao()

    @Provides
    fun provideLocationDao(database: AppDatabase) = database.locationDao()

    @Provides
    fun provideGameDao(database: AppDatabase) = database.gameDao()

    @Provides
    fun provideGuessDao(database: AppDatabase) = database.guessDao()
}
