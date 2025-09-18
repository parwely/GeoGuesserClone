package com.example.geogeusserclone.ui.components

import android.R
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.BuildConfig
import java.net.URLDecoder
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

    // State für Fallback-Handling
    var shouldUseFallbackImage by remember { mutableStateOf(false) }
    var fallbackImageUrl by remember { mutableStateOf<String?>(null) }
    var webViewError by remember { mutableStateOf<String?>(null) }

    // VEREINFACHT: Da Backend jetzt saubere URLs liefert, nur minimale Validierung
    val cleanedUrl = remember(location.id, location.imageUrl) {
        val result = validateCleanUrl(location.imageUrl)
        if (result.isNotBlank()) {
            println("LocationImageScreen: ✅ Clean URL from backend: ${result.take(80)}...")
        }
        result
    }

    // VEREINFACHT: Direkte Verwendung der Backend-URLs ohne komplexe Bereinigung
    val shouldUseWebView = remember(cleanedUrl) {
        cleanedUrl.contains("google.com/maps/embed/v1/streetview") &&
        !shouldUseFallbackImage
    }

    // Fallback-Generierung nur bei Bedarf
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
            // Fallback-Image verwenden
            shouldUseFallbackImage && stableFallbackUrl != null -> {
                FallbackImageView(
                    imageUrl = stableFallbackUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Interactive Street View (Google Maps Embed)
            shouldUseWebView -> {
                LaunchedEffect(location.id) {
                    println("LocationImageScreen: 🎮 Lade Clean Interactive Street View für ${location.city}")
                }

                CleanWebView(
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
            cleanedUrl.startsWith("https://maps.googleapis.com/maps/api/streetview") -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cleanedUrl)
                        .crossfade(300)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .error(R.drawable.ic_menu_report_image)
                        .placeholder(R.drawable.ic_menu_gallery)
                        .build(),
                    contentDescription = "Static Street View",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Fallback Images (Unsplash etc.)
            cleanedUrl.contains("unsplash.com") || cleanedUrl.contains("images.") -> {
                FallbackImageView(
                    imageUrl = cleanedUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Empty URLs
            cleanedUrl.isBlank() -> {
                EmptyLocationView(
                    locationName = location.city ?: "Unbekannt",
                    country = location.country,
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

        // UI Overlay-Komponenten
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

        // Error Overlay bei WebView-Problemen
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
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
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
        println("LocationImageScreen: 🧹 ORIGINAL URL: $originalUrl")

        // KRITISCH: Minimale Bereinigung - nur offensichtlich korrupte Daten entfernen
        var cleanUrl = originalUrl
            .replace("[object Object]", "")
            .replace("undefined", "")
            .trim()

        println("LocationImageScreen: 🧹 NACH GRUNDBEREINIGUNG: $cleanUrl")

        // KRITISCH: Für Google Maps URLs - KEINE weiteren Änderungen!
        if (cleanUrl.contains("google.com/maps/embed/v1/streetview")) {
            // NEUE: Prüfe location Parameter BEVOR weitere Verarbeitung
            val hasLocationBefore = checkLocationParameter(cleanUrl)
            println("LocationImageScreen: 🔍 Location Parameter vor Verarbeitung: $hasLocationBefore")

            if (hasLocationBefore) {
                // NUR URL-Dekodierung, KEINE Parameter-Entfernung
                val decodedUrl = simpleUrlDecode(cleanUrl)
                println("LocationImageScreen: 🔄 DEKODIERTE URL: $decodedUrl")

                val hasLocationAfter = checkLocationParameter(decodedUrl)
                println("LocationImageScreen: 🔍 Location Parameter nach Dekodierung: $hasLocationAfter")

                if (hasLocationAfter) {
                    println("LocationImageScreen: ✅ URL ist gültig und bereinigt")
                    return decodedUrl
                } else {
                    println("LocationImageScreen: ❌ Location Parameter verloren bei Dekodierung!")
                    return cleanUrl // Verwende Original wenn Dekodierung Parameter zerstört
                }
            } else {
                println("LocationImageScreen: ❌ Kein location Parameter in Google Maps URL gefunden")
                return "" // Triggert Fallback
            }
        }

        // Für statische Street View URLs
        if (cleanUrl.startsWith("https://maps.googleapis.com/maps/api/streetview")) {
            val hasLocation = checkLocationParameter(cleanUrl)
            println("LocationImageScreen: 🔍 Statische URL Location Parameter: $hasLocation")

            if (hasLocation) {
                println("LocationImageScreen: ✅ Statische Street View URL ist gültig")
                return cleanUrl
            } else {
                println("LocationImageScreen: ❌ Statische URL ohne location Parameter")
                return ""
            }
        }

        // Andere URLs (Unsplash etc.) - unverändert durchlassen
        if (!cleanUrl.contains("[object Object]") && !cleanUrl.contains("undefined") && cleanUrl.isNotBlank()) {
            println("LocationImageScreen: ✅ Andere URL akzeptiert: ${cleanUrl.take(100)}")
            return cleanUrl
        }

        println("LocationImageScreen: ❌ URL konnte nicht bereinigt werden")
        return ""

    } catch (e: Exception) {
        println("LocationImageScreen: ❌ KRITISCHER FEHLER bei URL-Bereinigung: ${e.message}")
        println("LocationImageScreen: 🔧 Stack trace: ${e.stackTrace.take(3).joinToString()}")
        return ""
    }
}

// NEUE: Einfache URL-Dekodierung ohne Parameter-Verlust
private fun simpleUrlDecode(url: String): String {
    return try {
        // NUR die wichtigsten Dekodierungen - sehr konservativ
        val decoded = url
            .replace("%2C", ",")  // Komma - KRITISCH für location Parameter
            .replace("%20", " ")  // Leerzeichen

        println("LocationImageScreen: 🔄 Einfache Dekodierung: ${url.take(50)} -> ${decoded.take(50)}")
        decoded
    } catch (e: Exception) {
        println("LocationImageScreen: ⚠️ Dekodierung fehlgeschlagen: ${e.message}")
        url // Bei Fehlern: Original zurückgeben
    }
}

// NEUE: Robuste Location Parameter-Prüfung
private fun checkLocationParameter(url: String): Boolean {
    return try {
        // Verschiedene location Parameter-Formate prüfen
        val patterns = listOf(
            Regex("location=([0-9.-]+[%2C,][0-9.-]+)"), // Mit Koordinaten
            Regex("location=([^&]+)") // Beliebiger Wert nach location=
        )

        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                val locationValue = match.groupValues[1]
                println("LocationImageScreen: 🎯 Gefundener location Parameter: '$locationValue'")

                // Prüfe ob es leer oder ungültig ist
                if (locationValue.isNotBlank() &&
                    !locationValue.startsWith("&") &&
                    !locationValue.endsWith("=") &&
                    locationValue != "%2C" &&
                    locationValue != ",") {
                    return true
                }
            }
        }

        println("LocationImageScreen: ❌ Kein gültiger location Parameter gefunden in: ${url.take(100)}")
        false
    } catch (e: Exception) {
        println("LocationImageScreen: ❌ Fehler bei location Parameter-Prüfung: ${e.message}")
        false
    }
}

// ENTFERNE: removeUnsupportedEmbedParameters Funktion komplett
// ENTFERNE: hasValidLocationParameter Funktion (ersetzt durch checkLocationParameter)

// NEUE: Performance-optimierte WebView-Komponente
@Composable
private fun CleanWebView(
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
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = false
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = false
                    // KORRIGIERT: Bilder MÜSSEN geladen werden für Street View
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    blockNetworkLoads = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)

                    displayZoomControls = false
                    // KRITISCH: Mixed Content auf ALWAYS_ALLOW für Google Maps
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    mediaPlaybackRequiresUserGesture = false

                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    setGeolocationEnabled(false)

                    cacheMode = WebSettings.LOAD_DEFAULT
                    // KRITISCH: Korrekter User Agent für Google Maps
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"

                    // NEUE: Zusätzliche Einstellungen für Google Maps (ohne deprecated Methoden)
                    @Suppress("DEPRECATION")
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                }
                webViewClient = object : WebViewClient() {
                    private var errorCount = 0
                    private val maxErrors = 3
                    private var pageStartTime = 0L

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return when {
                            url?.startsWith("https://www.google.com/maps") == true -> false
                            url?.startsWith("https://maps.gstatic.com") == true -> false
                            url?.startsWith("https://khms") == true -> false
                            url?.contains("google.com") == true -> false
                            else -> {
                                println("LocationImageScreen: 🚫 Blockiere externe URL: ${url?.take(50)}")
                                true
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
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
                            view?.postDelayed({
                                if (!hasPerformedValidation && validationAttempts < 2) {
                                    performDelayedValidation(view, onError)
                                }
                            }, 4000)
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

                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        val statusCode = errorResponse?.statusCode
                        val requestUrl = request?.url.toString()
                        if (statusCode == 400 && requestUrl.contains("streetview")) {
                            println("LocationImageScreen: 🔧 HTTP 400 'Missing location parameter' - sofortiger Fallback für $locationName")
                            onError("HTTP 400: Location Parameter fehlt - verwende Fallback-Bild")
                            return
                        }
                        if ((statusCode == 403 || statusCode == 404) &&
                            requestUrl.contains("streetview") &&
                            requestUrl.contains("google.com/maps/embed") &&
                            !requestUrl.contains("favicon")) {
                            println("LocationImageScreen: ��� HTTP $statusCode für $locationName, wechsle zu Fallback")
                            onError("HTTP $statusCode: Street View nicht verfügbar")
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        println("LocationImageScreen: 🔒 SSL Error: ${error?.toString()}")
                        handler?.proceed()
                    }

                    private fun performDelayedValidation(webView: WebView, onErrorCallback: (String) -> Unit) {
                        hasPerformedValidation = true
                        validationAttempts++
                        println("LocationImageScreen: 🔍 Starte verzögerte Validierung (Versuch $validationAttempts)")
                        webView.evaluateJavascript("""
                            (function() {
                                try {
                                    var bodyText = document.body ? document.body.innerText.toLowerCase() : '';
                                    var bodyHtml = document.body ? document.body.innerHTML.toLowerCase() : '';
                                    
                                    // Prüfe auf explizite Fehlermeldungen
                                    if (bodyText.includes('invalid request') && bodyText.includes('missing')) {
                                        return 'explicit_error_detected';
                                    }
                                    if (bodyText.includes('rejected') && bodyText.includes('request')) {
                                        return 'request_rejected';
                                    }
                                    
                                    // Prüfe auf Street View Erfolgs-Indikatoren
                                    var hasStreetViewIndicators = bodyHtml.includes('streetview') ||
                                                                 bodyHtml.includes('street-view') ||
                                                                 bodyHtml.includes('maps-embed') ||
                                                                 (bodyHtml.includes('google') && bodyHtml.includes('maps'));
                                    
                                    if (hasStreetViewIndicators) {
                                        return 'streetview_success';
                                    }
                                    
                                    // Content vorhanden aber unsicher
                                    if (bodyText.length > 50 || bodyHtml.length > 200) {
                                        return 'content_present_assume_ok';
                                    }
                                    
                                    return 'no_clear_indicators';
                                } catch (innerError) {
                                    return 'js_inner_error';
                                }
                            })();
                        """) { result ->
                            when {
                                result?.contains("explicit_error_detected") == true -> {
                                    onErrorCallback("Google Maps: Explizite Fehlermeldung erkannt")
                                }
                                result?.contains("request_rejected") == true -> {
                                    onErrorCallback("Google Maps: Request wurde abgelehnt")
                                }
                                result?.contains("streetview_success") == true -> {
                                    println("LocationImageScreen: ✅ Street View erfolgreich validiert")
                                    isWebViewReady = true
                                }
                                result?.contains("content_present_assume_ok") == true -> {
                                    println("LocationImageScreen: ✅ Content vorhanden, nehme als erfolgreich an")
                                    isWebViewReady = true
                                }
                                result?.contains("js_") == true -> {
                                    println("LocationImageScreen: ✅ JavaScript ausgeführt, nehme als erfolgreich an")
                                    isWebViewReady = true
                                }
                                else -> {
                                    if (validationAttempts < 2) {
                                        println("LocationImageScreen: 🔄 Validation unschlüssig, weitere Versuche...")
                                        webView.postDelayed({
                                            performDelayedValidation(webView, onErrorCallback)
                                        }, 3000)
                                    } else {
                                        println("LocationImageScreen: ✅ Max Validation-Versuche erreicht, nehme als OK an")
                                        isWebViewReady = true
                                    }
                                }
                            }
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        loadingProgress = newProgress
                        if (newProgress == 100) {
                            isWebViewReady = true
                        }
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.deny()
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message() ?: ""
                        if (message.contains("Invalid request", ignoreCase = true) &&
                            message.contains("missing", ignoreCase = true)) {
                            onError("JavaScript: $message")
                        }
                        return true
                    }
                }

                try {
                    if (url.contains("/maps/embed/v1/")) {
                        // KORRIGIERT: Google Maps Embed API MUSS in iframe geladen werden!
                        println("LocationImageScreen: 🌐 Lade Google Maps Embed in iframe mit optimierten Einstellungen")
                        println("LocationImageScreen: 🔗 FINALE URL für iframe: $url")

                        val html = """
                            <!DOCTYPE html>
                            <html lang="de">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                                <meta http-equiv="X-UA-Compatible" content="IE=edge">
                                <title>Street View</title>
                                <style>
                                    * { 
                                        margin: 0; 
                                        padding: 0; 
                                        box-sizing: border-box; 
                                    }
                                    html, body { 
                                        width: 100%; 
                                        height: 100%; 
                                        overflow: hidden; 
                                        background: #1a1a1a;
                                        font-family: Arial, sans-serif;
                                    }
                                    #streetview-container {
                                        position: relative;
                                        width: 100vw; 
                                        height: 100vh;
                                        background: #1a1a1a;
                                    }
                                    #streetview-frame { 
                                        width: 100%; 
                                        height: 100%; 
                                        border: none; 
                                        display: block;
                                        background: #f0f0f0;
                                        position: absolute;
                                        top: 0;
                                        left: 0;
                                    }
                                    .loading {
                                        position: absolute;
                                        top: 50%;
                                        left: 50%;
                                        transform: translate(-50%, -50%);
                                        color: #fff;
                                        font-size: 16px;
                                        z-index: 2;
                                        text-align: center;
                                    }
                                    .loading-spinner {
                                        width: 40px;
                                        height: 40px;
                                        border: 4px solid #444;
                                        border-top: 4px solid #fff;
                                        border-radius: 50%;
                                        animation: spin 1s linear infinite;
                                        margin: 0 auto 10px;
                                    }
                                    @keyframes spin {
                                        0% { transform: rotate(0deg); }
                                        100% { transform: rotate(360deg); }
                                    }
                                </style>
                            </head>
                            <body>
                                <div id="streetview-container">
                                    <div class="loading" id="loading">
                                        <div class="loading-spinner"></div>
                                        <div>Lade Street View...</div>
                                    </div>
                                    <iframe 
                                        id="streetview-frame"
                                        src="$url" 
                                        width="100%" 
                                        height="100%" 
                                        frameborder="0" 
                                        scrolling="no"
                                        allowfullscreen
                                        loading="eager"
                                        sandbox="allow-scripts allow-same-origin allow-forms allow-top-navigation"
                                        referrerpolicy="origin"
                                        allow="geolocation; fullscreen">
                                    </iframe>
                                </div>
                                <script>
                                    var iframe = document.getElementById('streetview-frame');
                                    var loading = document.getElementById('loading');
                                    var loadTimeout;
                        
                                    // Funktion zum Verstecken des Loading-Indikators
                                    function hideLoading() {
                                        if (loading) {
                                            loading.style.display = 'none';
                                            console.log('Loading-Indikator versteckt');
                                        }
                                    }
                                    
                                    // iframe onload Event
                                    iframe.onload = function() {
                                        console.log('Street View iframe erfolgreich geladen');
                                        clearTimeout(loadTimeout);
                                        setTimeout(hideLoading, 1000);
                                    };

                                    // iframe onerror Event
                                    iframe.onerror = function() {
                                        console.log('Street View iframe Fehler beim Laden');
                                        hideLoading();
                                    };
                                    
                                    // Timeout als Fallback
                                    loadTimeout = setTimeout(function() {
                                        console.log('Street View Timeout erreicht, verstecke Loading');
                                        hideLoading();
                                    }, 10000);
                                    
                                    // Zusätzliche Checks für iframe Content
                                    setTimeout(function() {
                                        try {
                                            if (iframe.contentWindow) {
                                                hideLoading();
                                            }
                                        } catch (e) {
                                            console.log('Street View Cross-origin - normal für Google Maps');
                                            hideLoading();
                                        }
                                    }, 3000);

                                    // Überwache iframe für HTTP-Fehler
                                    iframe.addEventListener('load', function() {
                                        setTimeout(function() {
                                            try {
                                                var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                                                var bodyText = iframeDoc.body ? iframeDoc.body.innerText.toLowerCase() : '';
                                                if (bodyText.includes('invalid request') || bodyText.includes('missing')) {
                                                    console.error('Street View: HTTP 400 - Invalid request detected');
                                                }
                                            } catch (e) {
                                                console.log('Street View Cross-origin - normal für Google Maps');
                                            }
                                        }, 2000);
                                    });
                                </script>
                            </body>
                            </html>
                        """.trimIndent()

                        // KRITISCH: loadDataWithBaseURL mit korrekter Base URL für Google Maps
                        loadDataWithBaseURL("https://www.google.com/", html, "text/html", "UTF-8", null)
                        println("LocationImageScreen: 🌐 Google Maps iframe HTML geladen")
                    } else {
                        val headers = mapOf(
                            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                            "Accept-Language" to "de-DE,de;q=0.9,en;q=0.8",
                            "Cache-Control" to "max-age=300",
                            "Connection" to "keep-alive"
                        )
                        loadUrl(url, headers)
                    }
                } catch (e: Exception) {
                    onError("URL-Ladefehler: ${e.message}")
                }
            }
        },
        modifier = modifier
    )
}

// NEUE: Haupt-URL-Validierungsfunktion
private fun validateCleanUrl(url: String): String {
    return when {
        url.isBlank() -> ""
        url.contains("[object Object]") -> ""
        url.contains("PLACEHOLDER_API_KEY") -> ""
        url.contains("undefined") -> ""
        else -> url.trim()
    }
}
