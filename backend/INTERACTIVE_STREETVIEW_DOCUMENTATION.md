# Interactive Street View Implementation - Backend Documentation

## Übersicht

Das Backend wurde erfolgreich von statischen Street View-Bildern zu interaktiven Street View-URLs umgestellt. Diese Dokumentation beschreibt die neuen API-Endpunkte und deren Verwendung.

## Neue API-Endpunkte

### 1. Interaktiver Street View für spezifische Location

```http
GET /api/locations/:id/streetview?interactive=true&heading=90&zoom=1.2
```

**Parameter:**

- `interactive` (boolean): `true` für interaktive URLs, `false` für statische (Standard: true)
- `heading` (number): Blickrichtung in Grad (0-360)
- `zoom` (number): Zoom-Level (0.5-2.0)
- `fallbackType` (string): Art des Fallbacks ('static', 'image', 'none')

**Response:**

```json
{
  "success": true,
  "data": {
    "location": {
      "id": 123,
      "name": "Berlin Brandenburg Gate",
      "coordinates": {
        "latitude": 52.5163,
        "longitude": 13.3777
      }
    },
    "streetView": {
      "type": "interactive",
      "embedUrl": "https://www.google.com/maps/embed/v1/streetview?key=...&location=52.5163,13.3777&heading=90&zoom=1&navigation=1&controls=1",
      "staticFallback": "https://maps.googleapis.com/maps/api/streetview?...",
      "navigationEnabled": true,
      "quality": "high"
    }
  }
}
```

### 2. Erweiterte Interaktive Street View

```http
GET /api/locations/:id/streetview/interactive?heading=180&enableNavigation=true&quality=high
```

**Parameter:**

- `heading` (number): Blickrichtung
- `zoom` (number): Zoom-Level
- `enableNavigation` (boolean): Navigation aktivieren (Standard: true)
- `quality` (string): Qualitätsstufe ('low', 'medium', 'high')

**Response:**

```json
{
  "success": true,
  "data": {
    "location": {...},
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
        "heading": 180,
        "zoom": 1.0,
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

### 3. Dynamische Street View Navigation

```http
POST /api/locations/streetview/navigate
Content-Type: application/json

{
  "latitude": 52.5163,
  "longitude": 13.3777,
  "direction": "north",
  "distance": 50,
  "zoom": 1.0
}
```

**Parameter:**

- `latitude` (number, required): Aktuelle Breite
- `longitude` (number, required): Aktuelle Länge
- `direction` (string, required): Bewegungsrichtung ('north', 'south', 'east', 'west', 'northeast', 'northwest', 'southeast', 'southwest')
- `distance` (number): Entfernung in Metern (Standard: 50)
- `zoom` (number): Zoom-Level (Standard: 1.0)

**Response:**

```json
{
  "success": true,
  "data": {
    "navigation": {
      "originalPosition": {
        "latitude": 52.5163,
        "longitude": 13.3777
      },
      "newPosition": {
        "latitude": 52.51675,
        "longitude": 13.3777
      },
      "direction": "north",
      "distance": 50,
      "heading": 0
    },
    "streetView": {
      "type": "interactive",
      "embedUrl": "https://www.google.com/maps/embed/v1/streetview?...",
      "navigationEnabled": true
    }
  }
}
```

### 4. Zufällige Locations mit Street View

```http
GET /api/locations/random?count=1&includeStreetView=true&difficulty=3
```

**Neue Parameter:**

- `includeStreetView` (boolean): Street View-Daten in Response einbinden

**Response:**

```json
{
  "success": true,
  "data": {
    "count": 1,
    "locations": [
      {
        "id": 123,
        "name": "Location Name",
        "coordinates": {...},
        "streetView": {
          "type": "interactive",
          "embedUrl": "https://www.google.com/maps/embed/v1/streetview?...",
          "staticFallback": "https://maps.googleapis.com/..."
        }
      }
    ],
    "filters": {...},
    "streetViewIncluded": true
  }
}
```

## Rückwärtskompatibilität

### Statische URLs (Legacy-Modus)

```http
GET /api/locations/:id/streetview?interactive=false
```

Gibt weiterhin die alte statische URL zurück:

```json
{
  "success": true,
  "data": {
    "location": {...},
    "streetViewUrl": "https://maps.googleapis.com/maps/api/streetview?..."
  }
}
```

## Frontend-Integration

### WebView (Empfohlen für Interactive URLs)

```kotlin
// Android WebView
webView.loadUrl(response.data.streetView.embedUrl)

