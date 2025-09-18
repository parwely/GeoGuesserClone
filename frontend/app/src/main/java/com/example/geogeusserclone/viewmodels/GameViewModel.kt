package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.models.*
import com.example.geogeusserclone.data.repositories.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun startNewRound(difficulty: Int? = null, category: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // PERFORMANCE: Verwende Dispatchers.IO für Backend-Calls
                val result = withContext(Dispatchers.IO) {
                    gameRepository.startNewRound(difficulty, category)
                }

                result.fold(
                    onSuccess = { newRoundResponse ->
                        // PERFORMANCE: UI-Updates auf Main Thread
                        withContext(Dispatchers.Main) {
                            _gameState.value = _gameState.value.copy(
                                currentRound = GameRound(
                                    roundId = newRoundResponse.roundId,
                                    location = newRoundResponse.location,
                                    guess = null,
                                    score = 0,
                                    distanceMeters = 0.0,
                                    isCompleted = false
                                ),
                                isRoundActive = true
                            )
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                streetViewReady = false,
                                showGuessMap = false
                            )
                        }

                        // PERFORMANCE: Street View Check asynchron starten
                        launch(Dispatchers.IO) {
                            checkStreetViewAvailability(newRoundResponse.location.id)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to start new round"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun submitGuess(guessLat: Double, guessLng: Double, timeSpentSeconds: Int = 0) {
        val currentRound = _gameState.value.currentRound ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingGuess = true)

            try {
                val guessRequest = GuessRequest(
                    roundId = currentRound.roundId,
                    guessLat = guessLat,
                    guessLng = guessLng,
                    timeSpentSeconds = timeSpentSeconds
                )

                // PERFORMANCE: Verwende Dispatchers.IO für Backend-Calls
                val result = withContext(Dispatchers.IO) {
                    gameRepository.submitGuess(guessRequest)
                }

                result.fold(
                    onSuccess = { scoreResponse ->
                        // PERFORMANCE: UI-Updates auf Main Thread
                        withContext(Dispatchers.Main) {
                            val updatedRound = currentRound.copy(
                                guess = GuessLocation(guessLat, guessLng, timeSpentSeconds),
                                score = scoreResponse.score,
                                distanceMeters = scoreResponse.distanceMeters,
                                isCompleted = true
                            )

                            _gameState.value = _gameState.value.copy(
                                currentRound = updatedRound,
                                totalScore = _gameState.value.totalScore + scoreResponse.score,
                                completedRounds = _gameState.value.completedRounds + updatedRound,
                                isRoundActive = false
                            )

                            _uiState.value = _uiState.value.copy(
                                isSubmittingGuess = false,
                                showResults = true,
                                lastScoreResponse = scoreResponse
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSubmittingGuess = false,
                            error = error.message ?: "Failed to submit guess"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingGuess = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun checkStreetViewAvailability(locationId: Int) {
        viewModelScope.launch(Dispatchers.IO) { // PERFORMANCE: Explizit IO-Thread verwenden
            try {
                val result = gameRepository.checkStreetViewAvailability(locationId)
                result.fold(
                    onSuccess = { isAvailable ->
                        // PERFORMANCE: UI-Update auf Main Thread
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                streetViewAvailable = isAvailable,
                                streetViewReady = isAvailable
                            )
                        }
                    },
                    onFailure = {
                        // PERFORMANCE: UI-Update auf Main Thread - Assume available if check fails
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                streetViewAvailable = true,
                                streetViewReady = true
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                // PERFORMANCE: UI-Update auf Main Thread - Assume available if check fails
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        streetViewAvailable = true,
                        streetViewReady = true
                    )
                }
            }
        }
    }

    fun onStreetViewReady() {
        _uiState.value = _uiState.value.copy(streetViewReady = true)
    }

    fun showGuessMap() {
        _uiState.value = _uiState.value.copy(showGuessMap = true)
    }

    fun hideGuessMap() {
        _uiState.value = _uiState.value.copy(showGuessMap = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun nextRound() {
        _uiState.value = _uiState.value.copy(
            showResults = false,
            lastScoreResponse = null
        )
        startNewRound()
    }

    fun resetGame() {
        _gameState.value = GameState()
        _uiState.value = GameUiState()
    }
}
