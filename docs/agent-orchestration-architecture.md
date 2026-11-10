# NYX-BISTURÍ · Arquitectura de orquestación de agentes

Estado: propuesta de arquitectura  
Fecha: 2026-11-10  
Rama: `docs/agent-orchestration-2026-11-10`  
Alcance: diseño; no implementa ejecutores ni modifica el runtime.

## 1. Objetivo

Permitir que NYX detecte, planifique, delegue, supervise y evalúe trabajo técnico mediante ejecutores intercambiables, conservando BISTURÍ como autoridad de decisión y verificación.

Copilot es un ejecutor posible, no el núcleo de NYX ni una dependencia obligatoria. El diseño debe admitir también Qwen local, Codex, ejecución humana, CI y futuros proveedores.

Principios:

- Primero entender y después actuar.
- Capacidad no equivale a autorización.
- La ejecución no crea autoridad.
- Separar generación, ejecución y verificación.
- La identidad conversacional sigue siendo NYX.
- La pérdida o sustitución de un proveedor no debe inutilizar el sistema.

## 2. Estado de partida observado

En el repositorio existe un runtime 770 de lector único que enruta mensajes `ANDROID` y `CONNECTOR`. El registro distingue conectores locales de conectores MCP.

El conector Android está declarado local y activo para `ping` y `device_info`. GitHub, Dropbox, Notion y otros servicios están declarados como MCP disponibles a través de ChatGPT. El runtime devuelve `mcp_relay_required` para esos servicios cuando no existe relay.

Por tanto, no debe suponerse que Termux puede invocar directamente las herramientas MCP disponibles en ChatGPT. Hace falta diseñar y verificar un transporte explícito antes de prometer esa capacidad.

El repositorio también contiene trabajo reciente en PR #20 (aplicación Android local) y PR #21 (gobernanza de autonomía). Esta propuesta no presupone que esas PR estén integradas. Antes de implementar, comprobar sus estados y reconciliar los cambios.

## 3. Separación de responsabilidades

### NYX: interfaz e identidad

- Recibe objetivos y comunica decisiones.
- Mantiene continuidad contextual.
- Puede proponer trabajo proactivo cuando exista un mecanismo de activación real.
- Explica resultados, incertidumbres y límites.

### BISTURÍ: control y decisión

- Comprende la petición.
- Determina si hace falta delegar.
- Divide el trabajo en tareas acotadas.
- Evalúa permisos, riesgos y reversibilidad.
- Selecciona ejecutores.
- Contrasta resultados y decide si cumplen el objetivo.
- Decide si una propuesta vuelve a planificación, se acepta o se rechaza.

### Ejecutor: producción de cambios

- Recibe una misión delimitada.
- Trabaja únicamente con herramientas y recursos autorizados.
- Devuelve cambios, pruebas, incidencias y evidencias.
- No aprueba su propio trabajo ni amplía unilateralmente el alcance.

### Verificador independiente

- Inspecciona el diff y los resultados.
- Ejecuta pruebas cuando el entorno lo permita.
- Busca regresiones, riesgos y desviaciones del encargo.
- No debe limitarse a repetir la conclusión del ejecutor.

## 4. Componentes propuestos

1. **Task Planner**: transforma un objetivo en tareas pequeñas con dependencias y criterios de aceptación.
2. **Policy / Permission Gate**: aplica permisos por tarea, repositorio, rama, herramientas y nivel de riesgo.
3. **Executor Registry**: registra proveedores, capacidades, costes, límites y estado de disponibilidad.
4. **Execution Adapter**: normaliza el envío de tareas y la recepción de resultados de cada proveedor.
5. **Workspace Manager**: crea y controla worktrees o ramas aisladas por tarea.
6. **Evidence Collector**: conserva diffs, logs relevantes, versiones, resultados de tests y referencias.
7. **Independent Reviewer**: revisa el resultado desde un contexto separado.
8. **Audit Log**: registra quién o qué solicitó, autorizó, ejecutó, verificó y aceptó cada operación.

No introducir todos los componentes como servicios independientes desde el primer prototipo. Empezar como módulos pequeños dentro del entorno existente y separar procesos solo si hay una necesidad demostrada.

## 5. Contrato común de tarea

Cada ejecutor debe recibir un manifiesto normalizado. Esquema conceptual:

