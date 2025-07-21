package com.example.geogeusserclone.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.geogeusserclone.data.database.dao.*
import com.example.geogeusserclone.data.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        GameEntity::class,
        LocationEntity::class,
        GuessEntity::class
    ],
    version = 2, // Version erhöht wegen Index-Änderungen
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun locationDao(): LocationDao
    abstract fun guessDao(): GuessDao
}