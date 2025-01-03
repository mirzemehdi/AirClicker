package com.mmk.airclicker.rotationsensor

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.PI

internal class IosRotationSensor : RotationSensor() {

    private val motionManager = CMMotionManager()

    override fun observeRotationData(): Flow<RotationData> = callbackFlow {
        if (!motionManager.isDeviceMotionAvailable()) {
            throw Exception("Device motion is not available")
        }

        motionManager.deviceMotionUpdateInterval = 0.1
        motionManager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue()) { motion, error ->
            if (error != null) {
                throw Exception("Error starting device motion updates: ${error.localizedDescription}")
            }

            motion?.let {
                val yawInDegrees = (-motion.attitude.yaw) * (180.0 / PI)
                val pitchInDegrees =(-motion.attitude.pitch) * (180.0 / PI)

                val rotationData = processRotationData(yawInDegrees, pitchInDegrees)
                rotationData?.let { data -> trySend(data) }

            }
        }
        awaitClose {
            motionManager.stopDeviceMotionUpdates()
        }
    }


}