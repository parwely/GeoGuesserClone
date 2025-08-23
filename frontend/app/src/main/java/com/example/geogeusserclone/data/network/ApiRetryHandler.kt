package com.example.geogeusserclone.data.network

import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Retry-Mechanismus für robuste Backend-Integration
 * Behebt die "Retrieved locations: 0" und "401 Unauthorized" Probleme
 */
object ApiRetryHandler {

    /**
     * Führt API-Calls mit intelligenter Retry-Logik aus
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        delayMs: Long = 1000L,
        backoffMultiplier: Float = 2f,
        apiCall: suspend () -> Response<T>
    ): Response<T> {
        var lastException: Exception? = null
        var delay = delayMs

        repeat(maxRetries) { attempt ->
            try {
                val response = apiCall()

                // Erfolgreiche Response oder nicht-retryable Fehler
                if (response.isSuccessful || !isRetryableError(response.code())) {
                    return response
                }

                // Bei retryable Fehlern: warte und versuche erneut
                if (attempt < maxRetries - 1) {
                    println("ApiRetryHandler: Attempt ${attempt + 1} failed with ${response.code()}, retrying in ${delay}ms")
                    delay(delay)
                    delay = (delay * backoffMultiplier).toLong()
                }

            } catch (e: Exception) {
                lastException = e

                if (!isRetryableException(e) || attempt == maxRetries - 1) {
                    throw e
                }

                println("ApiRetryHandler: Attempt ${attempt + 1} failed with ${e.javaClass.simpleName}, retrying in ${delay}ms")
                delay(delay)
                delay = (delay * backoffMultiplier).toLong()
            }
        }

        // Alle Versuche fehlgeschlagen
        throw lastException ?: Exception("All retry attempts failed")
    }

    /**
     * Prüft ob HTTP-Statuscode retry-fähig ist
     */
    private fun isRetryableError(httpCode: Int): Boolean {
        return when (httpCode) {
            429, // Too Many Requests
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504, // Gateway Timeout
            408  // Request Timeout
            -> true
            else -> false
        }
    }

    /**
     * Prüft ob Exception retry-fähig ist
     */
    private fun isRetryableException(exception: Exception): Boolean {
        return when (exception) {
            is SocketTimeoutException,
            is UnknownHostException,
            is IOException -> true
            else -> false
        }
    }

    /**
     * Spezieller Retry für Location-Endpoints mit verschiedenen Parametern
     * KORRIGIERT: Sequenziell statt parallel um Backend nicht zu überlasten
     */
    suspend fun <T> executeLocationRetry(
        categories: List<String> = listOf("urban", "landmark", "rural"),
        difficulties: List<Int> = listOf(1, 2, 3),
        apiCall: suspend (category: String, difficulty: Int) -> Response<T>
    ): Response<T>? {

        // KORREKTUR: Mische Parameter für bessere Randomisierung
        val shuffledCategories = categories.shuffled()
        val shuffledDifficulties = difficulties.shuffled()

        // Versuche verschiedene Kombinationen SEQUENZIELL (nicht parallel)
        for (category in shuffledCategories) {
            for (difficulty in shuffledDifficulties) {
                try {
                    println("ApiRetryHandler: Versuche category=$category, difficulty=$difficulty")

                    val response = executeWithRetry(maxRetries = 1) { // Nur 1 Retry pro Kombination
                        apiCall(category, difficulty)
                    }

                    if (response.isSuccessful) {
                        println("ApiRetryHandler: ✅ Erfolgreich mit category=$category, difficulty=$difficulty")
                        return response
                    } else {
                        println("ApiRetryHandler: ❌ HTTP ${response.code()} für category=$category, difficulty=$difficulty")
                    }

                } catch (e: Exception) {
                    println("ApiRetryHandler: ❌ Exception für category=$category, difficulty=$difficulty: ${e.message}")
                    continue
                }

                // Kleine Pause zwischen Requests um Backend zu entlasten
                delay(100)
            }
        }

        return null // Alle Parameter-Kombinationen fehlgeschlagen
    }
}
