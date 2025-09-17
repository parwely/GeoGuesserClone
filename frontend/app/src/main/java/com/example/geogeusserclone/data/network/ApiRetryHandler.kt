package com.example.geogeusserclone.data.network

import kotlinx.coroutines.delay
import retrofit2.Response

/**
 * Intelligent API Retry Handler für robuste Backend-Kommunikation
 * Versucht verschiedene Parameter-Kombinationen für optimale Location-Beschaffung
 */
object ApiRetryHandler {

    /**
     * Führt intelligente Retry-Versuche für Location-APIs durch
     * Versucht verschiedene Kombinationen von Kategorie und Schwierigkeit
     */
    suspend fun <T> executeLocationRetry(
        categories: List<String>,
        difficulties: List<Int>,
        maxRetries: Int = 6,
        delayMs: Long = 500,
        apiCall: suspend (category: String, difficulty: Int) -> Response<T>
    ): Response<T>? {

        val attempts = mutableListOf<Pair<String, Int>>()

        // Erstelle alle Kombinationen
        for (category in categories) {
            for (difficulty in difficulties) {
                attempts.add(category to difficulty)
            }
        }

        // Shuffel für bessere Verteilung
        attempts.shuffle()

        // Begrenze auf maxRetries
        val limitedAttempts = attempts.take(maxRetries)

        for ((index, categoryDifficulty) in limitedAttempts.withIndex()) {
            val (category, difficulty) = categoryDifficulty

            try {
                println("ApiRetryHandler: Versuche category=$category, difficulty=$difficulty")

                val response = apiCall(category, difficulty)

                if (response.isSuccessful) {
                    println("ApiRetryHandler: ✅ Erfolgreich mit category=$category, difficulty=$difficulty")
                    return response
                } else {
                    println("ApiRetryHandler: ❌ Fehlgeschlagen ($response.code()) mit category=$category, difficulty=$difficulty")
                }

            } catch (e: Exception) {
                println("ApiRetryHandler: ❌ Exception bei category=$category, difficulty=$difficulty: ${e.message}")
            }

            // Delay zwischen Versuchen (aber nicht nach dem letzten)
            if (index < limitedAttempts.size - 1) {
                delay(delayMs)
            }
        }

        println("ApiRetryHandler: ❌ Alle Retry-Versuche fehlgeschlagen")
        return null
    }

    /**
     * Einfacher Retry für einzelne API-Calls
     */
    suspend fun <T> executeSimpleRetry(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        apiCall: suspend () -> Response<T>
    ): Response<T>? {

        repeat(maxRetries) { attempt ->
            try {
                println("ApiRetryHandler: Einfacher Retry-Versuch ${attempt + 1}/$maxRetries")

                val response = apiCall()

                if (response.isSuccessful) {
                    println("ApiRetryHandler: ✅ Einfacher Retry erfolgreich nach ${attempt + 1} Versuchen")
                    return response
                } else {
                    println("ApiRetryHandler: ❌ Einfacher Retry fehlgeschlagen (${response.code()})")
                }

            } catch (e: Exception) {
                println("ApiRetryHandler: ❌ Exception bei einfachem Retry: ${e.message}")
            }

            // Delay zwischen Versuchen (aber nicht nach dem letzten)
            if (attempt < maxRetries - 1) {
                delay(delayMs)
            }
        }

        println("ApiRetryHandler: ❌ Alle einfachen Retry-Versuche fehlgeschlagen")
        return null
    }

    /**
     * Exponential Backoff Retry für kritische APIs
     */
    suspend fun <T> executeExponentialRetry(
        maxRetries: Int = 3,
        baseDelayMs: Long = 1000,
        apiCall: suspend () -> Response<T>
    ): Response<T>? {

        repeat(maxRetries) { attempt ->
            try {
                println("ApiRetryHandler: Exponential Retry-Versuch ${attempt + 1}/$maxRetries")

                val response = apiCall()

                if (response.isSuccessful) {
                    println("ApiRetryHandler: ✅ Exponential Retry erfolgreich nach ${attempt + 1} Versuchen")
                    return response
                } else {
                    println("ApiRetryHandler: ❌ Exponential Retry fehlgeschlagen (${response.code()})")
                }

            } catch (e: Exception) {
                println("ApiRetryHandler: ❌ Exception bei Exponential Retry: ${e.message}")
            }

            // Exponential Backoff: 1s, 2s, 4s, etc.
            if (attempt < maxRetries - 1) {
                val delayTime = baseDelayMs * (1L shl attempt) // 2^attempt
                println("ApiRetryHandler: Warte ${delayTime}ms vor nächstem Versuch...")
                delay(delayTime)
            }
        }

        println("ApiRetryHandler: ❌ Alle Exponential Retry-Versuche fehlgeschlagen")
        return null
    }
}
