package com.example.geogeusserclone.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.database.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        GameEntity::class,
        GuessEntity::class,
        LocationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun guessDao(): GuessDao
    abstract fun locationDao(): LocationDao
}
