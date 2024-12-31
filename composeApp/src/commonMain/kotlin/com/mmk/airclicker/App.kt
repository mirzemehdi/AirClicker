package com.mmk.airclicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import airclicker.composeapp.generated.resources.Res
import airclicker.composeapp.generated.resources.compose_multiplatform
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.ktor.client.HttpClient

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*


@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        val gyroscopeSensor = rememberGyroscopeSensor()


        val screenWidth = 411f
        val screenHeight = 842f
        var ballPosition by remember { mutableStateOf(Offset(0f, 0f)) }


        // Collect accelerometer data
        LaunchedEffect(Unit) {
            gyroscopeSensor.observeGyroscopeData().collect { data ->
                // Update the ball's position based on accelerometer data

                ballPosition = Offset(
                    x = (ballPosition.x + data.x * 2f).coerceIn(-500f, 500f), // Scale and constrain the movement
                    y = (ballPosition.y + data.y * 2f).coerceIn(-500f, 500f)  // Scale and constrain the movement
                )

                // Ensure the ball stays within the screen bounds
//                ballPosition = ballPosition.copy(
//                    x = ballPosition.x.coerceIn(-screenWidth / 2, screenWidth / 2),
//                    y = ballPosition.y.coerceIn(-screenHeight / 2, screenHeight / 2)
//                )
            }
        }

        // Draw the circle based on accelerometer data
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Circle representing accelerometer position
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.Center)
                    .offset(
                        x = ballPosition.x.dp,
                        y = ballPosition.y.dp,
                    )
                    .background(Red, shape = androidx.compose.foundation.shape.CircleShape)
            )

        }
    }
}


//@Composable
//@Preview
//fun App() {
//    MaterialTheme {
//        var showContent by remember { mutableStateOf(false) }
//        val coroutineScope = rememberCoroutineScope()
//
//
//
//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = {
//                showContent = !showContent
//
//                coroutineScope.launch {
//                    sendMessageToSocket()
//                }
//
//            }) {
//                Text("Click me!")
//
//            }
//            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
//                Column(
//                    Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }
//    }
//}

private suspend fun sendMessageToSocket() {

    val client = HttpClient {
        install(WebSockets) {
//            pingIntervalMillis = 20_000
        }
    }
    client.webSocket(method = HttpMethod.Get, host = "", port = 8080, path = "/echo") {
        while (true) {
            val othersMessage = incoming.receive() as? Frame.Text
            println("WebSocketMessage:${othersMessage?.readText()}")
//            send("Mirzamehdi")

        }
    }

    client.close()
}