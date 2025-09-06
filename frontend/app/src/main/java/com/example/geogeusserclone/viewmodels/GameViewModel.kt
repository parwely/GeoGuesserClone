package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.repositories.GameRepository
import com.example.geogeusserclone.data.repositories.LocationRepository
import com.example.geogeusserclone.data.repositories.UserRepository
import com.example.geogeusserclone.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
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

    // Optimierte Location-Management
    private val gameLocations = mutableListOf<LocationEntity>()
    private val usedLocationIds = mutableSetOf<String>()
    private var locationPreloadJob: Job? = null
    private var currentLocationIndex = 0

    init {
        initializeUser()
        startLocationPreloading()
    }

    private fun initializeUser() {
        viewModelScope.launch {
            currentUserId = userRepository.getCurrentUser()?.id
        }
    }

    private fun startLocationPreloading() {
        locationPreloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                println("GameViewModel: Starte SEQUENZIELLE Location-Beschaffung...")

                val uniqueLocations = mutableListOf<LocationEntity>()
                val maxAttempts = 12 // Mehr Versuche für Street View
                var attempts = 0

                while (uniqueLocations.size < 5 && attempts < maxAttempts) {
                    attempts++
                    val locationResult = locationRepository.getRandomLocation()
                    locationResult.getOrNull()?.let { location ->
                        // Prüfe auf Duplikate und Street View-URL
                        val isDuplicate = uniqueLocations.any { existing ->
                            val latDiff = kotlin.math.abs(existing.latitude - location.latitude)
                            val lngDiff = kotlin.math.abs(existing.longitude - location.longitude)
                            latDiff < 0.001 && lngDiff < 0.001
                        }
                        val isStreetView = location.imageUrl.startsWith("https://maps.googleapis.com/maps/api/streetview")
                        if (!isDuplicate && isStreetView) {
                            uniqueLocations.add(location)
                            println("GameViewModel: StreetView-Location ${uniqueLocations.size}: ${location.city} (${location.country})")
                        } else if (!isStreetView) {
                            println("GameViewModel: Überspringe Fallback/Unsplash-Location: ${location.city}")
                        } else {
                            println("GameViewModel: Duplikat übersprungen: ${location.city}")
                        }
                    }
                    if (attempts < maxAttempts) {
                        delay(200)
                    }
                }

                if (uniqueLocations.size >= 5) {
                    gameLocations.clear()
                    gameLocations.addAll(uniqueLocations.take(5))
                    println("GameViewModel: ✅ ${gameLocations.size} StreetView-Locations geladen")
                } else {
                    // Fallback: Fülle mit generierten Locations auf
                    gameLocations.clear()
                    gameLocations.addAll(uniqueLocations)
                    val missingCount = 5 - uniqueLocations.size
                    gameLocations.addAll(generateUniqueLocations(missingCount, uniqueLocations))
                    println("GameViewModel: ⚠️ ${gameLocations.size} Locations (${uniqueLocations.size} StreetView, ${missingCount} generiert)")
                }
                usedLocationIds.clear()

            } catch (e: Exception) {
                println("GameViewModel: Preloading-Fehler: ${e.message}, verwende Fallback")
                createFallbackLocations()
            }
        }
    }

    fun startNewGame() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val userId = currentUserId ?: "offline_user_${System.currentTimeMillis()}"
                println("GameViewModel: Starte neues Spiel für User: $userId")

                // Warte auf Location-Preloading (max 3 Sekunden)
                withTimeoutOrNull(3000L) {
                    locationPreloadJob?.join()
                }

                // Stelle sicher, dass wir Locations haben
                if (gameLocations.isEmpty()) {
                    createFallbackLocations()
                }

                // Erstelle Spiel mit optimiertem Backend-Aufruf
                val gameResult = createGameWithFallback(userId)

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
                        currentLocationIndex = 0
                        loadNextUniqueLocation()
                    },
                    onFailure = { error ->
                        println("GameViewModel: Spiel-Erstellung fehlgeschlagen: ${error.message}")
                        startEmergencyGame(userId)
                    }
                )
            } catch (e: Exception) {
                println("GameViewModel: Kritischer Fehler: ${e.message}")
                startEmergencyGame(currentUserId ?: "emergency_user")
            }
        }
    }

    private suspend fun createGameWithFallback(userId: String): Result<GameEntity> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Parallele Versuche: Backend und Offline
            val backendDeferred = async {
                try {
                    withTimeout(4000L) {
                        gameRepository.createGame(userId, "single", 5)
                    }
                } catch (e: Exception) {
                    Result.failure<GameEntity>(e)
                }
            }

            val offlineDeferred = async {
                delay(4500L) // Warte 4.5s bevor Offline-Fallback
                createOfflineGame(userId)
            }

            // Nimm das erste erfolgreiche Ergebnis
            select<Result<GameEntity>> {
                backendDeferred.onAwait { result ->
                    if (result.isSuccess) {
                        offlineDeferred.cancel()
                        result
                    } else {
                        offlineDeferred.await()
                    }
                }
                offlineDeferred.onAwait { result ->
                    backendDeferred.cancel()
                    result
                }
            }
        } catch (e: Exception) {
            createOfflineGame(userId)
        }
    }

    private suspend fun createOfflineGame(userId: String): Result<GameEntity> {
        return try {
            // Stelle sicher, dass User existiert
            val user = userRepository.getCurrentUser() ?: run {
                val emergencyUser = UserEntity(
                    id = userId,
                    username = "Offline User",
                    email = "offline@local.com",
                    totalScore = 0,
                    gamesPlayed = 0,
                    bestScore = 0,
                    lastLoginAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                userRepository.insertEmergencyUser(emergencyUser)
                emergencyUser
            }

            val offlineGame = GameEntity(
                id = "offline_${System.currentTimeMillis()}",
                userId = user.id,
                gameMode = "single",
                totalRounds = 5,
                currentRound = 1,
                score = 0,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis()
            )

            Result.success(offlineGame)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun startEmergencyGame(userId: String) {
        try {
            createFallbackLocations() // Notfall-Locations erstellen

            val emergencyGame = GameEntity(
                id = "emergency_${System.currentTimeMillis()}",
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
                    currentGame = emergencyGame,
                    isLoading = false,
                    currentRound = 1,
                    totalRounds = 5,
                    gameScore = 0,
                    error = "Notfall-Modus: Offline-Spiel gestartet"
                )
            }

            currentLocationIndex = 0
            loadNextUniqueLocation()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Kritischer Fehler: Spiel kann nicht gestartet werden"
                )
            }
        }
    }

    private fun loadNextUniqueLocation() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val nextLocation = getNextUniqueLocation()

                if (nextLocation != null) {
                    println("GameViewModel: Lade Location ${currentLocationIndex}: ${nextLocation.city}")

                    _uiState.update { state ->
                        state.copy(
                            currentLocation = nextLocation,
                            isLoading = false,
                            error = null
                        )
                    }
                    startRoundTimer()

                    // Preload nächste Location im Hintergrund
                    preloadNextLocationInBackground()
                } else {
                    // Keine Locations mehr verfügbar
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Keine weiteren Locations verfügbar"
                        )
                    }
                }
            } catch (e: Exception) {
                println("GameViewModel: Fehler beim Location-Laden: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler beim Laden der Location: ${e.message}"
                    )
                }
            }
        }
    }

    private fun getNextUniqueLocation(): LocationEntity? {
        return if (currentLocationIndex < gameLocations.size) {
            val location = gameLocations[currentLocationIndex]
            usedLocationIds.add(location.id)
            currentLocationIndex++
            location
        } else {
            null // Alle Locations verwendet
        }
    }

    private fun preloadNextLocationInBackground() {
        // Background-Preloading für bessere Performance beim nächsten Laden
        viewModelScope.launch(Dispatchers.IO) {
            if (currentLocationIndex < gameLocations.size) {
                try {
                    // Preload Image für nächste Location
                    val nextLocation = gameLocations[currentLocationIndex]
                    // Hier könnte man das Bild vorläufig cachen
                    println("GameViewModel: Preloading Location ${currentLocationIndex + 1}: ${nextLocation.city}")
                } catch (e: Exception) {
                    // Silent fail
                }
            }
        }
    }

    private fun generateUniqueLocations(count: Int, existing: List<LocationEntity>): List<LocationEntity> {
        val existingCoords = existing.map { "${it.latitude}-${it.longitude}" }.toSet()
        val fallbackLocations = listOf(
            LocationEntity("gen_paris", 48.8566, 2.3522, "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800", "France", "Paris", 2, true, false),
            LocationEntity("gen_london", 51.5074, -0.1278, "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800", "United Kingdom", "London", 2, true, false),
            LocationEntity("gen_newyork", 40.7128, -74.0060, "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800", "United States", "New York", 3, true, false),
            LocationEntity("gen_tokyo", 35.6762, 139.6503, "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800", "Japan", "Tokyo", 4, true, false),
            LocationEntity("gen_sydney", -33.8688, 151.2093, "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800", "Australia", "Sydney", 3, true, false),
            LocationEntity("gen_berlin", 52.5200, 13.4050, "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800", "Germany", "Berlin", 2, true, false),
            LocationEntity("gen_rome", 41.9028, 12.4964, "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800", "Italy", "Rome", 3, true, false),
            LocationEntity("gen_barcelona", 41.3851, 2.1734, "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800", "Spain", "Barcelona", 3, true, false)
        )

        return fallbackLocations
            .filter { "${it.latitude}-${it.longitude}" !in existingCoords }
            .take(count)
    }

    private fun createFallbackLocations() {
        gameLocations.clear()
        gameLocations.addAll(generateUniqueLocations(5, emptyList()))
        println("GameViewModel: ${gameLocations.size} Fallback-Locations erstellt")
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

            if (currentGame.currentRound < currentGame.totalRounds) {
                // Nächste Runde
                _uiState.update { state ->
                    state.copy(
                        showRoundResult = false,
                        revealGuessResult = null,
                        showMap = false,
                        currentRound = currentGame.currentRound + 1,
                        timeRemaining = Constants.MAX_ROUND_TIME_MS
                    )
                }

                loadNextUniqueLocation()
            } else {
                // Spiel beendet
                completeGame()
            }
        }
    }

    private suspend fun completeGame() {
        val currentGame = _uiState.value.currentGame ?: return

        gameRepository.completeGame(currentGame.id)

        _uiState.update {
            it.copy(
                showGameCompletion = true,
                showRoundResult = false,
                showMap = false
            )
        }
    }

    fun showMap() {
        _uiState.update { it.copy(showMap = true) }
    }

    fun hideMap() {
        _uiState.update { it.copy(showMap = false) }
    }

    fun getGameGuesses(): Flow<List<GuessEntity>> {
        val currentGame = _uiState.value.currentGame
        return if (currentGame != null) {
            gameRepository.getGuessesByGame(currentGame.id)
        } else {
            flowOf(emptyList())
        }
    }

    private fun startRoundTimer() {
        gameTimer?.cancel()
        gameTimer = viewModelScope.launch {
            var timeLeft = Constants.MAX_ROUND_TIME_MS
            while (timeLeft > 0) {
                _uiState.update { it.copy(timeRemaining = timeLeft) }
                delay(1000)
                timeLeft -= 1000
            }
            // Zeit abgelaufen
            submitGuess(0.0, 0.0) // Default guess bei Timeout
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameTimer?.cancel()
        locationPreloadJob?.cancel()
    }
}