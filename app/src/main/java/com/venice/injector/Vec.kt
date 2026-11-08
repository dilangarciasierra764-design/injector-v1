package com.venice.injector

data class Vector3(val x: Float, val y: Float, val z: Float) {
    fun sub(o: Vector3) = Vector3(x - o.x, y - o.y, z - o.z)
    fun len() = kotlin.math.sqrt(x * x + y * y + z * z)
}

class Matrix4x4(val m: FloatArray) {

    fun w2s(v: Vector3, w: Int, h: Int): FloatArray? {
        val cx = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]
        val cy = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]
        val cz = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]
        if (cz < 0.1f) return null
        val sx = (w / 2f) * (1f + cx / cz)
        val sy = (h / 2f) * (1f - cy / cz)
        if (sx < 0 || sy < 0 || sx > w || sy > h) return null
        return floatArrayOf(sx, sy)
    }
}
