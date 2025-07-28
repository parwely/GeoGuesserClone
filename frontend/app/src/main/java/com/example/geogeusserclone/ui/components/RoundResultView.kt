package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.database.entities.GuessEntity

@Composable
fun RoundResultView(
    guess: GuessEntity?,
    onNextRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    guess?.let { guessEntity ->
        Card(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Rundenergebnis",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "${guessEntity.score} Punkte",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Entfernung: ${"%.1f".format(guessEntity.distance)} km",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onNextRound,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Weiter")
                }
            }
        }
    }
}
