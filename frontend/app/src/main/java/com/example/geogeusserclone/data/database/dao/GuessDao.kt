package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GuessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuessDao {

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY timestamp")
    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>>

    @Query("SELECT * FROM guesses WHERE id = :guessId")
    suspend fun getGuessById(guessId: String): GuessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuess(guess: GuessEntity)

    @Query("SELECT AVG(distance) FROM guesses WHERE gameId IN (SELECT id FROM games WHERE userId = :userId)")
    suspend fun getAverageDistance(userId: String): Double?

    @Query("SELECT MIN(distance) FROM guesses WHERE gameId IN (SELECT id FROM games WHERE userId = :userId)")
    suspend fun getBestDistance(userId: String): Double?

    @Query("DELETE FROM guesses WHERE gameId = :gameId")
    suspend fun deleteGuessesByGame(gameId: String)
}