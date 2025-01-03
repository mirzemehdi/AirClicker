package com.mmk.airclicker

import kotlinx.serialization.Serializable


@Serializable
sealed interface MouseCommand {
    @Serializable
    data class MoveByDelta(val deltaX: Float, val deltaY: Float) : MouseCommand

    @Serializable
    data class MoveByYawPitch(val yaw: Float, val pitch: Float) : MouseCommand

    @Serializable
    data object LeftClick : MouseCommand

    @Serializable
    data object RightClick : MouseCommand
}