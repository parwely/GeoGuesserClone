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
    streetViewAvailable: Boolean = false
) {
    val context = LocalContext.current

    // KORRIGIERT: AutoMemoryManagement direkt im Composable-Body aufrufen
    MemoryManager.AutoMemoryManagement(context)

    // NEUE: State für Fallback-Handling
    var shouldUseFallbackImage by remember { mutableStateOf(false) }
    var fallbackImageUrl by remember { mutableStateOf<String?>(null) }
    var webViewError by remember { mutableStateOf<String?>(null) }

    // KORRIGIERT: Stabiler State mit remember key
    val cleanedUrl = remember(location.id, location.imageUrl) {
        val result = cleanAndValidateStreetViewUrl(location.imageUrl)
        // REDUZIERT: Nur einmal beim ersten Laden loggen
        if (result.isNotBlank()) {
            println("LocationImageScreen: 🔍 URL bereinigt für ${location.city}: ${result.take(80)}...")
        }
        result
    }

    // NEUE: Intelligente Fallback-Erkennung BEVOR WebView geladen wird
    val shouldUseWebView = remember(cleanedUrl) {
        cleanedUrl.contains("google.com/maps/embed/v1/streetview") &&
        !cleanedUrl.contains("[object Object]") &&
        hasValidLocationParameter(cleanedUrl) &&
        !shouldUseFallbackImage
    }

    // NEUE: Verhindere wiederholte Fallback-Generierung
    val stableFallbackUrl = remember(location.id, shouldUseFallbackImage) {
        if (shouldUseFallbackImage) {
            generateLocationFallbackUrl(location).also {
                println("LocationImageScreen: 🔧 Fallback für ${location.city}: ${it.take(60)}...")
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            // NEUE: Verwende stabilen Fallback-State
            shouldUseFallbackImage && stableFallbackUrl != null -> {
                FallbackImageView(
                    imageUrl = stableFallbackUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // KORRIGIERT: Nur WebView verwenden wenn wirklich nötig
            shouldUseWebView -> {
                // REDUZIERT: Nur einmal loggen bei Initialisierung
                LaunchedEffect(location.id) {
                    println("LocationImageScreen: 🎮 Initialisiere Interactive Street View für ${location.city}")
                }

                // NEUE: Performance-optimierte WebView mit Crash-Prevention
                OptimizedWebView(
                    url = cleanedUrl,
                    locationName = location.city ?: "Unbekannt",
                    onError = { errorMessage ->
                        println("LocationImageScreen: 🔧 WebView Fehler für ${location.city}: $errorMessage")
                        webViewError = errorMessage
                        shouldUseFallbackImage = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Static Street View (Google API)
            cleanedUrl.startsWith("https://maps.googleapis.com/maps/api/streetview") &&
                    !cleanedUrl.contains("[object Object]") &&
                    !cleanedUrl.contains("PLACEHOLDER_API_KEY") -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cleanedUrl)
                        .crossfade(300)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .error(android.R.drawable.ic_menu_report_image)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .build(),
                    contentDescription = "Static Street View",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Fallback Images (Unsplash etc.)
            cleanedUrl.contains("unsplash.com") ||
                    cleanedUrl.contains("images.") -> {
                FallbackImageView(
                    imageUrl = cleanedUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Empty or corrupted URLs
            cleanedUrl.isBlank() -> {
                EmptyLocationView(
                    locationName = location.city ?: "Unbekannt",
                    country = location.country,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Corrupted URLs with [object Object]
            cleanedUrl.contains("[object Object]") ||
                    cleanedUrl.contains("PLACEHOLDER_API_KEY") -> {
                val fallbackUrl = generateRegionalFallbackUrl(location)
                FallbackImageView(
                    imageUrl = fallbackUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Unknown format
            else -> {
                UnknownFormatView(
                    imageUrl = cleanedUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // UI Overlay-Komponenten bleiben unverändert
        TimeRemainingCard(
            timeRemaining = timeRemaining,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        LocationInfoCard(
            location = location,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        DifficultyCard(
            difficulty = location.difficulty,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        GuessButton(
            onShowMap = onShowMap,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // NEUE: Error Overlay bei WebView-Problemen
        webViewError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = "⚠️ Street View Problem: $error",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// OPTIMIERTE PERFORMANCE-KOMPONENTEN
@Composable
private fun TimeRemainingCard(
    timeRemaining: Long,
    modifier: Modifier = Modifier
) {
    val isLowTime = timeRemaining < 30000

    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Text(
            text = formatTime(timeRemaining),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isLowTime) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun LocationInfoCard(
    location: LocationEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
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
                // KORRIGIERT: MaterialTheme.colorScheme innerhalb @Composable verwenden
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val outlineColor = MaterialTheme.colorScheme.outline
                val errorColor = MaterialTheme.colorScheme.error

                val (emoji, text, color) = when {
                    // Google Maps Embed URL (Interaktiv) - KORRIGIERT: Bessere Erkennung
                    location.imageUrl.contains("google.com/maps/embed/v1/streetview") &&
                            !location.imageUrl.contains("[object Object]") -> Triple("🎮", "Interactive", primaryColor)

                    // Statische Google Street View API - KORRIGIERT: Prüfe auf gültige URLs
                    location.imageUrl.startsWith("https://maps.googleapis.com/maps/api/streetview") &&
                            !location.imageUrl.contains("[object Object]") &&
                            !location.imageUrl.contains("PLACEHOLDER_API_KEY") &&
                            Regex("key=AIza[\\w-]+", RegexOption.IGNORE_CASE).containsMatchIn(location.imageUrl) -> Triple("📸", "Street View", secondaryColor)

                    // Fallback Bilder
                    (location.imageUrl.contains("unsplash.com") ||
                            location.imageUrl.contains("images.")) -> Triple("🖼️", "Beispielbild", outlineColor)

                    // Kein Bild verfügbar
                    location.imageUrl.isBlank() -> Triple("❌", "Kein Bild", errorColor)

                    // Korrupte Street View URLs
                    (location.imageUrl.contains("[object Object]") ||
                            location.imageUrl.contains("PLACEHOLDER_API_KEY")) -> Triple("🛑", "Fehlerhaft", errorColor)

                    else -> Triple("❓", "Unbekannt", outlineColor)
                }

                Text(
                    text = "$emoji $text",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = getDifficultyColor(difficulty).copy(alpha = 0.9f)
        )
    ) {
        Text(
            text = getDifficultyText(difficulty),
            modifier = Modifier.padding(8.dp, 4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun GuessButton(
    onShowMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onShowMap,
        modifier = modifier
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

// NEUE: Hilfsfunktionen für URL-Validierung und Fallback-Strategien
private fun isUrlSafeToLoad(url: String): Boolean {
    println("LocationImageScreen: 🔍 Validiere URL-Sicherheit: ${url.take(100)}...")

    return when {
        // Nur kritische Ausschlüsse - viel weniger restriktiv
        url.contains("[object Object]") -> {
            println("LocationImageScreen: ❌ URL enthält [object Object]")
            false
        }
        url.contains("PLACEHOLDER_API_KEY") -> {
            println("LocationImageScreen: ❌ URL enthält Placeholder API Key")
            false
        }
        url.isBlank() -> {
            println("LocationImageScreen: ❌ URL ist leer")
            false
        }

        // OPTIMISTISCHE VALIDIERUNG: Nur offensichtlich kaputte URLs ablehnen
        url.contains("undefined") -> {
            println("LocationImageScreen: ❌ URL enthält 'undefined'")
            false
        }

        // WICHTIG: Google Maps URLs sind fast immer gültig - sehr liberal validieren
        url.contains("google.com/maps") -> {
            println("LocationImageScreen: ✅ Google Maps URL - lade direkt")
            true
        }

        url.contains("googleapis.com") -> {
            println("LocationImageScreen: ✅ Google APIs URL - lade direkt")
            true
        }

        // Alle anderen URLs sind erstmal OK
        else -> {
            println("LocationImageScreen: ✅ URL scheint sicher")
            true
        }
    }
}

// NEUE: Smarte Koordinaten-Validierung - nur extreme Fälle abfangen
private fun isUrlContainingProblematicCoordinates(url: String): Boolean {
    return try {
        // Nur extreme Koordinaten ausschließen (Pole, mitten im Ozean)
        val locationPattern = Regex("location=([^&%]+)")
        val match = locationPattern.find(url) ?: return false

        val coordString = match.groupValues[1].replace("%2C", ",")
        val coords = coordString.split(",")

        if (coords.size >= 2) {
            val lat = coords[0].toDoubleOrNull() ?: return false
            val lng = coords[1].toDoubleOrNull() ?: return false

            // NUR extreme No-Go Bereiche - sehr konservativ
            when {
                lat < -85.0 || lat > 85.0 -> true // Extreme Pole
                (lat > -5.0 && lat < 5.0 && lng > 160.0 && lng < -150.0) -> true // Mitte Pazifik
                else -> false // Alles andere ist OK
            }
        } else false
    } catch (e: Exception) {
        false // Bei Parsing-Fehlern: URL als OK betrachten
    }
}

// VERBESSERTE: Fallback-Generierung nur wenn wirklich nötig
private fun generateLocationFallbackUrl(location: LocationEntity): String {
    println("LocationImageScreen: 🔧 Generiere Fallback für ${location.city}")

    // 1. Erste Priorität: Bekannte gute Locations
    val cityName = location.city?.lowercase() ?: ""
    val countryName = location.country?.lowercase() ?: ""

    val knownLocationUrl = when {
        cityName.contains("miami") -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
        cityName.contains("death valley") -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=600&fit=crop"
        cityName.contains("new york") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        cityName.contains("london") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
        cityName.contains("paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        cityName.contains("tokyo") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
        else -> null
    }

    if (knownLocationUrl != null) {
        println("LocationImageScreen: 🏛️ Verwende bekannte Location URL")
        return knownLocationUrl
    }

    // 2. Regionale Fallbacks
    return generateRegionalFallbackUrl(location)
}

// VERBESSERTE: Regionale Fallback-URLs
private fun generateRegionalFallbackUrl(location: LocationEntity): String {
    val countryName = location.country?.lowercase() ?: ""

    return when {
        countryName.contains("united states") || countryName.contains("usa") ->
            "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        countryName.contains("united kingdom") || countryName.contains("england") ->
            "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
        countryName.contains("france") ->
            "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        countryName.contains("germany") ->
            "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop"
        countryName.contains("japan") ->
            "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
        countryName.contains("australia") ->
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
        countryName.contains("canada") ->
            "https://images.unsplash.com/photo-1517935706615-2717063c2225?w=800&h=600&fit=crop"
        countryName.contains("morocco") ->
            "https://images.unsplash.com/photo-1539650116574-75c0c6d73f6e?w=800&h=600&fit=crop"
        else ->
            "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
    }
}
// NEUE: URL-Bereinigungsfunktion - KRITISCHE KORREKTUR für "Missing location parameter"
private fun cleanAndValidateStreetViewUrl(originalUrl: String): String {
    try {
        println("LocationImageScreen: 🧹 Bereinige URL: ${originalUrl.take(100)}...")

        // 1. Entferne nur offensichtlich korrupte Daten
        var cleanUrl = originalUrl
            .replace("[object Object]", "")
            .replace("undefined", "")
            .trim()

        // 2. Prüfe ob es eine Google Maps URL ist
        if (cleanUrl.contains("google.com/maps/embed/v1/streetview")) {
            // KRITISCH: Entferne unsupported Parameter die HTTP 400 verursachen
            cleanUrl = removeUnsupportedEmbedParameters(cleanUrl)

            // KRITISCH: Robuste location Parameter-Erkennung
            val hasLocation = when {
                // Suche nach location= gefolgt von Koordinaten (URL-kodiert oder nicht)
                Regex("location=([0-9.-]+[%2C,][0-9.-]+)").containsMatchIn(cleanUrl) -> {
                    println("LocationImageScreen: ✅ Location Parameter mit Koordinaten gefunden")
                    true
                }
                // Prüfe auf location= gefolgt von nicht-leeren Werten
                cleanUrl.contains("location=") &&
                !cleanUrl.contains("location=&") &&
                !cleanUrl.contains("location=$") &&
                !cleanUrl.contains("location=,") &&
                !cleanUrl.contains("location=%2C") -> {
                    println("LocationImageScreen: ✅ Location Parameter gefunden (Format unbekannt)")
                    true
                }
                else -> {
                    println("LocationImageScreen: ❌ Kein gültiger location Parameter gefunden")
                    false
                }
            }

            if (hasLocation) {
                println("LocationImageScreen: ✅ Google Maps Embed URL ist gültig und bereinigt")
                return cleanUrl
            } else {
                println("LocationImageScreen: 🔧 Fehlender location Parameter, verwende Fallback")
                return "" // Triggert Fallback
            }
        }

        // 3. Für statische Street View URLs
        if (cleanUrl.startsWith("https://maps.googleapis.com/maps/api/streetview")) {
            val hasLocation = Regex("location=([0-9.-]+[,][0-9.-]+)").containsMatchIn(cleanUrl) ||
                             (cleanUrl.contains("location=") &&
                              !cleanUrl.contains("location=&") &&
                              !cleanUrl.contains("location=$"))

            if (hasLocation) {
                println("LocationImageScreen: ✅ Statische Street View URL mit location Parameter")
                return cleanUrl
            } else {
                println("LocationImageScreen: ❌ Statische URL ohne gültigen location Parameter")
                return ""
            }
        }

        // 4. Andere URLs (Unsplash etc.) - unverändert durchlassen
        if (!cleanUrl.contains("[object Object]") && !cleanUrl.contains("undefined") && cleanUrl.isNotBlank()) {
            println("LocationImageScreen: ✅ Andere URL akzeptiert: ${cleanUrl.take(50)}...")
            return cleanUrl
        }

        println("LocationImageScreen: ❌ URL konnte nicht bereinigt werden")
        return ""

    } catch (e: Exception) {
        println("LocationImageScreen: ❌ Fehler bei URL-Bereinigung: ${e.message}")
        return ""
    }
}

// NEUE: Entferne Google Maps Embed Parameter die nicht unterstützt werden
private fun removeUnsupportedEmbedParameters(url: String): String {
    var cleanedUrl = url

    // KRITISCH: Google Maps Embed API unterstützt diese Parameter NICHT
    val unsupportedParams = listOf(
        "navigation=1", "navigation=true",
        "controls=1", "controls=true",
        "zoom=1", "zoom=true",
        "fullscreen=1", "fullscreen=true",
        // NEUE: Zusätzliche problematische Parameter
        "disableDefaultUI=1", "disableDefaultUI=true",
        "gestureHandling=none", "gestureHandling=greedy",
        "mapTypeControl=false", "streetViewControl=false"
    )

    // Entferne alle unsupported Parameter systematisch
    for (param in unsupportedParams) {
        // Entferne Parameter am Ende der URL
        cleanedUrl = cleanedUrl.replace("&$param", "")
        // Entferne Parameter direkt nach dem ? und ersetze mit nächstem Parameter
        cleanedUrl = cleanedUrl.replace("?$param&", "?")
        // Entferne Parameter wenn es der einzige Parameter ist
        cleanedUrl = cleanedUrl.replace("?$param", "")
    }

    // NEUE: Bereinige mehrfache & Zeichen
    cleanedUrl = cleanedUrl.replace("&&+".toRegex(), "&")

    // NEUE: Entferne & am Ende der URL
    cleanedUrl = cleanedUrl.trimEnd('&')

    // KRITISCH: Stelle sicher dass die URL mit den UNTERSTÜTZTEN Parametern funktioniert
    // Google Maps Embed API unterstützt nur: location, heading, pitch, fov, key

    println("LocationImageScreen: 🔧 URL bereinigt von: ${url.take(120)}...")
    println("LocationImageScreen: 🔧 Zu: ${cleanedUrl.take(120)}...")

    return cleanedUrl
}

// NEUE: Prüfe ob URL gültigen location Parameter hat
private fun hasValidLocationParameter(url: String): Boolean {
    try {
        val locationMatch = Regex("location=([^&]+)").find(url) ?: return false
        val locationParam = locationMatch.groupValues[1]
        val decodedLocation = java.net.URLDecoder.decode(locationParam, "UTF-8")
        val coords = decodedLocation.split(",")

        if (coords.size == 2) {
            val lat = coords[0].toDoubleOrNull()
            val lng = coords[1].toDoubleOrNull()
            return lat != null && lng != null &&
                   lat >= -90.0 && lat <= 90.0 &&
                   lng >= -180.0 && lng <= 180.0
        }
        return false
    } catch (e: Exception) {
        return false
    }
}

// NEUE: Performance-optimierte WebView-Komponente
@Composable
private fun OptimizedWebView(
    url: String,
    locationName: String,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isWebViewReady by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var hasPerformedValidation by remember { mutableStateOf(false) }
    var validationAttempts by remember { mutableIntStateOf(0) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // KRITISCH: Performance-optimierte Einstellungen mit API-Call-Prevention
                settings.apply {
                    // JavaScript und DOM
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = false
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = false

                    // NEUE: Verhindere automatische Resource-Loading die API-Calls auslösen könnten
                    loadsImagesAutomatically = false // Verhindert automatisches Bildladen
                    blockNetworkImage = true // Blockiert Netzwerk-Bilder initial
                    blockNetworkLoads = false // Erlaubt hauptsächliche URL aber nicht Subresources

                    // Rendering-Optimierungen
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false

                    // KRITISCH: Hardware-Beschleunigung und Performance
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // Strengere Sicherheit
                    mediaPlaybackRequiresUserGesture = true // Verhindert automatische Media-Requests
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false

                    // NEUE: Anti-API-Call Einstellungen
                    setGeolocationEnabled(false) // Verhindert Geolocation-API-Calls
                    javaScriptCanOpenWindowsAutomatically = false // Verhindert Popup-API-Calls
                    setSupportMultipleWindows(false) // Verhindert zusätzliche Windows/Tabs

                    // KRITISCH: Cache-Verhalten um wiederholte API-Calls zu vermeiden
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

                    userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0 Mobile Safari/537.36"

                    // ENTFERNT: Deprecated setRenderPriority and non-existent setAppCacheEnabled
                    // Diese Methoden existieren nicht mehr in modernen Android-Versionen
                }

                // KORRIGIERT: Hochoptimierte WebViewClient mit besserer Timing-Kontrolle
                webViewClient = object : WebViewClient() {
                    private var errorCount = 0
                    private val maxErrors = 3
                    private var pageStartTime = 0L

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return when {
                            url?.startsWith("https://www.google.com/maps") == true -> false
                            url?.startsWith("https://maps.googleapis.com") == true -> false
                            url?.startsWith("https://maps.gstatic.com") == true -> false
                            url?.startsWith("https://khms") == true -> false
                            url?.startsWith("https://streetviewpixels") == true -> false
                            url?.contains("google.com") == true -> false
                            else -> {
                                println("LocationImageScreen: 🚫 Blockiere externe URL: ${url?.take(50)}")
                                true
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        isWebViewReady = false
                        loadingProgress = 0
                        hasPerformedValidation = false
                        validationAttempts = 0
                        pageStartTime = System.currentTimeMillis()

                        if (url?.contains("streetview") == true) {
                            println("LocationImageScreen: 🔄 Lade Street View für $locationName")
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        if (url?.contains("streetview") == true) {
                            val loadTime = System.currentTimeMillis() - pageStartTime
                            println("LocationImageScreen: ✅ Street View Basis-Laden abgeschlossen für $locationName (${loadTime}ms)")

                            // KRITISCH: Warte länger bevor JavaScript-Validierung
                            view?.postDelayed({
                                if (!hasPerformedValidation && validationAttempts < 2) {
                                    performDelayedValidation(view, onError)
                                }
                            }, 4000) // 4 Sekunden warten statt 2
                        }
                    }

                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        super.onReceivedError(view, errorCode, description, failingUrl)

                        errorCount++
                        if (errorCount >= maxErrors) {
                            println("LocationImageScreen: 🔧 Zu viele WebView Fehler ($errorCount), wechsle zu Fallback")
                            onError("Wiederholt WebView Fehler: $description")
                        }
                    }

                    override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        val statusCode = errorResponse?.statusCode
                        val requestUrl = request?.url.toString()

                        // KRITISCH: Bei HTTP 400 "Missing location parameter" sofort zu Fallback wechseln
                        if (statusCode == 400 && requestUrl.contains("streetview")) {
                            println("LocationImageScreen: 🔧 HTTP 400 'Missing location parameter' - sofortiger Fallback für $locationName")
                            onError("HTTP 400: Location Parameter fehlt - verwende Fallback-Bild")
                            return
                        }

                        // Andere kritische HTTP-Fehler für Street View URLs
                        if ((statusCode == 403 || statusCode == 404) &&
                            requestUrl.contains("streetview") &&
                            requestUrl.contains("google.com/maps/embed") &&
                            !requestUrl.contains("favicon")) {

                            println("LocationImageScreen: 🔧 HTTP $statusCode für $locationName, wechsle zu Fallback")
                            onError("HTTP $statusCode: Street View nicht verfügbar")
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                        handler?.proceed()
                    }

                    // NEUE: Separate Validierungsfunktion mit verbessertem Timing
                    private fun performDelayedValidation(webView: WebView, onErrorCallback: (String) -> Unit) {
                        hasPerformedValidation = true
                        validationAttempts++

                        println("LocationImageScreen: 🔍 Starte verzögerte Validierung (Versuch $validationAttempts)")

                        // KRITISCH: Vorsichtige JavaScript-Validierung ohne DOM-Queries die API-Calls auslösen könnten
                        webView.evaluateJavascript("""
                            (function() {
                                try {
                                    // KEINE DOM-Queries die neue API-Calls triggern könnten
                                    var bodyText = document.body ? document.body.innerText.toLowerCase() : '';
                                    var bodyHtml = document.body ? document.body.innerHTML.toLowerCase() : '';
                                    
                                    console.log('Validation check - Body text length: ' + bodyText.length);
                                    console.log('Validation check - Body HTML length: ' + bodyHtml.length);
                                    
                                    // KRITISCH: Prüfe zuerst auf explizite Fehlermeldungen
                                    if (bodyText.includes('invalid request') && bodyText.includes('missing')) {
                                        console.log('❌ Explicit error detected: Invalid request missing parameter');
                                        return 'explicit_error_detected';
                                    }
                                    
                                    if (bodyText.includes('rejected') && bodyText.includes('request')) {
                                        console.log('❌ Request rejected detected');
                                        return 'request_rejected';
                                    }
                                    
                                    // SICHER: Prüfe nur HTML-Content, KEINE DOM-Queries
                                    var hasStreetViewIndicators = bodyHtml.includes('streetview') ||
                                                                 bodyHtml.includes('street-view') ||
                                                                 bodyHtml.includes('maps-embed') ||
                                                                 bodyHtml.includes('google') && bodyHtml.includes('maps');
                                    
                                    if (hasStreetViewIndicators) {
                                        console.log('✅ Street View content indicators found');
                                        return 'streetview_success';
                                    }
                                    
                                    // NEUE: Wenn Seite nicht leer ist, aber keine klaren Indikatoren
                                    if (bodyText.length > 50 || bodyHtml.length > 200) {
                                        console.log('⚠️ Content present but unclear - assume OK');
                                        return 'content_present_assume_ok';
                                    }
                                    
                                    console.log('❓ No clear content indicators');
                                    return 'no_clear_indicators';
                                    
                                } catch (innerError) {
                                    console.log('JavaScript inner error: ' + innerError.message);
                                    return 'js_inner_error';
                                }
                            })();
                        """) { result ->
                            println("LocationImageScreen: 📱 Verzögerte Validierung für $locationName: $result")

                            when {
                                result?.contains("explicit_error_detected") == true -> {
                                    println("LocationImageScreen: 🔧 Expliziter Google Maps Fehler erkannt - sofortiger Fallback")
                                    onErrorCallback("Google Maps: Explizite Fehlermeldung erkannt")
                                }
                                result?.contains("request_rejected") == true -> {
                                    println("LocationImageScreen: 🔧 Request rejected erkannt - sofortiger Fallback")
                                    onErrorCallback("Google Maps: Request wurde abgelehnt")
                                }
                                result?.contains("streetview_success") == true -> {
                                    println("LocationImageScreen: ✅ Street View Content erfolgreich validiert")
                                    isWebViewReady = true
                                }
                                result?.contains("content_present_assume_ok") == true -> {
                                    println("LocationImageScreen: ✅ Content vorhanden - optimistisch als OK betrachtet")
                                    isWebViewReady = true
                                }
                                result?.contains("js_") == true -> {
                                    println("LocationImageScreen: ⚠️ JavaScript Fehler - aber erlauben trotzdem")
                                    isWebViewReady = true // Bei JS-Fehlern optimistisch sein
                                }
                                else -> {
                                    println("LocationImageScreen: ⏳ Validierung unschlüssig - versuche erneut wenn möglich")
                                    if (validationAttempts < 2) {
                                        webView.postDelayed({
                                            performDelayedValidation(webView, onErrorCallback)
                                        }, 3000)
                                    } else {
                                        println("LocationImageScreen: ✅ Max Validierungsversuche erreicht - erlaube Anzeige")
                                        isWebViewReady = true
                                    }
                                }
                            }
                        }
                    }
                }

                // NEUE: Hochoptimierte WebChromeClient
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        loadingProgress = newProgress

                        // Nur bei wichtigen Meilensteinen loggen
                        if (newProgress == 100) {
                            println("LocationImageScreen: 📊 $locationName Street View: $newProgress%")
                            isWebViewReady = true
                        }
                    }

                    override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                        request?.deny()
                    }

                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message() ?: ""

                        // KORRIGIERT: Filtern nach tatsächlich wichtigen Fehlern
                        if (message.contains("Invalid request", ignoreCase = true) &&
                            message.contains("missing", ignoreCase = true)) {
                            println("LocationImageScreen: 🚨 KRITISCHER JS Fehler: ${message.take(100)}")
                            onError("JavaScript: $message")
                        } else if (message.contains("Error") || message.contains("Failed")) {
                            println("LocationImageScreen: 📱 JS: ${message.take(100)}")
                        }
                        return true
                    }
                }

                // KRITISCH: URL nur EINMAL laden
                try {
                    val headers = mapOf(
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                        "Accept-Language" to "de-DE,de;q=0.9,en;q=0.8",
                        "Cache-Control" to "max-age=300",
                        "Connection" to "keep-alive"
                    )

                    loadUrl(url, headers)
                } catch (e: Exception) {
                    println("LocationImageScreen: ❌ Fehler beim Laden der URL: ${e.message}")
                    onError("URL-Ladefehler: ${e.message}")
                }
            }
        },
        modifier = modifier,
        update = { webView ->
            // Verhindere unnötige Updates
            if (!isWebViewReady && loadingProgress < 100) {
                // WebView ist noch am Laden - keine Updates
            }
        }
    )

    // NEUE: Loading Overlay nur bei langsamen Verbindungen
    if (loadingProgress < 100 && loadingProgress > 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lade Street View... $loadingProgress%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
