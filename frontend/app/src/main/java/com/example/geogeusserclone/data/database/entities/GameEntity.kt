package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val score: Int,
    val totalRounds: Int,
    val currentRound: Int,
    val isCompleted: Boolean,
    val gameMode: String, // "SINGLE", "MULTIPLAYER", "BATTLE_ROYALE"
    val timestamp: Long,
    val duration: Long? = null
)