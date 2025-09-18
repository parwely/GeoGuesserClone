package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = modifier) {
        // Google Map
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null)
                    mapView = this
                    getMapAsync { map ->
                        googleMap = map

                        // Configure map
                        map.apply {
                            // Set initial camera position to world view
                            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 2f))

                            // Enable map controls
                            uiSettings.apply {
                                isZoomControlsEnabled = true
                                isCompassEnabled = true
                                isMyLocationButtonEnabled = false
                                isMapToolbarEnabled = false
                            }

                            // Handle map clicks
                            setOnMapClickListener { latLng ->
                                // Clear previous markers
                                clear()

                                // Add new marker
                                addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title("Your Guess")
                                )

                                selectedLocation = latLng
                            }
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
                    Lifecycle.Event.ON_CREATE -> mapView?.onCreate(null)
                    Lifecycle.Event.ON_START -> mapView?.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                    Lifecycle.Event.ON_STOP -> mapView?.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView?.onDestroy()
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
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap on the map to make your guess",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onMapClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Map"
                    )
                }
            }
        }

        // Bottom action area
        selectedLocation?.let { location ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selected Location:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${location.latitude.format(4)}, ${location.longitude.format(4)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                googleMap?.clear()
                                selectedLocation = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }

                        Button(
                            onClick = {
                                onGuessSelected(location.latitude, location.longitude)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit Guess")
                        }
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
