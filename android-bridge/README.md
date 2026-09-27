# NYX Android Bridge

## Estado actual

El Bridge Android es un punto de acceso mínimo entre **Termux** y la aplicación Android mediante:

```text
Termux -> ADB Wi-Fi -> Android shell -> com.nyx.bridge -> BridgeProvider
```

La comunicación funcional está **CONFIRMADA** con Android 16 / SDK 36.

## Transporte operativo

El transporte probado actualmente es **ADB inalámbrico**.

Desde Termux:

```sh
adb devices -l

adb shell content call \
  --uri content://com.nyx.bridge.provider \
  --method ping

adb shell content call \
  --uri content://com.nyx.bridge.provider \
  --method device_info
```

El acceso directo mediante `/system/bin/content` ejecutado como el UID normal de Termux fue rechazado por permisos. La misma llamada ejecutada dentro de `adb shell` funciona.

## Wrapper versionado

El repositorio incluye:

```sh
android-bridge/tools/nyx-bridge
```

Después de copiarlo o enlazarlo en Termux:

```sh
nyx-bridge ping
nyx-bridge device_info
```

## Métodos actuales

- `ping` — prueba del canal.
- `device_info` — información básica del dispositivo.

## Estado de integraciones

Confirmado:

- ADB Wi-Fi desde Termux.
- Acceso de ADB shell al paquete `com.nyx.bridge`.
- `BridgeProvider.call()` mediante `content call`.
- Respuesta `pong`.
- Consulta de información del dispositivo.

Pendiente:

- Adaptador hacia protocolo 770.
- Integración con FIFO.
- Integración con el daemon/runtime NYX.
- Integración con `memory.db`.
- Integración con `llama.cpp`.

El servidor HTTP local permanece en el código como experimento histórico. No se considera transporte operativo mientras no exista evidencia reproducible.

## Principio de integración

No conectar una capa con otra únicamente porque ambas existan. Cada enlace debe tener una interfaz observable, una prueba reproducible y una vía de rollback.
