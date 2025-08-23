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
     * VOLLSTÄNDIG KORRIGIERT: Detaillierte Endpoint-Analyse basierend auf Backend-Logs
     */
    private fun needsAuthentication(path: String): Boolean {
        return when {
            // AUTH-PFLICHTIGE ENDPOINTS (benötigen Bearer Token)
            path.contains("/auth/refresh") -> true
            path.contains("/auth/logout") -> true
            path.contains("/user/") -> true
            path.contains("/games/") -> true // KRITISCH: War false, jetzt true

            // ÖFFENTLICHE ENDPOINTS (kein Token benötigt)
            path.contains("/auth/login") -> false
            path.contains("/auth/register") -> false
            path.contains("/locations/") -> false // Locations sind öffentlich
            path.contains("/health") -> false

            // SICHERE DEFAULT-REGEL: Unbekannte Endpunkte benötigen Auth
            else -> true // Geändert von false zu true für bessere Sicherheit
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
