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
    val rating = ScoreCalculator.getScoreRating(guess.score)
    val color = ScoreCalculator.getScoreColor(rating)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Runden-Ergebnis",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${guess.score} Punkte",
                style = MaterialTheme.typography.displayMedium,
                color = color
            )

            Text(
                text = "Entfernung: ${DistanceCalculator.formatDistance(guess.distance)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = onShowMap) {
                    Text("Karte anzeigen")
                }

                Button(onClick = onNextRound) {
                    Text(if (isLastRound) "Spiel beenden" else "Nächste Runde")
                }
            }
        }
    }
}