package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.utils.Constants
import com.example.geogeusserclone.utils.DistanceCalculator
import com.example.geogeusserclone.utils.ScoreCalculator
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val apiService: ApiService,
    private val gameDao: GameDao,
    private val guessDao: GuessDao
) : BaseRepository() {

    suspend fun createGame(
        userId: String,
        gameMode: String = "classic",
        rounds: Int = 5
    ): Result<GameEntity> {
        return try {
            // Erstelle neue Spiel-ID
            val gameId = UUID.randomUUID().toString()

            val game = GameEntity(
                id = gameId,
                userId = userId,
                gameMode = gameMode,
                totalRounds = rounds,
                currentRound = 1,
                score = 0,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis()
            )

            gameDao.insertGame(game)
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitGuess(
        gameId: String,
        locationId: String,
        guessLat: Double,
        guessLng: Double,
        actualLat: Double,
        actualLng: Double,
        timeSpent: Long = 0L
    ): Result<GuessEntity> {
        return try {
            val distance = DistanceCalculator.calculateDistance(
                guessLat, guessLng, actualLat, actualLng
            )

            val score = ScoreCalculator.calculateScore(
                distanceKm = distance,
                timeSpentMs = timeSpent,
                maxTimeMs = Constants.MAX_ROUND_TIME_MS
            )

            val guess = GuessEntity(
                id = UUID.randomUUID().toString(),
                gameId = gameId,
                locationId = locationId,
                guessLat = guessLat,     // Konsistent mit Entity
                guessLng = guessLng,     // Konsistent mit Entity
                actualLat = actualLat,   // Konsistent mit Entity
                actualLng = actualLng,   // Konsistent mit Entity
                distance = distance,
                score = score,
                timeSpent = timeSpent,
                submittedAt = System.currentTimeMillis()
            )

            guessDao.insertGuess(guess)

            // Update game
            val currentGame = gameDao.getGameById(gameId)
            currentGame?.let { game ->
                val newScore = game.score + score
                val newRound = game.currentRound + 1

                gameDao.updateGame(
                    game.copy(
                        score = newScore,
                        currentRound = newRound
                    )
                )
            }

            Result.success(guess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveGame(userId: String): GameEntity? {
        return gameDao.getActiveGameByUser(userId)
    }

    suspend fun completeGame(gameId: String): Result<GameEntity> {
        return try {
            val game = gameDao.getGameById(gameId)
            if (game != null) {
                val completedGame = game.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
                gameDao.updateGame(completedGame)
                Result.success(completedGame)
            } else {
                Result.failure(Exception("Spiel nicht gefunden"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>> {
        return guessDao.getGuessesByGame(gameId)
    }

    suspend fun getGameHistory(userId: String): Flow<List<GameEntity>> {
        return gameDao.getCompletedGamesByUser(userId)
    }

    suspend fun deleteGame(gameId: String) {
        gameDao.deleteGame(gameId)
    }

    private suspend fun updateGameScore(gameId: String, finalScore: Int) {
        val game = gameDao.getGameById(gameId)
        game?.let {
            gameDao.updateGame(it.copy(score = finalScore))
        }
    }
}