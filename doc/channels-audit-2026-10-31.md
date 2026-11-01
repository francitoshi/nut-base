# Auditoría del paquete `channels`

**Fecha:** 2026-10-31  
**Alcance:** `nut-base-commons/src/main/java/io/github/francitoshi/nut/base/commons/concurrent/channel/`  
**Archivos revisados:** 19 clases, ~3.099 líneas, más los tests del paquete.  
**Tipo de auditoría:** solo lectura. No se modificó ningún archivo del repositorio.

## Resumen

Se revisaron los canales buffered, unbuffered, unlimited, conflated, duplex, fan-out y `Select`, tanto en sus variantes cerrables como no cerrables. El foco fue condiciones de carrera, pérdida de datos, bloqueos indefinidos, starvation y degradación de rendimiento.

**Resultado:** 6 hallazgos críticos, 8 medios y 5 menores. Los críticos están reproducidos con harnesses ejecutables contra las clases actuales del proyecto.

Los dos problemas de fondo más graves son:

1. **El cierre usa un `POISON` en la misma cola que los valores reales, pero se inserta antes de esperar a los escritores en vuelo.** Un escritor que ya pasó el chequeo de `closed` puede depositar su valor después del `POISON`; el consumidor recibe `null` y el valor queda huérfano en la cola.
2. **Varias esperas de cierre no tienen deadline real** (`close(timeout)` puede tardar mucho más que el timeout, o no terminar) y **`CloseableFanoutChannel.close()` no tiene timeout alguno**.

## Tabla de hallazgos

| ID | Severidad | Hallazgo | Ubicación principal |
|---|---|---|---|
| C1 | Crítico | `POISON` antes que los escritores en vuelo: pérdida de datos en `CloseableBufferedChannel`/`CloseableUnbufferedChannel`; el `put(...,0,unit)` además escapa del conteo de escritores | `CloseableBufferedChannel.java` 108-120, 162-200, 300-345 |
| C2 | Crítico | `POISON` antes de adquirir el write-lock: pérdida de datos en `CloseableUnlimitedChannel` | `CloseableUnlimitedChannel.java` 126-164, 253-282 |
| C3 | Crítico | Carrera entre `drainAfterClose()` y un escritor que aún no ha hecho `queue.put`: el lector devuelve `null` con un valor todavía en cola | `CloseableBufferedChannel.java` 271-298 |
| C4 | Crítico | `close(timeout)` ignora el timeout mientras inserta `POISON` para los lectores; puede tardar O(gets × 100 ms) o no terminar | `CloseableBufferedChannel.java` 300-345 |
| C5 | Crítico | `DuplexChannel` hereda el iterador no cerrable: después de cerrar la entrada devuelve `hasNext=true`/`next=null` para siempre | `DuplexChannel.java` 17, 44-54; `Channel.java` 77-94 |
| C6 | Crítico | `CloseableFanoutChannel.close()` espera indefinidamente a escritores en vuelo; un target que ignora interrupciones cuelga el cierre | `CloseableFanoutChannel.java` 202-244 |
| M1 | Medio | `addTarget()` después de `close()` deja el nuevo target abierto para siempre | `FanoutChannel.java` 66-70; `CloseableFanoutChannel.java` 231-241 |
| M2 | Medio | `CloseableFanoutChannel`: carrera de `closeResult`, idempotencia rota y fallos parciales que nunca se reintentan | `CloseableFanoutChannel.java` 61-67, 202-244 |
| M3 | Medio | Fan-out secuencial: un target lento bloquea a todos; el timeout es por target (total N × timeout) y la entrega puede ser parcial | `FanoutChannel.java` 100-131; `CloseableFanoutChannel.java` 145-166 |
| M4 | Medio | `Select` queda en polling pegajoso a 200 µs para siempre si alguna vez hubo un canal que lo requiere | `Select.java` 150-163, 219-222 |
| M5 | Medio | `Select` puede marcar un canal como agotado mientras un cierre está en curso y descartar un valor que se commiteará después | `Select.java` 186-198; `CloseableBufferedChannel.java` 300-345 |
| M6 | Medio | `close()` no es idempotente de verdad en buffered/unlimited: llamadas concurrentes o repetidas re-ejecutan el cierre y pueden devolver resultados distintos | `CloseableBufferedChannel.java` 300-359; `CloseableUnlimitedChannel.java` 253-296 |
| M7 | Medio | `close()` interrumpe indiscriminadamente a escritores que no están bloqueados; el flag de interrupción puede quedar activo en el hilo llamador | `CloseableBufferedChannel.java` 305-316; `CloseableFanoutChannel.java` 211-228 |
| M8 | Medio | `join()` retorna apenas se marca `closed`, antes de que terminen los escritores o de que se cierren los targets | `CloseableBufferedChannel.java` 371-380; `CloseableUnlimitedChannel.java` 307-317; `CloseableFanoutChannel.java` 265-274 |
| m1 | Menor | `CloseableUnlimitedChannel.drainAfterClose()` hace busy-spin con `Thread.yield()` mientras haya un read-lock tomado | `CloseableUnlimitedChannel.java` 234-251 |
| m2 | Menor | `CloseableConflatedChannel.put(timeout)` ignora el timeout y `get(0)` usa un lock no interrumpible, incumpliendo el contrato de interrupción/timeout bajo contención | `CloseableConflatedChannel.java` 69-99, 156-178 |
| m3 | Menor | El iterador de `CloseableChannel` puede emitir un elemento `null` si `get()` fue interrumpido sin cerrar el canal | `CloseableChannel.java` 43-79 |
| m4 | Menor | El bucle de `POISON` puede dejar varios `POISON` en una cola acotada (desperdicio y posible bloqueo de `offer`) | `CloseableBufferedChannel.java` 321-324 |
| m5 | Menor | `get()` después de cerrado puede quedar esperando a `activeWriters == 0`; un escritor detenido en `fireSignals()` retrasa indefinidamente al lector | `CloseableBufferedChannel.java` 271-298 |

