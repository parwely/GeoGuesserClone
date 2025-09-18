// app/src/main/java/com/example/geogeusserclone/data/models/GameUiState.kt
package com.example.geogeusserclone.data.models

import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity

// Game State classes for ViewModels
data class GameState(
    val currentRound: GameRound? = null,
    val completedRounds: List<GameRound> = emptyList(),
    val totalScore: Int = 0,
    val isRoundActive: Boolean = false,
    val gameSession: GameSession? = null
)

data class GameUiState(
    val isLoading: Boolean = false,
    val isSubmittingGuess: Boolean = false,
    val streetViewReady: Boolean = false,
    val streetViewAvailable: Boolean = true,
    val showGuessMap: Boolean = false,
    val showResults: Boolean = false,
    val error: String? = null,
    val lastScoreResponse: ScoreResponse? = null
)
