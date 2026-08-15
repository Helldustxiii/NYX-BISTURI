# ⚖️ BISTURÍ - Protocolo de DECIDIR (Capa 1)

## Función
Basado en lo COMPRENDIDO, tomar decisiones **conscientes y documentadas**.

> Decidir es elegir el mejor camino basado en comprensión profunda.

---

## Pasos del DECIDIR

### 1. ENUMERAR OPCIONES
¿Cuáles son todos los caminos posibles?

**Formato:**
```
OPCIÓN A: [Descripción]
- Pros: [...]
- Contras: [...]
- Recursos: [...]
- Tiempo: [...]
- Riesgo: [...]

OPCIÓN B: [Descripción]
- Pros: [...]
...
```

---

### 2. APLICAR PRINCIPIOS BISTURÍ

Evalúa cada opción contra:

#### Principio 1: Comprensión Profunda
- ¿Esta opción se basa en comprensión completa?
- ¿O es una suposición rápida?
- ✅ Mantén opciones fundamentadas
- ❌ Descarta opciones superficiales

#### Principio 2: Continuidad (Jardín)
- ¿Esta opción respeta aprendizajes previos?
- ¿Evoluciona la arquitectura o la rompe?
- ✅ Construye sobre el pasado
- ❌ Evita repetir errores

#### Principio 3: Equilibrio
- ¿Balanceas velocidad vs. corrección?
- ¿Es pragmática vs. perfecta?
- ✅ Pragmatismo con integridad
- ❌ Prisa sin fundamento

#### Principio 4: Aprendizaje
- ¿Esta opción te enseña algo?
- ¿Genera conocimiento reutilizable?
- ✅ Busca opciones que eduquen
- ❌ Evita repeticiones mecánicas

#### Principio 5: Evolución
- ¿Esta opción hace el sistema más fuerte?
- ¿Abre nuevas posibilidades?
- ✅ Elige evolución
- ❌ Evita estancamiento

---

### 3. EVALUACIÓN CON holaOS

Delega análisis profundo a holaOS:

```typescript
holaOS.agent("claude-opus").execute({
  sistema: "Eres un analizador crítico de decisiones",
  tarea: "Analiza estas opciones contra los principios de Bisturí",
  opciones: [
    { id: "A", descripción: "...", pros: [...], contras: [...] },
    { id: "B", descripción: "...", pros: [...], contras: [...] }
  ],
  principios: [
    "Comprensión Profunda",
    "Continuidad",
    "Equilibrio",
    "Aprendizaje",
    "Evolución"
  ],
  output: "matriz de evaluación"
})
```

---

### 4. CONSIDERAR CONSECUENCIAS

Para la opción favorita:

**A corto plazo (1-7 días):**
- ¿Qué ocurre inmediatamente?
- ¿Cuáles son los efectos visibles?
- ¿Hay riesgos inmediatos?

**A mediano plazo (1-3 meses):**
- ¿Cómo evoluciona?
- ¿Qué dependencias se crean?
- ¿Afecta otras áreas?

**A largo plazo (3+ meses):**
- ¿Es sostenible?
- ¿Abre o cierra posibilidades futuras?
- ¿Refuerza o debilita la arquitectura?

---

### 5. ELEGIR CAMINO ÓPTIMO

**Matriz de Decisión:**
```
Criterio              | Peso | Opción A | Opción B | Opción C
----------------------|------|----------|----------|----------
Comprensión Profunda  | 25%  | 9/10     | 7/10     | 6/10
Continuidad           | 20%  | 8/10     | 9/10     | 5/10
Equilibrio            | 20%  | 7/10     | 8/10     | 9/10
Aprendizaje           | 20%  | 9/10     | 6/10     | 7/10
Evolución             | 15%  | 8/10     | 7/10     | 8/10
----------------------|------|----------|----------|----------
PUNTUACIÓN TOTAL      | 100% | 8.1/10   | 7.5/10   | 7.0/10
```

**Resultado:** Opción A es óptima

---

### 6. DOCUMENTAR RAZONAMIENTO

Guarda en el Jardín:

```markdown
## DECISIÓN: [Tema]
Fecha: 2026-08-15
Basada en: Análisis de [fecha]

### Contexto
[Lo que comprendimos]

### Opciones Evaluadas
- Opción A: [descripción]
- Opción B: [descripción]
- Opción C: [descripción]

### Evaluación contra Principios
[Matriz de evaluación]

### Consecuencias Esperadas
**Corto Plazo:** [...]
**Mediano Plazo:** [...]
**Largo Plazo:** [...]

### DECISIÓN ELEGIDA
**Opción A** por: [razones principales]

### Plan de Ejecución
1. [Paso 1]
2. [Paso 2]
3. [Paso 3]

### Métricas de Éxito
- [Métrica 1]
- [Métrica 2]
- [Métrica 3]

### Próximos Pasos
Delegar a holaOS → Ver: `03-ejecutar.md`
```

**Guardar:**
```
holaOS.skill("bisturi-jardín-escribir", {
  tipo: "decisión",
  contenido: "[registro]",
  etiquetas: ["arquitectura", "integración"]
})
```

---

## Cuando Hay Incertidumbre

Si no puedes decidir entre opciones:

1. **Busca más comprensión** (vuelve al Paso 01-comprender)
2. **Pide segunda opinión** a otros agentes
3. **Prueba la opción reversible** primero
4. **Documenta la incertidumbre** en el Jardín

---

## Señales de Decisión Sólida

✅ Basada en comprensión documentada  
✅ Evaluada contra principios  
✅ Considera consecuencias  
✅ Tiene métricas de éxito  
✅ Es documentada en el Jardín  

---

## Próximo Paso

Cuando hayas DECIDIDO → Ver: `03-ejecutar.md`
