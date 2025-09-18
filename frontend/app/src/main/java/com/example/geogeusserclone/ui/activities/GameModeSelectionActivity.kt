/**
 * GameModeSelectionActivity.kt
 *
 * Diese Datei enthält die Spielmodus-Auswahl-Activity der GeoGuess-App.
 * Sie bietet eine detaillierte Übersicht aller verfügbaren Spielmodi
 * mit Beschreibungen, Features und Schwierigkeitsgraden.
 *
 * Architektur-Integration:
 * - Game Mode Hub: Zentrale Auswahl für alle Spielvarianten
 * - Information Display: Detaillierte Erklärungen für neue Benutzer
 * - Navigation Layer: Weiterleitung zur GameActivity mit korrekten Parametern
 * - UX Design: Benutzerfreundliche Darstellung komplexer Spielregeln
 */
package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geogeusserclone.data.models.GameMode
import com.example.geogeusserclone.ui.theme.GeoGuessTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity für detaillierte Spielmodus-Auswahl
 *
 * Diese Activity bietet eine umfassende Übersicht aller Spielmodi
 * mit detaillierten Beschreibungen, Feature-Listen und Schwierigkeitsgraden.
 * Sie hilft Benutzern den passenden Spielmodus für ihre Präferenzen zu wählen.
 *
 * Features:
 * - Detaillierte Spielmodus-Karten mit Erklärungen
 * - Schwierigkeitsgrad-Anzeigen
 * - Feature-Listen für jeden Modus
 * - Direkte Navigation zur GameActivity
 * - Hilfe-Sektion für neue Benutzer
 */
@AndroidEntryPoint
class GameModeSelectionActivity : ComponentActivity() {

    /**
     * Initialisiert die Spielmodus-Auswahl
     *
     * Konfiguriert die Compose-UI und definiert Navigation-Callbacks
     * für die Weiterleitung zur GameActivity mit dem gewählten Modus.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGuessTheme {
                GameModeSelectionScreen(
                    onGameModeSelected = { gameMode ->
                        val intent = Intent(this, GameActivity::class.java).apply {
                            putExtra("GAME_MODE", gameMode.name)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * Haupt-Composable für Spielmodus-Auswahl
 *
 * Stellt eine scrollbare Liste aller verfügbaren Spielmodi dar
 * mit detaillierten Informationen und direkter Auswahl-Möglichkeit.
 *
 * @param onGameModeSelected Callback für Spielmodus-Auswahl
 * @param onBack Callback für Rückkehr zum Hauptmenü
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeSelectionScreen(
    onGameModeSelected: (GameMode) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            // Top-Bar mit Navigation
            TopAppBar(
                title = { Text("Spielmodus wählen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header-Sektion mit Erklärung
                GameModeSelectionHeader()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Klassischer Modus - Detaillierte Karte
            item {
                ClassicModeCard(onGameModeSelected = onGameModeSelected)
            }

            // Blitz-Modus - Detaillierte Karte
            item {
                BlitzModeCard(onGameModeSelected = onGameModeSelected)
            }

            // Endlos-Modus - Detaillierte Karte
            item {
                EndlessModeCard(onGameModeSelected = onGameModeSelected)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Hilfe-Sektion für neue Benutzer
            item {
                GameModeHelpSection()
            }
        }
    }
}

/**
 * Header-Sektion mit Titel und Beschreibung
 */
@Composable
private fun GameModeSelectionHeader() {
    Column {
        Text(
            text = "Wähle deinen Spielmodus",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Jeder Modus bietet ein einzigartiges Spielerlebnis",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Detaillierte Karte für klassischen Modus
 *
 * @param onGameModeSelected Callback für Modus-Auswahl
 */
@Composable
private fun ClassicModeCard(onGameModeSelected: (GameMode) -> Unit) {
    GameModeDetailCard(
        title = "Klassischer Modus",
        subtitle = "Das traditionelle GeoGuessr-Erlebnis",
        description = "5 Runden • Keine Zeitbegrenzung • Freie Bewegung",
        details = listOf(
            "🎯 5 Runden pro Spiel",
            "🚶 Freie Street View Navigation",
            "⏰ Keine Zeitbegrenzung",
            "📊 Punkte basierend auf Genauigkeit"
        ),
        icon = "🌍",
        difficulty = "Entspannt",
        difficultyColor = MaterialTheme.colorScheme.primary,
        onClick = { onGameModeSelected(GameMode.CLASSIC) }
    )
}

/**
 * Detaillierte Karte für Blitz-Modus
 *
 * @param onGameModeSelected Callback für Modus-Auswahl
 */
@Composable
private fun BlitzModeCard(onGameModeSelected: (GameMode) -> Unit) {
    GameModeDetailCard(
        title = "Blitz-Modus",
        subtitle = "Schnell und intensiv",
        description = "10 Runden • 30s pro Runde • Keine Bewegung",
        details = listOf(
            "⚡ 10 schnelle Runden",
            "⏱️ 30 Sekunden pro Runde",
            "🚫 Keine Street View Navigation",
            "🏆 Zeit-Bonus für schnelle Antworten",
            "💀 Auto-Guess bei Zeitablauf"
        ),
        icon = "⚡",
        difficulty = "Intensiv",
        difficultyColor = MaterialTheme.colorScheme.error,
        onClick = { onGameModeSelected(GameMode.BLITZ) })
    }


/**
 * Detaillierte Karte für Endlos-Modus
 *
 * @param onGameModeSelected Callback für Modus-Auswahl
 */
@Composable
private fun EndlessModeCard(onGameModeSelected: (GameMode) -> Unit) {
    GameModeDetailCard(
        title = "Endlos-Modus",
        subtitle = "Spiele so lange du willst",
        description = "Unbegrenzte Runden • Streak-System • Steigende Schwierigkeit",
        details = listOf(
            "∞ Unbegrenzte Runden",
            "🔥 Streak-System für Bonus-Punkte",
            "📈 Schwierigkeit steigt mit Streak",
            "💾 Automatisches Speichern",
            "🏃 Beende jederzeit"
        ),
        icon = "∞",
        difficulty = "Herausforderung",
        difficultyColor = MaterialTheme.colorScheme.tertiary,
        onClick = { onGameModeSelected(GameMode.ENDLESS) })
    }


/**
 * Wiederverwendbare Spielmodus-Detail-Karte
 *
 * Stellt einen Spielmodus mit umfassenden Informationen dar.
 *
 * @param title Titel des Spielmodus
 * @param subtitle Untertitel/Slogan
 * @param description Kurzbeschreibung
 * @param details Liste von Features und Regeln
 * @param icon Emoji-Icon für den Modus
 * @param difficulty Schwierigkeitsgrad-Text
 * @param difficultyColor Farbe für Schwierigkeitsgrad
 * @param onClick Callback beim Klick auf die Karte
 */
@Composable
private fun GameModeDetailCard(
    title: String,
    subtitle: String,
    description: String,
    details: List<String>,
    icon: String,
    difficulty: String,
    difficultyColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header mit Icon und Titel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Spielen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beschreibung
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feature-Details
            details.forEach { detail ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Schwierigkeitsgrad und Spielen-Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = difficulty,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = difficultyColor.copy(alpha = 0.1f)
                    )
                )

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Spielen")
                }
            }
        }
    }
}

/**
 * Hilfe-Sektion mit Tipps und Erklärungen
 */
@Composable
private fun GameModeHelpSection() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💡 Tipps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Beginne mit dem klassischen Modus\n" +
                        "• Blitz-Modus trainiert schnelle Entscheidungen\n" +
                        "• Endlos-Modus ist perfekt für lange Sessions",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
