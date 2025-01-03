package com.mmk.airclicker

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class WebSocketManager {

    companion object {
        private const val PATH = "/mouse-control"
    }

    private var host: String? = null
    private var port: Int = 8080

    private val client by lazy {
        HttpClient {
            install(WebSockets)
        }
    }

    private var scope = CoroutineScope(Dispatchers.IO)

    fun startConnection(
        host: String,
        port: Int = 8080,
        onMessageReceived: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        this.host = host
        this.port = port
        scope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = host, port = port, path = PATH) {
                    for (message in incoming) {
                        val text = (message as? Frame.Text)?.readText() ?: continue
                        onMessageReceived(text)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onError("WebSocket error: ${e.message}")
            }
        }
    }

    fun sendMouseCommand(mouseCommand: MouseCommand, onError: (String) -> Unit = {}) {
        val json = Json { encodeDefaults = true }
        val message = json.encodeToString(MouseCommand.serializer(), mouseCommand)
        println("Sending Mouse Command: $message")
        sendMessage(message, onError)
    }


    private fun sendMessage(message: String, onError: (String) -> Unit = {}) {
        scope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = host, port = port, path = PATH) {
                    send(Frame.Text(message))
                }
            } catch (e: Exception) {
                onError("Error sending message: ${e.message}")
            }
        }
    }

    fun stopConnection() {
        scope.coroutineContext.cancelChildren()
    }
}
