# 🧠 BISTURÍ - Protocolo de COMPRENDER

## Función

**COMPRENDER** construye una representación suficiente, contextualizada y verificable de lo que está ocurriendo antes de actuar.

> **Primero entender y después actuar.**

Comprender no exige certeza absoluta. Exige saber qué se sabe, qué se infiere y qué sigue sin comprobar.

---

## Pasos del COMPRENDER

### 1. RECUPERAR CONTEXTO

Consultar el contexto disponible y el Jardín cuando sea relevante:

- decisiones previas;
- estado actual del proyecto;
- principios vigentes;
- restricciones conocidas;
- aprendizajes que puedan afectar la situación.

La memoria se usa de forma selectiva. No se asume que lo antiguo sigue vigente sin comprobarlo.

### 2. DEFINIR EL PROBLEMA

Separar:

- petición explícita;
- objetivo real;
- síntoma observable;
- resultado deseado;
- límites de la tarea.

Preguntas mínimas:

- ¿Qué se intenta conseguir?
- ¿Qué problema real debe resolverse?
- ¿Qué queda fuera del alcance?
- ¿Qué consecuencias tendría actuar mal?

### 3. SEPARAR EVIDENCIA E INFERENCIA

Para cada afirmación relevante:

| Elemento | Estado |
|---|---|
| Hecho comprobado | **CONFIRMADO** |
| Inferencia razonable | **PLAUSIBLE** |
| Dato contradictorio, insuficiente o no verificable | **FALLIDO-INCIERTO** |

No elevar una inferencia a hecho porque encaje bien con la historia.

### 4. IDENTIFICAR CONTRADICCIONES

Buscar activamente:

- versiones incompatibles;
- datos que no cuadran;
- memoria antigua frente a evidencia actual;
- restricciones ocultas;
- resultados que contradicen la expectativa.

Cuando exista contradicción relevante, no ocultarla para mantener una respuesta coherente.

### 5. IDENTIFICAR INCERTIDUMBRE

Registrar de forma explícita:

- qué falta;
- por qué importa;
- qué podría confirmar o refutar la hipótesis;
- qué acciones siguen siendo seguras pese a la incertidumbre.

Cuando no haya suficiente información:

> **NO SÉ TODAVÍA**

no es un fallo del sistema. Es un estado informativo válido.

### 6. RECUPERAR PATRONES DEL JARDÍN

Buscar antecedentes útiles:

- qué funcionó;
- qué falló;
- qué decisiones ya fueron tomadas;
- qué cambios ya se intentaron;
- qué reglas o dependencias siguen vigentes.

Los antecedentes orientan, pero no sustituyen la evidencia actual.

### 7. DETERMINAR EL UMBRAL DE COMPRENSIÓN

Antes de pasar a DECIDIR, debe quedar claro:

- problema;
- contexto;
- evidencia disponible;
- hipótesis e inferencias;
- contradicciones;
- restricciones;
- riesgos;
- incertidumbres;
- siguiente información necesaria, cuando exista.

No hace falta eliminar toda incertidumbre. Sí hace falta evitar actuar fingiendo que no existe.

---

## Registro de COMPRENDER

Formato recomendado:

```markdown
## ANÁLISIS: [Tema]
Fecha: [AAAA-MM-DD]
Estado: CONFIRMADO | PLAUSIBLE | FALLIDO-INCIERTO

### Objetivo
[Qué se intenta conseguir]

### Problema
[Problema real]

### Contexto
[Contexto relevante]

### Evidencia
- [Dato] — CONFIRMADO
- [Interpretación] — PLAUSIBLE
- [Dato pendiente/contradictorio] — FALLIDO-INCIERTO

### Restricciones
- [...]

### Riesgos
- [...]

### Incertidumbres
- [...]

### Conclusión
[Qué puede afirmarse con seguridad y qué no]

### Próxima necesidad de información
[Qué comprobar antes de avanzar, si aplica]
```

---

## Regla de Salida

COMPRENDER puede terminar en cualquiera de estos estados:

**Listo para decidir**  
La información disponible permite comparar acciones con un nivel de riesgo aceptable.

**Necesita evidencia**  
Falta una comprobación concreta que puede cambiar la decisión.

**Necesita replanteamiento**  
La definición inicial del problema era incorrecta o incompleta.

**No sé todavía**  
No hay base suficiente para sostener una conclusión.

En todos los casos, el estado debe quedar explícito.
