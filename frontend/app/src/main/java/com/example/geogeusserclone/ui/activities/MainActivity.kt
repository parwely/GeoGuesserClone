/**
 * MainActivity.kt
 *
 * Diese Datei enthält die Haupteinstiegs-Activity der GeoGuess-App.
 * Sie fungiert als Router und entscheidet basierend auf dem Login-Status,
 * wohin der Benutzer weitergeleitet wird.
 *
 * Architektur-Integration:
 * - Entry Point: Erste Activity, die beim App-Start geladen wird
 * - Router-Funktion: Leitet basierend auf Authentifizierungsstatus weiter
 * - Session-Management: Prüft gespeicherte Login-Sessions
 * - Navigation-Hub: Zentrale Stelle für initiale App-Navigation
 */
package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGuessTheme
import com.example.geogeusserclone.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Haupt-Entry-Point der Anwendung
 *
 * Diese Activity wird beim App-Start geladen und entscheidet basierend auf dem
 * Authentifizierungsstatus des Benutzers, zu welcher Activity weitergeleitet wird.
 *
 * Navigations-Flow:
 * - Eingeloggt: Direkt zum MenuActivity
 * - Nicht eingeloggt: Zeigt Welcome-Screen oder leitet zu AuthActivity weiter
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Initialisiert die Haupt-Activity
     *
     * Setzt das Compose-Theme und definiert Navigation-Callbacks für
     * Authentifizierung und Hauptmenü.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGuessTheme {
                MainScreen(
                    onNavigateToAuth = {
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    },
                    onNavigateToMenu = {
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * Haupt-Bildschirm Composable mit Router-Logik
 *
 * Entscheidet basierend auf dem Authentifizierungsstatus, welcher Bildschirm
 * angezeigt werden soll. Führt automatische Navigation durch.
 *
 * @param onNavigateToAuth Callback für Navigation zur Authentifizierung
 * @param onNavigateToMenu Callback für Navigation zum Hauptmenü
 * @param authViewModel ViewModel für Authentifizierungs-Zustand
 */
@Composable
fun MainScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMenu: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()

    // Automatische Navigation basierend auf Login-Status
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onNavigateToMenu()
        }
    }

    // Loading-Zustand während Authentifizierungsprüfung
    if (authState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Welcome-Screen für nicht eingeloggte Benutzer
    if (!authState.isLoggedIn) {
        WelcomeScreen(onGetStarted = onNavigateToAuth)
    }
}

/**
 * Welcome-Screen für neue Benutzer
 *
 * Zeigt eine einladende Übersicht der App-Features und einen Call-to-Action
 * Button für den Einstieg in die Authentifizierung.
 *
 * @param onGetStarted Callback für den Start der Benutzer-Registrierung/Login
 */
@Composable
private fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App-Logo
        Text(
            text = "🌍",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App-Titel
        Text(
            text = "GeoGuess",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App-Beschreibung
        Text(
            text = "Erkunde die Welt und teste dein geografisches Wissen!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Haupt-Call-to-Action Button
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Los geht's!",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Feature-Übersicht
        FeatureOverviewCard()
    }
}

/**
 * Feature-Übersicht Card
 *
 * Zeigt die wichtigsten App-Features in einer übersichtlichen Card-Darstellung.
 * Dient als Marketing-Element für neue Benutzer.
 */
@Composable
private fun FeatureOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "✨ Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Feature-Liste
            FeatureItem("🎯", "Einzelspieler Modus")
            FeatureItem("🗺️", "Interaktive Weltkarte")
            FeatureItem("📱", "Offline-Modus verfügbar")
            FeatureItem("📊", "Detaillierte Statistiken")
        }
    }
}

/**
 * Einzelnes Feature-List-Item
 *
 * Stellt ein einzelnes Feature mit Icon und Beschreibung dar.
 *
 * @param icon Emoji-Icon für das Feature
 * @param text Beschreibungstext des Features
 */
@Composable
private fun FeatureItem(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
