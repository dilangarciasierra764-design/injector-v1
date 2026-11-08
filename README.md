# vni

Overlay de ESP + aim para Free Fire. Requiere root.

## Requisitos

- Móvil arm64 con Android 8 o superior
- Root: KernelSU + SUSFS (mejor), APatch, o Magisk + Shamiko
- PC o Termux para el dump

## Pasos

1. Push a `main`. Actions compila.
2. Descargar el artifact `vni-debug`.
3. Instalar el APK.
4. Conceder overlay, accesibilidad y root (botones 1, 2, 3).
5. Dumpear offsets y rellenar `Offsets.kt`.
6. Push, bajar artifact nuevo, reinstalar, pulsar "4 · iniciar".

## Dump

Extraer `libil2cpp.so` y `global-metadata.dat` del APK, pasar por Il2CppDumper, leer `dump.cs`.

## Diagnóstico

`Esp.error` indica el corte: `sin root`, `facade`, `sin partida`, `sin lista`.

## Bypass

KernelSU + SUSFS, o APatch, o Magisk + Shamiko con DenyList. Añadir `com.dts.freefireth` y `com.venice.injector`.
