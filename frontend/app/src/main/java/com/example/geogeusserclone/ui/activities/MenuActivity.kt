/**
 * MenuActivity.kt
 *
 * Diese Datei enthält das Hauptmenü der GeoGuess-App.
 * Sie stellt das zentrale Hub für alle Spielmodi und Benutzeroptionen dar.
 *
 * Architektur-Integration:
 * - Navigation-Hub: Zentrale Anlaufstelle nach dem Login
 * - Game Mode Selection: Auswahl zwischen verschiedenen Spielmodi
 * - User Dashboard: Anzeige von Benutzerstatistiken und Fortschritt
 * - Session Management: Logout-Funktionalität
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGuessTheme
import com.example.geogeusserclone.viewmodels.AuthViewModel
import com.example.geogeusserclone.viewmodels.AuthState // KORRIGIERT: Richtiger Import
import com.example.geogeusserclone.data.models.GameMode
import com.example.geogeusserclone.data.database.entities.UserEntity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hauptmenü-Activity der Anwendung
 *
 * Diese Activity fungiert als zentraler Hub nach dem Login und bietet
 * Zugang zu allen Spielmodi, Benutzerstatistiken und App-Einstellungen.
 *
 * Features:
 * - Spielmodus-Auswahl (Klassisch, Blitz, Endlos)
 * - Benutzer-Dashboard mit Statistiken
 * - Schnellstart-Funktionalität
 * - Logout-Option
 */
@AndroidEntryPoint
class MenuActivity : ComponentActivity() {

