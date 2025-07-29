package com.example.geogeusserclone.data.network

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    data class Exception<T>(val e: Throwable) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}
