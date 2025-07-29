package com.example.geogeusserclone.data.models

import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity

data class GameState(
    val currentGame: GameEntity? = null,
    val currentLocation: LocationEntity? = null,
    val currentGuess: GuessEntity? = null,
    val revealGuessResult: GuessEntity? = null,
    val isLoading: Boolean = false,
    val showMap: Boolean = false,
    val showRoundResult: Boolean = false,
    val showGameCompletion: Boolean = false,
    val timeRemaining: Long = 0L,
    val error: String? = null
)