// iOS WKWebView
webView.load(URLRequest(url: URL(string: embedUrl)!))
```

### AsyncImage Fallback

```kotlin
// Für statische Fallback-URLs
AsyncImage(
    url = response.data.streetView.staticFallback,
    contentDescription = "Street View"
)
```

## Technische Details

### URL-Struktur für Interactive Street View

```
https://www.google.com/maps/embed/v1/streetview
  ?key=YOUR_API_KEY
  &location=LATITUDE,LONGITUDE
  &heading=HEADING_IN_DEGREES
  &zoom=ZOOM_LEVEL
  &navigation=1
  &controls=1
```

### Navigation-Berechnung

Die Navigation verwendet die folgende Logik:

- **Norden**: Breitengrad erhöhen
- **Süden**: Breitengrad verringern
- **Osten**: Längengrad erhöhen
- **Westen**: Längengrad verringern
- **Diagonale**: Kombination der entsprechenden Richtungen

### Fehlerbehandlung

Alle Endpunkte geben konsistente Fehlerformate zurück:

```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "debug": "Development-only debug info"
}
```

### HTTP Status Codes

- `200`: Erfolgreiche Response
- `400`: Ungültige Parameter
- `404`: Location nicht gefunden
- `500`: Server-Fehler

## Konfiguration

### Umgebungsvariablen

```env
GOOGLE_STREET_VIEW_API_KEY=your_api_key_here
MAPILLARY_CLIENT_ID=your_mapillary_client_id_here
```

### Service-Initialisierung

Der Street View Service wird beim Server-Start automatisch initialisiert und zeigt:

```
🌍 Street View service initialized with interactive support
```

## Migration Guide

### Von statischen zu interaktiven URLs

1. **Sofortige Migration**: Setze `?interactive=true` Parameter
2. **Schrittweise Migration**: Nutze `includeStreetView=true` für neue Features
3. **Vollständige Migration**: Ersetze alle AsyncImage mit WebView-Implementierungen

### Breaking Changes

- **Keine Breaking Changes**: Alle bestehenden Endpunkte funktionieren weiterhin
- **Neue Parameter**: Optionale Parameter erweitern die Funktionalität
- **Response-Struktur**: Neue Felder, bestehende Struktur bleibt erhalten

## Beispiel-Integration

### React/TypeScript Frontend

```typescript
interface StreetViewResponse {
  streetView: {
    type: "interactive" | "static";
    embedUrl: string;
    staticFallback?: string;
    navigationEnabled: boolean;
  };
}

// WebView-Integration
const StreetViewComponent = ({ embedUrl }: { embedUrl: string }) => (
  <iframe
    src={embedUrl}
    width="100%"
    height="400px"
    frameBorder="0"
    allowFullScreen
  />
);
```

### Mobile App Integration

```kotlin
// Kotlin/Android
data class StreetViewData(
    val type: String,
    val embedUrl: String,
    val staticFallback: String?,
    val navigationEnabled: Boolean
)

// WebView Setup
webView.settings.javaScriptEnabled = true
webView.loadUrl(streetViewData.embedUrl)
```

## Performance-Optimierung

### Caching-Strategien

- **URL-Caching**: Generierte URLs werden im Cache-Service gespeichert
- **Navigation-Cache**: Berechnete Navigationspositionen werden zwischengespeichert
- **Fallback-Cache**: Statische URLs als Backup verfügbar

### Lazy Loading

```javascript
// Nur Street View laden wenn benötigt
const response = await fetch("/api/locations/random?count=5");
const locationsWithStreetView = await Promise.all(
  locations.map(async (location) => {
    const streetView = await fetch(
      `/api/locations/${location.id}/streetview?interactive=true`
    );
    return { ...location, streetView: await streetView.json() };
  })
);
```

## Monitoring und Debugging

### Logs

Der Server loggt alle Street View-Requests:

```
✅ Interactive Street View URL generated for location 123
🧭 Street View navigation request: {...}
✅ Street View navigation completed from 52.5163,13.3777 to 52.51675,13.3777
```

### Health Checks

```http
GET /api/locations/:id/streetview/check
```

Prüft die Verfügbarkeit verschiedener Street View-Services.

## Zukünftige Erweiterungen

1. **Mapillary Integration**: Alternative Street View-Quelle
2. **Custom Street View**: Eigene 360°-Bilder
3. **AR Integration**: Augmented Reality Features
4. **Offline Mode**: Cached Street View-Daten
