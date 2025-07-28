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
        Index(value = ["submittedAt"])
    ]
)
data class GuessEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val locationId: String,
    val guessLat: Double,  // Geändert von guessLatitude
    val guessLng: Double,  // Geändert von guessLongitude
    val actualLat: Double, // Geändert von actualLatitude
    val actualLng: Double, // Geändert von actualLongitude
    val distance: Double, // in kilometers
    val score: Int,
    val timeSpent: Long, // in milliseconds
    val submittedAt: Long = System.currentTimeMillis() // Geändert von timestamp
)