package com.example.geogeusserclone.utils

object Constants {
    // Network Configuration
    const val BASE_URL = "https://api.geoguessr-clone.com/"
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

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
