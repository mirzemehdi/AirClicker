package com.mmk.airclicker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform