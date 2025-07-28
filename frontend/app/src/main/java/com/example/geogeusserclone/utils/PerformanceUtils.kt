package com.example.geogeusserclone.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Performance-optimierte State Management Utils
 */

// Optimierte collectAsState für bessere Performance
@Composable
fun <T> StateFlow<T>.collectAsStateOptimized(): State<T> {
    return collectAsState()
}

// Lazy State für schwere Berechnungen
@Composable
fun <T> rememberLazy(calculation: () -> T): T {
    return remember { calculation() }
}

// Throttled State Updates
@Composable
fun <T> rememberThrottledState(
    initialValue: T,
    throttleMs: Long = 100L
): MutableState<T> {
    val state = remember { mutableStateOf(initialValue) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= throttleMs) {
                    state.value = value
                    lastUpdateTime = currentTime
                }
            }

        override fun component1(): T = value
        override fun component2(): (T) -> Unit = { value = it }
    }
}
