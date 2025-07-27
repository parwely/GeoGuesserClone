package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
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
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var currentGame: GameEntity? = null

    init {
        loadActiveGame()
    }

    private fun loadActiveGame() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser != null) {
                    val activeGame = gameRepository.getActiveGame(currentUser.id)
                    if (activeGame != null && !activeGame.isCompleted) {
                        currentGame = activeGame
                        _state.value = _state.value.copy(currentGame = activeGame)
                        loadCurrentLocation()
                        loadGameGuesses(activeGame.id)
                        startTimer()
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Fehler beim Laden des Spiels: ${e.message}")
            }
        }
    }

    fun createNewGame() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null, gameCompleted = false)

                val currentUser = userRepository.getCurrentUser()
                    ?: throw Exception("Kein Benutzer angemeldet")

                val result = gameRepository.createGame(
                    userId = currentUser.id,
                    gameMode = "classic",
                    rounds = 5
                )

                result.onSuccess { game ->
                    currentGame = game
                    _state.value = _state.value.copy(
                        currentGame = game,
                        currentGuesses = emptyList(),
                        lastGuessResult = null,
                        showingResults = false,
                        isMapVisible = false
                    )
                    loadCurrentLocation()
                    startTimer()
                }.onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Fehler beim Erstellen des Spiels: ${exception.message}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Unerwarteter Fehler: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            try {
                val result = locationRepository.getRandomLocation()
                result.onSuccess { location ->
                    _state.value = _state.value.copy(
                        currentLocation = location,
                        isLoading = false
                    )
                }.onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Fehler beim Laden der Location: ${exception.message}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Fehler beim Laden der Location: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun submitGuess(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val game = currentGame ?: return@launch
                val location = _state.value.currentLocation ?: return@launch

                stopTimer()

                val result = gameRepository.submitGuess(
                    gameId = game.id,
                    locationId = location.id,
                    guessLat = latitude,
                    guessLng = longitude,
                    actualLat = location.latitude,
                    actualLng = location.longitude
                )

                result.onSuccess { guess ->
                    _state.value = _state.value.copy(
                        lastGuessResult = guess,
                        showingResults = true,
                        isGuessSubmitted = true,
                        comparisonGuessPoint = GeoPoint(latitude, longitude),
                        comparisonActualPoint = GeoPoint(location.latitude, location.longitude)
                    )

                    // Reload current game to get updated score
                    val updatedGame = gameRepository.getActiveGame(game.userId)
                    updatedGame?.let {
                        currentGame = it
                        _state.value = _state.value.copy(currentGame = it)
                    }

                    loadGameGuesses(game.id)
                }.onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Fehler beim Übermitteln der Vermutung: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Unerwarteter Fehler: ${e.message}"
                )
            }
        }
    }

    fun proceedToNextRound() {
        viewModelScope.launch {
            val game = currentGame ?: return@launch

            if (game.currentRound > game.totalRounds) {
                // Spiel beendet
                completeGame()
            } else {
                // Nächste Runde
                _state.value = _state.value.copy(
                    showingResults = false,
                    lastGuessResult = null,
                    isGuessSubmitted = false,
                    comparisonGuessPoint = null,
                    comparisonActualPoint = null,
                    isLoading = true
                )
                loadCurrentLocation()
                startTimer()
            }
        }
    }

    private fun completeGame() {
        viewModelScope.launch {
            val game = currentGame ?: return@launch

            gameRepository.completeGame(game.id).onSuccess {
                _state.value = _state.value.copy(
                    gameCompleted = true,
                    showingResults = false
                )
                stopTimer()
            }
        }
    }

    fun showMap() {
        _state.value = _state.value.copy(isMapVisible = true)
    }

    fun hideMap() {
        _state.value = _state.value.copy(isMapVisible = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun loadGameGuesses(gameId: String) {
        viewModelScope.launch {
            gameRepository.getGuessesByGame(gameId).collect { guesses ->
                _state.value = _state.value.copy(currentGuesses = guesses)
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            var timeRemaining = Constants.MAX_ROUND_TIME_MS
            _state.value = _state.value.copy(timeRemaining = timeRemaining)

            while (timeRemaining > 0 && !_state.value.isGuessSubmitted) {
                delay(1000)
                timeRemaining -= 1000
                _state.value = _state.value.copy(timeRemaining = timeRemaining)
            }

            if (timeRemaining <= 0 && !_state.value.isGuessSubmitted) {
                // Zeit abgelaufen - automatisch eine Vermutung abgeben
                val location = _state.value.currentLocation
                if (location != null) {
                    submitGuess(0.0, 0.0) // Default position
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }


    fun showLocationReveal(actualLocation: LocationEntity, guessResult: GuessEntity) {
        setState(state.value.copy(
            isMapVisible = true,
            showLocationReveal = true,
            revealActualLocation = actualLocation,
            revealGuessResult = guessResult
        ))
    }

    fun hideLocationReveal() {
        setState(
            state.value.copy(
                showLocationReveal = false,
                revealActualLocation = null,
                revealGuessResult = null
            )
        )
    }

    // setState Funktion als private Funktion in der Klasse
    private fun setState(newState: GameState) {
        _state.value = newState
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}