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

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                showContent = !showContent

                coroutineScope.launch {
                    sendMessageToSocket()
                }

            }) {
                Text("Click me!")

            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

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