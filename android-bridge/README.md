# NYX Android

## Aplicación completa

El paquete Android com.nyx.bridge pasa a ser la aplicación principal **NYX**. La app integra:

- Conversación local con BISTURÍ/Qwen.
- Historial persistente dentro de la app.
- Panel de estado de Qwen, Runtime 770 y canales FIFO.
- Configuración de Termux y diagnóstico de conexión.
- Receptor de imágenes compartidas en Download/NYX-Inbox/.

No se crea una segunda app.

El Content Provider de `com.nyx.bridge` responde a `ping` y `device_info` con `bridge_version=0.2`.

## Arquitectura de conversación

~~~text
NYX Android
  -> Termux RUN_COMMAND (comando fijo nyx-chat)
  -> FIFO 770 (link)
  -> Runtime 770 (único lector)
  -> protocol770 / kernel.py
  -> Qwen local
  -> FIFO 770 (out)
  -> nyx-chat recoge el ID coincidente
  -> Termux devuelve stdout por PendingIntent
  -> NYX Android actualiza el historial local
~~~

La app no ejecuta texto del usuario como código shell. El mensaje se pasa como un argumento de una invocación explícita al script nyx-chat. El script solo admite conversación o diagnóstico de estado.

### Ruta por defecto

La app espera encontrar este ejecutable dentro del checkout local de Termux:

~~~text
/data/data/com.termux/files/home/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat
~~~

RUN_COMMAND_PATH debe ser una ruta absoluta. Si Termux está instalado en una ruta diferente, cambia la ruta desde Ajustes de NYX.

La ruta puede cambiarse desde Ajustes. Debe ser ejecutable:

~~~sh
chmod +x "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat"
~~~

## Requisitos de Termux

1. Termux instalado y con el Runtime 770 y Qwen iniciados.
2. En Ajustes de Android, conceder a NYX el permiso **Run commands in Termux environment** (com.termux.permission.RUN_COMMAND).
3. Termux ejecuta el script mediante el runner oficial app-shell y devuelve el resultado a NYX con PendingIntent.\n4. En ~/.termux/termux.properties, establecer:

~~~properties
allow-external-apps=true
~~~

Si la propiedad ya existe, cambiar su valor en vez de duplicarla. Reiniciar Termux después de cambiar la configuración.

5. Comprobar que nyx-chat existe en la ruta configurada.
6. Mantener Termux sin restricciones agresivas de batería si el fabricante interrumpe los comandos en segundo plano.

El permiso Android y allow-external-apps son dos controles de seguridad separados. La documentación oficial de Termux explica ambos y el retorno de resultados mediante PendingIntent.

## Contrato CLI

Chat:

~~~sh
"$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat" "Hola, BISTURÍ"
~~~

Diagnóstico:

~~~sh
"$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat" --status
~~~

El diagnóstico devuelve JSON con el estado del Runtime 770, Qwen, ambos FIFO y allow-external-apps.

El cliente abre out antes de escribir en link, genera un ID único y solo acepta la respuesta con ese ID. Un bloqueo de cliente evita que dos peticiones de la app se pisen. Las respuestas de otros IDs se conservan en ~/NYX/channel/unmatched/.

No debe ejecutarse un cat manual sobre out mientras NYX esté esperando una respuesta: FIFO distribuye los bytes entre lectores y el resultado podría ser consumido por el lector equivocado.

## Receptor de imágenes

NYX también se registra como destino de **Compartir** para imágenes Android. Guarda JPEG, PNG y WebP validados en Download/NYX-Inbox/, con un máximo de 20 MiB por imagen y 5 imágenes por envío. No sube imágenes a Internet ni las inyecta automáticamente en la conversación.

## Compilar y probar

Desde android-bridge/:

~~~sh
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
~~~

Pruebas del CLI:

~~~sh
python -m unittest discover -s tests -v
~~~

El APK debug aparece en app/build/outputs/apk/debug/app-debug.apk.

La CI compila y publica el APK como artefacto del workflow. La instalación y la conversación real deben validarse en el Redmi con Termux, permisos y Runtime activos.

## Seguridad y límites

