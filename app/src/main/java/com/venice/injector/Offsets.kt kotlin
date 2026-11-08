package com.venice.injector

object Offsets {

    // Cabecera de los arrays administrados de IL2CPP.
    // 0x20 en arm64, 0x10 en arm32. Free Fire actual es arm64.
    const val ARRAY_HEADER = 0x20L
    const val MAX_PLAYERS = 60

    // --- Punteros de escena ---
    const val GAME_FACADE = 0x0L   // campo estático "GameFacade" en GameFacade
    const val MATCH       = 0x0L   // GameFacade -> partida actual
    const val CAMERA      = 0x0L   // partida -> cámara
    const val VIEW_MATRIX = 0x0L   // cámara -> matriz de vista
    const val LOCAL       = 0x0L   // partida -> jugador local

    // --- Lista de jugadores ---
    const val LIST  = 0x0L         // partida -> array de jugadores
    const val COUNT = 0x0L         // partida -> cantidad

    // --- Entidad ---
    const val ENTITY   = 0x0L      // jugador -> entidad raíz
    const val POSITION = 0x0L      // entidad -> Vector3 pies
    const val HEAD     = 0x0L      // entidad -> Vector3 cabeza
    const val TEAM     = 0x0L      // jugador -> equipo
    const val HEALTH   = 0x0L      // jugador -> vida
    const val DEAD     = 0x0L      // jugador -> bool muerto
}
