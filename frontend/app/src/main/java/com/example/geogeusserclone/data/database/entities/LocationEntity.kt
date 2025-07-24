package com.example.geogeusserclone.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String,
    val country: String? = null,
    val city: String? = null,
    val difficulty: Int = 1, // 1-5 scale
    val isCached: Boolean = false,
    val isUsed: Boolean = false, // Hinzugefügt
    val localImagePath: String? = null
)