package com.example.geogeusserclone.utils

import android.content.ComponentCallbacks2
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi

/**
 * Memory Management Utilities für bessere Performance
 */
object MemoryManager {

    /**
     * Räumt Image Cache bei Memory Pressure auf
     */
    @Suppress("DEPRECATION")
    fun handleMemoryPressure(context: Context, level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            // Verwende moderne Alternative oder suppresse warning für legacy support
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Leichte Bereinigung - nur einen Teil des Cache leeren
                clearImageCache(context, moderate = true)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Aggressive Bereinigung - kompletten Cache leeren
                clearImageCache(context, moderate = false)
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun clearImageCache(context: Context, moderate: Boolean) {
        try {
            val imageLoader = ImageLoader(context)
            if (moderate) {
                // Bei moderater Bereinigung nur Memory Cache leeren
                imageLoader.memoryCache?.clear()
            } else {
                // Bei kritischer Memory-Situation alles leeren
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            }
        } catch (e: Exception) {
            // Silent fail - Cache clearing sollte App nicht zum Absturz bringen
        }
    }

    /**
     * Composable für automatisches Memory Management basierend auf Lifecycle
     */
    @Composable
    fun AutoMemoryManagement(context: Context) {
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        // App ist in Background - moderate Bereinigung
                        handleMemoryPressure(context, ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
                    }
                    Lifecycle.Event.ON_STOP -> {
                        // App ist nicht mehr sichtbar - stärkere Bereinigung
                        handleMemoryPressure(context, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        // App wird zerstört - komplette Bereinigung
                        handleMemoryPressure(context, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
                    }
                    else -> Unit
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    /**
     * Optimiert Coil ImageLoader für bessere Memory Performance
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Verwende maximal 25% des verfügbaren Speichers
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB Disk Cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    /**
     * Berechnet verfügbaren Speicher
     */
    fun getAvailableMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024) // In MB
    }

    /**
     * Prüft ob Low Memory Situation vorliegt
     */
    fun isLowMemory(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }
}

/**
 * Optimierte Composable für schwere UI-Operationen
 */
@Composable
fun <T> LazyComposable(
    key: Any,
    computation: () -> T
): T {
    return remember(key) {
        computation()
    }
}
