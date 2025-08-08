package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.*

/**
 * 360° StreetView Component mit Drag-to-Pan Funktionalität
 */
@Composable
fun StreetViewComponent(
    imageUrl: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var isPanning by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 360° Image mit Pan-Funktionalität
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "360° Street View",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isPanning = true },
                        onDragEnd = { isPanning = false }
                    ) { change, dragAmount ->
                        // Horizontale Bewegung = Rotation
                        rotation += dragAmount.x * 0.5f

                        // Vertikale Bewegung = Zoom (begrenzt)
                        val deltaY = -dragAmount.y
                        zoom = (zoom + deltaY * 0.001f).coerceIn(0.8f, 3.0f)
                    }
                }
        )

        // Navigation Overlay
        StreetViewOverlay(
            rotation = rotation,
            zoom = zoom,
            isPanning = isPanning,
            onNavigationClick = onNavigationClick,
            modifier = Modifier.fillMaxSize()
        )

        // Controls
        StreetViewControls(
            rotation = rotation,
            zoom = zoom,
            onResetView = {
                rotation = 0f
                zoom = 1f
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun StreetViewOverlay(
    rotation: Float,
    zoom: Float,
    isPanning: Boolean,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Compass
        StreetViewCompass(
            rotation = rotation,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(60.dp)
        )

        // Center Crosshair
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val crosshairColor = if (isPanning) Color.Yellow else Color.White

            // Horizontal line
            drawLine(
                color = crosshairColor,
                start = Offset(center.x - 12.dp.toPx(), center.y),
                end = Offset(center.x + 12.dp.toPx(), center.y),
                strokeWidth = 2.dp.toPx()
            )

            // Vertical line
            drawLine(
                color = crosshairColor,
                start = Offset(center.x, center.y - 12.dp.toPx()),
                end = Offset(center.x, center.y + 12.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Center dot
            drawCircle(
                color = crosshairColor,
                radius = 3.dp.toPx(),
                center = center
            )
        }

        // Navigation Button
        FloatingActionButton(
            onClick = onNavigationClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Text("📍", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun StreetViewCompass(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Compass background
        drawCircle(
            color = Color.Black.copy(alpha = 0.7f),
            radius = radius,
            center = center
        )

        drawCircle(
            color = Color.White,
            radius = radius - 2.dp.toPx(),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // North indicator (red)
        val northAngle = -rotation * PI / 180
        val northEnd = Offset(
            center.x + cos(northAngle - PI/2).toFloat() * (radius * 0.8f),
            center.y + sin(northAngle - PI/2).toFloat() * (radius * 0.8f)
        )

        drawLine(
            color = Color.Red,
            start = center,
            end = northEnd,
            strokeWidth = 3.dp.toPx()
        )

        // Cardinal directions
        val directions = listOf("N", "E", "S", "W")
        directions.forEachIndexed { index, direction ->
            val angle = (index * 90 - rotation) * PI / 180
            val textPos = Offset(
                center.x + cos(angle - PI/2).toFloat() * (radius * 0.6f),
                center.y + sin(angle - PI/2).toFloat() * (radius * 0.6f)
            )

            // Note: In production, you'd use proper text rendering
            // This is simplified for the example
        }
    }
}

@Composable
private fun StreetViewControls(
    rotation: Float,
    zoom: Float,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Reset View Button
        FloatingActionButton(
            onClick = onResetView,
            modifier = Modifier.size(40.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text("⟲", style = MaterialTheme.typography.bodyLarge)
        }

        // Zoom Level Indicator
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(zoom * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${rotation.toInt()}°",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Enhanced StreetView mit WebView für echte 360° Unterstützung
 */
@Composable
fun Enhanced360StreetView(
    imageUrl: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                android.webkit.WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true

                    // Load custom 360° viewer HTML
                    val html360 = create360ViewerHtml(imageUrl)
                    loadDataWithBaseURL(null, html360, "text/html", "UTF-8", null)

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Navigation overlay
        FloatingActionButton(
            onClick = onNavigationClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Text("📍", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun create360ViewerHtml(imageUrl: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; overflow: hidden; background: black; }
                #viewer { width: 100vw; height: 100vh; }
                img { width: 100%; height: 100%; object-fit: cover; }
            </style>
        </head>
        <body>
            <div id="viewer">
                <img src="$imageUrl" alt="360 Street View" id="panorama">
            </div>
            
            <script>
                let isDragging = false;
                let lastX = 0;
                let lastY = 0;
                let rotationX = 0;
                let rotationY = 0;
                
                const img = document.getElementById('panorama');
                
                function handleStart(e) {
                    isDragging = true;
                    const touch = e.touches ? e.touches[0] : e;
                    lastX = touch.clientX;
                    lastY = touch.clientY;
                }
                
                function handleMove(e) {
                    if (!isDragging) return;
                    e.preventDefault();
                    
                    const touch = e.touches ? e.touches[0] : e;
                    const deltaX = touch.clientX - lastX;
                    const deltaY = touch.clientY - lastY;
                    
                    rotationY += deltaX * 0.5;
                    rotationX -= deltaY * 0.5;
                    
                    // Limit vertical rotation
                    rotationX = Math.max(-90, Math.min(90, rotationX));
                    
                    img.style.transform = 'rotateX(' + rotationX + 'deg) rotateY(' + rotationY + 'deg)';
                    
                    lastX = touch.clientX;
                    lastY = touch.clientY;
                }
                
                function handleEnd() {
                    isDragging = false;
                }
                
                // Mouse events
                img.addEventListener('mousedown', handleStart);
                document.addEventListener('mousemove', handleMove);
                document.addEventListener('mouseup', handleEnd);
                
                // Touch events
                img.addEventListener('touchstart', handleStart);
                document.addEventListener('touchmove', handleMove);
                document.addEventListener('touchend', handleEnd);
                
                // Prevent context menu
                img.addEventListener('contextmenu', e => e.preventDefault());
            </script>
        </body>
        </html>
    """.trimIndent()
}
