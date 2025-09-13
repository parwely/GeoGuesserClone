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
import java.util.Locale

@Composable
fun LocationImageScreen(
    location: LocationEntity,
    timeRemaining: Long,
    onShowMap: () -> Unit,
    onPan: (Float) -> Unit = {},
    streetViewAvailable: Boolean = false // NEW: pass from UI state
) {
    val context = LocalContext.current

    // Automatisches Memory Management
    MemoryManager.AutoMemoryManagement(context)

    // Smart Image URL Selection mit Fallback-Logik
    val isValidStreetViewUrl = remember(location.imageUrl) {
        // Accept any Google Maps Street View URL with a key
        location.imageUrl.startsWith("https://maps.googleapis.com/maps/api/streetview") &&
        Regex("key=AIza[\\w-]+", RegexOption.IGNORE_CASE).containsMatchIn(location.imageUrl)
    }
    val effectiveImageUrl = location.imageUrl

    // Tracking der aktuellen Position für erweiterte Navigation
    var currentLatitude by remember { mutableDoubleStateOf(location.latitude) }
    var currentLongitude by remember { mutableDoubleStateOf(location.longitude) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Smart Image Display mit robustem Fallback
        if (streetViewAvailable && effectiveImageUrl.isNotBlank()) {
            println("LocationImageScreen: Zeige Interactive Street View an: $effectiveImageUrl")

            // Debug: Check Coil painter state for image loading errors
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(effectiveImageUrl)
                    .crossfade(300)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()
            )
            println("LocationImageScreen: Coil painter state: ${painter.state}")

            InteractiveStreetView(
                imageUrl = effectiveImageUrl,
                modifier = Modifier.fillMaxSize(),
                onPan = onPan,
                onLocationChange = { lat, lng ->
                    currentLatitude = lat
                    currentLongitude = lng
                    println("LocationImageScreen: Neue Position: $lat, $lng")
                }
            )
        } else if (effectiveImageUrl.isNotBlank()) {
            // Statisches Fallback-Bild mit verbesserter UI
            val fallbackUrl = getImageUrlForCity(location.city ?: "Unknown")
            println("LocationImageScreen: Zeige Fallback-Bild an: $fallbackUrl")

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fallbackUrl)
                    .crossfade(300)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Location: ${location.city}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Fallback Overlay Indicator
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Beispielbild",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else {
            // Emergency Fallback - keine URL verfügbar
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
                        text = "Bild nicht verfügbar",
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
                    text = "Pos: ${String.format(Locale.US, "%.4f", currentLatitude)}, ${String.format(Locale.US, "%.4f", currentLongitude)}",
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
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
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

// Hilfsfunktion für Fallback-Bilder
private fun getImageUrlForCity(city: String): String {
    val imageMap = mapOf(
        "Paris" to "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop",
        "London" to "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop",
        "New York" to "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop",
        "Berlin" to "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop",
        "Tokyo" to "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop",
        "Sydney" to "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop",
        "Rome" to "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop",
        "Barcelona" to "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop",
        "Moscow" to "https://images.unsplash.com/photo-1547036967-23d11aacaee0?w=800&h=600&fit=crop",
        "Mexico City" to "https://images.unsplash.com/photo-1512813195452-83104b651e5f?w=800&h=600&fit=crop"
    )

    return imageMap[city.split(" ").first()]
        ?: "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
}