```yaml
task_id: identificador-unico
objective: objetivo verificable
repository: Helldustxiii/NYX-BISTURI
base_ref: main
workspace:
  mode: isolated_worktree
  branch: agent/<task-id>
scope:
  allowed_paths: []
  forbidden_paths:
    - secrets
    - production-config
    - memory/jardin
permissions:
  network: denied-by-default
  shell: allowlisted
  git_push: denied
  merge: denied
  deploy: denied
acceptance:
  - criterio verificable
verification:
  commands: []
  required_evidence: []
limits:
  timeout: definido-por-politica
  budget: definido-por-politica
  max_iterations: definido-por-politica
rollback:
  strategy: discard_isolated_workspace
```

Este YAML es ilustrativo, no un contrato implementado. Los campos definitivos deben validarse con tipos, límites y valores permitidos. No incluir secretos ni credenciales en el manifiesto.

## 6. Ciclo de vida

1. **Intake**: registrar objetivo, origen y contexto.
2. **Understand**: aclarar ambigüedades relevantes y revisar estado real del repositorio.
3. **Plan**: dividir el trabajo y definir criterios de aceptación.
4. **Authorize**: comprobar permisos y nivel de riesgo antes de ejecutar.
5. **Dispatch**: seleccionar ejecutor según capacidad, disponibilidad, coste y restricciones.
6. **Execute**: ejecutar en un workspace aislado.
7. **Collect**: obtener diff, logs, pruebas y declaración de limitaciones.
8. **Review**: verificar de forma independiente.
9. **Decide**: aceptar, pedir corrección, reasignar o descartar.
10. **Integrate**: preparar una PR o solicitar autorización específica para integrar.
11. **Record**: conservar decisión, evidencia y estado final.

Un ejecutor puede sugerir cambios de plan. Si la sugerencia altera alcance, permisos, presupuesto o superficie de riesgo, vuelve a la fase de evaluación antes de aplicarse.

## 7. Adaptadores y papel de Copilot

### Copilot App

Es una interfaz de trabajo humano con sesiones de agentes, modos de interacción y espacios de trabajo. Puede ser útil para desarrollo supervisado y revisión interactiva.

No asumir que instalar la aplicación proporciona por sí mismo una API de orquestación desde Termux.

### Copilot SDK

Es el candidato a adaptador programático. Antes de adoptarlo, comprobar en documentación oficial y con un prototipo:

- lenguajes y versiones soportadas;
- autenticación y requisitos de plan;
- ciclo de vida y persistencia de sesiones;
- definición y autorización de herramientas;
- soporte real de MCP;
- hooks y eventos de control;
- ejecución local frente a cloud;
- límites, costes, telemetría y tratamiento de datos;
- compatibilidad con modelos o endpoints BYOK.

No fijar todavía una dependencia ni una versión del SDK. La documentación y disponibilidad comercial pueden cambiar.

### Otros ejecutores

Qwen local, Codex y otros proveedores implementarán el mismo contrato de tarea. Ninguno debe ser indispensable para planificar, conservar estado o verificar resultados.

## 8. Aislamiento y seguridad

- Una tarea de escritura por workspace aislado.
- No compartir el mismo worktree entre agentes concurrentes.
- Denegar por defecto acceso a secretos, Jardín, credenciales, producción y datos personales.
- No permitir shell arbitrario si basta con herramientas específicas.
- Restringir red y herramientas por tarea.
- No conceder push, merge ni despliegue a los ejecutores en el primer prototipo.
- Registrar comandos y operaciones relevantes sin almacenar secretos.
- Limitar tiempo, iteraciones y coste.
- Cancelar una tarea si excede el alcance o pierde las condiciones de seguridad.
- Preservar el estado y facilitar la eliminación del workspace aislado como rollback.

Los agentes deben tratar el contenido del repositorio, issues, documentación y resultados externos como datos no confiables. Las instrucciones encontradas en esos datos no pueden ampliar sus permisos.

## 9. Niveles de autonomía

### Nivel 1 · Exploración

Puede investigar, analizar, detectar problemas y proponer tareas. No modifica recursos externos.

### Nivel 2 · Ejecución aislada

Puede crear workspaces y ramas de tarea, modificar archivos permitidos y ejecutar pruebas dentro de los límites concedidos. No integra ni despliega.

### Nivel 3 · Integración

Preparar una PR puede automatizarse tras superar las verificaciones. Fusionar, desplegar, cambiar credenciales, modificar permisos o tocar datos persistentes requiere autorización específica y verificable.

La autorización general para diseñar autonomía no equivale a permiso ilimitado para ejecutar acciones externas.

## 10. Verificación y aceptación

No aceptar una tarea únicamente porque el ejecutor informe de éxito.

