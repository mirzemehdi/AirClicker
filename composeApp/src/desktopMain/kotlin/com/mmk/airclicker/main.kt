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
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.launch
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.Toolkit
import java.net.ServerSocket
import kotlin.math.roundToInt

fun main() = application {

    // Example: Move the mouse by (10, 20) units
    moveMouseByDelta(10f, 20f)

    // Simulate continuous movement based on some hypothetical data
    repeat(50) {
        moveMouseByDelta(5f, -5f) // Adjust the deltas as needed
        Thread.sleep(100) // Small delay to simulate real-time updates
    }


    var isServerRunning by remember { mutableStateOf(false) }
    var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? by remember {
        mutableStateOf(
            null
        )
    }
    val applicationScope = rememberCoroutineScope()

    var status: String by remember { mutableStateOf("") }
    var port: Int by remember { mutableStateOf(8080) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "AirClicker",
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
        ) {
            Text("AirClicker WebSocket Server")

            BasicTextField(
                value = "$port",
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
}

fun moveMouseByDelta(deltaX: Float, deltaY: Float) {
    val robot = Robot()
    val mousePointer = MouseInfo.getPointerInfo().location

    val sensitivity = 1f
    // Calculate new position
    val newX = mousePointer.x + (deltaX * sensitivity).toInt()
    val newY = mousePointer.y + (deltaY * sensitivity).toInt()

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val constrainedX = newX.coerceIn(0, screenSize.width - 1)
    val constrainedY = newY.coerceIn(0, screenSize.height - 1)
    robot.mouseMove(constrainedX, constrainedY)
}

fun startServer(
    port: Int = 8080,
    onMessage: (String) -> Unit
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? {
    // Check if port is already in use
    if (isPortInUse(port)) {
        onMessage("Port $port is already in use. Cannot start the server.")
        return null
    }

    val server = embeddedServer(Netty, port = port, module = Application::module)
    server.start(wait = false)
    onMessage("Started server")
    return server
}


fun stopServer(
    server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>?,
    onMessage: (String) -> Unit
) {
    server?.apply {
        onMessage("Stopped server")
        stop(1000, 1000)
    }
}


fun Application.module() {
    install(WebSockets) {
//        pingPeriod = 15.seconds
//        timeout = 15.seconds
//        maxFrameSize = Long.MAX_VALUE
//        masking = false
    }
    routing {
        get("/") {
            call.respondText("Hello, from AirClicker!. Server is running currently")
        }

        webSocket("/echo") {
            send("Please enter your name")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                if (receivedText.equals("bye", ignoreCase = true)) {
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                } else {
                    send(Frame.Text("Hi, $receivedText!"))
                }
            }
        }
    }


}

fun isPortInUse(port: Int): Boolean {
    try {
        ServerSocket(port).use { socket ->
            // If the socket is bound successfully, the port is available
            return false
        }
    } catch (e: Exception) {
        // If an exception is thrown, the port is already in use
        return true
    }
}