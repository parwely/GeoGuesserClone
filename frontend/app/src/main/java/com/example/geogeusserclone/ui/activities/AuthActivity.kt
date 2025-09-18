/**
 * AuthActivity.kt
 *
 * Diese Datei enthält die Authentifizierungs-Activity für die GeoGuess-App.
 * Sie ist verantwortlich für das Login und die Registrierung neuer Benutzer.
 *
 * Architektur-Integration:
 * - Activity-Layer: Stellt die UI für Authentifizierung bereit
 * - ViewModel-Integration: Nutzt AuthViewModel für Geschäftslogik
 * - Navigation: Leitet nach erfolgreichem Login zur MenuActivity weiter
 * - Dependency Injection: Hilt-Annotationen für automatische Abhängigkeitsinjektion
 */
package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGuessTheme
import com.example.geogeusserclone.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Haupt-Activity für Benutzerauthentifizierung
 *
 * Diese Activity behandelt sowohl Login als auch Registrierung in einer einzigen UI.
 * Sie verwendet Jetpack Compose für die UI und Hilt für Dependency Injection.
 *
 * Features:
 * - Umschaltung zwischen Login- und Registrierungs-Modi
 * - Form-Validierung
 * - Fehlerbehandlung
 * - Automatische Navigation nach erfolgreichem Login
 */
@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    /**
     * Initialisiert die Activity und setzt die Compose-UI
     *
     * Konfiguriert Edge-to-Edge-Display und definiert Navigation-Callbacks
     * für erfolgreiche Authentifizierung.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGuessTheme {
                AuthScreen(
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
 * Haupt-Composable für den Authentifizierungs-Bildschirm
 *
 * Verwaltet den Zustand zwischen Login- und Registrierungs-Modi und
 * behandelt die automatische Navigation bei erfolgreichem Login.
 *
 * @param onNavigateToMenu Callback für Navigation zum Hauptmenü
 * @param authViewModel ViewModel für Authentifizierungs-Geschäftslogik
 */
@Composable
fun AuthScreen(
    onNavigateToMenu: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }

    // Automatische Navigation bei erfolgreichem Login
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onNavigateToMenu()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App-Logo und Titel
            Text(
                text = "🌍",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GeoGuess",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Dynamische Form-Anzeige basierend auf Modus
            if (isLoginMode) {
                LoginForm(
                    onLogin = { email, password ->
                        authViewModel.login(email, password)
                    },
                    onSwitchToRegister = { isLoginMode = false },
                    isLoading = authState.isLoading
                )
            } else {
                RegisterForm(
                    onRegister = { username, email, password ->
                        authViewModel.register(username, email, password)
                    },
                    onSwitchToLogin = { isLoginMode = true },
                    isLoading = authState.isLoading
                )
            }

            // Fehleranzeige für Authentifizierungsfehler
            authState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
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

/**
 * Login-Formular Composable
 *
 * Stellt ein Formular für Benutzeranmeldung bereit mit E-Mail und Passwort-Feldern.
 * Enthält Validierung und Sichtbarkeits-Toggle für Passwörter.
 *
 * @param onLogin Callback für Login-Versuch mit E-Mail und Passwort
 * @param onSwitchToRegister Callback zum Wechseln zur Registrierung
 * @param isLoading Zeigt an, ob gerade ein Login-Versuch läuft
 */
@Composable
fun LoginForm(
    onLogin: (String, String) -> Unit,
    onSwitchToRegister: () -> Unit,
    isLoading: Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Anmelden",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // E-Mail Eingabefeld mit Validierung
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Passwort-Eingabefeld mit Sichtbarkeits-Toggle
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.AccountBox else Icons.Default.Lock,
                        contentDescription = if (passwordVisible) "Passwort verbergen" else "Passwort anzeigen"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login-Button mit Loading-Indikator
        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Anmelden")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link zur Registrierung
        TextButton(
            onClick = onSwitchToRegister,
            enabled = !isLoading
        ) {
            Text("Noch kein Konto? Registrieren")
        }
    }
}

/**
 * Registrierungs-Formular Composable
 *
 * Stellt ein Formular für Benutzerregistrierung bereit mit Benutzername, E-Mail und Passwort.
 * Enthält Passwort-Validierung (mindestens 6 Zeichen) und Sichtbarkeits-Toggle.
 *
 * @param onRegister Callback für Registrierungs-Versuch mit allen erforderlichen Daten
 * @param onSwitchToLogin Callback zum Wechseln zum Login
 * @param isLoading Zeigt an, ob gerade ein Registrierungs-Versuch läuft
 */
@Composable
fun RegisterForm(
    onRegister: (String, String, String) -> Unit,
    onSwitchToLogin: () -> Unit,
    isLoading: Boolean
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Registrieren",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Benutzername-Eingabefeld
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Benutzername") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // E-Mail Eingabefeld
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Passwort-Eingabefeld mit Längen-Validierung
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort (min. 6 Zeichen)") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Face else Icons.Default.Lock,
                        contentDescription = if (passwordVisible) "Passwort verbergen" else "Passwort anzeigen"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Registrierungs-Button mit Formular-Validierung
        Button(
            onClick = { onRegister(username, email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && username.isNotBlank() && email.isNotBlank() && password.length >= 6
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Registrieren")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link zum Login
        TextButton(
            onClick = onSwitchToLogin,
            enabled = !isLoading
        ) {
            Text("Bereits ein Konto? Anmelden")
        }
    }
}
