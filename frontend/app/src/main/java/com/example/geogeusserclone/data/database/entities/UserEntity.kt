// UserEntity.kt
package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val authToken: String?
)

// GameEntity.kt  
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val score: Int,
    val isCompleted: Boolean,
    val timestamp: Long
)