package com.example.geogeusserclone.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place // Verwende Place statt Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.database.entities.GuessEntity
import com.example.geogeusserclone.ui.components.GameCompletionScreen
import com.example.geogeusserclone.ui.components.RoundResultView
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.utils.DistanceCalculator
import com.example.geogeusserclone.utils.enableEdgeToEdge
import com.example.geogeusserclone.viewmodels.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class GameActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeoGeusserCloneTheme {
                GameScreen(
                    gameViewModel = gameViewModel,
                    onBackPressed = { finish() }
                )
            }
        }

        // Starte neues Spiel
        gameViewModel.startNewGame()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onBackPressed: () -> Unit
) {
    val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(gameState.error) {
        gameState.error?.let { error ->
            // Zeige Error-Snackbar
            delay(3000)
            gameViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("GeoGuesser")

                        val currentGame = gameState.currentGame
                        if (currentGame != null) {
                            Text(
                                text = "Runde ${currentGame.currentRound}/${currentGame.totalRounds}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${currentGame.score} Punkte",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (gameState.currentLocation != null && !gameState.showMap) {
                        IconButton(onClick = { gameViewModel.showMap() }) {
                            Icon(Icons.Default.Place, contentDescription = "Karte anzeigen")
                        }
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
                gameState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                gameState.showGameCompletion -> {
                    val currentGame = gameState.currentGame
                    if (currentGame != null) {
                        val guesses by gameViewModel.getGameGuesses().collectAsStateWithLifecycle(initialValue = emptyList())

                        GameCompletionScreen(
                            game = currentGame,
                            guesses = guesses,
                            onPlayAgain = { gameViewModel.startNewGame() },
                            onMainMenu = onBackPressed
                        )
                    }
                }

                gameState.showRoundResult && gameState.revealGuessResult != null -> {
                    RoundResultView(
                        guess = gameState.revealGuessResult,
                        onNextRound = { gameViewModel.proceedToNextRound() }
                    )
                }

                gameState.showMap -> {
                    MapViewScreen(
                        onGuessSubmitted = { lat, lng ->
                            gameViewModel.submitGuess(lat, lng)
                        },
                        onBackToImage = { gameViewModel.hideMap() }
                    )
                }

                else -> {
                    val currentLocation = gameState.currentLocation
                    if (currentLocation != null) {
                        LocationImageScreen(
                            location = currentLocation,
                            timeRemaining = gameState.timeRemaining,
                            onShowMap = { gameViewModel.showMap() }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Lade Spiel...")
                        }
                    }
                }
            }

            // Error Snackbar
            gameState.error?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
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
fun LocationImageScreen(
    location: com.example.geogeusserclone.data.database.entities.LocationEntity,
    timeRemaining: Long,
    onShowMap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Timer
        LinearProgressIndicator(
            progress = { (timeRemaining / 120000f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                timeRemaining > 60000 -> MaterialTheme.colorScheme.primary
                timeRemaining > 30000 -> Color(0xFFFFC107)
                else -> MaterialTheme.colorScheme.error
            }
        )

        Text(
            text = "Zeit: ${timeRemaining / 1000}s",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Location Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(location.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Location Image",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { onShowMap() }
        )

        // Anweisungen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Wo befindet sich dieser Ort?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tippe auf das Bild oder den Karten-Button, um deine Vermutung auf der Karte zu platzieren.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onShowMap,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Karte öffnen")
        }
    }
}

@Composable
fun MapViewScreen(
    onGuessSubmitted: (Double, Double) -> Unit,
    onBackToImage: () -> Unit
) {
    // Vereinfachte Map-Implementation
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = onBackToImage,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Zurück zum Bild")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Karte hier implementieren",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Dummy-Guess für Test
                        onGuessSubmitted(48.8566, 2.3522) // Paris
                    }
                ) {
                    Text("Test-Guess abgeben")
                }
            }
        }
    }
}