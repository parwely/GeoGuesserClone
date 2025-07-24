package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.database.entities.UserEntity
import com.example.geogeusserclone.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: UserEntity? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel<AuthState>(AuthState()) {

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            userRepository.getCurrentUserFlow().collect { user ->
                setState(state.value.copy(
                    currentUser = user,
                    isLoggedIn = user != null
                ))
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            setState(state.value.copy(isLoading = true, error = null))

            userRepository.login(email, password)
                .onSuccess { user ->
                    setState(state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = user,
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
        viewModelScope.launch {
            setState(state.value.copy(isLoading = true, error = null))

            userRepository.register(username, email, password)
                .onSuccess { user ->
                    setState(state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = user,
                        error = null
                    ))
                }
                .onFailure { exception ->
                    setState(state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Registrierung fehlgeschlagen"
                    ))
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            setState(state.value.copy(
                isLoggedIn = false,
                currentUser = null,
                error = null
            ))
        }
    }

    fun clearError() {
        setState(state.value.copy(error = null))
    }
}
