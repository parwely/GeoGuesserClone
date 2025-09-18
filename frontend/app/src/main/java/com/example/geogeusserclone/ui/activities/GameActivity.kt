/**
 * GameActivity.kt
 *
 * Diese Datei enthält die Haupt-Spiel-Activity der GeoGuess-App.
 * Sie orchestriert das komplette Spielerlebnis und integriert alle Spielmodi,
 * Street View-Komponenten und Benutzerinteraktionen.
 *
 * Architektur-Integration:
 * - Game Controller: Zentrale Steuerung des Spielablaufs
 * - Multi-Mode Support: Unterstützt Classic, Blitz und Endless Modi
 * - Component Integration: Verbindet Street View, Maps und UI-Overlays
 * - State Management: Koordiniert GameState und GameUiState
 * - Fallback Systems: Emulator-Support und Fehlerbehandlung
 */
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
import com.example.geogeusserclone.ui.theme.GeoGuessTheme
import com.example.geogeusserclone.viewmodels.GameViewModel
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.models.GameUiState
import com.example.geogeusserclone.data.models.GameMode
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.ui.components.EmulatorFallbackMapComponent
import com.example.geogeusserclone.ui.components.BlitzModeUI
import com.example.geogeusserclone.ui.components.EndlessModeUI
import com.example.geogeusserclone.ui.components.GameCompleteView
import dagger.hilt.android.AndroidEntryPoint

/**
 * Haupt-Spiel-Activity
 *
 * Diese Activity koordiniert das komplette Spielerlebnis:
 * - Lädt und zeigt Street View-Inhalte
 * - Verwaltet Spielmodus-spezifische UI-Overlays
 * - Behandelt Benutzer-Guesses und Score-Berechnung
 * - Koordiniert Rundenübergänge und Spielende
 * - Bietet Fallback-Mechanismen für Emulator-Probleme
 */
@AndroidEntryPoint
class GameActivity : ComponentActivity() {

    // Injiziertes ViewModel für Spiellogik
    private val gameViewModel: GameViewModel by viewModels()

