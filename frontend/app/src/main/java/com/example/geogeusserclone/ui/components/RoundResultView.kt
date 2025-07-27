package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.utils.DistanceCalculator
import com.example.geogeusserclone.utils.ScoreCalculator

@Composable
fun RoundResultView(
    guess: GuessEntity,
    onNextRound: () -> Unit,
    onShowMap: () -> Unit,
    isLastRound: Boolean,
    modifier: Modifier = Modifier
) {
    val scoreRating = ScoreCalculator.getScoreRating(guess.score)
    val scoreColor = ScoreCalculator.getScoreColor(scoreRating)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = scoreColor.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score Animation
                Text(
                    text = "${guess.score}",
                    style = MaterialTheme.typography.displayLarge,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = getRatingText(scoreRating),
                    style = MaterialTheme.typography.titleMedium,
                    color = scoreColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Distance Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DistanceCalculator.formatDistance(guess.distance),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "entfernt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score Breakdown
        ScoreBreakdownCard(guess = guess)

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Visualization
        PerformanceIndicator(scoreRating = scoreRating)

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onShowMap,
                modifier = Modifier.weight(1f)
            ) {
                Text("Karte anzeigen")
            }

            Button(
                onClick = onNextRound,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isLastRound) "Ergebnisse" else "Weiter")
            }
        }
    }
}

@Composable
private fun ScoreBreakdownCard(
    guess: GuessEntity,
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
                text = "Punkte-Aufschlüsselung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val distanceScore = ScoreCalculator.calculateScore(guess.distance, 0L)
            val timeBonus = guess.score - distanceScore

            ScoreBreakdownItem(
                label = "Entfernungs-Punkte",
                value = distanceScore,
                maxValue = 5000
            )

            if (timeBonus > 0) {
                ScoreBreakdownItem(
                    label = "Zeit-Bonus",
                    value = timeBonus,
                    maxValue = 500
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gesamt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${guess.score}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ScoreBreakdownItem(
    label: String,
    value: Int,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { (value.toFloat() / maxValue.toFloat()).coerceAtMost(1f) },
                modifier = Modifier.width(60.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PerformanceIndicator(
    scoreRating: ScoreRating,
    modifier: Modifier = Modifier
) {
    val performanceData = listOf(
        "Perfekt" to (if (scoreRating == ScoreRating.PERFECT) 1f else 0f),
        "Ausgezeichnet" to (if (scoreRating == ScoreRating.EXCELLENT) 1f else 0f),
        "Gut" to (if (scoreRating == ScoreRating.GOOD) 1f else 0f),
        "Okay" to (if (scoreRating == ScoreRating.FAIR) 1f else 0f),
        "Schlecht" to (if (scoreRating == ScoreRating.POOR || scoreRating == ScoreRating.TERRIBLE) 1f else 0f)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Leistung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                performanceData.forEach { (label, progress) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (progress > 0f)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (progress > 0f) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (progress > 0f)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getRatingText(rating: ScoreRating): String {
    return when (rating) {
        ScoreRating.PERFECT -> "Perfekt! 🎯"
        ScoreRating.EXCELLENT -> "Ausgezeichnet! 🌟"
        ScoreRating.GOOD -> "Gut gemacht! 👍"
        ScoreRating.FAIR -> "Nicht schlecht! 👌"
        ScoreRating.POOR -> "Verbesserungswürdig 📍"
        ScoreRating.TERRIBLE -> "Mehr Glück beim nächsten Mal 🤞"
    }
}}