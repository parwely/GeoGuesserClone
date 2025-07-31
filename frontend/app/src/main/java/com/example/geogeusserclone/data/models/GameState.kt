package com.example.geogeusserclone.data.models

data class GameState(
    val currentGame: com.example.geogeusserclone.data.database.entities.GameEntity? = null,
    val currentLocation: com.example.geogeusserclone.data.database.entities.LocationEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showMap: Boolean = false,
    val showRoundResult: Boolean = false,
    val showGameCompletion: Boolean = false,
    val revealGuessResult: com.example.geogeusserclone.data.database.entities.GuessEntity? = null,
    val timeRemaining: Long = 120000L, // 2 Minuten default
    val currentRound: Int = 1,
    val totalRounds: Int = 5,
    val gameScore: Int = 0
)
