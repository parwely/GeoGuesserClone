# GeoGuessr Android App - Offline Edition

## 🌍 Projektübersicht

Eine vollständige, offline-fähige GeoGuessr Android-Anwendung, die als APK-Datei verteilt werden kann. Die App funktioniert komplett ohne Internetverbindung und nutzt ausschließlich kostenlose Technologien und Datenquellen.

## Zielplattform

- **Android Version**: 8.0+ (API Level 26+)
- **Distribution**: Direct APK Installation
- **Netzwerk**: Vollständig offline-fähig
- **Kosten**: 100% kostenlose Implementierung

## Technologie-Stack

### Core Framework
- **Sprache**: Kotlin
- **IDE**: Android Studio
- **Build System**: Gradle
- **Architektur**: MVVM (Model-View-ViewModel)
- **UI Pattern**: ViewBinding + LiveData

### Datenbank & Storage
- **Lokale Datenbank**: Room (SQLite Wrapper)
- **Asset Storage**: JSON-Dateien für Location-Daten
- **User Preferences**: SharedPreferences
- **Media Files**: Internal/External Storage für Bilder

### Kartenlösungen (Kostenlos)
- **Primary**: OpenStreetMap mit Leaflet WebView
- **Alternative**: Mapbox SDK (50k kostenlose Map Loads)
- **Fallback**: Custom Bitmap-Weltkarte
- **Tiles**: Vorgeladene OSM-Tiles in Assets

### Bildquellen (Kostenlos)
- **Wikimedia Commons**: Geotagged Photos
- **Mapillary API**: Street-level Imagery (kostenlos)
- **Custom Collection**: Eigene GPS-markierte Fotos
- **Asset Bundle**: Vorinstallierte Bildsammlung

### UI/UX Libraries
- **Material Design**: Android Material Components
- **Image Loading**: Glide
- **Charts & Statistics**: MPAndroidChart
- **Maps**: OSMDroid oder WebView mit Leaflet

### Machine Learning (Optional)
- **TensorFlow Lite**: Offline Objekterkennung
- **ML Kit**: Text/Landmark Recognition

## 📱 Kernfunktionen

### 1. Gameplay Features
-  **Zufällige Standortzuweisung**: Random location selection from JSON database
-  **Interaktive Weltkarte**: OpenStreetMap-basierte Guess-Interface
-  **Punktebewertung**: Haversine-Formel für Distanzberechnung
-  **Street-View Navigation**: 360° Photo Viewer mit Pan/Zoom
-  **Zeitlimit-Modus**: Countdown-Timer pro Runde
-  **Multi-Round Games**: Standard 5-Runden Spielformat

### 2. Spielmodi
-  **Einzelspieler**: Local Highscore Tracking
-  **Multiplayer/Battle Royale**: WiFi-Direct Local Multiplayer
-  **Challenge-Modus**: Sharable Game Codes
-  **Länder-/Städte-Modi**: Filtered Location Sets

### 3. Social Features (Offline)
-  **Leaderboards**: Lokale Highscore-Tabellen
-  **Freundeslisten**: Manual Friend Codes
-  **Statistiken**: Detaillierte Spielanalyse mit Charts

### 4. Erweiterte Features
-  **Hint-System**: Contextual Clues aus JSON-Daten
-  **Pro/Hardcore-Modi**: Movement/Time Restrictions
-  **Tägliche Challenges**: Algorithm-basierte Daily Puzzles
-  **Custom Maps**: User-Created Location Sets
-  **KI-Analyse**: TensorFlow Lite Object Recognition

## 🗂️ Projektstruktur

```
app/
├── src/main/
│   ├── java/com/geoguessr/
│   │   ├── activities/
│   │   │   ├── MainActivity.kt
│   │   │   ├── GameActivity.kt
│   │   │   ├── MapGuessActivity.kt
│   │   │   ├── StatsActivity.kt
│   │   │   ├── MultiplayerActivity.kt
│   │   │   └── SettingsActivity.kt
│   │   ├── fragments/
│   │   │   ├── StreetViewFragment.kt
│   │   │   ├── MapFragment.kt
│   │   │   └── ScoreFragment.kt
│   │   ├── database/
│   │   │   ├── entities/
│   │   │   ├── dao/
│   │   │   └── AppDatabase.kt
│   │   ├── models/
│   │   │   ├── Location.kt
│   │   │   ├── Game.kt
│   │   │   ├── Player.kt
│   │   │   └── Score.kt
│   │   ├── repositories/
│   │   │   ├── LocationRepository.kt
│   │   │   ├── GameRepository.kt
│   │   │   └── ScoreRepository.kt
│   │   ├── viewmodels/
│   │   │   ├── GameViewModel.kt
│   │   │   ├── MapViewModel.kt
│   │   │   └── StatsViewModel.kt
│   │   └── utils/
│   │       ├── DistanceCalculator.kt
│   │       ├── ScoreCalculator.kt
│   │       └── ImageLoader.kt
│   ├── assets/
│   │   ├── locations/
│   │   │   ├── world_locations.json
│   │   │   ├── europe_capitals.json
│   │   │   └── asia_cities.json
│   │   ├── images/
│   │   │   ├── street_views/
│   │   │   └── map_tiles/
│   │   └── maps/
│   │       └── world_map.png
│   └── res/
│       ├── layout/
│       ├── drawable/
│       ├── values/
│       └── menu/
└── build.gradle
```

## Datenmodell

