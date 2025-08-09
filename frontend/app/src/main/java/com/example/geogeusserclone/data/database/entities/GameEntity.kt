package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["isCompleted"]),
        Index(value = ["createdAt"])
    ]
)
data class GameEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val gameMode: String = "classic",
    val totalRounds: Int = 5,
    val currentRound: Int = 1,
    val score: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val duration: Long? = null
)
