package com.example.geogeusserclone.utils

import androidx.compose.ui.graphics.Color
import com.example.geogeusserclone.utils.Constants

enum class ScoreRating {
    PERFECT, EXCELLENT, GOOD, FAIR, POOR, TERRIBLE
}

object ScoreCalculator {

    /**
     * Berechnet den Score basierend auf Entfernung und Zeit
     */
    fun calculateScore(
        distanceKm: Double,
        timeSpentMs: Long = 0L,
        maxTimeMs: Long = Constants.MAX_ROUND_TIME_MS
    ): Int {
        // Basis-Score basierend auf Entfernung (0-5000 Punkte)
        val distanceScore = when {
            distanceKm <= Constants.PERFECT_DISTANCE_KM -> 5000
            distanceKm <= Constants.EXCELLENT_DISTANCE_KM -> 4500 - ((distanceKm - 1) * 50).toInt()
            distanceKm <= Constants.GOOD_DISTANCE_KM -> 4000 - ((distanceKm - 10) * 75).toInt()
            distanceKm <= Constants.FAIR_DISTANCE_KM -> 2000 - ((distanceKm - 50) * 10).toInt()
            distanceKm <= Constants.POOR_DISTANCE_KM -> 500 - ((distanceKm - 200) * 0.5).toInt()
            else -> 0
        }.coerceIn(0, 5000)

        // Zeit-Bonus (0-500 Punkte)
        val timeBonus = if (timeSpentMs > 0 && maxTimeMs > 0) {
            val timeRatio = (maxTimeMs - timeSpentMs).toDouble() / maxTimeMs
            (timeRatio * Constants.TIME_BONUS_MAX).toInt().coerceIn(0, Constants.TIME_BONUS_MAX)
        } else 0

        return (distanceScore + timeBonus).coerceIn(0, 5500)
    }

    /**
     * Bestimmt die Score-Bewertung
     */
    fun getScoreRating(score: Int): ScoreRating {
        return when {
            score >= 4500 -> ScoreRating.PERFECT
            score >= 3500 -> ScoreRating.EXCELLENT
            score >= 2500 -> ScoreRating.GOOD
            score >= 1000 -> ScoreRating.FAIR
            score >= 200 -> ScoreRating.POOR
            else -> ScoreRating.TERRIBLE
        }
    }

    /**
     * Gibt die passende Farbe für den Score zurück
     */
    fun getScoreColor(rating: ScoreRating): Color {
        return when (rating) {
            ScoreRating.PERFECT -> Color(0xFF4CAF50)      // Grün
            ScoreRating.EXCELLENT -> Color(0xFF8BC34A)    // Hell-Grün
            ScoreRating.GOOD -> Color(0xFFFFC107)         // Gelb
            ScoreRating.FAIR -> Color(0xFFFF9800)         // Orange
            ScoreRating.POOR -> Color(0xFFFF5722)         // Rot-Orange
            ScoreRating.TERRIBLE -> Color(0xFFF44336)     // Rot
        }
    }
}
