package com.example.geogeusserclone.utils

object Constants {
    // Backend API Configuration
    const val BASE_URL = "http://10.0.2.2:3000/api/" // Android Emulator
    // Alternative für echtes Gerät: const val BASE_URL = "http://YOUR_LOCAL_IP:3000/api/"
    // Production: const val BASE_URL = "https://your-backend.com/api/"

    // Mapillary API (fallback)
    const val MAPILLARY_BASE_URL = "https://graph.mapillary.com/"
    const val MAPILLARY_ACCESS_TOKEN = "MLY|your_token_here"

    // Street View Default Parameters
    const val DEFAULT_HEADING = 0
    const val DEFAULT_PITCH = 0
    const val DEFAULT_FOV = 90

    // Game Configuration
    const val DEFAULT_ROUND_COUNT = 5
    const val DEFAULT_TIME_LIMIT_MS = 60000L // 1 minute

    // API Timeouts
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