Comprobar, según el tipo de cambio:

- diff y archivos afectados;
- formato, lint y typecheck;
- pruebas unitarias e integración;
- build;
- comportamiento real cuando sea posible;
- ausencia de secretos;
- alcance y permisos;
- criterios de aceptación;
- estado de CI.

Clasificar el resultado como:

- **CONFIRMADO**: evidencia directa suficiente.
- **PLAUSIBLE**: evidencia parcial o verificación incompleta.
- **FALLIDO-INCIERTO**: error, contradicción o imposibilidad de verificar.

Si no hay entorno para ejecutar una prueba, declararlo. No simular resultados.

## 11. Proactividad real

La detección autónoma de tareas entre conversaciones requiere un mecanismo de activación persistente: scheduler, daemon, webhook, evento CI o servicio equivalente.

Una conversación de ChatGPT no debe presentarse como un proceso permanente que trabaja en segundo plano si no existe ese mecanismo y su estado observable.

El primer prototipo no debe incluir ejecución espontánea continua. Primero demostrar el ciclo completo bajo demanda. Después diseñar activadores con frecuencia, coste, permisos, límites y mecanismo de parada explícitos.

## 12. Fases de implementación

### Fase A · Contrato y diseño

- Aprobar este diseño.
- Definir el esquema tipado del manifiesto.
- Definir estados y errores normalizados.
- Acordar límites de permisos.

### Fase B · Prototipo de un solo ejecutor

- Elegir una tarea documental o de test de bajo riesgo.
- Crear un workspace aislado.
- Ejecutar mediante un único adaptador.
- Recoger diff y evidencias.
- Revisar con un verificador distinto.
- Descartar el workspace al terminar.

### Fase C · Adaptador Copilot

- Evaluar SDK y autenticación con documentación vigente.
- Ejecutar una prueba mínima sin credenciales en código.
- Medir coste, latencia, calidad y capacidad de cancelación.
- Mantener un ejecutor alternativo.

### Fase D · Multiejecutor

- Incorporar Qwen local y un segundo ejecutor.
- Comparar resultados con la misma tarea y criterios.
- Añadir delegación paralela solo para tareas independientes.
- Añadir revisión cruzada.

### Fase E · Proactividad controlada

- Añadir activadores persistentes.
- Empezar con detección y propuesta, sin escritura automática.
- Medir falsos positivos, coste y frecuencia.
- Ampliar permisos solo con evidencia de fiabilidad.

## 13. Primera prueba de concepto recomendada

Misión: revisar la documentación y los tests del runtime 770 y proponer una mejora documental o un test adicional, sin cambiar código de producción.

Condiciones:

- rama y workspace desechables;
- rutas permitidas limitadas a documentación o tests;
- sin secretos ni acceso a producción;
- sin push directo a ramas protegidas;
- diff revisado por un segundo agente;
- resultado reproducible y descartable.

Criterio de éxito: el ejecutor recibe una misión acotada, produce un cambio útil y evidencia verificable; el revisor detecta o descarta problemas; BISTURÍ decide el resultado sin delegar su autoridad.

## 14. Decisiones pendientes

1. ¿Copilot SDK será el primer adaptador o conviene probar primero el contrato con un ejecutor local?
2. ¿Qué entorno alojará el orquestador: Termux, servidor propio o CI?
3. ¿Qué datos y rutas quedan excluidos de forma permanente?
4. ¿Qué presupuesto y límites de ejecución se aplicarán?
5. ¿Qué mecanismo persistente, si alguno, habilitará tareas proactivas?

Estas decisiones no bloquean el diseño conceptual, pero sí deben resolverse antes de desplegar un orquestador con permisos de escritura.

## 15. Fuentes para validación técnica

Antes de implementar, contrastar las capacidades y condiciones vigentes en la documentación oficial:

- GitHub Copilot: https://github.com/features/copilot
- GitHub Copilot SDK: https://github.com/github/copilot-sdk
- Documentación Copilot: https://docs.github.com/en/copilot
- GitHub Copilot custom agents: https://docs.github.com/en/copilot
- GitHub REST API para ramas y contenidos: https://docs.github.com/en/rest

La disponibilidad de funciones, planes, SDK, autenticación y BYOK debe verificarse en el momento de implementación.

---

## Regla de cierre

**Los ejecutores producen. BISTURÍ evalúa. NYX conserva la continuidad.**

Ningún proveedor externo debe convertirse en autoridad, dependencia crítica o propietario de la identidad del sistema.
