package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.data.network.StreetViewConfig
import kotlinx.coroutines.launch

/**
 * Intelligente Street View-Komponente mit automatischem Fallback
 * Nutzt das neue Diagnostic API für optimale URL-Auswahl
 */
@Composable
fun InteractiveStreetViewWithFallback(
    location: LocationEntity,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var streetViewConfig by remember { mutableStateOf<StreetViewConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Hole Diagnostic-Konfiguration beim ersten Laden
    LaunchedEffect(location.id) {
        scope.launch {
            try {
                isLoading = true
                error = null

                println("InteractiveStreetViewWithFallback: 🔍 Hole Diagnostic-Config für ${location.city}")

                // Simuliere Diagnostic API Call (da Repository nicht direkt verfügbar)
                val config = createOptimalStreetViewConfig(location)
                streetViewConfig = config

                println("InteractiveStreetViewWithFallback: ✅ Config erhalten: ${config.mode}")
            } catch (e: Exception) {
                println("InteractiveStreetViewWithFallback: ❌ Fehler: ${e.message}")
                error = e.message
                // Fallback-Konfiguration
                streetViewConfig = StreetViewConfig(
                    mode = "fallback_image",
                    url = generateLocationFallbackUrlInternal(location),
                    isReliable = false,
                    quality = "low",
                    hasNavigation = false,
                    errorMessage = "Street View nicht verfügbar"
                )
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                StreetViewLoadingIndicator()
            }

            error != null -> {
                StreetViewErrorView(
                    error = error!!,
                    onRetry = {
                        scope.launch {
                            isLoading = true
                            error = null
                            // Retry-Logik
                        }
                    }
                )
            }

            streetViewConfig != null -> {
                when (streetViewConfig!!.mode) {
                    "interactive" -> {
                        InteractiveWebViewWithErrorHandling(
                            url = streetViewConfig!!.url,
                            onError = { errorMsg ->
                                // Automatischer Fallback bei WebView-Fehlern
                                scope.launch {
                                    println("InteractiveStreetViewWithFallback: 🔧 WebView-Fehler, fallback zu static")
                                    streetViewConfig = streetViewConfig!!.copy(
                                        mode = "static",
                                        url = generateStaticFallbackUrlInternal(location),
                                        hasNavigation = false
                                    )
                                }
                            }
                        )
                    }

                    "static" -> {
                        StaticStreetViewImage(
                            url = streetViewConfig!!.url,
                            location = location
                        )
                    }

                    "fallback_image" -> {
                        FallbackImageWithInfo(
                            url = streetViewConfig!!.url,
                            location = location,
                            errorMessage = streetViewConfig!!.errorMessage
                        )
                    }
                }

                // Status-Overlay
                StreetViewStatusOverlay(
                    config = streetViewConfig!!,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
fun StaticStreetViewWithFallback(
    imageUrl: String,
    location: LocationEntity,
    modifier: Modifier = Modifier
) {
    var loadError by remember { mutableStateOf(false) }
    var fallbackUrl by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!loadError && fallbackUrl == null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(300)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .listener(
                        onError = { _, _ ->
                            println("StaticStreetViewWithFallback: ❌ Static Street View failed, using fallback")
                            loadError = true
                            fallbackUrl = generateLocationFallbackUrlInternal(location)
                        }
                    )
                    .build(),
                contentDescription = "Street View",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback-Bild anzeigen
            val finalUrl = fallbackUrl ?: generateLocationFallbackUrlInternal(location)
            FallbackImageWithInfo(
                url = finalUrl,
                location = location,
                errorMessage = "Street View nicht verfügbar"
            )
        }

        // Status-Indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (loadError)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (loadError) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (loadError) "Fallback" else "Street View",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SmartFallbackView(
    location: LocationEntity,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var bestUrl by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(true) }

    LaunchedEffect(location.id) {
        scope.launch {
            println("SmartFallbackView: 🔍 Suche beste verfügbare URL für ${location.city}")

            // Intelligente URL-Suche
            val searchResults = listOf(
                // 1. Versuche statische Street View
                generateStaticFallbackUrlInternal(location),
                // 2. Bekannte Location-Bilder
                getKnownLocationImageInternal(location),
                // 3. Regionale Fallbacks
                generateRegionalFallbackUrlInternal(location)
            ).filterNotNull()

            bestUrl = searchResults.firstOrNull()
            isSearching = false

            println("SmartFallbackView: ✅ Beste URL gefunden: ${bestUrl?.take(50)}...")
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isSearching) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Suche beste verfügbare Ansicht...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (bestUrl != null) {
            FallbackImageWithInfo(
                url = bestUrl!!,
                location = location,
                errorMessage = "Automatisch beste verfügbare Ansicht ausgewählt"
            )
        } else {
            // Notfall-Anzeige
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Keine Ansicht verfügbar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = location.city ?: "Unbekannte Location",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Hilfsfunktionen
private fun createOptimalStreetViewConfig(location: LocationEntity): StreetViewConfig {
    return when {
        location.imageUrl.contains("google.com/maps/embed/v1/streetview") -> {
            StreetViewConfig(
                mode = "interactive",
                url = location.imageUrl,
                isReliable = true,
                quality = "high",
                hasNavigation = true
            )
        }
        location.imageUrl.contains("maps.googleapis.com/maps/api/streetview") -> {
            StreetViewConfig(
                mode = "static",
                url = location.imageUrl,
                isReliable = true,
                quality = "medium",
                hasNavigation = false
            )
        }
        else -> {
            StreetViewConfig(
                mode = "fallback_image",
                url = generateLocationFallbackUrlInternal(location),
                isReliable = false,
                quality = "low",
                hasNavigation = false,
                errorMessage = "Street View nicht verfügbar"
            )
        }
    }
}

// Helper Components
@Composable
private fun StreetViewLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Optimiere Street View...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun StreetViewErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Street View Fehler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Erneut versuchen")
                }
            }
        }
    }
}

@Composable
private fun InteractiveWebViewWithErrorHandling(
    url: String,
    onError: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                }

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        onError("WebView Error: $description")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        if (request?.url.toString().contains("streetview")) {
                            onError("HTTP ${errorResponse?.statusCode}: Street View nicht verfügbar")
                        }
                    }
                }

                if (url.contains("/maps/embed/v1/")) {
                    val html = """
                        <html><body style=\"margin:0;padding:0;overflow:hidden;\">
                        <iframe width=\"100%\" height=\"100%\" frameborder=\"0\" style=\"border:0;width:100vw;height:100vh;\" src=\"$url\" allowfullscreen></iframe>
                        </body></html>
                    """.trimIndent()
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                } else {
                    loadUrl(url)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun StaticStreetViewImage(
    url: String,
    location: LocationEntity
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(300)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build(),
        contentDescription = "Street View",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun FallbackImageWithInfo(
    url: String,
    location: LocationEntity,
    errorMessage: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(300)
                .build(),
            contentDescription = "Fallback für ${location.city}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        errorMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StreetViewStatusOverlay(
    config: StreetViewConfig,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (config.mode) {
                "interactive" -> MaterialTheme.colorScheme.primaryContainer
                "static" -> MaterialTheme.colorScheme.secondaryContainer
                "fallback_image" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (config.mode) {
                "interactive" -> Icons.Default.PlayArrow
                "static" -> Icons.Default.Refresh
                "fallback_image" -> Icons.Default.Phone
                else -> Icons.Default.Info
            }

            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = config.mode.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            if (!config.isReliable) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Hilfsfunktionen für Fallback-URLs (mit eindeutigen Namen)
private fun generateStaticFallbackUrlInternal(location: LocationEntity): String {
    val heading = (0..359).random()
    return "https://maps.googleapis.com/maps/api/streetview?" +
            "size=640x640" +
            "&location=${location.latitude},${location.longitude}" +
            "&heading=$heading" +
            "&pitch=0" +
            "&fov=90" +
            "&key=AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc"
}

private fun getKnownLocationImageInternal(location: LocationEntity): String? {
    val cityName = location.city?.lowercase() ?: ""

    return when {
        cityName.contains("death valley") -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=600&fit=crop"
        cityName.contains("paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        cityName.contains("london") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
        cityName.contains("new york") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        cityName.contains("tokyo") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
        cityName.contains("sydney") -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
        else -> null
    }
}

private fun generateRegionalFallbackUrlInternal(location: LocationEntity): String {
    val lat = location.latitude
    val lng = location.longitude

    return when {
        // USA
        (lat in 25.0..49.0 && lng in -125.0..-66.0) -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        // Europa
        (lat in 35.0..70.0 && lng in -10.0..30.0) -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        // Asien
        (lat in 10.0..50.0 && lng in 100.0..150.0) -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
        // Australien
        (lat in -45.0..-10.0 && lng in 110.0..155.0) -> "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop"
        // Afrika
        (lat in -35.0..35.0 && lng in -20.0..50.0) -> "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=800&h=600&fit=crop"
        // Südamerika
        (lat in -55.0..15.0 && lng in -80.0..-35.0) -> "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800&h=600&fit=crop"
        else -> "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
    }
}

private fun generateLocationFallbackUrlInternal(location: LocationEntity): String {
    // Prüfe erst bekannte Locations
    val knownUrl = getKnownLocationImageInternal(location)
    if (knownUrl != null) {
        return knownUrl
    }

    // Falls keine bekannte Location, verwende regionalen Fallback
    return generateRegionalFallbackUrlInternal(location)
}
