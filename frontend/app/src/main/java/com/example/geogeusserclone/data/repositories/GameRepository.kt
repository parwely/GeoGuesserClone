package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.CreateGameRequest
import com.example.geogeusserclone.data.network.GuessRequest
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val apiService: ApiService,
    private val gameDao: GameDao,
    private val guessDao: GuessDao
) {

    fun getGamesByUser(userId: String): Flow<List<GameEntity>> = gameDao.getGamesByUser(userId)

    suspend fun getActiveGame(userId: String): GameEntity? = gameDao.getActiveGame(userId)

    suspend fun createGame(userId: String, gameMode: String, rounds: Int = 5): Result<GameEntity> {
        return try {
            // Try online first
            val response = apiService.createGame(CreateGameRequest(gameMode, rounds))

            val gameEntity = if (response.isSuccessful) {
                val gameResponse = response.body()!!
                GameEntity(
                    id = gameResponse.id,
                    userId = userId,
                    score = 0,
                    totalRounds = rounds,
                    currentRound = 1,
                    isCompleted = false,
                    gameMode = gameMode,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                // Fallback to offline mode
                GameEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    score = 0,
                    totalRounds = rounds,
                    currentRound = 1,
                    isCompleted = false,
                    gameMode = gameMode,
                    timestamp = System.currentTimeMillis()
                )
            }

            gameDao.insertGame(gameEntity)
            Result.success(gameEntity)

        } catch (e: Exception) {
            // Offline fallback
            val gameEntity = GameEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                score = 0,
                totalRounds = rounds,
                currentRound = 1,
                isCompleted = false,
                gameMode = gameMode,
                timestamp = System.currentTimeMillis()
            )
            gameDao.insertGame(gameEntity)
            Result.success(gameEntity)
        }
    }

    suspend fun submitGuess(
        gameId: String,
        locationId: String,
        guessLat: Double,
        guessLng: Double,
        actualLat: Double,
        actualLng: Double
    ): Result<GuessEntity> {
        return try {
            val distance = calculateDistance(guessLat, guessLng, actualLat, actualLng)
            val score = calculateScore(distance)

            // Try to submit online
            try {
                val response = apiService.submitGuess(
                    GuessRequest(gameId, locationId, guessLat, guessLng)
                )

                if (response.isSuccessful) {
                    val guessResponse = response.body()!!
                    // Use server calculated values if available
                    val finalScore = guessResponse.score
                    val finalDistance = guessResponse.distance

                    val guessEntity = createGuessEntity(
                        gameId, locationId, guessLat, guessLng,
                        actualLat, actualLng, finalDistance, finalScore
                    )

                    guessDao.insertGuess(guessEntity)
                    updateGameScore(gameId, finalScore)

                    Result.success(guessEntity)
                } else {
                    throw Exception("Server error")
                }
            } catch (e: Exception) {
                // Offline fallback
                val guessEntity = createGuessEntity(
                    gameId, locationId, guessLat, guessLng,
                    actualLat, actualLng, distance, score
                )

                guessDao.insertGuess(guessEntity)
                updateGameScore(gameId, score)

                Result.success(guessEntity)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createGuessEntity(
        gameId: String, locationId: String, guessLat: Double, guessLng: Double,
        actualLat: Double, actualLng: Double, distance: Double, score: Int
    ): GuessEntity {
        return GuessEntity(
            id = UUID.randomUUID().toString(),
            gameId = gameId,
            locationId = locationId,
            guessLatitude = guessLat,
            guessLongitude = guessLng,
            actualLatitude = actualLat,
            actualLongitude = actualLng,
            distance = distance,
            score = score,
            timeSpent = 0L // Can be implemented later
        )
    }

    private suspend fun updateGameScore(gameId: String, additionalScore: Int) {
        val game = gameDao.getGameById(gameId)
        game?.let {
            val updatedGame = it.copy(
                score = it.score + additionalScore,
                currentRound = it.currentRound + 1,
                isCompleted = it.currentRound >= it.totalRounds
            )
            gameDao.updateGame(updatedGame)
        }
    }

    suspend fun completeGame(gameId: String): Result<GameEntity> {
        return try {
            val game = gameDao.getGameById(gameId)
            game?.let {
                val completedGame = it.copy(
                    isCompleted = true,
                    duration = System.currentTimeMillis() - it.timestamp
                )
                gameDao.updateGame(completedGame)
                Result.success(completedGame)
            } ?: Result.failure(Exception("Game not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStats(userId: String): GameStats {
        val gamesCount = gameDao.getCompletedGamesCount(userId)
        val bestScore = gameDao.getBestScore(userId) ?: 0
        val averageScore = gameDao.getAverageScore(userId) ?: 0.0
        val averageDistance = guessDao.getAverageDistance(userId) ?: 0.0
        val bestDistance = guessDao.getBestDistance(userId) ?: 0.0

        return GameStats(
            gamesPlayed = gamesCount,
            bestScore = bestScore,
            averageScore = averageScore,
            averageDistance = averageDistance,
            bestDistance = bestDistance
        )
    }

    fun getGuessesByGame(gameId: String): Flow<List<GuessEntity>> = guessDao.getGuessesByGame(gameId)

    // Utility functions
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun calculateScore(distance: Double): Int {
        return when {
            distance <= 1.0 -> 5000
            distance <= 10.0 -> 4000
            distance <= 50.0 -> 3000
            distance <= 100.0 -> 2000
            distance <= 500.0 -> 1000
            distance <= 1000.0 -> 500
            else -> 0
        }.coerceAtLeast(0)
    }
}

data class GameStats(
    val gamesPlayed: Int,
    val bestScore: Int,
    val averageScore: Double,
    val averageDistance: Double,
    val bestDistance: Double
)