package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun InteractiveStreetView(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onPan: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Performance-optimierte State Management
    var currentHeading by remember { mutableIntStateOf(0) }
    var currentPitch by remember { mutableIntStateOf(0) }
    var currentZoom by remember { mutableFloatStateOf(90f) }
    var currentLocation by remember { mutableStateOf(extractLocationFromUrl(imageUrl)) }

    // Debouncing für Performance
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    var pendingUpdate by remember { mutableStateOf(false) }

    // Gesture State mit Optimierung
    var dragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember { mutableStateOf(false) }
    var lastImageUrl by remember { mutableStateOf("") }

    // Debounced URL Update für bessere Performance
    val streetViewUrl = remember(imageUrl, currentHeading, currentPitch, currentZoom, currentLocation) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > 100) { // 100ms Debounce
            lastUpdateTime = currentTime
            buildOptimizedStreetViewUrl(
                baseUrl = imageUrl,
                location = currentLocation,
                heading = currentHeading,
                pitch = currentPitch,
                fov = currentZoom.toInt()
            ).also { lastImageUrl = it }
        } else {
            lastImageUrl
        }
    }

    // Performance-optimierte Image Loading
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(streetViewUrl)
            .size(Size.ORIGINAL)
            .crossfade(200) // Reduzierte Crossfade Zeit
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    )

    Box(modifier = modifier.fillMaxSize()) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                LoadingIndicator()
            }
            is AsyncImagePainter.State.Success -> {
                PerformantStreetViewCanvas(
                    painter = painter,
                    currentZoom = currentZoom,
                    onZoomChange = { newZoom ->
                        currentZoom = newZoom.coerceIn(20f, 120f)
                    },
                    onDragAccumulator = { offset ->
                        dragAccumulator += offset
                    },
                    onDragEnd = {
                        scope.launch {
                            if (!pendingUpdate) {
                                pendingUpdate = true
                                delay(50) // Debounce drag updates

                                handleOptimizedDragEnd(
                                    dragAccumulator = dragAccumulator,
                                    sensitivity = with(density) { 2.dp.toPx() },
                                    onHeadingChange = { newHeading ->
                                        currentHeading = normalizeHeading(newHeading)
                                        onPan(newHeading.toFloat())
                                    },
                                    onPitchChange = { newPitch ->
                                        currentPitch = newPitch.coerceIn(-90, 90)
                                    }
                                )
                                dragAccumulator = Offset.Zero
                                pendingUpdate = false
                            }
                        }
                    },
                    onTap = { offset ->
                        // Optional: Movement Funktionalität
                        scope.launch {
                            if (!isLoading) {
                                isLoading = true
                                try {
                                    moveForwardOptimized(
                                        currentLocation = currentLocation,
                                        heading = currentHeading,
                                        stepSize = 15.0
                                    ) { newLocation ->
                                        currentLocation = newLocation
                                    }
                                } finally {
                                    delay(500)
                                    isLoading = false
                                }
                            }
                        }
                    }
                )

                // Loading Overlay
                if (isLoading) {
                    LoadingOverlay()
                }

                // Navigation HUD
                StreetViewHUD(
                    heading = currentHeading,
                    pitch = currentPitch,
                    zoom = currentZoom,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            is AsyncImagePainter.State.Error -> {
                // Fallback für normale Bilder
                FallbackImageView(
                    imageUrl = imageUrl,
                    onPan = onPan,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun PerformantStreetViewCanvas(
    painter: AsyncImagePainter,
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onDragAccumulator: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTap: (Offset) -> Unit
) {
    val imageBitmap = remember(painter) {
        (painter.state as AsyncImagePainter.State.Success).result.drawable.let {
            (it as android.graphics.drawable.BitmapDrawable).bitmap
        }.asImageBitmap()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput("zoom") {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            onZoomChange(currentZoom / zoom)
                        }
                        if (pan != Offset.Zero) {
                            onDragAccumulator(pan)
                        }
                    }
                )
            }
            .pointerInput("drag") {
                detectDragGestures(
                    onDragEnd = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onDragAccumulator(dragAmount)
                }
            }
            .pointerInput("tap") {
                detectTapGestures(onTap = onTap)
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val imageWidth = imageBitmap.width.toFloat()
        val imageHeight = imageBitmap.height.toFloat()

        // Optimierte Skalierung
        val baseScale = maxOf(
            canvasWidth / imageWidth,
            canvasHeight / imageHeight
        )

        val zoomFactor = 90f / currentZoom
        val finalScale = baseScale * zoomFactor

        val offsetX = (canvasWidth - imageWidth * finalScale) / 2
        val offsetY = (canvasHeight - imageHeight * finalScale) / 2

        // Hardware-beschleunigte Transformation
        scale(zoomFactor) {
            translate(offsetX / zoomFactor, offsetY / zoomFactor) {
                drawImage(
                    image = imageBitmap,
                    dstSize = IntSize(
                        (imageWidth * baseScale).toInt(),
                        (imageHeight * baseScale).toInt()
                    )
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Lade neue Ansicht...")
            }
        }
    }
}

@Composable
private fun StreetViewHUD(
    heading: Int,
    pitch: Int,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Richtung: ${heading}°",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Neigung: ${pitch}°",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Zoom: ${(90f / zoom * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FallbackImageView(
    imageUrl: String,
    onPan: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Einfache Fallback-Implementierung für normale Bilder
    Box(modifier = modifier) {
        Text(
            text = "Normale Bildansicht",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// Utility Functions
private fun extractLocationFromUrl(imageUrl: String): StreetViewLocation {
    val locationRegex = Regex("location=([^&]+)")
    val match = locationRegex.find(imageUrl)

    return if (match != null) {
        val coords = match.groupValues[1].split(",")
        if (coords.size == 2) {
            StreetViewLocation(
                latitude = coords[0].toDoubleOrNull() ?: 48.8566,
                longitude = coords[1].toDoubleOrNull() ?: 2.3522
            )
        } else {
            StreetViewLocation(48.8566, 2.3522) // Paris fallback
        }
    } else {
        StreetViewLocation(48.8566, 2.3522) // Paris fallback
    }
}

private fun buildOptimizedStreetViewUrl(
    baseUrl: String,
    location: StreetViewLocation,
    heading: Int,
    pitch: Int,
    fov: Int
): String {
    return if (baseUrl.contains("maps.googleapis.com")) {
        val apiKeyMatch = Regex("key=([^&]+)").find(baseUrl)
        val apiKey = apiKeyMatch?.groupValues?.get(1) ?: ""

        "https://maps.googleapis.com/maps/api/streetview?" +
                "size=1024x1024" + // Höhere Auflösung für bessere Qualität
                "&location=${location.latitude},${location.longitude}" +
                "&heading=$heading" +
                "&pitch=$pitch" +
                "&fov=$fov" +
                "&key=$apiKey"
    } else {
        baseUrl
    }
}

private fun normalizeHeading(heading: Int): Int {
    return ((heading % 360) + 360) % 360
}

private fun handleOptimizedDragEnd(
    dragAccumulator: Offset,
    sensitivity: Float,
    onHeadingChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit
) {
    val dragThreshold = sensitivity * 2

    if (abs(dragAccumulator.x) > dragThreshold || abs(dragAccumulator.y) > dragThreshold) {
        val headingDelta = (dragAccumulator.x / sensitivity * 2).toInt()
        val pitchDelta = (-dragAccumulator.y / sensitivity * 2).toInt()

        onHeadingChange(headingDelta)
        onPitchChange(pitchDelta)
    }
}

private suspend fun moveForwardOptimized(
    currentLocation: StreetViewLocation,
    heading: Int,
    stepSize: Double,
    onLocationUpdate: (StreetViewLocation) -> Unit
) {
    val headingRad = Math.toRadians(heading.toDouble())
    val deltaLat = cos(headingRad) * stepSize / 111000.0 // ~111km per degree
    val deltaLng = sin(headingRad) * stepSize / (111000.0 * cos(Math.toRadians(currentLocation.latitude)))

    val newLocation = StreetViewLocation(
        latitude = currentLocation.latitude + deltaLat,
        longitude = currentLocation.longitude + deltaLng
    )

    onLocationUpdate(newLocation)
}

// Data Classes
data class StreetViewLocation(
    val latitude: Double,
    val longitude: Double
)
