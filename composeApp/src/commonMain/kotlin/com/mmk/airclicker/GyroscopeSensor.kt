package com.mmk.airclicker

import kotlinx.coroutines.flow.Flow

interface GyroscopeSensor {
    fun observeGyroscopeData(): Flow<GyroscopeData>
}