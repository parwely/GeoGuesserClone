package com.example.geogeusserclone.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.*

/**
 * Utility-Klasse für Score-Berechnungen und -Bewertungen
 */
object ScoreCalculator {

    /**
     * Berechnet den Score basierend auf Distanz und Zeit
     */
    fun calculateScore(distance: Double, timeSpent: Long): Int {
        val baseScore = calculateDistanceScore(distance)
        val timeBonus = calculateTimeBonus(timeSpent)
        return (baseScore + timeBonus).coerceAtLeast(0)
    }

    /**
     * Berechnet Score basierend auf Distanz
     */
    private fun calculateDistanceScore(distance: Double): Int {
        return when {
            distance <= 1.0 -> 5000      // Perfekt
            distance <= 10.0 -> 4000     // Ausgezeichnet
            distance <= 50.0 -> 3000     // Gut
            distance <= 200.0 -> 2000    // Okay
            distance <= 1000.0 -> 1000   // Schlecht
            else -> 0                    // Sehr schlecht
        }
    }

    /**
     * Berechnet Zeit-Bonus
     */
    private fun calculateTimeBonus(timeSpent: Long): Int {
        val seconds = timeSpent / 1000.0
        val maxTime = 120.0 // 2 Minuten

        return if (seconds <= maxTime) {
            val timeRatio = (maxTime - seconds) / maxTime
            (timeRatio * 500).toInt() // Max 500 Punkte Zeitbonus
        } else {
            0
        }
    }

    /**
     * Score-Bewertung enum
     */
    enum class ScoreRating {
        PERFECT, EXCELLENT, GOOD, FAIR, POOR, TERRIBLE
    }

    /**
     * Gibt Score-Rating zurück
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
     * Gibt Farbe für Score zurück
     */
    fun getScoreColor(score: Int): Color {
        return when (getScoreRating(score)) {
            ScoreRating.PERFECT -> Color(0xFF4CAF50)      // Grün
            ScoreRating.EXCELLENT -> Color(0xFF8BC34A)    // Hellgrün
            ScoreRating.GOOD -> Color(0xFFFFEB3B)         // Gelb
            ScoreRating.FAIR -> Color(0xFFFF9800)         // Orange
            ScoreRating.POOR -> Color(0xFFFF5722)         // Rot
            ScoreRating.TERRIBLE -> Color(0xFF9E9E9E)     // Grau
        }
    }

    /**
     * Berechnet Ranking basierend auf Score
     */
    fun calculateRanking(score: Int, totalPlayers: Int = 100): Int {
        val percentile = when {
            score >= 4500 -> 0.95   // Top 5%
            score >= 3500 -> 0.80   // Top 20%
            score >= 2500 -> 0.60   // Top 40%
            score >= 1500 -> 0.40   // Top 60%
            score >= 500 -> 0.20    // Top 80%
            else -> 0.05            // Bottom 95%
        }

        return ((1.0 - percentile) * totalPlayers).toInt().coerceAtLeast(1)
    }

    /**
     * Formatiert Score für Anzeige
     */
    fun formatScore(score: Int): String {
        return when {
            score >= 1000 -> "${score / 1000}.${(score % 1000) / 100}k"
            else -> score.toString()
        }
    }
}
