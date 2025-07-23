package com.example.geogeusserclone.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.components.*
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoGeusserCloneTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.currentGame == null) {
            viewModel.createNewGame()
        }
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.gameCompleted -> {
            GameCompletionScreen(
                game = state.currentGame!!,
                guesses = state.currentGuesses,
                onPlayAgain = { viewModel.createNewGame() },
                onMainMenu = { /* Navigate to main menu */ }
            )
        }

        state.showingResults && state.lastGuessResult != null -> {
            RoundResultView(
                guess = state.lastGuessResult!!,
                onNextRound = { viewModel.proceedToNextRound() },
                onShowMap = { /* Show result map */ },
                isLastRound = state.currentGame?.let {
                    it.currentRound >= it.totalRounds
                } ?: false
            )
        }

        state.isMapVisible -> {
            GuessMapView(
                onGuessSelected = { lat, lng ->
                    viewModel.submitGuess(lat, lng)
                    viewModel.hideMap()
                },
                onMapClose = { viewModel.hideMap() }
            )
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Game Progress
                state.currentGame?.let { game ->
                    GameProgressCard(
                        currentRound = game.currentRound,
                        totalRounds = game.totalRounds,
                        score = game.score
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location Image
                LocationImageView(
                    location = state.currentLocation,
                    timeRemaining = state.timeRemaining,
                    onMapClick = { viewModel.showMap() },
                    modifier = Modifier.weight(1f)
                )

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameProgressCard(
    currentRound: Int,
    totalRounds: Int,
    score: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Round $currentRound/$totalRounds",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Score: $score",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}