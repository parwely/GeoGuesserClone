package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun InteractiveStreetView(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onPan: (Float) -> Unit
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .size(coil.size.Size.ORIGINAL) // Originalgröße für Panning laden
            .build()
    )

    var offsetX by remember { mutableFloatStateOf(0f) }

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

                    // Bild skalieren, um die Höhe des Canvas auszufüllen
                    val scale = canvasHeight / imageHeight.toFloat()
                    val scaledWidth = imageWidth * scale

                    // Bild als umhüllbar für 360-Grad-Ansicht behandeln
                    val wrappedOffsetX = offsetX.mod(scaledWidth)

                    // Das Bild zeichnen, möglicherweise zweimal für den Wrap-Around-Effekt
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(wrappedOffsetX.toInt(), 0),
                        dstSize = IntSize(scaledWidth.toInt(), canvasHeight.toInt())
                    )
                    // Zweiten Teil des Bildes zeichnen, wenn es umbricht
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
            is AsyncImagePainter.State.Error -> {
                // Fehlerstatus behandeln, evtl. Platzhalter anzeigen
            }
            else -> {}
        }
    }
}

// Helfer für Modulo-Operation bei Floats
fun Float.mod(other: Float): Float {
    return ((this % other) + other) % other
}
