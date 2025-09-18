package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Composable
fun GuessMapView(
    onGuessSelected: (Double, Double) -> Unit,
    onMapClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    // KRITISCH: Debug-State für Map-Clicks
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Box(modifier = modifier.fillMaxSize()) {
        // Google Map
        AndroidView(
            factory = { context ->
                println("GuessMapView: 🗺️ Erstelle MapView...")
                MapView(context).apply {
                    onCreate(null)
                    mapView = this

                    // KRITISCH: Stelle sicher, dass die Map Touch-Events empfangen kann
                    isClickable = true
                    isFocusable = true
                    isFocusableInTouchMode = true

                    getMapAsync { map: GoogleMap ->
                        println("GuessMapView: ✅ GoogleMap initialisiert")
                        googleMap = map
                        isMapReady = true

                        // VERBESSERTE Map-Konfiguration
                        map.apply {
                            // Setze initiale Kamera-Position
                            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 2f))

                            // KRITISCH: UI-Einstellungen für Touch-Interaktion
                            uiSettings.apply {
                                isZoomControlsEnabled = true
                                isCompassEnabled = true
                                isMyLocationButtonEnabled = false
                                isMapToolbarEnabled = false
                                isScrollGesturesEnabled = true
                                isZoomGesturesEnabled = true
                                isRotateGesturesEnabled = true
                                isTiltGesturesEnabled = true
                                // WICHTIG: Aktiviere alle Gesten
                                isIndoorLevelPickerEnabled = true
                                isZoomGesturesEnabled = true
                            }

                            // KRITISCH: Map-Click-Listener mit verbessertem Debugging
                            setOnMapClickListener { latLng: LatLng ->
                                val currentTime = System.currentTimeMillis()
                                clickCount++
                                lastClickTime = currentTime

                                println("GuessMapView: 🎯 MAP CLICK DETECTED! Count: $clickCount")
                                println("GuessMapView: 📍 Location: ${latLng.latitude}, ${latLng.longitude}")
                                println("GuessMapView: ⏰ Time: $currentTime")

                                // Clear previous markers
                                clear()

                                // Add new marker
                                addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title("Your Guess")
                                        .snippet("Lat: ${latLng.latitude.format(4)}, Lng: ${latLng.longitude.format(4)}")
                                )

                                // Update state
                                selectedLocation = latLng
                                println("GuessMapView: ✅ Marker gesetzt und State aktualisiert")
                            }

                            // ZUSÄTZLICH: Long-Click-Listener als Fallback
                            setOnMapLongClickListener { latLng: LatLng ->
                                println("GuessMapView: 🔗 LONG CLICK DETECTED als Fallback!")
                                // Verwende den gleichen Code wie bei normalem Click
                                clickCount++

                                clear()
                                addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title("Your Guess (Long Click)")
                                        .snippet("Lat: ${latLng.latitude.format(4)}, Lng: ${latLng.longitude.format(4)}")
                                )
                                selectedLocation = latLng
                            }

                            // DEBUGGING: Marker-Click-Listener
                            setOnMarkerClickListener { marker ->
                                println("GuessMapView: 🏷️ Marker clicked: ${marker.title}")
                                false // Return false um Standard-Verhalten zu erlauben
                            }

                            println("GuessMapView: ✅ Alle Map-Listener konfiguriert")
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Handle lifecycle events
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        println("GuessMapView: 🔄 Lifecycle ON_CREATE")
                        mapView?.onCreate(null)
                    }
                    Lifecycle.Event.ON_START -> {
                        println("GuessMapView: 🔄 Lifecycle ON_START")
                        mapView?.onStart()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        println("GuessMapView: 🔄 Lifecycle ON_RESUME")
                        mapView?.onResume()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        println("GuessMapView: 🔄 Lifecycle ON_PAUSE")
                        mapView?.onPause()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        println("GuessMapView: 🔄 Lifecycle ON_STOP")
                        mapView?.onStop()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        println("GuessMapView: 🔄 Lifecycle ON_DESTROY")
                        mapView?.onDestroy()
                    }
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView?.onDestroy()
            }
        }

        // Loading Indicator
        if (!isMapReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading Map...")
                        if (clickCount > 0) {
                            Text(
                                "Clicks detected: $clickCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Top bar with close button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Make Your Guess",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap anywhere on the map",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // DEBUG INFO
                    if (clickCount > 0) {
                        Text(
                            text = "Debug: $clickCount clicks detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onMapClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Map"
                    )
                }
            }
        }

        // Bottom action area - VERBESSERT
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Status Text
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (selectedLocation != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (selectedLocation != null) {
                        Column {
                            Text(
                                text = "Location Selected ✅",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedLocation!!.latitude.format(4)}, ${selectedLocation!!.longitude.format(4)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "No location selected - tap on the map",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isMapReady && clickCount == 0) {
                                Text(
                                    text = "Map ready - waiting for tap...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onMapClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    // Clear Button (only when location selected)
                    if (selectedLocation != null) {
                        OutlinedButton(
                            onClick = {
                                println("GuessMapView: 🧹 Clearing selection")
                                googleMap?.clear()
                                selectedLocation = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }
                    }

                    // VERBESSERT: Submit Button mit besserem Feedback
                    Button(
                        onClick = {
                            selectedLocation?.let { location ->
                                println("GuessMapView: ✅ Submitting guess: ${location.latitude}, ${location.longitude}")
                                onGuessSelected(location.latitude, location.longitude)
                            }
                        },
                        modifier = Modifier.weight(2f),
                        enabled = selectedLocation != null
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedLocation != null) "Submit Guess" else "Select Location First",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Helpful hint + Debug info
                Spacer(modifier = Modifier.height(8.dp))
                if (selectedLocation == null) {
                    Text(
                        text = "💡 Tip: Zoom in for better accuracy. ${if (isMapReady) "Map is ready!" else "Loading..."}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // DEBUG: Zeige Map-Status
                if (isMapReady) {
                    Text(
                        text = "🗺️ Map Status: Ready | Clicks: $clickCount | Selected: ${selectedLocation != null}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
