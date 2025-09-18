/**
 * ScoreCalculator.kt
 *
 * Diese Datei enthält Utility-Funktionen für die Score-Berechnung in der GeoGuess-App.
 * Sie implementiert verschiedene Bewertungsalgorithmen basierend auf Genauigkeit,
 * Zeit und Spielmodus-spezifischen Boni.
 *
 * Architektur-Integration:
 * - Game Logic: Zentrale Score-Berechnung für alle Spielmodi
 * - Algorithm Implementation: Mathematische Formeln für faire Bewertung
 * - Balance System: Ausgewogene Punkteverteilung für verschiedene Fähigkeitsstufen
 * - Performance Metrics: Grundlage für Statistiken und Leaderboards
 */
package com.example.geogeusserclone.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.*

/**
 * Utility-Klasse für Score-Berechnungen und -Bewertungen
 *
 * Diese Klasse implementiert die komplette Score-Logik der App:
 * - Distanz-basierte Grundpunkte
 * - Zeit-Boni für schnelle Antworten
 * - Spielmodus-spezifische Multiplikatoren
 * - Bewertungs-Kategorien und Farb-Kodierung
 * - Statistische Funktionen für Leistungsanalyse
 */
object ScoreCalculator {

    // ===== SCORE-KONSTANTEN =====

    /** Maximaler Score pro Runde */
    private const val MAX_SCORE = 5000

    /** Maximaler Zeit-Bonus */
    private const val MAX_TIME_BONUS = 500

    /** Maximale Zeit für Zeit-Bonus (2 Minuten) */
    private const val MAX_TIME_FOR_BONUS = 120.0

    /**
     * Berechnet den Gesamt-Score basierend auf Distanz und Zeit
     *
     * Kombiniert Distanz-Score mit Zeit-Bonus für eine ausgewogene Bewertung.
     * Berücksichtigt sowohl Genauigkeit als auch Geschwindigkeit.
     *
     * @param distance Distanz zwischen Guess und tatsächlicher Location in Kilometern
     * @param timeSpent Verbrachte Zeit in Millisekunden
     * @return Gesamt-Score zwischen 0 und 5500 Punkten
     */
    fun calculateScore(distance: Double, timeSpent: Long): Int {
        val baseScore = calculateDistanceScore(distance)
        val timeBonus = calculateTimeBonus(timeSpent)
        return (baseScore + timeBonus).coerceAtLeast(0)
    }

    /**
     * Berechnet Score basierend auf Distanz mit exponentieller Abnahme
     *
     * Verwendet eine optimierte Formel die sehr genaue Guesses stark belohnt
     * und eine faire Verteilung für verschiedene Genauigkeitsstufen bietet.
     *
     * @param distance Distanz in Kilometern
     * @return Basis-Score zwischen 0 und 5000 Punkten
     */
    private fun calculateDistanceScore(distance: Double): Int {
        return when {
            distance <= 0.025 -> MAX_SCORE              // Perfekt: 25 Meter oder weniger
            distance <= 1.0 -> (MAX_SCORE * 0.9).toInt()   // Ausgezeichnet: 1 km
            distance <= 10.0 -> (MAX_SCORE * 0.8).toInt()  // Sehr gut: 10 km
            distance <= 50.0 -> (MAX_SCORE * 0.6).toInt()  // Gut: 50 km
            distance <= 200.0 -> (MAX_SCORE * 0.4).toInt() // OK: 200 km
            distance <= 1000.0 -> (MAX_SCORE * 0.2).toInt() // Schlecht: 1000 km
            distance <= 2000.0 -> (MAX_SCORE * 0.1).toInt() // Sehr schlecht: 2000 km
            else -> 0                                    // Keine Punkte: > 2000 km
        }
    }

    /**
     * Berechnet Zeit-Bonus für schnelle Antworten
     *
     * Belohnt schnelle Entscheidungen mit zusätzlichen Punkten.
     * Der Bonus nimmt linear ab je länger man braucht.
     *
     * @param timeSpent Verbrachte Zeit in Millisekunden
     * @return Zeit-Bonus zwischen 0 und 500 Punkten
     */
    private fun calculateTimeBonus(timeSpent: Long): Int {
        val seconds = timeSpent / 1000.0

        return if (seconds <= MAX_TIME_FOR_BONUS) {
            val timeRatio = (MAX_TIME_FOR_BONUS - seconds) / MAX_TIME_FOR_BONUS
            (timeRatio * MAX_TIME_BONUS).toInt().coerceAtLeast(0)
        } else {
            0 // Kein Bonus nach 2 Minuten
        }
    }

    /**
     * Score-Bewertungs-Kategorien für UI-Feedback
     */
    enum class ScoreRating(val displayName: String, val color: Color, val emoji: String) {
        PERFECT("Perfekt!", Color(0xFF4CAF50), "🎯"),
        EXCELLENT("Ausgezeichnet!", Color(0xFF8BC34A), "🏆"),
        VERY_GOOD("Sehr gut!", Color(0xFFCDDC39), "⭐"),
        GOOD("Gut!", Color(0xFFFFEB3B), "👍"),
        OK("OK", Color(0xFFFF9800), "👌"),
        POOR("Schlecht", Color(0xFFFF5722), "😕"),
        VERY_POOR("Sehr schlecht", Color(0xFFF44336), "😞")
    }

