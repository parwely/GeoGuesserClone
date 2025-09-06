package com.example.geogeusserclone.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity

@Database(
    entities = [
        UserEntity::class,
        LocationEntity::class,
        GameEntity::class,
        GuessEntity::class
    ],
    version = 8, // Aktuelle Version nach Migrationen
    exportSchema = false
)
@TypeConverters()
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun gameDao(): GameDao
    abstract fun guessDao(): GuessDao

    companion object {
        const val DATABASE_NAME = "geoguesser_database"
    }
}
