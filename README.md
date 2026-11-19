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

---

## Solución del Paso 4: Cancelar la tarea con Job

Se implementó el control de cancelación cooperativa y prevención de ejecuciones concurrentes mediante una referencia `trabajo: Job?` y el estado reactivo `corriendo: StateFlow<Boolean>`.

* **Prevención de doble inicio:** Si la tarea ya se encuentra en ejecución (`_corriendo.value == true`), nuevas invocaciones a `tareaLarga()` se descartan inmediatamente.
* **Mecanismo de cancelación:** La función `cancelar()` invoca `trabajo?.cancel()`, deteniendo el bucle cooperativo en `delay()`, fijando el estado del mensaje en `"Cancelado"` y congelando la barra de progreso en el porcentaje alcanzado.
* **Garantía en ciclo de vida (bloque `finally`):** El bloque `finally` asegura que `_corriendo.value = false` se ejecute invariablemente ante éxito o cancelación, habilitando el botón **"Iniciar"** y deshabilitando **"Cancelar"**.

---

## Solución del Paso 5: Contar números primos en Dispatchers.Default con cancelación

Se implementó el cálculo intensivo de números primos delegando la carga pesada al despachador optimizado para CPU `withContext(Dispatchers.Default)` y garantizando cancelación cooperativa sin demoras artificiales.

* **Algoritmo en Repository (`TareasRepositoryLocal.kt`):**
  * La función `contarPrimos(hasta: Int)` itera en el rango `2..hasta`.
  * En cada vuelta del bucle se invoca `ensureActive()`, lo que permite cancelar la operación pesada en cualquier instante liberando los núcleos del procesador de forma inmediata.
  * La función privada `esPrimo(n: Int)` evalúa divisores hasta la raíz cuadrada utilizando la condición `while (d.toLong() * d <= n)`, evitando el costo de `sqrt()` y previniendo desbordamientos aritméticos en enteros de 32 bits.
* **Control reactivo en ViewModel (`TareasViewModel.kt`):**
  * Se gestiona el ciclo con `trabajoPrimos: Job?` y se expone `primos: StateFlow<PrimosUi>`.
  * Se mide con precisión el tiempo de ejecución mediante `System.currentTimeMillis()`.
  * Ante una interrupción del usuario, se captura `CancellationException`, se asigna el estado `PrimosUi.Cancelado` y se relanza la excepción con `throw e` para no romper la propagación del ciclo de vida de la corrutina.
  * La función `cancelarPrimos()` detiene la corrutina llamando a `trabajoPrimos?.cancel()`.
* **Interfaz de usuario (`PantallaTareas.kt`):**
  * Se integró la tarjeta *"Contar números primos"* con un campo de texto numérico `OutlinedTextField` inicialmente vacío (con label y borde azulados), validando únicamente enteros positivos $\ge 2$.
  * Botón **"Contar"** reactivo (deshabilitado mientras calcula o si el campo está vacío/inválido) y botón **"Cancelar"** activo solo durante el cómputo.
  * Recuadro de resultado estilizado en verde suave (`#E8F5E9`, borde `#A5D6A7`) con texto verde oscuro (`#1B5E20`), que presenta la cantidad de números primos encontrados y los milisegundos requeridos, comprobándose total fluidez en el hilo principal (el contador del botón *"Tócame"* continúa sumando en tiempo real).

---

## Solución del Paso 6: Dos cargas a la vez con async

Se comparó empíricamente la carga de datos independiente de forma secuencial frente a la concurrencia estructurada mediante `async` y `await()`.

* **Carga secuencial (`cargarUnaPorUna()`):**
  * Invoca primero `repo.cargarPerfil()` (espera de 2000 ms) y luego `repo.cargarPedidos(false)` (espera de 3000 ms).
  * **Tiempo total observado:** $\approx 5000\text{ ms}$ ($2\text{ s} + 3\text{ s} = 5\text{ s}$).
* **Carga paralela / concurrente (`cargarALaVez()`):**
  * Utiliza `coroutineScope` para garantizar concurrencia estructurada y lanza ambas operaciones simultáneamente mediante `async`:
    ```kotlin
    val (perfil, pedidos) = coroutineScope {
        val a = async { repo.cargarPerfil() }
        val b = async { repo.cargarPedidos(false) }
        Pair(a.await(), b.await())
    }
    ```
  * Ambas operaciones corren en paralelo en hilos secundarios. La corrutina principal suspende hasta que la más lenta termina.
  * **Tiempo total observado:** $\approx 3000\text{ ms}$ ($\max(2000, 3000) = 3000\text{ ms}$), logrando una reducción del 40% en el tiempo de espera del usuario.
* **Componentes de UI (`PantallaTareas.kt`):**
  * Tarjeta **"Dos cargas"** con botones *"Una por una"* (`OutlinedButton`) y *"A la vez"* (`Button`).
  * Deshabilitación reactiva de ambos botones mientras `CargaUi.Cargando` para impedir solicitudes duplicadas.
  * `CircularProgressIndicator` centrado durante la petición.
  * Recuadro de resultado verde suave (`#E8F5E9`) detallando modo, nombre del perfil, cantidad de pedidos y milisegundos reales transcurridos, junto con el indicador comparativo de desempeño.

---

## Solución del Paso 7: Estados de error y reintento

Se diseñó la arquitectura de manejo resiliente de fallos en llamadas asíncronas de red/I-O sin comprometer la estabilidad del proceso.

* **Control de simulación (`simularError`):**
  * Se expone `simularError: StateFlow<Boolean>` y la función `cambiarSimularError(valor: Boolean)` en el ViewModel, transmitiendo el parámetro a `repo.cargarPedidos(simularError)`.
* **Tratamiento estructurado de excepciones (`try/catch`):**
  * Se envolvieron las llamadas en un bloque `try/catch` riguroso que prioriza la excepción de control de corrutinas:
    ```kotlin
    try {
        ...
    } catch (e: CancellationException) {
        throw e // Preserva la cancelación cooperativa del Job
    } catch (e: Exception) {
        _carga.value = CargaUi.Error("No se pudo cargar: ${e.message}")
    }
    ```
  * Si la corrutina secundaria lanza una `IOException("Sin conexión")`, se captura de forma controlada y se transiciona de inmediato al estado reactivo `CargaUi.Error`.
* **Mecanismo de recuperación y UI (`PantallaTareas.kt`):**
  * Se incorporó el interruptor `Switch` con etiqueta *"Simular error"* en la tarjeta **"Dos cargas"**.
  * Al activarse el fallo, la interfaz presenta un recuadro de alerta estilizado en rojo suave (`#FEE2E2` con borde `#FCA5A5` y tipografía `#DC2626`) indicando: `"No se pudo cargar: Sin conexión"`.
  * Se habilita el botón interactivo **"Reintentar"** (`OutlinedButton`) que reejecuta directamente la carga concurrente (`cargarALaVez()`).
  * Al desactivar el interruptor y pulsar *"Reintentar"*, la corrutina completa satisfactoriamente y retorna al recuadro verde con la información cargada, sin que la aplicación se cierre en ningún escenario.






