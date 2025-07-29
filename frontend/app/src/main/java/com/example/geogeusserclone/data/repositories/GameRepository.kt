package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.CreateGameRequest
import com.example.geogeusserclone.data.network.GuessRequest
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
    private val guessDao: GuessDao,
    private val userRepository: UserRepository
) : BaseRepository() {

    suspend fun createGame(
        userId: String,
        gameMode: String,
        rounds: Int
    ): Result<GameEntity> {
        return try {
            // Versuche zuerst online zu erstellen
            val response = apiService.createGame(CreateGameRequest(gameMode, rounds))
            if (response.isSuccessful) {
                val gameResponse = response.body()!!
                val gameEntity = GameEntity(
                    id = gameResponse.id,
                    userId = userId,
                    gameMode = gameMode,
                    totalRounds = rounds,
                    currentRound = 1,
                    score = 0,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    startedAt = System.currentTimeMillis()
                )
                gameDao.insertGame(gameEntity)
                Result.success(gameEntity)
            } else {
                createOfflineGame(userId, gameMode, rounds)
            }
        } catch (e: Exception) {
            // Fallback für Offline-Modus
            createOfflineGame(userId, gameMode, rounds)
        }
    }

    private suspend fun createOfflineGame(
        userId: String,
        gameMode: String,
        rounds: Int
    ): Result<GameEntity> {
        val gameEntity = GameEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            gameMode = gameMode,
            totalRounds = rounds,
            currentRound = 1,
            score = 0,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            startedAt = System.currentTimeMillis()
        )
        gameDao.insertGame(gameEntity)
        return Result.success(gameEntity)
    }

    suspend fun submitGuess(
        gameId: String,
        locationId: String,
        guessLat: Double,
        guessLng: Double,
        actualLat: Double,
        actualLng: Double,
        timeSpent: Long
    ): Result<GuessEntity> {
        return try {
            // Berechne Score und Distanz lokal
            val distance = DistanceCalculator.calculateDistance(
                guessLat, guessLng, actualLat, actualLng
            )
            val score = ScoreCalculator.calculateScore(distance, timeSpent)

            val guessEntity = GuessEntity(
                id = UUID.randomUUID().toString(),
                gameId = gameId,
                locationId = locationId,
                guessLat = guessLat,
                guessLng = guessLng,
                actualLat = actualLat,
                actualLng = actualLng,
                distance = distance,
                score = score,
                timeSpent = timeSpent,
                submittedAt = System.currentTimeMillis()
            )

            // Speichere Guess lokal
            guessDao.insertGuess(guessEntity)

            // Update Game Score
            val currentGame = gameDao.getGameById(gameId)
            if (currentGame != null) {
                val updatedGame = currentGame.copy(
                    score = currentGame.score + score,
                    currentRound = currentGame.currentRound + 1
                )
                gameDao.updateGame(updatedGame)
            }

            // Versuche online zu synchronisieren
            try {
                apiService.submitGuess(
                    gameId,
                    GuessRequest(locationId, guessLat, guessLng, timeSpent)
                )
            } catch (e: Exception) {
                // Silent fail - Guess ist bereits lokal gespeichert
            }

            Result.success(guessEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeGame(gameId: String): Result<GameEntity> {
        return try {
            val game = gameDao.getGameById(gameId)
            if (game != null) {
                val completedGame = game.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - (game.startedAt ?: game.createdAt)
                )
                gameDao.updateGame(completedGame)

                // Update User Stats
                userRepository.updateUserStats(
                    totalScore = completedGame.score,
                    gamesPlayed = 1,
                    bestScore = completedGame.score
                )

                Result.success(completedGame)
            } else {
                Result.failure(Exception("Spiel nicht gefunden"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getGameHistory(userId: String): Flow<List<GameEntity>> {
        return gameDao.getGamesByUser(userId)
    }

    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>> {
        return guessDao.getGuessesByGame(gameId)
    }

    suspend fun getCurrentGame(userId: String): GameEntity? {
        return gameDao.getCurrentGameForUser(userId)
    }
}
