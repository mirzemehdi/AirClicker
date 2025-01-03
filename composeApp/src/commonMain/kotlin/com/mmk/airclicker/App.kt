package com.mmk.airclicker

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {
        val webSocketManager = remember { WebSocketManager() }
        val rotationSensor = rememberRotationSensor()
        var isStarted by rememberSaveable { mutableStateOf(false) }
        var isSensorEnabled by rememberSaveable { mutableStateOf(false) }
        var sensorSensitivity by rememberSaveable { mutableStateOf(5f) }
        var infoText by remember { mutableStateOf("") }


        DisposableEffect(Unit) {
            onDispose {
                webSocketManager.stopConnection()
                isStarted = false
            }
        }
        LaunchedEffect(isSensorEnabled, isStarted) {
            if (isSensorEnabled && isStarted) {
                rotationSensor
                    .observeRotationData()
                    .buffer(onBufferOverflow = BufferOverflow.DROP_OLDEST)
                    .collect { rotationData ->
                        val command = MouseCommand.MoveByYawPitch(
                            yaw = rotationData.mapYawToCursorPosition(sensorSensitivity),
                            pitch = rotationData.mapPitchToCursorPosition(sensorSensitivity)
                        )
                        webSocketManager.sendMouseCommand(
                            mouseCommand = command,
                            onError = {
                                infoText = "Error: $it"
                            }
                        )
                    }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoBox(text = infoText)

            if (isStarted) {
                StopConnectionView {
                    webSocketManager.stopConnection()
                    isStarted = false
                }
                SensorControlView(isSensorEnabled = isSensorEnabled,
                    onSensorEnabledChange = { isSensorEnabled = it },
                    sensitivity = sensorSensitivity,
                    onSensitivityChange = {
                        sensorSensitivity = it
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            webSocketManager.sendMouseCommand(
                                mouseCommand = MouseCommand.LeftClick,
                                onError = {
                                    infoText = "Error: $it"
                                })
                        }) {
                        Text("Left Click")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            webSocketManager.sendMouseCommand(
                                mouseCommand = MouseCommand.RightClick,
                                onError = {
                                    infoText = "Error: $it"
                                })
                        }) {
                        Text("Right Click")
                    }
                }
                ManualMouseControlView(
                    onDirectionUpdate = { xDelta, yDelta ->
                        webSocketManager.sendMouseCommand(
                            mouseCommand = MouseCommand.MoveByDelta(xDelta, yDelta),
                            onError = {
                                infoText = "Error: $it"
                            }
                        )
                    })
            } else {
                StartConnectionView(modifier = Modifier.fillMaxWidth()) { host, port ->
                    webSocketManager.startConnection(
                        host = host,
                        port = port.toIntOrNull() ?: 8080,
                        onMessageReceived = { message ->
                            isStarted = true
                            infoText = "Message received: $message"
                        },
                        onError = { error ->
                            infoText = "Error: $error"
                        }

                    )
                }
            }

        }


    }
}


@Composable
fun ManualMouseControlView(
    onDirectionUpdate: (xDelta: Float, yDelta: Float) -> Unit
) {
    var sensitivity by remember { mutableStateOf(5f) }

    Card(
        elevation = 4.dp, modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Manual Control", color = Color.Black, style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))

            // "Up" Button
            HoldToSendButton(
                text = "Up",
                xDelta = 0f,
                yDelta = sensitivity * (-1f) * 10,
                onDirection = onDirectionUpdate
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
            ) {
                // "Left" Button
                HoldToSendButton(
                    text = "Left",
                    xDelta = (-1f) * sensitivity * 10,
                    yDelta = 0f,
                    onDirection = onDirectionUpdate
                )

                Spacer(modifier = Modifier.width(16.dp))

                // "Right" Button
                HoldToSendButton(
                    text = "Right",
                    xDelta = (1f) * sensitivity * 10,
                    yDelta = 0f,
                    onDirection = onDirectionUpdate
                )

            }

            // "Down" Button
            HoldToSendButton(
                text = "Down",
                xDelta = 0f,
                yDelta = (1f) * sensitivity * 10,
                onDirection = onDirectionUpdate
            )

            SensitivitySlider(sensitivity = sensitivity, onSensitivityChange = {
                sensitivity = it
            })


        }
    }


}


@Composable
fun HoldToSendButton(
    modifier: Modifier = Modifier,
    text: String,
    xDelta: Float,
    yDelta: Float,
    onDirection: (xDelta: Float, yDelta: Float) -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Launch a coroutine to send data repeatedly when the button is pressed
    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (isPressed) {
                onDirection(xDelta, yDelta)  // Keep sending the same data
                delay(50) // Adjust the sending frequency (ms)
            }
        }
    }

    Button(
        elevation = ButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        modifier = Modifier.size(80.dp).clip(CircleShape),
        shape = CircleShape,
        onClick = {
            onDirection(xDelta, yDelta)
        },
        interactionSource = interactionSource
    ) {
        Text(text)
    }
}

@Composable
fun SensitivitySlider(sensitivity: Float, onSensitivityChange: (Float) -> Unit) {

    Column(modifier = Modifier.padding(16.dp)) {
        // Text displaying the current sensitivity
        Text(text = "Sensitivity: $sensitivity")

        // Slider for adjusting the sensitivity
        Slider(
            value = sensitivity,
            onValueChange = {
                // Send the updated sensitivity value to the desktop server
                onSensitivityChange(it)
            },
            valueRange = 1f..10f, // Minimum and maximum sensitivity values
            steps = 9, // Number of steps between min and max
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
fun StartConnectionView(modifier: Modifier = Modifier, onClickStart: (String, String) -> Unit) {
    var hostText by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("8080") }
    Card(modifier = modifier, elevation = 4.dp, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = hostText,
                label = { Text("Host Address") },
                placeholder = { Text("Host Address (ex: 192.168.1.1)") },
                onValueChange = { hostText = it },
            )

            TextField(
                value = portText,
                label = { Text("Port") },
                placeholder = { Text("Port (Default: 8080)") },
                onValueChange = { portText = it },
            )

            Button(
                enabled = hostText.isNotEmpty() && portText.isNotEmpty(),
                onClick = { onClickStart(hostText, portText) }) {
                Text("Start Connection")
            }

        }

    }
}

@Composable
fun StopConnectionView(modifier: Modifier = Modifier, onClickStop: () -> Unit) {
    Button(
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
        onClick = onClickStop,
        modifier = modifier
    ) {
        Text("Stop Connection")
    }
}

@Composable
fun InfoBox(modifier: Modifier = Modifier, text: String) {
    if (text.isEmpty()) return
    Card(modifier = modifier, backgroundColor = MaterialTheme.colors.secondary) {
        Text(text = text, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun SensorControlView(
    modifier: Modifier = Modifier,
    isSensorEnabled: Boolean,
    sensitivity: Float,
    onSensorEnabledChange: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit
) {

    Card(
        elevation = 4.dp, modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Sensor Control", color = Color.Black, style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
                Text("Sensor Enabled: ", color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = isSensorEnabled, onCheckedChange = {
                    onSensorEnabledChange(it)
                })
            }

            SensitivitySlider(sensitivity = sensitivity, onSensitivityChange = {
                onSensitivityChange(it)
            })


        }
    }


}