---

## Hallazgos críticos

### C1. `POISON` se inserta antes de que terminen los escritores en vuelo (buffered/unbuffered)

**Ubicación:** `CloseableBufferedChannel.java` 108-120, 162-200, 300-345.

`close(timeout)` hace, en este orden:

1. `closed = true` bajo `lock`;
2. captura e interrumpe a los escritores registrados;
3. inserta un `POISON` por cada lector contado en `gets`;
4. recién entonces espera a que `activeWriters` llegue a cero.

Un escritor que ya pasó el chequeo `if (closed)` puede seguir dentro de `queue.put(value)` y depositar el valor **después** del `POISON`. Como `get()` hace:

```java
Object item = queue.take();
return item == POISON ? null : (E) item;
```

el consumidor recibe `null` al llegar al `POISON` y el valor real queda detrás, sin que nadie lo lea. `close()` puede devolver `true` y aun así se perdió un valor desde el punto de vista del consumidor que ya terminó.

Además, la ruta rápida de `put(value, timeout, unit)` con `timeout == 0` (líneas 117-120) **no incrementa `activeWriters` ni vuelve a comprobar `closed` bajo `lock`**:

```java
if (timeout == 0)
{
    return fireIf(queue.offer(value));
}
```

Por eso `close()` puede observar `activeWriters == 0`, insertar el `POISON`, retornar `true`, y aun así un `put(..., 0, unit)` concurrente puede encolar su valor después del `POISON`. Es la misma pérdida de datos por una vía todavía más difícil de detectar.

**Impacto:** pérdida silenciosa de datos, cierre reportado como exitoso, consumidores que terminan antes de tiempo y valores que quedan en memoria sin posibilidad de ser leídos por el consumidor que ya salió.

**Evidencia (`PoisonOrder`):**

```text
buffered close=true
buffered first get = null
buffered second get= committed-after-poison
```

**Recomendación:** no usar el `POISON` como marcador insertado antes de que los escritores terminen. Opciones:

- esperar a que `activeWriters == 0` (con deadline) y recién entonces insertar el marcador de cierre;
- registrar también la ruta `timeout == 0` y revalidar `closed` dentro del mismo lock que usa el cierre;
- o separar el estado de cierre del contenido de la cola, de modo que `get()` drene todos los valores y el cierre no compita con escritores.

---

### C2. `POISON` antes del write-lock en `CloseableUnlimitedChannel`

**Ubicación:** `CloseableUnlimitedChannel.java` 126-164, 253-282.

El cierre hace:

