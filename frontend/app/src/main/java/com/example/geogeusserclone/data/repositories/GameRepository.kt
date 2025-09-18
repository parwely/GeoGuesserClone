package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.models.*
import com.example.geogeusserclone.data.network.GameApi
import com.example.geogeusserclone.data.network.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameApi: GameApi
) {
    suspend fun startNewRound(difficulty: Int? = null, category: String? = null): Result<NewRoundResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("GameRepository: Starting new round with difficulty=$difficulty, category=$category")

                val response = gameApi.newRound(difficulty, category)

                if (response.isSuccessful && response.body() != null) {
                    val backendResponse = response.body()!!
                    println("GameRepository: ✅ Backend response received - ID: ${backendResponse.id}, Location: ${backendResponse.location_hint}")

                    // Convert backend response to expected format
                    val newRound = backendResponse.toNewRoundResponse()

                    println("GameRepository: ✅ New round started - ID: ${newRound.roundId}, Location: ${newRound.location.city}")
                    Result.success(newRound)
                } else {
                    println("GameRepository: ❌ API call failed: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            } catch (e: Exception) {
                println("GameRepository: ❌ Exception in newRound: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun submitGuess(guess: GuessRequest): Result<ScoreResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("GameRepository: Submitting guess for round ${guess.roundId}")

                val response = gameApi.submitGuess(guess)

                if (response.isSuccessful && response.body() != null) {
                    val scoreResponse = response.body()!!
                    if (scoreResponse.success) {
                        println("GameRepository: ✅ Guess submitted - Score: ${scoreResponse.score}, Distance: ${scoreResponse.distanceMeters}m")
                        Result.success(scoreResponse)
                    } else {
                        println("GameRepository: ❌ Server returned success=false: ${scoreResponse.message}")
                        Result.failure(Exception(scoreResponse.message ?: "Failed to submit guess"))
                    }
                } else {
                    println("GameRepository: ❌ API call failed: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            } catch (e: Exception) {
                println("GameRepository: ❌ Exception in submitGuess: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun checkStreetViewAvailability(locationId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                println("GameRepository: Checking Street View availability for location $locationId")

                // NEUE: Mehrere API-Endpunkte probieren bei 404-Fehlern
                val endpoints = listOf(
                    { gameApi.checkStreetViewAvailability(locationId) },
                    { gameApi.getLocationStreetView(locationId) },
                    { gameApi.getStreetViewStatus(locationId) }
                )

                for ((index, endpoint) in endpoints.withIndex()) {
                    try {
                        val response = endpoint()

                        if (response.isSuccessful && response.body() != null) {
                            val availability = response.body()!!
                            if (availability.success) {
                                println("GameRepository: ✅ Street View available via endpoint ${index + 1}: ${availability.streetViewAvailable}")
                                return@withContext Result.success(availability.streetViewAvailable)
                            }
                        } else if (response.code() == 404) {
                            println("GameRepository: ⚠️ Endpoint ${index + 1} not found (404), trying next...")
                            continue
                        }
                    } catch (e: Exception) {
                        println("GameRepository: ⚠️ Endpoint ${index + 1} failed: ${e.message}")
                        continue
                    }
                }

                // FALLBACK: Wenn alle Endpunkte fehlschlagen, nehme an dass Street View verfügbar ist
                println("GameRepository: 🔧 All Street View endpoints failed, assuming available")
                Result.success(true)

            } catch (e: Exception) {
                println("GameRepository: ❌ Street View check completely failed: ${e.message}")
                // Optimistische Annahme: Street View ist verfügbar
                Result.success(true)
            }
        }
    }

    suspend fun createGameSession(): Result<GameSession> {
        return withContext(Dispatchers.IO) {
            try {
                val response = gameApi.createGameSession()

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create game session"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getGameSession(sessionId: String): Result<GameSession> {
        return withContext(Dispatchers.IO) {
            try {
                val response = gameApi.getGameSession(sessionId)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to get game session"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getLeaderboard(limit: Int = 10): Result<List<LeaderboardEntry>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = gameApi.getLeaderboard(limit)

                if (response.isSuccessful && response.body() != null) {
                    val leaderboardResponse = response.body()!!
                    if (leaderboardResponse.success) {
                        Result.success(leaderboardResponse.leaderboard)
                    } else {
                        Result.failure(Exception("Failed to get leaderboard"))
                    }
                } else {
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
