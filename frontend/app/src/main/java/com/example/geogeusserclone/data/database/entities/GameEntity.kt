package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"]),
        Index(value = ["isCompleted"])
    ]
)
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