package com.example.geogeusserclone.utils

import android.content.ComponentCallbacks2
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader

/**
 * Memory Management Utilities für bessere Performance
 */
object MemoryManager {

    /**
     * Räumt Image Cache bei Memory Pressure auf
     */
    fun handleMemoryPressure(context: Context, level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                // Leichte Bereinigung - nur einen Teil des Cache leeren
                clearImageCache(context, moderate = true)
            }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Aggressive Bereinigung - kompletten Cache leeren
                clearImageCache(context, moderate = false)
            }
        }
    }

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
     * Lifecycle-aware Memory Management
     */
    @Composable
    fun LifecycleAwareMemoryCleanup(onCleanup: () -> Unit) {
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> onCleanup()
                    Lifecycle.Event.ON_DESTROY -> onCleanup()
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
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