- No se ofrece ejecución arbitraria de comandos.
- El servicio de resultados interno no está exportado.
- El permiso RUN_COMMAND debe concederse explícitamente.
- El script CLI valida FIFO, estado del Runtime y tamaño del mensaje.
- Un tiempo de espera agotado se comunica como error, no como respuesta vacía.
- El historial de chat queda en almacenamiento privado de NYX.
- El puente HTTP de diagnóstico anterior se conserva por compatibilidad, pero no transporta el chat.

## Regla de integración

Cada enlace debe tener una interfaz observable y una prueba reproducible. La app no se considera completamente validada hasta que el APK se instale en el dispositivo real y una conversación llegue desde Android a Qwen y regrese.


## Dropbox local para NYX-BISTURÍ

El conector Dropbox se ejecuta localmente en Termux mediante OAuth 2.0 Authorization Code + PKCE. No utiliza App Secret.

- Scopes solicitados: `files.metadata.read` y `files.content.read`.
- Acciones permitidas: `status`, `list` y `download` de imágenes.
- No hay operaciones de escritura, borrado, movimiento ni compartición.
- El refresh token se guarda en `~/.config/nyx/dropbox.json` con permisos 0600; el directorio usa 0700.
- Las descargas admiten PNG/JPEG/WebP hasta 20 MiB y se guardan en `~/NYX/Dropbox-Inbox/`.

Desde la raíz del repositorio en Termux:

~~~sh
export NYX_DROPBOX_APP_KEY="APP_KEY_DE_DROPBOX"
python android-bridge/tools/nyx-dropbox auth
python android-bridge/tools/nyx-dropbox status
python android-bridge/tools/nyx-dropbox list --path "/Nyx Ia"
~~~

La App Key es un identificador público de la aplicación. En la consola de Dropbox, habilita **Allow public clients** en OAuth 2 para permitir PKCE. No hace falta registrar un Redirect URI: al omitirlo, Dropbox muestra el código para copiarlo en Termux. La autorización inicial se completa manualmente en el navegador y pegando el código de Dropbox en Termux. No guardes el refresh token en el repositorio ni lo compartas.

La descarga de una imagen no implica que Qwen la haya analizado. La entrada visual requiere verificar por separado el modelo, el proyector multimodal y la versión de llama.cpp instalada en el dispositivo.


## Dropbox desde la aplicación Android

La pestaña **Dropbox** de NYX utiliza el mismo ejecutable fijo `nyx-chat` mediante Termux RUN_COMMAND. No invoca shell arbitrario.

Antes de usarla, completa una sola vez la autorización OAuth PKCE en Termux:

~~~sh
export NYX_DROPBOX_APP_KEY="APP_KEY_DE_DROPBOX"
python "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-dropbox" auth
~~~

En la consola de Dropbox habilita **Allow public clients** y solo los scopes `files.metadata.read` y `files.content.read`. Sustituye la App Key de ejemplo por la tuya; nunca pegues el refresh token en la app ni en el repositorio.

Después, desde la pestaña Dropbox:
- Comprobar autorización.
- Listar una carpeta mediante una ruta absoluta, por ejemplo `/Nyx Ia`.
- Abrir subcarpetas.
- Descargar imágenes PNG/JPEG/WebP hasta 20 MiB a `~/NYX/Dropbox-Inbox/`.

El listado muestra 100 elementos por página. Cuando Dropbox devuelve un cursor, la pestaña permite cargar los siguientes 100 sin perder los resultados anteriores. No hay subida, borrado, movimiento ni compartición. La descarga local todavía no implica análisis visual por Qwen.


## Correo diario local de NYX

La redacción se puede ejecutar localmente con Qwen mediante `nyx-email-daily`. El proceso no consume las FIFO del Runtime 770 ni necesita depuración ADB. Las instrucciones de privacidad, configuración, pruebas y envío manual están en [EMAIL-PRIVADO-LOCAL.md](docs/EMAIL-PRIVADO-LOCAL.md).

Este componente no queda programado ni envía correos durante la instalación. La sustitución de la automatización alojada requiere primero comprobar Qwen en el dispositivo, revisar un borrador y validar un envío manual. Gmail SMTP recibe el correo en claro: el envío local elimina OpenAI/ChatGPT de la generación, pero no oculta el contenido a Google.
