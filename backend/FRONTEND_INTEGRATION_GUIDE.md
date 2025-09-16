# 🚀 Frontend Integration Guide: Interactive Street View Implementation

## Übersicht

Dieses Guide zeigt dir Schritt für Schritt, wie du die neuen interaktiven Street View-Features im Frontend integrierst. Das Backend wurde komplett von statischen Street View-Bildern auf interaktive, navigierbare URLs umgestellt.

---

## 📋 Step-by-Step Integration Guide

### **✅ Backend Status: APIs bereits implementiert**

Die folgenden Backend-Endpunkte sind **bereits vollständig implementiert** und bereit für Frontend-Integration:

- ✅ `GET /api/locations/{id}/streetview/interactive` - Enhanced Interactive Street View
- ✅ `POST /api/locations/streetview/navigate` - Dynamische Navigation
- ✅ `GET /api/locations/random/enhanced` - Random Locations mit Street View-Daten
- ✅ `GET /api/locations/streetview/bulk` - Bulk Street View Loading

**Fokus:** UI-Integration und WebView-Implementation im Frontend

### **Step 1: API-Endpunkte verstehen**

Die neuen Backend-Endpunkte bieten folgende Funktionalitäten:

```typescript
// Neue API-Struktur
interface StreetViewResponse {
  success: true;
  data: {
    location: {
      id: number;
      name: string;
      coordinates: { latitude: number; longitude: number };
    };
    streetView: {
      type: "interactive" | "static";
      embedUrl: string; // ← NEUE interaktive URL
      staticFallback?: string; // ← Backup für Kompatibilität
      navigationEnabled: boolean;
      quality: "low" | "medium" | "high";
    };
  };
}
```

### **Step 2: AsyncImage durch WebView ersetzen**

**VORHER (Alt - Statisches Bild):**

```kotlin
// Android/Compose
AsyncImage(
    model = "https://maps.googleapis.com/maps/api/streetview?size=640x640&location=52.5200,13.4050&key=API_KEY",
    contentDescription = "Street View",
    modifier = Modifier.fillMaxSize()
)
```

**NACHHER (Neu - Interaktive Navigation):**

```kotlin
// Android/Compose - WebView für interaktive Street View
@Composable
fun InteractiveStreetView(embedUrl: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }
        },
        update = { webView ->
            webView.loadUrl(embedUrl)
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

### **Step 3: API-Calls aktualisieren**

**✅ BEREITS IMPLEMENTIERT - Für einzelne Locations:**

```kotlin
// Repository/Service Layer
suspend fun getInteractiveStreetView(locationId: Int): StreetViewResponse {
    return apiService.get("/api/locations/$locationId/streetview?interactive=true&quality=high")
}

// ✅ BEREITS IMPLEMENTIERT - Mit Navigation und Zoom-Kontrollen
suspend fun getEnhancedStreetView(locationId: Int, heading: Int? = null): StreetViewResponse {
    return apiService.get("/api/locations/$locationId/streetview/interactive?heading=$heading&enableNavigation=true")
}
```

**✅ BEREITS IMPLEMENTIERT - Für Random Locations mit Street View:**

```kotlin
// ✅ BEREITS IMPLEMENTIERT - Enhanced Random Locations
suspend fun getRandomEnhancedLocations(
    count: Int = 1,
    difficulty: Int? = null
): LocationResponse {
    return apiService.get("/api/locations/random/enhanced?count=$count&difficulty=$difficulty")
}

