package com.venice.injector

import org.json.JSONObject

object Offsets {

    const val ARRAY_HEADER = 0x20L
    const val MAX_PLAYERS = 60

    @Volatile var GAME_FACADE = 0L
    @Volatile var MATCH = 0L
    @Volatile var CAMERA = 0L
    @Volatile var VIEW_MATRIX = 0L
    @Volatile var LOCAL = 0L
    @Volatile var LIST = 0L
    @Volatile var COUNT = 0L
    @Volatile var ENTITY = 0L
    @Volatile var POSITION = 0L
    @Volatile var HEAD = 0L
    @Volatile var TEAM = 0L
    @Volatile var HEALTH = 0L
    @Volatile var DEAD = 0L

    @Volatile var source = ""

    private val candidates = listOf(
        "/sdcard/vni/offsets.json",
        "/storage/emulated/0/vni/offsets.json",
        "/sdcard/Download/offsets.json",
        "/data/local/tmp/offsets.json"
    )

    fun isReady() = GAME_FACADE != 0L && MATCH != 0L && CAMERA != 0L &&
        VIEW_MATRIX != 0L && LOCAL != 0L && LIST != 0L && COUNT != 0L &&
        ENTITY != 0L && POSITION != 0L && HEAD != 0L && TEAM != 0L &&
        HEALTH != 0L && DEAD != 0L

    fun load(): Boolean {
        for (p in candidates) {
            val txt = Root.sh("cat $p 2>/dev/null")
            if (!txt.startsWith("{")) continue
            try {
                apply(txt)
                source = p
                return isReady()
            } catch (_: Exception) {}
        }
        return false
    }

    private fun apply(t: String) {
        val o = JSONObject(t)
        GAME_FACADE = n(o, "GAME_FACADE", GAME_FACADE)
        MATCH       = n(o, "MATCH", MATCH)
        CAMERA      = n(o, "CAMERA", CAMERA)
        VIEW_MATRIX = n(o, "VIEW_MATRIX", VIEW_MATRIX)
        LOCAL       = n(o, "LOCAL", LOCAL)
        LIST        = n(o, "LIST", LIST)
        COUNT       = n(o, "COUNT", COUNT)
        ENTITY      = n(o, "ENTITY", ENTITY)
        POSITION    = n(o, "POSITION", POSITION)
        HEAD        = n(o, "HEAD", HEAD)
        TEAM        = n(o, "TEAM", TEAM)
        HEALTH      = n(o, "HEALTH", HEALTH)
        DEAD        = n(o, "DEAD", DEAD)
    }

    private fun n(o: JSONObject, k: String, d: Long): Long {
        if (!o.has(k)) return d
        return when (val v = o.get(k)) {
            is Number -> v.toLong()
            is String -> v.trim().removePrefix("0x").removePrefix("0X").toLongOrNull(16) ?: d
            else -> d
        }
    }
}
