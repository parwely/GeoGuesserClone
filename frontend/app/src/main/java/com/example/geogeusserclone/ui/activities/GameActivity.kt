package com.example.geogeusserclone.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.ui.components.GameCompletionScreen
import com.example.geogeusserclone.ui.components.GuessMapView
import com.example.geogeusserclone.ui.components.LocationImageScreen
import com.example.geogeusserclone.ui.components.RoundResultView
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.utils.enableEdgeToEdge
import com.example.geogeusserclone.viewmodels.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow

@AndroidEntryPoint
class GameActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeoGeusserCloneTheme {
                val gameState by gameViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    gameViewModel.startNewGame()
                }

                GameScreen(
                    gameState = gameState,
                    onGuess = { lat, lng -> gameViewModel.submitGuess(lat, lng) },
                    onNextRound = { gameViewModel.proceedToNextRound() },
                    onShowMap = { gameViewModel.showMap() },
                    onHideMap = { gameViewModel.hideMap() },
                    onPan = { /* Pan-Event wird bereits in LocationImageScreen behandelt */ },
                    onBack = { finish() },
                    getGuesses = { gameViewModel.getGameGuesses() }
                )
            }
        }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onGuess: (Double, Double) -> Unit,
    onNextRound: () -> Unit,
    onShowMap: () -> Unit,
    onHideMap: () -> Unit,
    onPan: (Float) -> Unit,
    onBack: () -> Unit,
    getGuesses: () -> Flow<List<GuessEntity>>
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(gameState.error) {
        gameState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Haupt-Spielinhalt
            if (gameState.isLoading && gameState.currentGame == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (gameState.currentLocation != null && !gameState.showGameCompletion) {
                LocationImageScreen(
                    location = gameState.currentLocation,
                    timeRemaining = gameState.timeRemaining,
                    onShowMap = onShowMap,
                    onPan = onPan,
                    streetViewAvailable = gameState.streetViewAvailable // Pass flag from UI state
                )
            }

            // Map-Ansicht zum Raten
            if (gameState.showMap) {
                GuessMapView(
                    onGuessSelected = { lat, lng ->
                        onGuess(lat, lng)
                        onHideMap()
                    },
                    onMapClose = onHideMap
                )
            }

            // Ergebnis der Runde
            if (gameState.showRoundResult) {
                RoundResultView(
                    guess = gameState.revealGuessResult,
                    onNextRound = onNextRound
                )
            }

            // Spiel-Abschluss-Bildschirm
            if (gameState.showGameCompletion) {
                val guesses by getGuesses().collectAsState(initial = emptyList())
                gameState.currentGame?.let { game ->
                    GameCompletionScreen(
                        game = game,
                        guesses = guesses,
                        onPlayAgain = { /* TODO: Implement Play Again */ },
                        onMainMenu = onBack
                    )
                }
            }
        }
    }
}