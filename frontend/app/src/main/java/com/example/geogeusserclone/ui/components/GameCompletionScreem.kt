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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Final Score Card
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
                    text = "Gesamtpunkte",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Round Summary
        Text(
            text = "Runden-Übersicht",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(guesses.size) { index ->
                val guess = guesses[index]
                RoundSummaryCard(
                    guess = guess,
                    roundNumber = index + 1, // Übergabe der Rundennummer
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
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

@Composable
fun RoundSummaryCard(
    guess: GuessEntity,
    roundNumber: Int, // Hinzufügen des roundNumber Parameters
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ScoreCalculator.getScoreColor(
                ScoreCalculator.getScoreRating(guess.score)
            ).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Runde $roundNumber", // Verwende roundNumber anstatt guesses.indexOf
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = DistanceCalculator.formatDistance(guess.distance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${guess.score}",
                style = MaterialTheme.typography.headlineSmall,
                color = ScoreCalculator.getScoreColor(
                    ScoreCalculator.getScoreRating(guess.score)
                )
            )
        }
    }
}