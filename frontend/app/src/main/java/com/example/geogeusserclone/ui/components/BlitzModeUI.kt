/**
 * BlitzModeUI.kt
 *
 * Diese Datei enthält die spezielle UI-Komponenten für den Blitz-Spielmodus.
 * Sie implementiert ein zeitdruckbasiertes Interface mit Countdown-Timer
 * und vereinfachten Interaktionen für schnelle Entscheidungen.
 *
 * Architektur-Integration:
 * - Game Mode UI: Spezialisierte UI für Blitz-Modus Anforderungen
 * - Timer Management: Visueller Countdown mit Warnungen
 * - Simplified UX: Reduzierte Optionen für schnellere Entscheidungen
 * - Animation System: Smooth Animationen für Timer und Status-Updates
 * - Accessibility: Klare visuelle Hinweise für zeitkritische Aktionen
 */
package com.example.geogeusserclone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star // Sicher verfügbar
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow // KORRIGIERT: Speed → PlayArrow (sicher verfügbar)
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.models.GameState
import com.example.geogeusserclone.data.models.GameUiState
import kotlinx.coroutines.delay

/**
 * Haupt-UI-Komponente für den Blitz-Modus
 *
 * Stellt eine kompakte, zeitdruckoptimierte Benutzeroberfläche bereit.
 * Features:
 * - 30-Sekunden Vorbereitungszeit für Orientierung
 * - Prominenter Countdown-Timer mit visuellen Warnungen
 * - Vereinfachte Action-Buttons für schnelle Entscheidungen
 * - Kompakte Statistik-Anzeige
 * - Automatische Phase-Verwaltung (Vorbereitung → Aktiv → Timeout)
 *
 * @param gameState Aktueller Spielzustand
 * @param uiState Aktueller UI-Zustand
 * @param onShowMap Callback zum Anzeigen der Guess-Karte
 * @param onBack Callback für Rückkehr zum Menü
 * @param modifier Modifier für Layout-Anpassungen
 */
