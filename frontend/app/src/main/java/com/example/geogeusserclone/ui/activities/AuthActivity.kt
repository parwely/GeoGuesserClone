package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateToMenu: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Navigation bei erfolgreichem Login/Registrierung
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginMode) "Anmelden" else "Registrieren",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Username field (nur bei Registrierung)
                    if (!isLoginMode) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Benutzername") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-Mail") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Passwort") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isLoginMode) {
                                    authViewModel.login(email, password)
                                } else {
                                    authViewModel.register(username, email, password)
                                }
                            }
                        )
                    )

                    // Error message
                    authState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit button
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                authViewModel.login(email, password)
                            } else {
                                authViewModel.register(username, email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !authState.isLoading
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isLoginMode) "Anmelden" else "Registrieren")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle button
                    TextButton(
                        onClick = {
                            isLoginMode = !isLoginMode
                            authViewModel.clearError()
                        }
                    ) {
                        Text(
                            if (isLoginMode) {
                                "Noch kein Konto? Registrieren"
                            } else {
                                "Bereits ein Konto? Anmelden"
                            }
                        )
                    }
                }
            }
        }
    }
}