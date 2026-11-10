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

## Receptor de imágenes

NYX Bridge se registra como destino de **Compartir** para imágenes Android.

Flujo:

1. En Dropbox, abre las opciones del JPG y utiliza **Exportar**.
2. En la hoja de compartir de Android, selecciona **NYX Bridge**.
3. El receptor valida el archivo y lo copia a `Download/NYX-Inbox/`.
4. Desde Termux, consulta el contenido con `nyx-bridge inbox`.

Formatos admitidos: JPEG/JPG, PNG y WebP. Límite: 20 MiB por imagen y 5 imágenes por envío. Se comprueban la firma binaria, las dimensiones y el tamaño; la extensión o el MIME declarado por la aplicación emisora no se consideran prueba suficiente.

La importación usa el almacenamiento compartido de Android (MediaStore Downloads), no el directorio privado de la app. No crea enlaces, no sube imágenes a Internet y no envía el contenido a ChatGPT. Las imágenes guardadas quedan accesibles a Termux cuando este tiene concedido el permiso de almacenamiento compartido.

### Preparación de Termux

Concede el permiso de almacenamiento una vez:

```sh
termux-setup-storage
```

Instala el wrapper actualizado desde una copia del repositorio:

```sh
chmod +x android-bridge/tools/nyx-bridge
cp android-bridge/tools/nyx-bridge "$PREFIX/bin/nyx-bridge"
```

Consulta las imágenes recibidas:

```sh
nyx-bridge inbox
```

Ruta compartida esperada:

```text
~/storage/shared/Download/NYX-Inbox/
```

**Límite conocido:** este receptor hace que las imágenes estén disponibles localmente para Termux/NYX-BISTURÍ. No constituye un transporte de imágenes desde Dropbox hacia el modelo ChatGPT de esta conversación.

## Wrapper versionado

El repositorio incluye:

```sh
android-bridge/tools/nyx-bridge
```

Comandos:

- `nyx-bridge ping` — prueba del canal.
- `nyx-bridge device_info` — información básica del dispositivo.
- `nyx-bridge inbox` — lista imágenes locales recibidas.

### Compilar y ejecutar pruebas Android

Desde el directorio del proyecto Android:

```sh
cd android-bridge
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Estado de integraciones

Confirmado anteriormente:

- ADB Wi-Fi desde Termux.
- Acceso de ADB shell al paquete `com.nyx.bridge`.
- `BridgeProvider.call()` mediante `content call`.
- Respuesta `pong`.
- Consulta de información del dispositivo.

Implementado en la rama de integración:

- Adaptador hacia protocolo 770.
- Gateway de conectores 770.
- Canal FIFO como interfaz del adaptador/gateway.

Pendiente:

- Integración del adaptador con el daemon/runtime NYX como proceso único.
- Integración con `memory.db`.
- Integración con `llama.cpp`.
- Prueba de recepción en el dispositivo real y lectura efectiva desde Termux.

La implementación 770 no implica todavía una conexión ChatGPT ↔ Termux en tiempo real.

El servidor HTTP local permanece en el código como experimento histórico. No se considera transporte operativo mientras no exista evidencia reproducible.

## Principio de integración

No conectar una capa con otra únicamente porque ambas existan. Cada enlace debe tener una interfaz observable, una prueba reproducible y una vía de rollback.

## Runtime del protocolo 770

El único consumidor autorizado de `~/NYX/channel/link` es ahora:

```sh
python "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-770-runtime.py"
```

El runtime enruta los tipos `ANDROID` y `CONNECTOR` en un único proceso, mantiene el bloqueo exclusivo `~/NYX/channel/runtime.lock`, y vuelve a abrir el FIFO cuando un emisor cierra su extremo. Los scripts antiguos `nyx-770-android-adapter.py` y `nyx-770-connectors.py` ya no consumen ese FIFO de manera independiente: muestran un aviso y salen.

Pruebas unitarias del enrutamiento:

```sh
python -m unittest discover -s android-bridge/tests -v
```

**Estado de verificación:** la prueba de la CI verifica el enrutamiento con dependencias simuladas. La ejecución del nuevo runtime contra el FIFO y el dispositivo real debe confirmarse en Termux antes de declararlo operativo. No se inicia automáticamente ni modifica el Jardín.


## Dropbox local para NYX-BISTURÍ

Se ha añadido un conector Dropbox de solo lectura en `tools/connectors/dropbox_local.py` y un asistente CLI en `tools/nyx-dropbox`.

- OAuth 2.0 Authorization Code + PKCE; no usa App Secret.
- Scopes mínimos de lectura: metadatos, contenido y cuenta.
- Refresh token en `~/.config/nyx/dropbox.json` (directorio 0700, archivo 0600).
- Acciones 770: `status`, `list`, `download`.
- Descargas de imágenes PNG/JPEG/WebP, máximo 20 MiB, a `~/NYX/Dropbox-Inbox/`.
- No hay operaciones de escritura, borrado, movimiento ni enlaces compartidos.

La App Key debe configurarse en Termux como `NYX_DROPBOX_APP_KEY` y la autorización debe completarse manualmente. El token no se incluye en el repositorio.

La descarga local no implica todavía análisis visual: el soporte multimodal del modelo Qwen instalado debe verificarse por separado.
