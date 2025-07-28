package com.example.geogeusserclone.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.components.MapGuessComponent
import com.example.geogeusserclone.ui.components.StreetViewComponent
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.geogeusserclone.ui.components.GameCompletionScreen
import com.example.geogeusserclone.ui.components.RoundResultView

@AndroidEntryPoint
class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
                GameScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onNavigateBack: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel()
) {
    val gameState by gameViewModel.uiState.collectAsState()
    var guesses by remember { mutableStateOf<List<com.example.geogeusserclone.data.database.entities.GuessEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (gameState.currentGame == null) {
            gameViewModel.startNewGame()
        }
    }

    // Collect guesses
    LaunchedEffect(gameState.currentGame) {
        gameState.currentGame?.let { game ->
            gameViewModel.getGameGuesses().collect { guessList ->
                guesses = guessList
            }
        }
    }

    if (gameState.isLoading && gameState.currentGame == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    gameState.currentGame?.let { game ->
                        Text("Runde ${game.currentRound}/${game.totalRounds}")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                gameState.showMap -> {
                    MapGuessComponent(
                        onGuessSelected = { lat, lng ->
                            gameViewModel.submitGuess(lat, lng)
                        },
                        onMapClose = {
                            gameViewModel.hideMap()
                        }
                    )
                }

                gameState.showRoundResult -> {
                    RoundResultView(
                        guess = gameState.revealGuessResult,
                        onNextRound = {
                            gameViewModel.proceedToNextRound()
                        }
                    )
                }

                gameState.showGameCompletion -> {
                    gameState.currentGame?.let { game ->
                        GameCompletionScreen(
                            game = game,
                            guesses = guesses,
                            onPlayAgain = {
                                gameViewModel.startNewGame()
                            },
                            onMainMenu = onNavigateBack
                        )
                    }
                }

                else -> {
                    GamePlayScreen(
                        gameState = gameState,
                        onMapClick = { gameViewModel.showMap() },
                        onClearError = { gameViewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
fun GamePlayScreen(
    gameState: com.example.geogeusserclone.data.models.GameState,
    onMapClick: () -> Unit,
    onClearError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Game Info Header
        gameState.currentGame?.let { game ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Punkte: ${game.score}")
                    Text("Zeit: ${gameState.timeRemaining / 1000}s")
                }
            }
        }

        // Street View
        StreetViewComponent(
            location = gameState.currentLocation,
            onMapClick = onMapClick,
            modifier = Modifier.weight(1f)
        )
    }

    // Error Snackbar
    gameState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            onClearError()
        }
    }
}

@Composable
fun RoundResultScreen(
    guess: com.example.geogeusserclone.data.database.entities.GuessEntity?,
    onNextRound: () -> Unit
) {
    guess?.let { guessEntity ->
        Card(
            modifier = Modifier
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
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "${guessEntity.score} Punkte",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
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
