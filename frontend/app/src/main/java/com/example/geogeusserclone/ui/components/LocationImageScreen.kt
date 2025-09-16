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

    // KORRIGIERT: AutoMemoryManagement direkt im Composable-Body aufrufen
    MemoryManager.AutoMemoryManagement(context)

    // KORRIGIERT: Verwende direkte Street View-Integration ohne externe Komponente
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // KORRIGIERT: Direkte Street View-Integration basierend auf URL-Typ
        when {
            // Interactive Street View (Google Maps Embed)
            location.imageUrl.contains("google.com/maps/embed/v1/streetview") &&
            !location.imageUrl.contains("[object Object]") -> {
                // KORRIGIERT: Optimierte WebView mit robuster Fehlerbehandlung
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                // KORRIGIERT: Optimierte Einstellungen für Google Maps
                                javaScriptEnabled = true
                                javaScriptCanOpenWindowsAutomatically = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = false
                                displayZoomControls = false
                                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

                                // VERBESSERT: Wichtige Einstellungen für Google Maps mit HTTP 400 Prevention
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = false
                                allowContentAccess = false
                                allowUniversalAccessFromFileURLs = false
                                allowFileAccessFromFileURLs = false

                                // NEUE: User Agent für bessere Kompatibilität
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; Android SDK built for x86) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"

                                // NEUE: Performance-Optimierungen
                                setRenderPriority(WebSettings.RenderPriority.HIGH)
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }

                            // VERBESSERT: WebViewClient mit robuster Fehlerbehandlung
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    println("LocationImageScreen: WebView URL-Navigation: ${url?.take(100)}...")
                                    return when {
                                        url?.startsWith("https://www.google.com/maps") == true -> false
                                        url?.startsWith("https://maps.googleapis.com") == true -> false
                                        url?.startsWith("https://maps.gstatic.com") == true -> false
                                        url?.startsWith("https://khms") == true -> false // Google Tile Server
                                        url?.startsWith("https://streetviewpixels") == true -> false
                                        else -> {
                                            println("LocationImageScreen: ❌ Blockierte URL: $url")
                                            true
                                        }
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    println("LocationImageScreen: 🔄 Lade Street View: ${url?.take(100)}...")
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    println("LocationImageScreen: ✅ Street View geladen: ${url?.take(100)}...")

                                    // NEUE: Inject JavaScript für bessere Navigation
                                    view?.evaluateJavascript("""
                                        console.log('Street View loaded successfully');
                                        
                                        // Verbesserte Touch-Handling
                                        document.addEventListener('touchstart', function(e) {
                                            console.log('Touch detected on Street View');
                                        });
                                        
                                        // Debug: Street View Status
                                        setTimeout(function() {
                                            var streetviewElements = document.querySelectorAll('[data-ved]');
                                            console.log('Street View elements found: ' + streetviewElements.length);
                                        }, 2000);
                                    """, null)
                                }

                                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    println("LocationImageScreen: ❌ WebView Error: $errorCode - $description for URL: $failingUrl")

                                    // NEUE: Bei HTTP 400 Fehler automatisch auf Fallback umschalten
                                    if (errorCode == -6 || description?.contains("400") == true) {
                                        println("LocationImageScreen: 🔧 HTTP 400 erkannt, verwende Fallback-Strategie")
                                        // Lade Fallback-URL für diese Location
                                        val fallbackUrl = generateLocationFallbackUrl(location)
                                        view?.loadUrl(fallbackUrl)
                                    }
                                }

                                override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    val statusCode = errorResponse?.statusCode
                                    println("LocationImageScreen: ❌ HTTP Error: $statusCode for ${request?.url}")

                                    // NEUE: Bei HTTP 400 automatisch auf Fallback umschalten
                                    if (statusCode == 400) {
                                        println("LocationImageScreen: 🔧 HTTP 400 erkannt, verwende Fallback-Strategie")
                                        val fallbackUrl = generateLocationFallbackUrl(location)
                                        view?.loadUrl(fallbackUrl)
                                    }
                                }
                            }

                            // VERBESSERT: WebChromeClient für erweiterte Fehlerbehandlung
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    println("LocationImageScreen: JS Console: ${consoleMessage?.message()}")
                                    return true
                                }

                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    if (newProgress % 25 == 0) { // Log every 25%
                                        println("LocationImageScreen: Loading progress: ${newProgress}%")
                                    }
                                }
                            }

                            // KORRIGIERT: Lade URL mit verbesserter Fehlerbehandlung
                            println("LocationImageScreen: 🎮 Lade Interactive Street View: ${location.imageUrl.take(120)}...")

                            // NEUE: URL-Validierung vor dem Laden
                            if (isUrlSafeToLoad(location.imageUrl)) {
                                loadUrl(location.imageUrl)
                            } else {
                                println("LocationImageScreen: ⚠️ URL unsicher, verwende Fallback")
                                val fallbackUrl = generateLocationFallbackUrl(location)
                                loadUrl(fallbackUrl)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Static Street View (Google API)
            location.imageUrl.startsWith("https://maps.googleapis.com/maps/api/streetview") &&
            !location.imageUrl.contains("[object Object]") &&
            !location.imageUrl.contains("PLACEHOLDER_API_KEY") -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(location.imageUrl)
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
            location.imageUrl.contains("unsplash.com") ||
            location.imageUrl.contains("images.") -> {
                FallbackImageView(
                    imageUrl = location.imageUrl,
                    locationName = location.city ?: "Unbekannt",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Empty or corrupted URLs
            location.imageUrl.isBlank() -> {
                EmptyLocationView(
                    locationName = location.city ?: "Unbekannt",
                    country = location.country,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Corrupted URLs with [object Object]
            location.imageUrl.contains("[object Object]") ||
            location.imageUrl.contains("PLACEHOLDER_API_KEY") -> {
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
                    imageUrl = location.imageUrl,
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
    return when {
        // Basis-Checks
        url.contains("[object Object]") -> false
        url.contains("PLACEHOLDER_API_KEY") -> false
        url.isBlank() -> false

        // Google Maps URLs sollten gültige API-Keys haben
        url.contains("google.com/maps/embed") && !url.contains("key=AIza") -> false

        // URL sollte nicht zu lang sein (HTTP 400 Prevention)
        url.length > 2048 -> false

        // Bekannte problematische Koordinaten prüfen
        isUrlContainingProblematicCoordinates(url) -> false

        else -> true
    }
}

private fun isUrlContainingProblematicCoordinates(url: String): Boolean {
    // Extrahiere Koordinaten aus URL
    val locationPattern = Regex("location=([^&]+)")
    val match = locationPattern.find(url)

    if (match != null) {
        val coordString = match.groupValues[1]
        val coords = coordString.split(",", "%2C")

        if (coords.size >= 2) {
            val lat = coords[0].toDoubleOrNull()
            val lng = coords[1].toDoubleOrNull()

            if (lat != null && lng != null) {
                // Bekannte problematische Koordinaten die HTTP 400 verursachen
                val problematicAreas = listOf(
                    // Central Park problematische Bereiche
                    Pair(40.785091, -73.968285),
                    // Copacabana Beach
                    Pair(-22.971177, -43.182543),
                    // Bondi Beach
                    Pair(-33.890542, 151.274856)
                )

                for ((problemLat, problemLng) in problematicAreas) {
                    val distance = calculateSimpleDistance(lat, lng, problemLat, problemLng)
                    if (distance < 0.05) { // Weniger als 50m
                        return true
                    }
                }
            }
        }
    }

    return false
}

private fun calculateSimpleDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 6371.0 // Earth radius in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadius * c
}

private fun generateLocationFallbackUrl(location: LocationEntity): String {
    // Intelligente Fallback-Strategie basierend auf Location-Daten
    val cityName = location.city?.lowercase() ?: ""
    val countryName = location.country?.lowercase() ?: ""

    return when {
        // Bekannte Städte mit hochwertigen Fallback-Bildern
        cityName.contains("new york") || cityName.contains("manhattan") ->
            "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"

        cityName.contains("london") ->
            "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"

        cityName.contains("paris") ->
            "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"

        cityName.contains("tokyo") ->
            "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"

        cityName.contains("berlin") ->
            "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop"

        cityName.contains("sydney") ->
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"

        cityName.contains("rome") || cityName.contains("roma") ->
            "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop"

        cityName.contains("rio") && countryName.contains("brazil") ->
            "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800&h=600&fit=crop"

        // Regionale Fallbacks nach Land
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

        countryName.contains("brazil") ->
            "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800&h=600&fit=crop"

        countryName.contains("italy") ->
            "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop"

        countryName.contains("spain") ->
            "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop"

        // Standard-Fallback für unbekannte Locations
        else ->
            "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
    }
}

// NEUE: Regionale Fallback-URL-Generation
private fun generateRegionalFallbackUrl(location: LocationEntity): String {
    // Intelligente regionale Fallback-Strategie
    val cityName = location.city?.lowercase() ?: ""
    val countryName = location.country?.lowercase() ?: ""

    return when {
        // Länder-basierte Fallbacks
        countryName.contains("united states") || countryName.contains("usa") ->
            "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"

        countryName.contains("united kingdom") || countryName.contains("england") || countryName.contains("scotland") || countryName.contains("wales") ->
            "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"

        countryName.contains("france") ->
            "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"

        countryName.contains("germany") || countryName.contains("deutschland") ->
            "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop"

        countryName.contains("japan") || countryName.contains("nippon") ->
            "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"

        countryName.contains("australia") ->
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"

        countryName.contains("brazil") || countryName.contains("brasil") ->
            "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800&h=600&fit=crop"

        countryName.contains("italy") || countryName.contains("italia") ->
            "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800&h=600&fit=crop"

        countryName.contains("spain") || countryName.contains("españa") ->
            "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop"

        countryName.contains("russia") || countryName.contains("rossiya") ->
            "https://images.unsplash.com/photo-1547036967-23d11aacaee0?w=800&h=600&fit=crop"

        countryName.contains("china") || countryName.contains("中国") ->
            "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=800&h=600&fit=crop"

        countryName.contains("india") || countryName.contains("भारत") ->
            "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800&h=600&fit=crop"

        countryName.contains("canada") ->
            "https://images.unsplash.com/photo-1503614472-8c93d56cd8d1?w=800&h=600&fit=crop"

        countryName.contains("mexico") || countryName.contains("méxico") ->
            "https://images.unsplash.com/photo-1518105779142-d975f22f1b0a?w=800&h=600&fit=crop"

        countryName.contains("netherlands") || countryName.contains("nederland") ->
            "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=800&h=600&fit=crop"

        countryName.contains("belgium") || countryName.contains("belgique") ->
            "https://images.unsplash.com/photo-1558618666-fccd38c73cd5?w=800&h=600&fit=crop"

        countryName.contains("switzerland") || countryName.contains("schweiz") ->
            "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800&h=600&fit=crop"

        countryName.contains("austria") || countryName.contains("österreich") ->
            "https://images.unsplash.com/photo-1471919743851-c4df8b6ee133?w=800&h=600&fit=crop"

        countryName.contains("sweden") || countryName.contains("sverige") ->
            "https://images.unsplash.com/photo-1509356843151-3e7d96241e11?w=800&h=600&fit=crop"

        countryName.contains("norway") || countryName.contains("norge") ->
            "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800&h=600&fit=crop"

        countryName.contains("denmark") || countryName.contains("danmark") ->
            "https://images.unsplash.com/photo-1513622470522-26c3c8a854bc?w=800&h=600&fit=crop"

        countryName.contains("south korea") || countryName.contains("korea") ->
            "https://images.unsplash.com/photo-1517154421773-0529f29ea451?w=800&h=600&fit=crop"

        countryName.contains("thailand") ->
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"

        countryName.contains("singapore") ->
            "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=800&h=600&fit=crop"

        // Kontinental-basierte Fallbacks
        isEuropeanCountry(countryName) ->
            "https://images.unsplash.com/photo-1467269204594-9661b134dd2b?w=800&h=600&fit=crop"

        isAsianCountry(countryName) ->
            "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"

        isAfricanCountry(countryName) ->
            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=800&h=600&fit=crop"

        isSouthAmericanCountry(countryName) ->
            "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800&h=600&fit=crop"

        isNorthAmericanCountry(countryName) ->
            "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"

        isOceaniaCountry(countryName) ->
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"

        // Standard-Fallback für unbekannte Regionen
        else ->
            "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
    }
}

// Hilfsfunktionen für kontinentale Erkennung
private fun isEuropeanCountry(country: String): Boolean {
    val europeanCountries = listOf(
        "albania", "andorra", "armenia", "austria", "azerbaijan", "belarus", "belgium",
        "bosnia", "bulgaria", "croatia", "cyprus", "czech", "denmark", "estonia",
        "finland", "france", "georgia", "germany", "greece", "hungary", "iceland",
        "ireland", "italy", "kosovo", "latvia", "liechtenstein", "lithuania",
        "luxembourg", "malta", "moldova", "monaco", "montenegro", "netherlands",
        "north macedonia", "norway", "poland", "portugal", "romania", "san marino",
        "serbia", "slovakia", "slovenia", "spain", "sweden", "switzerland",
        "ukraine", "united kingdom", "vatican"
    )
    return europeanCountries.any { country.contains(it) }
}

private fun isAsianCountry(country: String): Boolean {
    val asianCountries = listOf(
        "afghanistan", "armenia", "azerbaijan", "bahrain", "bangladesh", "bhutan",
        "brunei", "cambodia", "china", "cyprus", "georgia", "india", "indonesia",
        "iran", "iraq", "israel", "japan", "jordan", "kazakhstan", "kuwait",
        "kyrgyzstan", "laos", "lebanon", "malaysia", "maldives", "mongolia",
        "myanmar", "nepal", "north korea", "oman", "pakistan", "palestine",
        "philippines", "qatar", "saudi arabia", "singapore", "south korea",
        "sri lanka", "syria", "tajikistan", "thailand", "timor", "turkey",
        "turkmenistan", "united arab emirates", "uzbekistan", "vietnam", "yemen"
    )
    return asianCountries.any { country.contains(it) }
}

private fun isAfricanCountry(country: String): Boolean {
    val africanCountries = listOf(
        "algeria", "angola", "benin", "botswana", "burkina", "burundi", "cameroon",
        "cape verde", "central african", "chad", "comoros", "congo", "djibouti",
        "egypt", "equatorial guinea", "eritrea", "eswatini", "ethiopia", "gabon",
        "gambia", "ghana", "guinea", "ivory coast", "kenya", "lesotho", "liberia",
        "libya", "madagascar", "malawi", "mali", "mauritania", "mauritius",
        "morocco", "mozambique", "namibia", "niger", "nigeria", "rwanda",
        "sao tome", "senegal", "seychelles", "sierra leone", "somalia",
        "south africa", "south sudan", "sudan", "tanzania", "togo", "tunisia",
        "uganda", "zambia", "zimbabwe"
    )
    return africanCountries.any { country.contains(it) }
}

private fun isSouthAmericanCountry(country: String): Boolean {
    val southAmericanCountries = listOf(
        "argentina", "bolivia", "brazil", "chile", "colombia", "ecuador",
        "french guiana", "guyana", "paraguay", "peru", "suriname", "uruguay", "venezuela"
    )
    return southAmericanCountries.any { country.contains(it) }
}

private fun isNorthAmericanCountry(country: String): Boolean {
    val northAmericanCountries = listOf(
        "antigua", "bahamas", "barbados", "belize", "canada", "costa rica",
        "cuba", "dominica", "dominican republic", "el salvador", "grenada",
        "guatemala", "haiti", "honduras", "jamaica", "mexico", "nicaragua",
        "panama", "saint kitts", "saint lucia", "saint vincent", "trinidad",
        "united states", "usa"
    )
    return northAmericanCountries.any { country.contains(it) }
}

private fun isOceaniaCountry(country: String): Boolean {
    val oceaniaCountries = listOf(
        "australia", "fiji", "kiribati", "marshall islands", "micronesia",
        "nauru", "new zealand", "palau", "papua new guinea", "samoa",
        "solomon islands", "tonga", "tuvalu", "vanuatu"
    )
    return oceaniaCountries.any { country.contains(it) }
}