    /**
     * Initialisiert die Spiel-Activity
     *
     * Extrahiert Spielmodus aus Intent, konfiguriert Compose-UI
     * und startet das entsprechende Spiel.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extrahiere Spielmodus aus Intent-Parametern
        val gameModeString = intent.getStringExtra("GAME_MODE") ?: "CLASSIC"
        val gameMode = try {
            GameMode.valueOf(gameModeString)
        } catch (e: Exception) {
            GameMode.CLASSIC // Fallback zum klassischen Modus
        }

        setContent {
            GeoGuessTheme {
                val gameState by gameViewModel.gameState.collectAsState()
                val uiState by gameViewModel.uiState.collectAsState()

                // Starte Spiel beim ersten Laden
                LaunchedEffect(Unit) {
                    gameViewModel.startGame(gameMode)
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
                    onClearError = { gameViewModel.clearError() },
                    onEndEndlessGame = { gameViewModel.endEndlessGame() }
                )
            }
        }
    }
}

/**
 * Haupt-Spielbildschirm Composable
 *
 * Orchestriert alle Spiel-Komponenten und verwaltet verschiedene UI-States:
 * - Loading: Neue Runden laden
 * - Street View: Interaktive 360°-Ansicht der Location
 * - Guess Map: Weltkarte für Benutzer-Guess
 * - Results: Rundenergebnisse und Score-Anzeige
 * - Game Complete: Finale Statistiken und Aktionen
 *
 * @param gameState Aktueller Spielzustand
 * @param uiState Aktueller UI-Zustand
 * @param onGuess Callback für Benutzer-Guess mit Koordinaten
 * @param onNextRound Callback für Übergang zur nächsten Runde
 * @param onShowMap Callback zum Anzeigen der Guess-Karte
 * @param onHideMap Callback zum Verstecken der Guess-Karte
 * @param onStreetViewReady Callback wenn Street View geladen ist
 * @param onBack Callback für Verlassen des Spiels
 * @param onClearError Callback zum Löschen von Fehlermeldungen
 * @param onEndEndlessGame Callback für manuelles Beenden im Endlos-Modus
 */
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
    onClearError: () -> Unit,
    onEndEndlessGame: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Automatische Fehlerbehandlung mit Snackbar-Anzeige
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            onClearError()
        }
    }

    // Spielende-Zustand: Zeige finale Statistiken
    if (uiState.gameComplete) {
        GameCompleteView(
            gameMode = gameState.gameMode,
            finalStats = uiState.finalStats,
            onPlayAgain = {
                // Restart mit gleichem Modus (TODO: Implementierung im ViewModel)
            },
            onBackToMenu = onBack
        )
        return
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
                // Loading-Zustand: Neue Runde wird geladen
                uiState.isLoading -> {
                    LoadingScreen()
                }

                // Aktive Runde: Street View + Spielmodus-UI
                gameState.isRoundActive && gameState.currentRound != null -> {
                    // Street View als Hauptkomponente
                    InteractiveStreetViewWithFallback(
                        location = LocationEntity(
                            id = gameState.currentRound.location.id.toString(),
                            latitude = gameState.currentRound.location.lat,
                            longitude = gameState.currentRound.location.lng,
                            imageUrl = "", // Wird automatisch generiert
                            country = gameState.currentRound.location.country,
                            city = gameState.currentRound.location.city,
                            difficulty = gameState.currentRound.location.difficulty
                        ),
                        onStreetViewReady = onStreetViewReady,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Spielmodus-spezifische UI-Overlays
                    when (gameState.gameMode) {
                        GameMode.BLITZ -> {
                            BlitzModeUI(
                                gameState = gameState,
                                uiState = uiState,
                                onShowMap = onShowMap,
                                onBack = onBack,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        GameMode.ENDLESS -> {
                            EndlessModeUI(
                                gameState = gameState,
                                uiState = uiState,
                                onShowMap = onShowMap,
                                onBack = onBack,
                                onEndGame = onEndEndlessGame,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        GameMode.CLASSIC -> {
                            GameUIOverlay(
                                uiState = uiState,
                                gameState = gameState,
                                onShowMap = onShowMap,
                                onBack = onBack
                            )
                        }
                    }
                }

                // Keine aktive Runde: Fehler-Zustand
                else -> {
                    ErrorScreen(message = "Keine aktive Runde verfügbar")
                }
            }

            // Guess-Karte Overlay mit intelligentem Fallback-System
            if (uiState.showGuessMap) {
                GuessMapWithFallback(
                    onGuess = onGuess,
                    onHideMap = onHideMap
                )
            }

            // Rundenergebnis-Overlay
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

/**
 * Loading-Screen für Rundenübergänge
 *
 * Zeigt eine ansprechende Loading-Animation während neue Locations geladen werden.
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Lade neue Location...",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Fehler-Screen für unerwartete Zustände
 *
 * @param message Anzuzeigende Fehlermeldung
 */
@Composable
private fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Guess-Karte mit intelligentem Emulator-Fallback
 *
 * Bietet automatische Erkennung von Emulator-Problemen und alternative
 * Eingabemethoden wenn Map-Clicks nicht funktionieren.
 *
 * @param onGuess Callback für ausgewählte Koordinaten
 * @param onHideMap Callback zum Schließen der Karte
 */
@Composable
private fun GuessMapWithFallback(
    onGuess: (Double, Double) -> Unit,
    onHideMap: () -> Unit
) {
    // Emulator-Detection und Fallback-Management
    var showEmulatorFallback by remember { mutableStateOf(false) }
    var mapFailureDetected by remember { mutableStateOf(false) }
    var startTime by remember { mutableLongStateOf(0L) }

    // Starte Timer bei erstem Map-Anzeigen
    LaunchedEffect(Unit) {
        startTime = System.currentTimeMillis()
        showEmulatorFallback = false
        mapFailureDetected = false

        // Automatische Emulator-Fallback-Detection nach 15 Sekunden ohne Map-Clicks
        kotlinx.coroutines.delay(15000)
        if (!mapFailureDetected) {
            println("GameActivity: 🔧 Automatische Emulator-Fallback-Detection nach 15s ohne Map-Clicks")
            showEmulatorFallback = true
        }
    }

    if (showEmulatorFallback) {
        // Fallback-Komponente für Emulator-Probleme
        EmulatorFallbackMapComponent(
            onLocationSelected = { lat, lng ->
                println("GameActivity: ✅ Fallback-Location ausgewählt: $lat, $lng")
                onGuess(lat, lng)
                onHideMap()
            },
            onClose = {
                showEmulatorFallback = false
                onHideMap()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay mit Option zurück zur normalen Map
        FallbackModeIndicator(
            onSwitchBack = { showEmulatorFallback = false }
        )
    } else {
        // Normale GuessMapView mit Fallback-Button
        Box(modifier = Modifier.fillMaxSize()) {
            GuessMapView(
                onGuessSelected = { lat, lng ->
                    mapFailureDetected = false // Erfolgreicher Map-Click
                    onGuess(lat, lng)
                    onHideMap()
                },
                onMapClose = onHideMap,
                modifier = Modifier.fillMaxSize()
            )

            // Emulator-Fallback-Button nach 10 Sekunden
            if (System.currentTimeMillis() - startTime > 10000) {
                EmulatorFallbackButton(
                    onClick = { showEmulatorFallback = true }
                )
            }
        }
    }
}

/**
 * Klassisches Spiel-UI-Overlay
 *
 * Standard-UI für den klassischen Modus ohne Zeitdruck oder Streaks.
 * Fokussiert auf einfache, klare Benutzerführung.
 *
 * @param uiState Aktueller UI-Zustand
 * @param gameState Aktueller Spielzustand
 * @param onShowMap Callback zum Anzeigen der Guess-Karte
 * @param onBack Callback für Verlassen des Spiels
 */
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
        // Top-Bar mit Back-Button und Score-Anzeige
        ClassicTopBar(
            totalScore = gameState.totalScore,
            onBack = onBack
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action-Bereich am unteren Rand
        if (uiState.streetViewReady && !uiState.showGuessMap && !uiState.showResults) {
            ClassicActionArea(onShowMap = onShowMap)
        }

        // Loading-Indikator wenn Street View nicht bereit ist
        if (!uiState.streetViewReady && uiState.streetViewAvailable) {
            StreetViewLoadingIndicator()
        }

        // Street View nicht verfügbar Nachricht
        if (!uiState.streetViewAvailable) {
            StreetViewUnavailableCard(onShowMap = onShowMap)
        }
    }
}

/**
 * Top-Bar für klassischen Modus
 *
 * @param totalScore Aktueller Gesamtscore
 * @param onBack Callback für Verlassen des Spiels
 */
@Composable
private fun ClassicTopBar(
    totalScore: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Back-Button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Text("← Zurück")
        }

        // Score-Anzeige
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Text(
                text = "Score: $totalScore",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Action-Bereich für klassischen Modus
 *
 * @param onShowMap Callback zum Anzeigen der Guess-Karte
 */
@Composable
private fun ClassicActionArea(onShowMap: () -> Unit) {
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
                text = "Wo denkst du bist du?",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onShowMap,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Jetzt raten!")
            }
        }
    }
}

/**
 * Loading-Indikator für Street View
 */
@Composable
private fun StreetViewLoadingIndicator() {
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
            Text("Lade Street View...")
        }
    }
}

/**
 * Karte für nicht verfügbare Street View
 *
 * @param onShowMap Callback zum trotzdem Raten
 */
@Composable
private fun StreetViewUnavailableCard(onShowMap: () -> Unit) {
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
                text = "Street View nicht verfügbar für diese Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onShowMap) {
                Text("Trotzdem raten")
            }
        }
    }
}

/**
 * Fallback-Modus-Indikator
 *
 * @param onSwitchBack Callback zum Zurückwechseln zur normalen Karte
 */
@Composable
private fun FallbackModeIndicator(onSwitchBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔧 Emulator-Modus aktiv",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSwitchBack) {
                    Text("Zurück zur Map")
                }
            }
        }
    }
}

/**
 * Emulator-Fallback-Button
 *
 * @param onClick Callback zum Aktivieren des Fallback-Modus
 */
@Composable
private fun EmulatorFallbackButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        TextButton(onClick = onClick) {
            Text(
                text = "🔧 Map-Clicks funktionieren nicht? Hier klicken!",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
