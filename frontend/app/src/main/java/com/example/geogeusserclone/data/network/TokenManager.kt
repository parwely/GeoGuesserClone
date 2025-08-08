package com.example.geogeusserclone.data.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }

    fun saveToken(token: String, expiresIn: String = "7d") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, calculateExpiryTime(expiresIn))
            .apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)

        return if (token != null && System.currentTimeMillis() < expiry) {
            token
        } else {
            null
        }
    }

    fun saveRefreshToken(refreshToken: String) {
        prefs.edit()
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }

    fun isTokenValid(): Boolean {
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() < expiry
    }

    private fun calculateExpiryTime(expiresIn: String): Long {
        val currentTime = System.currentTimeMillis()
        return when {
            expiresIn.endsWith("d") -> {
                val days = expiresIn.removeSuffix("d").toLongOrNull() ?: 7
                currentTime + (days * 24 * 60 * 60 * 1000)
            }
            expiresIn.endsWith("h") -> {
                val hours = expiresIn.removeSuffix("h").toLongOrNull() ?: 24
                currentTime + (hours * 60 * 60 * 1000)
            }
            expiresIn.endsWith("m") -> {
                val minutes = expiresIn.removeSuffix("m").toLongOrNull() ?: 60
                currentTime + (minutes * 60 * 1000)
            }
            else -> {
                // Default: 7 Tage
                currentTime + (7 * 24 * 60 * 60 * 1000)
            }
        }
    }
}
