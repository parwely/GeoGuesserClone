package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.network.LoginRequest
import com.example.geogeusserclone.data.network.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val authInterceptor: AuthInterceptor
) : BaseRepository() {

    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUser()

    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUserSync()

    suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val authResponse = response.body()!!

                // Set tokens in AuthInterceptor
                authInterceptor.setToken(authResponse.token)
                authInterceptor.setRefreshToken(authResponse.refreshToken)

                val user = UserEntity(
                    id = authResponse.user.id,
                    username = authResponse.user.username,
                    email = authResponse.user.email,
                    authToken = authResponse.token,
                    refreshToken = authResponse.refreshToken
                )

                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Login fehlgeschlagen"))
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

                authInterceptor.setToken(authResponse.token)
                authInterceptor.setRefreshToken(authResponse.refreshToken)

                val user = UserEntity(
                    id = authResponse.user.id,
                    username = authResponse.user.username,
                    email = authResponse.user.email,
                    authToken = authResponse.token,
                    refreshToken = authResponse.refreshToken
                )

                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Registrierung fehlgeschlagen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (e: Exception) {
            // Continue with local logout even if API call fails
        }

        authInterceptor.clearTokens()
        userDao.clearCurrentUser()
    }

    suspend fun refreshToken(): String? {
        return try {
            val currentUser = getCurrentUser()
            currentUser?.refreshToken?.let { refreshToken ->
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                if (response.isSuccessful) {
                    val authResponse = response.body()!!

                    authInterceptor.setToken(authResponse.token)
                    authInterceptor.setRefreshToken(authResponse.refreshToken)

                    val updatedUser = currentUser.copy(
                        authToken = authResponse.token,
                        refreshToken = authResponse.refreshToken
                    )
                    userDao.insertUser(updatedUser)

                    authResponse.token
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}