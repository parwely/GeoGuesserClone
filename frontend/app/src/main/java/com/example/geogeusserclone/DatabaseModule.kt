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
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_guesses_timestamp` ON `guesses` (`timestamp`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId` ON `games` (`userId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_timestamp` ON `games` (`timestamp`)")
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

            // Composite Index für bessere Query Performance
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_games_userId_isCompleted_completedAt` ON `games` (`userId`, `isCompleted`, `completedAt`)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "geoguessr_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .setQueryExecutor(Executors.newFixedThreadPool(4)) // Thread Pool für bessere Performance
            .build()
    }
}