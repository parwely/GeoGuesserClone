package com.example.geogeusserclone.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geogeusserclone.ui.theme.GeoGeusserCloneTheme
import com.example.geogeusserclone.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeoGeusserCloneTheme {
                MainScreen()
            }
        }
    }

    @Composable
    private fun MainScreen(
        authViewModel: AuthViewModel = hiltViewModel()
    ) {
        val authState by authViewModel.state.collectAsState()
        var hasNavigated by remember { mutableStateOf(false) }

        LaunchedEffect(authState.isLoggedIn, hasNavigated) {
            if (!hasNavigated) {
                if (authState.isLoggedIn) {
                    startActivity(Intent(this@MainActivity, MenuActivity::class.java))
                } else {
                    startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                }
                hasNavigated = true
                finish()
            }
        }

        // Ladebildschirm während der Authentifizierungsprüfung
        // Keine UI hier, da sofort navigiert wird
    }
}