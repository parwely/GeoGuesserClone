package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE isCompleted = 1 AND userId = :userId ORDER BY completedAt DESC")
    fun getCompletedGamesByUser(userId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 0 LIMIT 1")
    suspend fun getActiveGameByUser(userId: String): GameEntity?

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGame(gameId: String)

    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllGamesByUser(userId: String): Flow<List<GameEntity>>

    @Query("DELETE FROM games WHERE userId = :userId")
    suspend fun deleteAllGamesByUser(userId: String)

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedGamesCount(userId: String): Int

    @Query("SELECT MAX(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getBestScore(userId: String): Int?

    @Query("SELECT AVG(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getAverageScore(userId: String): Double?
}