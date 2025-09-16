# 📡 Available API Endpoints Overview

## ✅ Bereits implementierte Backend-Endpunkte

Die folgenden API-Endpunkte sind **vollständig implementiert** und bereit für Frontend-Integration:

### 1. Enhanced Interactive Street View

```http
GET /api/locations/{id}/streetview/interactive
```

**Query Parameters:**

- `heading` (optional): Blickrichtung in Grad (0-360)
- `zoom` (optional): Zoom-Level (0.5-2.0)
- `enableNavigation` (optional): Navigation aktivieren (default: true)
- `quality` (optional): Qualitätsstufe ('low', 'medium', 'high')

**Response Structure:**

```json
{
  "success": true,
  "data": {
    "location": {
      "id": 123,
      "name": "Location Name",
      "coordinates": { "latitude": 52.52, "longitude": 13.405 }
    },
    "interactive": {
      "embedUrl": "https://www.google.com/maps/embed/v1/streetview?...",
      "navigationEnabled": true,
      "controls": {
        "pan": true,
        "zoom": true,
        "compass": true,
        "streetViewControls": true
      },
      "configuration": {
        "heading": 90,
        "zoom": 1.2,
        "quality": "high"
      },
      "fallback": {
        "staticUrl": "https://maps.googleapis.com/...",
        "reason": "backup"
      }
    },
    "metadata": {
      "quality": "high",
      "supportsNavigation": true,
      "recommendedFor": ["webview", "iframe"],
      "apiVersion": "1.0"
    }
  }
}
```

### 2. Dynamische Street View Navigation

```http
POST /api/locations/streetview/navigate
Content-Type: application/json
```

**Request Body:**

```json
{
  "latitude": 52.52,
  "longitude": 13.405,
  "direction": "north",
  "distance": 50,
  "zoom": 1.0
}
```

**Response Structure:**

```json
{
  "success": true,
  "data": {
    "navigation": {
      "originalPosition": { "latitude": 52.52, "longitude": 13.405 },
      "newPosition": { "latitude": 52.52045, "longitude": 13.405 },
      "direction": "north",
      "distance": 50,
      "heading": 0
    },
    "streetView": {
      "type": "interactive",
      "embedUrl": "https://www.google.com/maps/embed/v1/streetview?...",
      "navigationEnabled": true,
      "quality": "high"
    }
  }
}
```

### 3. Enhanced Random Locations

```http
GET /api/locations/random/enhanced
```

**Query Parameters:**

- `count` (optional): Anzahl Locations (default: 1)
- `difficulty` (optional): Schwierigkeitsgrad 1-5
- `category` (optional): Location-Kategorie
- `includeMetadata` (optional): Erweiterte Metadaten einbeziehen

**Response Structure:**

```json
{
  "success": true,
  "data": {
    "count": 5,
    "locations": [
      {
        "id": 123,
        "name": "Location Name",
        "coordinates": { "latitude": 52.52, "longitude": 13.405 },
        "difficulty": 3,
        "category": "urban",
        "streetView": {
          "type": "interactive",
          "embedUrl": "https://www.google.com/maps/embed/v1/streetview?...",
          "staticFallback": "https://maps.googleapis.com/...",
          "navigationEnabled": true,
          "quality": "high"
        },
        "metadata": {
          "country": "Germany",
          "continent": "Europe",
          "climate": "temperate",
          "terrain": "urban"
        }
      }
    ],
    "filters": {
      "difficulty": 3,
      "category": "urban",
      "includeMetadata": true
    }
  }
}
```

### 4. Bulk Street View Loading

```http
GET /api/locations/streetview/bulk
```

**Query Parameters:**

- `ids` (required): Komma-separierte Location-IDs (z.B. "1,2,3,4,5")
- `quality` (optional): Qualitätsstufe für alle Locations
- `interactive` (optional): Interaktive URLs generieren (default: true)

**Response Structure:**

```json
{
  "success": true,
  "data": {
    "streetViews": [
      {
        "locationId": 1,
        "streetView": {
          "type": "interactive",
          "embedUrl": "https://www.google.com/maps/embed/v1/streetview?...",
          "staticFallback": "https://maps.googleapis.com/...",
          "navigationEnabled": true,
          "quality": "high"
        }
      },
      {
        "locationId": 2,
        "streetView": {
          /* ... */
        }
      }
    ],
    "performance": {
      "requestedCount": 5,
      "successCount": 5,
      "failedCount": 0,
      "processingTime": "245ms"
    }
  }
}
```

## 🔄 Zusätzlich verfügbare Endpunkte (Backward Compatible)

### Legacy Street View (mit Interactive Parameter)

```http
GET /api/locations/{id}/streetview?interactive=true&quality=high
```

### Legacy Random with Street View

```http
GET /api/locations/random?includeStreetView=true&count=5&difficulty=3
```

## 🚀 Frontend Integration Mapping

### Repository/Service Layer Integration:

```kotlin
class StreetViewRepository {

    // ✅ BEREITS VERFÜGBAR
    suspend fun getEnhancedStreetView(locationId: Int): EnhancedStreetViewResponse {
        return apiService.get("/api/locations/$locationId/streetview/interactive")
    }

    // ✅ BEREITS VERFÜGBAR
    suspend fun navigateStreetView(request: NavigationRequest): NavigationResponse {
        return apiService.post("/api/locations/streetview/navigate", request)
    }

    // ✅ BEREITS VERFÜGBAR
    suspend fun getEnhancedRandomLocations(count: Int, difficulty: Int?): EnhancedLocationResponse {
        return apiService.get("/api/locations/random/enhanced?count=$count&difficulty=$difficulty")
    }

    // ✅ BEREITS VERFÜGBAR
    suspend fun getBulkStreetViewData(locationIds: List<Int>): BulkStreetViewResponse {
        return apiService.get("/api/locations/streetview/bulk?ids=${locationIds.joinToString(",")}")
    }
}
```

### Game Integration Ready:

```kotlin
class GameRepository {

    suspend fun startNewEnhancedGame(difficulty: Int): GameData {
        // ✅ Enhanced API direkt nutzen
        val response = streetViewRepository.getEnhancedRandomLocations(5, difficulty)

        return GameData(
            locations = response.data.locations.map { location ->
                GameLocation(
                    id = location.id,
                    coordinates = location.coordinates,
                    streetView = location.streetView, // Bereits enthalten!
                    metadata = location.metadata
                )
            }
        )
    }
}
```

## 🎯 Next Steps für Frontend

1. **✅ APIs sind bereit** - Keine Backend-Arbeit erforderlich
2. **🔧 UI Integration** - WebView-Komponenten für Interactive Street View erstellen
3. **🎮 Game Flow** - Enhanced APIs in bestehende Game-Logic integrieren
4. **🚀 Performance** - Bulk Loading für bessere User Experience nutzen
5. **🧭 Navigation** - Optional: Street View Navigation Controls implementieren

**Status: Backend vollständig implementiert - Frontend-Integration kann beginnen! 🚀**
