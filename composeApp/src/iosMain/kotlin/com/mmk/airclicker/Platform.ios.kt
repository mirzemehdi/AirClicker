package com.mmk.airclicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mmk.airclicker.rotationsensor.IosRotationSensor
import com.mmk.airclicker.rotationsensor.RotationSensor


@Composable
actual fun rememberRotationSensor(): RotationSensor {
    return remember { IosRotationSensor() }
}