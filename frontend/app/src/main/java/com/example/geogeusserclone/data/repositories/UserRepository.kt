package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.network.LoginRequest
import com.example.geogeusserclone.data.network.RegisterRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val authInterceptor: AuthInterceptor
) {

    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()

    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val authResponse = response.body()!!
                val user = UserEntity(
                    id = authResponse.user.id,
                    username = authResponse.user.username,
                    email = authResponse.user.email,
                    authToken = authResponse.token,
                    lastLoginAt = System.currentTimeMillis()
                )

                // Set token for future requests
                authInterceptor.setToken(authResponse.token)

                // Cache user locally
                userDao.insertUser(user)

                Result.success(user)
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val authResponse = response.body()!!
                val user = UserEntity(
                    id = authResponse.user.id,
                    username = authResponse.user.username,
                    email = authResponse.user.email,
                    authToken = authResponse.token,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )

                authInterceptor.setToken(authResponse.token)
                userDao.insertUser(user)

                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        val user = getCurrentUser()
        user?.let {
            userDao.updateAuthToken(it.id, null, System.currentTimeMillis())
        }
        authInterceptor.setToken(null)
    }

    suspend fun updateUserStats(userId: String, totalScore: Int, gamesPlayed: Int, bestScore: Int) {
        userDao.updateUserStats(userId, totalScore, gamesPlayed, bestScore)
    }

    suspend fun refreshUserToken(): Result<String> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser?.authToken != null) {
                // Implement refresh token logic here if your backend supports it
                Result.success(currentUser.authToken)
            } else {
                Result.failure(Exception("No valid token to refresh"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}