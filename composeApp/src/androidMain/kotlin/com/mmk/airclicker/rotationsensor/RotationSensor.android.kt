package com.mmk.airclicker.rotationsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class AndroidRotationSensor(context: Context) : RotationSensor {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var rotationSensor: Sensor? = null

    private val rotationMatrix = FloatArray(9) { 0f }
    private val orientation = FloatArray(3) { 0f }

    private var initialYaw: Float? = null

    private val yawBuffer = mutableListOf<Float>()
    private val yawBufferSize = 10


    override fun observeRotationData(): Flow<RotationData> = callbackFlow {
        val listener: SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

                    initYawIfNecessary(yaw)


                    val relativeYaw = (yaw - (initialYaw ?: 0f)) % 360f
                    val normalizedRelativeYaw =
                        if (relativeYaw < 0) relativeYaw + 360 else relativeYaw

                    if (initialYaw != null) {
                        trySend(RotationData(yaw = normalizedRelativeYaw, pitch = pitch))
                    }

                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor == null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotationSensor == null) throw Exception("Your device doesn't support gyroscope sensor")
        }
        rotationSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            onReset()
            sensorManager.unregisterListener(listener)
        }
    }

    private fun initYawIfNecessary(yaw: Float) {
        if (yawBuffer.size < yawBufferSize) {
            yawBuffer.add(yaw)
        } else
            if (initialYaw == null) {
                println("initYawIfNecessary")
                // Calculate the average of the first 10 yaw values and set it as the initialYaw
                initialYaw = yawBuffer.average().toFloat()
            }
    }

    private fun onReset() {
        rotationSensor = null
        initialYaw = null
        yawBuffer.clear()
    }


}