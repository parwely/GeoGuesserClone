package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.geogeusserclone.data.models.ScoreResponse
import com.example.geogeusserclone.data.models.GameState
import kotlin.math.roundToInt

@Composable
fun RoundResultView(
    scoreResponse: ScoreResponse,
    gameState: GameState,
    onNextRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { }, // Prevent dismissing by clicking outside
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Round Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Score display
                ScoreDisplay(
                    score = scoreResponse.score,
                    maxScore = scoreResponse.maxScore
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Distance display
                DistanceDisplay(distanceMeters = scoreResponse.distanceMeters)

                Spacer(modifier = Modifier.height(24.dp))

                // Location info
                LocationInfoCard(scoreResponse = scoreResponse)

                Spacer(modifier = Modifier.height(16.dp))

                // Total score
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Score",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${gameState.totalScore}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Next round button
                Button(
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Next Round")
                }
            }
        }
    }
}

@Composable
private fun ScoreDisplay(score: Int, maxScore: Int) {
    val scorePercentage = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val scoreColor = when {
        scorePercentage >= 0.8f -> Color(0xFF4CAF50) // Green
        scorePercentage >= 0.5f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Score",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = "out of $maxScore",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DistanceDisplay(distanceMeters: Double) {
    val distanceText = when {
        distanceMeters < 1000 -> "${distanceMeters.roundToInt()} m"
        distanceMeters < 100000 -> "${(distanceMeters / 1000).roundToInt()} km"
        else -> "${(distanceMeters / 1000).roundToInt()} km"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Distance",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = distanceText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun LocationInfoCard(scoreResponse: ScoreResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Actual Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            scoreResponse.actualLocation.city?.let { city ->
                Text(
                    text = "City: $city",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            scoreResponse.actualLocation.country?.let { country ->
                Text(
                    text = "Country: $country",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "Coordinates: ${scoreResponse.actualLocation.lat.format(4)}, ${scoreResponse.actualLocation.lng.format(4)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
