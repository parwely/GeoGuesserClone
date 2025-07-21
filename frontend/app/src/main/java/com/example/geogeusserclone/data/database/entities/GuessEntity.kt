package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "guesses",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GuessEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val locationId: String,
    val guessLatitude: Double,
    val guessLongitude: Double,
    val actualLatitude: Double,
    val actualLongitude: Double,
    val distance: Double, // in kilometers
    val score: Int,
    val timeSpent: Long, // in milliseconds
    val timestamp: Long = System.currentTimeMillis()
)