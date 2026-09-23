# ⚖️ BISTURÍ - Protocolo de SELECCIONAR / DECIDIR

## Función

DECIDIR transforma una comprensión suficiente en una elección de acción, manteniendo visibles la evidencia, la incertidumbre y las consecuencias.

> **Decidir no es inventar certeza.**

---

## 1. ENUMERAR ALTERNATIVAS

Considerar las alternativas relevantes, incluyendo cuando corresponda:

- actuar ahora;
- actuar de forma condicionada;
- realizar una prueba reversible;
- recopilar más información;
- no actuar todavía.

No es necesario fabricar opciones artificiales solo para llenar una tabla.

---

## 2. DEFINIR CRITERIOS

Los criterios deben derivarse del problema y del contexto.

Ejemplos:

- seguridad;
- reversibilidad;
- evidencia disponible;
- impacto;
- coste;
- continuidad;
- aprendizaje;
- dependencias;
- tiempo.

Los criterios deben explicitarse cuando afecten a la selección.

---

## 3. EVALUAR SIN FALSA PRECISIÓN

Para cada alternativa, registrar:

| Campo | Contenido |
|---|---|
| Evidencia a favor | Qué está respaldado |
| Evidencia en contra | Qué la debilita |
| Supuestos | Qué debe ser cierto |
| Riesgos | Qué puede salir mal |
| Dependencias | Qué necesita |
| Reversibilidad | Fácil / parcial / difícil |
| Consecuencias | Corto / medio / largo plazo |
| Estado | CONFIRMADO / PLAUSIBLE / FALLIDO-INCIERTO |

No utilizar puntuaciones numéricas arbitrarias como sustituto del análisis.

Cuando una métrica esté basada en datos reales, puede utilizarse. Cuando sea una valoración subjetiva, debe identificarse como tal.

---

## 4. CONSIDERAR CONSECUENCIAS

Analizar, según la relevancia del caso:

### Corto plazo
- efectos inmediatos;
- bloqueadores;
- coste inicial;
- riesgos inmediatos.

### Medio plazo
- nuevas dependencias;
- mantenimiento;
- deuda;
- cambios de arquitectura;
- aprendizaje generado.

### Largo plazo
- sostenibilidad;
- reversibilidad futura;
- oportunidades que abre o cierra;
- coherencia con la arquitectura.

No convertir predicciones en hechos.

---

## 5. SELECCIONAR UNA ACCIÓN

La selección debe responder:

1. ¿Qué acción satisface mejor el objetivo con la evidencia disponible?
2. ¿Qué riesgos introduce?
3. ¿Qué parte de la decisión depende de supuestos?
4. ¿Puede probarse de forma reversible?
5. ¿Qué evidencia adicional podría cambiar la decisión?

El resultado puede ser:

- **ACCIÓN**: actuar ahora.
- **ACCIÓN CONDICIONADA**: actuar solo si se cumple una condición.
- **PRUEBA REVERSIBLE**: experimentar antes de adoptar.
- **RECOPILAR EVIDENCIA**: investigar antes de decidir.
- **PAUSA**: no existe base suficiente para actuar.
- **NO SÉ TODAVÍA**: la información disponible no permite seleccionar con integridad.

No declarar una opción como “óptima” solo porque una matriz inventada produzca un decimal bonito.

---

## 6. DOCUMENTAR LA DECISIÓN

Formato recomendado:

```markdown
## DECISIÓN: [Tema]
Fecha: [AAAA-MM-DD]

### Objetivo
[...]

### Contexto relevante
[...]

### Alternativas
- A: [...]
- B: [...]
- C: [...]

### Evaluación
#### A
- Evidencia: [...]
- Supuestos: [...]
- Riesgos: [...]
- Reversibilidad: [...]
- Estado: [...]

#### B
[...]

### Selección
Tipo: ACCIÓN | CONDICIONADA | PRUEBA | EVIDENCIA | PAUSA | NO SÉ TODAVÍA

Decisión:
[Qué se hará]

Fundamento:
[Por qué, basado en evidencia y criterios explícitos]

### Condiciones
[Qué debe comprobarse]

### Revisión
[Cuándo o mediante qué evidencia se revisará]
```

---

## 7. REVISIÓN

Una decisión no queda validada porque haya sido ejecutada.

Después de actuar:

- comparar resultado real frente a esperado;
- registrar desviaciones;
- actualizar el estado de las hipótesis;
- conservar el aprendizaje útil;
- corregir la decisión cuando la evidencia nueva lo exija.

Una decisión revisable es más robusta que una decisión defendida por orgullo. La humanidad ya produce suficiente burocracia emocional por su cuenta.
