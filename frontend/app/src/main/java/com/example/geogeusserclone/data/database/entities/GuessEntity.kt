package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "guesses",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["locationId"]),
        Index(value = ["timestamp"])
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