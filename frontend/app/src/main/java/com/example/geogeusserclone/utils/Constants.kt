package com.example.geogeusserclone.utils

object Constants {
    // Game Modes
    const val GAME_MODE_SINGLE = "SINGLE"
    const val GAME_MODE_MULTIPLAYER = "MULTIPLAYER"
    const val GAME_MODE_BATTLE_ROYALE = "BATTLE_ROYALE"

    // Game Settings
    const val DEFAULT_GAME_ROUNDS = 5
    const val MAX_ROUND_TIME_MS = 60000L // 60 seconds
    const val MIN_ROUND_TIME_MS = 10000L // 10 seconds

    // Scoring
    const val MAX_SCORE_PER_ROUND = 5000
    const val PERFECT_GUESS_DISTANCE_KM = 1.0

    // Map Settings
    const val DEFAULT_ZOOM_LEVEL = 2.0
    const val GUESS_ZOOM_LEVEL = 10.0
    const val MIN_ZOOM_LEVEL = 1.0
    const val MAX_ZOOM_LEVEL = 18.0

    // Preferences Keys
    const val PREF_USER_ID = "user_id"
    const val PREF_AUTH_TOKEN = "auth_token"
    const val PREF_SOUND_ENABLED = "sound_enabled"
    const val PREF_VIBRATION_ENABLED = "vibration_enabled"

    // Score Thresholds (in km)
    const val PERFECT_DISTANCE_KM = 1.0
    const val EXCELLENT_DISTANCE_KM = 10.0
    const val GOOD_DISTANCE_KM = 50.0
    const val FAIR_DISTANCE_KM = 200.0
    const val POOR_DISTANCE_KM = 1000.0

    // Database
    const val DATABASE_NAME = "geoguessr_database"
    const val DATABASE_VERSION = 2

    // Network
    const val BASE_URL = "http://10.0.2.2:3000/api/"
    //oder:
    // const val BASE_URL = "http://deine-ip:3000/api/" // Für echtes Gerät
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

}