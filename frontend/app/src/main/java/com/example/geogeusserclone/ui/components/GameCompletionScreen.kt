/**
 * GameCompletionScreen.kt
 *
 * Diese Datei enthält die Spielabschluss-Komponenten für die GeoGuess-App.
 * Sie zeigt detaillierte Ergebnisse, Statistiken und Rundenergebnisse nach
 * einem abgeschlossenen Spiel an.
 *
 * Architektur-Integration:
 * - Game Results: Umfassende Darstellung von Spielergebnissen
 * - Statistics Display: Detaillierte Analyse der Spielperformance
 * - Navigation: Weiterleitungsoptionen für neue Spiele oder Hauptmenü
 * - Visual Feedback: Farbkodierte Bewertungen und Achievement-Anzeigen
 */
package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.database.entities.GameEntity
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.utils.DistanceCalculator
import com.example.geogeusserclone.utils.ScoreCalculator

/**
 * Hauptkomponente für Spielabschluss-Bildschirm
 *
 * Zeigt umfassende Spielstatistiken, Rundenergebnisse und Aktionsoptionen
 * nach einem abgeschlossenen Spiel.
 *
 * @param game Abgeschlossenes Spiel mit Gesamt-Score
 * @param guesses Liste aller Rateversuche des Spiels
 * @param onPlayAgain Callback für erneutes Spielen
 * @param onMainMenu Callback für Rückkehr zum Hauptmenü
 * @param modifier Layout-Modifier
 */
@Composable
fun GameCompletionScreen(
    game: GameEntity,
    guesses: List<GuessEntity>,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hauptergebnis-Card
            GameResultHeader(game = game, guesses = guesses)
        }

        item {
            Text(
                text = "Rundenergebnisse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Einzelne Rundenergebnisse
        items(guesses.withIndex().toList()) { (index, guess) ->
            GuessResultCard(
                roundNumber = index + 1,
                guess = guess
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action-Buttons
        item {
            ActionButtonsSection(
                onPlayAgain = onPlayAgain,
                onMainMenu = onMainMenu
            )
        }
    }
}

/**
 * Hauptergebnis-Header mit Gesamtstatistiken
 */
@Composable
private fun GameResultHeader(
    game: GameEntity,
    guesses: List<GuessEntity>
) {
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
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Spiel abgeschlossen!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${game.score} Punkte",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistiken
            if (guesses.isNotEmpty()) {
                val averageDistance = guesses.sumOf { it.distance } / guesses.size
                val bestGuess = guesses.minByOrNull { it.distance }

                Text(
                    text = "Durchschnittliche Entfernung: ${DistanceCalculator.formatDistance(averageDistance)}",
                    style = MaterialTheme.typography.bodyLarge
                )

                bestGuess?.let {
                    Text(
                        text = "Beste Vermutung: ${DistanceCalculator.formatDistance(it.distance)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Gesamtbewertung
                val rating = ScoreCalculator.getScoreRating(game.score)
                Text(
                    text = "${rating.emoji} ${rating.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = rating.color
                )
            }
        }
    }
}

/**
 * Einzelne Rundenergebnis-Card
 *
 * @param roundNumber Nummer der Runde
 * @param guess Rateversuch-Daten für diese Runde
 */
@Composable
private fun GuessResultCard(
    roundNumber: Int,
    guess: GuessEntity
) {
    val rating = ScoreCalculator.getScoreRating(guess.score)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Runde $roundNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${guess.score} Punkte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = rating.color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Entfernung",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DistanceCalculator.formatDistance(guess.distance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Zeit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${guess.timeSpent / 1000}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Bewertung",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${rating.emoji} ${rating.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = rating.color
                    )
                }
            }
        }
    }
}

/**
 * Action-Buttons-Sektion
 *
 * @param onPlayAgain Callback für erneutes Spielen
 * @param onMainMenu Callback für Hauptmenü
 */
@Composable
private fun ActionButtonsSection(
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
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
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Nochmal spielen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Zurück zum Menü")
        }
    }
}
