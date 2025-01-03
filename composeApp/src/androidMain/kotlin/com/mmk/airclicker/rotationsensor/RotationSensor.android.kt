package com.mmk.airclicker.rotationsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class AndroidRotationSensor(context: Context) : RotationSensor() {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var rotationSensor: Sensor? = null

    private val rotationMatrix = FloatArray(9) { 0f }
    private val orientation = FloatArray(3) { 0f }


    override fun observeRotationData(): Flow<RotationData> = callbackFlow {
        val listener: SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val yawInRadians = orientation[0].toDouble()
                    val pitchInRadians = orientation[1].toDouble()

                    val yawInDegrees = Math.toDegrees(yawInRadians)
                    val pitchInDegrees = Math.toDegrees(pitchInRadians)

                    val rotationData = processRotationData(yawInDegrees, pitchInDegrees)
                    rotationData?.let { trySend(it) }

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
            rotationSensor = null
            sensorManager.unregisterListener(listener)
        }
    }

}