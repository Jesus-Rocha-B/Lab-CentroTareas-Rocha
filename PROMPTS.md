# Bitácora de Asistencia con Inteligencia Artificial (Fase 2)

**Proyecto:** CentroTareas  
**Estudiante:** Jesús Rocha  
**Docente:** Juan León S.  
**Curso:** Programación en Móviles  
**Institución:** Tecsup, Departamento de Tecnología Digital  
**Rama:** `mejora-ia`  

---

## Registro de Prompts y Decisiones de Ingeniería

| Prompt | Resumen de la respuesta | Lo que adaptaste | Tu criterio técnico |
|---|---|---|---|
| «¿Cómo mover la tarea simulada con progreso al Repository exponiéndola como un Flow con flowOn(Dispatchers.Default)?» | Mover el bucle de iteración y retardo de 4 segundos fuera del ViewModel hacia el Repository, retornando un `Flow<Float>` frío creado con el constructor `flow { ... }` y acoplado a `flowOn(Dispatchers.Default)`. En el ViewModel, recolectar los valores emitidos con `.collect { ... }`. | Se actualizó la interfaz `TareasRepository` con el método `fun ejecutarTareaLarga(): Flow<Float>`, se implementó en `TareasRepositoryLocal` emitiendo `i / 100f` con `delay(40)` bajo `flowOn(Dispatchers.Default)`, y en `TareasViewModel` se sustituyó el bucle imperativo local por la recolección reactiva `repo.ejecutarTareaLarga().collect { _progreso.value = it }`. | Respeta el principio de responsabilidad única (SRP) y Clean Architecture: el ViewModel solo debe orquestar estados hacia la vista y no generar o simular bucles de datos en memoria. Al utilizar un `Flow` frío con `flowOn(Dispatchers.Default)`, la emisión ocurre en el pool de fondo de CPU sin saturar el hilo principal, garantizando cancelación cooperativa al detener el `Job`. |
