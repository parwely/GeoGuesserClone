package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE isCached = 1")
    fun getCachedLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isUsed = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnusedLocation(): LocationEntity?

    @Query("SELECT * FROM locations WHERE isUsed = 0 AND isCached = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomCachedUnusedLocation(): LocationEntity?

    @Query("SELECT * FROM locations ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomLocation(): LocationEntity?

    @Query("SELECT * FROM locations WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit")
    suspend fun getLocationsByDifficulty(difficulty: Int, limit: Int): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE country = :country ORDER BY RANDOM() LIMIT :limit")
    suspend fun getLocationsByCountry(country: String, limit: Int): List<LocationEntity>

    @Query("SELECT COUNT(*) FROM locations WHERE isUsed = 0")
    suspend fun getUnusedLocationCount(): Int

    @Query("SELECT COUNT(*) FROM locations WHERE isCached = 1")
    suspend fun getCachedLocationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Query("UPDATE locations SET isUsed = 1 WHERE id = :locationId")
    suspend fun markLocationAsUsed(locationId: String)

    @Query("UPDATE locations SET isCached = 1, cachedAt = :cachedAt WHERE id = :locationId")
    suspend fun markLocationAsCached(locationId: String, cachedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: String)

    @Query("DELETE FROM locations WHERE isUsed = 1 AND isCached = 0")
    suspend fun deleteUsedUncachedLocations()

    @Query("DELETE FROM locations WHERE isUsed = 1 AND cachedAt < :olderThan")
    suspend fun deleteUnusedOldLocations(olderThan: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000))

    @Query("DELETE FROM locations WHERE cachedAt < :olderThan")
    suspend fun deleteOldCachedLocations(olderThan: Long)

    @Query("UPDATE locations SET isUsed = 0")
    suspend fun resetAllLocationsAsUnused()

    @Query("SELECT * FROM locations WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng LIMIT :limit")
    suspend fun getLocationsByBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int
    ): List<LocationEntity>

    @Query("SELECT DISTINCT country FROM locations WHERE country IS NOT NULL")
    suspend fun getAllCountries(): List<String>

    @Query("SELECT DISTINCT city FROM locations WHERE city IS NOT NULL")
    suspend fun getAllCities(): List<String>
}