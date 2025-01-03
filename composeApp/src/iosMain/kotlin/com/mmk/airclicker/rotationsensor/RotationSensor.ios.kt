package com.mmk.airclicker.rotationsensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class IosRotationSensor : RotationSensor {
    override fun observeRotationData(): Flow<RotationData> {
        return flowOf(RotationData(0f, 0f))
    }
}