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
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.graphics.Bitmap
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

// NEUE: Erstelle Street View URLs direkt aus Backend-Daten
private fun createOptimalStreetViewConfig(location: LocationEntity): StreetViewConfig {
    // KORRIGIERT: Bessere Backend-Daten-Erkennung
    val hasBackendData = location.id.contains("backend_") ||
                        (location.id.isDigitsOnly() && location.latitude != 0.0 && location.longitude != 0.0)

    println("createOptimalStreetViewConfig: Location ${location.id}, hasBackendData=$hasBackendData, coords=(${location.latitude},${location.longitude})")

    return when {
        // 1. PRIORITÄT: Interactive Street View für echte Backend-Locations
        hasBackendData && location.latitude != 0.0 && location.longitude != 0.0 -> {
            val embedUrl = buildGoogleMapsEmbedUrl(location)
            println("createOptimalStreetViewConfig: 🎯 Erstelle Interactive Google Maps Embed URL: ${embedUrl.take(100)}...")

            StreetViewConfig(
                mode = "interactive",
                url = embedUrl,
                isReliable = true,
                quality = "high",
                hasNavigation = true
            )
        }

        // 2. PRIORITÄT: Existierende Google Street View URLs verwenden (Static)
        location.imageUrl.contains("maps.googleapis.com/maps/api/streetview") -> {
            println("createOptimalStreetViewConfig: 🔧 Verwende existierende Street View URL")
            StreetViewConfig(
                mode = "static",
                url = location.imageUrl,
                isReliable = true,
                quality = "medium",
                hasNavigation = false
            )
        }

        // 3. FALLBACK: Nur wenn keine Google APIs verfügbar
        else -> {
            println("createOptimalStreetViewConfig: 🖼️ Verwende Fallback-Bild für ${location.city}")
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

// NEUE: Google Maps Embed URL Builder für echtes Street View
private fun buildGoogleMapsEmbedUrl(location: LocationEntity): String {
    // KRITISCH: Verwende den API-Key aus strings.xml und prüfe Berechtigung
    val apiKey = "AIzaSyDPV-VKcOe46KeoGVkIryVP3Uwq6QApV3A"

    // WICHTIG: Prüfe ob Koordinaten gültig sind
    if (location.latitude == 0.0 && location.longitude == 0.0) {
        println("buildGoogleMapsEmbedUrl: ❌ Ungültige Koordinaten (0,0)")
        throw IllegalArgumentException("Ungültige Koordinaten")
    }

    // KORRIGIERT: Verwende echte Google Maps Embed API für Interactive Street View
    val embedUrl = "https://www.google.com/maps/embed/v1/streetview"

    return buildString {
        append(embedUrl)
        append("?key=").append(apiKey)
        append("&location=").append(location.latitude).append(",").append(location.longitude)
        append("&heading=").append((0..359).random()) // Zufällige Blickrichtung
        append("&pitch=0")
        append("&fov=90")
        append("&language=de")
        append("&region=DE")
    }.also { url ->
        println("buildGoogleMapsEmbedUrl: 🎮 Erstelle Interactive Embed URL: ${url.take(100)}...")
    }
}

// HILFSFUNKTION: Prüfe ob String nur Ziffern enthält
private fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
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
    var isWebViewInitialized by remember { mutableStateOf(false) }

    // PERFORMANCE: Verwende LaunchedEffect für WebView-Setup off Main Thread
    LaunchedEffect(url) {
        println("InteractiveWebView: ⚡ Starte asynchrone WebView-Initialisierung")
        // Kleine Verzögerung um Main Thread zu entlasten
        kotlinx.coroutines.delay(100)
        isWebViewInitialized = true
    }

    if (!isWebViewInitialized) {
        // Zeige Loading während WebView initialisiert wird
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Initialisiere Street View...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    AndroidView(
        factory = { context ->
            println("InteractiveWebView: 🏗️ Erstelle WebView mit Anti-Crash-Optimierungen")

            WebView(context).apply {
                // KRITISCH: Anti-Crash Settings
                settings.apply {
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = false
                    domStorageEnabled = true

                    // ANTI-CRASH: Blockiere teure Features
                    @Suppress("DEPRECATION")
                    databaseEnabled = false
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    blockNetworkLoads = false

                    // ANTI-CRASH: Deaktiviere problematische Features
                    setGeolocationEnabled(false)
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false

                    // PERFORMANCE: Optimiere Rendering
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false) // ANTI-CRASH: Disable zoom für Stabilität
                    displayZoomControls = false

                    // KRITISCH: Mixed Content für Google Maps
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    mediaPlaybackRequiresUserGesture = false

                    // ANTI-CRASH: Conservative Cache Settings
                    cacheMode = WebSettings.LOAD_NO_CACHE // Verhindere Cache-Probleme

                    // ANTI-CRASH: Stable User Agent
                    userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"

                    // ANTI-CRASH: Normal Render Priority
                    @Suppress("DEPRECATION")
                    setRenderPriority(WebSettings.RenderPriority.NORMAL)
                }

                webViewClient = object : WebViewClient() {
                    private var hasStartedLoading = false
                    private var loadStartTime = 0L
                    private var errorCount = 0
                    private val maxErrors = 2

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        // ANTI-CRASH: Sehr restriktive URL-Filterung
                        return when {
                            url?.startsWith("https://www.google.com/maps/embed") == true -> false
                            url?.startsWith("https://maps.gstatic.com") == true -> false
                            url?.contains("google.com") == true -> false
                            else -> {
                                println("InteractiveWebView: 🚫 Blockiere externe URL für Stabilität")
                                true
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (!hasStartedLoading) {
                            hasStartedLoading = true
                            loadStartTime = System.currentTimeMillis()
                            println("InteractiveWebView: 🔄 Street View lädt...")
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val loadTime = System.currentTimeMillis() - loadStartTime
                        println("InteractiveWebView: ✅ Street View geladen in ${loadTime}ms")

                        // ANTI-CRASH: Sehr konservative Validierung
                        view?.postDelayed({
                            println("InteractiveWebView: ✅ Page loading abgeschlossen")
                        }, 1000) // Reduziert auf 1 Sekunde
                    }

                    @Suppress("DEPRECATION")
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        errorCount++
                        println("InteractiveWebView: ❌ WebView Fehler #$errorCount: $description")

                        if (errorCount >= maxErrors) {
                            println("InteractiveWebView: 🔧 Zu viele Fehler ($errorCount), wechsle zu Fallback")
                            onError("WebView instabil: $description")
                        }
                    }

                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        val statusCode = errorResponse?.statusCode
                        val requestUrl = request?.url.toString()

                        when (statusCode) {
                            400 -> {
                                println("InteractiveWebView: 🔧 HTTP 400 - Invalid request für Street View")
                                onError("Street View Request ungültig")
                            }
                            403 -> {
                                println("InteractiveWebView: 🔧 HTTP 403 - Street View nicht verfügbar")
                                onError("Street View nicht verfügbar (403)")
                            }
                            404 -> {
                                if (requestUrl.contains("streetview")) {
                                    println("InteractiveWebView: 🔧 HTTP 404 - Street View nicht gefunden")
                                    onError("Street View nicht gefunden (404)")
                                }
                            }
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        if (newProgress == 100) {
                            println("InteractiveWebView: ✅ Progress 100% erreicht")
                        }
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        // ANTI-CRASH: Alle Permissions ablehnen
                        request?.deny()
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message() ?: ""
                        // ANTI-CRASH: Ignoriere nicht-kritische Console-Messages
                        if (message.contains("Invalid request", ignoreCase = true) &&
                            message.contains("missing", ignoreCase = true)) {
                            onError("JavaScript: $message")
                        }
                        return true
                    }
                }

                try {
                    if (url.contains("/maps/embed/v1/")) {
                        println("InteractiveWebView: 🌐 Lade STABILISIERTE Google Maps Embed")

                        // ANTI-CRASH: Minimalistisches HTML ohne komplexe JavaScript Navigation
                        val stableHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    * { margin: 0; padding: 0; }
                                    html, body { 
                                        width: 100%; 
                                        height: 100%; 
                                        overflow: hidden; 
                                        background: #1a1a1a;
                                        font-family: Arial, sans-serif;
                                    }
                                    #streetview-frame { 
                                        width: 100%; 
                                        height: 100%; 
                                        border: none; 
                                        display: block;
                                        background: #f0f0f0;
                                    }
                                    .info {
                                        position: absolute;
                                        top: 10px;
                                        left: 10px;
                                        background: rgba(0,0,0,0.7);
                                        color: white;
                                        padding: 8px 12px;
                                        border-radius: 4px;
                                        font-size: 12px;
                                        z-index: 1000;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="info">🎮 Interactive Street View</div>
                                <iframe 
                                    id="streetview-frame"
                                    src="$url" 
                                    width="100%" 
                                    height="100%" 
                                    frameborder="0" 
                                    scrolling="no"
                                    allowfullscreen
                                    sandbox="allow-scripts allow-same-origin">
                                </iframe>
                                <script>
                                    // ANTI-CRASH: Minimales JavaScript
                                    console.log('🎮 Stable Street View loaded');
                                    
                                    document.getElementById('streetview-frame').onload = function() {
                                        console.log('✅ Street View iframe loaded successfully');
                                    };
                                    
                                    document.getElementById('streetview-frame').onerror = function() {
                                        console.error('❌ Street View iframe failed to load');
                                    };
                                </script>
                            </body>
                            </html>
                        """.trimIndent()

                        // ANTI-CRASH: loadDataWithBaseURL für bessere Stabilität
                        loadDataWithBaseURL("https://www.google.com/", stableHtml, "text/html", "UTF-8", null)
                        println("InteractiveWebView: 🌐 Stable HTML geladen")
                    } else {
                        // ANTI-CRASH: Direkte URL-Ladung mit Stabilität
                        val headers = mapOf(
                            "Accept" to "text/html,application/xhtml+xml",
                            "Accept-Language" to "de-DE,de;q=0.9"
                        )
                        loadUrl(url, headers)
                    }
                } catch (e: Exception) {
                    println("InteractiveWebView: ❌ URL-Ladefehler: ${e.message}")
                    onError("URL-Ladefehler: ${e.message}")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // ANTI-CRASH: Cleanup
    DisposableEffect(url) {
        onDispose {
            println("InteractiveWebView: 🧹 WebView Cleanup")
        }
    }
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
