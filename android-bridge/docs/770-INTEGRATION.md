# NYX: enlace 770 -> Android Bridge

Fecha: 2026-09-27

## Estado demostrado

La comunicación por estas capas está probada de forma independiente y reproducible:

```text
Termux
  -> protocol 770 / FIFO
  -> nyx-770-android-adapter.py
  -> nyx-bridge
  -> ADB Wi-Fi
  -> Android shell
  -> com.nyx.bridge
  -> BridgeProvider
```

### Evidencia

La arquitectura sigue unificada bajo NYX como identidad única. El nombre `BISTURI` que aparece en ejemplos de paquetes es un identificador técnico del endpoint interno, no una segunda entidad conversacional.

### Evidencia

- `protocol770.py` permanece activo en Termux.
- El protocolo 770 ha respondido correctamente a un `PING` real mediante `~/NYX/channel/link` y `~/NYX/channel/out`.
- `nyx-bridge ping` devuelve `pong`.
- `nyx-bridge device_info` devuelve Xiaomi / `24090RA29G` / Android 16 / SDK 36.
- La comunicación ADB inalámbrica está confirmada.

## Adaptador

Archivo:

```text
android-bridge/tools/nyx-770-android-adapter.py
```

El adaptador no modifica el contrato interno de `protocol770.py`. Importa su parser/procesador y añade un tipo `ANDROID` con una lista cerrada de métodos:

```text
ping
device_info
```

Ejemplo de paquete:

```text
770|TEST004|TERMUX|BISTURI|ANDROID|{"method":"ping"}|OK
```

La respuesta se devuelve como `RESULT` o `ERROR` en el FIFO de salida.

## Regla operativa

El adaptador y `protocol770.py` no deben leer simultáneamente el mismo FIFO de entrada. El adaptador es una alternativa de proceso para la futura integración operativa.

## Límite actual

El enlace local Termux -> Android está implementado.

El enlace directo ChatGPT -> Termux no está conectado todavía como transporte en tiempo real: en esta sesión no hay un dispositivo Termux expuesto mediante Remote Desktop Commander ni otro canal MCP directo hacia el proceso local.

Por tanto, no se declara una conexión ChatGPT <-> Termux en tiempo real hasta que exista un transporte observable y verificable.

## Seguridad y reversibilidad

No se expone ejecución arbitraria de shell desde Android. Solo se permiten los métodos explícitamente incluidos en la lista blanca del adaptador.

No se modifica Jardín.

