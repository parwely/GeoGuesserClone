package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val currentUser: UserEntity? = null,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val isRegistering: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel<AuthState>(AuthState()) {

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUserFlow().collectLatest { user ->
                setState(state.value.copy(
                    currentUser = user,
                    isLoggedIn = user != null
                ))
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            setState(state.value.copy(error = "Email und Passwort sind erforderlich"))
            return
        }

        viewModelScope.launch {
            setState(state.value.copy(isLoading = true, error = null))

            userRepository.login(email, password)
                .onSuccess { user ->
                    setState(state.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isLoggedIn = true,
                        error = null
                    ))
                }
                .onFailure { exception ->
                    setState(state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Login fehlgeschlagen"
                    ))
                }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            setState(state.value.copy(error = "Alle Felder sind erforderlich"))
            return
        }

        viewModelScope.launch {
            setState(state.value.copy(isLoading = true, isRegistering = true, error = null))

            userRepository.register(username, email, password)
                .onSuccess { user ->
                    setState(state.value.copy(
                        isLoading = false,
                        isRegistering = false,
                        currentUser = user,
                        isLoggedIn = true,
                        error = null
                    ))
                }
                .onFailure { exception ->
                    setState(state.value.copy(
                        isLoading = false,
                        isRegistering = false,
                        error = exception.message ?: "Registrierung fehlgeschlagen"
                    ))
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            setState(state.value.copy(
                currentUser = null,
                isLoggedIn = false,
                error = null
            ))
        }
    }

    fun clearError() {
        setState(state.value.copy(error = null))
    }

    fun toggleAuthMode() {
        setState(state.value.copy(
            isRegistering = !state.value.isRegistering,
            error = null
        ))
    }
}