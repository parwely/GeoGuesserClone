package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geogeusserclone.data.database.entities.LocationEntity
import com.example.geogeusserclone.utils.Constants

@Composable
fun StreetViewComponent(
    location: LocationEntity?,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Street View URLs from backend
    val streetViewUrls = remember(location) {
        location?.let { loc ->
            listOf(
                "${Constants.BASE_URL}api/locations/${loc.id}/streetview",
                "${Constants.BASE_URL}api/locations/${loc.id}/streetview?angle=90",
                "${Constants.BASE_URL}api/locations/${loc.id}/streetview?angle=180",
                "${Constants.BASE_URL}api/locations/${loc.id}/streetview?angle=270"
            )
        } ?: emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (location != null && streetViewUrls.isNotEmpty()) {
            // Main Street View Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(streetViewUrls[currentImageIndex])
                    .crossfade(Constants.IMAGE_CROSSFADE_DURATION)
                    .build(),
                contentDescription = "Street View",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = {
                    isLoading = false
                    hasError = false
                },
                onError = {
                    isLoading = false
                    hasError = true
                }
            )

            // Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error State
            if (hasError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Bild konnte nicht geladen werden",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    hasError = false
                                    isLoading = true
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wiederholen")
                            }
                        }
                    }
                }
            }

            // Navigation Controls
            if (streetViewUrls.size > 1 && !isLoading && !hasError) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    streetViewUrls.forEachIndexed { index, _ ->
                        FloatingActionButton(
                            onClick = { currentImageIndex = index },
                            modifier = Modifier.size(40.dp),
                            containerColor = if (index == currentImageIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            }
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "N"
                                    1 -> "E"
                                    2 -> "S"
                                    3 -> "W"
                                    else -> "${index + 1}"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Map Button - Top Right
            FloatingActionButton(
                onClick = onMapClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("🗺️", style = MaterialTheme.typography.titleMedium)
            }

            // Location Info (for debugging - remove in production)
            if (location.country != null || location.city != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        location.country?.let { country ->
                            Text(
                                text = "Land: $country",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        location.city?.let { city ->
                            Text(
                                text = "Stadt: $city",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Schwierigkeit: ${location.difficulty}/5",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

        } else {
            // No location loaded
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lade Location...")
                    }
                }
            }
        }
    }
}
