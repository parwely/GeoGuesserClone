package com.example.geogeusserclone.utils

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MemoryManager {

    @Composable
    fun AutoMemoryManagement(context: Context) {
        val scope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            // Cleanup when composable is disposed
            onDispose {
                scope.launch(Dispatchers.IO) {
                    performMemoryCleanup(context)
                }
            }
        }
    }

    private fun performMemoryCleanup(context: Context) {
        try {
            // Trigger garbage collection
            System.gc()

            // Clear cache if needed
            val cacheDir = context.cacheDir
            if (cacheDir.exists() && cacheDir.length() > 50 * 1024 * 1024) { // 50MB
                // Optionally clear old cache files
                println("MemoryManager: Cache size exceeded, consider cleanup")
            }
        } catch (e: Exception) {
            println("MemoryManager: Error during cleanup: ${e.message}")
        }
    }
}
