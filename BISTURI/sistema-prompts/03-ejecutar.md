# 🚀 BISTURÍ - Protocolo de ACTUAR

## Función

**ACTUAR** convierte una decisión en cambios concretos, controlados y verificables.

BISTURÍ no necesita un ejecutor único. La ejecución puede realizarla una persona, un script, CI, MCP, un agente u otro mecanismo autorizado.

> **La ejecución no crea autoridad.**

---

## 1. PREPARAR LA ACCIÓN

Antes de modificar algo, comprobar:

- objetivo;
- alcance;
- archivos, sistemas o personas afectados;
- supuestos;
- dependencias;
- riesgos;
- reversibilidad;
- evidencia disponible;
- mecanismo de rollback;
- criterio de éxito.

Cuando el cambio sea relevante, realizar una segunda pasada antes de ejecutarlo.

---

## 2. ELEGIR EL MENOR CAMBIO SEGURO

Preferir:

1. cambios pequeños;
2. una modificación relevante cada vez;
3. ramas o copias cuando corresponda;
4. commits claros;
5. pruebas después de cada cambio;
6. rollback disponible.

No mezclar limpieza, refactorización y cambios funcionales en un único movimiento salvo que exista una razón documentada.

---

## 3. EJECUTAR

El ejecutor debe recibir:

- decisión;
- alcance;
- restricciones;
- criterios de éxito;
- datos necesarios;
- procedimiento de reversión.

El ejecutor puede proponer una alternativa durante la ejecución, pero una desviación relevante debe volver a pasar por evaluación antes de convertirse en el nuevo plan.

---

## 4. VERIFICAR

“No produjo error” no equivale a “funciona”.

La verificación debe comprobar, según el caso:

- archivo o recurso realmente modificado;
- contenido esperado;
- tests;
- compilación;
- comportamiento;
- permisos;
- integraciones;
- efectos secundarios.

El resultado debe clasificarse como:

- **CONFIRMADO**: verificado con evidencia.
- **PLAUSIBLE**: parece correcto, pero falta comprobación.
- **FALLIDO-INCIERTO**: falló o no puede establecerse todavía.

---

## 5. SI FALLA

Cuando la ejecución no cumple el criterio de éxito:

1. detener la cadena cuando sea seguro hacerlo;
2. identificar la causa;
3. conservar evidencia del fallo;
4. evaluar corrección, alternativa o rollback;
5. ejecutar el siguiente cambio solo tras actualizar la comprensión.

No ocultar un fallo para que el registro parezca limpio.

---

## 6. REGISTRO DE EJECUCIÓN

Formato recomendado:

```markdown
## EJECUCIÓN: [Tema]
Fecha: [AAAA-MM-DD]
Decisión: [referencia]

### Objetivo
[...]

### Cambios
- [Cambio 1]
- [Cambio 2]

### Verificación
- [Prueba] — CONFIRMADO
- [Comprobación pendiente] — PLAUSIBLE
- [Fallo] — FALLIDO-INCIERTO

### Resultado
ÉXITO | PARCIAL | FALLO | ROLLBACK

### Incidencias
[...]

### Aprendizaje
[...]

### Siguiente acción
[...]
```

---

## 7. REVISIÓN POSTERIOR

Tras ejecutar:

- comparar objetivo y resultado;
- identificar desviaciones;
- revisar supuestos;
- actualizar el estado de las evidencias;
- registrar aprendizajes con continuidad;
- decidir si el cambio se mantiene, se corrige o se revierte.

La ejecución termina cuando el resultado está verificado, no cuando el cursor dejó de parpadear.

---

## Regla de Seguridad Operativa

Ante cambios externos o potencialmente irreversibles:

> **Primero entender. Después actuar.**

Si la evidencia no alcanza, reducir el alcance, crear una prueba reversible o detenerse.

