# 🎯 Frontend Integration Prompt: Interactive Street View

## Prompt für Frontend-Entwicklung

**Kontext:** Das Backend wurde von statischen Street View-Bildern auf interaktive, navigierbare URLs umgestellt. **Alle API-Endpunkte sind vollständig implementiert und produktionsbereit.**

**✅ Bereits implementierte Backend-APIs:**

- `GET /api/locations/{id}/streetview/interactive` - Enhanced Interactive Street View
- `POST /api/locations/streetview/navigate` - Dynamische Navigation
- `GET /api/locations/random/enhanced` - Random Locations mit Street View-Daten
- `GET /api/locations/streetview/bulk` - Bulk Street View Loading

**🎯 Aufgabe:** Integriere die **bereits verfügbaren** interaktiven Street View-Features im Frontend (Android/Kotlin Compose).

---

## 🔄 Was sich geändert hat

**VORHER:**

```kotlin
// Statische Street View-Bilder
AsyncImage(
    model = "https://maps.googleapis.com/maps/api/streetview?size=640x640&location=...",
    contentDescription = "Street View"
)
```

**NACHHER:**

```kotlin
// Interaktive, navigierbare Street View
AndroidView(
    factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
        }
    },
    update = { webView ->
        webView.loadUrl(embedUrl) // ← Neue interaktive URL
    }
)
```

---

## 📡 Verfügbare API-Struktur

### ✅ Implementierte Enhanced APIs:

#### 1. Enhanced Interactive Street View:

```http
GET /api/locations/{id}/streetview/interactive?enableNavigation=true&quality=high
```

#### 2. Dynamische Navigation:

```http
POST /api/locations/streetview/navigate
Content-Type: application/json

{
  "latitude": 52.5200,
  "longitude": 13.4050,
  "direction": "north",
  "distance": 50
}
```

#### 3. Enhanced Random Locations:

```http
GET /api/locations/random/enhanced?count=5&difficulty=3
```

#### 4. Bulk Street View Loading:

```http
GET /api/locations/streetview/bulk?ids=1,2,3,4,5
```

### Response-Struktur (Enhanced):

```json
{
  "success": true,
  "data": {
    "location": { "id": 123, "coordinates": {...} },
    "interactive": {
      "embedUrl": "https://www.google.com/maps/embed/v1/streetview?navigation=1&controls=1&location=...",
      "navigationEnabled": true,
      "controls": { "pan": true, "zoom": true, "compass": true },
      "fallback": { "staticUrl": "https://maps.googleapis.com/..." }
    },
    "metadata": {
      "quality": "high",
      "supportsNavigation": true,
      "recommendedFor": ["webview", "iframe"]
    }
  }
}
```

---

## 🎯 Integration Tasks

### **1. WebView-Setup für Interactive Street View**

- Ersetze `AsyncImage` mit `AndroidView(WebView)`
- Konfiguriere WebView: JavaScript aktivieren, DOM Storage, Wide Viewport
- Implementiere Loading-States während WebView lädt

### **2. API-Layer erweitern**

- Erweitere Repository/Service um neue Endpunkte
- Füge `?interactive=true` Parameter zu bestehenden Calls hinzu
- Implementiere Fallback-Logic: Interactive → Static bei Fehlern

### **3. UI-Komponenten aktualisieren**

- Erstelle `InteractiveStreetView` Composable
- Aktualisiere Game Screen: Verwende neue Street View-Komponente
- Implementiere Error-Handling und Fallbacks

### **4. Game Integration**

- Nutze `/api/locations/random?includeStreetView=true` für direkten Street View-Einbettung
- Aktualisiere GameViewModel für neue API-Response-Struktur
- Teste verschiedene Difficulty-Levels mit Interactive Street View

### **5. Performance-Optimierung**

- Implementiere Lazy Loading für Street View-Daten
- Cache Embed-URLs (sind statisch für gleiche Parameter)
- Loading-Indicators für bessere UX

---

## 📋 Code-Beispiele

### Neue StreetView-Komponente:

```kotlin
@Composable
fun InteractiveStreetView(
    locationId: Int,
    modifier: Modifier = Modifier
) {
    var streetViewData by remember { mutableStateOf<StreetViewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(locationId) {
        try {
            streetViewData = repository.getInteractiveStreetView(locationId)
        } catch (e: Exception) {
            streetViewData = repository.getStaticStreetView(locationId) // Fallback
        }
        isLoading = false
    }

    Box(modifier = modifier) {
        when {
            isLoading -> CircularProgressIndicator()
            streetViewData?.data?.streetView?.type == "interactive" -> {
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
                        webView.loadUrl(streetViewData!!.data.streetView.embedUrl)
                    }
                )
            }
            else -> {
                AsyncImage(
                    model = streetViewData?.data?.streetView?.staticFallback,
                    contentDescription = "Street View"
                )
            }
        }
    }
}
```

### Repository-Erweiterung (Ready to Use):

```kotlin
class LocationRepository {
    // ✅ BEREITS VERFÜGBAR - Enhanced Interactive Street View
    suspend fun getEnhancedStreetView(locationId: Int): EnhancedStreetViewResponse {
        return apiService.get("/api/locations/$locationId/streetview/interactive?enableNavigation=true&quality=high")
    }

    // ✅ BEREITS VERFÜGBAR - Enhanced Random Locations
    suspend fun getEnhancedGameLocations(difficulty: Int): EnhancedLocationResponse {
        return apiService.get("/api/locations/random/enhanced?count=5&difficulty=$difficulty")
    }

    // ✅ BEREITS VERFÜGBAR - Bulk Loading für Performance
    suspend fun getBulkStreetViewData(locationIds: List<Int>): BulkStreetViewResponse {
        return apiService.get("/api/locations/streetview/bulk?ids=${locationIds.joinToString(",")}")
    }

    // ✅ BEREITS VERFÜGBAR - Dynamische Navigation
    suspend fun navigateStreetView(request: NavigationRequest): NavigationResponse {
        return apiService.post("/api/locations/streetview/navigate", request)
    }
}
```

---

## ✅ Erfolgs-Kriterien

- [ ] **WebView funktioniert**: Interaktive Street View lädt und ist navigierbar
- [ ] **Fallback funktioniert**: Bei Fehlern wird statisches Bild angezeigt
- [ ] **Game Integration**: Random Locations verwenden neue Interactive Street Views
- [ ] **Performance**: Keine merklichen Ladezeiten oder UI-Freezes
- [ ] **User Experience**: Smooth Navigation, Zoom, Pan in Street View

---

## 🔧 Debugging-Tipps

- **WebView Debug**: Aktiviere WebView-Debugging für Chrome DevTools
- **API-Testing**: Teste URLs direkt im Browser vor WebView-Integration
- **Fallback-Testing**: Simuliere API-Fehler um Fallback-Logic zu testen
- **Performance**: Monitor Memory Usage bei WebView-Usage

---

**Result:** Vollständig **interaktive Street View-Navigation** im Game statt statischer Bilder - Nutzer können sich frei in Street View bewegen, zoomen und navigieren! 🚀

**Backup-Plan:** Alle alten Endpunkte funktionieren weiterhin → Keine Breaking Changes, schrittweise Migration möglich.
