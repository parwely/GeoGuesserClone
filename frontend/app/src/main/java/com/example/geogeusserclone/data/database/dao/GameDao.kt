package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: String): GameEntity?

    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGamesByUser(userId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 0 LIMIT 1")
    suspend fun getCurrentGameForUser(userId: String): GameEntity?

    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 1 ORDER BY score DESC LIMIT 1")
    suspend fun getBestGameForUser(userId: String): GameEntity?

    @Query("SELECT * FROM games ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentGames(limit: Int = 10): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE userId = :userId")
    suspend fun deleteGamesByUser(userId: String)

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId")
    suspend fun getGameCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedGameCountByUser(userId: String): Int
}
