package com.venice.injector

import java.io.RandomAccessFile

object Root {

    @Volatile var pid = -1
    @Volatile var base = 0L
    @Volatile var ready = false
    @Volatile private var raf: RandomAccessFile? = null

    fun sh(cmd: String): String = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.trim()
    } catch (e: Exception) { "" }

    fun hasRoot(): Boolean = sh("id").contains("uid=0")

    fun attach(pkg: String = "com.dts.freefireth"): Boolean {
        detach()
        val p = sh("pidof $pkg").split(" ").firstOrNull()?.toIntOrNull() ?: return false
        pid = p
        sh("chmod 666 /proc/$p/mem")
        raf = try { RandomAccessFile("/proc/$p/mem", "r") } catch (e: Exception) { return false }
        base = findModule(p, "libil2cpp.so")
        ready = pid > 0 && base != 0L && raf != null
        return ready
    }

    private fun findModule(p: Int, name: String): Long =
        sh("cat /proc/$p/maps")
            .lineSequence()
            .filter { it.contains(name) && it.contains("r-xp") }
            .mapNotNull { it.substringBefore("-").toLongOrNull(16) }
            .minOrNull() ?: 0L

    fun detach() {
        try { raf?.close() } catch (_: Exception) {}
        raf = null
        ready = false
        pid = -1
        base = 0L
    }

    fun channel() = raf?.channel
}
