# Estado de la arquitectura de orquestación

**Corte documental:** 2026-11-15  
**Commit de referencia auditado:** `1718a72`  
**Propósito:** complementar y actualizar el estado de implementación de [la propuesta arquitectónica](agent-orchestration-architecture.md). Este documento no sustituye el diseño.

## Resumen ejecutivo

La arquitectura tiene un diseño documentado y un primer contrato de orquestación offline implementado. No hay evidencia suficiente para describirla como un sistema de agentes autónomos operativo.

## Estado por capacidad

| Capacidad | Estado | Evidencia observada | Límite |
|---|---|---|---|
| Diseño de orquestación | DOCUMENTADO | `docs/agent-orchestration-architecture.md` | Varias decisiones de despliegue siguen pendientes. |
| Contratos y políticas B1 | IMPLEMENTADO EN CÓDIGO | `android-bridge/tools/nyx_orchestration/` | Contrato local; no ejecuta herramientas externas. |
| Controlador y estados | IMPLEMENTADO EN CÓDIGO | `controller.py` | Ejecutores y revisores son adaptadores suministrados al controlador. |
| Pruebas del contrato | TESTS PRESENTES | `android-bridge/tests/test_nyx_orchestration.py` | Su presencia no demuestra que se hayan ejecutado en este corte. |
| Ejecutor real conectado | NO DEMOSTRADO | No aparece en el controlador B1 inspeccionado. | No afirmar que Qwen, Codex o Copilot ya reciben misiones desde BISTURÍ. |
| Revisión independiente real | NO DEMOSTRADA | El contrato define un revisor; los tests usan un revisor simulado. | La separación de interfaces no equivale a independencia operacional. |
| Aislamiento de procesos / sandbox | NO IMPLEMENTADO EN B1 | La documentación declara explícitamente esta limitación. | La política valida rutas y diffs declarados; no contiene al proceso ejecutor. |
| Transporte por protocolo 770 | NO IMPLEMENTADO EN B1 | La documentación declara que el controlador no conecta con FIFO ni runtime 770. | 770 no debe describirse como bus de ejecución de agentes. |
| Workflow de conectividad Copilot | CONFIGURADO, RESULTADO NO VERIFICADO | `.github/workflows/copilot-runner-smoke.yml`, disparo manual, runner Ubuntu, sesión `empty` y lista de herramientas vacía. | No se ha comprobado aquí una ejecución exitosa ni la disponibilidad del secreto `COPILOT_GITHUB_TOKEN`. |
| Autonomía proactiva persistente | NO DEMOSTRADA | El diseño exige scheduler, daemon, webhook o activador equivalente. | Una conversación no es un proceso residente. |

## Qué hace realmente B1

El controlador offline:

1. valida el manifiesto y la concesión de permisos;
2. reduce el alcance efectivo a las rutas concedidas;
3. solicita un informe al ejecutor suministrado;
4. valida los paths declarados y los incluidos en las cabeceras del diff;
5. rechaza ampliaciones de alcance, renombrados y diffs inválidos o excesivos;
6. exige evidencia no vacía;
7. solicita una decisión explícita a un revisor separado;
8. registra estados como ACCEPTED, REJECTED o BLOCKED.

No aplica parches, no ejecuta shell, no realiza llamadas de red, no escribe en el repositorio y no integra ni despliega cambios.

## Pruebas y verificación

El repositorio contiene una suite para el contrato:

```bash
python -m unittest discover -s android-bridge/tests -p 'test_nyx_orchestration.py' -v
```

**Resultado observado el 2026-11-15:** 26/26 pruebas pasaron en un entorno Python aislado (Python 3.13.5, `unittest`, 0.003 s). El entorno se preparó con una copia de trabajo de los módulos y el archivo de pruebas recuperados de `main` mediante el conector de GitHub. No fue una ejecución sobre un checkout Git íntegro ni en el dispositivo Android; por tanto, confirma el comportamiento de esa copia del snapshot, no una ejecución en Termux ni en producción. Para una verificación reproducible desde el propio repositorio, ejecutar el comando anterior en un checkout limpio.

El workflow Copilot es manual y está limitado a una sesión sin herramientas. Su existencia demuestra que el mecanismo de smoke test está configurado en el repositorio, no que haya autenticado correctamente ni que exista un agente de programación conectado.

## Próximos hitos técnicos

1. Repetir la suite desde un checkout limpio del repositorio y conservar el resultado de ese entorno.
2. Elegir un único ejecutor real para una prueba aislada y de bajo riesgo.
3. Implementar aislamiento de proceso/workspace y permisos efectivos, no solo validación declarativa.
4. Conectar el ciclo al transporte elegido únicamente después de definir concurrencia, cancelación, errores y auditoría.
5. Probar la revisión independiente con un verificador que no comparta ciegamente la conclusión del ejecutor.
6. Mantener push, merge, despliegue, secretos, memoria persistente y producción fuera del alcance inicial.
7. Comprobar el resultado del workflow Copilot y los requisitos de credenciales/coste antes de considerarlo validado.

## Regla de comunicación

Usar estos términos con precisión:

- **Diseñado:** existe especificación.
- **Implementado:** existe código.
- **Probado:** una prueba se ejecutó y produjo un resultado observado.
- **Operativo:** el flujo funciona en el entorno objetivo.
- **Autónomo/proactivo:** existe un activador persistente verificable y límites de ejecución definidos.

No ascender una capacidad de estado por inferencia.
