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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "geoguessr_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}