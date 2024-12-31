package com.mmk.airclicker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class IosGyroscopeSensor() : GyroscopeSensor {
    override fun observeGyroscopeData(): Flow<GyroscopeData> {
        return flowOf(GyroscopeData(0f, 0f, 0f))
    }
}