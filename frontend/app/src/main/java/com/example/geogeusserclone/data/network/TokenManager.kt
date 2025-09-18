/**
 * TokenManager.kt
 *
 * Diese Datei verwaltet die sichere Speicherung und Verwaltung von Authentifizierungs-Tokens
 * für die GeoGuess-App. Sie abstrahiert die Token-Persistierung über SharedPreferences
 * und bietet automatische Ablaufprüfung.
 *
 * Architektur-Integration:
 * - Security Layer: Sichere Token-Speicherung für API-Authentifizierung
 * - Session Management: Automatische Token-Ablaufprüfung und -erneuerung
 * - Dependency Injection: Hilt-managed Singleton für App-weite Verfügbarkeit
 * - Encryption: SharedPreferences-basierte lokale Speicherung
 * - Token Lifecycle: Verwaltet Access- und Refresh-Tokens
 */
package com.example.geogeusserclone.data.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zentrale Token-Verwaltung für Authentifizierung
 *
 * Diese Klasse verwaltet alle Aspekte der Token-Sicherheit:
 * - Sichere lokale Speicherung von Access- und Refresh-Tokens
 * - Automatische Ablaufprüfung für Token-Gültigkeit
 * - Token-Aktualisierung und -Löschung
 * - Thread-sichere Operationen über SharedPreferences
 * - Session-Persistierung zwischen App-Starts
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Private SharedPreferences-Instanz für sichere Token-Speicherung
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs", 
        Context.MODE_PRIVATE
    )

    companion object {
        // Konstanten für SharedPreferences-Keys
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }

    /**
     * Speichert einen neuen Access-Token mit Ablaufzeit
     *
     * Persistiert den Token sicher in SharedPreferences zusammen mit
     * der berechneten Ablaufzeit. Der Token wird automatisch als ungültig
     * betrachtet sobald die Ablaufzeit erreicht ist.
     *
     * @param token Der zu speichernde Access-Token
     * @param expiresIn Gültigkeitsdauer (Standard: "7d" für 7 Tage)
     */
    fun saveToken(token: String, expiresIn: String = "7d") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, calculateExpiryTime(expiresIn))
            .apply()
    }

    /**
     * Ruft den aktuellen Access-Token ab (mit Gültigkeitsprüfung)
     *
     * Überprüft automatisch die Ablaufzeit und gibt nur gültige Tokens zurück.
     * Abgelaufene Tokens werden automatisch als null behandelt.
     *
     * @return Gültiger Access-Token oder null wenn abgelaufen/nicht vorhanden
     */
    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        
        return if (token != null && System.currentTimeMillis() < expiry) {
            token
        } else {
            null
        }
    }

    /**
     * Speichert einen Refresh-Token für Token-Erneuerung
     *
     * Refresh-Tokens haben typischerweise eine längere Gültigkeitsdauer
     * als Access-Tokens und werden zur automatischen Token-Erneuerung verwendet.
     *
     * @param refreshToken Der zu speichernde Refresh-Token
     */
    fun saveRefreshToken(refreshToken: String) {
        prefs.edit()
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    /**
     * Ruft den gespeicherten Refresh-Token ab
     *
     * @return Refresh-Token oder null wenn nicht vorhanden
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Prüft ob ein gültiger Access-Token verfügbar ist
     *
     * Convenience-Methode für schnelle Gültigkeitsprüfung ohne Token-Abruf.
     * Nützlich für bedingte UI-Anzeige oder Routing-Entscheidungen.
     *
     * @return true wenn ein gültiger Token verfügbar ist
     */
    fun hasValidToken(): Boolean {
        return getToken() != null
    }

    /**
     * Löscht alle gespeicherten Tokens (Logout)
     *
     * Entfernt sowohl Access- als auch Refresh-Tokens aus der lokalen Speicherung.
     * Wird typischerweise beim Logout oder bei Authentifizierungsfehlern aufgerufen.
     */
    fun clearTokens() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }

    /**
     * Berechnet die absolute Ablaufzeit basierend auf Gültigkeitsdauer
     *
     * Konvertiert relative Zeitangaben (z.B. "7d", "2h") in absolute
     * Unix-Zeitstempel für die Ablaufprüfung.
     *
     * @param expiresIn Gültigkeitsdauer als String (z.B. "7d", "24h", "3600s")
     * @return Unix-Zeitstempel der Ablaufzeit
     */
    private fun calculateExpiryTime(expiresIn: String): Long {
        val currentTime = System.currentTimeMillis()

        // Parse verschiedene Zeitformat-Strings
        return when {
            expiresIn.endsWith("d") -> {
                val days = expiresIn.dropLast(1).toLongOrNull() ?: 7
                currentTime + (days * 24 * 60 * 60 * 1000)
            }
            expiresIn.endsWith("h") -> {
                val hours = expiresIn.dropLast(1).toLongOrNull() ?: 24
                currentTime + (hours * 60 * 60 * 1000)
            }
            expiresIn.endsWith("m") -> {
                val minutes = expiresIn.dropLast(1).toLongOrNull() ?: 60
                currentTime + (minutes * 60 * 1000)
            }
            expiresIn.endsWith("s") -> {
                val seconds = expiresIn.dropLast(1).toLongOrNull() ?: 3600
                currentTime + (seconds * 1000)
            }
            else -> {
                // Default: 7 Tage
                currentTime + (7 * 24 * 60 * 60 * 1000)
            }
        }
    }

    /**
     * Gibt die verbleibende Token-Gültigkeitsdauer zurück
     *
     * Berechnet die Zeit bis zum Token-Ablauf in Millisekunden.
     * Nützlich für proaktive Token-Erneuerung vor Ablauf.
     *
     * @return Verbleibende Gültigkeitsdauer in Millisekunden (0 wenn abgelaufen)
     */
    fun getTokenTimeToLive(): Long {
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0)
        val currentTime = System.currentTimeMillis()
        return maxOf(0, expiry - currentTime)
    }

    /**
     * Prüft ob der Token bald abläuft (innerhalb der nächsten Stunde)
     *
     * Ermöglicht proaktive Token-Erneuerung vor dem tatsächlichen Ablauf
     * um Unterbrechungen zu vermeiden.
     *
     * @return true wenn Token innerhalb der nächsten Stunde abläuft
     */
    fun isTokenExpiringSoon(): Boolean {
        val timeToLive = getTokenTimeToLive()
        return timeToLive > 0 && timeToLive < (60 * 60 * 1000) // Weniger als 1 Stunde
    }
}
