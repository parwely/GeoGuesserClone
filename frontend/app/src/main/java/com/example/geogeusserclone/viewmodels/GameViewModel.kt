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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private var currentUserId: String? = null
    private var preloadedLocations = mutableListOf<LocationEntity>()

    init {
        initializeUser()
        preloadGameLocations()
    }

    private fun initializeUser() {
        viewModelScope.launch {
            currentUserId = userRepository.getCurrentUser()?.id
        }
    }

    private fun preloadGameLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                locationRepository.preloadLocations()
                // Cache 5 Locations für bessere Performance
                repeat(5) {
                    locationRepository.getRandomLocation().getOrNull()?.let { location ->
                        preloadedLocations.add(location)
                    }
                }
            } catch (e: Exception) {
                // Silent fail - App funktioniert weiter ohne Preloading
            }
        }
    }

    fun startNewGame() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val userId = currentUserId ?: "offline_user_${System.currentTimeMillis()}"

                println("GameViewModel: Starte neues Spiel für User: $userId")

                // Erstelle Spiel - mit aggressivem Fallback
                val gameResult = try {
                    // Versuche erst Backend
                    withTimeout(5000L) { // 5 Sekunden Timeout
                        gameRepository.createGame(userId, "single", 5)
                    }
                } catch (e: Exception) {
                    println("GameViewModel: Backend nicht verfügbar, verwende Offline-Modus: ${e.message}")
                    // Offline Fallback
                    gameRepository.createGame(userId, "single", 5)
                }

                gameResult.fold(
                    onSuccess = { game ->
                        println("GameViewModel: Spiel erfolgreich erstellt: ${game.id}")
                        _uiState.update {
                            it.copy(
                                currentGame = game,
                                isLoading = false,
                                showMap = false,
                                showRoundResult = false,
                                showGameCompletion = false,
                                revealGuessResult = null,
                                timeRemaining = Constants.MAX_ROUND_TIME_MS,
                                currentRound = 1,
                                totalRounds = game.totalRounds,
                                gameScore = 0
                            )
                        }
                        // Lade sofort die erste Location
                        loadNextLocationWithFallback()
                    },
                    onFailure = { error ->
                        println("GameViewModel: Kritischer Fehler: ${error.message}")
                        // Notfall-Fallback: Starte ohne Backend
                        startOfflineGame(userId)
                    }
                )
            } catch (e: Exception) {
                println("GameViewModel: Unerwarteter Fehler: ${e.message}")
                // Ultimativer Fallback
                startOfflineGame(currentUserId ?: "emergency_user")
            }
        }
    }

    private suspend fun startOfflineGame(userId: String) {
        try {
            // Erstelle minimales Offline-Spiel
            val offlineGame = GameEntity(
                id = "offline_${System.currentTimeMillis()}",
                userId = userId,
                gameMode = "single",
                totalRounds = 5,
                currentRound = 1,
                score = 0,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis()
            )

            _uiState.update {
                it.copy(
                    currentGame = offlineGame,
                    isLoading = false,
                    currentRound = 1,
                    totalRounds = 5,
                    gameScore = 0,
                    error = "Offline-Modus aktiviert"
                )
            }

            // Lade sofort eine Fallback-Location
            loadOfflineLocation()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Kritischer Fehler: App kann nicht gestartet werden"
                )
            }
        }
    }

    private suspend fun loadNextLocationWithFallback() {
        try {
            _uiState.update { it.copy(isLoading = true) }
            
            println("GameViewModel: Lade nächste Location...")

            // 1. Versuche preloaded Location
            val location = if (preloadedLocations.isNotEmpty()) {
                println("GameViewModel: Verwende preloaded Location")
                preloadedLocations.removeAt(0) // API Level kompatible Alternative zu removeFirst()
            } else {
                // 2. Versuche Location Repository mit Timeout
                println("GameViewModel: Lade Location aus Repository...")
                try {
                    withTimeout(8000L) { // 8 Sekunden Timeout
                        locationRepository.getRandomLocation().getOrNull()
                    }
                } catch (e: Exception) {
                    println("GameViewModel: Repository Timeout, verwende Emergency Location")
                    null
                }
            }

            if (location != null) {
                println("GameViewModel: Location erfolgreich geladen: ${location.city}")
                _uiState.update { state ->
                    state.copy(
                        currentLocation = location,
                        isLoading = false,
                        error = null
                    )
                }
                startRoundTimer()
                preloadNextLocationInBackground()
            } else {
                // 3. Letzte Rettung: Emergency Location
                println("GameViewModel: Verwende Emergency Location")
                loadOfflineLocation()
            }
        } catch (e: Exception) {
            println("GameViewModel: Fehler beim Location laden: ${e.message}")
            loadOfflineLocation()
        }
    }

    private suspend fun loadOfflineLocation() {
        try {
            // Erstelle Emergency Location
            val emergencyLocation = LocationEntity(
                id = "emergency_${System.currentTimeMillis()}",
                latitude = 48.8566,
                longitude = 2.3522,
                imageUrl = "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800",
                country = "France",
                city = "Paris",
                difficulty = 2,
                isCached = true,
                isUsed = false
            )

            _uiState.update { state ->
                state.copy(
                    currentLocation = emergencyLocation,
                    isLoading = false,
                    error = "Offline-Modus: Fallback-Location verwendet"
                )
            }
            startRoundTimer()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Kritischer Fehler: Keine Location verfügbar"
                )
            }
        }
    }

    private fun preloadNextLocationInBackground() {
        if (preloadedLocations.size < 2) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    locationRepository.getRandomLocation().getOrNull()?.let { nextLocation ->
                        preloadedLocations.add(nextLocation)
                        println("GameViewModel: Nächste Location preloaded")
                    }
                } catch (e: Exception) {
                    println("GameViewModel: Preload fehlgeschlagen: ${e.message}")
                }
            }
        }
    }

    private suspend fun loadNextLocation() {
        loadNextLocationWithFallback()
    }

    private fun startRoundTimer() {
        gameTimer?.cancel()
        gameTimer = viewModelScope.launch {
            var timeLeft = Constants.MAX_ROUND_TIME_MS

            while (timeLeft > 0 && _uiState.value.currentLocation != null && !_uiState.value.showRoundResult) {
                _uiState.update { it.copy(timeRemaining = timeLeft) }
                delay(1000)
                timeLeft -= 1000
            }

            // Zeit abgelaufen - automatisch schlechten Guess abgeben
            if (timeLeft <= 0 && !_uiState.value.showRoundResult) {
                val currentLocation = _uiState.value.currentLocation
                if (currentLocation != null) {
                    // Zufällige Position als Guess (sehr schlecht)
                    submitGuess(0.0, 0.0)
                }
            }
        }
    }

    fun submitGuess(guessLat: Double, guessLng: Double) {
        viewModelScope.launch {
            try {
                val currentGame = _uiState.value.currentGame ?: return@launch
                val currentLocation = _uiState.value.currentLocation ?: return@launch
                val timeSpent = Constants.MAX_ROUND_TIME_MS - _uiState.value.timeRemaining

                gameTimer?.cancel()

                _uiState.update { it.copy(isLoading = true) }

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
                        // Update Game Entity lokal
                        val updatedGame = currentGame.copy(
                            score = currentGame.score + guess.score,
                            currentRound = currentGame.currentRound + 1
                        )

                        _uiState.update { state ->
                            state.copy(
                                currentGame = updatedGame,
                                revealGuessResult = guess,
                                showRoundResult = true,
                                showMap = false,
                                isLoading = false,
                                gameScore = updatedGame.score
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Fehler beim Übermitteln der Vermutung: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Unerwarteter Fehler: ${e.message}"
                    )
                }
            }
        }
    }

    fun proceedToNextRound() {
        viewModelScope.launch {
            val currentGame = _uiState.value.currentGame ?: return@launch

            if (currentGame.currentRound > currentGame.totalRounds) {
                // Spiel beenden
                completeGame()
            } else {
                // Nächste Runde
                _uiState.update { state ->
                    state.copy(
                        showRoundResult = false,
                        revealGuessResult = null,
                        currentRound = currentGame.currentRound,
                        timeRemaining = Constants.MAX_ROUND_TIME_MS
                    )
                }
                loadNextLocation()
            }
        }
    }

    private suspend fun completeGame() {
        try {
            val currentGame = _uiState.value.currentGame ?: return

            gameRepository.completeGame(currentGame.id).fold(
                onSuccess = { completedGame ->
                    _uiState.update { state ->
                        state.copy(
                            currentGame = completedGame,
                            showRoundResult = false,
                            showGameCompletion = true,
                            revealGuessResult = null
                        )
                    }

                    // Sync mit Backend im Hintergrund
                    syncGameResultWithBackend(completedGame)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = "Fehler beim Abschließen des Spiels: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Unerwarteter Fehler beim Spielende: ${e.message}")
            }
        }
    }

    private fun syncGameResultWithBackend(game: GameEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val guesses = gameRepository.getGuessesByGame(game.id).first()
                gameRepository.submitCompleteGameResult(game, guesses)
                // Silent success - Spiel ist bereits lokal gespeichert
            } catch (e: Exception) {
                // Silent fail - Spiel bleibt lokal gespeichert
            }
        }
    }

    fun showMap() {
        _uiState.update { it.copy(showMap = true) }
    }

    fun hideMap() {
        _uiState.update { it.copy(showMap = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getGameGuesses(): Flow<List<GuessEntity>> {
        val gameId = _uiState.value.currentGame?.id ?: return flowOf(emptyList())
        return gameRepository.getGuessesByGame(gameId)
    }

    override fun onCleared() {
        super.onCleared()
        gameTimer?.cancel()
    }
}