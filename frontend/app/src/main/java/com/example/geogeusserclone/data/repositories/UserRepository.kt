package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.database.dao.UserDao
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.LoginRequest
import com.example.geogeusserclone.data.network.RegisterRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) {

    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getCurrentUser()
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.success) {
                    val userData = authResponse.data.user
                    val userEntity = UserEntity(
                        id = userData.id.toString(),
                        username = userData.username,
                        email = userData.email,
                        authToken = authResponse.data.token,
                        totalScore = userData.totalScore,
                        gamesPlayed = userData.gamesPlayed,
                        bestScore = userData.bestScore,
                        lastLoginAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )

                    userDao.insertUser(userEntity)
                    Result.success(userEntity)
                } else {
                    Result.failure(Exception("Login fehlgeschlagen"))
                }
            } else {
                Result.failure(Exception("Netzwerkfehler beim Login"))
            }
        } catch (e: Exception) {
            // Offline-Fallback: Prüfe lokale Benutzer
            val localUser = userDao.getUserByEmail(email)
            if (localUser != null) {
                userDao.updateLastLogin(localUser.id, System.currentTimeMillis())
                Result.success(localUser)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun registerUser(username: String, email: String, password: String): Result<UserEntity> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.success) {
                    val userData = authResponse.data.user
                    val userEntity = UserEntity(
                        id = userData.id.toString(),
                        username = userData.username,
                        email = userData.email,
                        authToken = authResponse.data.token,
                        totalScore = 0,
                        gamesPlayed = 0,
                        bestScore = 0,
                        lastLoginAt = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )

                    userDao.insertUser(userEntity)
                    Result.success(userEntity)
                } else {
                    Result.failure(Exception("Registrierung fehlgeschlagen"))
                }
            } else {
                Result.failure(Exception("Netzwerkfehler bei der Registrierung"))
            }
        } catch (e: Exception) {
            // Offline-Fallback: Erstelle lokalen Benutzer
            val offlineUser = UserEntity(
                id = "offline_${System.currentTimeMillis()}",
                username = username,
                email = email,
                authToken = null,
                totalScore = 0,
                gamesPlayed = 0,
                bestScore = 0,
                lastLoginAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

            userDao.insertUser(offlineUser)
            Result.success(offlineUser)
        }
    }

    suspend fun updateUserStats(totalScore: Int, gamesPlayed: Int, bestScore: Int) {
        val currentUser = getCurrentUser()
        if (currentUser != null) {
            userDao.increaseUserScore(currentUser.id, totalScore)
            userDao.increaseGamesPlayed(currentUser.id, gamesPlayed)
            userDao.updateBestScore(currentUser.id, bestScore)
        }
    }

    suspend fun insertEmergencyUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun logout() {
        val currentUser = getCurrentUser()
        if (currentUser != null) {
            userDao.updateAuthToken(currentUser.id, null)
        }
    }

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    suspend fun getTopUsers(limit: Int = 10): List<UserEntity> {
        return userDao.getTopUsers(limit)
    }

    suspend fun deleteInactiveUsers(olderThanDays: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        userDao.deleteInactiveUsers(cutoffTime)
    }
}