```java
closed = true;
...
int count = gets.get();
for (int i = 0; i < count; i++)
{
    queue.put(POISON);
}

boolean acquired = rwLock.writeLock().tryLock(timeout, unit);
```

Los `put()` toman el **read-lock** y comprueban `closed` dentro de él. Un escritor que ya tiene el read-lock puede hacer su `queue.put(value)` después de que el cierre haya insertado los `POISON`, pero antes de que el cierre consiga el write-lock. Cuando el cierre obtiene el write-lock, todos los escritores terminaron; sin embargo, el valor quedó **detrás** de los `POISON`.

El lector bloqueado recibe el `POISON` y devuelve `null`. El valor queda en la cola y solo lo vería un lector posterior; el consumidor que estaba esperando en el momento del cierre lo pierde.

**Impacto:** idéntico a C1, pero en el canal ilimitado. Además, si `close(timeout)` devuelve `false`, el canal ya está marcado cerrado y los lectores ya fueron envenenados, aunque el escritor siga en vuelo.

**Evidencia (`PoisonOrder` y `UnlimitedRealRace2`):**

```text
unlimited close=true
unlimited first get = null
unlimited second get= committed-after-poison
```

```text
close(200ms) = false
blocked consumer got = null
close() = true
next get = late-value
then     = null
```

**Recomendación:** adquirir el write-lock (o un estado equivalente de “no más escritores”) **antes** de insertar los `POISON`; o hacer que `drainAfterClose()` siga drenando hasta que no haya escritores y la cola esté vacía, en lugar de detenerse en el primer `POISON`.

---

### C3. Carrera en `drainAfterClose()` de `CloseableBufferedChannel`

**Ubicación:** `CloseableBufferedChannel.java` 271-298.

`drainAfterClose()` hace:

```java
for (int i = 1;; i++)
{
    Object item = queue.poll();
    if (item != null)
    {
        return (item == POISON) ? null : (E) item;
    }

    synchronized (lock)
    {
        if (activeWriters == 0)
        {
            return null;
        }
        ...
    }
}
```

El `poll()` y la comprobación de `activeWriters` **no son atómicos**. Secuencia problemática:

1. el lector hace `queue.poll()` y obtiene `null`;
2. un escritor que ya estaba registrado hace `queue.put(value)` y luego decrementa `activeWriters` a 0;
3. el lector entra en `synchronized (lock)`, ve `activeWriters == 0` y retorna `null`;
4. el valor queda en la cola, pero el lector ya terminó.

Es una pérdida de dato para cualquier consumidor que use `null` como fin de stream (el patrón normal de estos canales). El valor solo lo vería otra llamada posterior a `get()`.

**Impacto:** el consumidor puede terminar antes de recibir un valor ya confirmado por el escritor. En pipelines con `for (E e = ch.get(); e != null; ...)` el elemento se pierde silenciosamente.

**Evidencia (`DrainRace`):**

```text
reader got = null
value still buffered = late-value
```

**Recomendación:** hacer atómica la decisión “no hay valores y no hay escritores”. Por ejemplo, que los escritores commiteen y decrementen `activeWriters` bajo el mismo lock, o que el lector, tras ver `activeWriters == 0`, vuelva a hacer `poll()` bajo el lock antes de retornar. La opción más robusta es no usar `POISON` y tener un estado de cierre separado con un contador de escritores y una condición.

---

### C4. `close(timeout)` no respeta el timeout al desbloquear lectores

**Ubicación:** `CloseableBufferedChannel.java` 300-345, especialmente 321-324.

El bucle:

```java
for (int i = 1; gets.get() > 0; i++)
{
    queue.offer(POISON, Math.min(i, 100), TimeUnit.MILLISECONDS);
}
```

no comprueba el deadline. Cada iteración puede bloquear hasta 100 ms y el bucle solo termina cuando `gets` llega a 0. El timeout se evalúa **después**, en la espera por `activeWriters` (líneas 326-342).

Consecuencias:

- `close(1, NANOSECONDS)` puede tardar cientos de milisegundos o más.
- Si `gets > 0` pero ningún lector consume el `POISON` (hilo pausado/descheduleado, hilo detenido, lector que ya abandonó `take()`, o un caso de rendezvous donde el lector aún no está parkeado), el bucle no termina nunca.
- En una cola acotada llena, cada `offer` espera hasta 100 ms aunque no haya espacio.
- `close(0, ...)` igualmente ejecuta el envenenamiento antes de retornar `false`, produciendo efectos laterales que el llamador no esperaba.

