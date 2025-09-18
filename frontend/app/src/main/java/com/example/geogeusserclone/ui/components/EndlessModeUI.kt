/**
 * EndlessModeUI.kt
 *
 * Diese Datei enthält die spezielle UI-Komponenten für den Endlos-Spielmodus.
 * Sie implementiert ein Streak-basiertes Interface mit unbegrenzten Runden
 * und motivierenden Elementen für längere Spielsessions.
 *
 * Architektur-Integration:
 * - Game Mode UI: Spezialisierte UI für Endlos-Modus Features
 * - Streak System: Visuelles Feedback für aufeinanderfolgende Erfolge
 * - Session Management: Tools zum manuellen Beenden langer Sessions
 * - Motivation Elements: Gamification für längere Spielzeiten
 * - Statistics Display: Live-Anzeige von Session-Fortschritt
 */
package com.example.geogeusserclone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.models.GameUiState

/**
 * Haupt-UI-Komponente für den Endlos-Modus
 *
 * Stellt eine motivierende, streak-fokussierte Benutzeroberfläche bereit.
 * Features:
 * - Prominente Streak-Anzeige mit visuellen Belohnungen
 * - Session-Management mit Beenden-Option
 * - Live-Statistiken für Motivation
 * - Unbegrenzte Runden-Unterstützung
 * - Adaptive Schwierigkeit basierend auf Streak
 *
 * @param gameState Aktueller Spielzustand
 * @param uiState Aktueller UI-Zustand
 * @param onShowMap Callback zum Anzeigen der Guess-Karte
 * @param onBack Callback für Rückkehr zum Menü
 * @param onEndGame Callback zum manuellen Beenden der Session
 * @param modifier Modifier für Layout-Anpassungen
 */
@Composable
fun EndlessModeUI(
    gameState: GameState,
    uiState: GameUiState,
    onShowMap: () -> Unit,
    onBack: () -> Unit,
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Kompakte Top-Bar mit Session-Management
        EndlessTopBar(
            gameState = gameState,
            onBack = onBack,
            onEndGame = onEndGame
        )

        Spacer(modifier = Modifier.weight(1f))

        // Hauptfokus: Streak-Anzeige als Motivationselement
        CompactStreakDisplay(
            currentStreak = gameState.streak,
            bestStreak = gameState.bestStreak,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action-Bereich nur wenn Street View bereit ist
        if (uiState.streetViewReady && !uiState.showGuessMap && !uiState.showResults) {
            CompactEndlessActionArea(
                onShowMap = onShowMap,
                streak = gameState.streak
            )
        }

        // Session-Statistiken am unteren Rand
        CompactEndlessStats(
            roundNumber = gameState.currentRoundNumber,
            totalScore = gameState.totalScore,
            streak = gameState.streak
        )
    }
}

/**
 * Top-Bar für Endlos-Modus mit Session-Management
 *
 * Zeigt Exit-Button, Endlos-Indikator und Beenden-Option.
 * Ermöglicht kontrollierten Ausstieg aus langen Sessions.
 *
 * @param gameState Aktueller Spielzustand
 * @param onBack Callback für sofortigen Exit
 * @param onEndGame Callback für kontrolliertes Session-Ende
 */
@Composable
private fun EndlessTopBar(
    gameState: GameState,
    onBack: () -> Unit,
    onEndGame: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exit-Button für sofortigen Ausstieg
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exit")
        }

        // Endlos-Indikator und Session-Beenden
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Endlos-Symbol mit aktueller Rundennummer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "∞",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Runde ${gameState.currentRoundNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Session beenden Button für kontrollierten Ausstieg
            OutlinedButton(
                onClick = onEndGame,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Beenden", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Animierte Streak-Anzeige als Hauptmotivationselement
 *
 * Zeigt aktuelle und beste Streak mit visuellen Belohnungen.
 * Verwendet Farb-Animationen und Emoji-Feedback für Gamification.
 *
 * @param currentStreak Aktuelle Anzahl aufeinanderfolgender Erfolge
 * @param bestStreak Beste jemals erreichte Streak
 * @param modifier Layout-Modifier
 */
@Composable
private fun CompactStreakDisplay(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    // Animierte Farbe basierend auf Streak-Level
    val streakColor by animateColorAsState(
        targetValue = when {
            currentStreak >= 10 -> Color(0xFF4CAF50) // Grün für hohe Streaks
            currentStreak >= 5 -> Color(0xFFFF9800) // Orange für mittlere Streaks
            currentStreak >= 1 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "streak_color"
    )

    // Subtle Skalierung für aktive Streaks
    val scale by animateFloatAsState(
        targetValue = if (currentStreak > 0) 1.05f else 1f,
        label = "streak_scale"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = streakColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Streak-Icon mit motivierendem Feedback
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(streakColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentStreak > 0) "🔥" else "💫",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Aktuelle Streak-Anzeige
            Text(
                text = "Aktuelle Serie",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currentStreak",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = streakColor
            )

            // Beste Streak als Referenz
            if (bestStreak > currentStreak) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Beste Serie: $bestStreak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Streak-Bonus Information
            if (currentStreak > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+${currentStreak * 100} Streak-Bonus",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = streakColor
                )
            }
        }
    }
}

/**
 * Kompakter Action-Bereich für Endlos-Modus
 *
 * Motivierende Call-to-Action mit Streak-spezifischen Nachrichten.
 * Passt sich an den aktuellen Streak-Status an.
 *
 * @param onShowMap Callback zum Öffnen der Guess-Karte
 * @param streak Aktuelle Streak für motivierende Nachrichten
 */
@Composable
private fun CompactEndlessActionArea(
    onShowMap: () -> Unit,
    streak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modus-Header mit Endlos-Symbol
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "∞",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENDLOS-MODUS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Streak-spezifische motivierende Nachricht
            Text(
                text = if (streak > 0)
                    "Halte deine Serie am Leben! 🔥"
                else
                    "Beginne eine neue Serie!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Haupt-Action-Button
            Button(
                onClick = onShowMap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "POSITION RATEN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Extra Motivation bei hohen Streaks
            if (streak >= 5) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🚀 Fantastische Serie! Weiter so!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Kompakte Session-Statistiken für Endlos-Modus
 *
 * Zeigt wichtige Session-Metriken in übersichtlicher Form.
 * Hilft Spielern ihren Fortschritt zu verfolgen.
 *
 * @param roundNumber Aktuelle Rundennummer
 * @param totalScore Gesamtscore der Session
 * @param streak Aktuelle Streak
 */
@Composable
private fun CompactEndlessStats(
    roundNumber: Int,
    totalScore: Int,
    streak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header mit Info-Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📊 Session-Statistiken",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dreispaltige Statistik-Anzeige
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Runden",
                    value = "$roundNumber",
                    icon = "🎯"
                )

                StatItem(
                    label = "Punkte",
                    value = "$totalScore",
                    icon = "🏆"
                )

                StatItem(
                    label = "Serie",
                    value = "$streak",
                    icon = "���"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hilfreicher Tipp für Streak-Management
            Text(
                text = "💡 Tipp: Erreiche >2000 Punkte pro Runde um deine Serie fortzusetzen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Einzelne Statistik-Anzeige mit Icon und Wert
 *
 * Wiederverwendbare Komponente für die Darstellung von Session-Metriken.
 *
 * @param label Beschreibung der Statistik
 * @param value Anzuzeigender Wert
 * @param icon Emoji-Icon für visuelle Identifikation
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon als visueller Anker
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )

        // Wert prominent hervorgehoben
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Label für Kontext
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
