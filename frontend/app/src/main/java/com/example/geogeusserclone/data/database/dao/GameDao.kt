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

    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentGameForUser(userId: String): GameEntity?

    @Query("SELECT * FROM games WHERE isCompleted = 1 ORDER BY score DESC LIMIT :limit")
    suspend fun getTopGames(limit: Int): List<GameEntity>

    @Query("SELECT * FROM games WHERE userId = :userId AND isCompleted = 1 ORDER BY score DESC LIMIT :limit")
    suspend fun getTopGamesByUser(userId: String, limit: Int): List<GameEntity>

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId")
    suspend fun getGameCountByUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getCompletedGameCountByUser(userId: String): Int

    @Query("SELECT AVG(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getAverageScoreByUser(userId: String): Double?

    @Query("SELECT MAX(score) FROM games WHERE userId = :userId AND isCompleted = 1")
    suspend fun getBestScoreByUser(userId: String): Int?

    @Query("SELECT * FROM games WHERE createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getGamesByDateRange(startTime: Long, endTime: Long): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: String)

    @Query("DELETE FROM games WHERE userId = :userId AND isCompleted = 0")
    suspend fun deleteIncompleteGamesByUser(userId: String)

    @Query("DELETE FROM games WHERE createdAt < :olderThan AND isCompleted = 1")
    suspend fun deleteOldCompletedGames(olderThan: Long)

    @Query("UPDATE games SET isCompleted = 1, completedAt = :completedAt WHERE id = :gameId")
    suspend fun markGameAsCompleted(gameId: String, completedAt: Long)

    @Query("UPDATE games SET score = :newScore WHERE id = :gameId")
    suspend fun updateGameScore(gameId: String, newScore: Int)

    @Query("UPDATE games SET currentRound = :newRound WHERE id = :gameId")
    suspend fun updateCurrentRound(gameId: String, newRound: Int)
}
