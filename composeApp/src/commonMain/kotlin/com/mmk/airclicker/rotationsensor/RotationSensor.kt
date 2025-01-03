package com.mmk.airclicker.rotationsensor

import kotlinx.coroutines.flow.Flow

interface RotationSensor {
    fun observeRotationData(): Flow<RotationData>
}