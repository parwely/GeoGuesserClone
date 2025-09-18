// app/src/main/java/com/example/geogeusserclone/data/models/GameUiState.kt
package com.example.geogeusserclone.data.models

import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity

// Game Mode Enum
enum class GameMode {
    CLASSIC,    // Klassisches 5-Runden Spiel
    BLITZ,      // Zeitdruck, keine Bewegung, schnelle Runden
    ENDLESS     // Endlos-Modus, spiele so lange du willst
}

// Game State classes for ViewModels
data class GameState(
    val currentRound: GameRound? = null,
    val completedRounds: List<GameRound> = emptyList(),
    val totalScore: Int = 0,
    val isRoundActive: Boolean = false,
    val gameSession: GameSession? = null,
    // NEUE FEATURES für erweiterte Modi
    val gameMode: GameMode = GameMode.CLASSIC,
    val roundTimeLimit: Long? = null, // Zeitlimit in Millisekunden (für Blitz-Modus)
    val isTimeLimited: Boolean = false,
    val maxRounds: Int = 5, // Für Classic und Blitz, -1 für Endless
    val currentRoundNumber: Int = 1,
    val isMovementDisabled: Boolean = false, // Für Blitz-Modus
    val gameStartTime: Long = System.currentTimeMillis(),
    val streak: Int = 0, // Für Endlos-Modus
    val bestStreak: Int = 0 // Für Endlos-Modus
)

data class GameUiState(
    val isLoading: Boolean = false,
    val isSubmittingGuess: Boolean = false,
    val streetViewReady: Boolean = false,
    val streetViewAvailable: Boolean = true,
    val showGuessMap: Boolean = false,
    val showResults: Boolean = false,
    val error: String? = null,
    val lastScoreResponse: ScoreResponse? = null,
    // NEUE UI-Features für erweiterte Modi
    val timeRemaining: Long = 0L, // Verbleibende Zeit in Millisekunden
    val isTimeRunningOut: Boolean = false, // Warnung bei wenig Zeit
    val showGameModeInfo: Boolean = false,
    val showEndlessStats: Boolean = false,
    val gameComplete: Boolean = false,
    val finalStats: GameStats? = null
)

// KORRIGIERT: Vollständige GameStats-Klasse mit allen benötigten Eigenschaften
data class GameStats(
    val totalRounds: Int = 0, // HINZUGEFÜGT
    val totalScore: Int = 0,
    val averageScore: Double = 0.0,
    val bestRoundScore: Int = 0, // HINZUGEFÜGT
    val worstRoundScore: Int = 0, // HINZUGEFÜGT
    val totalTime: Long = 0L, // HINZUGEFÜGT
    val averageTimePerRound: Long = 0L,
    val perfectRounds: Int = 0, // HINZUGEFÜGT - Runden mit maximaler Punktzahl
    val streak: Int = 0, // HINZUGEFÜGT
    val bestStreak: Int = 0 // HINZUGEFÜGT
)

// Blitz-Modus Konfiguration
data class BlitzModeConfig(
    val roundTimeLimit: Long = 30000L, // 30 Sekunden pro Runde
    val totalRounds: Int = 10, // 10 schnelle Runden
    val movementDisabled: Boolean = true, // Keine Street View Navigation
    val quickGuessBonus: Int = 500, // Bonus für schnelle richtige Antworten
    val timeMultiplier: Double = 1.5 // Score-Multiplikator basierend auf verbleibender Zeit
)

// Endlos-Modus Konfiguration
data class EndlessModeConfig(
    val streakBonus: Int = 100, // Bonus pro aufeinanderfolgender guter Runde
    val difficultyIncrease: Boolean = true, // Schwierigkeit steigt mit Streak
    val noTimeLimit: Boolean = true,
    val saveFrequency: Int = 5 // Speichere Progress alle 5 Runden
)

// Erweiterte Round-Daten für neue Modi
data class ExtendedGameRound(
    val roundId: String,
    val location: LocationData,
    val guess: GuessLocation? = null,
    val score: Int = 0,
    val distanceMeters: Double = 0.0,
    val isCompleted: Boolean = false,
    // NEUE FEATURES
    val roundNumber: Int = 1,
    val timeLimit: Long? = null, // Zeitlimit für diese Runde
    val timeSpent: Long = 0L, // Tatsächlich verbrachte Zeit
    val movementDisabled: Boolean = false,
    val difficulty: Int = 1, // Dynamische Schwierigkeit im Endlos-Modus
    val streakAtTime: Int = 0, // Streak-Status bei dieser Runde
    val bonusScore: Int = 0 // Zusätzliche Punkte (Zeit-Bonus, Streak-Bonus etc.)
)
