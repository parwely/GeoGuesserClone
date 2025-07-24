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
import com.example.geogeusserclone.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
                GameScreen(
                    onNavigateToMenu = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    onNavigateToMenu: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel()
) {
    val gameState by gameViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (gameState.currentGame == null) {
            gameViewModel.createNewGame(Constants.GAME_MODE_SINGLE)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                gameState.gameCompleted -> {
                    // Game completion screen
                    gameState.currentGame?.let { game ->
                        GameCompletionScreen(
                            game = game,
                            guesses = gameState.currentGuesses,
                            onPlayAgain = {
                                gameViewModel.createNewGame(Constants.GAME_MODE_SINGLE)
                            },
                            onMainMenu = onNavigateToMenu
                        )
                    }
                }

                gameState.showingResults && gameState.lastGuessResult != null -> {
                    // Round result screen
                    RoundResultView(
                        guess = gameState.lastGuessResult!!,
                        onNextRound = {
                            gameViewModel.proceedToNextRound()
                        },
                        onShowMap = {
                            // TODO: Show result map
                        },
                        isLastRound = gameState.currentGame?.currentRound == gameState.currentGame?.totalRounds
                    )
                }

                gameState.isMapVisible -> {
                    // Map for guessing
                    MapGuessComponent(
                        onGuessSelected = { lat, lng ->
                            gameViewModel.submitGuess(lat, lng)
                            gameViewModel.hideMap()
                        },
                        onMapClose = {
                            gameViewModel.hideMap()
                        }
                    )
                }

                else -> {
                    // Main game view with location image
                    LocationImageView(
                        location = gameState.currentLocation,
                        timeRemaining = gameState.timeRemaining,
                        onMapClick = {
                            gameViewModel.showMap()
                        }
                    )
                }
            }

            // Loading overlay
            if (gameState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Lade...")
                        }
                    }
                }
            }

            // Error handling
            gameState.error?.let { error ->
                LaunchedEffect(error) {
                    // Show error snackbar or handle error
                    gameViewModel.clearError()
                }
            }
        }
    }
}
