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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.utils.MemoryManager

@Composable
fun LocationImageScreen(
    location: LocationEntity,
    timeRemaining: Long,
    onShowMap: () -> Unit,
    onPan: (Float) -> Unit = {}
) {
    val context = LocalContext.current

    // Automatisches Memory Management
    MemoryManager.AutoMemoryManagement(context)

    // Tracking der aktuellen Position für erweiterte Navigation
    var currentLatitude by remember { mutableDoubleStateOf(location.latitude) }
    var currentLongitude by remember { mutableDoubleStateOf(location.longitude) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Vollwertige Interactive Street View mit Navigation
        if (location.imageUrl.isNotBlank()) {
            println("LocationImageScreen: Zeige Interactive Street View an: ${location.imageUrl}")

            InteractiveStreetView(
                imageUrl = location.imageUrl,
                modifier = Modifier.fillMaxSize(),
                onPan = onPan,
                onLocationChange = { lat, lng ->
                    currentLatitude = lat
                    currentLongitude = lng
                    println("LocationImageScreen: Neue Position: $lat, $lng")
                }
            )
        } else {
            // Fallback für leere URLs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Keine Street View verfügbar",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Location: ${location.city ?: "Unbekannt"}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Gradient Overlay für bessere UI-Lesbarkeit (nur minimal)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f)
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

        // Current Position Info (für Debug und erweiterte Navigation)
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = location.city ?: "Unbekannter Ort",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                location.country?.let { country ->
                    Text(
                        text = country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Zeige aktuelle Koordinaten (hilfreich für Navigation)
                Text(
                    text = "Pos: ${String.format("%.4f", currentLatitude)}, ${String.format("%.4f", currentLongitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Difficulty Indicator
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = getDifficultyColor(location.difficulty).copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = getDifficultyText(location.difficulty),
                modifier = Modifier.padding(8.dp, 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Guess Button - größer und prominent
        Button(
            onClick = onShowMap,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(width = 140.dp, height = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RATEN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Navigation Hint (einblendbar)
        Card(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "🎮 Navigation",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Ziehen zum Umschauen",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Doppeltipp zum Bewegen",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Pinch zum Zoomen",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Einfachtipp für Controls",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun getDifficultyColor(difficulty: Int): Color {
    return when (difficulty) {
        1 -> Color(0xFF4CAF50) // Grün
        2 -> Color(0xFF8BC34A) // Hellgrün
        3 -> Color(0xFFFFEB3B) // Gelb
        4 -> Color(0xFFFF9800) // Orange
        5 -> Color(0xFFFF5722) // Rot
        else -> Color(0xFF9E9E9E) // Grau
    }
}

private fun getDifficultyText(difficulty: Int): String {
    return when (difficulty) {
        1 -> "Sehr Einfach"
        2 -> "Einfach"
        3 -> "Mittel"
        4 -> "Schwer"
        5 -> "Sehr Schwer"
        else -> "Unbekannt"
    }
}
