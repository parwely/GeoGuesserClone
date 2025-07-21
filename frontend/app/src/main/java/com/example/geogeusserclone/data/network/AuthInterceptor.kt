package com.example.geogeusserclone.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private var authToken: String? = null
    private var refreshToken: String? = null
    private var tokenRefreshCallback: (suspend () -> String?)? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Add auth token if available
        val requestBuilder = originalRequest.newBuilder()
        authToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // Handle 401 Unauthorized - try to refresh token
        if (response.code == 401 && refreshToken != null) {
            response.close()

            val newToken = runBlocking {
                tokenRefreshCallback?.invoke()
            }

            if (newToken != null) {
                authToken = newToken

                // Retry the original request with new token
                val newRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $newToken")
                    .build()

                return chain.proceed(newRequest)
            }
        }

        return response
    }

    fun setToken(token: String?) {
        this.authToken = token
    }

    //Retry Logic:
    fun setRefreshToken(token: String?) {
        this.refreshToken = token
    }

    fun setTokenRefreshCallback(callback: suspend () -> String?) {
        this.tokenRefreshCallback = callback
    }

    fun clearTokens() {
        authToken = null
        refreshToken = null
        tokenRefreshCallback = null
    }
}