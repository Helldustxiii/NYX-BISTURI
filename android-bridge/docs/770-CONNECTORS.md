# NYX 770 Connector Gateway

Fecha: 2026-09-27

## Arquitectura

~~~text
770 -> connector gateway -> connector registry
                       ├-> Android (local execution)
                       ├-> GitHub (ChatGPT/MCP)
                       ├-> Dropbox (local Termux, OAuth PKCE)
                       ├-> Notion (ChatGPT/MCP)
                       ├-> Vercel (ChatGPT/MCP)
                       ├-> Basic Memory (ChatGPT/MCP)
                       ├-> Airtable (ChatGPT/MCP)
                       ├-> Granola (ChatGPT/MCP)
                       ├-> Automations (ChatGPT/MCP)
                       └-> OpenAI Platform (ChatGPT/MCP)
~~~

## Identidad y contrato

El campo `FROM` del protocolo es una dirección técnica del subsistema y no constituye una identidad conversacional independiente. NYX sigue siendo la única identidad visible; BISTURÍ y UMBRA son capacidades internas.

## Contrato

~~~text
770|ID|FROM|BISTURI|CONNECTOR|{"service":"github","action":"search","args":{...}}|OK
~~~

The registry validates service and action names. Local connectors execute in
Termux. MCP connectors are explicitly marked as requiring a relay.

## Importante

This does not claim that Termux can directly invoke ChatGPT's MCP servers.
There is currently no observable ChatGPT -> Termux live transport in the
environment. The gateway therefore returns mcp_relay_required for MCP-backed
services instead of pretending they executed.

## Seguridad

- No arbitrary shell execution.
- Explicit service allowlist.
- Explicit action allowlist.
- Unknown services/actions fail closed.
- Existing Android allowlist remains ping and device_info.
- Jardín is untouched.

## Examples

~~~text
770|TEST005|TERMUX|BISTURI|CONNECTOR|{"service":"android","action":"ping","args":{}}|OK
~~~

~~~text
770|TEST006|TERMUX|BISTURI|CONNECTOR|{"service":"github","action":"search","args":{"query":"NYX-BISTURI"}}|OK
~~~


## Enrutamiento de un solo lector

`nyx-770-runtime.py` es el punto único de entrada para `ANDROID` y `CONNECTOR`. No se deben iniciar los antiguos scripts de adaptador y gateway como procesos consumidores separados: ambos han quedado como avisos de compatibilidad y terminan sin abrir el FIFO. El runtime obtiene el bloqueo exclusivo `runtime.lock`, valida que `channel/link` sea un FIFO y mantiene separadas las acciones locales de los servicios MCP que requieren un relay.

La prueba automática cubre IDs de respuesta, llamadas Android permitidas, denegación de servicios/acciones desconocidos, respuesta `mcp_relay_required`, payloads inválidos y delegación de otros tipos de mensaje. Sigue siendo necesaria una prueba en el Termux objetivo para confirmar el ciclo FIFO real.


## Dropbox local de solo lectura

Dropbox se ejecuta como conector local de Termux, no como MCP. La primera versión permite consultar el estado, listar carpetas y descargar imágenes. No incluye acciones de escritura, borrado, movimiento ni compartición.

### Preparación única

1. Crea una aplicación Dropbox con acceso Full Dropbox y habilita únicamente los scopes `files.metadata.read` y `files.content.read`.
2. En la sección OAuth 2 de la consola, habilita **Allow public clients** para PKCE. No registres Redirect URI: se omite intencionadamente y Dropbox mostrará el código de autorización para copiarlo en Termux.
3. En Termux, desde la raíz del repositorio, define la App Key:

~~~sh
export NYX_DROPBOX_APP_KEY="APP_KEY_DE_DROPBOX"
~~~

4. Ejecuta `python android-bridge/tools/nyx-dropbox auth` y completa la autorización en el navegador. Dropbox mostrará un código que se pega de vuelta en Termux.
5. El refresh token se guarda en `~/.config/nyx/dropbox.json`; directorio 0700 y archivo 0600. No lo subas al repositorio ni lo compartas.

### Uso

~~~sh
python android-bridge/tools/nyx-dropbox status
python android-bridge/tools/nyx-dropbox list --path "/Nyx Ia"
python android-bridge/tools/nyx-dropbox download "/Nyx Ia/imagen.png"
~~~

Las imágenes descargadas se guardan en `~/NYX/Dropbox-Inbox/`, con límite de 20 MiB, extensión permitida y validación de firma PNG/JPEG/WebP. Los nombres se guardan como nombre base y se evita sobrescribir archivos existentes.

### Protocolo 770

~~~json
{"service":"dropbox","action":"list","args":{"path":"/Nyx Ia"}}
{"service":"dropbox","action":"download","args":{"path":"/Nyx Ia/imagen.png"}}
~~~

La conexión requiere autorización interactiva inicial. Las llamadas posteriores renuevan el access token con el refresh token. El conector no registra ni devuelve tokens. El almacenamiento local está protegido por permisos Unix, pero no cifrado con Android Keystore; el acceso al UID de Termux o un dispositivo comprometido podría leerlo.

**Límite:** Dropbox puede entregar la imagen a Termux. Esto no declara que Qwen actual sea multimodal ni que el modelo haya procesado la imagen. La integración de visión es una fase separada y requiere comprobar el modelo, el proyector visual y la versión de llama.cpp del teléfono.


## Dropbox desde NYX Android

La pestaña Dropbox de la aplicación llama a un conjunto cerrado de argumentos del script `nyx-chat`; no envía texto de usuario a un shell ni permite seleccionar ejecutables arbitrarios.

Interfaz CLI fija:
- `nyx-chat --dropbox-status`
- `nyx-chat --dropbox-list "/Nyx Ia"`
- `nyx-chat --dropbox-download "/Nyx Ia/imagen.png"`

El helper devuelve JSON con `ok`, `kind`, `action` y `data` o `error`. La app recibe el resultado mediante el PendingIntent de Termux y lo guarda en preferencias privadas para actualizar la pestaña.

La autorización inicial permanece manual en Termux: hay que configurar `NYX_DROPBOX_APP_KEY` y ejecutar `nyx-dropbox auth`. No se incorpora un formulario de tokens en Android. La app solo lista, abre carpetas y descarga imágenes PNG/JPEG/WebP. La respuesta de descarga indica la ruta local en `~/NYX/Dropbox-Inbox/`; no afirma que Qwen haya procesado la imagen.

El listado devuelve hasta 100 elementos por llamada. Si Dropbox entrega `has_more` y un cursor, la aplicación puede pedir la página siguiente mediante `nyx-chat --dropbox-more CURSOR`; el servicio de resultados combina las entradas anteriores y nuevas y conserva el cursor más reciente.
