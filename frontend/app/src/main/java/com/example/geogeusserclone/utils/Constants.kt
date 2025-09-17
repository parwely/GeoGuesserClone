package com.example.geogeusserclone.utils




object Constants {


    // Backend Configuration - Dein Google Maps Backend
    const val BASE_URL = "http://10.0.2.2:3000/api/" // Android Emulator
    // Alternative für echtes Gerät: const val BASE_URL = "http://YOUR_LOCAL_IP:3000/api/"
    // Production: const val BASE_URL = "https://your-backend.com/api/"

    // Mapillary als Fallback (nur wenn Backend nicht verfügbar)
    const val MAPILLARY_BASE_URL = "https://graph.mapillary.com/"
    // SICHERHEIT: API-Schlüssel NIEMALS im Code! Verwende BuildConfig oder Environment Variables
    const val MAPILLARY_ACCESS_TOKEN = "" // ENTFERNT - Verwende BuildConfig.MAPILLARY_API_KEY

    // Network Timeouts angepasst an deine Spezifikation
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    // Backend-First Mode (Mapillary nur als Fallback)
    const val ENABLE_OFFLINE_MODE = false // Backend verfügbar
    const val BACKEND_FALLBACK_DELAY_MS = 5000L // Längerer Timeout für Backend

    // Game Configuration
    const val MAX_ROUND_TIME_MS = 120000L // 2 Minuten
    const val TIME_BONUS_MAX = 500

    // Score Thresholds
    const val PERFECT_DISTANCE_KM = 1.0
    const val EXCELLENT_DISTANCE_KM = 10.0
    const val GOOD_DISTANCE_KM = 50.0
    const val FAIR_DISTANCE_KM = 200.0
    const val POOR_DISTANCE_KM = 1000.0

    // Cache Configuration
    const val IMAGE_CACHE_SIZE_MB = 50L
    const val MAP_TILE_CACHE_SIZE_MB = 100L
    const val LOCATION_PRELOAD_COUNT = 10

    // Performance Settings
    const val ANIMATION_DURATION_MS = 300L
    const val DEBOUNCE_DELAY_MS = 100L
    const val MAP_ZOOM_MIN = 1.0
    const val MAP_ZOOM_MAX = 19.0

    // Background Work
    const val LOCATION_PRELOAD_INTERVAL_HOURS = 2L
    const val CACHE_CLEANUP_INTERVAL_HOURS = 24L

    // UI Performance
    const val LAZY_COLUMN_PREFETCH_COUNT = 3
    const val IMAGE_CROSSFADE_DURATION = 200
}