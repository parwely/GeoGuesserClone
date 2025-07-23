package com.example.geogeusserclone.utils

import androidx.compose.ui.graphics.Color

object ScoreCalculator {

    fun calculateScore(
        distanceKm: Double,
        timeSpentMs: Long = 0L,
        maxTimeMs: Long = Constants.MAX_ROUND_TIME_MS
    ): Int {
        val baseScore = calculateDistanceScore(distanceKm)
        val timeBonus = calculateTimeBonus(timeSpentMs, maxTimeMs)

        return (baseScore + timeBonus).coerceAtMost(Constants.MAX_SCORE_PER_ROUND)
    }

    private fun calculateDistanceScore(distanceKm: Double): Int {
        return when {
            distanceKm <= Constants.PERFECT_DISTANCE_KM -> 5000
            distanceKm <= Constants.EXCELLENT_DISTANCE_KM -> {
                5000 - ((distanceKm - Constants.PERFECT_DISTANCE_KM) * 400).toInt()
            }
            distanceKm <= Constants.GOOD_DISTANCE_KM -> {
                3500 - ((distanceKm - Constants.EXCELLENT_DISTANCE_KM) * 25).toInt()
            }
            distanceKm <= Constants.FAIR_DISTANCE_KM -> {
                2500 - ((distanceKm - Constants.GOOD_DISTANCE_KM) * 20).toInt()
            }
            distanceKm <= Constants.POOR_DISTANCE_KM -> {
                1500 - ((distanceKm - Constants.FAIR_DISTANCE_KM) * 2.5).toInt()
            }
            else -> 200
        }.coerceAtLeast(0)
    }

    private fun calculateTimeBonus(timeSpentMs: Long, maxTimeMs: Long): Int {
        if (timeSpentMs <= 0 || maxTimeMs <= 0) return 0
        val timeRatio = timeSpentMs.toDouble() / maxTimeMs.toDouble()
        val bonusRatio = (1.0 - timeRatio).coerceAtLeast(0.0)
        return (bonusRatio * 500).toInt()
    }

    fun getScoreRating(score: Int): ScoreRating {
        return when {
            score >= 4500 -> ScoreRating.PERFECT
            score >= 3500 -> ScoreRating.EXCELLENT
            score >= 2500 -> ScoreRating.GOOD
            score >= 1500 -> ScoreRating.FAIR
            score >= 500 -> ScoreRating.POOR
            else -> ScoreRating.TERRIBLE
        }
    }

    fun getScoreColor(rating: ScoreRating): Color {
        return when (rating) {
            ScoreRating.PERFECT -> Color(0xFF4CAF50)
            ScoreRating.EXCELLENT -> Color(0xFF8BC34A)
            ScoreRating.GOOD -> Color(0xFFFFEB3B)
            ScoreRating.FAIR -> Color(0xFFFF9800)
            ScoreRating.POOR -> Color(0xFFFF5722)
            ScoreRating.TERRIBLE -> Color(0xFFF44336)
        }
    }
}

enum class ScoreRating {
    PERFECT, EXCELLENT, GOOD, FAIR, POOR, TERRIBLE
}