    /**
     * Bestimmt die Bewertungskategorie basierend auf Score
     *
     * @param score Erreichter Score
     * @param maxScore Maximaler möglicher Score
     * @return ScoreRating-Enum mit passender Bewertung
     */
    fun getScoreRating(score: Int, maxScore: Int = MAX_SCORE): ScoreRating {
        val percentage = score.toDouble() / maxScore.toDouble()

        return when {
            percentage >= 0.95 -> ScoreRating.PERFECT
            percentage >= 0.85 -> ScoreRating.EXCELLENT
            percentage >= 0.70 -> ScoreRating.VERY_GOOD
            percentage >= 0.55 -> ScoreRating.GOOD
            percentage >= 0.35 -> ScoreRating.OK
            percentage >= 0.15 -> ScoreRating.POOR
            else -> ScoreRating.VERY_POOR
        }
    }

    /**
     * Berechnet Streak-Bonus für Endlos-Modus
     *
     * @param currentStreak Aktuelle Streak-Anzahl
     * @param baseScore Basis-Score der Runde
     * @return Zusätzliche Bonus-Punkte
     */
    fun calculateStreakBonus(currentStreak: Int, baseScore: Int): Int {
        return when {
            currentStreak <= 0 -> 0
            currentStreak <= 5 -> currentStreak * 50 // 50 Punkte pro Streak-Level
            currentStreak <= 10 -> currentStreak * 75 // 75 Punkte für höhere Streaks
            else -> currentStreak * 100 // 100 Punkte für sehr hohe Streaks
        }
    }

    /**
     * Berechnet Blitz-Modus Multiplikator basierend auf verbleibender Zeit
     *
     * @param timeRemaining Verbleibende Zeit in Millisekunden
     * @param totalTime Gesamt-Zeit der Runde in Millisekunden
     * @return Multiplikator zwischen 1.0 und 2.0
     */
    fun calculateBlitzMultiplier(timeRemaining: Long, totalTime: Long): Double {
        if (totalTime <= 0) return 1.0

        val timeRatio = timeRemaining.toDouble() / totalTime.toDouble()
        return (1.0 + timeRatio).coerceIn(1.0, 2.0)
    }

    /**
     * Berechnet Schwierigkeits-Bonus basierend auf Location-Difficulty
     *
     * @param difficulty Schwierigkeitsgrad der Location (1-5)
     * @param baseScore Basis-Score der Runde
     * @return Zusätzliche Difficulty-Bonus-Punkte
     */
    fun calculateDifficultyBonus(difficulty: Int, baseScore: Int): Int {
        val multiplier = when (difficulty) {
            1 -> 0.0    // Einfach: Kein Bonus
            2 -> 0.1    // Normal: 10% Bonus
            3 -> 0.25   // Schwer: 25% Bonus
            4 -> 0.5    // Sehr schwer: 50% Bonus
            5 -> 1.0    // Extrem: 100% Bonus
            else -> 0.0
        }

        return (baseScore * multiplier).toInt()
    }

    /**
     * Berechnet die relative Performance im Vergleich zu anderen Spielern
     *
     * @param playerScore Score des Spielers
     * @param averageScore Durchschnitts-Score aller Spieler für diese Location
     * @return Performance-Wert zwischen 0.0 (schlechter als alle) und 2.0 (doppelt so gut)
     */
    fun calculateRelativePerformance(playerScore: Int, averageScore: Double): Double {
        return if (averageScore > 0) {
            (playerScore / averageScore).coerceIn(0.0, 2.0)
        } else {
            1.0
        }
    }

    /**
     * Bestimmt Achievement-Status basierend auf Score und Kontext
     *
     * @param score Erreichter Score
     * @param distance Distanz in Kilometern
     * @param timeSpent Zeit in Millisekunden
     * @return Liste von erreichten Achievements
     */
    fun determineAchievements(score: Int, distance: Double, timeSpent: Long): List<String> {
        val achievements = mutableListOf<String>()

        // Distanz-basierte Achievements
        when {
            distance <= 0.001 -> achievements.add("🎯 Pinpoint Precision")
            distance <= 0.025 -> achievements.add("🏹 Bullseye")
            distance <= 1.0 -> achievements.add("🎪 Close Call")
        }

        // Zeit-basierte Achievements
        val seconds = timeSpent / 1000
        when {
            seconds <= 10 -> achievements.add("⚡ Lightning Fast")
            seconds <= 30 -> achievements.add("🚀 Speed Demon")
            seconds <= 60 -> achievements.add("⏰ Quick Thinker")
        }

        // Score-basierte Achievements
        when {
            score >= 5000 -> achievements.add("💎 Perfect Score")
            score >= 4500 -> achievements.add("🌟 Nearly Perfect")
            score >= 4000 -> achievements.add("🏆 Excellent")
        }

        return achievements
    }

    /**
     * Formatiert Score für Anzeige mit Tausender-Trennzeichen
     *
     * @param score Score-Wert
     * @return Formatierter String (z.B. "4,250")
     */
    fun formatScore(score: Int): String {
        return "%,d".format(score)
    }

    /**
     * Berechnet prozentuale Genauigkeit basierend auf maximal möglichem Score
     *
     * @param achievedScore Erreichter Score
     * @param maxPossibleScore Maximal möglicher Score
     * @return Prozentsatz zwischen 0.0 und 1.0
     */
    fun calculateAccuracyPercentage(achievedScore: Int, maxPossibleScore: Int): Double {
        return if (maxPossibleScore > 0) {
            (achievedScore.toDouble() / maxPossibleScore.toDouble()).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }
}
