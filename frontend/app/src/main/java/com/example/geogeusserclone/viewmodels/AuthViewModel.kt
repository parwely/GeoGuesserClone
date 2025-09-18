/**
 * AuthViewModel.kt
 *
 * Diese Datei enthält das ViewModel für die Benutzerauthentifizierung der GeoGuess-App.
 * Sie verwaltet Login, Registrierung und den aktuellen Benutzer-Session-Zustand.
 *
 * Architektur-Integration:
 * - MVVM Pattern: Geschäftslogik für Authentifizierung getrennt von UI
 * - State Management: Zentraler AuthState für Login-Status und Benutzerdaten
 * - Repository Pattern: Nutzt UserRepository für Datenoperationen
 * - Coroutines: Asynchrone Netzwerk- und Datenbankoperationen
 * - Session Persistence: Automatische Wiederherstellung von gespeicherten Sessions
 */
package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Datenklasse für Authentifizierungs-Zustand
 *
 * Enthält alle relevanten Informationen über den aktuellen Login-Status
 * und wird von der UI observiert für reaktive Updates.
 *
 * @param isLoggedIn Ob der Benutzer aktuell angemeldet ist
 * @param isLoading Ob gerade eine Authentifizierungs-Operation läuft
 * @param error Aktuelle Fehlermeldung (null wenn kein Fehler)
 * @param currentUser Aktuell angemeldeter Benutzer (null wenn nicht angemeldet)
 */
data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: com.example.geogeusserclone.data.database.entities.UserEntity? = null
)