**Evidencia (`CloseTimeoutIgnored`):** el harness deja `gets = 1` sin un lector realmente parkeado y llama `close(1, NANOSECONDS)`:

```text
close(1ns) still running after 500ms = true
```

Al liberar el contador simulado, el cierre termina y devuelve `true`.

**Recomendación:** calcular un deadline absoluto y comprobarlo antes de cada `offer` y antes de cada espera. Para rendezvous, no depender de un `POISON` por cada `gets`: usar una condición/estado de cierre que despierte a todos los lectores y que `get()` consulte. Si el deadline vence, retornar `false` de inmediato y dejar el estado documentado.

---

### C5. `DuplexChannel` no es cerrable como iterador: bucle infinito de `null`

**Ubicación:** `DuplexChannel.java` 17, 44-54; `Channel.java` 77-94; `CloseableChannel.java` 43-79.

`DuplexChannel` extiende `Channel`, no `CloseableChannel`, y **no sobrescribe `iterator()`**. Por lo tanto hereda el iterador base:

```java
public boolean hasNext()
{
    return true;
}

public E next()
{
    return get();
}
```

Si el `in` del duplex es un canal cerrable y se cierra, `get()` devuelve `null` inmediatamente, pero `hasNext()` sigue devolviendo `true` y `next()` devuelve `null` para siempre. Un `for-each`, un `Stream` o un `Spliterator` sobre ese duplex no termina y genera una cantidad infinita de `null`.

**Impacto:** bucle infinito, consumo de CPU/memoria si se acumulan elementos, y violación del contrato de iteración que el resto de canales cerrables sí respetan.

**Evidencia (`DuplexIterator`):**

```text
hasNext=true next=null
hasNext=true next=null
hasNext=true next=null
hasNext=true next=null
hasNext=true next=null
yielded 5 elements after the input was closed
```

**Recomendación:** sobrescribir `iterator()` en `DuplexChannel` para usar la lógica de `CloseableChannel` cuando `in instanceof ChannelCloser`, o exponer un iterador basado en `isReadClosed()` que termine cuando `get()` devuelva `null` y la entrada esté cerrada. Añadir un test de regresión con `in` cerrado.

---

### C6. `CloseableFanoutChannel.close()` puede colgarse indefinidamente

**Ubicación:** `CloseableFanoutChannel.java` 202-244, 217-228; `put()` 96-119.

`close()` marca `closed`, interrumpe a todos los escritores registrados y luego:

```java
while (!writers.isEmpty())
{
    closeLock.wait();
}
```

No hay timeout. Si un `put()` está bloqueado en un target que **ignora `InterruptedException`** (o que tarda mucho en responder), el escritor nunca sale de `writers`; el cierre queda esperando para siempre. Los targets ni siquiera llegan a cerrarse.

Esto es especialmente peligroso porque `CloseableFanoutChannel` se documenta como una forma de propagar fin-de-datos a destinos, y sus targets pueden ser writers externos que no sigan el contrato de interrupción del paquete.

**Evidencia (`FanoutCloseHang`):** target que ignora interrupciones y espera un latch:

```text
close() still blocked after 500ms = true
close returned true
```

El cierre solo retorna después de liberar manualmente el target.

**Recomendación:** añadir una variante `close(timeout, unit)` y un deadline en la espera; no bloquear indefinidamente por escritores que no cooperan. Si se agota el timeout, retornar `false` y dejar claro que los targets pueden quedar abiertos. También conviene no interrumpir hilos de aplicación como único mecanismo de cancelación; usar un token de cancelación comprobado por el fan-out.

---

## Hallazgos medios

### M1. `addTarget()` después de `close()` deja el target abierto para siempre

**Ubicación:** `FanoutChannel.java` 66-70; `CloseableFanoutChannel.java` 231-241.

`addTarget()` está en la clase base y no comprueba el estado de cierre. Después de `fan.close()`, agregar un target:

- no recibe valores, porque `put()` ya lanza `IllegalStateException`;
- no se cierra, porque `close()` ya hizo su recorrido de targets;
- si tiene consumidores esperando en `get()`, quedan bloqueados para siempre.

