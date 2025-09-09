package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionLobbyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
                SessionLobbyScreen(
                    onBack = { finish() },
                    onStartGame = {
                        // TODO: Start multiplayer game logic
                    }
                )
            }
        }
    }
}

@Composable
fun SessionLobbyScreen(
    onBack: () -> Unit,
    onStartGame: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val sessionState by sessionViewModel.state.collectAsState()
    val error by sessionViewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer Lobby") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Person, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Session Code: ${sessionState.sessionId}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Status: ${sessionState.status}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Players:",
                style = MaterialTheme.typography.titleMedium
            )
            sessionState.players.forEach { player ->
                Text(text = player.username)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { sessionViewModel.createSession("battle-royale", mapOf()) }) {
                Text("Create Session")
            }
            Button(onClick = { /* TODO: Implement join session dialog */ }) {
                Text("Join Session")
            }
            Button(onClick = onStartGame, enabled = sessionState.players.size > 1) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Game")
            }
            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
