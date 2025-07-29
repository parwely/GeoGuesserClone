package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["authToken"]),
        Index(value = ["lastLoginAt"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val authToken: String? = null,
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0,
    val bestScore: Int = 0,
    val lastLoginAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
