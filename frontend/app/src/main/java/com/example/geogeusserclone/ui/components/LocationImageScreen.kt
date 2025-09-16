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
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.utils.MemoryManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LocationImageScreen(
    location: LocationEntity,
    timeRemaining: Long,
    onShowMap: () -> Unit,
    onPan: (Float) -> Unit = {},
    streetViewAvailable: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Automatisches Memory Management
    MemoryManager.AutoMemoryManagement(context)

    // Enhanced Street View Detection mit mehr Präzision
    val streetViewMode = remember(location.imageUrl) {
        when {
            // Google Maps Embed URL (Interaktiv)
            location.imageUrl.contains("google.com/maps/embed/v1/streetview") ||
            location.imageUrl.contains("navigation=1") -> StreetViewMode.Interactive

            // Statische Google Street View API
            location.imageUrl.startsWith("https://maps.googleapis.com/maps/api/streetview") &&
            Regex("key=AIza[\\w-]+", RegexOption.IGNORE_CASE).containsMatchIn(location.imageUrl) -> StreetViewMode.Static

            // Fallback Bilder
            location.imageUrl.contains("unsplash.com") ||
            location.imageUrl.contains("images.") -> StreetViewMode.Fallback

            // Kein Bild verfügbar
            location.imageUrl.isBlank() -> StreetViewMode.Empty

            else -> StreetViewMode.Unknown
        }
    }

    // Navigation State für interaktive Street View
    var currentHeading by remember { mutableIntStateOf(0) }
    var currentLatitude by remember { mutableDoubleStateOf(location.latitude) }
    var currentLongitude by remember { mutableDoubleStateOf(location.longitude) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (streetViewMode) {
            StreetViewMode.Interactive -> {
                println("LocationImageScreen: Zeige Enhanced Interactive Street View an")

                // NEUE: Enhanced Street View Komponente
                EnhancedStreetViewComponent(
                    locationId = location.id.toIntOrNull() ?: 1,
                    modifier = Modifier.fillMaxSize(),
                    quality = "high",
                    enableNavigation = true,
                    enableControls = true,
                    onNavigationRequest = { direction, heading, stepSize ->
                        // Handle navigation requests
                        scope.launch {
                            println("Navigation Request: $direction, heading: $heading°, step: ${stepSize}m")
                            currentHeading = heading
                            // TODO: Implement actual navigation via repository
                        }
                    },
                    onError = { error ->
                        println("Enhanced Street View Error: $error")
                    }
                )
            }

            StreetViewMode.Static -> {
                println("LocationImageScreen: Zeige bestehende Interactive Street View (statisch) an")

                InteractiveStreetView(
                    imageUrl = location.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    onPan = onPan,
                    onLocationChange = { lat, lng ->
                        currentLatitude = lat
                        currentLongitude = lng
                        println("LocationImageScreen: Position geändert: $lat, $lng")
                    }
                )
            }

            StreetViewMode.Fallback -> {
                println("LocationImageScreen: Zeige Fallback-Bild an: ${location.imageUrl}")

                FallbackImageView(
                    imageUrl = location.imageUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            StreetViewMode.Empty -> {
                EmptyLocationView(
                    locationName = location.city ?: "Unbekannt",
                    country = location.country,
                    modifier = Modifier.fillMaxSize()
                )
            }

            StreetViewMode.Unknown -> {
                UnknownFormatView(
                    imageUrl = location.imageUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Gradient Overlay für bessere UI-Lesbarkeit (nur bei non-interactive)
        if (streetViewMode != StreetViewMode.Interactive) {
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
        }

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

        // Enhanced Location Info Card
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

                // Street View Type Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (emoji, text, color) = when (streetViewMode) {
                        StreetViewMode.Interactive -> Triple("🎮", "Interactive", MaterialTheme.colorScheme.primary)
                        StreetViewMode.Static -> Triple("📸", "Street View", MaterialTheme.colorScheme.secondary)
                        StreetViewMode.Fallback -> Triple("🖼️", "Beispielbild", MaterialTheme.colorScheme.outline)
                        StreetViewMode.Empty -> Triple("❌", "Kein Bild", MaterialTheme.colorScheme.error)
                        StreetViewMode.Unknown -> Triple("❓", "Unbekannt", MaterialTheme.colorScheme.outline)
                    }

                    Text(
                        text = "$emoji $text",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Current coordinates (hilfreich für Debug)
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

        // Enhanced Guess Button
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

        // Enhanced Navigation Hint für interaktive Street View
        if (streetViewMode == StreetViewMode.Interactive) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🎮 Interaktive Navigation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "• Bewege dich durch Street View",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "• Nutze die Navigations-Controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "• Tippe für Controls ein/aus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else if (streetViewMode == StreetViewMode.Static) {
            // Bestehende Navigation Hints für statische Street View
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
                        text = "🎮 Simulation",
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
                }
            }
        }
    }
}

// Enum für verschiedene Street View-Modi
enum class StreetViewMode {
    Interactive,    // Google Maps Embed mit Navigation
    Static,        // Google Street View Static API
    Fallback,      // Fallback-Bilder (Unsplash etc.)
    Empty,         // Keine URL verfügbar
    Unknown        // Unbekanntes Format
}

@Composable
private fun FallbackImageView(
    imageUrl: String,
    locationName: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(300)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = "Fallback für $locationName",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Fallback Indicator
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
                Text("🖼️", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Beispielbild",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyLocationView(
    locationName: String,
    country: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                text = "Location: $locationName",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            country?.let {
                Text(
                    text = "Land: $it",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UnknownFormatView(
    imageUrl: String,
    locationName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "❓",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unbekanntes Bildformat",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "URL: ${imageUrl.take(50)}...",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
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
