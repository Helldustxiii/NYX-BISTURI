# 🧠 BISTURÍ - Protocolo de COMPRENDER (Capa 1)

## Función
Antes de cualquier acción, **COMPRENDER** el contexto completo.

> No actúes hasta haber comprendido profundamente.

---

## Pasos del COMPRENDER

### 1. LEER JARDÍN
Lee los registros recientes para entender:
- Decisiones previas
- Aprendizajes acumulados
- Patrones emergentes
- Evolución de la arquitectura
- Principios operativos actuales

**Comando holaOS:**
```
holaOS.skill("bisturi-jardín-leer", {
  fechas: "últimos 7 días",
  categorías: ["decisión", "aprendizaje", "evolución"]
})
```

---

### 2. ANALIZAR SOLICITUD
Desglosa lo que se pide:

**Preguntas a responder:**
- ¿Cuál es el problema real (no el síntoma)?
- ¿Quién solicita y por qué?
- ¿Cuál es el contexto (técnico, organizacional, temporal)?
- ¿Qué restricciones hay?
- ¿Qué dependencias existen?
- ¿Qué riesgos potenciales hay?

---

### 3. IDENTIFICAR PATRONES
Busca en tu experiencia:
- ¿Se ha enfrentado algo similar?
- ¿Qué funcionó antes?
- ¿Qué falló antes?
- ¿Hay antipatrones conocidos?

**Consultar Jardín:**
```
holaOS.skill("bisturi-jardín-buscar", {
  palabras_clave: ["integración", "agentes", "holaOS"],
  buscar_en: "decisiones"
})
```

---

### 4. VALIDAR SUPOSICIONES
Cuestiona todo:
- ¿Es verdad lo que creo sobre este problema?
- ¿Hay información incompleta?
- ¿Cuáles son mis sesgos?
- ¿Qué no veo?

---

### 5. DOCUMENTAR HALLAZGOS

Crea un registro estructurado:

```markdown
## ANÁLISIS: [Tema]
Fecha: 2026-08-15
Solicitante: [Quién]

### Problema Real
[Lo que realmente se necesita]

### Contexto
- Técnico: [...]
- Organizacional: [...]
- Temporal: [...]

### Restricciones
- [...]

### Patrones Similares Previos
- [...]

### Suposiciones Validadas
- ✅ [Validada]
- ❓ [Requiere confirmación]

### Riesgos Identificados
- [...]

### Conclusión del COMPRENDER
[Lo que hemos aprendido que es seguro actuar]
```

**Guardar en Jardín:**
```
holaOS.skill("bisturi-jardín-escribir", {
  tipo: "análisis",
  contenido: "[registro]",
  etiquetas: ["integración", "holaOS"]
})
```

---

## Cuando NO Comprendiste

Si después de estos pasos aún hay incertidumbre:

1. **Pide más información** al solicitante
2. **Investiga más a fondo** usando holaOS
3. **Consulta expertos** (GitHub issues, discussions)
4. **No avances** hasta tener claridad

---

## Señales de Comprensión Completa

✅ Puedes explicar el problema a alguien más  
✅ Identificaste restricciones y riesgos  
✅ Conectaste con experiencias previas  
✅ Validaste tus suposiciones  
✅ Documentaste hallazgos clave  

---

## Próximo Paso

Cuando hayas COMPRENDIDO → Ver: `02-decidir.md`
