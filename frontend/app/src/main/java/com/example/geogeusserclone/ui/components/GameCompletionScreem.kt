package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.utils.DistanceCalculator
import com.example.geogeusserclone.utils.ScoreCalculator

@Composable
fun GameCompletionScreen(
    game: GameEntity,
    guesses: List<GuessEntity>,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val averageScore = guesses.map { it.score }.average().toInt()
    val averageDistance = guesses.map { it.distance }.average()
    val bestRound = guesses.maxByOrNull { it.score }
    val worstRound = guesses.minByOrNull { it.score }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Final Score Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.displayMedium
                    )

                    Text(
                        text = "Spiel beendet!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${game.score}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "von ${5000 * game.totalRounds} Punkten",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (game.score.toFloat() / (5000 * game.totalRounds).toFloat()) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        item {
            // Performance Statistics
            GameStatisticsCard(
                averageScore = averageScore,
                averageDistance = averageDistance,
                bestRound = bestRound,
                worstRound = worstRound
            )
        }

        item {
            Text(
                text = "Runden-Übersicht",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(guesses) { index, guess ->
            EnhancedRoundSummaryCard(
                guess = guess,
                roundNumber = index + 1,
                isBestRound = guess == bestRound,
                isWorstRound = guess == worstRound
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hauptmenü")
                }

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nochmal spielen")
                }
            }
        }
    }
}

@Composable
private fun GameStatisticsCard(
    averageScore: Int,
    averageDistance: Double,
    bestRound: GuessEntity?,
    worstRound: GuessEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistiken",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Ø Punkte",
                    value = "$averageScore",
                    icon = "📊"
                )

                StatisticItem(
                    label = "Ø Entfernung",
                    value = DistanceCalculator.formatDistance(averageDistance),
                    icon = "📍"
                )

                bestRound?.let {
                    StatisticItem(
                        label = "Beste Runde",
                        value = "${it.score}",
                        icon = "🏆"
                    )
                }

                worstRound?.let {
                    StatisticItem(
                        label = "Schlechteste",
                        value = "${it.score}",
                        icon = "📉"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EnhancedRoundSummaryCard(
    guess: GuessEntity,
    roundNumber: Int,
    isBestRound: Boolean,
    isWorstRound: Boolean,
    modifier: Modifier = Modifier
) {
    val scoreRating = ScoreCalculator.getScoreRating(guess.score)
    val scoreColor = ScoreCalculator.getScoreColor(scoreRating)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isBestRound -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                isWorstRound -> Color(0xFFF44336).copy(alpha = 0.1f)
                else -> scoreColor.copy(alpha = 0.1f)
            }
        ),
        border = if (isBestRound || isWorstRound) {
            BorderStroke(
                2.dp,
                if (isBestRound) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isBestRound || isWorstRound) {
                    Text(
                        text = if (isBestRound) "🏆" else "📉",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Column {
                    Text(
                        text = "Runde $roundNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DistanceCalculator.formatDistance(guess.distance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getRatingText(scoreRating),
                        style = MaterialTheme.typography.bodySmall,
                        color = scoreColor
                    )
                }
            }

            Text(
                text = "${guess.score}",
                style = MaterialTheme.typography.headlineSmall,
                color = scoreColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}