La misma carrera existe si `addTarget()` ocurre concurrentemente con `close()` después de que éste tomó la instantánea de `targets`.

**Evidencia (`FanoutAddAfterClose`):**

```text
fan closed = true
late target closed = false
put rejected: closed
late target still open and will never be closed: true
```

**Recomendación:** sobrescribir `addTarget()` en `CloseableFanoutChannel`: si ya está cerrado, rechazar la operación con `IllegalStateException` o cerrar inmediatamente el target. Sincronizar `addTarget` con `close` usando `closeLock` o un estado atómico.

---

### M2. `CloseableFanoutChannel`: carrera de `closeResult`, idempotencia rota y fallos parciales sin reintento

**Ubicación:** `CloseableFanoutChannel.java` 61-67, 202-244.

`close()` hace:

```java
synchronized (closeLock)
{
    if (closed)
    {
        return closeResult;
    }
    closed = true;
    ...
}
```

pero `closeResult` se asigna **al final**, fuera del lock. Si dos hilos llaman `close()` concurrentemente:

- el primero marca `closed = true` y sale del lock para cerrar targets;
- el segundo ve `closed == true` y retorna `closeResult`, que todavía vale `false` por defecto;
- el segundo recibe `false` aunque el primero termine con éxito.

Además, si el primer cierre falla o es interrumpido, `closeResult = false`; las llamadas posteriores retornan `false` inmediatamente **sin reintentar** cerrar los targets que quedaron abiertos. La promesa de “idempotente: devuelve el mismo resultado que la primera llamada” no se cumple ni en concurrencia ni en fallo parcial.

**Recomendación:** modelar el cierre con estados (`OPEN`, `CLOSING`, `CLOSED`) y un `CompletableFuture`/condición: el primer llamador ejecuta el cierre, los demás esperan y reciben el mismo resultado. En fallo, permitir reintentar los targets pendientes o exponer explícitamente el estado parcial.

---

### M3. Fan-out secuencial: head-of-line blocking, timeout total N × timeout y entrega parcial

**Ubicación:** `FanoutChannel.java` 100-131; `CloseableFanoutChannel.java` 145-166.

El broadcast recorre los targets en orden y hace `target.put(value)` de forma secuencial. Un target lleno o lento bloquea a todos los siguientes, aunque los demás tengan capacidad. En el peor caso, un consumidor rápido queda starvation detrás de uno lento.

En la variante con timeout:

```java
if (!target.put(value, timeout, unit))
{
    return false;
}
```

el timeout se aplica **por target**, no al broadcast completo. Con N targets, el peor caso es N × timeout. Además, si el target k falla, los targets 0..k-1 ya recibieron el valor y los demás no: la operación devuelve `false` pero hubo entrega parcial; un reintento del llamador duplicaría el valor en los primeros.

**Impacto:** latencia no acotada, starvation de consumidores rápidos, entrega no atómica y posibles duplicados al reintentar.

**Recomendación:** definir explícitamente la semántica (at-most-once/at-least-once) y, si se necesita rendimiento, usar entrega paralela o colas por target. Para el timeout, calcular un deadline global y pasar a cada target solo el tiempo restante. Aislar excepciones de un target para no abortar silenciosamente el resto.

---

### M4. `Select` queda en polling pegajoso a 200 µs

**Ubicación:** `Select.java` 150-163, 219-222.

Durante el registro:

```java
if (!ch.register(signal) || ch.needsPolling())
{
    polling = true;
}
```

`polling` es una bandera global del `select`, no por canal, y nunca se vuelve a calcular. Si en el conjunto hay un canal rendezvous (o un `DuplexChannel` cuyo `in` no es `Channel`) que requiere polling, **todos** los canales se escanean cada 200 µs para siempre:

```java
if (polling)
{
    wait = Math.min(wait, POLL_NANOS);
}
```

Aunque ese canal se cierre y quede exhausto, el `select` sigue despertando 5.000 veces por segundo y recorriendo los canales restantes, en lugar de bloquearse en la señal del canal buffered. Con muchos selectores esto degrada CPU y latencia de forma medible.

