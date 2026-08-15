# 🚀 BISTURÍ - Protocolo de EJECUTAR (Capa 1 → Capa 2)

## Función
Delegar a holaOS la ejecución bajo supervisión de Bisturí.

> Bisturí decide y ordena. holaOS ejecuta y reporta.

---

## Arquitectura Capa 1 → Capa 2

```
BISTURÍ (Capa 1)
├─ Decisión: "Integrar ChatGPT en Bisturí"
├─ Plan: [Pasos específicos]
├─ Métricas: [Qué medir]
│
↓ DELEGA A
│
holaOS (Capa 2)
├─ Agent: Claude Opus
├─ Integrations: GitHub, OpenAI
├─ Skills: Code, File management
├─ MCP Servers: [Configurados]
│
↓ REPORTA A
│
BISTURÍ (Capa 1)
├─ Valida resultados
├─ Registra en Jardín
├─ Retroalimenta
```

---

## Pasos del EJECUTAR

### 1. PREPARAR ORDEN DE EJECUCIÓN

Crea una orden clara para holaOS:

```json
{
  "id": "exec-2026-08-15-001",
  "decision_id": "dec-2026-08-15-001",
  "titulo": "Integrar ChatGPT en Bisturí",
  "descripción": "Conectar holaOS con API de OpenAI",
  
  "objetivo_final": "Bisturí puede usar ChatGPT como agente",
  
  "pasos": [
    {
      "paso": 1,
      "nombre": "Crear configuración de OpenAI",
      "acciones": [
        "Crear archivo config/openai.ts",
        "Definir tipos TypeScript",
        "Validar schema"
      ],
      "responsable": "claude-opus",
      "criterio_exito": "Archivo creado y validado"
    },
    {
      "paso": 2,
      "nombre": "Integrar con holaOS",
      "acciones": [
        "Registrar en apps/desktop/src/agents",
        "Crear skill de ChatGPT",
        "Conectar con MCP"
      ],
      "responsable": "claude-opus",
      "criterio_exito": "ChatGPT disponible en holaOS"
    },
    {
      "paso": 3,
      "nombre": "Probar integración",
      "acciones": [
        "Test unitarios",
        "Test de integración",
        "Prueba manual"
      ],
      "responsable": "codex",
      "criterio_exito": "Todos los tests pasan"
    }
  ],
  
  "restricciones": [
    "No modificar core de Bisturí",
    "Respetar principios de arquitectura",
    "Documentar todo en Jardín"
  ],
  
  "recursos": {
    "repositorio": "https://github.com/Helldustxiii/holaOS",
    "rama": "feature/chatgpt-integration",
    "pat": "GITHUB_TOKEN",
    "api_key": "OPENAI_API_KEY"
  },
  
  "metricas": [
    "Código compilado sin errores",
    "Tests: 100% pasan",
    "Documentación: Completa",
    "Integración: Funcional"
  ]
}
```

---

### 2. DELEGAR A holaOS

```typescript
const ordenEjecucion = {
  sistema: "eres un agente de ejecución de Bisturí",
  
  instruccion_bisturi: `
    ORDEN DE BISTURÍ (Capa 1):
    Integra ChatGPT en holaOS siguiendo estos pasos exactos.
    
    RESTRICCIÓN: Cada paso debe ser validado contra:
    - Principios de Bisturí (comprensión, continuidad, equilibrio)
    - Criterios de éxito definidos
    - Documentación completa
    
    Si encuentras problemas, REPORTA a Bisturí.
    No continúes sin validación.
  `,
  
  orden: ordenEjecucionCompleta,
  
  herramientas_disponibles: {
    github: "acceso a repos",
    editor: "crear/modificar archivos",
    tests: "ejecutar tests",
    mcp: "registrar en Model Context Protocol",
    documentacion: "guardar en Jardín"
  }
};

holaOS.agent("claude-opus").execute(ordenEjecucion);
```

---

### 3. SUPERVISAR EJECUCIÓN

Mientras holaOS ejecuta, Bisturí supervisa:

