package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GuessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuessDao {

    @Query("SELECT * FROM guesses WHERE id = :guessId")
    suspend fun getGuessById(guessId: String): GuessEntity?

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY submittedAt ASC")
    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>>

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY submittedAt ASC")
    suspend fun getGuessesByGameSync(gameId: String): List<GuessEntity>

    @Query("SELECT * FROM guesses WHERE locationId = :locationId")
    suspend fun getGuessesByLocation(locationId: String): List<GuessEntity>

    @Query("SELECT AVG(score) FROM guesses WHERE gameId IN (SELECT id FROM games WHERE userId = :userId)")
    suspend fun getAverageScoreByUser(userId: String): Double?

    @Query("SELECT MAX(score) FROM guesses WHERE gameId IN (SELECT id FROM games WHERE userId = :userId)")
    suspend fun getBestScoreByUser(userId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuess(guess: GuessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuesses(guesses: List<GuessEntity>)

    @Update
    suspend fun updateGuess(guess: GuessEntity)

    @Delete
    suspend fun deleteGuess(guess: GuessEntity)

    @Query("DELETE FROM guesses WHERE gameId = :gameId")
    suspend fun deleteGuessesByGame(gameId: String)

    @Query("SELECT COUNT(*) FROM guesses WHERE gameId = :gameId")
    suspend fun getGuessCountByGame(gameId: String): Int
}
