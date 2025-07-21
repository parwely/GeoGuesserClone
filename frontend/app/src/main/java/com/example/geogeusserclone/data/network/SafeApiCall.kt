package com.example.geogeusserclone.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

suspend fun <T> safeApiCall(
    apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    NetworkResult.Success(body)
                } ?: NetworkResult.Error("Empty response body")
            } else {
                NetworkResult.Error(
                    message = response.message() ?: "Unknown error",
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message ?: "Network error occurred"
            )
        }
    }
}