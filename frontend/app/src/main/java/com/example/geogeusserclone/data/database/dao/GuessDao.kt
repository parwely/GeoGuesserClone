package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.GuessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuessDao {

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY submittedAt ASC")
    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>>

    @Query("SELECT * FROM guesses WHERE id = :guessId")
    suspend fun getGuessById(guessId: String): GuessEntity?

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY score DESC LIMIT 1")
    suspend fun getBestGuessForGame(gameId: String): GuessEntity?

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY score ASC LIMIT 1")
    suspend fun getWorstGuessForGame(gameId: String): GuessEntity?

    @Query("SELECT AVG(distance) FROM guesses WHERE gameId = :gameId")
    suspend fun getAverageDistanceForGame(gameId: String): Double?

    @Query("SELECT AVG(score) FROM guesses WHERE gameId = :gameId")
    suspend fun getAverageScoreForGame(gameId: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuess(guess: GuessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuesses(guesses: List<GuessEntity>)

    @Update
    suspend fun updateGuess(guess: GuessEntity)

    @Delete
    suspend fun deleteGuess(guess: GuessEntity)

    @Query("DELETE FROM guesses WHERE gameId = :gameId")
    suspend fun deleteGuessesForGame(gameId: String)
}
