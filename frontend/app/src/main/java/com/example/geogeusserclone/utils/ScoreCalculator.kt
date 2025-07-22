package com.example.geogeusserclone.utils

object ScoreCalculator {

    fun calculate(distanceKm: Double, timeSpentMs: Long = 0L, maxTimeMs: Long = 60000L): Int {
        val baseScore = calculateDistanceScore(distanceKm)
        val timeBonus = calculateTimeBonus(timeSpentMs, maxTimeMs)
        return (baseScore + timeBonus).coerceAtLeast(0)
    }

    private fun calculateDistanceScore(distanceKm: Double): Int {
        return when {
            distanceKm <= 1 -> 5000
            distanceKm <= 10 -> (5000 - (distanceKm - 1) * 400).toInt()
            distanceKm <= 100 -> (1400 - (distanceKm - 10) * 10).toInt()
            distanceKm <= 1000 -> (500 - (distanceKm - 100) * 0.5).toInt()
            else -> 0
        }.coerceAtLeast(0)
    }

    private fun calculateTimeBonus(timeSpentMs: Long, maxTimeMs: Long): Int {
        if (timeSpentMs <= 0 || timeSpentMs >= maxTimeMs) return 0

        val timeRatio = 1.0 - (timeSpentMs.toDouble() / maxTimeMs.toDouble())
        return (timeRatio * 1000).toInt()
    }

    fun getScoreCategory(score: Int): String {
        return when {
            score >= 4500 -> "Perfect!"
            score >= 3500 -> "Excellent"
            score >= 2500 -> "Great"
            score >= 1500 -> "Good"
            score >= 500 -> "Not bad"
            else -> "Better luck next time"
        }
    }
}