**Recomendación:** recalcular `polling` después de cada escaneo considerando solo canales no exhaustos; si ya no queda ningún canal que requiera polling, usar la espera larga de la señal. También se puede registrar por canal si necesita polling y desregistrar/ignorar los cerrados.

---

### M5. `Select` puede descartar un canal con cierre en curso y perder el valor que se commiteará después

**Ubicación:** `Select.java` 186-198; `CloseableBufferedChannel.java` 300-345.

El escaneo hace:

```java
boolean wasClosed = ch.isReadClosed();
T value = ch.get(0, TimeUnit.NANOSECONDS);
if (value != null)
{
    return SelectResult.value(i, value);
}
if (wasClosed)
{
    exhausted[i] = true;
    open--;
}
```

`isReadClosed()` es verdadero en cuanto `close()` pone `closed = true`, **antes** de que el cierre espere a los escritores. Si en ese momento la cola está vacía porque el escritor aún no commiteó, `get(0)` devuelve `null`, el canal se marca `exhausted` y `open--`. Después el escritor commitea su valor, pero `Select` ya no vuelve a mirar ese canal: el valor se pierde para el `select` (y, combinado con C1/C2, también para el consumidor que se detuvo en el `POISON`).

**Impacto:** un `Select` puede terminar con `CLOSED` o quedarse esperando otros canales mientras un valor ya confirmado por el escritor queda huérfano.

**Recomendación:** no marcar `exhausted` hasta que el canal esté realmente drenado y sin escritores en vuelo. Exponer un estado `terminated` distinto de `closed`, o hacer que `get(0)` indique “cerrado pero con escritores pendientes” para que el `select` reintente.

---

### M6. `close()` no es idempotente de verdad en buffered/unlimited

**Ubicación:** `CloseableBufferedChannel.java` 300-359; `CloseableUnlimitedChannel.java` 253-296.

`ChannelCloser` documenta que `close()` es idempotente y devuelve el mismo valor que la primera llamada. En estas implementaciones no hay un resultado almacenado ni una guarda de “cierre en curso”:

- dos hilos pueden ejecutar el envenenamiento y la espera a la vez;
- cada llamada adicional puede insertar más `POISON` y alterar el estado de la cola;
- una primera llamada con timeout puede devolver `false` y una segunda, más tarde, devolver `true` (o al revés);
- `close(0, unit)` ejecuta efectos laterales antes de devolver `false`.

El test `CloseableUnlimitedChannelCloseTest` incluso espera que un reintento devuelva `false` mientras el lock está tomado y `true` después; eso es razonable como operación reintentable, pero contradice la documentación de “mismo resultado que la primera llamada”. Hay que decidir y documentar una semántica única.

**Recomendación:** implementar una máquina de estados de cierre con resultado único y condición de finalización; el primer llamador ejecuta, los demás esperan. Si se quiere semántica de reintento, documentarla y no llamarla idempotente.

---

### M7. `close()` interrumpe escritores no bloqueados y puede dejar el flag de interrupción activo

**Ubicación:** `CloseableBufferedChannel.java` 305-316; `CloseableFanoutChannel.java` 211-228.

El cierre interrumpe a **todos** los hilos registrados como escritores, no solo a los que están bloqueados. Un escritor puede estar:

- después de que `queue.put(value)` ya retornó, dentro de `fireSignals()`;
- en un `FanoutChannel` haciendo un `target.put()` no bloqueante;
- a punto de salir del broadcast.

En esos casos no hay ningún `InterruptedException` que consuma la interrupción; el `put()` retorna normalmente y el hilo llamador queda con el **flag de interrupción activo**. La siguiente operación bloqueante del llamador puede abortar de forma inesperada, y el contrato del paquete dice que la interrupción se consume, no se re-señala.

**Impacto:** interrupciones espurias en hilos de aplicación, fallos intermitentes difíciles de reproducir y posibles cierres en cascada si el llamador usa un canal cerrable.

**Recomendación:** interrumpir solo a escritores que se sabe que están parkeados, o usar un token de cancelación que el escritor compruebe en puntos seguros. Si el cierre inyecta la interrupción y el `put()` no la observó, limpiar el flag antes de retornar (o no interrumpir en absoluto y cerrar la cola para desbloquear).

---

### M8. `join()` retorna antes de que el cierre termine de verdad

