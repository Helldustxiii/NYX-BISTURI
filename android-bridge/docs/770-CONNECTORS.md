# NYX 770 Connector Gateway

Fecha: 2026-09-27

## Arquitectura

~~~text
770 -> connector gateway -> connector registry
                       ├-> Android (local execution)
                       ├-> GitHub (ChatGPT/MCP)
                       ├-> Dropbox (ChatGPT/MCP)
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

Dropbox se ejecuta ahora como conector local de Termux, no como MCP. La primera versión permite consultar el estado, listar carpetas y descargar imágenes. No incluye acciones de escritura, borrado, movimiento ni compartición.

### Preparación única

1. Crea una aplicación Dropbox con acceso **Full Dropbox** y habilita únicamente estos scopes: `files.metadata.read` y `files.content.read`.
2. En Termux, define la App Key (no se necesita App Secret):

```sh
export NYX_DROPBOX_APP_KEY="TU_APP_KEY"
```

3. Ejecuta `python "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-dropbox" auth` y completa la autorización en el navegador. Dropbox mostrará un código que se pega de vuelta en Termux.
4. El refresh token se guarda en `~/.config/nyx/dropbox.json`; directorio 0700 y archivo 0600. El archivo queda dentro del sandbox privado de Termux. No lo subas al repositorio ni lo compartas.

### Uso

```sh
python "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-dropbox" status
python "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-dropbox" list --path "/Nyx Ia/05_Recursos_Visuales"
python "$HOME/nyx-android-bridge-integrate/android-bridge/tools/nyx-dropbox" download "/Nyx Ia/05_Recursos_Visuales/archivo.png"
```

Las imágenes descargadas se guardan en `~/NYX/Dropbox-Inbox/`, con límite de 20 MiB, extensión permitida y validación de firma PNG/JPEG/WebP. Los nombres se guardan como nombre base y se evita sobrescribir archivos existentes.

### Protocolo 770

```json
{"service":"dropbox","action":"list","args":{"path":"/Nyx Ia/05_Recursos_Visuales"}}
{"service":"dropbox","action":"download","args":{"path":"/Nyx Ia/05_Recursos_Visuales/archivo.png"}}
```

La conexión requiere autorización interactiva inicial. Las llamadas posteriores renuevan el access token con el refresh token. El conector no registra ni devuelve tokens. El almacenamiento local de credenciales está protegido por permisos Unix, pero no cifrado con Android Keystore; el acceso al UID de Termux o un dispositivo comprometido podría leerlo.

**Alcance deliberado:** Dropbox puede entregar la imagen a Termux. Esto no declara que Qwen actual sea multimodal ni que el modelo haya procesado la imagen. La integración de visión es una fase separada y debe probarse con un modelo y un proyector multimodal compatibles.
