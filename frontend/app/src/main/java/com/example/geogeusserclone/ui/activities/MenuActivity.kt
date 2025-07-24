package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.AuthViewModel
import com.example.geogeusserclone.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
                MenuScreen(
                    onNavigateToGame = {
                        startActivity(Intent(this, GameActivity::class.java))
                    },
                    onNavigateToAuth = {
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun MenuScreen(
    onNavigateToGame: () -> Unit,
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()

    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn) {
            onNavigateToAuth()
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

            authState.currentUser?.let { user ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Willkommen, ${user.username}!",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNavigateToGame,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Einzelspieler")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* TODO: Mehrspieler */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mehrspieler")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* TODO: Battle Royale */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Battle Royale")
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = {
                    authViewModel.logout()
                    onNavigateToAuth()
                }
            ) {
                Text("Abmelden")
            }
        }
    }
}
