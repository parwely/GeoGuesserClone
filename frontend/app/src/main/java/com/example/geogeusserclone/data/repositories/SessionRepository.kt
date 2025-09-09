package com.example.geogeusserclone.data.repositories

import com.example.geogeusserclone.data.network.ApiService
import com.example.geogeusserclone.data.network.SocketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val apiService: ApiService,
    private val socketService: SocketService
) {
    private val _sessionState = MutableStateFlow<SessionState?>(null)
    val sessionState: StateFlow<SessionState?> = _sessionState

    fun connectSocket(jwt: String) {
        socketService.connect(jwt,
            onConnected = { /* Handle connect */ },
            onError = { error -> /* Handle error */ }
        )
    }

    fun disconnectSocket() {
        socketService.disconnect()
    }

    fun emitSubmitGuess(lat: Double, lng: Double, roundNumber: Int) {
        val payload = JSONObject().apply {
            put("lat", lat)
            put("lng", lng)
            put("roundNumber", roundNumber)
        }
        socketService.emit("submit-guess", payload)
    }

    fun onEvent(event: String, handler: (JSONObject) -> Unit) {
        socketService.on(event, handler)
    }

    // REST API Methoden
    suspend fun createSession(mode: String, settings: Map<String, Any>): Result<SessionCreateResponse> {
        val request = SessionCreateRequest(mode, settings)
        val response = apiService.createSession(request)
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Fehler beim Erstellen der Session"))
        }
    }

    suspend fun joinSession(sessionId: String): Result<SessionJoinResponse> {
        val request = SessionJoinRequest(sessionId)
        val response = apiService.joinSession(request)
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Fehler beim Beitreten der Session"))
        }
    }

    suspend fun getSessionInfo(sessionId: String): Result<SessionInfoResponse> {
        val response = apiService.getSessionInfo(sessionId)
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Fehler beim Laden der Session-Info"))
        }
    }
}

// Datenklassen für Session-API

data class SessionState(
    val sessionId: String = "",
    val players: List<PlayerInfo> = emptyList(),
    val status: String = "waiting",
    val settings: Map<String, Any> = emptyMap()
)

data class PlayerInfo(
    val userId: String,
    val username: String
)

data class SessionCreateRequest(
    val mode: String,
    val settings: Map<String, Any>
)

data class SessionCreateResponse(
    val sessionId: String,
    val settings: Map<String, Any>
)

data class SessionJoinRequest(
    val sessionId: String
)

data class SessionJoinResponse(
    val sessionId: String,
    val players: List<PlayerInfo>
)

data class SessionInfoResponse(
    val sessionId: String,
    val players: List<PlayerInfo>,
    val status: String,
    val settings: Map<String, Any>
)

