package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.GameDao
import com.example.geogeusserclone.data.database.dao.GuessDao
import com.example.geogeusserclone.data.database.dao.LocationDao
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.*
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
    private val locationDao: LocationDao,
    private val userRepository: UserRepository
) : BaseRepository() {

    suspend fun createGame(
        userId: String,
        gameMode: String = "single",
        rounds: Int = 5
    ): Result<GameEntity> {
        return try {
            // KORREKTUR: Stelle sicher, dass User angemeldet ist für Backend-Games
            val currentUser = userRepository.getCurrentUser()

            if (currentUser?.authToken != null) {
                // Verwende Backend API nur mit gültigem Token
                val response = apiService.createSinglePlayerGame(
                    GameCreateRequest(
                        difficulty = 2,
                        category = "urban",
                        rounds = rounds
                    )
                )

                if (response.isSuccessful) {
                    val gameResponse = response.body()!!
                    if (gameResponse.success) {
                        val gameEntity = GameEntity(
                            id = gameResponse.data.gameId.toString(), // Convert Int to String
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

                        // Speichere die Locations aus dem Backend
                        val locationEntities = gameResponse.data.locations.map { backendLocation ->
                            LocationEntity(
                                id = backendLocation.id.toString(), // Convert Int to String
                                latitude = backendLocation.coordinates.latitude,
                                longitude = backendLocation.coordinates.longitude,
                                imageUrl = backendLocation.imageUrls.firstOrNull() ?: "",
                                country = backendLocation.country,
                                city = backendLocation.city,
                                difficulty = backendLocation.difficulty,
                                isCached = true,
                                isUsed = false
                            )
                        }
                        locationDao.insertLocations(locationEntities)

                        return Result.success(gameEntity)
                    }
                }
            }

            // Fallback für Offline-Modus oder wenn kein Auth Token vorhanden
            println("GameRepository: Verwende Offline-Modus (kein gültiger Auth Token)")
            createOfflineGame(userId, gameMode, rounds)
        } catch (e: Exception) {
            println("GameRepository: Backend-Fehler, verwende Offline-Modus: ${e.message}")
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

            // Versuche online zu synchronisieren - remove this part that causes error
            // The backend doesn't have this endpoint, so we skip it

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

    // Neue Methode für komplettes Spiel-Ergebnis
    suspend fun submitCompleteGameResult(
        game: GameEntity,
        guesses: List<GuessEntity>
    ): Result<GameResultResponse> {
        return try {
            val gameResultRequest = GameResultRequest(
                totalScore = game.score,
                totalDistance = guesses.sumOf { it.distance },
                accuracy = calculateAccuracy(guesses),
                timeTaken = game.duration ?: 0L,
                roundsData = guesses.map { guess ->
                    RoundData(
                        locationId = guess.locationId.toInt(), // Convert String to Int
                        guessLatitude = guess.guessLat,
                        guessLongitude = guess.guessLng,
                        actualLatitude = guess.actualLat,
                        actualLongitude = guess.actualLng,
                        distance = guess.distance,
                        score = guess.score,
                        timeSpent = guess.timeSpent
                    )
                }
            )

            val response = apiService.submitGameResult(game.id.toInt(), gameResultRequest)

            if (response.isSuccessful) {
                val result = response.body()!!
                Result.success(result)
            } else {
                Result.failure(Exception("Fehler beim Übermitteln des Spielergebnisses"))
            }
        } catch (e: Exception) {
            // Silent fail - Spiel ist lokal gespeichert
            Result.failure(e)
        }
    }

    private fun calculateAccuracy(guesses: List<GuessEntity>): Double {
        if (guesses.isEmpty()) return 0.0

        val averageDistance = guesses.sumOf { it.distance } / guesses.size
        return when {
            averageDistance <= 1.0 -> 100.0
            averageDistance <= 10.0 -> 90.0
            averageDistance <= 50.0 -> 80.0
            averageDistance <= 200.0 -> 70.0
            averageDistance <= 1000.0 -> 50.0
            else -> 20.0
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
