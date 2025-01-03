package com.mmk.airclicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mmk.airclicker.rotationsensor.AndroidRotationSensor
import com.mmk.airclicker.rotationsensor.RotationSensor


@Composable
actual fun rememberRotationSensor(): RotationSensor {
    val context = LocalContext.current
    return remember { AndroidRotationSensor(context.applicationContext) }
}