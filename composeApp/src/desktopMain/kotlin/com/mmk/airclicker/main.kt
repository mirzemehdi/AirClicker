package com.mmk.airclicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.ServerSocket

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AirClicker",
    ) {
        DesktopApp(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun DesktopApp(modifier: Modifier = Modifier) {
    var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? by remember {
        mutableStateOf(null)
    }
    var isServerRunning by remember { mutableStateOf(false) }
    val applicationScope = rememberCoroutineScope()
    var status: String by remember { mutableStateOf("") }
    var port: Int by remember { mutableStateOf(8080) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = modifier
    ) {
        Text("AirClicker WebSocket Server")
        Text("Host: ${InetAddress.getLocalHost().hostAddress}")

        BasicTextField(
            value = "PORT: $port",
            onValueChange = {
                port = it.toIntOrNull() ?: 8080
            }

        )

        Button(onClick = {
            applicationScope.launch {
                if (isServerRunning) {
                    stopServer(server, onMessage = { status = it })
                    isServerRunning = false
                } else {
                    startServer(port = port, onMessage = { status = it }).also {
                        it?.let {
                            isServerRunning = true
                            server = it
                        }
                    }
                }
            }
        }) {
            Text(if (isServerRunning) "Stop Server" else "Start Server")
        }


        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Text("Status: ")
            Text(status)
        }

    }
}


private fun Application.module() {
    val mouseCommandExecutor = MouseCommandExecutor()
    install(WebSockets)
    routing {
        get("/") {
            call.respondText("Hello, from AirClicker!. Server is running currently")
        }
        webSocket("/mouse-control") {
            send("Hello, from AirClicker!. Control mouse either with manual coordinates or sensor data")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                try {
                    // Parse the received message as "x,y"
                    println("Received message: $receivedText")
                    val json = Json { ignoreUnknownKeys = true } // Handle unknown fields gracefully
                    val command = json.decodeFromString(MouseCommand.serializer(), receivedText)
                    mouseCommandExecutor.invoke(command)

                } catch (e: Exception) {
                    send("Error parsing input, Ensure your JSON is correctly formatted")
                }
            }
        }
    }

}

private fun startServer(
    port: Int = 8080,
    onMessage: (String) -> Unit
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? {

    // Check if port is already in use
    val isPortInUse = runCatching { ServerSocket(port).use { false } }.getOrDefault(true)

    if (isPortInUse) {
        onMessage("Port $port is already in use. Cannot start the server.")
        return null
    }

    val server = embeddedServer(Netty, port = port, module = Application::module)
    server.start(wait = false)
    onMessage("Started server")
    return server
}


private fun stopServer(
    server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>?,
    onMessage: (String) -> Unit
) {
    server?.apply {
        onMessage("Stopped server")
        stop(1000, 1000)
    }
}