@Composable
fun BlitzModeUI(
    gameState: GameState,
    uiState: GameUiState,
    onShowMap: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Verwalte die verschiedenen Phasen des Blitz-Modus
    var gamePhase by remember { mutableStateOf("PREPARATION") } // PREPARATION, ACTIVE, TIME_UP
    var preparationTimeRemaining by remember { mutableLongStateOf(30000L) } // 30s Vorbereitungszeit

    // Vorbereitungsphase-Timer
    LaunchedEffect(gameState.currentRound?.roundId) {
        if (gameState.currentRound != null) {
            gamePhase = "PREPARATION"
            preparationTimeRemaining = 30000L

            // 30 Sekunden Vorbereitungsphase für Orientierung
            while (preparationTimeRemaining > 0 && gamePhase == "PREPARATION") {
                delay(100)
                preparationTimeRemaining -= 100
            }

            if (gamePhase == "PREPARATION") {
                gamePhase = "ACTIVE"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Kompakte Top-Bar mit Exit und Score
        BlitzTopBar(
            gameState = gameState,
            onBack = onBack
        )

        Spacer(modifier = Modifier.weight(1f))

        // Haupt-Timer/Status-Anzeige je nach Phase
        when (gamePhase) {
            "PREPARATION" -> {
                PreparationTimer(
                    timeRemaining = preparationTimeRemaining,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            "ACTIVE" -> {
                BlitzTimer(
                    timeRemaining = uiState.timeRemaining,
                    isTimeRunningOut = uiState.isTimeRunningOut,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action-Bereich - nur während aktiver Phase anzeigen
        if (gamePhase == "ACTIVE" && uiState.streetViewReady && !uiState.showGuessMap && !uiState.showResults) {
            CompactBlitzActionArea(
                onShowMap = onShowMap,
                timeRemaining = uiState.timeRemaining
            )
        }

        // Kompakte Informations-Anzeige
        CompactBlitzInfo(
            roundNumber = gameState.currentRoundNumber,
            maxRounds = gameState.maxRounds,
            phase = gamePhase
        )
    }
}

/**
 * Kompakte Top-Bar für Blitz-Modus
 *
 * Zeigt Exit-Button, Rundenzähler und aktuellen Score in platzsparender Anordnung.
 * Optimiert für schnelle Orientierung ohne Ablenkung vom Hauptspiel.
 *
 * @param gameState Aktueller Spielzustand für Score und Rundenzähler
 * @param onBack Callback für Verlassen des Spiels
 */
@Composable
private fun BlitzTopBar(
    gameState: GameState,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exit-Button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exit")
        }

        // Rundenzähler und Score in kompakter Darstellung
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rundenzähler mit Blitz-Icon
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow, // Blitz-Symbol
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${gameState.currentRoundNumber}/${gameState.maxRounds}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Aktueller Score
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = "${gameState.totalScore}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Animierter Countdown-Timer für aktive Blitz-Phase
 *
 * Zeigt verbleibende Zeit mit visuellen Warnungen und Animationen.
 * Verwendet Farb-Übergänge und Skalierung für erhöhte Dringlichkeit.
 *
 * @param timeRemaining Verbleibende Zeit in Millisekunden
 * @param isTimeRunningOut Ob die Zeit kritisch niedrig ist
 * @param modifier Layout-Modifier
 */
@Composable
private fun BlitzTimer(
    timeRemaining: Long,
    isTimeRunningOut: Boolean,
    modifier: Modifier = Modifier
) {
    val timeInSeconds = (timeRemaining / 1000).toInt()
    val progress = timeRemaining / 30000f // 30 Sekunden Gesamtzeit

    // Animierte Farb-Übergänge basierend auf verbleibender Zeit
    val timerColor by animateColorAsState(
        targetValue = when {
            isTimeRunningOut -> Color.Red
            timeInSeconds <= 15 -> Color(0xFFFF9800) // Orange Warnung
            else -> MaterialTheme.colorScheme.primary
        },
        label = "timer_color"
    )

    // Animations-Skalierung für Dringlichkeits-Feedback
    val scale by animateFloatAsState(
        targetValue = if (isTimeRunningOut) 1.1f else 1f,
        label = "timer_scale"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = timerColor.copy(alpha = 0.1f)
        ),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Kreisförmiger Progress-Indikator
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = timerColor,
                strokeWidth = 6.dp,
            )

            // Zentrale Zeit-Anzeige
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$timeInSeconds",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
                Text(
                    text = "sek",
                    style = MaterialTheme.typography.bodySmall,
                    color = timerColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Vorbereitungs-Timer für die 30-Sekunden Orientierungsphase
 *
 * Gibt dem Spieler Zeit sich zu orientieren bevor der eigentliche
 * Countdown beginnt.
 *
 * @param timeRemaining Verbleibende Vorbereitungszeit in Millisekunden
 * @param modifier Layout-Modifier
 */
@Composable
private fun PreparationTimer(
    timeRemaining: Long,
    modifier: Modifier = Modifier
) {
    val timeInSeconds = (timeRemaining / 1000).toInt()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vorbereitungs-Text
            Text(
                text = "Bereit in $timeInSeconds",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Kompakter Action-Bereich für Blitz-Modus
 *
 * Bietet den Haupt-"Jetzt Raten" Button mit klaren Anweisungen
 * und Zeitwarnungen für optimale Benutzerführung.
 *
 * @param onShowMap Callback zum Öffnen der Guess-Karte
 * @param timeRemaining Verbleibende Zeit für Warnungen
 */
@Composable
private fun CompactBlitzActionArea(
    onShowMap: () -> Unit,
    timeRemaining: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modus-Titel
            Text(
                text = "BLITZ-MODUS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Spielregeln-Hinweis
            Text(
                text = "Schnell entscheiden! Keine Navigation möglich.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Haupt-Action-Button
            Button(
                onClick = onShowMap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "JETZT RATEN!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Zeitwarnung bei kritisch niedriger Zeit
            if (timeRemaining < 10000) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Wenig Zeit! Auto-Guess in ${(timeRemaining / 1000).toInt()}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Kompakte Info-Leiste für Blitz-Modus Status
 *
 * Zeigt aktuellen Rundenstatus und Spielregeln in minimaler Form.
 * Angepasst an die verschiedenen Spiel-Phasen.
 *
 * @param roundNumber Aktuelle Rundennummer
 * @param maxRounds Maximale Anzahl Runden
 * @param phase Aktuelle Spielphase (PREPARATION/ACTIVE)
 */
@Composable
private fun CompactBlitzInfo(
    roundNumber: Int,
    maxRounds: Int,
    phase: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star, // Timer-Icon ersetzt durch Star (sicher verfügbar)
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Phasen-spezifischer Informationstext
            Text(
                text = when (phase) {
                    "PREPARATION" -> "Vorbereitung: ${roundNumber} von $maxRounds"
                    "ACTIVE" -> "Runde $roundNumber von $maxRounds • 30s pro Runde • Keine Street View Navigation"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
