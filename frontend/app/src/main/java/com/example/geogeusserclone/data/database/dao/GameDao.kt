package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGamesByUser(userId: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: String): GameEntity?

    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentGameForUser(userId: String): GameEntity?

    @Query("SELECT * FROM games ORDER BY score DESC LIMIT :limit")
    suspend fun getTopScores(limit: Int = 10): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE userId = :userId")
    suspend fun deleteAllGamesForUser(userId: String)

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedGamesCount(userId: String): Int

    @Query("SELECT AVG(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getAverageScore(userId: String): Double?

    @Query("SELECT MAX(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getBestScore(userId: String): Int?
}
