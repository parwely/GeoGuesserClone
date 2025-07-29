package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
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

@Composable
fun AuthScreen(
    onNavigateToMenu: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }

    // Navigation bei erfolgreichem Login
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
            Text(
                text = "🌍",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GeoGuessr Clone",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

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

            // Error Anzeige
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

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "👁️" else "🔒",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        TextButton(
            onClick = onSwitchToRegister,
            enabled = !isLoading
        ) {
            Text("Noch kein Konto? Registrieren")
        }
    }
}

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

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Benutzername") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort (min. 6 Zeichen)") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "👁️" else "🔒",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        TextButton(
            onClick = onSwitchToLogin,
            enabled = !isLoading
        ) {
            Text("Bereits ein Konto? Anmelden")
        }
    }
}
