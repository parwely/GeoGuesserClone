package com.example.geogeusserclone.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.geogeusserclone.data.database.dao.*
import com.example.geogeusserclone.data.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        LocationEntity::class,
        GameEntity::class,
        GuessEntity::class
    ],
    version = 6, // Version erhöht von 5 auf 6
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun gameDao(): GameDao
    abstract fun guessDao(): GuessDao
}
