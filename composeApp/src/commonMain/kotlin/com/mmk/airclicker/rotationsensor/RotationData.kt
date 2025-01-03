package com.mmk.airclicker.rotationsensor

data class RotationData(
    val yaw: Float,  //  (left-right) (0f - 360f)
    val pitch: Float // (up-down) (-180f - 180f)
) {

    // Normalize to range [-1, 1] (-1 = top of screen, 1 = bottom of screen)
    fun mapPitchToCursorPosition(sensitivity: Float = 5f): Float {
        val maxDegree = getMaxDegreeFromSensitivity(sensitivity)
        return pitch.coerceIn(-maxDegree, maxDegree) / maxDegree
    }


    // Normalize to range [-1, 1] (-1 = left part of screen, 1 = right part of screen)
    fun mapYawToCursorPosition(sensitivity: Float = 5f): Float {
        val maxDegree = getMaxDegreeFromSensitivity(sensitivity)
        val boundedYaw = yaw.coerceIn(0f, 360f)
        return when (yaw.coerceIn(0f, 360f)) {
            in 0f..180f -> {
                (boundedYaw / maxDegree).coerceIn(0f, 1f)
            }

            in 180f..360f -> {
                ((boundedYaw - 360) / maxDegree).coerceIn(-1f, 0f)
            }

            else -> 0f  // Fallback to 0 if outside the range
        }
    }

    /**
     * When sensitivity increase, degree should be lower
     * for example:
     * 3f -> 30
     * 2f -> 45
     * 1f -> 60
     */
    private fun getMaxDegreeFromSensitivity(sensitivity: Float): Float {
        val boundedSensitivity = sensitivity.coerceIn(1f, 10f)
        val minSensitivity = 1f
        val maxDegrees = 60f
        val minDegrees = 10f
        val rangeDegrees = maxDegrees - minDegrees
        val rangeSensitivity = 10f - minSensitivity

        val degree =
            maxDegrees - (boundedSensitivity - minSensitivity) * (rangeDegrees / rangeSensitivity)

        return degree
    }
}