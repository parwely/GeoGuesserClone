package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE authToken IS NOT NULL LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE authToken IS NOT NULL")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET authToken = :token, lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateAuthToken(userId: String, token: String?, timestamp: Long)

    @Query("UPDATE users SET totalScore = :totalScore, gamesPlayed = :gamesPlayed, bestScore = :bestScore WHERE id = :userId")
    suspend fun updateUserStats(userId: String, totalScore: Int, gamesPlayed: Int, bestScore: Int)

    @Query("DELETE FROM users WHERE authToken IS NULL")
    suspend fun deleteLoggedOutUsers()

    @Query("SELECT * FROM users WHERE authToken IS NOT NULL LIMIT 1")
    suspend fun getCurrentUserSync(): UserEntity?

    @Query("UPDATE users SET authToken = NULL, refreshToken = NULL WHERE authToken IS NOT NULL")
    suspend fun clearCurrentUser()
}