**Ubicación:** `CloseableBufferedChannel.java` 371-380; `CloseableUnlimitedChannel.java` 307-317; `CloseableFanoutChannel.java` 265-274.

`join()` espera únicamente a que `closed` sea verdadero. Pero `close()` pone `closed = true` al principio:

- en buffered/unlimited, antes de esperar a los escritores y de que los lectores drenen;
- en fanout, antes de esperar a los escritores y de cerrar los targets.

Un consumidor que usa `join()` como “el productor terminó, puedo hacer teardown” puede observar que el cierre aún está en curso, que hay escritores en vuelo o que los targets siguen abiertos. En el caso de fanout, `join()` puede retornar mientras `close()` todavía está bloqueado en el `while (!writers.isEmpty())`.

**Recomendación:** separar `closing` de `closed` (completamente cerrado). `join()` debe esperar a que el cierre haya terminado y los targets/escritores estén resueltos, no solo a la bandera inicial.

---

## Hallazgos menores

### m1. Busy-spin en `CloseableUnlimitedChannel.drainAfterClose()`

**Ubicación:** `CloseableUnlimitedChannel.java` 234-251.

```java
while (true)
{
    Object item = queue.poll();
    if (item != null)
    {
        return (item == POISON) ? null : (E) item;
    }

    if (rwLock.getReadLockCount() == 0)
    {
        return null;
    }
    Thread.yield();
}
```

Mientras un escritor mantenga el read-lock (por ejemplo, descheduleado o detenido), el lector gira consumiendo CPU sin backoff ni espera. No es una pérdida de datos como C3, pero puede degradar el sistema y consumir un core. Usar una condición/`readLock` con espera o un contador de escritores con `wait/notify`.

### m2. `CloseableConflatedChannel`: timeout e interrupción incompletos

**Ubicación:** `CloseableConflatedChannel.java` 69-99, 156-178.

- `put(value, timeout, unit)` usa `lock.lockInterruptibly()` sin deadline: si el lock está tomado, puede esperar más que `timeout` (aunque las secciones críticas suelen ser cortas).
- `get(0, unit)` usa `lock.lock()` no interrumpible y no llama a `handleInterruptedException()` ni a `closeIfCloseable()` si el hilo es interrumpido mientras adquiere el lock. El flag queda activo y el canal no se cierra, a diferencia del resto de operaciones.

### m3. El iterador de `CloseableChannel` puede emitir `null`

**Ubicación:** `CloseableChannel.java` 43-79.

Si `get()` devuelve `null` por una interrupción mientras el canal **no** está cerrado, `hasNext()` guarda `null`, devuelve `true`, y `next()` entrega `null` como si fuera un elemento. El Javadoc lo menciona, pero rompe la suposición de que los canales no transportan `null` y puede provocar bucles en consumidores que solo terminan con `null` + `isClosed()`.

### m4. El bucle de `POISON` puede dejar marcadores extra en la cola

**Ubicación:** `CloseableBufferedChannel.java` 321-324.

El bucle inserta un `POISON` por cada `gets` observado, pero `gets` puede decrementarse entre iteraciones. Los `POISON` sobrantes quedan en la cola acotada, ocupan capacidad y pueden hacer que un `offer` posterior espere. No es tan grave como C1/C4, pero conviene insertar exactamente un marcador de cierre o usar un estado fuera de la cola.

### m5. `get()` tras el cierre puede quedar esperando a un escritor detenido

**Ubicación:** `CloseableBufferedChannel.java` 271-298.

`drainAfterClose()` no retorna hasta que `activeWriters == 0`. Si un escritor quedó detenido dentro de `fireSignals()` (por ejemplo, por un `SelectSignal` bloqueado o un hilo pausado), el lector puede quedar esperando indefinidamente después de que el canal ya está cerrado. El cierre con timeout mitiga parcialmente esto, pero `get()` no tiene deadline propio.

---

## Reproducciones

Los harnesses están en `/tmp/opencode/channel-audit/` y se compilaron contra:

```text
nut-base-commons/build/classes/java/main
```

Comando usado:

```bash
CP=/home/franci/git/nut/nut-base/nut-base-commons/build/classes/java/main:/tmp/opencode/channel-audit
java -cp "$CP" PoisonOrder
java -cp "$CP" DrainRace
java -cp "$CP" UnlimitedRealRace2
java -cp "$CP" DuplexIterator
java -cp "$CP" FanoutAddAfterClose
java -cp "$CP" FanoutCloseHang
java -cp "$CP" CloseTimeoutIgnored
```