### Location Entity (JSON + Room)
```json
{
  "id": 1,
  "latitude": 48.8566,
  "longitude": 2.3522,
  "country": "France",
  "city": "Paris",
  "region": "Europe",
  "difficulty": "medium",
  "category": "capital",
  "images": [
    "paris_eiffel_1.jpg",
    "paris_seine_2.jpg"
  ],
  "hints": [
    "Französische Straßenschilder",
    "Klassische europäische Architektur",
    "Berühmtes Wahrzeichen sichtbar"
  ],
  "metadata": {
    "climate": "temperate",
    "language": "french",
    "drive_side": "right"
  }
}
```

### Game State Management
```kotlin
data class GameSession(
    val id: String,
    val mode: GameMode,
    val rounds: List<Round>,
    val currentRound: Int,
    val totalScore: Int,
    val timeLimit: Int?,
    val startTime: Long
)

data class Round(
    val locationId: Int,
    val guessLat: Double?,
    val guessLng: Double?,
    val distance: Double?,
    val score: Int?,
    val timeSpent: Long?
)
```

## 🎮 Spielmechanik

### Scoring Algorithm
```kotlin
/**
 * Berechnet Punkte basierend auf Distanz (Haversine Formula)
 * Max: 5000 Punkte bei perfekter Schätzung
 * Exponential decay für größere Distanzen
 */
fun calculateScore(distanceKm: Double): Int {
    val maxScore = 5000
    val decayFactor = 2000.0 // km für 50% Score
    return (maxScore * exp(-distanceKm / decayFactor)).toInt()
}
```

### Distance Calculation (Haversine)
```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * 
            cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    return earthRadius * c
}
```

## 🌐 Offline Multiplayer

### Local WiFi Implementation
```kotlin
// WiFi Direct für lokale Multiplayer-Sessions
class MultiplayerManager {
    fun createGameSession(): String // Game Code generieren
    fun joinGameSession(code: String): Boolean // Session beitreten
    fun broadcastGuess(guess: GuessData) // Guess an alle Spieler
    fun syncScores(): List<PlayerScore> // Score Synchronisation
}
```

### Game Code System
- **Format**: 6-stelliger alphanumerischer Code
- **Gültigkeit**: 24 Stunden
- **Übertragung**: QR-Code oder manuell

## 📊 Analytics & Statistics

### Tracked Metrics
- Gesamtspiele gespielt
- Durchschnittliche Genauigkeit
- Beste/Schlechteste Länder
- Zeitbasierte Performance
- Streak-Aufzeichnungen
- Difficulty-Progress

### Visualization (MPAndroidChart)
- Line Charts für Fortschritt über Zeit
- Bar Charts für Länder-Performance
- Pie Charts für Game Mode Verteilung
- Radar Charts für Skill-Analysis

## Installation & Distribution

### APK Building
```bash
# Debug Build
./gradlew assembleDebug

# Release Build (Signed)
./gradlew assembleRelease
```

### Installation Instructions
1. **Download**: APK-Datei herunterladen
2. **Settings**: "Unbekannte Quellen" in Android-Einstellungen aktivieren
3. **Install**: Auf APK-Datei tippen und Installation bestätigen
4. **Security**: "Unbekannte Quellen" wieder deaktivieren
5. **Launch**: App über App-Drawer starten

### APK Optimization
- **ProGuard**: Code-Obfuscation und Minimierung
- **Asset Compression**: Optimierte Bild- und Datengrößen
- **Multi-APK**: Verschiedene Builds für verschiedene Architekturen

### Build Configuration
```gradle
android {
    compileSdk 34
    defaultConfig {
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    
    // Architecture Components
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    
    // Database
    implementation 'androidx.room:room-runtime:2.5.0'
    implementation 'androidx.room:room-ktx:2.5.0'
    kapt 'androidx.room:room-compiler:2.5.0'
    
    // UI
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    
    // Maps
    implementation 'org.osmdroid:osmdroid-android:6.1.17'
    
    // JSON Parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Machine Learning (Optional)
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'
}
```

## Performance Optimizations

### Memory Management
- Lazy loading für große Bild-Assets
- Effiziente Bitmap-Handhabung mit Glide
- RecyclerView für große Listen
- Proper lifecycle-aware components

### Battery Optimization
- Minimal background processing
- Efficient location updates
- Smart caching strategies
- CPU-optimized algorithms

### Storage Efficiency
- Compressed image assets
- Optimized JSON structures
- Smart database indexing
- Cache cleanup routines

## Testing Strategy

### Unit Tests
- Distance calculation algorithms
- Score computation logic
- Game state management
- JSON parsing routines

### Integration Tests
- Database operations
- File system interactions
- UI component integration
- Multi-round game flows

### Manual Testing Checklist
- [ ] Installation auf verschiedenen Geräten
- [ ] Offline-Funktionalität
- [ ] Performance bei niedriger RAM
- [ ] Bildqualität auf verschiedenen Bildschirmgrößen
- [ ] Multiplayer-Synchronisation

## 📋 Roadmap & Future Features

### Version 1.0 (MVP)
-  Core Gameplay
- Single Player Mode
- Basic Statistics
- Offline Maps

### Version 1.1 (Enhanced)
- Local Multiplayer
- Custom Map Creation
- Advanced Hints
- Achievement System

### Version 1.2 (Advanced)
- ⏳ AR Mode (Camera-based guessing)
- ⏳ Machine Learning Hints
- ⏳ Community Challenges
- ⏳ Advanced Analytics

## 📄 License & Legal

### Open Source Components
- Android SDK: Apache 2.0
- Kotlin: Apache 2.0
- Room Database: Apache 2.0
- Glide: BSD, part MIT and Apache 2.0
- OSMDroid: Apache 2.0

### Image Sources
- Wikimedia Commons: Various Creative Commons licenses
- Mapillary: Mapillary Terms of Use
- Custom Photos: Proprietary/Educational Use

### Distribution
- APK Distribution: Educational/Personal Use
- No commercial redistribution without permission
- Third-party liability limitations

---