    /**
     * Initialisiert das Hauptmenü
     *
     * Setzt das Compose-Theme und definiert Navigation-Callbacks für
     * Spielstart und Logout-Funktionalität.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGuessTheme {
                MenuScreen(
                    onStartGame = { gameMode ->
                        val intent = when (gameMode) {
                            GameMode.CLASSIC -> Intent(this, GameActivity::class.java).apply {
                                putExtra("GAME_MODE", "CLASSIC")
                            }
                            else -> Intent(this, GameModeSelectionActivity::class.java)
                        }
                        startActivity(intent)
                    },
                    onLogout = {
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * Hauptmenü-Bildschirm Composable
 *
 * Stellt das vollständige Hauptmenü dar mit Benutzer-Dashboard,
 * Spielmodus-Auswahl und Schnellstart-Optionen.
 *
 * @param onStartGame Callback für Spielstart mit ausgewähltem Modus
 * @param onLogout Callback für Benutzer-Abmeldung
 * @param authViewModel ViewModel für Benutzer-Authentifizierung
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onStartGame: (GameMode) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            // App-Top-Bar mit Titel und Logout-Button
            TopAppBar(
                title = { Text("GeoGuess") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Abmelden")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Benutzer-Welcome-Card mit Statistiken
                UserWelcomeCard(authState = authState)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Schnellstart-Button für klassischen Modus
            item {
                QuickStartButton(onStartGame = onStartGame)
            }

            // Spielmodus-Sektion
            item {
                GameModeSection(onStartGame = onStartGame)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Informations-Sektion
            item {
                GameModeInfoCard()
            }
        }
    }
}

/**
 * Benutzer-Willkommen-Card
 *
 * Zeigt eine personalisierte Begrüßung mit Benutzerstatistiken
 * wie Gesamtpunkte, gespielte Spiele und Bestleistung.
 *
 * @param authState Aktueller Authentifizierungsstatus mit Benutzerdaten
 */
@Composable
private fun UserWelcomeCard(authState: AuthState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App-Logo
            Text(
                text = "🌍",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Personalisierte Begrüßung
            authState.currentUser?.let { user ->
                Text(
                    text = "Willkommen, ${user.username}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Benutzerstatistiken in drei Spalten
                UserStatsRow(user = user)
            }
        }
    }
}

/**
 * Benutzerstatistiken-Zeile
 *
 * Zeigt die wichtigsten Benutzerstatistiken in einer dreispaltigen Anordnung.
 *
 * @param user Benutzerdaten mit Statistiken
 */
@Composable
private fun UserStatsRow(user: UserEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Gesamtpunkte
        StatItem(
            value = user.totalScore.toString(),
            label = "Punkte",
            color = MaterialTheme.colorScheme.primary
        )

        // Gespielte Spiele
        StatItem(
            value = user.gamesPlayed.toString(),
            label = "Spiele",
            color = MaterialTheme.colorScheme.primary
        )

        // Bestleistung
        StatItem(
            value = user.bestScore.toString(),
            label = "Bestzeit",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Einzelne Statistik-Anzeige
 *
 * Stellt eine einzelne Statistik mit Wert, Label und Farbe dar.
 *
 * @param value Anzuzeigender Wert
 * @param label Beschriftung der Statistik
 * @param color Farbe für die Wertanzeige
 */
@Composable
private fun StatItem(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Schnellstart-Button
 *
 * Großer prominenter Button für den sofortigen Start des klassischen Spielmodus.
 *
 * @param onStartGame Callback für Spielstart
 */
@Composable
private fun QuickStartButton(onStartGame: (GameMode) -> Unit) {
    Button(
        onClick = { onStartGame(GameMode.CLASSIC) },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Schnellstart (Klassisch)",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Spielmodus-Sektion
 *
 * Zeigt alle verfügbaren Spielmodi als auswählbare Cards an.
 *
 * @param onStartGame Callback für Spielstart mit ausgewähltem Modus
 */
@Composable
private fun GameModeSection(onStartGame: (GameMode) -> Unit) {
    Text(
        text = "Spielmodi",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Klassischer Modus
    GameModeCard(
        title = "Klassischer Modus",
        description = "Das traditionelle GeoGuessr mit 5 Runden",
        icon = "🎯",
        onClick = { onStartGame(GameMode.CLASSIC) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Blitz-Modus
    GameModeCard(
        title = "Blitz-Modus",
        description = "Schnelle Runden mit Zeitdruck und keiner Bewegung",
        icon = "⚡",
        onClick = { onStartGame(GameMode.BLITZ) },
        enabled = true,
        isNew = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Endlos-Modus
    GameModeCard(
        title = "Endlos-Modus",
        description = "Spiele so lange du willst mit Streak-System",
        icon = "∞",
        onClick = { onStartGame(GameMode.ENDLESS) },
        enabled = true,
        isNew = true
    )
}

/**
 * Einzelne Spielmodus-Card
 *
 * Stellt einen Spielmodus mit Icon, Titel, Beschreibung und Verfügbarkeitsstatus dar.
 *
 * @param title Titel des Spielmodus
 * @param description Beschreibung des Spielmodus
 * @param icon Emoji-Icon für den Spielmodus
 * @param onClick Callback beim Klick auf die Card
 * @param enabled Ob der Spielmodus verfügbar ist
 * @param isNew Ob der Spielmodus als "neu" markiert werden soll
 */
@Composable
private fun GameModeCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isNew: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else { {} },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spielmodus-Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Titel und Beschreibung
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // Status-Anzeige
                if (!enabled) {
                    Text(
                        text = "Bald verfügbar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isNew) {
                    Text(
                        text = "✨ Neu verfügbar!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Play-Icon für verfügbare Modi
            if (enabled) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Spielmodus-Informations-Card
 *
 * Stellt eine Übersicht aller Spielmodi mit ihren Eigenschaften dar.
 * Dient als Hilfe und Erklärung für neue Benutzer.
 */
@Composable
private fun GameModeInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Info-Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spielmodi erklärt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spielmodus-Erklärungen
            Text(
                text = "���� Klassisch: 5 Runden, keine Zeitbegrenzung, freie Navigation\n" +
                        "⚡ Blitz: 10 Runden, 30s pro Runde, keine Navigation\n" +
                        "∞ Endlos: Unbegrenzte Runden mit Streak-Bonus-System",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