// ✅ BEREITS IMPLEMENTIERT - Bulk Street View Daten
suspend fun getBulkStreetViewData(locationIds: List<Int>): BulkStreetViewResponse {
    return apiService.get("/api/locations/streetview/bulk?ids=${locationIds.joinToString(",")}")
}
```

### **Step 4: UI-Komponenten erweitern**

**Neue StreetView-Komponente:**

```kotlin
@Composable
fun StreetViewComponent(
    locationId: Int,
    modifier: Modifier = Modifier
) {
    var streetViewData by remember { mutableStateOf<StreetViewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(locationId) {
        try {
            streetViewData = repository.getInteractiveStreetView(locationId)
            isLoading = false
        } catch (e: Exception) {
            // Fallback zu statischer URL
            streetViewData = repository.getStaticStreetView(locationId)
            isLoading = false
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> CircularProgressIndicator()
            streetViewData?.data?.streetView?.type == "interactive" -> {
                InteractiveStreetView(
                    embedUrl = streetViewData.data.streetView.embedUrl
                )
            }
            else -> {
                // Fallback zu statischem Bild
                AsyncImage(
                    model = streetViewData?.data?.streetView?.staticFallback,
                    contentDescription = "Street View",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
```

### **Step 5: Navigation implementieren**

**✅ BEREITS IMPLEMENTIERT - Für dynamische Street View Navigation:**

```kotlin
// Navigation-Request Data Class
data class NavigationRequest(
    val latitude: Double,
    val longitude: Double,
    val direction: String, // "north", "south", "east", "west", etc.
    val distance: Int = 50,
    val zoom: Double = 1.0
)

// ✅ BEREITS IMPLEMENTIERT - Navigation-Funktion
suspend fun navigateStreetView(request: NavigationRequest): StreetViewResponse {
    return apiService.post("/api/locations/streetview/navigate", request)
}

// UI mit Navigation-Controls - INTEGRATION ERFORDERLICH
@Composable
fun StreetViewWithNavigation(
    initialLatitude: Double,
    initialLongitude: Double
) {
    var currentStreetView by remember { mutableStateOf<StreetViewResponse?>(null) }
    var currentPosition by remember { mutableStateOf(Pair(initialLatitude, initialLongitude)) }

    // ✅ API bereits verfügbar - nur UI-Integration nötig
    suspend fun navigateInDirection(direction: String) {
        try {
            val navigationRequest = NavigationRequest(
                latitude = currentPosition.first,
                longitude = currentPosition.second,
                direction = direction,
                distance = 50
            )

            val newStreetView = repository.navigateStreetView(navigationRequest)
            currentStreetView = newStreetView

            // Update current position from response
            val newPos = newStreetView.data.navigation?.newPosition
            if (newPos != null) {
                currentPosition = Pair(newPos.latitude, newPos.longitude)
            }
        } catch (e: Exception) {
            // Handle navigation error
            Log.e("Navigation", "Failed to navigate: ${e.message}")
        }
    }

    Column {
        // Street View Display
        Box(modifier = Modifier.weight(1f)) {
            currentStreetView?.let { streetView ->
                InteractiveStreetView(streetView.data.streetView.embedUrl)
            }
        }

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                coroutineScope.launch { navigateInDirection("north") }
            }) { Text("⬆️") }

            Button(onClick = {
                coroutineScope.launch { navigateInDirection("south") }
            }) { Text("⬇️") }

            Button(onClick = {
                coroutineScope.launch { navigateInDirection("west") }
            }) { Text("⬅️") }

            Button(onClick = {
                coroutineScope.launch { navigateInDirection("east") }
            }) { Text("➡️") }
        }
    }
}
```

### **Step 6: Game Integration aktualisieren**

**✅ BEREITS IMPLEMENTIERT - Random Game mit Enhanced Locations:**

```kotlin
// GameViewModel/Repository
class GameRepository {

    // ✅ BEREITS IMPLEMENTIERT - Enhanced Random API
    suspend fun startNewGame(difficulty: Int): GameData {
        val locationResponse = apiService.get(
            "/api/locations/random/enhanced?count=5&difficulty=$difficulty"
        )

        return GameData(
            locations = locationResponse.data.locations.map { location ->
                GameLocation(
                    id = location.id,
                    coordinates = location.coordinates,
                    streetView = location.streetView, // Bereits in Enhanced API enthalten
                    metadata = location.metadata
                )
            }
        )
    }

    // ✅ BEREITS IMPLEMENTIERT - Bulk Street View Loading für Performance
    suspend fun loadStreetViewDataBulk(locationIds: List<Int>): List<StreetViewData> {
        val bulkResponse = apiService.get(
            "/api/locations/streetview/bulk?ids=${locationIds.joinToString(",")}"
        )
        return bulkResponse.data.streetViews
    }
}

// Game Screen - UI INTEGRATION ERFORDERLICH
@Composable
fun GameScreen(gameData: GameData) {
    val currentLocation = gameData.locations[currentLocationIndex]

    Column {
        // ✅ Street View Daten bereits verfügbar - nur UI-Integration nötig
        if (currentLocation.streetView != null) {
            InteractiveStreetView(
                embedUrl = currentLocation.streetView.embedUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        } else {
            // Fallback: Load Street View on demand
            StreetViewComponent(
                locationId = currentLocation.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }

        // Optional: Navigation Controls für Street View
        if (currentLocation.streetView?.navigationEnabled == true) {
            StreetViewNavigationControls(
                latitude = currentLocation.coordinates.latitude,
                longitude = currentLocation.coordinates.longitude
            )
        }

        // Rest der Game-UI (Map, Buttons, etc.)
        GameControls(...)
    }
}
```

### **Step 7: Error Handling & Fallbacks**

**Robuste Implementierung mit Fallback:**

```kotlin
@Composable
fun RobustStreetViewComponent(locationId: Int) {
    var streetViewState by remember { mutableStateOf<StreetViewState>(StreetViewState.Loading) }

    LaunchedEffect(locationId) {
        try {
            // Versuche interaktive URL
            val response = repository.getInteractiveStreetView(locationId)
            streetViewState = StreetViewState.Interactive(response.data.streetView.embedUrl)
        } catch (e: Exception) {
            try {
                // Fallback zu statischer URL
                val fallbackResponse = repository.getStaticStreetView(locationId)
                streetViewState = StreetViewState.Static(fallbackResponse.data.streetViewUrl)
            } catch (e: Exception) {
                streetViewState = StreetViewState.Error("Street View nicht verfügbar")
            }
        }
    }

    when (val state = streetViewState) {
        is StreetViewState.Loading -> CircularProgressIndicator()
        is StreetViewState.Interactive -> InteractiveStreetView(state.embedUrl)
        is StreetViewState.Static -> AsyncImage(model = state.staticUrl, ...)
        is StreetViewState.Error -> Text("Error: ${state.message}")
    }
}

sealed class StreetViewState {
    object Loading : StreetViewState()
    data class Interactive(val embedUrl: String) : StreetViewState()
    data class Static(val staticUrl: String) : StreetViewState()
    data class Error(val message: String) : StreetViewState()
}
```

### **Step 8: Performance-Optimierung**

**Lazy Loading für bessere Performance:**

```kotlin
// Lazy Loading von Street View-Daten
@Composable
fun LazyStreetViewGrid(locations: List<Location>) {
    LazyColumn {
        items(locations) { location ->
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                // Nur laden wenn sichtbar
                isVisible = true
            }

            if (isVisible) {
                StreetViewComponent(
                    locationId = location.id,
                    modifier = Modifier.height(200.dp)
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading Street View...")
                }
            }
        }
    }
}
```

### **Step 9: Backward Compatibility**

**Schrittweise Migration:**

```kotlin
// Feature Flag für schrittweise Rollout
class FeatureFlags {
    val useInteractiveStreetView: Boolean = true // Über Remote Config steuerbar
}

@Composable
fun CompatibleStreetView(locationId: Int) {
    val featureFlags = LocalFeatureFlags.current

    if (featureFlags.useInteractiveStreetView) {
        // Neue interaktive Implementation
        StreetViewComponent(locationId = locationId)
    } else {
        // Alte statische Implementation
        LegacyStreetView(locationId = locationId)
    }
}
```

### **Step 10: Testing**

**Unit Tests für neue API-Integration:**

```kotlin
@Test
fun testInteractiveStreetViewAPI() = runTest {
    // Mock Response
    val mockResponse = StreetViewResponse(
        success = true,
        data = StreetViewData(
            location = Location(1, "Test", Coordinates(52.5200, 13.4050)),
            streetView = StreetView(
                type = "interactive",
                embedUrl = "https://www.google.com/maps/embed/v1/streetview?...",
                staticFallback = "https://maps.googleapis.com/...",
                navigationEnabled = true,
                quality = "high"
            )
        )
    )

    coEvery { apiService.getInteractiveStreetView(1) } returns mockResponse

    val result = repository.getInteractiveStreetView(1)

    assertEquals("interactive", result.data.streetView.type)
    assertTrue(result.data.streetView.navigationEnabled)
}
```

---

## 📱 Migration Checklist

### ✅ Backend bereits implementiert:

- [x] **Enhanced API-Endpunkte**: Alle notwendigen Endpunkte sind verfügbar
- [x] **Navigation API**: POST /api/locations/streetview/navigate implementiert
- [x] **Bulk Loading**: Bulk Street View API für Performance optimiert
- [x] **Enhanced Random**: Random Locations mit Street View-Daten integriert

### 🎯 Frontend-Integration erforderlich:

- [ ] **WebView-Setup**: AndroidView für interaktive Street Views einrichten
- [ ] **API-Integration**: Bereits implementierte Endpunkte in Repository/Service einbinden
- [ ] **UI-Komponenten**: InteractiveStreetView Composables erstellen
- [ ] **Navigation UI**: Navigation-Controls für Street View implementieren

### 🚀 Erweiterte Features:

- [ ] **Bulk Loading**: Nutze `/streetview/bulk` für bessere Performance
- [ ] **Enhanced Game Flow**: Integriere `/random/enhanced` für optimierte Game-Experience
- [ ] **Quality-Settings**: Nutzer können Street View-Qualität wählen
- [ ] **Offline-Fallback**: Cached statische URLs als Backup

### 📊 Optional:

- [ ] **Custom Controls**: Eigene UI-Elemente über WebView legen
- [ ] **Analytics**: Tracking für interaktive Street View-Nutzung
- [ ] **A/B Testing**: Vergleich zwischen statischen und interaktiven Views

---

## 🔗 API-Endpunkt Referenz

### ✅ Implementierte Backend-Endpunkte:

```
GET /api/locations/{id}/streetview/interactive?enableNavigation=true&quality=high
POST /api/locations/streetview/navigate
GET /api/locations/random/enhanced?count=5&difficulty=3
GET /api/locations/streetview/bulk?ids=1,2,3,4,5
```

### 🔄 Zusätzlich verfügbare Endpunkte:

```
GET /api/locations/:id/streetview?interactive=true&quality=high (Backward Compatible)
GET /api/locations/random?includeStreetView=true&count=5 (Alternative)
```

---

## 💡 Pro-Tips

1. **WebView Performance**: Aktiviere Hardware-Beschleunigung für smooth Navigation
2. **Fallback-Strategie**: Immer staticFallback-URL als Backup bereithalten
3. **Lazy Loading**: Street View nur laden wenn tatsächlich angezeigt
4. **Cache**: Embed-URLs können gecacht werden (sind statisch für gleiche Parameter)
5. **User Experience**: Loading-States für bessere UX implementieren

---

Diese Integration ermöglicht vollständig **navigierbare Street View-Ansichten** mit Zoom, Pan und Bewegung direkt im Game - ein massives Upgrade von statischen Bildern! 🚀
