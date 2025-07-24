package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessMapView(
    onGuessSelected: (lat: Double, lng: Double) -> Unit,
    onMapClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )
    }

    Card(
        modifier = modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Header
            TopAppBar(
                title = { Text("Wähle deine Vermutung") },
                navigationIcon = {
                    IconButton(onClick = onMapClose) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }
            )

            // Map
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(2.0)
                            controller.setCenter(GeoPoint(0.0, 0.0))

                            val mapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { point ->
                                        selectedPoint = point
                                        overlays.clear()

                                        val marker = Marker(this@apply)
                                        marker.position = point
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        overlays.add(marker)
                                        invalidate()
                                    }
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint?): Boolean = false
                            }

                            overlays.add(MapEventsOverlay(mapEventsReceiver))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Submit Button
            selectedPoint?.let { point ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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

                        Spacer(modifier = Modifier.height(8.dp))

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
}