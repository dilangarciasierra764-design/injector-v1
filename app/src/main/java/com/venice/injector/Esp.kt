package com.venice.injector

object Esp {

    data class Enemy(val x: Float, val top: Float, val bot: Float, val hp: Int, val dist: Int)

    @Volatile var snapshot: List<Enemy> = emptyList()
    @Volatile var nearest: Enemy? = null
    @Volatile var running = false
    @Volatile var error = ""

    private var w = 0
    private var h = 0

    fun start(width: Int, height: Int) {
        if (running) return
        w = width
        h = height
        running = true
        val t = Thread {
            while (running) {
                try {
                    tick()
                } catch (e: Exception) {
                    error = e.message ?: "err"
                }
                try {
                    Thread.sleep(if (error.isEmpty()) 16L else 120L)
                } catch (e: InterruptedException) {
                    return@Thread
                }
            }
        }
        t.isDaemon = true
        t.start()
    }

    fun stop() {
        running = false
        snapshot = emptyList()
        nearest = null
    }

    private fun tick() {
        if (!Root.ready) {
            error = "sin root"
            return
        }
        if (!Offsets.isReady()) {
            error = "offsets"
            return
        }

        val facade = Mem.u64(Root.base + Offsets.GAME_FACADE)
        if (!Mem.valid(facade)) {
            error = "facade"
            return
        }

        val match = Mem.u64(facade + Offsets.MATCH)
        if (!Mem.valid(match)) {
            error = "sin partida"
            return
        }

        val cam = Mem.u64(match + Offsets.CAMERA)
        if (!Mem.valid(cam)) return

        val matrix = Mem.mat(cam + Offsets.VIEW_MATRIX) ?: return

        val local = Mem.u64(match + Offsets.LOCAL)
        val mePos = if (Mem.valid(local)) Mem.chain(local, Offsets.ENTITY, Offsets.POSITION) else 0L
        val me = if (Mem.valid(mePos)) Mem.vec3(mePos) else null
        val myTeam = if (Mem.valid(local)) Mem.i32(local + Offsets.TEAM) else -1

        val list = Mem.u64(match + Offsets.LIST)
        val count = Mem.i32(match + Offsets.COUNT)
        if (!Mem.valid(list) || count <= 0) {
            error = "sin lista"
            return
        }

        val n = count.coerceAtMost(Offsets.MAX_PLAYERS)
        val out = ArrayList<Enemy>(n)

        for (i in 0 until n) {
            val p = Mem.u64(list + Offsets.ARRAY_HEADER + i * 8L)
            if (!Mem.valid(p)) continue
            if (p == local) continue
            if (Mem.u8(p + Offsets.DEAD) != 0) continue
            if (myTeam != -1 && Mem.i32(p + Offsets.TEAM) == myTeam) continue

            val ent = Mem.u64(p + Offsets.ENTITY)
            if (!Mem.valid(ent)) continue

            val feet = Mem.vec3(ent + Offsets.POSITION) ?: continue
            val head = Mem.vec3(ent + Offsets.HEAD) ?: continue

            val sf = matrix.w2s(feet, w, h) ?: continue
            val shp = matrix.w2s(head, w, h) ?: continue

            val dist = me?.let { it.sub(feet).len().toInt() } ?: 0
            out.add(Enemy(shp[0], shp[1], sf[1], Mem.i32(p + Offsets.HEALTH), dist))
        }

        nearest = out.minByOrNull { it.dist }
        snapshot = out
        error = ""
    }
}
