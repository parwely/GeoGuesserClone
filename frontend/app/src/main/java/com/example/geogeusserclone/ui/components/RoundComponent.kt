package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreetViewComponent(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    // Für später: WebView oder Street View API Integration
    Card(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Street View Komponente\n(Für zukünftige Implementierung)",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}