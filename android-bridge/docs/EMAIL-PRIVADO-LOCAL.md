# Correo diario local de NYX

## Objetivo

El script espera un servidor llama.cpp/Qwen compatible con la API OpenAI en `127.0.0.1:8080`. Comprueba `/health` y `/v1/models` antes de generar, y usa `/v1/chat/completions` para redactar el correo.

La generación se ejecuta localmente en Termux. El proceso no usa ChatGPT, OpenAI API, Vercel ni otro proveedor de inferencia para redactar el mensaje.

El script es independiente del lector único del protocolo 770 y no consume las FIFO `link` ni `out`. Tampoco necesita ADB ni depuración inalámbrica: se ejecuta directamente dentro de Termux y habla con Qwen por loopback.

## Límites de privacidad

- La URL de Qwen queda fijada a `http://127.0.0.1:8080/v1`. El cliente desactiva proxies del entorno, rechaza endpoints remotos y bloquea redirecciones fuera de `127.0.0.1`.
- El perfil, la configuración y la contraseña de aplicación viven en el directorio privado de Termux, nunca en GitHub.
- Los archivos privados se crean con permisos `0700` para directorios y `0600` para archivos; el script rechaza ficheros de configuración, perfil o secreto demasiado abiertos.
- El script no escribe el cuerpo en su propio historial ni en su registro de estado. El estado conserva fechas y huellas SHA-256, no el cuerpo ni el asunto generado. La deduplicación es exacta por huella; no demuestra que dos textos distintos no traten la misma idea.
- El bloqueo de proceso impide dos ejecuciones concurrentes. Una fecha ya enviada no vuelve a enviarse automáticamente.
- El cuerpo permanece en memoria durante la generación y el envío. No debe redirigirse `--preview` a archivos de registro.

**Revisión del servidor local:** llama.cpp documenta una opción específica `--log-prompts-dir` para guardar prompts con fines de depuración, desactivada por defecto. No habilites esa opción para este uso. Evita también `--log-verbose` / `-v`; confirma el comando real de arranque de Qwen antes de dar por cerrada la revisión de privacidad. [Documentación del servidor llama.cpp](https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md).

**Importante:** Gmail SMTP recibe el mensaje en claro. Google puede procesar y almacenar el contenido en sus sistemas. Esta arquitectura retira OpenAI/ChatGPT de la generación, pero no proporciona cifrado de extremo a extremo frente a Google. Para ocultar también el cuerpo a Google habría que añadir OpenPGP y un método de descifrado en el dispositivo receptor; esa fase no está implementada aquí.

## Instalación en Termux

Desde la raíz del repositorio ya clonado:

```sh
mkdir -p "$HOME/bin"
install -m 700 android-bridge/tools/nyx-email-daily "$HOME/bin/nyx-email-daily"
"$HOME/bin/nyx-email-daily" --setup
```

`--setup` pide la cuenta emisora, el destinatario y una contraseña de aplicación de Gmail mediante entrada oculta. No pegues esa contraseña en ChatGPT ni la guardes en el repositorio. Para usar contraseñas de aplicación, la cuenta de Google debe tener activada la verificación en dos pasos y permitir generar una contraseña. Google las considera menos seguras que el inicio de sesión mediante OAuth y recomienda usar `Sign in with Google` cuando esté disponible. [Ayuda oficial de Google](https://support.google.com/accounts/answer/185833).

El asistente crea:

- `~/.config/nyx/email/config.json`
- `~/.config/nyx/email/smtp.password`
- `~/.config/nyx/email/profile.md`

Completa `profile.md` con el contexto privado que quieras que Qwen use. Ese archivo no se sincroniza ni se sube desde este script. No introduzcas contraseñas, tokens, claves privadas ni información que no deba entrar en el prompt local.

## Comprobación y envío manual

```sh
nyx-email-daily --check
nyx-email-daily --preview
nyx-email-daily --send
```

- `--check`: comprueba la API local de Qwen, detecta el modelo servido y valida el perfil/configuración. No genera ni envía correo.
- `--preview`: genera y muestra el borrador, sin enviarlo ni guardarlo. La salida puede quedar registrada por el terminal si se redirige; no la redirijas a logs.
- `--send`: genera y envía el correo. El asunto es deliberadamente genérico (`P.D. privada · fecha`) para no exponer el tema en los metadatos del correo.

El servidor Qwen debe estar iniciado antes de cada ejecución. Si no responde, el script falla sin recurrir a un modelo remoto. El envío solo se registra después de que SMTP acepte el mensaje; como en cualquier sistema SMTP, un corte de conexión en el instante de confirmación puede dejar un resultado ambiguo y requerir comprobación manual del buzón antes de repetir.

## Programación diaria

La programación no se activa durante la instalación del script. Primero deben pasar `--check`, una revisión con `--preview` y un envío manual de prueba con `--send`. Después se puede configurar un disparador local de Termux/Android para ejecutar `nyx-email-daily --send` en el horario deseado.

El disparador deberá comprobar que Qwen está disponible. No se debe recurrir a ChatGPT, a un modelo remoto ni a Vercel como alternativa automática si el teléfono no está disponible. Un fallo de Qwen debe dejar el correo sin enviar y reportar un estado sin contenido.

## Desinstalación y revocación

Para retirar el ejecutable basta con eliminar `~/bin/nyx-email-daily`. Los datos privados permanecen en `~/.config/nyx/email/` y `~/.local/state/nyx/`; elimínalos desde Termux solo después de decidir si quieres conservar el perfil o el registro de deduplicación. Revoca la contraseña de aplicación desde la seguridad de la cuenta Google cuando dejes de usarla.