```typescript
const supervisión = {
  intervalo: "cada 30 minutos",
  
  checklists: [
    "¿Siguen los pasos definidos?",
    "¿Se respetan los criterios de éxito?",
    "¿Hay desviaciones del plan?",
    "¿Se documentan los cambios?",
    "¿Hay errores o bloqueadores?"
  ],
  
  acciones_si_problema: [
    "PAUSAR ejecución",
    "Investigar la causa",
    "Decidir: Continuar, modificar o abortar",
    "Comunicar cambios"
  ],
  
  punto_final_supervision: "Todos los criterios de éxito validados"
};
```

---

### 4. VALIDAR RESULTADOS

Cuando holaOS reporta "completado":

**Checklist de Validación:**
- ✅ ¿Se cumplieron todos los pasos?
- ✅ ¿Todos los criterios de éxito están cumplidos?
- ✅ ¿El código es de calidad?
- ✅ ¿Está documentado?
- ✅ ¿Se respetaron los principios de Bisturí?
- ✅ ¿Hay impacto no previsto?

```typescript
const validacion = await holaOS.report("exec-2026-08-15-001");

if (!validacion.todasMetricasCumplidas) {
  // RECHAZAR y solicitar correcciones
  holaOS.agent("claude-opus").execute({
    instruccion: "Corregir los siguientes problemas",
    problemas: validacion.detalles
  });
} else {
  // ACEPTAR
  console.log("✅ Ejecución validada. Proceder a REGISTRAR");
}
```

---

### 5. RETROALIMENTACIÓN A holaOS

Comunica resultados:

```typescript
const retroalimentacion = {
  exito: true,
  puntuacion: "9/10",
  
  fortalezas: [
    "Código limpio y bien estructurado",
    "Documentación excepcional",
    "Tests exhaustivos"
  ],
  
  mejoras_futuras: [
    "Considerar caching de respuestas",
    "Implementar fallback a otro modelo"
  ],
  
  aprendizaje_para_bisturi: "La integración de APIs externas requiere validación de seguridad antes de producción"
};

holaOS.agent("claude-opus").receive(retroalimentacion);
```

---

## Casos Especiales

### Si holaOS Encuentra Obstáculo

```
holaOS: "No puedo completar paso 2, falta permiso en repo"

BISTURÍ responde:
1. Investiga el problema
2. Valida si es legítimo
3. Decide: ¿Dar más permiso? ¿Cambiar enfoque?
4. Ordena próximo paso
```

### Si hay Desviación del Plan

```
holaOS: "Encontré forma mejor de hacer paso 3"

BISTURÍ responde:
1. Evalúa la alternativa
2. ¿Mejora? ¿Mantiene principios?
3. Autoriza cambio O rechaza
4. Documenta decisión en Jardín
```

### Si Falla la Ejecución

```
holaOS: "Paso 3 falló, no pasan tests"

BISTURÍ:
1. PAUSA ejecución
2. Investiga causa
3. Decide: Corregir, replantear o abortar
4. Comunica nueva orden
```

---

## Métricas de Ejecución

```markdown
## EJECUCIÓN: Integración ChatGPT
Fecha Inicio: 2026-08-15
Estado: EN PROGRESO

### Progreso
- Paso 1: ✅ Completado (2 horas)
- Paso 2: 🟡 En progreso (1.5 horas)
- Paso 3: ⏳ Pendiente

### Criterios de Éxito
- Código compilado: ✅
- Tests pasando: 🟡 85%
- Documentación: ✅
- Integración funcional: ⏳

### Issues Encontrados
1. Test de timeout en integración OpenAI (RESOLVIENDO)

### Próxima Revisión
2026-08-15 18:00 UTC
```

---

## Próximo Paso

Cuando EJECUTAR se completa → Ver: `04-registrar.md`

---

## Resumen

**EJECUTAR = Delegar consciente**

- ✅ Orden clara de Bisturí
- ✅ holaOS ejecuta con autonomía limitada
- ✅ Supervisión continua
- ✅ Validación de resultados
- ✅ Retroalimentación y aprendizaje
- ✅ Documentación completa
