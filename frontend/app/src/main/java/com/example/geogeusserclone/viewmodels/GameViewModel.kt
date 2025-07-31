package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import com.example.geogeusserclone.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var gameTimer: Job? = null
    private var currentUser: com.example.geogeusserclone.data.database.entities.UserEntity? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUser = userRepository.getCurrentUser()
        }
    }

    fun startNewGame(gameMode: String = "single", rounds: Int = 5) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val user = currentUser ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Benutzer nicht angemeldet"
                    )
                    return@launch
                }

                // Erstelle neues Spiel über Repository (mit Backend-Integration)
                val gameResult = gameRepository.createGame(user.id, gameMode, rounds)

                if (gameResult.isSuccess) {
                    val game = gameResult.getOrNull()!!

                    // Lade erste Location
                    loadNextLocation(game)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Spiel konnte nicht erstellt werden: ${gameResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Starten des Spiels: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadNextLocation(game: GameEntity) {
        try {
            val locationResult = locationRepository.getRandomLocation()

            if (locationResult.isSuccess) {
                val location = locationResult.getOrNull()!!

                _uiState.value = _uiState.value.copy(
                    currentGame = game,
                    currentLocation = location,
                    isLoading = false,
                    showMap = false,
                    showRoundResult = false,
                    showGameCompletion = false,
                    timeRemaining = Constants.MAX_ROUND_TIME_MS,
                    currentRound = game.currentRound,
                    totalRounds = game.totalRounds,
                    gameScore = game.score
                )

                startRoundTimer()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Location konnte nicht geladen werden"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Fehler beim Laden der Location: ${e.message}"
            )
        }
    }

    private fun startRoundTimer() {
        gameTimer?.cancel()
        gameTimer = viewModelScope.launch {
            var timeLeft = Constants.MAX_ROUND_TIME_MS

            while (timeLeft > 0 && !_uiState.value.showMap && !_uiState.value.showRoundResult) {
                delay(1000)
                timeLeft -= 1000
                _uiState.value = _uiState.value.copy(timeRemaining = timeLeft)
            }

            // Zeit abgelaufen - automatisch zur Karte wechseln
            if (timeLeft <= 0 && !_uiState.value.showMap && !_uiState.value.showRoundResult) {
                showMap()
            }
        }
    }

    fun showMap() {
        gameTimer?.cancel()
        _uiState.value = _uiState.value.copy(showMap = true)
    }

    fun hideMap() {
        _uiState.value = _uiState.value.copy(showMap = false)
        startRoundTimer()
    }

    fun submitGuess(guessLat: Double, guessLng: Double) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val game = currentState.currentGame ?: return@launch
            val location = currentState.currentLocation ?: return@launch

            _uiState.value = currentState.copy(isLoading = true)

            try {
                val timeSpent = Constants.MAX_ROUND_TIME_MS - currentState.timeRemaining

                val guessResult = gameRepository.submitGuess(
                    gameId = game.id,
                    locationId = location.id,
                    guessLat = guessLat,
                    guessLng = guessLng,
                    actualLat = location.latitude,
                    actualLng = location.longitude,
                    timeSpent = timeSpent
                )

                if (guessResult.isSuccess) {
                    val guess = guessResult.getOrNull()!!

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showMap = false,
                        showRoundResult = true,
                        revealGuessResult = guess,
                        gameScore = game.score + guess.score
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Guess konnte nicht übermittelt werden"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Übermitteln des Guess: ${e.message}"
                )
            }
        }
    }

    fun proceedToNextRound() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val game = currentState.currentGame ?: return@launch

            if (game.currentRound >= game.totalRounds) {
                // Spiel beenden
                completeGame(game)
            } else {
                // Nächste Runde
                val updatedGame = game.copy(currentRound = game.currentRound + 1)
                loadNextLocation(updatedGame)
            }
        }
    }

    private suspend fun completeGame(game: GameEntity) {
        try {
            val completionResult = gameRepository.completeGame(game.id)

            if (completionResult.isSuccess) {
                val completedGame = completionResult.getOrNull()!!

                _uiState.value = _uiState.value.copy(
                    currentGame = completedGame,
                    showRoundResult = false,
                    showGameCompletion = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Spiel konnte nicht abgeschlossen werden"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Fehler beim Abschließen des Spiels: ${e.message}"
            )
        }
    }

    fun getGameGuesses(): Flow<List<GuessEntity>> {
        val gameId = _uiState.value.currentGame?.id
        return if (gameId != null) {
            gameRepository.getGuessesByGame(gameId)
        } else {
            flowOf(emptyList())
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        gameTimer?.cancel()
    }
}
