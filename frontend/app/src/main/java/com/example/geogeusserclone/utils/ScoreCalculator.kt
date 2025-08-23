package com.example.geogeusserclone.utils

import androidx.compose.ui.graphics.Color
import com.example.geogeusserclone.utils.Constants

/**
 * Utility-Klasse für Score-Berechnungen basierend auf Distanz und Zeit
 */
object ScoreCalculator {

    enum class ScoreRating {
        PERFECT, EXCELLENT, GOOD, FAIR, POOR, TERRIBLE
    }

    /**
     * Berechnet den Score basierend auf Distanz und Zeit
     * @param distance Distanz in Kilometern
     * @param timeSpent Zeit in Millisekunden
     * @return Score zwischen 0 und 5000
     */
    fun calculateScore(distance: Double, timeSpent: Long): Int {
        val baseScore = calculateDistanceScore(distance)
        val timeBonus = calculateTimeBonus(timeSpent)

        return (baseScore + timeBonus).coerceIn(0, 5000)
    }

    /**
     * Berechnet das Score-Rating basierend auf dem Score
     */
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

    /**
     * Gibt die Farbe für einen Score zurück
     */
    fun getScoreColor(score: Int): Color {
        return when (getScoreRating(score)) {
            ScoreRating.PERFECT -> Color(0xFF4CAF50) // Grün
            ScoreRating.EXCELLENT -> Color(0xFF8BC34A) // Hellgrün
            ScoreRating.GOOD -> Color(0xFFFFEB3B) // Gelb
            ScoreRating.FAIR -> Color(0xFFFF9800) // Orange
            ScoreRating.POOR -> Color(0xFFFF5722) // Rot-Orange
            ScoreRating.TERRIBLE -> Color(0xFFF44336) // Rot
        }
    }

    /**
     * Berechnet den Rang basierend auf dem Score
     */
    fun getScoreRank(score: Int): String {
        return when {
            score >= 4500 -> "🏆 Weltklasse"
            score >= 3500 -> "🥇 Experte"
            score >= 2500 -> "🥈 Fortgeschritten"
            score >= 1500 -> "🥉 Gut"
            score >= 500 -> "📍 Okay"
            else -> "🎯 Übung macht den Meister"
        }
    }

    /**
     * Berechnet den Gesamtscore für ein komplettes Spiel
     */
    fun calculateGameScore(scores: List<Int>): GameScoreResult {
        val totalScore = scores.sum()
        val averageScore = if (scores.isNotEmpty()) totalScore / scores.size else 0
        val maxPossibleScore = scores.size * 5000
        val accuracyPercentage = if (maxPossibleScore > 0) {
            (totalScore.toDouble() / maxPossibleScore * 100).toInt()
        } else 0

        return GameScoreResult(
            totalScore = totalScore,
            averageScore = averageScore,
            accuracyPercentage = accuracyPercentage,
            rank = getScoreRank(averageScore)
        )
    }

    /**
     * Berechnet den Basis-Score basierend auf der Distanz
     */
    private fun calculateDistanceScore(distance: Double): Int {
        return when {
            distance <= Constants.PERFECT_DISTANCE_KM -> 5000
            distance <= Constants.EXCELLENT_DISTANCE_KM -> {
                // Linear von 5000 auf 4000 zwischen 1km und 10km
                val ratio = (distance - Constants.PERFECT_DISTANCE_KM) /
                           (Constants.EXCELLENT_DISTANCE_KM - Constants.PERFECT_DISTANCE_KM)
                (5000 - (ratio * 1000)).toInt()
            }
            distance <= Constants.GOOD_DISTANCE_KM -> {
                // Linear von 4000 auf 2000 zwischen 10km und 50km
                val ratio = (distance - Constants.EXCELLENT_DISTANCE_KM) /
                           (Constants.GOOD_DISTANCE_KM - Constants.EXCELLENT_DISTANCE_KM)
                (4000 - (ratio * 2000)).toInt()
            }
            distance <= Constants.FAIR_DISTANCE_KM -> {
                // Linear von 2000 auf 1000 zwischen 50km und 200km
                val ratio = (distance - Constants.GOOD_DISTANCE_KM) /
                           (Constants.FAIR_DISTANCE_KM - Constants.GOOD_DISTANCE_KM)
                (2000 - (ratio * 1000)).toInt()
            }
            distance <= Constants.POOR_DISTANCE_KM -> {
                // Linear von 1000 auf 100 zwischen 200km und 1000km
                val ratio = (distance - Constants.FAIR_DISTANCE_KM) /
                           (Constants.POOR_DISTANCE_KM - Constants.FAIR_DISTANCE_KM)
                (1000 - (ratio * 900)).toInt()
            }
            else -> {
                // Über 1000km: sehr niedrige Punkte
                maxOf(10, (100 - (distance / 100)).toInt())
            }
        }
    }

    /**
     * Berechnet Zeit-Bonus
     */
    private fun calculateTimeBonus(timeSpent: Long): Int {
        val timeSpentSeconds = timeSpent / 1000
        val maxTimeSeconds = Constants.MAX_ROUND_TIME_MS / 1000

        return if (timeSpentSeconds < maxTimeSeconds) {
            val timeRatio = 1.0 - (timeSpentSeconds.toDouble() / maxTimeSeconds)
            (timeRatio * Constants.TIME_BONUS_MAX).toInt()
        } else {
            0
        }
    }
}

data class GameScoreResult(
    val totalScore: Int,
    val averageScore: Int,
    val accuracyPercentage: Int,
    val rank: String
)
