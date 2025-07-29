package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.modules.TileWriter
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapGuessComponent(
    onGuessSelected: (lat: Double, lng: Double) -> Unit,
    onMapClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }

    // Performance-optimierte OSMDroid Konfiguration
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
            userAgentValue = "GeoGuesserClone/1.0"

            // Performance Settings
            setTileFileSystemThreads(4) // Optimiert für moderne Geräte
            setTileFileSystemMaxQueueSize(40)
            setTileDownloadThreads(8)
            setTileDownloadMaxQueueSize(20)

            // Cache Settings für bessere Performance
            osmdroidTileCache = context.cacheDir
            osmdroidBasePath = context.filesDir
            setMapViewHardwareAccelerated(true)

            // Memory Management
            setTileFileSystemCacheMaxBytes(50 * 1024 * 1024L) // 50MB Cache
            setTileFileSystemCacheTrimBytes(40 * 1024 * 1024L) // Trim bei 40MB
        }
    }

    Card(
        modifier = modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Header mit Performance-Indikator
            TopAppBar(
                title = { Text("Wähle deine Vermutung") },
                navigationIcon = {
                    IconButton(onClick = onMapClose) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }
            )

            // Optimierte Map
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            // Performance-optimierte Tile Source
                            setTileSource(createOptimizedTileSource())

                            // Hardware Acceleration aktivieren
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            // Optimierte Touch-Controls
                            setMultiTouchControls(true)
                            isTilesScaledToDpi = true
                            isHorizontalMapRepetitionEnabled = false
                            isVerticalMapRepetitionEnabled = false

                            // Optimierter Zoom und Center
                            controller.setZoom(3.0) // Besserer Start-Zoom
                            controller.setCenter(GeoPoint(20.0, 0.0)) // Zentriert auf bewohnte Gebiete

                            // Zoom-Limits für Performance
                            minZoomLevel = 2.0
                            maxZoomLevel = 18.0

                            // Performance Settings
                            overlayManager.tilesOverlay.setLoadingBackgroundColor(android.graphics.Color.TRANSPARENT)

                            // Optimierte Map Events
                            val mapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { point ->
                                        selectedPoint = point

                                        // Effizientes Marker-Management
                                        currentMarker?.let { marker ->
                                            overlays.remove(marker)
                                        }

                                        // Erstelle neuen Marker mit optimierter Performance
                                        val marker = Marker(this@apply).apply {
                                            position = point
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                            title = "Deine Vermutung"
                                            snippet = "Lat: ${"%.4f".format(point.latitude)}, Lng: ${"%.4f".format(point.longitude)}"

                                            // Performance: Custom Icon nur wenn nötig
                                            icon = context.getDrawable(android.R.drawable.ic_menu_mylocation)
                                        }

                                        overlays.add(marker)
                                        currentMarker = marker

                                        // Optimiertes Invalidate
                                        post { invalidate() }
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }

                            overlays.add(MapEventsOverlay(mapEventsReceiver))
                            mapView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        // Lazy updates für bessere Performance
                    }
                )

                // Zoom Controls für bessere UX
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

                // Quick Navigation
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            mapView?.let { map ->
                                map.controller.animateTo(GeoPoint(54.5260, 15.2551), 4.0, 500L)
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
                                map.controller.animateTo(GeoPoint(39.8283, -98.5795), 4.0, 500L)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text("🇺🇸", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Submit Button mit verbesserter UX
            selectedPoint?.let { point ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ausgewählte Position:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Lat: ${"%.4f".format(point.latitude)}, Lng: ${"%.4f".format(point.longitude)}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onGuessSelected(point.latitude, point.longitude) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Vermutung bestätigen")
                        }
                    }
                }
            }
        }
    }

    // Cleanup für Memory Management
    DisposableEffect(Unit) {
        onDispose {
            mapView?.let { map ->
                map.onDetach()
                currentMarker = null
            }
        }
    }
}

// Performance-optimierte Tile Source
private fun createOptimizedTileSource(): OnlineTileSourceBase {
    val urls = arrayOf(
        "https://a.tile.openstreetmap.org/",
        "https://b.tile.openstreetmap.org/",
        "https://c.tile.openstreetmap.org/"
    )

    return object : OnlineTileSourceBase(
        "OptimizedMapnik",
        1, 19, 256, ".png",
        urls
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
            val x = org.osmdroid.util.MapTileIndex.getX(pMapTileIndex)
            val y = org.osmdroid.util.MapTileIndex.getY(pMapTileIndex)

            val baseUrl = urls[((x + y) % urls.size)]
            return "$baseUrl$zoom/$x/$y.png"
        }
    }
}