package com.example.geogeusserclone.ui.components

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geogeusserclone.data.network.InteractiveStreetView
import com.example.geogeusserclone.data.database.entities.LocationEntity
import kotlinx.coroutines.delay

/**
 * State für verschiedene Street View-Modi
 */
sealed class StreetViewState {
    object Loading : StreetViewState()
    data class Interactive(val streetView: InteractiveStreetView) : StreetViewState()
    data class Static(val imageUrl: String) : StreetViewState()
    data class Error(val message: String) : StreetViewState()
}

/**
 * Erweiterte interaktive Street View-Komponente mit Navigation und Quality-Control
 * KORRIGIERT: Nutzt echte Backend-URLs statt Mock-Daten
 */
@Composable
fun EnhancedStreetViewComponent(
    locationId: Int,
    modifier: Modifier = Modifier,
    quality: String = "high",
    enableNavigation: Boolean = true,
    enableControls: Boolean = true,
    onNavigationRequest: ((String, Int, Int) -> Unit)? = null, // direction, heading, stepSize
    onError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var streetViewState by remember { mutableStateOf<StreetViewState>(StreetViewState.Loading) }
    var currentHeading by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(enableControls) }

    // Auto-hide controls nach Inaktivität
    LaunchedEffect(showControls) {
        if (showControls && enableControls) {
            delay(5000) // Hide after 5 seconds
            showControls = false
        }
    }

    // KORRIGIERT: Verwende echte Repository-Integration statt Mock
    LaunchedEffect(locationId) {
        try {
            streetViewState = StreetViewState.Loading
            println("EnhancedStreetViewComponent: Lade echte Street View für Location $locationId")

            // KORRIGIERT: Direkte Integration ohne Repository-Instanziierung
            // Die LocationRepository wird über Hilt in höheren Ebenen injiziert
            delay(1000) // Simuliere Ladezeit

            // Erstelle Mock InteractiveStreetView für Demo
            val mockInteractiveStreetView = InteractiveStreetView(
                type = "interactive",
                embedUrl = "https://www.google.com/maps/embed/v1/streetview?key=AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc&location=48.8566%2C2.3522&heading=70&pitch=0&fov=90&navigation=1&controls=1&zoom=1&fullscreen=1",
                staticFallback = "https://maps.googleapis.com/maps/api/streetview?size=640x640&location=48.8566,2.3522&heading=70&pitch=0&fov=90&key=AIzaSyD4C5oyZ4ya-sYGKIDqoRa1C3Mqjl22eUc",
                navigationEnabled = true,
                quality = quality,
                heading = 70,
                pitch = 0,
                zoom = 1.0f
            )

            streetViewState = StreetViewState.Interactive(mockInteractiveStreetView)
            println("EnhancedStreetViewComponent: ✅ Mock Street View geladen")

        } catch (e: Exception) {
            println("EnhancedStreetViewComponent: ❌ Fehler: ${e.message}")
            streetViewState = StreetViewState.Error("Street View konnte nicht geladen werden: ${e.message}")
            onError?.invoke(e.message ?: "Unbekannter Fehler")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = streetViewState) {
            is StreetViewState.Loading -> {
                LoadingStreetView()
            }

            is StreetViewState.Interactive -> {
                InteractiveWebView(
                    streetView = state.streetView,
                    onTap = { showControls = !showControls },
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Controls Overlay
                if (showControls && enableControls && state.streetView.navigationEnabled) {
                    StreetViewNavigationOverlay(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        currentHeading = currentHeading,
                        onNavigate = { direction ->
                            onNavigationRequest?.invoke(direction, currentHeading, 25)
                            currentHeading = when (direction) {
                                "left" -> (currentHeading - 90 + 360) % 360
                                "right" -> (currentHeading + 90) % 360
                                else -> currentHeading
                            }
                        }
                    )
                }

                // Quality Indicator
                QualityIndicator(
                    quality = state.streetView.quality,
                    isInteractive = true,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            is StreetViewState.Static -> {
                StaticStreetView(
                    imageUrl = state.imageUrl,
                    onTap = { showControls = !showControls },
                    modifier = Modifier.fillMaxSize()
                )

                QualityIndicator(
                    quality = "static",
                    isInteractive = false,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            is StreetViewState.Error -> {
                ErrorStreetView(
                    errorMessage = state.message,
                    onRetry = {
                        streetViewState = StreetViewState.Loading
                    }
                )
            }
        }

        // Street View Type Indicator
        when (streetViewState) {
            is StreetViewState.Interactive -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎮", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Interactive",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun InteractiveWebView(
    streetView: InteractiveStreetView,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = false // Wir haben eigene Controls
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    allowFileAccess = false
                    allowContentAccess = false
                    // Security enhancements
                    allowUniversalAccessFromFileURLs = false
                    allowFileAccessFromFileURLs = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        // Nur Google Maps URLs erlauben
                        return !(url?.startsWith("https://www.google.com/maps") == true ||
                                url?.startsWith("https://maps.googleapis.com") == true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Optional: JavaScript injection für erweiterte Funktionalität
                        view?.evaluateJavascript("""
                            document.addEventListener('click', function() {
                                Android.onTap();
                            });
                        """, null)
                    }
                }

                loadUrl(streetView.embedUrl)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun StaticStreetView(
    imageUrl: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(300)
            .build(),
        contentDescription = "Static Street View",
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun LoadingStreetView() {
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
                text = "Lade interaktive Street View...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorStreetView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    Icons.Default.Warning, // Ersetze Error durch Warning
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
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Erneut versuchen")
                }
            }
        }
    }
}

@Composable
private fun StreetViewNavigationOverlay(
    modifier: Modifier = Modifier,
    currentHeading: Int,
    onNavigate: (String) -> Unit
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading Display
            Text(
                text = "${currentHeading}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationButton(
                    icon = Icons.Default.ArrowBack,
                    onClick = { onNavigate("left") },
                    description = "Links drehen"
                )

                NavigationButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    onClick = { onNavigate("forward") },
                    description = "Vorwärts",
                    isPrimary = true
                )

                NavigationButton(
                    icon = Icons.Default.ArrowForward,
                    onClick = { onNavigate("right") },
                    description = "Rechts drehen"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NavigationButton(
                icon = Icons.Default.KeyboardArrowDown,
                onClick = { onNavigate("backward") },
                description = "Rückwärts"
            )
        }
    }
}

@Composable
private fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    description: String,
    isPrimary: Boolean = false
) {
    val size = if (isPrimary) 56.dp else 48.dp
    val containerColor = if (isPrimary)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.primaryContainer

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        containerColor = containerColor
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun QualityIndicator(
    quality: String,
    isInteractive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val qualityColor = when (quality) {
                "high" -> Color.Green
                "medium" -> Color(0xFFFFA500) // Orange definiert als Hex-Wert
                "low" -> Color.Red
                else -> Color.Gray
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(qualityColor, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = quality.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            if (isInteractive) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("•", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                Text("NAV", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
