package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE authToken = :token")
    suspend fun getUserByToken(token: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users ORDER BY totalScore DESC LIMIT :limit")
    suspend fun getTopUsers(limit: Int): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY lastLoginAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("UPDATE users SET authToken = :token WHERE id = :userId")
    suspend fun updateAuthToken(userId: String, token: String?)

    @Query("UPDATE users SET totalScore = totalScore + :scoreIncrease WHERE id = :userId")
    suspend fun increaseUserScore(userId: String, scoreIncrease: Int)

    @Query("UPDATE users SET gamesPlayed = gamesPlayed + :gamesIncrease WHERE id = :userId")
    suspend fun increaseGamesPlayed(userId: String, gamesIncrease: Int)

    @Query("UPDATE users SET bestScore = :bestScore WHERE id = :userId AND bestScore < :bestScore")
    suspend fun updateBestScore(userId: String, bestScore: Int)

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    @Query("DELETE FROM users WHERE lastLoginAt < :olderThan")
    suspend fun deleteInactiveUsers(olderThan: Long)

    @Query("UPDATE users SET authToken = NULL")
    suspend fun clearAllAuthTokens()
}
