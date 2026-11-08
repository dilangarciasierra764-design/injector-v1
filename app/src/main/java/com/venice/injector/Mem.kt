package com.venice.injector

import java.nio.ByteBuffer
import java.nio.ByteOrder

object Mem {

    private val lock = Any()
    private val LE = ByteOrder.LITTLE_ENDIAN

    fun read(addr: Long, size: Int): ByteBuffer? {
        val ch = Root.channel() ?: return null
        if (addr <= 0) return null
        val bb = ByteBuffer.allocate(size).order(LE)
        return try {
            synchronized(lock) {
                ch.position(addr)
                var n = 0
                while (n < size) {
                    val r = ch.read(bb)
                    if (r <= 0) break
                    n += r
                }
                if (n != size) null else { bb.rewind(); bb }
            }
        } catch (e: Exception) { null }
    }

    fun u8(a: Long) = read(a, 1)?.get()?.toInt()?.and(0xFF) ?: 0
    fun i32(a: Long) = read(a, 4)?.int ?: 0
    fun u64(a: Long) = read(a, 8)?.long ?: 0L
    fun f32(a: Long) = read(a, 4)?.float ?: 0f

    fun vec3(a: Long): Vector3? {
        val b = read(a, 12) ?: return null
        return Vector3(b.float, b.float, b.float)
    }

    fun mat(a: Long): Matrix4x4? {
        val b = read(a, 64) ?: return null
        return Matrix4x4(FloatArray(16) { b.float })
    }

    fun chain(start: Long, vararg offs: Long): Long {
        var a = start
        for (o in offs) {
            if (a <= 0) return 0
            a = u64(a + o)
        }
        return a
    }

    fun valid(a: Long) = a > 0x1000 && a < 0x7FFFFFFFFFFFL && (a and 3L) == 0L
}