/**
 * ViewModel für Authentifizierungs-Geschäftslogik
 *
 * Verwaltet alle Aspekte der Benutzerauthentifizierung:
 * - Login und Registrierung neuer Benutzer
 * - Session-Wiederherstellung beim App-Start
 * - Logout und Session-Management
 * - Fehlerbehandlung für Netzwerk- und Validierungsfehler
 * - Automatische Fallbacks für Offline-Nutzung
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // Privater State für interne Änderungen
    private val _state = MutableStateFlow(AuthState())

    // Öffentlicher State für UI-Beobachtung
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * Initialisiert ViewModel und prüft existierende Session
     */
    init {
        checkCurrentUser()
    }

    /**
     * Prüft ob bereits ein Benutzer angemeldet ist
     *
     * Wird beim App-Start aufgerufen um gespeicherte Sessions zu laden.
     * Ermöglicht nahtlose Weiternutzung ohne erneutes Login.
     */
    private fun checkCurrentUser() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val currentUser = userRepository.getCurrentUser()
                _state.value = _state.value.copy(
                    isLoggedIn = currentUser != null,
                    currentUser = currentUser,
                    isLoading = false,
                    error = null
                )

                if (currentUser != null) {
                    println("AuthViewModel: ✅ Session wiederhergestellt für ${currentUser.username}")
                } else {
                    println("AuthViewModel: ℹ️ Keine gespeicherte Session gefunden")
                }
            } catch (e: Exception) {
                println("AuthViewModel: ❌ Fehler beim Laden der Session: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Fehler beim Laden des Benutzers: ${e.message}",
                    isLoggedIn = false,
                    currentUser = null
                )
            }
        }
    }

    /**
     * Meldet einen Benutzer mit E-Mail und Passwort an
     *
     * Führt Validierung durch, kontaktiert das Repository für Authentifizierung
     * und aktualisiert den State entsprechend. Behandelt sowohl Online- als auch
     * Offline-Szenarien mit automatischen Fallbacks.
     *
     * @param email E-Mail-Adresse des Benutzers
     * @param password Passwort des Benutzers
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            // Eingabe-Validierung
            if (email.isBlank() || password.isBlank()) {
                _state.value = _state.value.copy(
                    error = "E-Mail und Passwort dürfen nicht leer sein"
                )
                return@launch
            }

            if (!isValidEmail(email)) {
                _state.value = _state.value.copy(
                    error = "Bitte geben Sie eine gültige E-Mail-Adresse ein"
                )
                return@launch
            }

            // Setze Loading-Zustand
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            try {
                println("AuthViewModel: 🔐 Starte Login für $email")
                val result = userRepository.loginUser(email, password)

                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    _state.value = _state.value.copy(
                        isLoggedIn = true,
                        currentUser = user,
                        isLoading = false,
                        error = null
                    )
                    println("AuthViewModel: ✅ Login erfolgreich für ${user.username}")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Login fehlgeschlagen"
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessage,
                        isLoggedIn = false,
                        currentUser = null
                    )
                    println("AuthViewModel: ❌ Login fehlgeschlagen: $errorMessage")
                }
            } catch (e: Exception) {
                println("AuthViewModel: ❌ Login-Exception: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Unerwarteter Fehler beim Login: ${e.message}",
                    isLoggedIn = false,
                    currentUser = null
                )
            }
        }
    }

    /**
     * Registriert einen neuen Benutzer
     *
     * Führt umfassende Validierung durch, erstellt neuen Account über Repository
     * und meldet den Benutzer automatisch an. Unterstützt Offline-Registrierung
     * mit lokaler Speicherung.
     *
     * @param username Gewünschter Benutzername
     * @param email E-Mail-Adresse für den Account
     * @param password Passwort (mindestens 6 Zeichen)
     */
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            // Umfassende Eingabe-Validierung
            val validationError = validateRegistrationInput(username, email, password)
            if (validationError != null) {
                _state.value = _state.value.copy(error = validationError)
                return@launch
            }

            // Setze Loading-Zustand
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            try {
                println("AuthViewModel: 📝 Starte Registrierung für $username ($email)")
                val result = userRepository.registerUser(username, email, password)

                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    _state.value = _state.value.copy(
                        isLoggedIn = true,
                        currentUser = user,
                        isLoading = false,
                        error = null
                    )
                    println("AuthViewModel: ✅ Registrierung erfolgreich für ${user.username}")
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Registrierung fehlgeschlagen"
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessage,
                        isLoggedIn = false,
                        currentUser = null
                    )
                    println("AuthViewModel: ❌ Registrierung fehlgeschlagen: $errorMessage")
                }
            } catch (e: Exception) {
                println("AuthViewModel: ❌ Registrierung-Exception: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Unerwarteter Fehler bei der Registrierung: ${e.message}",
                    isLoggedIn = false,
                    currentUser = null
                )
            }
        }
    }

    /**
     * Validiert Registrierungseingaben
     *
     * @param username Benutzername
     * @param email E-Mail-Adresse
     * @param password Passwort
     * @return Fehlermeldung oder null wenn alles gültig ist
     */
    private fun validateRegistrationInput(username: String, email: String, password: String): String? {
        return when {
            username.isBlank() -> "Benutzername darf nicht leer sein"
            username.length < 3 -> "Benutzername muss mindestens 3 Zeichen lang sein"
            username.length > 20 -> "Benutzername darf maximal 20 Zeichen lang sein"
            !username.matches(Regex("^[a-zA-Z0-9_-]+$")) ->
                "Benutzername darf nur Buchstaben, Zahlen, _ und - enthalten"
            email.isBlank() -> "E-Mail darf nicht leer sein"
            !isValidEmail(email) -> "Bitte geben Sie eine gültige E-Mail-Adresse ein"
            password.isBlank() -> "Passwort darf nicht leer sein"
            password.length < 6 -> "Passwort muss mindestens 6 Zeichen lang sein"
            password.length > 128 -> "Passwort darf maximal 128 Zeichen lang sein"
            else -> null
        }
    }

    /**
     * Validiert E-Mail-Format
     *
     * @param email Zu validierende E-Mail-Adresse
     * @return true wenn E-Mail gültig ist
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Meldet den aktuellen Benutzer ab
     *
     * Löscht die lokale Session und setzt den AuthState zurück.
     * Leitet zur Login-Seite weiter.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                println("AuthViewModel: 🚪 Starte Logout für ${_state.value.currentUser?.username}")

                // Lösche Session im Repository
                userRepository.logout()

                // Setze State zurück
                _state.value = AuthState(
                    isLoggedIn = false,
                    currentUser = null,
                    isLoading = false,
                    error = null
                )

                println("AuthViewModel: ✅ Logout erfolgreich")
            } catch (e: Exception) {
                println("AuthViewModel: ❌ Logout-Fehler: ${e.message}")
                // Auch bei Fehlern: State zurücksetzen für Sicherheit
                _state.value = AuthState(
                    isLoggedIn = false,
                    currentUser = null,
                    isLoading = false,
                    error = "Fehler beim Abmelden: ${e.message}"
                )
            }
        }
    }

    /**
     * Löscht die aktuelle Fehlermeldung
     *
     * Wird von der UI aufgerufen um Fehlermeldungen zu schließen.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Aktualisiert Benutzerstatistiken
     *
     * Wird nach abgeschlossenen Spielen aufgerufen um Score und
     * Spielanzahl zu aktualisieren.
     *
     * @param additionalScore Zusätzlicher Score aus dem Spiel
     * @param gamesPlayed Anzahl zusätzlich gespielter Spiele
     * @param newBestScore Neuer Bestrekord (falls erreicht)
     */
    fun updateUserStats(additionalScore: Int, gamesPlayed: Int, newBestScore: Int? = null) {
        viewModelScope.launch {
            try {
                val currentUser = _state.value.currentUser ?: return@launch

                // Update im Repository
                userRepository.updateUserStats(
                    totalScore = additionalScore,
                    gamesPlayed = gamesPlayed,
                    bestScore = newBestScore ?: currentUser.bestScore
                )

                // Update lokalen State
                val updatedUser = currentUser.copy(
                    totalScore = currentUser.totalScore + additionalScore,
                    gamesPlayed = currentUser.gamesPlayed + gamesPlayed,
                    bestScore = maxOf(currentUser.bestScore, newBestScore ?: 0)
                )

                _state.value = _state.value.copy(currentUser = updatedUser)

                println("AuthViewModel: ✅ Benutzerstatistiken aktualisiert: +$additionalScore Punkte, +$gamesPlayed Spiele")
            } catch (e: Exception) {
                println("AuthViewModel: ❌ Fehler beim Aktualisieren der Statistiken: ${e.message}")
                // Fehler nicht an UI weiterleiten, da es kein kritischer Fehler ist
            }
        }
    }

    /**
     * Lädt aktuelle Benutzerdaten neu
     *
     * Synchronisiert lokale Daten mit dem Server.
     * Nützlich nach längerer Inaktivität oder Netzwerkproblemen.
     */
    fun refreshUserData() {
        viewModelScope.launch {
            val currentUser = _state.value.currentUser ?: return@launch

            try {
                println("AuthViewModel: 🔄 Aktualisiere Benutzerdaten für ${currentUser.username}")

                // Hole aktuelle Daten vom Repository
                val refreshedUser = userRepository.getCurrentUser()

                if (refreshedUser != null) {
                    _state.value = _state.value.copy(
                        currentUser = refreshedUser,
                        error = null
                    )
                    println("AuthViewModel: ✅ Benutzerdaten erfolgreich aktualisiert")
                }
            } catch (e: Exception) {
                println("AuthViewModel: ❌ Fehler beim Aktualisieren der Benutzerdaten: ${e.message}")
                // Setze Fehler nur wenn kritisch, sonst ignorieren
                if (e.message?.contains("unauthorized") == true || e.message?.contains("invalid") == true) {
                    _state.value = _state.value.copy(
                        error = "Session abgelaufen. Bitte melden Sie sich erneut an.",
                        isLoggedIn = false,
                        currentUser = null
                    )
                }
            }
        }
    }

    /**
     * Prüft ob die aktuelle Session noch gültig ist
     *
     * @return true wenn Session gültig und aktiv ist
     */
    fun isSessionValid(): Boolean {
        val currentUser = _state.value.currentUser
        if (currentUser == null || !_state.value.isLoggedIn) {
            return false
        }

        // Prüfe ob Session zu alt ist (z.B. 24 Stunden)
        val sessionAge = System.currentTimeMillis() - (currentUser.lastLoginAt ?: 0)
        val maxSessionAge = 24 * 60 * 60 * 1000L // 24 Stunden

        return sessionAge < maxSessionAge
    }
}
