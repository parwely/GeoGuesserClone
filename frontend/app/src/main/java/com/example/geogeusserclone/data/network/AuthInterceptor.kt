package com.example.geogeusserclone.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")

        // Füge Token hinzu wenn verfügbar und für geschützte Endpoints benötigt
        val token = tokenManager.getToken()
        if (token != null && needsAuth(originalRequest.url.encodedPath)) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun needsAuth(path: String): Boolean {
        return path.contains("/games/") ||
               path.contains("/auth/refresh") ||
               path.contains("/auth/logout")
    }

    // Legacy support für direkte Token-Setzung
    fun setAuthToken(token: String?) {
        if (token != null) {
            tokenManager.saveToken(token)
        } else {
            tokenManager.clearTokens()
        }
    }
}
