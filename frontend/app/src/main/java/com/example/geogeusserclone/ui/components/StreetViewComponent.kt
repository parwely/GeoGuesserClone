package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity

@Composable
fun LocationImageView(
    location: LocationEntity?,
    timeRemaining: Long,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (location != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(location.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Location Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Overlay with controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Timer
                TimerDisplay(
                    timeRemaining = timeRemaining,
                    modifier = Modifier.align(Alignment.End)
                )

                // Map Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(
                        onClick = onMapClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open Map"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerDisplay(
    timeRemaining: Long,
    modifier: Modifier = Modifier
) {
    val minutes = (timeRemaining / 1000) / 60
    val seconds = (timeRemaining / 1000) % 60

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (timeRemaining < 10000) Color.Red.copy(alpha = 0.9f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "%d:%02d".format(minutes, seconds),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}