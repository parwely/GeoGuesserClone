package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE isCached = 1")
    fun getCachedLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomLocations(count: Int): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE isUsed = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnusedLocation(): LocationEntity?

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("UPDATE locations SET isUsed = 1 WHERE id = :locationId")
    suspend fun markLocationAsUsed(locationId: String)

    @Query("UPDATE locations SET isUsed = 0")
    suspend fun resetAllLocationsUsage()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Query("DELETE FROM locations WHERE isCached = 0")
    suspend fun deleteNonCachedLocations()

    @Query("SELECT COUNT(*) FROM locations WHERE isCached = 1")
    suspend fun getCachedLocationCount(): Int
}