package com.example.geogeusserclone.data.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import javax.inject.Singleton

@Singleton
class SocketService {
    private var socket: Socket? = null

    fun connect(jwt: String, onConnected: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        val opts = IO.Options()
        opts.query = "auth=$jwt"
        try {
            socket = IO.socket("http://10.0.2.2:3000", opts)
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketService", "Verbunden mit Socket.IO")
                onConnected?.invoke()
            }
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.getOrNull(0)?.toString() ?: "Unbekannter Fehler"
                Log.e("SocketService", "Socket.IO Fehler: $error")
                onError?.invoke(error)
            }
            socket?.on("error") { args ->
                val error = args.getOrNull(0)?.toString() ?: "Unbekannter Fehler"
                Log.e("SocketService", "Socket.IO Fehler: $error")
                onError?.invoke(error)
            }
            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketService", "Verbindungsfehler: ${e.message}")
            onError?.invoke(e.message ?: "Verbindungsfehler")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun emit(event: String, payload: JSONObject) {
        socket?.emit(event, payload)
    }

    fun on(event: String, handler: (JSONObject) -> Unit) {
        socket?.on(event) { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    // Event-Handler für alle relevanten Socket.IO-Events
    fun onConnected(handler: () -> Unit) {
        socket?.on("connected") { handler() }
    }

    fun onPlayerJoined(handler: (JSONObject) -> Unit) {
        socket?.on("player-joined") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onPlayerLeft(handler: (JSONObject) -> Unit) {
        socket?.on("player-left") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onSessionStarted(handler: (JSONObject) -> Unit) {
        socket?.on("session-started") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onRoundStarted(handler: (JSONObject) -> Unit) {
        socket?.on("round-started") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onGuessSubmitted(handler: (JSONObject) -> Unit) {
        socket?.on("guess-submitted") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onGuessConfirmed(handler: (JSONObject) -> Unit) {
        socket?.on("guess-confirmed") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onRoundEnded(handler: (JSONObject) -> Unit) {
        socket?.on("round-ended") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onPlayerEliminated(handler: (JSONObject) -> Unit) {
        socket?.on("player-eliminated") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onSessionEnded(handler: (JSONObject) -> Unit) {
        socket?.on("session-ended") { args ->
            val data = args.getOrNull(0) as? JSONObject
            if (data != null) handler(data)
        }
    }

    fun onDisconnect(handler: () -> Unit) {
        socket?.on(Socket.EVENT_DISCONNECT) { handler() }
    }

    fun onError(handler: (String) -> Unit) {
        socket?.on("error") { args ->
            val error = args.getOrNull(0)?.toString() ?: "Unbekannter Fehler"
            handler(error)
        }
    }
}
