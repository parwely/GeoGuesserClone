package com.example.geogeusserclone.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
                GameScreen(
                    onGameFinished = { finish() }
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    onGameFinished: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel()
) {
    val gameState by gameViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (gameState.currentGame == null) {
            gameViewModel.createNewGame()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                gameState.isLoading && gameState.currentLocation == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                gameState.gameCompleted -> {
                    gameState.currentGame?.let { game ->
                        GameCompletionScreen(
                            game = game,
                            guesses = gameState.currentGuesses,
                            onPlayAgain = {
                                gameViewModel.createNewGame()
                            },
                            onMainMenu = onGameFinished
                        )
                    }
                }

                gameState.isMapVisible -> {
                    GuessMapView(
                        onGuessSelected = { lat: Double, lng: Double ->
                            gameViewModel.submitGuess(lat, lng)
                            gameViewModel.hideMap()
                        },
                        onMapClose = {
                            gameViewModel.hideMap()
                        }
                    )
                }

                gameState.showingResults && gameState.lastGuessResult != null -> {
                    val game = gameState.currentGame
                    val lastGuess = gameState.lastGuessResult // Lokale Variable erstellen
                    val isLastRound = game != null && game.currentRound > game.totalRounds

                    lastGuess?.let { guess -> // Safe call verwenden
                        RoundResultView(
                            guess = guess,
                            onNextRound = {
                                gameViewModel.proceedToNextRound()
                            },
                            onShowMap = {
                                gameViewModel.showMap()
                            },
                            isLastRound = isLastRound
                        )
                    }
                }

                else -> {
                    // Haupt-Spielansicht
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Game Header
                        gameState.currentGame?.let { game ->
                            GameHeader(
                                currentRound = game.currentRound,
                                totalRounds = game.totalRounds,
                                score = game.score,
                                timeRemaining = gameState.timeRemaining,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        // Street View
                        StreetViewComponent(
                            location = gameState.currentLocation,
                            modifier = Modifier.weight(1f),
                            onMapClick = {
                                gameViewModel.showMap()
                            }
                        )
                    }
                }
            }

            // Error handling
            gameState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { gameViewModel.clearError() }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameHeader(
    currentRound: Int,
    totalRounds: Int,
    score: Int,
    timeRemaining: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Runde $currentRound/$totalRounds",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Punkte: $score",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "${timeRemaining / 1000}s",
                style = MaterialTheme.typography.titleMedium,
                color = if (timeRemaining < 10000) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}