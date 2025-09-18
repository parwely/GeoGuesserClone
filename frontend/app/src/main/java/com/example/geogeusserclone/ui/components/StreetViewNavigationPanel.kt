package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Erweiterte Street View Navigation-Steuerung
 * Bietet programmatische Navigation und Kamera-Kontrolle
 */
@Composable
fun StreetViewNavigationPanel(
    streetViewPanorama: StreetViewPanorama?,
    currentLocation: StreetViewPanoramaLocation?,
    coroutineScope: CoroutineScope,
    onLocationChanged: (LatLng) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAdvancedControls by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎮 Street View Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showAdvancedControls = !showAdvancedControls },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (showAdvancedControls) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, // KORRIGIERT: Verwende KeyboardArrow Icons
                        contentDescription = "Toggle advanced controls"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Directional Navigation (Always visible)
            DirectionalNavigation(
                streetViewPanorama = streetViewPanorama,
                isNavigating = isNavigating,
                onNavigationStart = { isNavigating = true },
                onNavigationEnd = { isNavigating = false },
                onLocationChanged = onLocationChanged,
                coroutineScope = coroutineScope
            )

            // Advanced Controls (Expandable)
            if (showAdvancedControls) {
                Spacer(modifier = Modifier.height(16.dp))

                AdvancedStreetViewControls(
                    streetViewPanorama = streetViewPanorama,
                    currentLocation = currentLocation,
                    coroutineScope = coroutineScope
                )
            }

            // Current Status
            Spacer(modifier = Modifier.height(12.dp))
            StreetViewStatusDisplay(
                currentLocation = currentLocation,
                isNavigating = isNavigating
            )
        }
    }
}

/**
 * Richtungsbasierte Navigation (Hoch/Runter/Links/Rechts)
 */
