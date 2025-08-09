package com.example.geogeusserclone.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    private var currentToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Hole aktuellen Token
        val token = currentToken ?: tokenManager.getToken()

        // Prüfe ob dieser Request Authentication benötigt
        val needsAuth = needsAuthentication(originalRequest.url.encodedPath)

        return if (token != null && needsAuth) {
            // Füge Authorization Header hinzu
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

            val response = chain.proceed(authenticatedRequest)

            // Handle 401 Unauthorized
            if (response.code == 401) {
                response.close()

                // Versuche Token zu refreshen
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    // Hier könnte Token Refresh Logic implementiert werden
                    // Für jetzt loggen wir nur aus
                    tokenManager.clearTokens()
                    currentToken = null
                }

                // Retry ohne Token
                val retryRequest = originalRequest.newBuilder().build()
                chain.proceed(retryRequest)
            } else {
                response
            }
        } else {
            // Kein Token oder kein Auth benötigt
            chain.proceed(originalRequest)
        }
    }

    /**
     * Prüft ob der Request Authentication benötigt
     */
    private fun needsAuthentication(path: String): Boolean {
        return when {
            path.contains("/auth/refresh") -> true
            path.contains("/auth/logout") -> true
            path.contains("/games/") -> true
            path.contains("/user/") -> true
            // Public Endpoints benötigen keine Auth
            path.contains("/auth/login") -> false
            path.contains("/auth/register") -> false
            path.contains("/locations/random") -> false
            path.contains("/locations/") && path.contains("/streetview") -> false
            path.contains("/health") -> false
            else -> false
        }
    }

    /**
     * Setzt den Auth Token für nachfolgende Requests
     */
    fun setAuthToken(token: String?) {
        currentToken = token
        if (token != null) {
            tokenManager.saveToken(token)
        } else {
            tokenManager.clearTokens()
        }
    }

    /**
     * Gibt den aktuellen Token zurück
     */
    fun getCurrentToken(): String? {
        return currentToken ?: tokenManager.getToken()
    }
}
