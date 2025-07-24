package com.example.geogeusserclone.data.models

data class GameState(
    val currentRound: Int = 1,
    val totalRounds: Int = 5,
    val timeRemaining: Long = 60000L, // 60 Sekunden
    val score: Int = 0,
    val isGameOver: Boolean = false
)