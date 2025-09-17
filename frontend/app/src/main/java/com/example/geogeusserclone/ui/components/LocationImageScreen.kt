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