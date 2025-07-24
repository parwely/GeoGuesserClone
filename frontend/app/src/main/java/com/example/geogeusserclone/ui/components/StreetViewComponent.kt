package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity

@Composable
fun LocationImageView(
    location: LocationEntity?,
    timeRemaining: Long,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Location Image
        if (location != null) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(location.imageUrl)
                    .crossfade(true)
                    .build()
            )

            Image(
                painter = painter,
                contentDescription = "Location to guess",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Timer overlay (top left)
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
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
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
        location?.let { loc ->
            if (!loc.country.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Difficulty: ${getDifficultyText(loc.difficulty)}",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        if (!loc.country.isNullOrBlank()) {
                            Text(
                                text = "Region: ${loc.country}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
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