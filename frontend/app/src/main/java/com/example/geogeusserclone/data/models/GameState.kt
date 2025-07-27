package com.example.geogeusserclone.data.models

import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import org.osmdroid.util.GeoPoint

data class GameState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentGame: GameEntity? = null,
    val currentLocation: LocationEntity? = null,
    val currentGuesses: List<GuessEntity> = emptyList(),
    val lastGuessResult: GuessEntity? = null,
    val showingResults: Boolean = false,
    val gameCompleted: Boolean = false,
    val isMapVisible: Boolean = false,
    val isGuessSubmitted: Boolean = false,
    val timeRemaining: Long = 0L,
    val comparisonGuessPoint: GeoPoint? = null,
    val comparisonActualPoint: GeoPoint? = null,
    val showLocationReveal: Boolean = false,
    val revealActualLocation: LocationEntity? = null,
    val revealGuessResult: GuessEntity? = null
)