Salidas observadas:

```text
=== PoisonOrder ===
buffered close=true
buffered first get = null
buffered second get= committed-after-poison
unlimited close=true
unlimited first get = null
unlimited second get= committed-after-poison

=== DrainRace ===
reader got = null
value still buffered = late-value

=== UnlimitedRealRace2 ===
close(200ms) = false
blocked consumer got = null
close() = true
next get = late-value
then     = null

=== DuplexIterator ===
hasNext=true next=null
hasNext=true next=null
hasNext=true next=null
hasNext=true next=null
hasNext=true next=null
yielded 5 elements after the input was closed

=== FanoutAddAfterClose ===
fan closed = true
late target closed = false
put rejected: closed
late target still open and will never be closed: true

=== FanoutCloseHang ===
close() still blocked after 500ms = true
close returned true

=== CloseTimeoutIgnored ===
close(1ns) still running after 500ms = true
returned = true, result = true
```

Notas sobre los harnesses:

- `PoisonOrder` usa reflexión para insertar el `POISON` en el mismo punto en que lo haría `close()`, y luego un `put()` en vuelo. Representa de forma determinista el interleaving de C1/C2.
- `DrainRace` usa reflexión para forzar el interleaving entre `poll()` y la comprobación de `activeWriters` (C3), que de otro modo es difícil de reproducir de forma fiable.
- `UnlimitedRealRace2` bloquea un lector real en `get()`, simula un escritor que ya tiene el read-lock y llama `close(200ms)`; reproduce C2 de punta a punta.
- `CloseTimeoutIgnored` fuerza `gets = 1` sin un lector realmente parkeado para demostrar que `close(timeout)` no tiene deadline en el bucle de `POISON` (C4).
- `FanoutCloseHang` usa un target que ignora deliberadamente `InterruptedException` (C6).

## Cobertura de tests existente y huecos

Los tests actuales cubren cierres simples, drenaje de valores ya encolados, idempotencia secuencial sin escritores bloqueados y fairness básica de `Select`. No cubren, entre otros:

- el orden `POISON` vs. escritor en vuelo (C1/C2);
- la carrera `poll()`/`activeWriters` de `drainAfterClose()` (C3);
- `close(timeout)` con `gets > 0` y sin un lector parkeado (C4);
- iteración de `DuplexChannel` con entrada cerrada (C5);
- cierre de fan-out con un target que ignora interrupciones (C6);
- `addTarget()` posterior o concurrente con `close()` (M1);
- dos `close()` concurrentes (M2/M6);
- `Select` cuando un canal de polling se cierra (M4) o cuando un cierre está en curso (M5).

## Orden de corrección sugerido

1. **C1/C2/C3:** rediseñar el cierre para que el marcador de fin de stream se inserte solo cuando no pueda haber más escritores, y hacer atómica la comprobación “cola vacía + sin escritores”. Sin esto, cualquier otra corrección deja pérdida de datos.
2. **C4/C6:** deadlines reales en todos los caminos de cierre; ningún `wait`/`offer` sin comprobación de deadline. Añadir `close(timeout)` al fan-out.
3. **C5:** iterador cerrable en `DuplexChannel`.
4. **M1/M2/M3:** estado de cierre del fan-out, targets añadidos después del cierre y semántica de entrega/timeout.
5. **M4/M5:** polling adaptativo y estado de “realmente drenado” para `Select`.
6. **M6/M7/M8 y menores:** idempotencia, interrupciones selectivas y `join()` de cierre completo.

---

**Conclusión:** el paquete tiene una base de concurrencia cuidada (volatile/locks, contadores de lectores/escritores, señales para `Select`), pero el protocolo de cierre actual no es seguro frente a escritores en vuelo. Los fallos C1–C3 pueden producir pérdida silenciosa de datos en producción; C4 y C6 pueden colgar el apagado; C5 puede colgar cualquier iteración sobre un duplex. Se recomienda tratar el cierre como una barrera explícita (sin `POISON` insertado antes de la barrera) y añadir tests de interleaving como los harnesses de esta auditoría.
