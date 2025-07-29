package com.example.geogeusserclone.data.database.dao

import androidx.room.*
import com.example.geogeusserclone.data.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY lastLoginAt DESC LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearCurrentUser()

    @Query("SELECT * FROM users ORDER BY totalScore DESC LIMIT :limit")
    suspend fun getTopUsers(limit: Int = 10): List<UserEntity>

    @Query("UPDATE users SET totalScore = totalScore + :scoreToAdd, gamesPlayed = gamesPlayed + 1 WHERE id = :userId")
    suspend fun updateUserScore(userId: String, scoreToAdd: Int)

    @Query("UPDATE users SET bestScore = :newBestScore WHERE id = :userId AND bestScore < :newBestScore")
    suspend fun updateBestScore(userId: String, newBestScore: Int)
}
