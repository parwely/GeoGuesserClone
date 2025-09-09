package com.example.geogeusserclone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geogeusserclone.data.repositories.SessionRepository
import com.example.geogeusserclone.data.repositories.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun connectSocket(jwt: String) {
        sessionRepository.connectSocket(jwt)
        sessionRepository.onEvent("error") { errorJson ->
            _error.value = errorJson.optString("message")
        }
        sessionRepository.onEvent("session-started") { json ->
            // Parse und update State
            val sessionId = json.optString("sessionId")
            val status = json.optString("status")
            _state.value = _state.value.copy(sessionId = sessionId, status = status)
        }
        sessionRepository.onEvent("player-joined") { json ->
            // Spieler hinzufügen
            val userId = json.optJSONObject("player")?.optString("userId") ?: ""
            val username = json.optJSONObject("player")?.optString("username") ?: ""
            val newPlayer = com.example.geogeusserclone.data.repositories.PlayerInfo(userId, username)
            _state.value = _state.value.copy(players = _state.value.players + newPlayer)
        }
        sessionRepository.onEvent("player-left") { json ->
            val userId = json.optString("userId")
            _state.value = _state.value.copy(players = _state.value.players.filter { it.userId != userId })
        }
        sessionRepository.onEvent("session-ended") { json ->
            _state.value = _state.value.copy(status = "ended")
        }
    }

    fun disconnectSocket() {
        sessionRepository.disconnectSocket()
    }

    fun createSession(mode: String, settings: Map<String, Any>) {
        viewModelScope.launch {
            val result = sessionRepository.createSession(mode, settings)
            result.fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(sessionId = response.sessionId, settings = response.settings, status = "waiting")
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
        }
    }

    fun joinSession(sessionId: String) {
        viewModelScope.launch {
            val result = sessionRepository.joinSession(sessionId)
            result.fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(sessionId = response.sessionId, players = response.players, status = "waiting")
                },
                onFailure = { error ->
                    _error.value = error.message
                }
            )
        }
    }

    fun submitGuess(lat: Double, lng: Double, roundNumber: Int) {
        sessionRepository.emitSubmitGuess(lat, lng, roundNumber)
    }

    fun clearError() {
        _error.value = null
    }
}

