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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuess(guess: GuessEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuesses(guesses: List<GuessEntity>)

    @Update
    suspend fun updateGuess(guess: GuessEntity)

    @Query("DELETE FROM guesses WHERE gameId = :gameId")
    suspend fun deleteGuessesByGame(gameId: String)

    @Query("DELETE FROM guesses WHERE id = :guessId")
    suspend fun deleteGuess(guessId: String)

    @Query("SELECT COUNT(*) FROM guesses WHERE gameId = :gameId")
    suspend fun getGuessCountByGame(gameId: String): Int

    @Query("SELECT * FROM guesses WHERE gameId = :gameId ORDER BY submittedAt DESC LIMIT 1")
    suspend fun getLatestGuessByGame(gameId: String): GuessEntity?
}