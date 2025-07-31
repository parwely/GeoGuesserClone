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

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: com.example.geogeusserclone.data.database.entities.UserEntity? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val currentUser = userRepository.getCurrentUser()
                _state.value = _state.value.copy(
                    isLoggedIn = currentUser != null,
                    currentUser = currentUser,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Fehler beim Laden des Benutzers: ${e.message}"
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val result = userRepository.login(email, password)

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    _state.value = _state.value.copy(
                        isLoggedIn = true,
                        currentUser = user,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Login fehlgeschlagen"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Netzwerkfehler: ${e.message}"
                )
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val result = userRepository.register(username, email, password)

                if (result.isSuccess) {
                    val user = result.getOrNull()!!
                    _state.value = _state.value.copy(
                        isLoggedIn = true,
                        currentUser = user,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Registrierung fehlgeschlagen"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Netzwerkfehler: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
                _state.value = AuthState() // Reset to initial state
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Fehler beim Abmelden: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
