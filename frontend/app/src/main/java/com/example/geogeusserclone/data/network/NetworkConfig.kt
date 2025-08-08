package com.example.geogeusserclone.data.network

import com.example.geogeusserclone.BuildConfig

object NetworkConfig {
    // Android Emulator
    const val EMULATOR_BASE_URL = "http://10.0.2.2:3000/api/"

    // Lokales Netzwerk (IP-Adresse anpassen)
    const val LOCAL_BASE_URL = "http://192.168.1.100:3000/api/"

    // Production (falls vorhanden)
    const val PROD_BASE_URL = "https://your-domain.com/api/"

    val BASE_URL = if (BuildConfig.DEBUG) {
        EMULATOR_BASE_URL // Für Android Emulator
        // LOCAL_BASE_URL // Uncomment für echtes Gerät
    } else {
        PROD_BASE_URL
    }

    // Timeout-Konfiguration
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
