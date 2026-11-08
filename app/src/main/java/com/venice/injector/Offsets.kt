package com.venice.injector

import org.json.JSONObject

object Offsets {

    const val ARRAY_HEADER = 0x20L
    const val MAX_PLAYERS = 60

    // ============================================================
    //  RELLENA AQUÍ. Todo en hexadecimal y con la L al final.
    //  Mientras un valor esté en 0L, el panel dirá "offsets: faltan".
    // ============================================================

    @Volatile var GAME_FACADE = 0L   // estático: facade / GameFacade
    @Volatile var MATCH = 0L         // facade -> match actual
    @Volatile var CAMERA = 0L        // match -> camera
    @Volatile var VIEW_MATRIX = 0L   // camera -> matriz de vista
    @Volatile var LOCAL = 0L         // match -> jugador local
    @Volatile var LIST = 0L          // match -> array de jugadores
    @Volatile var COUNT = 0L         // match -> número de jugadores
    @Volatile var ENTITY = 0L        // jugador -> entidad
    @Volatile var POSITION = 0L      // entidad -> posición (pies)
    @Volatile var HEAD = 0L          // entidad -> posición (cabeza)
    @Volatile var TEAM = 0L          // jugador -> equipo
    @Volatile var HEALTH = 0L        // jugador -> vida
    @Volatile var DEAD = 0L          // jugador -> byte de muerto

    // ============================================================

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

    fun missing(): String {
        val m = ArrayList<String>(13)
        if (GAME_FACADE == 0L) m.add("GAME_FACADE")
        if (MATCH == 0L) m.add("MATCH")
        if (CAMERA == 0L) m.add("CAMERA")
        if (VIEW_MATRIX == 0L) m.add("VIEW_MATRIX")
        if (LOCAL == 0L) m.add("LOCAL")
        if (LIST == 0L) m.add("LIST")
        if (COUNT == 0L) m.add("COUNT")
        if (ENTITY == 0L) m.add("ENTITY")
        if (POSITION == 0L) m.add("POSITION")
        if (HEAD == 0L) m.add("HEAD")
        if (TEAM == 0L) m.add("TEAM")
        if (HEALTH == 0L) m.add("HEALTH")
        if (DEAD == 0L) m.add("DEAD")
        return m.joinToString(", ")
    }

    fun load(): Boolean {
        for (p in candidates) {
            val txt = Root.sh("cat $p 2>/dev/null")
            if (!txt.startsWith("{")) continue
            try {
                apply(txt)
                source = p
                return isReady()
            } catch (e: Exception) {
            }
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
