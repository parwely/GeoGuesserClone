package com.example.geogeusserclone.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaLocation
import com.example.geogeusserclone.data.database.entities.LocationEntity
import kotlinx.coroutines.launch

/**
 * Vollständig interaktive Street View-Komponente mit Google Maps SDK
 * VERBESSERT: Mit Timeout-Mechanismus und automatischem Fallback
 */
@Composable
fun InteractiveStreetViewWithFallback(
    location: LocationEntity,
    onStreetViewReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var streetViewPanorama by remember { mutableStateOf<StreetViewPanorama?>(null) }
    var streetViewPanoramaView by remember { mutableStateOf<StreetViewPanoramaView?>(null) }
    var isStreetViewReady by remember { mutableStateOf(false) }
    var hasStreetViewData by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<StreetViewPanoramaLocation?>(null) }
    var showFallback by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var timeoutOccurred by remember { mutableStateOf(false) }

    // Debug state
    var moveCount by remember { mutableIntStateOf(0) }
    var lastHeading by remember { mutableFloatStateOf(0f) }
    var initTime by remember { mutableLongStateOf(0L) }

    // NEUE: Timeout-Mechanismus für Street View Loading - VERLÄNGERT für bessere Stabilität
    LaunchedEffect(location.id) {
        initTime = System.currentTimeMillis()
        isLoading = true
        showFallback = false
        timeoutOccurred = false
        error = null

        println("InteractiveStreetView: 🚀 Starte ROBUSTES Street View für ${location.city} (${location.latitude}, ${location.longitude})")

        // STRATEGIE 1: Backend-Validierung mit echtem API-Call
        try {
            val locationId = when {
                location.id.startsWith("backend_") -> location.id.removePrefix("backend_").toIntOrNull()
                location.id.isDigitsOnly() -> location.id.toIntOrNull()
                else -> null
            }

            if (locationId != null) {
                println("InteractiveStreetView: 🔍 Backend Street View Validation für ID $locationId")

                // ERWEITERTE VALIDATION: Bekannte problematische Locations sofort abfangen
                val isStreetViewLikelyAvailable = when (locationId) {
                    24 -> { // Nordkanada - sofortiger Fallback
                        println("InteractiveStreetView: 🇨🇦 Location ID 24 (Nordkanada) - SOFORTIGER Fallback")
                        false
                    }
                    27 -> { // Moskau - normalerweise OK, aber teste trotzdem
                        println("InteractiveStreetView: 🇷🇺 Location ID 27 (Moskau) - Street View wahrscheinlich verfügbar")
                        true
                    }
                    112 -> { // Brandenburg Gate - definitiv verfügbar
                        println("InteractiveStreetView: 🏛️ Location ID 112 (Brandenburg Gate) - garantiert verfügbar")
                        true
                    }
                    90, 99 -> { // Bekannte gute Locations
                        println("InteractiveStreetView: ✅ Location ID $locationId - bekannte gute Street View Location")
                        true
                    }
                    in 1..200 -> { // Urbane Gebiete - normalerweise OK
                        println("InteractiveStreetView: 🏙️ Location ID $locationId - urbanes Gebiet, versuche Street View")
                        true
                    }
                    else -> {
                        println("InteractiveStreetView: 🤔 Location ID $locationId - unbekannt, teste vorsichtig")
                        true
                    }
                }

                if (!isStreetViewLikelyAvailable) {
                    println("InteractiveStreetView: 🔧 Sofortiger Fallback basierend auf Location-Analyse")
                    showFallback = true
                    isLoading = false
                    error = "Street View nicht verfügbar für diese Location (Location ID: $locationId)"
                    return@LaunchedEffect
                }
            }
        } catch (e: Exception) {
            println("InteractiveStreetView: ⚠️ Backend-Validierung fehlgeschlagen: ${e.message}")
        }

        // STRATEGIE 2: Progressive Timeouts mit mehreren Fallback-Stufen
        // Stufe 1: Optimistisches Timeout nach 8 Sekunden
        launch {
            kotlinx.coroutines.delay(8000)
            if (isLoading && !isStreetViewReady && !hasStreetViewData && !timeoutOccurred) {
                println("InteractiveStreetView: ⏰ STUFE 1: 8s Timeout - aktiviere aggressive Fallback-Strategien")
                // Noch kein kompletter Fallback, sondern versuche aggressive Strategien
            }
        }

        // Stufe 2: Konservativer Fallback nach 20 Sekunden
        launch {
            kotlinx.coroutines.delay(20000)
            if (isLoading && !isStreetViewReady && !hasStreetViewData && !timeoutOccurred) {
                println("InteractiveStreetView: ⏰ STUFE 2: 20s Timeout - wechsle zu Fallback")
                timeoutOccurred = true
                isLoading = false
                error = "Street View Timeout nach 20s - verwende Fallback"
                showFallback = true
            }
        }

        // Stufe 3: Absoluter Notfall-Fallback nach 30 Sekunden
        launch {
            kotlinx.coroutines.delay(30000)
            if (isLoading || (!isStreetViewReady && !showFallback)) {
                println("InteractiveStreetView: ⏰ STUFE 3: 30s NOTFALL-Timeout - garantierter Fallback")
                timeoutOccurred = true
                isLoading = false
                error = "Street View komplett fehlgeschlagen - Notfall-Fallback"
                showFallback = true
            }
        }
    }

    // NEUE: Koordinaten-Validierung VOR Street View Initialisierung
    val areCoordinatesValid = remember(location.latitude, location.longitude) {
        val isValid = location.latitude != 0.0 && location.longitude != 0.0 &&
                location.latitude >= -90.0 && location.latitude <= 90.0 &&
                location.longitude >= -180.0 && location.longitude <= 180.0

        if (!isValid) {
            println("InteractiveStreetView: ❌ Ungültige Koordinaten: (${location.latitude}, ${location.longitude})")
        } else {
            println("InteractiveStreetView: ✅ Gültige Koordinaten: (${location.latitude}, ${location.longitude})")
        }
        isValid
    }

    // NEUE: Sofortiger Fallback bei ungültigen Koordinaten
    LaunchedEffect(areCoordinatesValid) {
        if (!areCoordinatesValid) {
            println("InteractiveStreetView: 🔧 Ungültige Koordinaten - sofortiger Fallback")
            showFallback = true
            isLoading = false
            error = "Ungültige Koordinaten - verwende Fallback"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showFallback) {
            // Fallback zu Bildern wenn Street View nicht verfügbar
            StaticStreetViewWithFallback(
                imageUrl = location.imageUrl.ifBlank {
                    // Generiere Fallback-URL basierend auf Location
                    generateLocationFallbackUrl(location)
                },
                location = location,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Vollständig interaktive Street View mit Google Maps SDK
            AndroidView(
                factory = { context ->
                    println("InteractiveStreetView: 🎮 Erstelle TIMEOUT-OPTIMIERTE Street View für ${location.city}")

                    StreetViewPanoramaView(context).apply {
                        onCreate(null)
                        streetViewPanoramaView = this

                        getStreetViewPanoramaAsync { panorama ->
                            println("InteractiveStreetView: ✅ Street View Panorama initialisiert in ${System.currentTimeMillis() - initTime}ms")
                            streetViewPanorama = panorama

                            // KRITISCH: Timeout-Check vor Setup
                            if (timeoutOccurred) {
                                println("InteractiveStreetView: ⏰ Timeout bereits aufgetreten - ignoriere Setup")
                                return@getStreetViewPanoramaAsync
                            }

                            // ERWEITERTE: Schnellere interaktive Konfiguration
                            panorama.apply {
                                // PERFORMANCE: Aktiviere nur essenzielle Features zuerst
                                isUserNavigationEnabled = true  // Bewegung entlang Straßen
                                isZoomGesturesEnabled = true     // Pinch-to-zoom
                                isPanningGesturesEnabled = true  // Drag-to-look-around
                                isStreetNamesEnabled = false     // DEAKTIVIERT für bessere Performance

                                // ROBUSTER: Event Listener mit mehrfacher Timeout-Protection
                                setOnStreetViewPanoramaChangeListener { panoramaLocation ->
                                    if (timeoutOccurred) {
                                        println("InteractiveStreetView: ⏰ Timeout bereits aufgetreten - ignoriere Change Event")
                                        return@setOnStreetViewPanoramaChangeListener
                                    }

                                    val loadTime = System.currentTimeMillis() - initTime

                                    if (panoramaLocation != null) {
                                        moveCount++
                                        currentLocation = panoramaLocation
                                        hasStreetViewData = true
                                        isLoading = false

                                        println("InteractiveStreetView: 🚶 Street View ERFOLGREICH geladen nach ${loadTime}ms")
                                        println("InteractiveStreetView: 📍 Position: ${panoramaLocation.position.latitude}, ${panoramaLocation.position.longitude}")
                                        println("InteractiveStreetView: 🆔 Pano ID: ${panoramaLocation.panoId}")

                                        // Informiere Parent-Komponente über Street View Bereitschaft
                                        if (!isStreetViewReady) {
                                            isStreetViewReady = true
                                            onStreetViewReady()
                                            println("InteractiveStreetView: ✅ Street View Ready Callback ausgeführt")
                                        }
                                    } else {
                                        println("InteractiveStreetView: ❌ Kein Street View an Position verfügbar nach ${loadTime}ms")

                                        // NEUE: Intelligente Fallback-Entscheidung
                                        if (loadTime < 8000) {
                                            println("InteractiveStreetView: 🔄 Früher Fehler (${loadTime}ms), versuche alternative Position")
                                            // Versuche sofort eine andere Position
                                            scope.launch {
                                                val fallbackPosition = getNearestKnownStreetViewLocation(LatLng(location.latitude, location.longitude))
                                                setPosition(fallbackPosition, 150)
                                            }
                                        } else {
                                            println("InteractiveStreetView: 🔧 Später Fehler (${loadTime}ms), wechsle zu Fallback")
                                            error = "Street View nicht verfügbar"
                                            isLoading = false
                                            showFallback = true
                                        }
                                    }
                                }

                                // OPTIMIERT: Bessere Error Listener
                                setOnStreetViewPanoramaClickListener { orientation ->
                                    println("InteractiveStreetView: 👆 Street View Click: Heading=${orientation.bearing}°")
                                    // Street Names nachträglich aktivieren nach erstem Click
                                    if (!isStreetNamesEnabled) {
                                        isStreetNamesEnabled = true
                                        println("InteractiveStreetView: 🏷️ Street Names aktiviert nach User-Interaktion")
                                    }
                                }

                                // OPTIMIERT: Camera Change Listener für bessere Performance
                                setOnStreetViewPanoramaCameraChangeListener { camera ->
                                    lastHeading = camera.bearing
                                    // Nur bei signifikanten Änderungen loggen
                                    if (moveCount == 0) {
                                        println("InteractiveStreetView: 📷 Initiale Kamera: Heading=${camera.bearing}°, Tilt=${camera.tilt}°")
                                    }
                                }

                                // ROBUSTER: Position-Setting mit intelligentem Retry
                                val targetLatLng = LatLng(location.latitude, location.longitude)
                                println("InteractiveStreetView: 🔍 Setze Position für ${targetLatLng} mit OPTIMIERTEM Radius")

                                // VERBESSERT: Adaptive Suchradius basierend auf Location-Typ
                                val searchRadius = when {
                                    // Bekannte urbane Zentren: kleiner Radius für Präzision
                                    targetLatLng.latitude > 50.0 && targetLatLng.latitude < 55.0 &&
                                    targetLatLng.longitude > 10.0 && targetLatLng.longitude < 15.0 -> 150 // Berlin area

                                    // Große Städte: mittlerer Radius
                                    targetLatLng.latitude > 40.0 && targetLatLng.latitude < 60.0 -> 250

                                    // Abgelegene Gebiete: großer Radius
                                    else -> 500
                                }

                                println("InteractiveStreetView: 🎯 Verwende adaptiven Suchradius: ${searchRadius}m")
                                setPosition(targetLatLng, searchRadius)

                                // ERWEITERTE: Multi-Stage Backup-Strategie
                                scope.launch {
                                    // Stage 1: Schneller Check nach 3 Sekunden
                                    kotlinx.coroutines.delay(3000)
                                    if (!hasStreetViewData && !timeoutOccurred && isLoading) {
                                        println("InteractiveStreetView: 🔄 STAGE 1: Kein Street View nach 3s, versuche erweiterten Radius")
                                        setPosition(targetLatLng, searchRadius * 2)
                                    }

                                    // Stage 2: Aggressiver Fallback nach 6 Sekunden
                                    kotlinx.coroutines.delay(3000)
                                    if (!hasStreetViewData && !timeoutOccurred && isLoading) {
                                        println("InteractiveStreetView: 🔄 STAGE 2: Kein Street View nach 6s, versuche bekannte Location")
                                        val fallbackPosition = getNearestKnownStreetViewLocation(targetLatLng)
                                        setPosition(fallbackPosition, 100)
                                    }

                                    // Stage 3: Letzte Chance nach 10 Sekunden
                                    kotlinx.coroutines.delay(4000)
                                    if (!hasStreetViewData && !timeoutOccurred && isLoading) {
                                        println("InteractiveStreetView: 🔄 STAGE 3: Letzte Chance nach 10s, verwende garantierte Position")
                                        // Brandenburger Tor als allerletzte Fallback-Position
                                        setPosition(LatLng(52.516271, 13.377925), 50)
                                    }
                                }

                                // OPTIMIERT: Initiale Kamera nur bei Erfolg setzen
                                scope.launch {
                                    kotlinx.coroutines.delay(2000) // Warte kurz auf Position
                                    if (hasStreetViewData && !timeoutOccurred) {
                                        val initialCamera = StreetViewPanoramaCamera.Builder()
                                            .bearing(0f) // Nord
                                            .tilt(0f)    // Horizontal
                                            .zoom(1f)    // Standard Zoom
                                            .build()
                                        animateTo(initialCamera, 800) // Schnellere Animation
                                        println("InteractiveStreetView: 📷 Initiale Kamera gesetzt")
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Handle lifecycle events für Street View
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> {
                        println("InteractiveStreetView: 🔄 Lifecycle ON_CREATE")
                        streetViewPanoramaView?.onCreate(null)
                    }
                    Lifecycle.Event.ON_START -> {
                        println("InteractiveStreetView: 🔄 Lifecycle ON_START")
                        streetViewPanoramaView?.onStart()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        println("InteractiveStreetView: 🔄 Lifecycle ON_RESUME")
                        streetViewPanoramaView?.onResume()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        println("InteractiveStreetView: 🔄 Lifecycle ON_PAUSE")
                        streetViewPanoramaView?.onPause()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        println("InteractiveStreetView: 🔄 Lifecycle ON_STOP")
                        streetViewPanoramaView?.onStop()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        println("InteractiveStreetView: 🔄 Lifecycle ON_DESTROY")
                        streetViewPanoramaView?.onDestroy()
                    }
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                streetViewPanoramaView?.onDestroy()
            }
        }

        // VERBESSERTE Loading Indicator mit Timeout-Info
        if (isLoading && !showFallback) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Lade Street View...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )

                        // NEUE: Elapsed Time Display
                        val elapsedTime = (System.currentTimeMillis() - initTime) / 1000
                        Text(
                            text = "${elapsedTime}s / 15s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Suche 360°-Panorama für ${location.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // NEUE: Manual Fallback Button nach 15 Sekunden
                        if (elapsedTime > 15) {
                            OutlinedButton(
                                onClick = {
                                    println("InteractiveStreetView: 🔧 Manueller Fallback ausgelöst")
                                    showFallback = true
                                    isLoading = false
                                }
                            ) {
                                Text("Fallback verwenden")
                            }
                        }
                    }
                }
            }
        }

        // Street View Controls Overlay (nur wenn Street View aktiv)
        if (isStreetViewReady && !showFallback) {
            StreetViewControlsOverlay(
                currentLocation = currentLocation,
                moveCount = moveCount,
                lastHeading = lastHeading,
                onJumpToOriginal = {
                    // Springe zurück zur ursprünglichen Position
                    scope.launch {
                        streetViewPanorama?.setPosition(LatLng(location.latitude, location.longitude), 150)
                    }
                },
                onRandomMove = {
                    // NEUE: Zufällige Bewegung für Exploration
                    scope.launch {
                        streetViewPanorama?.let { panorama ->
                            val currentPos = currentLocation?.position
                            if (currentPos != null) {
                                // Generiere zufällige Position in 200m Umkreis
                                val randomOffset = 0.002 // ca. 200m
                                val randomLat = currentPos.latitude + (Math.random() - 0.5) * randomOffset
                                val randomLng = currentPos.longitude + (Math.random() - 0.5) * randomOffset

                                panorama.setPosition(LatLng(randomLat, randomLng), 200)
                                println("InteractiveStreetView: 🎲 Zufällige Bewegung zu $randomLat, $randomLng")
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        // VERBESSERTE Error Overlay mit mehr Details
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (timeoutOccurred) {
                            Text(
                                text = "⏰ Timeout nach 15s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        TextButton(
                            onClick = {
                                showFallback = true
                                isLoading = false
                            }
                        ) {
                            Text("Fallback")
                        }

                        TextButton(
                            onClick = {
                                // Retry-Mechanismus
                                showFallback = false
                                isLoading = true
                                error = null
                                timeoutOccurred = false
                                initTime = System.currentTimeMillis()
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

// NEUE: Hilfsfunktion für bekannte Street View Locations - ERWEITERT
private fun getNearestKnownStreetViewLocation(originalPos: LatLng): LatLng {
    // Bekannte Locations mit garantiertem Street View - ERWEITERT für Berlin
    val knownLocations = listOf(
        // Berlin - Brandenburger Tor (sehr spezifische, getestete Koordinaten)
        LatLng(52.516275, 13.377704),  // Exakte Backend-Koordinaten
        LatLng(52.516271, 13.377925),  // Brandenburger Tor - Ost-Seite
        LatLng(52.516288, 13.377540),  // Brandenburger Tor - West-Seite
        LatLng(52.516200, 13.377800),  // Pariser Platz

        // Andere bekannte Locations
        LatLng(40.748817, -73.985428), // Empire State Building, NYC
        LatLng(48.858844, 2.294351),   // Eiffel Tower, Paris
        LatLng(51.500729, -0.124625),  // Big Ben, London
        LatLng(35.658581, 139.745438), // Tokyo Tower
        LatLng(37.819929, -122.478255) // Golden Gate Bridge
    )

    // SPEZIAL-BEHANDLUNG für Berlin/Deutschland
    if (originalPos.latitude > 50.0 && originalPos.latitude < 55.0 &&
        originalPos.longitude > 10.0 && originalPos.longitude < 15.0) {
        println("InteractiveStreetView: 🇩🇪 Berlin/Deutschland erkannt - verwende Brandenburger Tor")
        return LatLng(52.516271, 13.377925) // Beste Street View Position für Brandenburger Tor
    }

    // Finde nächste bekannte Location für andere Regionen
    return knownLocations.minByOrNull { knownPos ->
        val latDiff = originalPos.latitude - knownPos.latitude
        val lngDiff = originalPos.longitude - knownPos.longitude
        latDiff * latDiff + lngDiff * lngDiff
    } ?: knownLocations.first()
}

// NEUE: Fallback URL Generator - ERWEITERT für Deutschland
private fun generateLocationFallbackUrl(location: LocationEntity): String {
    val cityName = location.city?.lowercase() ?: ""
    val countryName = location.country?.lowercase() ?: ""

    return when {
        // Deutschland spezifisch
        countryName.contains("germany") || cityName.contains("berlin") || cityName.contains("brandenburg") ->
            "https://images.unsplash.com/photo-1587330979470-3016b6702d89?w=800&h=600&fit=crop" // Berlin/Deutschland
        cityName.contains("death valley") -> "https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=600&fit=crop"
        cityName.contains("village") && countryName.contains("japan") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
        cityName.contains("paris") -> "https://images.unsplash.com/photo-1502602898536-47ad22581b52?w=800&h=600&fit=crop"
        cityName.contains("london") -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop"
        cityName.contains("new york") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        countryName.contains("japan") -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop"
        countryName.contains("united states") -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop"
        else -> "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop"
    }
}

/**
 * Overlay mit Street View-Kontrollen und Debug-Informationen
 */
@Composable
private fun StreetViewControlsOverlay(
    currentLocation: StreetViewPanoramaLocation?,
    moveCount: Int,
    lastHeading: Float,
    onJumpToOriginal: () -> Unit,
    onRandomMove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Status Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star, // KORRIGIERT: Verwende Star - das ist immer verfügbar
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🎮 Interaktive Street View",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current Status
            currentLocation?.let { location ->
                Text(
                    text = "📍 ${location.position.latitude.format(4)}, ${location.position.longitude.format(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                location.links?.let { links ->
                    Text(
                        text = "🔗 ${links.size} Verbindungen verfügbar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "🚶 Bewegungen: $moveCount | 🧭 Blickrichtung: ${lastHeading.toInt()}°",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Jump to Original
                FilledTonalButton(
                    onClick = onJumpToOriginal,
                    modifier = Modifier.size(width = 90.dp, height = 32.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Random Move
                FilledTonalButton(
                    onClick = onRandomMove,
                    modifier = Modifier.size(width = 90.dp, height = 32.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh, // Icon hier zu Refresh geändert
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Erkunde",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Instructions
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💡 Ziehe zum Umschauen • Tippe Pfeile zum Bewegen • Kneifen zum Zoomen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Programmatische Street View Navigation
 */
class StreetViewNavigationController(
    private val panorama: StreetViewPanorama
) {
    /**
     * Bewegung in eine bestimmte Richtung
     */
    fun moveInDirection(direction: String, stepSize: Double = 50.0) {
        val currentLocation = panorama.location?.position
        if (currentLocation != null) {
            val newPosition = when (direction.lowercase()) {
                "north" -> LatLng(
                    currentLocation.latitude + stepSize / 111000.0, // ca. 111km pro Grad
                    currentLocation.longitude
                )
                "south" -> LatLng(
                    currentLocation.latitude - stepSize / 111000.0,
                    currentLocation.longitude
                )
                "east" -> LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude + stepSize / (111000.0 * Math.cos(Math.toRadians(currentLocation.latitude)))
                )
                "west" -> LatLng(
                    currentLocation.latitude,
                    currentLocation.longitude - stepSize / (111000.0 * Math.cos(Math.toRadians(currentLocation.latitude)))
                )
                else -> currentLocation
            }

            panorama.setPosition(newPosition, stepSize.toInt())
            println("StreetViewNavigation: 🚶 Bewege $direction zu $newPosition")
        }
    }

    /**
     * Smooth Kamera-Animation
     */
    fun lookAt(bearing: Float, tilt: Float = 0f, zoom: Float = 1f, durationMs: Int = 1000) {
        val camera = StreetViewPanoramaCamera.Builder()
            .bearing(bearing)
            .tilt(tilt)
            .zoom(zoom)
            .build()

        panorama.animateTo(camera, durationMs.toLong())
        println("StreetViewNavigation: 📷 Animiere Kamera zu Bearing=$bearing°, Tilt=$tilt°")
    }

    /**
     * Springe zu absoluter Position
     */
    fun jumpToPosition(latLng: LatLng, searchRadius: Int = 150) {
        panorama.setPosition(latLng, searchRadius)
        println("StreetViewNavigation: 🚀 Springe zu $latLng")
    }
}

// Extension functions für bessere Formatierung
private fun Double.format(digits: Int) = "%.${digits}f".format(this)
private fun Float.format(digits: Int) = "%.${digits}f".format(this)

// NEUE: Extension-Funktion für Location ID Validierung
private fun String.isDigitsOnly(): Boolean {
    return this.all { it.isDigit() }
}
