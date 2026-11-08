package com.venice.injector

import java.io.RandomAccessFile

object Root {

    private val PKGS = listOf("com.dts.freefireth", "com.dts.freefiremax")

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

    fun attach(): Boolean {
        detach()
        for (pkg in PKGS) {
            val p = sh("pidof $pkg").split(" ").firstOrNull()?.toIntOrNull() ?: continue
            if (!sh("cat /proc/$p/cmdline 2>/dev/null").contains(pkg)) continue
            sh("chmod 666 /proc/$p/mem 2>/dev/null")
            raf = try { RandomAccessFile("/proc/$p/mem", "r") } catch (e: Exception) { null } ?: continue
            base = findModule(p, "libil2cpp.so")
            if (base == 0L) { detach(); continue }
            pid = p
            ready = true
            return true
        }
        return false
    }

    private fun findModule(p: Int, name: String): Long {
        var best = 0L
        for (line in sh("cat /proc/$p/maps").lineSequence()) {
            if (!line.contains(name)) continue
            val parts = line.split(" ", limit = 6)
            if (parts.size < 6) continue
            val start = parts[0].substringBefore("-").toLongOrNull(16) ?: continue
            val off = parts[2].toLongOrNull(16) ?: continue
            val cand = start - off
            if (best == 0L || cand < best) best = cand
        }
        return best
    }

    fun detach() {
        try { raf?.close() } catch (_: Exception) {}
        raf = null
        ready = false
        pid = -1
        base = 0L
    }

    fun channel() = raf?.channel
}
