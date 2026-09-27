# NYX Android Bridge

## Estado

Bridge Android mínimo entre **Termux** y la aplicación Android.

Transport:
- Android ContentProvider
- autoridad: `com.nyx.bridge.provider`
- comunicación local en el mismo dispositivo
- métodos actuales de solo lectura

## Prueba desde Termux

Abrir NYX Bridge en Android y ejecutar:

```sh
/system/bin/content call --uri content://com.nyx.bridge.provider --method ping
```

Debe devolver un `Bundle` con:

```text
status=ok
response=pong
bridge_version=0.1
transport=content_provider
```

Información básica del dispositivo:

```sh
/system/bin/content call --uri content://com.nyx.bridge.provider --method device_info
```

## Alcance de esta versión

Esta implementación **no** conecta todavía:
- protocolo 770
- FIFO
- SQLite/memory.db
- llama.cpp
- daemon/runtime NYX

El objetivo de 0.1 es demostrar primero el canal Android ↔ Termux de forma simple y reversible.
