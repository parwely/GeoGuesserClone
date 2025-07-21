// UserDao.kt
package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

// GameDao.kt
@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    suspend fun getAllGames(): List<GameEntity>

    @Insert
    suspend fun insertGame(game: GameEntity)
}