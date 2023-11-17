package com.inovace.remconnect

object MySingleton {
    private var proximity = false

    fun getSomeData(): Boolean {
        return proximity
    }

    fun setSomeData(data: Boolean) {
        proximity = data
    }
}
