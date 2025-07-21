package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.network.NetworkResult
import com.example.geogeusserclone.data.network.safeApiCall
import retrofit2.Response

abstract class BaseRepository {

    protected suspend fun <T> executeNetworkCall(
        apiCall: suspend () -> Response<T>
    ): NetworkResult<T> {
        return safeApiCall { apiCall() }
    }

    protected suspend fun <T, R> executeWithFallback(
        networkCall: suspend () -> NetworkResult<T>,
        localCall: suspend () -> R,
        transform: (T) -> R
    ): R {
        return when (val result = networkCall()) {
            is NetworkResult.Success -> {
                try {
                    transform(result.data)
                } catch (e: Exception) {
                    localCall()
                }
            }
            is NetworkResult.Error -> localCall()
            is NetworkResult.Loading -> localCall()
        }
    }
}