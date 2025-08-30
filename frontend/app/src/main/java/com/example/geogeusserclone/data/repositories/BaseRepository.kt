package com.example.geogeusserclone.data.repositories

abstract class BaseRepository {
    
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(apiCall())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    protected fun <T> handleApiResponse(
        response: retrofit2.Response<T>
    ): Result<T> {
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
        }
    }
}