@Composable
private fun DirectionalNavigation(
    streetViewPanorama: StreetViewPanorama?,
    isNavigating: Boolean,
    onNavigationStart: () -> Unit,
    onNavigationEnd: () -> Unit,
    onLocationChanged: (LatLng) -> Unit,
    coroutineScope: CoroutineScope
) {
    fun navigateInDirection(direction: String) {
        streetViewPanorama?.let { panorama ->
            coroutineScope.launch {
                onNavigationStart()

                val currentPos = panorama.location?.position
                if (currentPos != null) {
                    val stepSize = 50.0 // 50 Meter
                    val latOffset = stepSize / 111000.0 // ca. 111km pro Grad
                    val lngOffset = stepSize / (111000.0 * Math.cos(Math.toRadians(currentPos.latitude)))

                    val newPosition = when (direction) {
                        "north" -> LatLng(currentPos.latitude + latOffset, currentPos.longitude)
                        "south" -> LatLng(currentPos.latitude - latOffset, currentPos.longitude)
                        "east" -> LatLng(currentPos.latitude, currentPos.longitude + lngOffset)
                        "west" -> LatLng(currentPos.latitude, currentPos.longitude - lngOffset)
                        else -> currentPos
                    }

                    panorama.setPosition(newPosition, 100) // 100m Suchradius
                    onLocationChanged(newPosition)

                    println("StreetViewNavigation: 🧭 Bewegung $direction zu $newPosition")
                }

                // Simulation delay
                kotlinx.coroutines.delay(1000)
                onNavigationEnd()
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // North Button
        NavigationButton(
            icon = Icons.Default.KeyboardArrowUp,
            label = "Nord",
            onClick = { navigateInDirection("north") },
            enabled = !isNavigating
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // West Button
            NavigationButton(
                icon = Icons.Default.KeyboardArrowLeft,
                label = "West",
                onClick = { navigateInDirection("west") },
                enabled = !isNavigating
            )

            // Center Info
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isNavigating)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isNavigating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.LocationOn, // KORRIGIERT: Verwende LocationOn statt MyLocation
                        contentDescription = "Current position",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // East Button
            NavigationButton(
                icon = Icons.Default.KeyboardArrowRight,
                label = "Ost",
                onClick = { navigateInDirection("east") },
                enabled = !isNavigating
            )
        }

        // South Button
        NavigationButton(
            icon = Icons.Default.KeyboardArrowDown,
            label = "Süd",
            onClick = { navigateInDirection("south") },
            enabled = !isNavigating
        )
    }
}

/**
 * Einzelner Navigations-Button
 */
@Composable
private fun NavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Erweiterte Street View-Kontrollen
 */
@Composable
private fun AdvancedStreetViewControls(
    streetViewPanorama: StreetViewPanorama?,
    currentLocation: StreetViewPanoramaLocation?,
    coroutineScope: CoroutineScope
) {
    Column {
        Text(
            text = "🔧 Erweiterte Kontrollen",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Camera Controls
        CameraControlSection(streetViewPanorama, coroutineScope)

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Actions
        QuickActionSection(streetViewPanorama, currentLocation, coroutineScope)
    }
}

/**
 * Kamera-Kontrollen für 360°-Bewegung
 */
@Composable
private fun CameraControlSection(
    streetViewPanorama: StreetViewPanorama?,
    coroutineScope: CoroutineScope
) {
    var currentBearing by remember { mutableFloatStateOf(0f) }
    var currentTilt by remember { mutableFloatStateOf(0f) }
    var currentZoom by remember { mutableFloatStateOf(1f) }

    fun animateCamera(bearing: Float? = null, tilt: Float? = null, zoom: Float? = null) {
        streetViewPanorama?.let { panorama ->
            coroutineScope.launch {
                val camera = StreetViewPanoramaCamera.Builder()
                    .bearing(bearing ?: currentBearing)
                    .tilt(tilt ?: currentTilt)
                    .zoom(zoom ?: currentZoom)
                    .build()

                panorama.animateTo(camera, 1000)

                bearing?.let { currentBearing = it }
                tilt?.let { currentTilt = it }
                zoom?.let { currentZoom = it }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "📷 Kamera-Kontrolle",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bearing (Richtung) Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Richtung:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("N" to 0f, "O" to 90f, "S" to 180f, "W" to 270f).forEach { (label, bearing) ->
                        FilterChip(
                            onClick = { animateCamera(bearing = bearing) },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            selected = Math.abs(currentBearing - bearing) < 10f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tilt Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Neigung:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("⬆️" to -30f, "➡️" to 0f, "⬇️" to 30f).forEach { (emoji, tilt) ->
                        FilterChip(
                            onClick = { animateCamera(tilt = tilt) },
                            label = { Text(emoji, style = MaterialTheme.typography.bodySmall) },
                            selected = Math.abs(currentTilt - tilt) < 5f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zoom Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zoom:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("🔍-" to 0.5f, "🔍" to 1f, "🔍+" to 2f).forEach { (label, zoom) ->
                        FilterChip(
                            onClick = { animateCamera(zoom = zoom) },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            selected = Math.abs(currentZoom - zoom) < 0.1f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Schnellaktionen für Street View
 */
@Composable
private fun QuickActionSection(
    streetViewPanorama: StreetViewPanorama?,
    currentLocation: StreetViewPanoramaLocation?,
    coroutineScope: CoroutineScope
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "⚡ Schnellaktionen",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Random Walk
                AssistChip(
                    onClick = {
                        coroutineScope.launch {
                            streetViewPanorama?.let { panorama ->
                                val currentPos = panorama.location?.position
                                if (currentPos != null) {
                                    // Generiere Position in 200m Umkreis
                                    val randomOffset = 0.002
                                    val randomLat = currentPos.latitude + (Math.random() - 0.5) * randomOffset
                                    val randomLng = currentPos.longitude + (Math.random() - 0.5) * randomOffset

                                    panorama.setPosition(LatLng(randomLat, randomLng), 200)
                                }
                            }
                        }
                    },
                    label = { Text("🚶 Spaziergang", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp)) } // KORRIGIERT: Verwende Place statt Navigation
                )

                // Look Around
                AssistChip(
                    onClick = {
                        coroutineScope.launch {
                            streetViewPanorama?.let { panorama ->
                                // 360° Rotation
                                repeat(4) { i ->
                                    val bearing = i * 90f
                                    val camera = StreetViewPanoramaCamera.Builder()
                                        .bearing(bearing)
                                        .tilt(0f)
                                        .zoom(1f)
                                        .build()
                                    panorama.animateTo(camera, 1000)
                                    kotlinx.coroutines.delay(1500)
                                }
                            }
                        }
                    },
                    label = { Text("👀 Umschauen", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) } // KORRIGIERT: Verwende Refresh statt RotateRight
                )
            }
        }
    }
}

/**
 * Status-Anzeige für Street View
 */
@Composable
private fun StreetViewStatusDisplay(
    currentLocation: StreetViewPanoramaLocation?,
    isNavigating: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "📊 Status",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            currentLocation?.let { location ->
                Text(
                    text = "📍 ${location.position.latitude.format(4)}, ${location.position.longitude.format(4)}",
                    style = MaterialTheme.typography.bodySmall
                )

                location.links?.let { links ->
                    Text(
                        text = "🔗 ${links.size} Verbindungen verfügbar",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "📸 Panorama-ID: ${location.panoId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isNavigating) {
                Text(
                    text = "🚶 Navigation aktiv...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Extension function
private fun Double.format(digits: Int) = "%.${digits}f".format(this)
