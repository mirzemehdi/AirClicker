package com.mmk.airclicker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal class AndroidGyroscopeSensor(context: Context) : GyroscopeSensor {
    companion object {
        private const val EPSILON = 1e-10f
    }

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var sensor: Sensor? = null

    // Create a constant to convert nanoseconds to seconds.
    private val NS2S = 1.0f / 1000000000.0f
    private var rotationCurrent = FloatArray(9) { 0f }
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f

    private val gravity = FloatArray(3) { 0f }
    private val linearAcceleration = FloatArray(3) { 0f }
    private val alpha: Float = 0.8f  // Low-pass filter constant

    override fun observeGyroscopeData(): Flow<GyroscopeData> = callbackFlow {
        val listener: SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Apply low-pass filter to isolate gravity
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                    // Apply high-pass filter to get linear acceleration (movement)
                    linearAcceleration[0] = event.values[0] - gravity[0]
                    linearAcceleration[1] = event.values[1] - gravity[1]
                    linearAcceleration[2] = event.values[2] - gravity[2]

                    // Send the linear acceleration data to the flow
                    trySend(GyroscopeData(linearAcceleration[0], linearAcceleration[1]))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor == null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (sensor == null) throw Exception("Your device doesn't support gyroscope sensor")
        }
        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun processGyroscopeData(x: Float, y: Float): GyroscopeData {
        // Smooth the data (simple example: apply a moving average or low-pass filter)

        val screenWidth = 411f
        val screenHeight = 842f

        val scaleX = screenWidth / 2f // Max value for X axis (centered at 0)
        val scaleY = screenHeight / 2f // Max value for Y axis (centered at 0)

        // Apply a multiplier for sensitivity. You can adjust this value.
        val sensitivity = 1f

        val smoothedX = x * sensitivity
        val smoothedY = y * sensitivity

        // Normalize the values to stay within the screen boundaries
        val normalizedX = smoothedX.coerceIn(-scaleX, scaleX)
        val normalizedY = smoothedY.coerceIn(-scaleY, scaleY)

        println("Gyroscope data: $normalizedX, $normalizedY")
        return GyroscopeData(normalizedX, normalizedY)
    }

    private fun getSensorData(event: SensorEvent?): GyroscopeData? {
        if (event?.sensor?.type != Sensor.TYPE_GYROSCOPE) return null
        if (timestamp == 0f) {
            timestamp = event.timestamp.toFloat()
            return null
        }
        val dT = (event.timestamp - timestamp) * NS2S
        // Axis of the rotation sample, not normalized yet.
        var axisX: Float = event.values[0]
        var axisY: Float = event.values[1]
        var axisZ: Float = event.values[2]

        // Calculate the angular speed of the sample
        val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

        // Normalize the rotation vector if it's big enough to get the axis
        // (that is, EPSILON should represent your maximum allowable margin of error)
        if (omegaMagnitude > EPSILON) {
            axisX /= omegaMagnitude
            axisY /= omegaMagnitude
            axisZ /= omegaMagnitude
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        val thetaOverTwo: Float = omegaMagnitude * dT / 2.0f
        val sinThetaOverTwo: Float = sin(thetaOverTwo)
        val cosThetaOverTwo: Float = cos(thetaOverTwo)
        deltaRotationVector[0] = sinThetaOverTwo * axisX
        deltaRotationVector[1] = sinThetaOverTwo * axisY
        deltaRotationVector[2] = sinThetaOverTwo * axisZ
        deltaRotationVector[3] = cosThetaOverTwo

        timestamp = event.timestamp.toFloat()
        val deltaRotationMatrix = FloatArray(9) { 0f }
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector)

        for (i in rotationCurrent.indices) {
            rotationCurrent[i] = deltaRotationMatrix[i]
        }

        println("Gyroscope data: ${rotationCurrent[0]}, ${rotationCurrent[1]}")
        return GyroscopeData(rotationCurrent[0], rotationCurrent[1], rotationCurrent[2])
    }
}