package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.AuthInterceptor
import com.example.geogeusserclone.data.network.LoginRequest
import com.example.geogeusserclone.data.network.RegisterRequest
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

    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getCurrentUser()
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val loginResponse = response.body()!!

                // Setze Auth Token
                authInterceptor.setAuthToken(loginResponse.token)

                val userEntity = UserEntity(
                    id = loginResponse.user.id,
                    username = loginResponse.user.username,
                    email = loginResponse.user.email,
                    authToken = loginResponse.token,
                    totalScore = loginResponse.user.totalScore,
                    gamesPlayed = loginResponse.user.gamesPlayed,
                    bestScore = loginResponse.user.bestScore,
                    lastLoginAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )

                userDao.insertUser(userEntity)
                Result.success(userEntity)
            } else {
                Result.failure(Exception("Login fehlgeschlagen: ${response.message()}"))
            }
        } catch (e: Exception) {
            // Fallback für Offline-Modus
            val offlineUser = createOfflineUser(email)
            userDao.insertUser(offlineUser)
            Result.success(offlineUser)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val loginResponse = response.body()!!

                authInterceptor.setAuthToken(loginResponse.token)

                val userEntity = UserEntity(
                    id = loginResponse.user.id,
                    username = loginResponse.user.username,
                    email = loginResponse.user.email,
                    authToken = loginResponse.token,
                    totalScore = 0,
                    gamesPlayed = 0,
                    bestScore = 0,
                    lastLoginAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )

                userDao.insertUser(userEntity)
                Result.success(userEntity)
            } else {
                Result.failure(Exception("Registrierung fehlgeschlagen: ${response.message()}"))
            }
        } catch (e: Exception) {
            // Fallback für Offline-Modus
            val offlineUser = createOfflineUser(email, username)
            userDao.insertUser(offlineUser)
            Result.success(offlineUser)
        }
    }

    suspend fun logout() {
        authInterceptor.setAuthToken(null)
        userDao.clearCurrentUser()
    }

    suspend fun updateUserStats(totalScore: Int, gamesPlayed: Int, bestScore: Int) {
        val currentUser = userDao.getCurrentUser()
        currentUser?.let { user ->
            val updatedUser = user.copy(
                totalScore = totalScore,
                gamesPlayed = gamesPlayed,
                bestScore = if (bestScore > user.bestScore) bestScore else user.bestScore,
                lastLoginAt = System.currentTimeMillis()
            )
            userDao.updateUser(updatedUser)
        }
    }

    private fun createOfflineUser(email: String, username: String? = null): UserEntity {
        return UserEntity(
            id = UUID.randomUUID().toString(),
            username = username ?: email.substringBefore("@"),
            email = email,
            authToken = null,
            totalScore = 0,
            gamesPlayed = 0,
            bestScore = 0,
            lastLoginAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
    }
}
