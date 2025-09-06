package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    onPan: (Float) -> Unit = {},
    onLocationChange: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Street View State Management
    var currentHeading by remember { mutableIntStateOf(0) }
    var currentPitch by remember { mutableIntStateOf(0) }
    var currentZoom by remember { mutableFloatStateOf(90f) }
    var currentLocation by remember { mutableStateOf(extractLocationFromUrl(imageUrl)) }
    var showControls by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    // Gesture State
    var dragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    // Auto-hide controls after inactivity
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000) // Hide after 3 seconds
            showControls = false
        }
    }

    // Debounced Street View URL - KORRIGIERT: Verwende nur Backend-URLs
    val streetViewUrl = remember(imageUrl) {
        // SICHERHEIT: Verwende NIEMALS clientseitige Street View URL Generation!
        // Alle URLs müssen vom Backend kommen
        imageUrl
    }

    // Image Painter with optimized settings
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(streetViewUrl)
            .size(Size.ORIGINAL)
            .crossfade(300)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    )

    // Get current state safely
    val painterState = painter.state

    Box(modifier = modifier.fillMaxSize()) {
        when (painterState) {
            is AsyncImagePainter.State.Loading -> {
                StreetViewLoadingIndicator()
            }
            is AsyncImagePainter.State.Success -> {
                // Main Street View Canvas with Touch Gestures
                InteractiveStreetViewCanvas(
                    painter = painter,
                    currentZoom = currentZoom,
                    onZoomChange = { newZoom ->
                        currentZoom = newZoom.coerceIn(20f, 120f)
                    },
                    onDragStart = {
                        showControls = true
                    },
                    onDragAccumulator = { offset ->
                        dragAccumulator += offset
                    },
                    onDragEnd = {
                        scope.launch {
                            handleDragGesture(
                                dragAccumulator = dragAccumulator,
                                sensitivity = with(density) { 2.dp.toPx() },
                                onHeadingChange = { deltaHeading ->
                                    currentHeading = normalizeHeading(currentHeading + deltaHeading)
                                    onPan(deltaHeading.toFloat())
                                },
                                onPitchChange = { deltaPitch ->
                                    currentPitch = (currentPitch + deltaPitch).coerceIn(-90, 90)
                                }
                            )
                            dragAccumulator = Offset.Zero
                        }
                    },
                    onDoubleTap = { offset ->
                        // Double tap to move forward
                        scope.launch {
                            moveForward(
                                currentLocation = currentLocation,
                                heading = currentHeading,
                                stepSize = 25.0,
                                isLoading = isLoading,
                                onLocationUpdate = { newLocation ->
                                    currentLocation = newLocation
                                    onLocationChange?.invoke(newLocation.latitude, newLocation.longitude)
                                },
                                onLoadingChange = { loading ->
                                    isLoading = loading
                                }
                            )
                        }
                    },
                    onSingleTap = {
                        showControls = !showControls
                    }
                )

                // Loading Overlay during movement
                if (isLoading) {
                    StreetViewLoadingOverlay()
                }

                // Street View HUD (always visible but minimal)
                StreetViewHUD(
                    heading = currentHeading,
                    pitch = currentPitch,
                    zoom = currentZoom,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Navigation Controls (show/hide based on user interaction)
                if (showControls) {
                    StreetViewNavigationControls(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onMoveForward = {
                            scope.launch {
                                moveForward(
                                    currentLocation = currentLocation,
                                    heading = currentHeading,
                                    stepSize = 25.0,
                                    isLoading = isLoading,
                                    onLocationUpdate = { newLocation ->
                                        currentLocation = newLocation
                                        onLocationChange?.invoke(newLocation.latitude, newLocation.longitude)
                                    },
                                    onLoadingChange = { loading ->
                                        isLoading = loading
                                    }
                                )
                            }
                        },
                        onMoveBackward = {
                            scope.launch {
                                moveForward(
                                    currentLocation = currentLocation,
                                    heading = normalizeHeading(currentHeading + 180),
                                    stepSize = 25.0,
                                    isLoading = isLoading,
                                    onLocationUpdate = { newLocation ->
                                        currentLocation = newLocation
                                        onLocationChange?.invoke(newLocation.latitude, newLocation.longitude)
                                    },
                                    onLoadingChange = { loading ->
                                        isLoading = loading
                                    }
                                )
                            }
                        },
                        onTurnLeft = {
                            currentHeading = normalizeHeading(currentHeading - 30)
                            onPan(-30f)
                        },
                        onTurnRight = {
                            currentHeading = normalizeHeading(currentHeading + 30)
                            onPan(30f)
                        },
                        onZoomIn = {
                            currentZoom = (currentZoom - 10f).coerceAtLeast(20f)
                        },
                        onZoomOut = {
                            currentZoom = (currentZoom + 10f).coerceAtMost(120f)
                        },
                        onLookUp = {
                            currentPitch = (currentPitch + 15).coerceAtMost(90)
                        },
                        onLookDown = {
                            currentPitch = (currentPitch - 15).coerceAtLeast(-90)
                        }
                    )
                }

                // Zoom Controls (always visible on the right)
                StreetViewZoomControls(
                    currentZoom = currentZoom,
                    onZoomIn = {
                        currentZoom = (currentZoom - 10f).coerceAtLeast(20f)
                    },
                    onZoomOut = {
                        currentZoom = (currentZoom + 10f).coerceAtMost(120f)
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            is AsyncImagePainter.State.Error -> {
                // Fallback to simple image view
                StreetViewErrorFallback(
                    originalImageUrl = imageUrl,
                    error = painterState.result.throwable.message
                )
            }
            else -> {
                StreetViewLoadingIndicator()
            }
        }
    }
}

@Composable
private fun InteractiveStreetViewCanvas(
    painter: AsyncImagePainter,
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragAccumulator: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
    onSingleTap: (Offset) -> Unit
) {
    // Safe state access
    val painterState = painter.state
    if (painterState !is AsyncImagePainter.State.Success) return

    val imageBitmap = remember(painter) {
        painterState.result.drawable.let {
            (it as android.graphics.drawable.BitmapDrawable).bitmap
        }.asImageBitmap()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput("transform") {
                detectTransformGestures(
                    panZoomLock = false
                ) { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onZoomChange(currentZoom / zoom)
                    }
                    if (pan != Offset.Zero) {
                        onDragAccumulator(pan)
                    }
                }
            }
            .pointerInput("drag") {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onDragAccumulator(dragAmount)
                }
            }
            .pointerInput("tap") {
                detectTapGestures(
                    onDoubleTap = onDoubleTap,
                    onTap = onSingleTap
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val imageWidth = imageBitmap.width.toFloat()
        val imageHeight = imageBitmap.height.toFloat()

        // Calculate optimal scaling
        val baseScale = maxOf(
            canvasWidth / imageWidth,
            canvasHeight / imageHeight
        )

        val zoomFactor = 90f / currentZoom
        val finalScale = baseScale * zoomFactor

        val offsetX = (canvasWidth - imageWidth * finalScale) / 2
        val offsetY = (canvasHeight - imageHeight * finalScale) / 2

        // Draw with hardware acceleration
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
private fun StreetViewNavigationControls(
    modifier: Modifier = Modifier,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    onTurnLeft: () -> Unit,
    onTurnRight: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLookUp: () -> Unit,
    onLookDown: () -> Unit
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Look Up/Down Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                IconButton(
                    onClick = onLookUp,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Blick nach oben")
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Navigation Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Turn Left
                IconButton(
                    onClick = onTurnLeft,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Refresh, "Links drehen")
                }

                // Move Forward
                IconButton(
                    onClick = onMoveForward,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, // Ersetze ArrowUpward durch KeyboardArrowUp
                        "Vorwärts",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Turn Right
                IconButton(
                    onClick = onTurnRight,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Refresh, "Rechts drehen")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Look Down & Backward Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move Backward
                IconButton(
                    onClick = onMoveBackward,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Rückwärts", modifier = Modifier.size(20.dp)) // Ersetze ArrowDownward
                }

                // Look Down
                IconButton(
                    onClick = onLookDown,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Blick nach unten")
                }

                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }
}

@Composable
private fun StreetViewZoomControls(
    currentZoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, "Zoom In") // Add ist verfügbar
            }

            Text(
                text = "${(90f / currentZoom * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Delete, "Zoom Out") // Ersetze Remove durch Delete
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
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn, // Ersetze MyLocation durch LocationOn
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${heading}°",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.KeyboardArrowUp, // Ersetze ArrowUpward durch KeyboardArrowUp
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pitch}°",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Lade Street View...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StreetViewLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Bewege zu neuer Position...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StreetViewErrorFallback(
    originalImageUrl: String,
    error: String?
) {
    val context = LocalContext.current

    // Smart Fallback: Versuche Unsplash-Bild basierend auf URL-Parameter
    val fallbackImageUrl = remember(originalImageUrl) {
        when {
            originalImageUrl.contains("location=48.8584") ->
                "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            originalImageUrl.contains("Paris") || originalImageUrl.contains("paris") ->
                "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
            originalImageUrl.contains("London") || originalImageUrl.contains("london") ->
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
            else ->
                "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Zeige Fallback-Bild statt Street View
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(fallbackImageUrl)
                .crossfade(300)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = "Fallback image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Error overlay mit hilfreicher Information
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Street View nicht verfügbar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Zeige Beispielbild der Region",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// Utility Functions
private fun extractLocationFromUrl(imageUrl: String): StreetViewLocation {
    val locationRegex = Regex("location=([^&]+)")
    val match = locationRegex.find(imageUrl)

    return if (match != null) {
        val coords = match.groupValues[1].split(",")
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

private fun normalizeHeading(heading: Int): Int {
    return ((heading % 360) + 360) % 360
}

private suspend fun handleDragGesture(
    dragAccumulator: Offset,
    sensitivity: Float,
    onHeadingChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit
) {
    val dragThreshold = sensitivity * 2

    if (abs(dragAccumulator.x) > dragThreshold || abs(dragAccumulator.y) > dragThreshold) {
        val headingDelta = (dragAccumulator.x / sensitivity * 3).toInt() // More sensitive
        val pitchDelta = (-dragAccumulator.y / sensitivity * 3).toInt() // More sensitive

        onHeadingChange(headingDelta)
        onPitchChange(pitchDelta)
    }
}

private suspend fun moveForward(
    currentLocation: StreetViewLocation,
    heading: Int,
    stepSize: Double,
    isLoading: Boolean,
    onLocationUpdate: (StreetViewLocation) -> Unit,
    onLoadingChange: (Boolean) -> Unit
) {
    if (isLoading) return

    onLoadingChange(true)

    try {
        val headingRad = Math.toRadians(heading.toDouble())
        val deltaLat = cos(headingRad) * stepSize / 111000.0 // ~111km per degree
        val deltaLng = sin(headingRad) * stepSize / (111000.0 * cos(Math.toRadians(currentLocation.latitude)))

        val newLocation = StreetViewLocation(
            latitude = currentLocation.latitude + deltaLat,
            longitude = currentLocation.longitude + deltaLng
        )

        // Simulate loading delay for realistic feel
        delay(500)

        onLocationUpdate(newLocation)
    } finally {
        onLoadingChange(false)
    }
}

// Data Classes
data class StreetViewLocation(
    val latitude: Double,
    val longitude: Double
)
