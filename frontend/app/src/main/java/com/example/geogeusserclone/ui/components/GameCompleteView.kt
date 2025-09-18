package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.models.GameMode
import com.example.geogeusserclone.data.models.GameStats
import java.util.concurrent.TimeUnit

@Composable
fun GameCompleteView(
    gameMode: GameMode,
    finalStats: GameStats?,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (finalStats == null) {
        // Fallback wenn keine Stats verfügbar
        BasicGameCompleteView(
            gameMode = gameMode,
            onPlayAgain = onPlayAgain,
            onBackToMenu = onBackToMenu,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header mit Modus-spezifischem Titel
            GameCompleteHeader(gameMode = gameMode, finalStats = finalStats)
        }

        item {
            // Haupt-Score-Anzeige
            MainScoreCard(finalStats = finalStats)
        }

        when (gameMode) {
            GameMode.CLASSIC -> {
                item { ClassicModeResults(finalStats = finalStats) }
            }
            GameMode.BLITZ -> {
                item { BlitzModeResults(finalStats = finalStats) }
            }
            GameMode.ENDLESS -> {
                item { EndlessModeResults(finalStats = finalStats) }
            }
        }

        item {
            // Detaillierte Statistiken
            DetailedStatsCard(finalStats = finalStats)
        }

        item {
            // Action Buttons
            ActionButtonsSection(
                onPlayAgain = onPlayAgain,
                onBackToMenu = onBackToMenu,
                gameMode = gameMode
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GameCompleteHeader(
    gameMode: GameMode,
    finalStats: GameStats
) {
    val (title, subtitle, emoji) = when (gameMode) {
        GameMode.CLASSIC -> Triple(
            "Spiel beendet!",
            "Klassischer Modus abgeschlossen",
            "🎯"
        )
        GameMode.BLITZ -> Triple(
            "Blitz beendet!",
            "Schnelle Runden gemeistert",
            "⚡"
        )
        GameMode.ENDLESS -> Triple(
            "Session beendet!",
            "Endlos-Modus gestoppt",
            "∞"
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MainScoreCard(finalStats: GameStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gesamt-Score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${finalStats.totalScore}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScoreStat(
                    label = "Runden",
                    value = "${finalStats.totalRounds}",
                    icon = Icons.Default.Star // Bereits verfügbar
                )
                ScoreStat(
                    label = "Durchschnitt",
                    value = "${finalStats.averageScore.toInt()}",
                    icon = Icons.Default.Info // KORRIGIERT: Calculate → Info (garantiert verfügbar)
                )
            }
        }
    }
}

@Composable
private fun ClassicModeResults(finalStats: GameStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🎯 Klassik-Ergebnis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val performance = when {
                finalStats.averageScore >= 4000 -> "Exzellent! 🏆"
                finalStats.averageScore >= 3000 -> "Sehr gut! 🥈"
                finalStats.averageScore >= 2000 -> "Gut! 🥉"
                else -> "Üben macht den Meister! 💪"
            }

            Text(
                text = "Leistung: $performance",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Perfekte Runden: ${finalStats.perfectRounds}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BlitzModeResults(finalStats: GameStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚡ Blitz-Ergebnis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val avgTimePerRound = finalStats.averageTimePerRound / 1000

            Text(
                text = "Durchschnittszeit: ${avgTimePerRound}s pro Runde",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            val speed = when {
                avgTimePerRound < 15 -> "Blitzschnell! ⚡"
                avgTimePerRound < 25 -> "Schnell! 🚀"
                else -> "Bedacht! 🤔"
            }

            Text(
                text = "Geschwindigkeit: $speed",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EndlessModeResults(finalStats: GameStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "∞ Endlos-Ergebnis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Beste Serie",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${finalStats.bestStreak} 🔥", // KORRIGIERT: Verwende bestStreak
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Column {
                    Text(
                        text = "Session-Zeit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatDuration(finalStats.totalTime), // KORRIGIERT: Verwende totalTime
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedStatsCard(finalStats: GameStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Detaillierte Statistiken",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailStat(
                    label = "Beste Runde",
                    value = "${finalStats.bestRoundScore}", // KORRIGIERT: Verwende bestRoundScore
                    color = Color(0xFF4CAF50)
                )

                DetailStat(
                    label = "Schlechteste",
                    value = "${finalStats.worstRoundScore}", // KORRIGIERT: Verwende worstRoundScore
                    color = Color(0xFFFF5722)
                )

                DetailStat(
                    label = "Perfekte",
                    value = "${finalStats.perfectRounds}", // KORRIGIERT: Verwende perfectRounds
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    gameMode: GameMode
) {
    val playAgainText = when (gameMode) {
        GameMode.CLASSIC -> "Nochmal spielen"
        GameMode.BLITZ -> "Neuer Blitz"
        GameMode.ENDLESS -> "Neue Session"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playAgainText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onBackToMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Zurück zum Menü")
        }
    }
}

@Composable
private fun BasicGameCompleteView(
    gameMode: GameMode,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (gameMode) {
                GameMode.CLASSIC -> "🎯"
                GameMode.BLITZ -> "⚡"
                GameMode.ENDLESS -> "∞"
            },
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Spiel beendet!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nochmal spielen")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackToMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zurück zum Menü")
        }
    }
}

@Composable
private fun ScoreStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailStat(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return "${minutes}m ${seconds}s"
}
