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
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Initialisierung wenn nötig
    }

    fun startNewGame() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    _state.update { it.copy(isLoading = false, error = "Benutzer nicht angemeldet") }
                    return@launch
                }

                val gameResult = gameRepository.createGame(
                    userId = currentUser.id,
                    gameMode = "classic",
                    rounds = 5
                )

                gameResult.fold(
                    onSuccess = { game ->
                        _state.update {
                            it.copy(
                                currentGame = game,
                                isLoading = false,
                                showMap = false,
                                showRoundResult = false,
                                showGameCompletion = false
                            )
                        }
                        loadNextLocation()
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Spiel konnte nicht erstellt werden: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Unerwarteter Fehler: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadNextLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            locationRepository.getRandomLocation().fold(
                onSuccess = { location ->
                    _state.update {
                        it.copy(
                            currentLocation = location,
                            isLoading = false,
                            error = null
                        )
                    }
                    startTimer()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Location konnte nicht geladen werden: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = Constants.MAX_ROUND_TIME_MS

            while (timeLeft > 0) {
                _state.update { it.copy(timeRemaining = timeLeft) }
                delay(1000)
                timeLeft -= 1000
            }

            // Zeit abgelaufen
            _state.update { it.copy(timeRemaining = 0) }
            // Automatisch schlechteste Vermutung abgeben
            submitTimeoutGuess()
        }
    }

    private fun submitTimeoutGuess() {
        val currentGame = _state.value.currentGame
        val currentLocation = _state.value.currentLocation

        if (currentGame != null && currentLocation != null) {
            // Zufällige Koordinaten für Timeout-Guess
            submitGuess(0.0, 0.0) // Worst possible guess
        }
    }

    fun submitGuess(guessLat: Double, guessLng: Double) {
        viewModelScope.launch {
            val currentGame = _state.value.currentGame
            val currentLocation = _state.value.currentLocation

            if (currentGame == null || currentLocation == null) {
                _state.update { it.copy(error = "Kein aktives Spiel oder Location") }
                return@launch
            }

            timerJob?.cancel()

            val timeSpent = Constants.MAX_ROUND_TIME_MS - _state.value.timeRemaining

            gameRepository.submitGuess(
                gameId = currentGame.id,
                locationId = currentLocation.id,
                guessLat = guessLat,
                guessLng = guessLng,
                actualLat = currentLocation.latitude,
                actualLng = currentLocation.longitude,
                timeSpent = timeSpent
            ).fold(
                onSuccess = { guess ->
                    _state.update {
                        it.copy(
                            currentGuess = guess,
                            showMap = false,
                            showRoundResult = true,
                            revealGuessResult = guess
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(error = "Fehler beim Speichern: ${error.message}") }
                }
            )
        }
    }

    fun proceedToNextRound() {
        viewModelScope.launch {
            val currentGame = _state.value.currentGame

            if (currentGame == null) return@launch

            if (currentGame.currentRound >= currentGame.totalRounds) {
                // Spiel beenden
                completeGame()
            } else {
                // Nächste Runde
                _state.update {
                    it.copy(
                        showRoundResult = false,
                        currentGuess = null,
                        revealGuessResult = null
                    )
                }
                loadNextLocation()
            }
        }
    }

    private fun completeGame() {
        viewModelScope.launch {
            val currentGame = _state.value.currentGame ?: return@launch

            gameRepository.completeGame(currentGame.id).fold(
                onSuccess = { completedGame ->
                    _state.update {
                        it.copy(
                            currentGame = completedGame,
                            showRoundResult = false,
                            showGameCompletion = true
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(error = "Fehler beim Beenden: ${error.message}") }
                }
            )
        }
    }

    fun showMap() {
        _state.update { it.copy(showMap = true) }
    }

    fun hideMap() {
        _state.update { it.copy(showMap = false) }
    }

    fun showRevealMap() {
        // Map mit Ergebnis anzeigen
        _state.update { it.copy(showMap = true) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun getGameGuesses(): Flow<List<GuessEntity>> {
        return _state.value.currentGame?.let { game ->
            gameRepository.getGuessesByGame(game.id)
        } ?: flowOf(emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}