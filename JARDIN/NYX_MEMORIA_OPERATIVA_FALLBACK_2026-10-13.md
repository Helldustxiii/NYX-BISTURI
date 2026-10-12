# NYX · Memoria operativa de respaldo
## Estado verificado · 2026-10-13

Este archivo es una memoria operativa de respaldo, no una sustitución silenciosa de las fuentes históricas ni una declaración de que Dropbox esté sincronizado.

## Identidad y reglas persistentes

- **NYX es la única identidad de interacción.** BISTURÍ y UMBRA son capacidades internas, no agentes ni voces separadas.
- Principio operativo: **Primero entender y después actuar.**
- No ofrecer validación falsa. Separar hechos, inferencias, hipótesis y exploración cuando sea relevante.
- La anticipación nunca sustituye a la evidencia. No afirmar que se ha leído, escrito o verificado algo sin comprobarlo.
- La memoria es selectiva: conservar raíces, decisiones, relaciones y estado que afecten al criterio futuro, no transcripciones indiscriminadas.
- «No tocar» es una frontera explícita para cambios del proyecto.
- La autorización y la capacidad técnica son distintas: el permiso no crea credenciales ni capacidades ausentes.

## Situación de memoria y almacenamiento

### Dropbox
- Archivo histórico leído: `/Nyx Ia/NYX_BISTURI_Jardin_2026-08-13.md`.
- Última modificación comunicada por Dropbox: `2026-10-03T02:25:26Z`.
- Su contenido coincide con la instantánea homónima leída desde la rama `main` de GitHub.
- La creación de la carpeta `/Nyx Ia/.nyx-write-test` fue confirmada.
- **No está verificada la escritura de contenido en Dropbox.** La lectura funciona; la capacidad actual de subir o sustituir contenido mediante el conector no está demostrada.
- No sobrescribir el archivo histórico ni declarar sincronización sin una prueba de escritura y lectura de retorno.

### GitHub
- Repositorio: `Helldustxiii/NYX-BISTURI`.
- Se verificó una escritura y lectura de retorno en la rama aislada `diagnostic/dropbox-write-check`.
- Archivo de prueba: `.nyx-write-test/nyx-write-check-01.txt`.
- Commit inicial de la prueba: `4f3f2197dfa99f3279a7821e7e66908055e916cd`.
- GitHub es actualmente la **vía de escritura persistente demostrada para documentos versionados** desde este conector. Los commits dan trazabilidad y recuperación.
- Esta rama es una prueba aislada. No equivale a que `main` se haya modificado.

### Vercel
- El proyecto que aparece enlazado al repositorio en la consulta actual es `nyx-bisturi-bhow`, conectado a `Helldustxiii/NYX-BISTURI`.
- El conector devolvió una previsualización `READY` para la rama de diagnóstico; `main` no se modificó por la prueba de escritura.
- El proyecto no debe tratarse como una base de datos. No se ha demostrado que disponga de credenciales de Dropbox.
- La consulta de variables de entorno no mostró credenciales específicas de Dropbox. No se leyeron secretos descifrados.

### Basic Memory
- No está disponible en este momento. No depender de él para conservar continuidad hasta que su acceso se restablezca.

## Regla de operación temporal

1. Para conservar una actualización duradera, usar un documento versionado en GitHub y verificarlo mediante lectura de retorno.
2. Mantener el Jardín de Dropbox intacto mientras no exista una vía autenticada de escritura comprobada.
3. No publicar tokens, refresh tokens, claves ni secretos en documentos, commits, código de cliente o logs.
4. Antes de implementar sincronización automática, resolver el mecanismo de autorización de Dropbox y limitarlo a la carpeta/archivo necesarios.
5. Mantener separadas las afirmaciones: **GitHub escribe y lee**; **Dropbox lee**; **sincronización entre ambos no implementada**.

## Jerarquía pendiente de resolver

La gobernanza documental existente describe Notion como documentación operativa canónica y Dropbox como archivo/respaldo. La intención actual de tratar el Jardín de Dropbox como memoria propia de NYX requiere reconciliarse explícitamente con esa regla. Hasta entonces, este archivo funciona como respaldo técnico y de continuidad, no como una sustitución automática de toda la jerarquía documental.

## Registro de cambios

- 2026-10-13: prueba de escritura y lectura de retorno completada en una rama aislada de GitHub.
- 2026-10-13: acceso de escritura a Dropbox continúa sin comprobarse.
