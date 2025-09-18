/**
 * GameViewModel.kt
 *
 * Diese Datei enthält das zentrale ViewModel für die Spiellogik der GeoGuess-App.
 * Sie verwaltet den gesamten Spielzustand und koordiniert alle Spielmodi.
 *
 * Architektur-Integration:
 * - MVVM Pattern: Zentrale Geschäftslogik für Spiel-UI
 * - State Management: Verwaltet GameState und GameUiState
 * - Repository Integration: Nutzt GameRepository für API-Calls
 * - Coroutines: Asynchrone Operationen für Netzwerk und Timer
 * - Multi-Mode Support: Unterstützt Classic, Blitz und Endless Modi
 */
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Haupt-ViewModel für Spiellogik und Zustandsverwaltung
 *
 * Dieses ViewModel koordiniert alle Aspekte des Spiels:
 * - Rundenmanagement (neue Runden, Bewertung, Navigation)
 * - Spielmodus-spezifische Logik (Classic, Blitz, Endless)
 * - Timer-Management für zeitbasierte Modi
 * - Score-Berechnung und Statistiken
 * - UI-Zustand und Benutzerinteraktionen
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    // Spielzustand - enthält alle spielrelevanten Daten
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // UI-Zustand - enthält UI-spezifische Daten wie Loading, Errors, etc.
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Timer für zeitbasierte Spielmodi
    private var timerJob: Job? = null

    // Konfigurationen für verschiedene Spielmodi
    private val blitzConfig = BlitzModeConfig()
    private val endlessConfig = EndlessModeConfig()

    /**
     * Startet ein neues Spiel mit dem angegebenen Modus
     *
     * Initialisiert den Spielzustand entsprechend dem gewählten Modus und
     * lädt die erste Runde. Jeder Modus hat eigene Regeln und Zeitlimits.
     *
     * @param gameMode Der zu startende Spielmodus (Classic, Blitz, Endless)
     */
    fun startGame(gameMode: GameMode = GameMode.CLASSIC) {
        viewModelScope.launch {
            val initialState = when (gameMode) {
                GameMode.CLASSIC -> GameState(
                    gameMode = GameMode.CLASSIC,
                    maxRounds = 5,
                    isTimeLimited = false,
                    isMovementDisabled = false
                )
                GameMode.BLITZ -> GameState(
                    gameMode = GameMode.BLITZ,
                    maxRounds = blitzConfig.totalRounds,
                    roundTimeLimit = blitzConfig.roundTimeLimit,
                    isTimeLimited = true,
                    isMovementDisabled = blitzConfig.movementDisabled
                )
                GameMode.ENDLESS -> GameState(
                    gameMode = GameMode.ENDLESS,
                    maxRounds = -1, // Unbegrenzt
                    isTimeLimited = false,
                    isMovementDisabled = false
                )
            }

            _gameState.value = initialState
            _uiState.value = GameUiState(isLoading = true)

            // Lade erste Runde
            startNewRound()
        }
    }

    /**
     * Startet eine neue Spielrunde
     *
     * Lädt eine neue Location vom Repository und initialisiert den Rundenzustand.
     * Startet Timer falls erforderlich und setzt UI-Zustand zurück.
     */
    private suspend fun startNewRound() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Hole neue Location vom Repository
            val roundResult = gameRepository.startNewRound(
                difficulty = getDynamicDifficulty(),
                category = null
            )

            if (roundResult.isSuccess) {
                val newRoundResponse = roundResult.getOrThrow()
                val newRound = GameRound(
                    roundId = newRoundResponse.roundId,
                    location = newRoundResponse.location
                )

                // Aktualisiere Spielzustand
                _gameState.value = _gameState.value.copy(
                    currentRound = newRound,
                    isRoundActive = true,
                    currentRoundNumber = _gameState.value.currentRoundNumber
                )

                // Setze UI-Zustand zurück
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streetViewReady = false,
                    streetViewAvailable = true,
                    showGuessMap = false,
                    showResults = false,
                    timeRemaining = _gameState.value.roundTimeLimit ?: 0L
                )

                // Starte Timer falls erforderlich
                if (_gameState.value.isTimeLimited) {
                    startRoundTimer()
                }

                println("GameViewModel: ✅ Neue Runde ${_gameState.value.currentRoundNumber} gestartet")
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Laden der neuen Runde: ${roundResult.exceptionOrNull()?.message}"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Unerwarteter Fehler: ${e.message}"
            )
        }
    }

    /**
     * Startet den Countdown-Timer für zeitbasierte Modi
     *
     * Läuft nur im Blitz-Modus und reduziert kontinuierlich die verbleibende Zeit.
     * Löst automatischen Guess aus wenn Zeit abläuft.
     */
    private fun startRoundTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val timeLimit = _gameState.value.roundTimeLimit ?: return@launch
            var remainingTime = timeLimit

            while (remainingTime > 0 && _gameState.value.isRoundActive) {
                _uiState.value = _uiState.value.copy(
                    timeRemaining = remainingTime,
                    isTimeRunningOut = remainingTime <= 10000 // Warnung bei 10s
                )

                delay(100) // Update alle 100ms für flüssige Animation
                remainingTime -= 100
            }

            // Zeit abgelaufen - automatischer Guess
            if (_gameState.value.isRoundActive) {
                println("GameViewModel: ⏰ Zeit abgelaufen - automatischer Guess")
                autoGuessOnTimeout()
            }
        }
    }

    /**
     * Führt automatischen Guess bei Zeitablauf durch
     *
     * Wird nur im Blitz-Modus verwendet wenn der Timer abläuft.
     * Ratet zufällige Koordinaten mit 0 Punkten.
     */
    private suspend fun autoGuessOnTimeout() {
        // Zufällige Koordinaten für Auto-Guess
        val randomLat = (Math.random() * 180) - 90
        val randomLng = (Math.random() * 360) - 180

        submitGuess(randomLat, randomLng, isAutoGuess = true)
    }

    /**
     * Berechnet dynamische Schwierigkeit basierend auf Spielmodus und Fortschritt
     *
     * @return Schwierigkeitsgrad von 1 (einfach) bis 3 (schwer)
     */
    private fun getDynamicDifficulty(): Int {
        return when (_gameState.value.gameMode) {
            GameMode.CLASSIC -> 2 // Standard-Schwierigkeit
            GameMode.BLITZ -> 1 // Einfacher wegen Zeitdruck
            GameMode.ENDLESS -> {
                // Schwierigkeit steigt mit Streak
                when (_gameState.value.streak) {
                    in 0..2 -> 1
                    in 3..7 -> 2
                    else -> 3
                }
            }
        }
    }

    /**
     * Verarbeitet Benutzer-Guess und berechnet Score
     *
     * Sendet den Guess an das Repository, berechnet Score und aktualisiert
     * den Spielzustand entsprechend.
     *
     * @param guessLat Geratene Latitude
     * @param guessLng Geratene Longitude
     * @param isAutoGuess Ob dies ein automatischer Guess bei Timeout ist
     */
    fun submitGuess(guessLat: Double, guessLng: Double, isAutoGuess: Boolean = false) {
        val currentRound = _gameState.value.currentRound ?: return

        viewModelScope.launch {
            try {
                timerJob?.cancel() // Stoppe Timer
                _uiState.value = _uiState.value.copy(isSubmittingGuess = true)

                val guessRequest = GuessRequest(
                    roundId = currentRound.roundId,
                    guessLat = guessLat,
                    guessLng = guessLng,
                    timeSpentSeconds = calculateTimeSpent()
                )

                val scoreResult = gameRepository.submitGuess(guessRequest)

                if (scoreResult.isSuccess) {
                    val scoreResponse = scoreResult.getOrThrow()

                    // Berechne Bonus-Punkte für spezielle Modi
                    val bonusScore = calculateBonusScore(scoreResponse, isAutoGuess)
                    val finalScore = scoreResponse.score + bonusScore

                    // Aktualisiere Spielzustand
                    updateGameStateAfterGuess(scoreResponse, finalScore, isAutoGuess)

                    // Zeige Ergebnisse
                    _uiState.value = _uiState.value.copy(
                        isSubmittingGuess = false,
                        showResults = true,
                        lastScoreResponse = scoreResponse.copy(score = finalScore)
                    )

                    println("GameViewModel: ✅ Guess verarbeitet - Score: $finalScore (Original: ${scoreResponse.score}, Bonus: $bonusScore)")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingGuess = false,
                        error = "Fehler beim Verarbeiten des Guess: ${scoreResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingGuess = false,
                    error = "Unerwarteter Fehler beim Guess: ${e.message}"
                )
            }
        }
    }

    /**
     * Berechnet verbrachte Zeit für aktuelle Runde
     *
     * @return Zeit in Sekunden seit Rundenstart
     */
    private fun calculateTimeSpent(): Int {
        val timeLimit = _gameState.value.roundTimeLimit ?: return 0
        val remaining = _uiState.value.timeRemaining
        return ((timeLimit - remaining) / 1000).toInt()
    }

    /**
     * Berechnet Bonus-Punkte basierend auf Spielmodus
     *
     * @param scoreResponse Original-Score-Response
     * @param isAutoGuess Ob es ein automatischer Guess war
     * @return Zusätzliche Bonus-Punkte
     */
    private fun calculateBonusScore(scoreResponse: ScoreResponse, isAutoGuess: Boolean): Int {
        if (isAutoGuess) return 0 // Kein Bonus für Auto-Guess

        return when (_gameState.value.gameMode) {
            GameMode.BLITZ -> {
                // Zeit-Bonus im Blitz-Modus
                val timeBonus = (_uiState.value.timeRemaining / 1000) * 10
                timeBonus.toInt()
            }
            GameMode.ENDLESS -> {
                // Streak-Bonus im Endlos-Modus
                _gameState.value.streak * endlessConfig.streakBonus
            }
            GameMode.CLASSIC -> 0 // Kein Bonus im klassischen Modus
        }
    }

    /**
     * Aktualisiert Spielzustand nach verarbeitetem Guess
     *
     * @param scoreResponse Score-Response vom Server
     * @param finalScore Finaler Score inkl. Boni
     * @param isAutoGuess Ob es ein automatischer Guess war
     */
    private fun updateGameStateAfterGuess(scoreResponse: ScoreResponse, finalScore: Int, isAutoGuess: Boolean) {
        val currentState = _gameState.value
        val updatedCompletedRounds = currentState.completedRounds + listOf(
            currentState.currentRound!!.copy(
                score = finalScore,
                distanceMeters = scoreResponse.distanceMeters,
                isCompleted = true,
                guess = GuessLocation(
                    lat = scoreResponse.actualLocation.lat,
                    lng = scoreResponse.actualLocation.lng,
                    timeSpentSeconds = calculateTimeSpent()
                )
            )
        )

        // Aktualisiere Streak für Endlos-Modus
        val newStreak = if (currentState.gameMode == GameMode.ENDLESS) {
            if (finalScore >= 2000) currentState.streak + 1 else 0
        } else currentState.streak

        val newBestStreak = maxOf(currentState.bestStreak, newStreak)

        _gameState.value = currentState.copy(
            currentRound = null,
            completedRounds = updatedCompletedRounds,
            totalScore = currentState.totalScore + finalScore,
            isRoundActive = false,
            currentRoundNumber = currentState.currentRoundNumber,
            streak = newStreak,
            bestStreak = newBestStreak
        )
    }

    /**
     * Geht zur nächsten Runde oder beendet das Spiel
     *
     * Prüft ob weitere Runden verfügbar sind und startet diese,
     * oder beendet das Spiel mit Statistiken.
     */
    fun nextRound() {
        viewModelScope.launch {
            val currentState = _gameState.value

            // Prüfe Spielende-Bedingungen
            val shouldEndGame = when (currentState.gameMode) {
                GameMode.CLASSIC, GameMode.BLITZ ->
                    currentState.currentRoundNumber >= currentState.maxRounds
                GameMode.ENDLESS ->
                    currentState.streak == 0 && currentState.currentRoundNumber > 1 // Streak unterbrochen
            }

            if (shouldEndGame) {
                endGame()
            } else {
                // Nächste Runde
                _gameState.value = currentState.copy(
                    currentRoundNumber = currentState.currentRoundNumber + 1
                )
                _uiState.value = _uiState.value.copy(showResults = false)
                startNewRound()
            }
        }
    }

    /**
     * Beendet das aktuelle Spiel und zeigt finale Statistiken
     *
     * Berechnet finale Statistiken und setzt UI-Zustand für Spielende.
     */
    private suspend fun endGame() {
        timerJob?.cancel()

        val finalStats = calculateFinalStats()

        _uiState.value = _uiState.value.copy(
            gameComplete = true,
            finalStats = finalStats,
            showResults = false
        )

        println("GameViewModel: 🏁 Spiel beendet - Finale Statistiken: $finalStats")
    }

    /**
     * Berechnet finale Spielstatistiken
     *
     * @return GameStats-Objekt mit allen relevanten Statistiken
     */
    private fun calculateFinalStats(): GameStats {
        val state = _gameState.value
        val completedRounds = state.completedRounds

        return GameStats(
            totalRounds = completedRounds.size,
            totalScore = state.totalScore,
            averageScore = if (completedRounds.isNotEmpty())
                state.totalScore.toDouble() / completedRounds.size else 0.0,
            bestRoundScore = completedRounds.maxOfOrNull { it.score } ?: 0,
            worstRoundScore = completedRounds.minOfOrNull { it.score } ?: 0,
            totalTime = System.currentTimeMillis() - state.gameStartTime,
            averageTimePerRound = if (completedRounds.isNotEmpty())
                (System.currentTimeMillis() - state.gameStartTime) / completedRounds.size else 0L,
            perfectRounds = completedRounds.count { it.score >= 4500 },
            streak = state.streak,
            bestStreak = state.bestStreak
        )
    }

    /**
     * Zeigt die Karte für Benutzer-Guess an
     */
    fun showGuessMap() {
        _uiState.value = _uiState.value.copy(showGuessMap = true)
    }

    /**
     * Versteckt die Guess-Karte
     */
    fun hideGuessMap() {
        _uiState.value = _uiState.value.copy(showGuessMap = false)
    }

    /**
     * Wird aufgerufen wenn Street View bereit ist
     */
    fun onStreetViewReady() {
        _uiState.value = _uiState.value.copy(streetViewReady = true)
    }

    /**
     * Löscht aktuelle Fehlermeldung
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Beendet Endlos-Spiel manuell
     *
     * Nur im Endlos-Modus verfügbar - beendet das Spiel auch bei aktiver Streak.
     */
    fun endEndlessGame() {
        if (_gameState.value.gameMode == GameMode.ENDLESS) {
            viewModelScope.launch {
                endGame()
            }
        }
    }

    /**
     * Cleanup beim Zerstören des ViewModels
     */
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
