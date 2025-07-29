package com.example.geogeusserclone.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")

        authToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
