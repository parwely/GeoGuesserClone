package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.network.LoginRequest
import com.example.geogeusserclone.data.network.RegisterRequest
import com.example.geogeusserclone.data.network.RefreshTokenRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val authInterceptor: AuthInterceptor
) : BaseRepository() {

    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()

    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUserSync()

    suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                val loginResponse = response.body()!!
                val user = UserEntity(
                    id = loginResponse.user.id,
                    username = loginResponse.user.username,
                    email = loginResponse.user.email,
                    authToken = loginResponse.token,
                    refreshToken = loginResponse.refreshToken,
                    totalScore = loginResponse.user.totalScore ?: 0,
                    gamesPlayed = loginResponse.user.gamesPlayed ?: 0,
                    bestScore = 0, // Verwende Standardwert da bestScore möglicherweise nicht verfügbar ist
                    lastLoginAt = System.currentTimeMillis()
                )

                userDao.clearCurrentUser()
                userDao.insertUser(user)
                authInterceptor.setAuthToken(loginResponse.token)

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
                val registerResponse = response.body()!!
                val user = UserEntity(
                    id = registerResponse.user.id,
                    username = registerResponse.user.username,
                    email = registerResponse.user.email,
                    authToken = registerResponse.token,
                    refreshToken = registerResponse.refreshToken,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )

                userDao.clearCurrentUser()
                userDao.insertUser(user)
                authInterceptor.setAuthToken(registerResponse.token)

                Result.success(user)
            } else {
                Result.failure(Exception("Registrierung fehlgeschlagen"))
            }
        } catch (e: Exception) {
            // Offline Fallback für Registrierung
            val offlineUser = UserEntity(
                id = UUID.randomUUID().toString(),
                username = username,
                email = email,
                authToken = "offline_token",
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )

            userDao.clearCurrentUser()
            userDao.insertUser(offlineUser)
            authInterceptor.setAuthToken("offline_token")

            Result.success(offlineUser)
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (e: Exception) {
            // Ignoriere Netzwerkfehler beim Logout
        } finally {
            userDao.clearCurrentUser()
            authInterceptor.clearAuthToken()
        }
    }

    suspend fun updateUserStats(userId: String, totalScore: Int, gamesPlayed: Int, bestScore: Int) {
        userDao.updateUserStats(userId, totalScore, gamesPlayed, bestScore)

        try {
            // Versuche auch online zu synchronisieren
            apiService.updateUserStats(userId, totalScore, gamesPlayed, bestScore)
        } catch (e: Exception) {
            // Ignoriere Netzwerkfehler
        }
    }

    suspend fun refreshToken(): Result<String> {
        return try {
            val currentUser = getCurrentUser()
            if (currentUser?.refreshToken != null) {
                val response = apiService.refreshToken(RefreshTokenRequest(currentUser.refreshToken))
                if (response.isSuccessful) {
                    val tokenResponse = response.body()!!
                    userDao.updateAuthToken(currentUser.id, tokenResponse.token, System.currentTimeMillis())
                    authInterceptor.setAuthToken(tokenResponse.token)
                    Result.success(tokenResponse.token)
                } else {
                    Result.failure(Exception("Token refresh failed"))
                }
            } else {
                Result.failure(Exception("No refresh token available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}