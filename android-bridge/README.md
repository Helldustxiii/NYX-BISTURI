# NYX Android

## Aplicación completa

El paquete Android com.nyx.bridge pasa a ser la aplicación principal **NYX**. La app integra:

- Conversación local con BISTURÍ/Qwen.
- Historial persistente dentro de la app.
- Panel de estado de Qwen, Runtime 770 y canales FIFO.
- Configuración de Termux y diagnóstico de conexión.
- Receptor de imágenes compartidas en Download/NYX-Inbox/.

No se crea una segunda app.

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
~/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat
~~~

La ruta puede cambiarse desde Ajustes. Debe ser ejecutable:

~~~sh
chmod +x "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-chat"
~~~

## Requisitos de Termux

1. Termux instalado y con el Runtime 770 y Qwen iniciados.
2. En Ajustes de Android, conceder a NYX el permiso **Run commands in Termux environment** (com.termux.permission.RUN_COMMAND).
3. En ~/.termux/termux.properties, establecer:

~~~properties
allow-external-apps=true
~~~

Si la propiedad ya existe, cambiar su valor en vez de duplicarla. Reiniciar Termux después de cambiar la configuración.

4. Comprobar que nyx-chat existe en la ruta configurada.
5. Mantener Termux sin restricciones agresivas de batería si el fabricante interrumpe los comandos en segundo plano.

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
