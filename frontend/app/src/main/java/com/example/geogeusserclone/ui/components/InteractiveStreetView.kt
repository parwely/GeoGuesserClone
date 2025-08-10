package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveStreetView(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onPan: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Street View Navigation State
    var currentHeading by remember { mutableIntStateOf(0) }
    var currentPitch by remember { mutableIntStateOf(0) } // Für Hoch/Runter schauen
    var currentZoom by remember { mutableFloatStateOf(90f) } // FOV (Field of View)
    var currentLocation by remember { mutableStateOf(extractLocationFromUrl(imageUrl)) }

    // Gesture State
    var dragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember { mutableStateOf(false) }
    var transformationState by remember { mutableStateOf(TransformState()) }

    // Berechne die Street View URL mit allen Parametern
    val streetViewUrl = remember(imageUrl, currentHeading, currentPitch, currentZoom, currentLocation) {
        buildStreetViewUrl(
            baseUrl = imageUrl,
            location = currentLocation,
            heading = currentHeading,
            pitch = currentPitch,
            fov = currentZoom.toInt()
        )
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(streetViewUrl)
            .crossfade(true)
            .size(coil.size.Size.ORIGINAL)
            .build()
    )

    Box(modifier = modifier.fillMaxSize()) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is AsyncImagePainter.State.Success -> {
                val imageBitmap = (painter.state as AsyncImagePainter.State.Success).result.drawable.let {
                    (it as android.graphics.drawable.BitmapDrawable).bitmap
                }.asImageBitmap()

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Multi-Touch Gesture Detection
                            detectTransformGestures(
                                onGesture = { _, pan, zoom, _ ->
                                    // Zoom (Pinch) Gesture
                                    if (zoom != 1f) {
                                        currentZoom = (currentZoom / zoom).coerceIn(20f, 120f)
                                    }

                                    // Pan Gesture für Look-Around
                                    if (pan != Offset.Zero) {
                                        dragAccumulator += pan
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            // Tap Gesture für Movement
                            detectTapGestures(
                                onTap = { offset ->
                                    scope.launch {
                                        moveForward(
                                            currentLocation = currentLocation,
                                            heading = currentHeading,
                                            stepSize = 10.0 // Meter
                                        ) { newLocation ->
                                            currentLocation = newLocation
                                            isLoading = true
                                            // Reset nach Bewegung
                                            scope.launch {
                                                kotlinx.coroutines.delay(1000)
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            // Separate Drag Detection für Präzise Kontrolle
                            detectDragGestures(
                                onDragEnd = {
                                    handleDragEnd(
                                        dragAccumulator = dragAccumulator,
                                        onHeadingChange = { newHeading ->
                                            currentHeading = newHeading
                                            onPan(newHeading.toFloat())
                                        },
                                        onPitchChange = { newPitch ->
                                            currentPitch = newPitch
                                        }
                                    )
                                    dragAccumulator = Offset.Zero
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val imageWidth = imageBitmap.width
                    val imageHeight = imageBitmap.height

                    // Angepasste Skalierung für Zoom
                    val baseScale = maxOf(
                        canvasWidth / imageWidth.toFloat(),
                        canvasHeight / imageHeight.toFloat()
                    )

                    // Zoom-Effekt durch FOV-Simulation
                    val zoomFactor = 90f / currentZoom // Inverse FOV für Zoom
                    val finalScale = baseScale * zoomFactor

                    val scaledWidth = imageWidth * finalScale
                    val scaledHeight = imageHeight * finalScale

                    // Zentriere das Bild mit Zoom-Offset
                    val offsetX = (canvasWidth - scaledWidth) / 2
                    val offsetY = (canvasHeight - scaledHeight) / 2

                    // Zeichne mit Transform
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

                // Loading Overlay während Navigation
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Navigation HUD
                StreetViewNavigationHUD(
                    currentHeading = currentHeading,
                    currentPitch = currentPitch,
                    currentZoom = currentZoom,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            is AsyncImagePainter.State.Error -> {
                // Fallback für normale Bilder ohne Street View
                NormalImageView(
                    imageUrl = imageUrl,
                    onPan = onPan,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {}
        }
    }
}

// Data Classes für Navigation
data class StreetViewLocation(
    val latitude: Double,
    val longitude: Double
)

data class TransformState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

// Street View URL Builder mit erweiterten Parametern
private fun buildStreetViewUrl(
    baseUrl: String,
    location: StreetViewLocation,
    heading: Int,
    pitch: Int,
    fov: Int
): String {
    return if (baseUrl.contains("maps.googleapis.com/maps/api/streetview")) {
        // Extrahiere API Key
        val apiKeyMatch = Regex("key=([^&]+)").find(baseUrl)
        val apiKey = apiKeyMatch?.groupValues?.get(1) ?: ""

        "https://maps.googleapis.com/maps/api/streetview?" +
                "size=800x600" +
                "&location=${location.latitude},${location.longitude}" +
                "&heading=$heading" +
                "&pitch=$pitch" +
                "&fov=$fov" +
                "&key=$apiKey"
    } else {
        baseUrl // Fallback für normale Bilder
    }
}

// Extrahiert Location aus einer Street View URL
private fun extractLocationFromUrl(url: String): StreetViewLocation {
    val locationMatch = Regex("location=([^&]+)").find(url)
    return if (locationMatch != null) {
        val coords = locationMatch.groupValues[1].split(",")
        if (coords.size >= 2) {
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

// Bewegung in Street View simulieren
private suspend fun moveForward(
    currentLocation: StreetViewLocation,
    heading: Int,
    stepSize: Double, // in Metern
    onLocationChanged: (StreetViewLocation) -> Unit
) {
    // Konvertiere Heading zu Radiant
    val headingRad = Math.toRadians(heading.toDouble())

    // Berechne neue Position (vereinfachte Bewegung)
    val earthRadius = 6371000.0 // Meter
    val deltaLat = stepSize * cos(headingRad) / earthRadius * (180.0 / Math.PI)
    val deltaLng = stepSize * sin(headingRad) / (earthRadius * cos(Math.toRadians(currentLocation.latitude))) * (180.0 / Math.PI)

    val newLocation = StreetViewLocation(
        latitude = currentLocation.latitude + deltaLat,
        longitude = currentLocation.longitude + deltaLng
    )

    onLocationChanged(newLocation)
}

// Behandle Drag-Gesten für Look-Around
private fun handleDragEnd(
    dragAccumulator: Offset,
    onHeadingChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit
) {
    val sensitivity = 0.5f

    // Horizontaler Drag = Heading ändern (links/rechts schauen)
    val headingChange = (dragAccumulator.x * sensitivity).toInt()
    if (kotlin.math.abs(headingChange) >= 5) {
        val newHeading = (360 - headingChange + 360) % 360
        onHeadingChange(newHeading)
    }

    // Vertikaler Drag = Pitch ändern (hoch/runter schauen)
    val pitchChange = (dragAccumulator.y * sensitivity * -1).toInt() // Invertiert
    if (kotlin.math.abs(pitchChange) >= 5) {
        val newPitch = (pitchChange).coerceIn(-90, 90)
        onPitchChange(newPitch)
    }
}

// Navigation HUD Component
@Composable
private fun StreetViewNavigationHUD(
    currentHeading: Int,
    currentPitch: Int,
    currentZoom: Float,
    modifier: Modifier = Modifier
) {
    // Debug/Info Overlay (kann später entfernt werden)
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Richtung: ${currentHeading}°",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Neigung: ${currentPitch}°",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Zoom: ${currentZoom.toInt()}°",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "💡 Tippen = Vorwärts",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "👆 Ziehen = Umschauen",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "🤏 Kneifen = Zoom",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun NormalImageView(
    imageUrl: String,
    onPan: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build()
    )

    var offsetX by remember { mutableFloatStateOf(0f) }

    when (painter.state) {
        is AsyncImagePainter.State.Success -> {
            val imageBitmap = (painter.state as AsyncImagePainter.State.Success).result.drawable.let {
                (it as android.graphics.drawable.BitmapDrawable).bitmap
            }.asImageBitmap()

            Canvas(
                modifier = modifier
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            onPan(dragAmount.x)
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val imageWidth = imageBitmap.width
                val imageHeight = imageBitmap.height

                val scale = canvasHeight / imageHeight.toFloat()
                val scaledWidth = imageWidth * scale
                val wrappedOffsetX = offsetX.mod(scaledWidth)

                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(wrappedOffsetX.toInt(), 0),
                    dstSize = IntSize(scaledWidth.toInt(), canvasHeight.toInt())
                )

                if (wrappedOffsetX > 0) {
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset((wrappedOffsetX - scaledWidth).toInt(), 0),
                        dstSize = IntSize(scaledWidth.toInt(), canvasHeight.toInt())
                    )
                } else {
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset((wrappedOffsetX + scaledWidth).toInt(), 0),
                        dstSize = IntSize(scaledWidth.toInt(), canvasHeight.toInt())
                    )
                }
            }
        }
        is AsyncImagePainter.State.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            // Error handling
        }
    }
}

// Helfer für Modulo-Operation bei Floats
fun Float.mod(other: Float): Float {
    return ((this % other) + other) % other
}
