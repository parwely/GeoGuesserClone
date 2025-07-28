package com.example.geogeusserclone.data.models

data class GameStats(
    val totalGames: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Double = 0.0,
    val bestScore: Int = 0,
    val perfectGuesses: Int = 0,
    val averageDistance: Double = 0.0,
    val bestDistance: Double = Double.MAX_VALUE,
    val totalTimeSpent: Long = 0L,
    val averageTimePerRound: Long = 0L,
    val gamesWon: Int = 0,
    val winRate: Double = 0.0,
    val favoriteCountry: String? = null,
    val monthlyStats: List<MonthlyGameStats> = emptyList()
)

data class MonthlyGameStats(
    val month: String,
    val year: Int,
    val gamesPlayed: Int,
    val totalScore: Int,
    val averageScore: Double
)

data class UserStats(
    val userId: String,
    val username: String,
    val totalGames: Int,
    val totalScore: Int,
    val averageScore: Double,
    val bestScore: Int,
    val rank: Int = 0,
    val lastActive: Long = 0L
)
