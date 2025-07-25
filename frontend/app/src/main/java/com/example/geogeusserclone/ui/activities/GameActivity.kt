package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.components.GameCompletionScreen
import com.example.geogeusserclone.ui.components.GuessMapView
import com.example.geogeusserclone.ui.components.LocationImageView
import com.example.geogeusserclone.ui.components.RoundResultView
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
                    onNavigateToMenu = {
                        startActivity(Intent(this, MenuActivity::class.java))
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
            gameViewModel.createNewGame()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when {
            gameState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
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
                        onMainMenu = onNavigateToMenu,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            gameState.isMapVisible -> {
                GuessMapView(
                    onGuessSelected = { lat, lng ->
                        gameViewModel.submitGuess(lat, lng)
                        gameViewModel.hideMap()
                    },
                    onMapClose = {
                        gameViewModel.hideMap()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            gameState.showingResults && gameState.lastGuessResult != null -> {
                val game = gameState.currentGame
                RoundResultView(
                    guess = gameState.lastGuessResult!!,
                    onNextRound = {
                        gameViewModel.proceedToNextRound()
                    },
                    onShowMap = {
                        // Show result map with both actual and guessed locations
                        gameViewModel.showMap()
                    },
                    isLastRound = game?.currentRound == game?.totalRounds,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                LocationImageView(
                    location = gameState.currentLocation,
                    timeRemaining = gameState.timeRemaining,
                    onMapClick = {
                        gameViewModel.showMap()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        gameState.error?.let { error ->
            LaunchedEffect(error) {
                // Show error snackbar or dialog
                gameViewModel.clearError()
            }
        }
    }
}