package com.mmk.airclicker.rotationsensor

import kotlinx.coroutines.flow.Flow

abstract class RotationSensor {

    private var initialYaw: Double? = null
    private val yawBuffer = mutableListOf<Double>()
    private val yawBufferSize = 10

    abstract fun observeRotationData(): Flow<RotationData>


    protected fun processRotationData(yawInDegrees: Double, pitchInDegrees: Double): RotationData? {
        initYawIfNecessary(yawInDegrees)
        if (initialYaw == null) return null

        val relativeYaw = (yawInDegrees - (initialYaw ?: 0.0)) % 360f
        val normalizedRelativeYaw = if (relativeYaw < 0) relativeYaw + 360 else relativeYaw

        return RotationData(
            yaw = normalizedRelativeYaw.toFloat(),
            pitch = pitchInDegrees.toFloat()
        )


    }

    private fun initYawIfNecessary(yaw: Double) {
        if (yawBuffer.size < yawBufferSize) {
            yawBuffer.add(yaw)
        } else
            if (initialYaw == null) {
                println("initYawIfNecessary")
                // Calculate the average of the first 10 yaw values and set it as the initialYaw
                initialYaw = yawBuffer.average()
            }
    }

    protected fun onReset() {
        initialYaw = null
        yawBuffer.clear()
    }
}