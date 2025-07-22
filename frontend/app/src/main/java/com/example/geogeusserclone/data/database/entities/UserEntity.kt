package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val authToken: String?,
    val refreshToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)