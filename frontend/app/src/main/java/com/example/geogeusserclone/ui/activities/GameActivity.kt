package com.example.geogeusserclone.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.ui.components.InteractiveStreetViewWithFallback
import com.example.geogeusserclone.ui.components.GuessMapView
import com.example.geogeusserclone.ui.components.RoundResultView
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.GameViewModel
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.models.GameUiState
import com.example.geogeusserclone.data.database.entities.LocationEntity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeoGeusserCloneTheme {
                val gameState by gameViewModel.gameState.collectAsState()
                val uiState by gameViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    gameViewModel.startNewRound()
                }

                StreetViewGameScreen(
                    gameState = gameState,
                    uiState = uiState,
                    onGuess = { lat, lng -> gameViewModel.submitGuess(lat, lng) },
                    onNextRound = { gameViewModel.nextRound() },
                    onShowMap = { gameViewModel.showGuessMap() },
                    onHideMap = { gameViewModel.hideGuessMap() },
                    onStreetViewReady = { gameViewModel.onStreetViewReady() },
                    onBack = { finish() },
                    onClearError = { gameViewModel.clearError() }
                )
            }
        }
    }
}

@Composable
fun StreetViewGameScreen(
    gameState: GameState,
    uiState: GameUiState,
    onGuess: (Double, Double) -> Unit,
    onNextRound: () -> Unit,
    onShowMap: () -> Unit,
    onHideMap: () -> Unit,
    onStreetViewReady: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            onClearError()
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
            when {
                // Loading state
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading new location...")
                        }
                    }
                }

                // Active round with Street View
                gameState.isRoundActive && gameState.currentRound != null -> {
                    InteractiveStreetViewWithFallback(
                        location = LocationEntity(
                            id = gameState.currentRound.location.id.toString(),
                            latitude = gameState.currentRound.location.lat,
                            longitude = gameState.currentRound.location.lng,
                            imageUrl = "", // Will be determined by the component
                            country = gameState.currentRound.location.country,
                            city = gameState.currentRound.location.city,
                            difficulty = gameState.currentRound.location.difficulty
                        ),
                        modifier = Modifier.fillMaxSize()
                    )

                    // Game UI overlay
                    GameUIOverlay(
                        uiState = uiState,
                        gameState = gameState,
                        onShowMap = onShowMap,
                        onBack = onBack
                    )
                }

                // No active round
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active round")
                    }
                }
            }

            // Guess Map overlay
            if (uiState.showGuessMap) {
                GuessMapView(
                    onGuessSelected = { lat, lng ->
                        onGuess(lat, lng)
                        onHideMap()
                    },
                    onMapClose = onHideMap,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Round Result overlay
            if (uiState.showResults && uiState.lastScoreResponse != null) {
                RoundResultView(
                    scoreResponse = uiState.lastScoreResponse,
                    gameState = gameState,
                    onNextRound = onNextRound,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun GameUIOverlay(
    uiState: GameUiState,
    gameState: GameState,
    onShowMap: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with back button and score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Text("← Back")
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "Score: ${gameState.totalScore}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom action area
        if (uiState.streetViewReady && !uiState.showGuessMap && !uiState.showResults) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Where do you think this is?",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onShowMap,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Make Your Guess")
                    }
                }
            }
        }

        // Loading indicator when Street View is not ready
        if (!uiState.streetViewReady && uiState.streetViewAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading Street View...")
                }
            }
        }

        // Street View not available message
        if (!uiState.streetViewAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Street View not available for this location",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onShowMap
                    ) {
                        Text("Make Your Guess Anyway")
                    }
                }
            }
        }
    }
}
