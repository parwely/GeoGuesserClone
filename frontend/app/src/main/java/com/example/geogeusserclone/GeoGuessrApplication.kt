package com.example.geogeusserclone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeoGuessrApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}