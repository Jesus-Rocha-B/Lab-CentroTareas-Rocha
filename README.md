# CentroTareas: que la pantalla nunca se congele

**Laboratorio N° 14: Hilos en Android**  
**Desarrollado por:** Jesús Rocha  
**Docente:** Juan León S.  
**Institución:** Tecsup, Departamento de Tecnología Digital  
**Curso:** Programación en Móviles  

---

## Observación del Paso 1: Provocar el problema (la pantalla se congela)

### 1. ¿Qué pasa con la pantalla?
Al pulsar el botón **"Tarea larga (MAL)"**, la interfaz de usuario se congela completamente durante 4 segundos. Durante este intervalo:
* Si el usuario pulsa repetidamente el botón **"Tócame: n"**, el contador no se incrementa.
* La pantalla no responde a ningún gesto táctil ni muestra animaciones de pulsación (*ripple effect*).
* La aplicación aparenta estar completamente bloqueada o colgada.

### 2. ¿Por qué no se ve "Trabajando..."?
Aunque en el código se asigna `_mensaje.value = "Trabajando..."` antes de la espera, esta instrucción se ejecuta secuencialmente en el **hilo principal (Main / UI Thread)**. Inmediatamente después, se invoca `Thread.sleep(4000)`, lo que pone a dormir al hilo principal encargado de:
1. Atender eventos de entrada del usuario (*touch events*).
2. Procesar las recomposiciones de Jetpack Compose.
3. Dibujar los fotogramas en pantalla a 60 fps / 120 fps (*Choreographer / vsync*).

Al despertar tras 4 segundos, el código asigna `_mensaje.value = "Terminó"`. Por lo tanto, cuando el hilo principal finalmente queda libre para recomponer y redibujar la vista, el valor intermedio *"Trabajando..."* ya fue sobrescrito, renderizándose directamente *"Terminó"*.

### 3. ¿Qué crees que mostraría Android si la espera fuera de 10 segundos?
Si el hilo principal permanece bloqueado por **más de 5 segundos** sin atender eventos de entrada del usuario o despachar mensajes del sistema, el sistema operativo Android detecta una pérdida de respuesta y lanza el diálogo del sistema **ANR (Application Not Responding)**:

> *"CentroTareas no responde. ¿Quieres cerrarla o esperar?"*

Este cuadro de diálogo permite al usuario forzar el cierre inmediato de la aplicación (*Force Close*), deteriorando gravemente la experiencia de usuario y provocando reportes de fallos en producción.

---

## Solución del Paso 2: Arreglarlo con una corrutina (withContext)

Se reemplazó el bloqueo de `Thread.sleep` por una corrutina estructurada lanzada con `viewModelScope.launch`. La tarea pesada se delega a un hilo de fondo del pool de trabajadores mediante `withContext(Dispatchers.Default)` utilizando la función de suspensión `delay(4000)`.

* **Comportamiento obtenido:** Mientras la tarea corre en segundo plano y muestra *"Trabajando..."*, el hilo principal permanece 100% libre para dibujar fotogramas y atender interacciones. El contador del botón **"Tócame: n"** continúa sumando fluidamente.
* **Traza verificada en Logcat (filtro `HILO`):**
  * `Antes: main` (Hilo principal donde se actualiza el estado a "Trabajando...")
  * `Dentro: DefaultDispatcher-worker-1` (Hilo de fondo en segundo plano donde se realiza la espera sin bloquear la UI)
  * `Despues: main` (Retorno automático al hilo principal para actualizar el estado final a "Terminó")

---

## Solución del Paso 3: Mostrar el progreso con StateFlow

Se incorporó un flujo reactivo inmutable `progreso: StateFlow<Float>` en el ViewModel para emitir valores normalizados entre `0f` y `1f` hacia la interfaz de usuario.

* **Algoritmo de avance:** Se ejecuta un bucle de 100 iteraciones en `withContext(Dispatchers.Default)`. Cada iteración suspende por `delay(40)` y actualiza el progreso con `i / 100f`, completando el 100% de la tarea en aproximadamente 4 segundos sin saturar el hilo principal.
* **Componentes de UI:** La interfaz observa el estado mediante `collectAsState()` y renderiza el componente oficial `LinearProgressIndicator(progress = { progreso })` junto con el porcentaje numérico calculado mediante `(progreso * 100).toInt() %`.


