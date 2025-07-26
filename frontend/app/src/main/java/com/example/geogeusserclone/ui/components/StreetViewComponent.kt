package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import kotlin.math.*

@Composable
fun LocationImageView(
    location: LocationEntity?,
    timeRemaining: Long,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf(ImageViewState()) }
    var isLoading by remember { mutableStateOf(true) }
    var isPanorama by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (location != null) {
            // Street View Image mit Touch-Kontrollen
            StreetViewImage(
                imageUrl = location.imageUrl,
                imageState = imageState,
                onImageStateChange = { imageState = it },
                onLoadingStateChange = { isLoading = it },
                onPanoramaDetected = { isPanorama = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Loading state
            LoadingView(modifier = Modifier.fillMaxSize())
        }

        // Timer overlay (top left)
        TimerOverlay(
            timeRemaining = timeRemaining,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Zoom controls (right side)
        if (!isLoading && isPanorama) {
            ZoomControls(
                zoomLevel = imageState.scale,
                onZoomIn = {
                    imageState = imageState.copy(scale = (imageState.scale * 1.2f).coerceAtMost(3f))
                },
                onZoomOut = {
                    imageState = imageState.copy(scale = (imageState.scale / 1.2f).coerceAtLeast(0.5f))
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // Navigation compass (if panorama)
        if (!isLoading && isPanorama) {
            NavigationCompass(
                rotation = imageState.rotation,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        // Map button (bottom right)
        FloatingActionButton(
            onClick = onMapClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Open Map",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Location info overlay (bottom left)
        LocationInfoOverlay(
            location = location,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun StreetViewImage(
    imageUrl: String,
    imageState: ImageViewState,
    onImageStateChange: (ImageViewState) -> Unit,
    onLoadingStateChange: (Boolean) -> Unit,
    onPanoramaDetected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        onState = { state ->
            when (state) {
                is AsyncImagePainter.State.Loading -> {
                    onLoadingStateChange(true)
                }
                is AsyncImagePainter.State.Success -> {
                    onLoadingStateChange(false)
                    // Einfache Heuristik für Panorama-Erkennung
                    val intrinsicSize = state.painter.intrinsicSize
                    val aspectRatio = intrinsicSize.width / intrinsicSize.height
                    onPanoramaDetected(aspectRatio > 1.8f) // Panorama wenn sehr breit
                }
                is AsyncImagePainter.State.Error -> {
                    onLoadingStateChange(false)
                    onPanoramaDetected(false)
                }
                else -> {}
            }
        }
    )

    AsyncImage(
        model = imageUrl,
        contentDescription = "Location to guess",
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer(
                scaleX = imageState.scale,
                scaleY = imageState.scale,
                translationX = imageState.offsetX,
                translationY = imageState.offsetY,
                rotationZ = imageState.rotation
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val newScale = (imageState.scale * zoom).coerceIn(0.5f, 3f)
                    val maxOffsetX = (size.width * (newScale - 1)) / 2
                    val maxOffsetY = (size.height * (newScale - 1)) / 2

                    val newOffsetX = (imageState.offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                    val newOffsetY = (imageState.offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    val newRotation = (imageState.rotation + rotation) % 360f

                    onImageStateChange(
                        imageState.copy(
                            scale = newScale,
                            offsetX = newOffsetX,
                            offsetY = newOffsetY,
                            rotation = newRotation
                        )
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val maxOffsetX = (size.width * (imageState.scale - 1)) / 2
                    val maxOffsetY = (size.height * (imageState.scale - 1)) / 2

                    val newOffsetX = (imageState.offsetX + change.x).coerceIn(-maxOffsetX, maxOffsetX)
                    val newOffsetY = (imageState.offsetY + change.y).coerceIn(-maxOffsetY, maxOffsetY)

                    onImageStateChange(
                        imageState.copy(
                            offsetX = newOffsetX,
                            offsetY = newOffsetY
                        )
                    )
                }
            },
        contentScale = ContentScale.Crop,
        painter = painter
    )
}

@Composable
private fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Color.Gray.copy(alpha = 0.3f),
            RoundedCornerShape(12.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location wird geladen...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TimerOverlay(
    timeRemaining: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏰ ${formatTime(timeRemaining)}",
                color = if (timeRemaining < 10000) Color.Red else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ZoomControls(
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onZoomIn,
                enabled = zoomLevel < 3f
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    tint = Color.White
                )
            }

            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                color = Color.White,
                fontSize = 12.sp
            )

            IconButton(
                onClick = onZoomOut,
                enabled = zoomLevel > 0.5f
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun NavigationCompass(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(44.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2

                // Kompass-Kreis
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )

                // Nord-Indikator
                val northAngle = -rotation * PI / 180
                val northX = center.x + (radius * 0.8f * sin(northAngle)).toFloat()
                val northY = center.y - (radius * 0.8f * cos(northAngle)).toFloat()

                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(northX, northY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            Text(
                text = "N",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LocationInfoOverlay(
    location: LocationEntity?,
    modifier: Modifier = Modifier
) {
    location?.let { loc ->
        if (!loc.country.isNullOrBlank() || loc.difficulty > 0) {
            Card(
                modifier = modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Schwierigkeit: ${getDifficultyText(loc.difficulty)}",
                        color = getDifficultyColor(loc.difficulty),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!loc.country.isNullOrBlank()) {
                        Text(
                            text = "Region: ${loc.country}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    if (!loc.city.isNullOrBlank()) {
                        Text(
                            text = "Stadt: ${loc.city}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val seconds = (timeMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "%d:%02d".format(minutes, remainingSeconds)
    } else {
        "%d".format(remainingSeconds)
    }
}

private fun getDifficultyText(difficulty: Int): String {
    return when (difficulty) {
        1 -> "Leicht"
        2 -> "Mittel"
        3 -> "Schwer"
        4 -> "Sehr schwer"
        5 -> "Extrem"
        else -> "Unbekannt"
    }
}

private fun getDifficultyColor(difficulty: Int): Color {
    return when (difficulty) {
        1 -> Color.Green
        2 -> Color.Yellow
        3 -> Color(0xFFFF9800) // Orange
        4 -> Color(0xFFFF5722) // Red-Orange
        5 -> Color.Red
        else -> Color.White
    }
}

data class ImageViewState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
)