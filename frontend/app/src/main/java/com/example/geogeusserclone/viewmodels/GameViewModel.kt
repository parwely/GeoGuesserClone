package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import com.example.geogeusserclone.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import javax.inject.Inject

data class GameState(
    val isLoading: Boolean = false,
    val currentGame: GameEntity? = null,
    val currentLocation: LocationEntity? = null,
    val currentGuesses: List<GuessEntity> = emptyList(),
    val isGuessSubmitted: Boolean = false,
    val lastGuessResult: GuessEntity? = null,
    val error: String? = null,
    val gameCompleted: Boolean = false,
    val showingResults: Boolean = false,
    val timeRemaining: Long = 0L,
    val isMapVisible: Boolean = false
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : BaseViewModel<GameState>(GameState()) {

    private var gameTimer: kotlinx.coroutines.Job? = null

    fun createNewGame(gameMode: String = Constants.GAME_MODE_SINGLE, rounds: Int = Constants.DEFAULT_GAME_ROUNDS) {
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUser()
            if (currentUser == null) {
                setState(state.value.copy(error = "Benutzer nicht angemeldet"))
                return@launch
            }

            setState(state.value.copy(isLoading = true, error = null))

            gameRepository.createGame(currentUser.id, gameMode, rounds)
                .onSuccess { game ->
                    setState(state.value.copy(
                        isLoading = false,
                        currentGame = game,
                        gameCompleted = false,
                        showingResults = false
                    ))
                    loadNextLocation()
                    observeGameGuesses(game.id)
                }
                .onFailure { exception ->
                    setState(state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Spiel konnte nicht erstellt werden"
                    ))
                }
        }
    }

    fun loadNextLocation() {
        viewModelScope.launch {
            setState(state.value.copy(isLoading = true))

            locationRepository.getRandomLocation()
                .onSuccess { location ->
                    setState(state.value.copy(
                        isLoading = false,
                        currentLocation = location,
                        isGuessSubmitted = false,
                        lastGuessResult = null,
                        showingResults = false,
                        isMapVisible = false
                    ))
                    startRoundTimer()
                }
                .onFailure { exception ->
                    setState(state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Location konnte nicht geladen werden"
                    ))
                }
        }
    }

    fun submitGuess(guessLat: Double, guessLng: Double) {
        val game = state.value.currentGame
        val location = state.value.currentLocation

        if (game == null || location == null) {
            setState(state.value.copy(error = "Spiel oder Location nicht verfügbar"))
            return
        }

        if (state.value.isGuessSubmitted) {
            return // Prevent double submission
        }

        viewModelScope.launch {
            setState(state.value.copy(isLoading = true, isGuessSubmitted = true))
            stopRoundTimer()

            gameRepository.submitGuess(
                gameId = game.id,
                locationId = location.id,
                guessLat = guessLat,
                guessLng = guessLng,
                actualLat = location.latitude,
                actualLng = location.longitude
            ).onSuccess { guess ->
                setState(state.value.copy(
                    isLoading = false,
                    lastGuessResult = guess,
                    showingResults = true
                ))

                // Check if game is completed
                checkGameCompletion()

            }.onFailure { exception ->
                setState(state.value.copy(
                    isLoading = false,
                    isGuessSubmitted = false,
                    error = exception.message ?: "Guess konnte nicht übermittelt werden"
                ))
            }
        }
    }

    private fun checkGameCompletion() {
        val game = state.value.currentGame ?: return

        viewModelScope.launch {
            val updatedGame = gameRepository.getActiveGame(game.userId)
            if (updatedGame?.isCompleted == true) {
                gameRepository.completeGame(game.id)
                setState(state.value.copy(
                    currentGame = updatedGame,
                    gameCompleted = true
                ))
            }
        }
    }

    fun proceedToNextRound() {
        val game = state.value.currentGame
        if (game?.isCompleted == true) {
            setState(state.value.copy(gameCompleted = true))
        } else {
            loadNextLocation()
            setState(state.value.copy(
                currentGame = game?.copy(currentRound = game.currentRound + 1)
            ))
        }
    }

    fun showMap() {
        setState(state.value.copy(isMapVisible = true))
    }

    fun hideMap() {
        setState(state.value.copy(isMapVisible = false))
    }

    private fun observeGameGuesses(gameId: String) {
        viewModelScope.launch {
            gameRepository.getGuessesByGame(gameId).collectLatest { guesses ->
                setState(state.value.copy(currentGuesses = guesses))
            }
        }
    }

    private fun startRoundTimer(duration: Long = 60000L) {
        gameTimer?.cancel()
        gameTimer = viewModelScope.launch {
            var timeRemaining = duration
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining -= 1000L
                setState(state.value.copy(timeRemaining = timeRemaining))
            }
            submitGuess(0.0, 0.0) // Automatische Abgabe bei Zeitablauf
        }
    }

    private fun stopRoundTimer() {
        gameTimer?.cancel()
    }

    fun clearError() {
        setState(state.value.copy(error = null))
    }

    override fun onCleared() {
        super.onCleared()
        stopRoundTimer()
    }
}