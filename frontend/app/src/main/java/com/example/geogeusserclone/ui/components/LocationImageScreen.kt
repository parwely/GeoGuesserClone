package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.utils.MemoryManager

@Composable
fun LocationImageScreen(
    location: LocationEntity,
    timeRemaining: Long,
    onShowMap: () -> Unit,
    onPan: (Float) -> Unit // Hinzugefügt für die Interaktion
) {
    val context = LocalContext.current
    var rotation by remember { mutableFloatStateOf(0f) }

    // Automatisches Memory Management
    MemoryManager.AutoMemoryManagement(context)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Ersetze statisches Bild durch interaktive Street-View-Komponente
        if (location.imageUrl.isNotBlank()) {
            InteractiveStreetView(
                imageUrl = location.imageUrl,
                modifier = Modifier.fillMaxSize(),
                onPan = { delta ->
                    rotation += delta
                    onPan(delta)
                }
            )
        } else {
            // Behalte den Lade-Indikator für leere URLs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lade Location...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Gradient Overlay für bessere Lesbarkeit
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Time Remaining Indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = formatTime(timeRemaining),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (timeRemaining < 30000) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Location Info (nur wenn verfügbar)
        if (!location.city.isNullOrBlank() || !location.country.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    location.city?.let { city ->
                        Text(
                            text = "Hinweis: $city",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    location.country?.let { country ->
                        Text(
                            text = country,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Difficulty Indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = getDifficultyColor(location.difficulty).copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "Schwierigkeit: ${getDifficultyText(location.difficulty)}",
                modifier = Modifier.padding(8.dp, 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Bottom Action Button
        FloatingActionButton(
            onClick = onShowMap,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = "Karte öffnen",
                tint = Color.White
            )
        }

        // Instruction Text für neue Spieler
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "🗺️ Tippe auf die Karte, um deinen Tipp abzugeben",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun getDifficultyColor(difficulty: Int): Color {
    return when (difficulty) {
        1 -> Color(0xFF4CAF50) // Grün - Einfach
        2 -> Color(0xFFFFC107) // Gelb - Mittel
        3 -> Color(0xFFFF9800) // Orange - Schwer
        4 -> Color(0xFFFF5722) // Rot-Orange - Sehr schwer
        5 -> Color(0xFFF44336) // Rot - Extrem
        else -> Color(0xFF9E9E9E) // Grau - Unbekannt
    }
}

private fun getDifficultyText(difficulty: Int): String {
    return when (difficulty) {
        1 -> "Einfach"
        2 -> "Mittel"
        3 -> "Schwer"
        4 -> "Sehr schwer"
        5 -> "Extrem"
        else -> "Unbekannt"
    }
}
