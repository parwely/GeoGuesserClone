package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.graphics.Paint
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geogeusserclone.utils.MapPerformanceUtils
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay

@Composable
fun GuessMapView(
    onGuessSelected: (lat: Double, lng: Double) -> Unit,
    onMapClose: () -> Unit,
    modifier: Modifier = Modifier,
    actualLocation: GeoPoint? = null,
    guessLocation: GeoPoint? = null,
    showLocationReveal: Boolean = false
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentGuessMarker by remember { mutableStateOf<Marker?>(null) }
    var actualLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var connectionLine by remember { mutableStateOf<Polyline?>(null) }
    var hasGuess by remember { mutableStateOf(false) }
    var isMapReady by remember { mutableStateOf(false) }

    // Performance-optimierte OSMDroid Konfiguration
    LaunchedEffect(Unit) {
        MapPerformanceUtils.configureMapPerformance(context)
        delay(100) // Kurze Verzögerung für bessere Performance
        isMapReady = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isMapReady) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        // Performance-optimierte Konfiguration
                        setTileSource(MapPerformanceUtils.createOptimizedTileSource())
                        MapPerformanceUtils.optimizeMapView(this)

                        setMultiTouchControls(true)
                        //Initiale Kamera Position
                        controller.setZoom(2.0)
                        controller.setCenter(GeoPoint(20.0, 0.0))

                        // Optimierte Map Events für Tap-to-Guess
                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                if (p != null && !showLocationReveal) {
                                    // Effizientes Marker Management
                                    currentGuessMarker?.let { marker ->
                                        overlays.remove(marker)
                                    }

                                    // Performance-optimierter Marker
                                    val marker = Marker(this@apply).apply {
                                        position = p
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "Deine Vermutung"
                                        snippet = "Lat: ${"%.4f".format(p.latitude)}, Lng: ${"%.4f".format(p.longitude)}"
                                        icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                                    }

                                    overlays.add(marker)
                                    currentGuessMarker = marker
                                    hasGuess = true

                                    // Optimiertes Invalidate
                                    post { invalidate() }
                                }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }

                        overlays.add(MapEventsOverlay(mapEventsReceiver))

                        // Performance-optimierter Kompass
                        val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this)
                        compassOverlay.enableCompass()
                        overlays.add(compassOverlay)

                        // Rotation Gestures mit Performance-Limits
                        val rotationGestureOverlay = RotationGestureOverlay(this)
                        rotationGestureOverlay.isEnabled = true
                        overlays.add(rotationGestureOverlay)

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    // Location Reveal Animation mit Performance-Optimierung
                    if (showLocationReveal && actualLocation != null) {
                        // Effiziente Overlay-Bereinigung
                        actualLocationMarker?.let { map.overlays.remove(it) }
                        connectionLine?.let { map.overlays.remove(it) }

                        // Performance-optimierter Actual Location Marker
                        val actualMarker = Marker(map).apply {
                            position = actualLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Tatsächlicher Standort"
                            snippet = "Lat: ${"%.4f".format(actualLocation.latitude)}, Lng: ${"%.4f".format(actualLocation.longitude)}"
                            icon = context.getDrawable(android.R.drawable.ic_menu_compass)
                        }
                        map.overlays.add(actualMarker)
                        actualLocationMarker = actualMarker

                        // Performance-optimierte Verbindungslinie
                        if (guessLocation != null) {
                            val guessPoint = GeoPoint(guessLocation.latitude, guessLocation.longitude)
                            val actualPoint = GeoPoint(actualLocation.latitude, actualLocation.longitude)

                            val line = Polyline().apply {
                                addPoint(guessPoint)
                                addPoint(actualPoint)
                                outlinePaint.apply {
                                    color = Color.Red.toArgb()
                                    strokeWidth = 8f
                                    style = Paint.Style.STROKE
                                    isAntiAlias = false // Performance-Optimierung
                                }
                                title = "Distanz zur tatsächlichen Location"
                            }
                            map.overlays.add(line)
                            connectionLine = line

                            // Performance-optimierte Kamera-Animation
                            val bounds = org.osmdroid.util.BoundingBox.fromGeoPoints(
                                listOf(guessPoint, actualPoint)
                            )

                            // Verwende animateTo für bessere Performance
                            map.post {
                                map.zoomToBoundingBox(bounds, true, 100)
                            }
                        } else {
                            // Sanfte Animation zum Standort
                            map.controller.animateTo(actualLocation, 10.0, 500L)
                        }

                        map.invalidate()
                    }
                }
            )
        } else {
            // Loading State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Performance-optimierte Top Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = onMapClose,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Close, contentDescription = "Karte schließen")
            }
        }

        // Performance-optimierte Zoom Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    mapView?.let { map ->
                        val currentZoom = map.zoomLevelDouble
                        if (currentZoom < map.maxZoomLevel) {
                            map.controller.setZoom(currentZoom + 1)
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FloatingActionButton(
                onClick = {
                    mapView?.let { map ->
                        val currentZoom = map.zoomLevelDouble
                        if (currentZoom > map.minZoomLevel) {
                            map.controller.setZoom(currentZoom - 1)
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Zoom Out")
            }
        }

        // Performance-optimierte Instruction Card
        if (!showLocationReveal) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = if (hasGuess) "Tippe 'Bestätigen' um deine Vermutung abzugeben" else "Tippe auf die Karte um deine Vermutung zu platzieren",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Performance-optimierter Confirm Button
        if (hasGuess && !showLocationReveal) {
            FloatingActionButton(
                onClick = {
                    currentGuessMarker?.let { marker ->
                        onGuessSelected(marker.position.latitude, marker.position.longitude)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Bestätigen")
                    Text("Bestätigen")
                }
            }
        }

        // Performance-optimierte Quick Navigation
        if (!showLocationReveal) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        mapView?.let { map ->
                            map.controller.animateTo(GeoPoint(20.0, 0.0), 2.0, 300L)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text("🌍", style = MaterialTheme.typography.bodySmall)
                }

                FloatingActionButton(
                    onClick = {
                        mapView?.let { map ->
                            map.controller.animateTo(GeoPoint(54.5260, 15.2551), 4.0, 300L)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text("🇪🇺", style = MaterialTheme.typography.bodySmall)
                }

                FloatingActionButton(
                    onClick = {
                        mapView?.let { map ->
                            map.controller.animateTo(GeoPoint(39.8283, -98.5795), 4.0, 300L)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Text("🇺🇸", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Performance-optimierte Location Reveal Info
        if (showLocationReveal && actualLocation != null && guessLocation != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Standort aufgedeckt!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Deine Vermutung:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${"%.4f".format(guessLocation.latitude)}, ${"%.4f".format(guessLocation.longitude)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column {
                            Text(
                                text = "Tatsächlicher Standort:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${"%.4f".format(actualLocation.latitude)}, ${"%.4f".format(actualLocation.longitude)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Performance-optimiertes Cleanup
    DisposableEffect(Unit) {
        onDispose {
            MapPerformanceUtils.cleanupMapResources(mapView)
        }
    }
}