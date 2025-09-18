package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Fallback-Komponente für Emulator-Situationen, wo normale Map-Clicks nicht funktionieren
 * Bietet alternative Eingabemethoden für Koordinaten
 */
@Composable
fun EmulatorFallbackMapComponent(
    onLocationSelected: (Double, Double) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var showManualInput by remember { mutableStateOf(false) }

    // Bekannte Locations für schnelle Auswahl
    val presetLocations = listOf(
        "Berlin, Germany" to Pair(52.5200, 13.4050),
        "New York, USA" to Pair(40.7128, -74.0060),
        "Tokyo, Japan" to Pair(35.6762, 139.6503),
        "London, UK" to Pair(51.5074, -0.1278),
        "Paris, France" to Pair(48.8566, 2.3522),
        "Sydney, Australia" to Pair(-33.8688, 151.2093),
        "Cairo, Egypt" to Pair(30.0444, 31.2357),
        "São Paulo, Brazil" to Pair(-23.5505, -46.6333),
        "Mumbai, India" to Pair(19.0760, 72.8777),
        "Random Location" to Pair((Math.random() * 180 - 90), (Math.random() * 360 - 180))
    )

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🔧 Emulator-Fallback",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Alternative Eingabemethoden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Warnung
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Map-Clicks funktionieren nicht im Emulator. Wähle eine Alternative:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preset-Locations
            Text(
                text = "📍 Bekannte Locations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetLocations.size) { index ->
                    val (name, coords) = presetLocations[index]
                    val isSelected = selectedPreset == name

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPreset = name
                                onLocationSelected(coords.first, coords.second)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${coords.first.format(4)}, ${coords.second.format(4)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Ausgewählt",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Auswählen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manuelle Eingabe Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Manuelle Koordinaten-Eingabe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = showManualInput,
                    onCheckedChange = { showManualInput = it }
                )
            }

            // Manuelle Eingabe
            if (showManualInput) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Latitude Input
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude (-90 bis 90)") },
                            placeholder = { Text("z.B. 52.5200") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Longitude Input
                        OutlinedTextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text("Longitude (-180 bis 180)") },
                            placeholder = { Text("z.B. 13.4050") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button für manuelle Eingabe
                        Button(
                            onClick = {
                                val lat = latText.toDoubleOrNull()
                                val lng = lngText.toDoubleOrNull()

                                if (lat != null && lng != null &&
                                    lat >= -90 && lat <= 90 &&
                                    lng >= -180 && lng <= 180) {
                                    onLocationSelected(lat, lng)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = run {
                                val lat = latText.toDoubleOrNull()
                                val lng = lngText.toDoubleOrNull()
                                lat != null && lng != null &&
                                lat >= -90 && lat <= 90 &&
                                lng >= -180 && lng <= 180
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Koordinaten verwenden")
                        }

                        // Validation feedback
                        val lat = latText.toDoubleOrNull()
                        val lng = lngText.toDoubleOrNull()

                        if (latText.isNotEmpty() && (lat == null || lat !in -90.0..90.0)) {
                            Text(
                                text = "⚠️ Latitude muss zwischen -90 und 90 liegen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (lngText.isNotEmpty() && (lng == null || lng !in -180.0..180.0)) {
                            Text(
                                text = "⚠️ Longitude muss zwischen -180 und 180 liegen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Abbrechen")
                }

                if (selectedPreset != null) {
                    Button(
                        onClick = {
                            val coords = presetLocations.find { it.first == selectedPreset }?.second
                            coords?.let { onLocationSelected(it.first, it.second) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Verwenden")
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
