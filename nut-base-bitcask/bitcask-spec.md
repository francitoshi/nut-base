# Especificación de formato tipo Bitcask (perfil de implementación v1)

Documento pensado para implementar un almacén clave-valor `byte[] -> byte[]` en **Java 8 puro**, sin dependencias, simple de mantener.

## 0. Estado del "estándar"

- **No existe un estándar formal** (RFC, ISO…) de Bitcask. La referencia es el paper de diseño de Basho:
  *Bitcask: A Log-Structured Hash Table for Fast Key/Value Data* (Sheehy y Smith, 2010), https://riak.com/assets/bitcask-intro.pdf
- El paper define la **estructura lógica** (entradas `crc | tstamp | ksz | valuesz | key | value`, keydir, merge, hint files), pero **no fija anchos de campo ni endianness**. Eso lo fija la implementación de referencia en Erlang (`basho/bitcask`, módulo `bitcask_fileops`), cuyo detalle conviene contrastar si quieres compatibilidad binaria.
- Este documento toma esa estructura y **fija todo lo que falta** para que dos implementaciones de este perfil lean los mismos ficheros. Los puntos donde **me aparto del original** están marcados con **[DESVÍO]**. Por eso los ficheros de este perfil **no son compatibles** con los de Riak/Erlang.

Palabras clave: **DEBE**, **NO DEBE**, **DEBERÍA** y **PUEDE** tienen el sentido habitual de RFC 2119.

## 1. Modelo

- Una base de datos es un **directorio** con ficheros de datos de solo-añadir (*append-only*), opcionalmente ficheros hint, y un fichero de bloqueo.
- Hay **un único fichero activo** (el único donde se escribe) y cero o más **ficheros inmutables**.
- Un **keydir** en memoria asocia cada clave viva a la posición de su entrada más reciente. **Todas las claves viven en RAM.**
- Operaciones: `put(k, v)`, `get(k)`, `delete(k)`, iteración de claves (sin orden) y `merge()`.
- **Un solo proceso escritor** por directorio. Dentro del proceso, acceso serializado (un `ReentrantReadWriteLock` o `synchronized`).

## 2. Convenciones de bajo nivel

| Aspecto | Valor |
|---|---|
| Endianness | **Big-endian** en todos los enteros (es el orden por defecto de `ByteBuffer`) |
| CRC | **CRC-32 IEEE 802.3** (`java.util.zip.CRC32`), guardado como entero sin signo de 32 bits |
| Enteros sin signo | `u16` y `u32` se leen con `& 0xFFFF` / `& 0xFFFFFFFFL` en Java |
| Claves y valores | Bytes opacos. Igualdad **por contenido** (en Java, envolver en `ByteBuffer.wrap` o clase propia, nunca usar `byte[]` como clave de `HashMap`) |
| Timestamp | `u32`, segundos desde epoch Unix (se desborda en 2106). Es **informativo**: el orden lo da la posición, no el timestamp |

## 3. Estructura del directorio

```
<dir>/
  bitcask.write.lock          fichero de bloqueo (FileLock exclusivo)
  <id>.bitcask.data           fichero de datos
  <id>.bitcask.hint           hint file (opcional, solo para ficheros generados por merge)
  *.tmp                       temporales de merge; se borran al abrir
```

- `<id>` es un **entero decimal sin signo de 64 bits, sin ceros a la izquierda** (regex `[1-9][0-9]*`).
- **Orden lógico de los ficheros = orden numérico de `<id>`** (no el orden alfabético de los nombres).
- El fichero activo es **siempre el de mayor `<id>`**.
- Un hint file **solo es válido junto a su fichero de datos** del mismo `<id>`.

## 4. Fichero de datos

El fichero es una **secuencia lineal de entradas**, sin cabecera de fichero ni pie, una detrás de otra. **[DESVÍO menor]**: el original tampoco tiene cabecera, pero este perfil exige además que el fichero no contenga nada más que entradas.

### 4.1 Entrada

```
offset  tamaño  campo     descripción
0       4       crc       CRC-32 de los bytes [4 .. fin de la entrada)
4       4       tstamp    u32, segundos epoch
8       2       ksz       u16, longitud de la clave, 1..65535
10      4       vsz       u32, longitud del valor, o 0xFFFFFFFF = TOMBSTONE
14      ksz     key       bytes de la clave
14+ksz  vsz     value     bytes del valor (ausente si es tombstone)
```

- Cabecera fija: **14 bytes**.
- Tamaño total de la entrada: `14 + ksz + (tombstone ? 0 : vsz)`.
- El CRC cubre **tstamp, ksz, vsz, key y value** (todo salvo los 4 bytes del propio CRC).
- `ksz` **NO DEBE** ser 0 (clave vacía prohibida).
- `vsz = 0` es válido (valor vacío, distinto de borrado).

### 4.2 Borrado (tombstone) **[DESVÍO]**

- Un borrado es una entrada con **`vsz = 0xFFFFFFFF`** y sin bytes de valor.
- El original de Basho codifica el borrado como un valor especial reservado. Aquí se usa un valor de longitud centinela para evitar colisiones con valores reales.
- Como los valores en Java no pueden superar `Integer.MAX_VALUE`, el valor máximo permitido es **`0x7FFFFFFE`** (2 147 483 646 bytes), y `vsz` entre `0x7FFFFFFF` y `0xFFFFFFFE` es **inválido** (se trata como corrupción).

### 4.3 Límites

| Elemento | Límite |
|---|---|
| Clave | 1 – 65 535 bytes |
| Valor | 0 – 2 147 483 646 bytes |
| Entrada | no puede superar `maxFileSize` salvo que sea la única del fichero |
| Fichero de datos | umbral de rotación `maxFileSize`, por defecto **1 GiB** (configurable) |

## 5. Keydir (en memoria)

Mapa `clave -> {fileId, entryOffset, entrySize, tstamp}`:

- `fileId`: `long`.
- `entryOffset`: `long`, posición del primer byte de la entrada (donde está el CRC).
- `entrySize`: `int`, tamaño total de la entrada (cabecera + clave + valor).
- `tstamp`: `int`, informativo.

Para leer un valor se lee la entrada completa de una vez (`entrySize` bytes en `entryOffset`), se verifica el CRC y se extrae el valor en `[14 + ksz, entrySize)`.

Si una clave no está en el keydir, **no existe**. El keydir es un índice denso.

## 6. Algoritmos

### 6.1 Abrir la base de datos (recuperación)

1. Crear el directorio si no existe. Abrir `bitcask.write.lock` y obtener `FileChannel.tryLock()`. Si falla, **abortar** (otro proceso la tiene abierta). Mantener el lock hasta `close()`.
2. Borrar todos los ficheros `*.tmp`.
3. Listar los `*.bitcask.data`, ordenar por `<id>` numérico ascendente.
4. Para cada fichero en ese orden, **cargar su contenido al keydir**:
   - Si existe un hint válido (sección 7.3), cargar desde el hint.
   - Si no, **escanear** el fichero de datos (6.2).
5. Aplicar las entradas **en orden de aparición** (fichero ascendente, offset ascendente). La **última gana**: un put reemplaza el keydir y un tombstone **elimina** la clave del keydir.
6. Crear un **fichero activo nuevo** con `<id> = (mayor id existente) + 1` (o `1` si no hay ninguno). **NO DEBE** reanudarse la escritura en un fichero previo: así un final truncado nunca se mezcla con datos nuevos.
7. Abrir en modo lectura los demás ficheros (se recomienda cachear los `FileChannel`).

### 6.2 Escaneo de un fichero de datos

Para cada posición `p` desde 0:

1. Si quedan menos de 14 bytes hasta el final del fichero: **cola truncada**.
2. Leer cabecera; si `ksz == 0` o `vsz` está en el rango inválido (`0x7FFFFFFF`..`0xFFFFFFFE`): **corrupto**.
3. Calcular `end = p + 14 + ksz + (tombstone ? 0 : vsz)`. Si `end` > longitud del fichero: **cola truncada**.
4. Leer la entrada completa y comprobar el CRC. Si no coincide: **corrupto**.
5. Aplicar la entrada (put o borrado) y continuar en `p = end`.

Tratamiento de los fallos:

- **Cola truncada o corrupta en el fichero con mayor id** (el último antes de este arranque): se asume que fue una escritura interrumpida. Se **trunca** el fichero a `p` (`FileChannel.truncate(p)`) y se continúa.
- **Cualquier fallo en un fichero que no es el último**: los ficheros inmutables no deberían corromperse. **Abortar la apertura** con error explícito (opcionalmente, ofrecer una opción de "recuperación forzada" que trunque igualmente).

### 6.3 `put(key, value)`

1. Validar tamaños (4.3). Construir la entrada con `tstamp = ahora`.
2. Si `tamaño del fichero activo + tamaño de la entrada > maxFileSize` **y** el fichero activo no está vacío: **rotar** (6.6).
3. Escribir la entrada al final del fichero activo (`FileChannel.write(buf, posición)` en bucle hasta consumir el buffer; una sola escritura no garantiza escribirlo todo).
4. Aplicar la política de sincronización (sección 9).
5. **Solo después de escribir**, actualizar el keydir con `{activeId, offset, size, tstamp}`.

### 6.4 `get(key)`

1. Buscar en el keydir. Si no está: devolver `null`.
2. Leer `entrySize` bytes en `entryOffset` del fichero `fileId` (lectura posicional, en bucle hasta completar).
3. Verificar el CRC. Si falla: **lanzar excepción** (corrupción), no devolver datos.
4. Verificar que `key` leída coincide con la pedida (defensa extra) y devolver el valor.

### 6.5 `delete(key)`

1. Si la clave no está en el keydir: no hacer nada (PUEDE evitar escribir tombstones inútiles).
2. Añadir una entrada tombstone (4.2) al fichero activo, con las mismas reglas de rotación y sincronización que `put`.
3. Quitar la clave del keydir.

### 6.6 Rotación del fichero activo

1. Sincronizar (`force(true)`) el fichero activo y dejarlo como **inmutable** (solo lectura).
2. Crear `<id+1>.bitcask.data` como nuevo activo.
3. Sincronizar el directorio (si la plataforma lo permite, ver sección 9).

### 6.7 Merge (compactación) **[DESVÍO: bloqueante]**

El original hace el merge en segundo plano. Este perfil lo hace **bloqueando la base de datos** (bajo el lock exclusivo). Es más simple y suficiente cuando no hay requisitos de latencia estrictos.

Precondición: se mantiene el lock del store; no hay otras lecturas ni escrituras en curso.

1. Cerrar el fichero activo actual (queda inmutable). Sea `M` el mayor `<id>` existente.
2. Crear los ficheros de salida con ids **`M+1, M+2, …`**, con extensión `.bitcask.data.tmp` (y `.bitcask.hint.tmp` para el hint).
3. Recorrer el keydir; para cada clave viva, **copiar su entrada** del fichero origen al de salida (reescribiendo la entrada con su `tstamp` original, o con el actual). Aplicar la regla de rotación por `maxFileSize` también aquí.
4. Escribir los hint files de salida (sección 7).
5. `force(true)` de todos los `.tmp`.
6. **Renombrar** los temporales a su nombre final, **primero los de datos, luego los hints**, en orden de id ascendente, con `Files.move(..., ATOMIC_MOVE)`. Sincronizar el directorio.
7. **Borrar los ficheros antiguos** (los que tenían id ≤ `M`) **de más antiguo a más reciente**. Borra cada `.data` y su `.hint`. Sincronizar el directorio.
8. Actualizar el keydir con las nuevas posiciones.
9. Abrir un nuevo fichero activo con id `(último id de salida) + 1`.

**Por qué es seguro ante un fallo en cualquier punto:**

- Antes del paso 6, los `.tmp` se ignoran y se borran al abrir.
- Entre el paso 6 y el 7 coexisten ficheros viejos y nuevos. Al recuperar, los nuevos (mayor id) ganan y contienen los mismos valores vivos, así que el resultado es correcto.
- Durante el paso 7, borrar **de más antiguo a más reciente** garantiza que nunca queda un `put` antiguo sin el tombstone que lo anulaba (si se borrara primero el fichero del tombstone, la clave "resucitaría").
- Como el merge procesa **todos** los ficheros inmutables y solo copia entradas vivas, **los tombstones se descartan** al fusionar.

## 7. Hint file

Acelera el arranque: contiene lo mismo que el fichero de datos salvo los valores. Solo se generan en el merge (no hay hint para ficheros nacidos de escrituras normales).

### 7.1 Entrada de hint

```
offset  tamaño  campo       descripción
0       4       tstamp      u32
4       2       ksz         u16, 1..65535
6       4       total_sz    u32, tamaño total de la entrada en el fichero de datos
10      8       offset      u64, posición de la entrada en el fichero de datos (no negativa)
18      ksz     key
```

- Cabecera fija: **18 bytes**.
- Un hint **NO DEBE** contener tombstones (el merge los elimina).

### 7.2 Registro final (trailer) **[DESVÍO]**

Al final de cada hint hay un registro de 18 bytes que marca que el fichero está completo:

```
tstamp   = 0
ksz      = 0                      (marca de trailer; ninguna entrada válida tiene ksz = 0)
total_sz = CRC-32 de TODOS los bytes anteriores del hint (sin incluir el trailer)
offset   = longitud esperada del fichero de datos asociado
```

### 7.3 Validez

Un hint **solo se usa** si:

1. Termina con un trailer correcto (`ksz = 0`) en los últimos 18 bytes.
2. El CRC del trailer coincide con el calculado sobre el contenido previo.
3. La longitud del fichero de datos del mismo `<id>` es igual al `offset` del trailer.

Si falla cualquiera: **ignorar el hint** y escanear el fichero de datos. Un hint inválido nunca es un error.

## 8. Concurrencia y procesos

- **Entre procesos:** `bitcask.write.lock` con `FileChannel.tryLock()`. Si no se obtiene, la apertura falla. Un segundo proceso **NO DEBE** abrir en modo lectura "sin lock" en este perfil.
- **Entre hilos:** un `ReentrantReadWriteLock`: lectores concurrentes con `get`/iteración, un único escritor para `put`/`delete`/`merge`/rotación.
- El keydir **NO DEBE** actualizarse antes de que la entrada esté escrita en el fichero (6.3, paso 5).

## 9. Durabilidad

Política configurable de sincronización:

| Política | Comportamiento |
|---|---|
| `NONE` | No llama a `force()`; la durabilidad queda en manos del SO |
| `EVERY_WRITE` | `channel.force(false)` tras cada `put`/`delete` |
| `INTERVAL(ms)` | `force(false)` periódico en segundo plano o cada N operaciones |

- `force(true)` en rotación y en cualquier paso del merge que se indique en 6.6 y 6.7.
- **Sincronizar el directorio** tras crear, renombrar o borrar ficheros: en Linux se puede abrir el directorio con `FileChannel.open(dir, READ)` y llamar `force(true)`. En Windows esa operación no es posible y se debe omitir o capturar la excepción.

## 10. Ejemplo con bytes (verificable)

Todo en hexadecimal, big-endian.

**Entrada 1:** `put("k", "hi")` con `tstamp = 1`

```
5bddf93b  00000001  0001  00000002  6b  6869
  crc      tstamp   ksz     vsz     key value
```
17 bytes en total. CRC-32 de `00000001 0001 00000002 6b 6869` = `0x5BDDF93B`.

**Entrada 2:** `delete("k")` con `tstamp = 2`

```
0143d506  00000002  0001  ffffffff  6b
  crc      tstamp   ksz   TOMBSTONE key
```
15 bytes en total.

**Hint** de un fichero que solo contiene la entrada 1 en el offset 0 (tamaño de entrada 17 = `0x11`):

```
entrada: 00000001 0001 00000011 0000000000000000 6b
trailer: 00000000 0000 fb63eb8e 0000000000000011
```
El CRC-32 de la entrada de hint (19 bytes: 18 de cabecera + 1 de clave) es `0xFB63EB8E`. El `offset` del trailer (`0x11` = 17) es la longitud del fichero de datos.

Estos vectores sirven como **test unitario** de vuestro codificador y decodificador.

## 11. Notas para Java 8

- Usa `ByteBuffer` (big-endian por defecto) y `CRC32.update(byte[], off, len)`. `getValue()` devuelve un `long` que cabe sin signo en 32 bits.
- `u16` → `buf.getShort() & 0xFFFF`. `u32` → `buf.getInt() & 0xFFFFFFFFL`, o compara `vsz == -1` (cuando leído como `int`) para detectar el tombstone.
- `FileChannel.read/write(buf, pos)` puede devolver menos bytes de los pedidos: siempre **en bucle**.
- `Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE)` para los renombrados del merge.
- **No uses** `MappedByteBuffer` para este diseño: complica la liberación de recursos y el límite de 2 GB por mapeo.
- `ByteBuffer.wrap(key)` implementa `equals`/`hashCode` por contenido, pero depende del array: haz **copia defensiva** de la clave que guardas en el keydir.

## 12. Fuera de alcance (a propósito)

- Merge en segundo plano o incremental (requiere un esquema de ids reservado para que las salidas del merge queden ordenadas antes que las escrituras nuevas).
- Múltiples procesos lectores simultáneos.
- Consultas por rango u orden de claves (el keydir es una tabla hash).
- Compresión.
- Transacciones o batches atómicos.

---

# Apéndice A. Perfil cifrado (opcional)

Para cifrado en reposo se sustituye el formato de entrada y se añade una cabecera de fichero. Solo usa primitivas del JDK 8: `AES/GCM/NoPadding`, `PBKDF2WithHmacSHA256`, `HmacSHA256` y `SecureRandom`.

## A.1 Fichero `bitcask.meta`

```
offset  tamaño  campo
0       4       magic          "BCKM"
4       1       versión        0x01
5       4       iteraciones    u32, PBKDF2 (mínimo 600 000)
9       16      salt           aleatorio, para PBKDF2
25      12      check_nonce    nonce aleatorio
37      32      check          AES-256-GCM(masterKey, check_nonce, "bitcask-check") = 16 bytes de texto + 16 de tag
```

- `masterKey = PBKDF2WithHmacSHA256(contraseña, salt, iteraciones, 256 bits)`.
- Al abrir se descifra `check`; si falla el tag, la **contraseña es incorrecta**.
- Se crea una vez, al inicializar la base de datos.

## A.2 Cabecera del fichero de datos (28 bytes)

```
offset  tamaño  campo
0       4       magic          "BCKE"
4       1       versión        0x01
5       8       file_id        u64, DEBE coincidir con el <id> del nombre
13      16      file_salt      aleatorio
```

- Subclave del fichero: `fileKey = HMAC-SHA256(masterKey, "bitcask-file" ‖ file_id ‖ file_salt)`, usada como clave AES-256.
- Las entradas empiezan en el offset 28.

## A.3 Entrada cifrada

```
offset  tamaño  campo
0       4       len            u32, longitud de (ciphertext + tag)
4       12      nonce          4 bytes a cero ‖ contador u64 big-endian
16      len     ciphertext + tag GCM (tag de 16 bytes al final)
```

- **Texto en claro** (lo que se cifra): `tstamp(4) ‖ ksz(2) ‖ vsz(4) ‖ key ‖ value`. No hay CRC: la autenticación la da el tag GCM.
- **Contador** de nonce: empieza en 0 en cada fichero y se incrementa en 1 por entrada. **NO DEBE repetirse** con la misma `fileKey` (como la `fileKey` es distinta por fichero, no hay colisión entre ficheros).
- **AAD** (datos asociados): `file_id(8) ‖ offset_de_la_entrada(8)`, ambos big-endian. Evita que se reubiquen o dupliquen entradas sin que falle el tag.
- Los tombstones se codifican igual que en 4.2 dentro del texto en claro (`vsz = 0xFFFFFFFF`).

## A.4 Recuperación y errores

- El escaneo (6.2) usa `len` para avanzar. Cola truncada = menos bytes de los que `len` promete. **Tag inválido en el último fichero y en la última entrada** → se trata como escritura interrumpida y se trunca. En cualquier otro caso es **corrupción o manipulación**: abortar.
- **NO se generan hint files** en este perfil (revelarían las claves). El arranque escanea y descifra todos los ficheros.
- El merge funciona como en 6.7, con ficheros nuevos que llevan cabecera, `file_salt` y contador propios.

## A.5 Limitaciones de seguridad que este perfil no resuelve

- Se ve el **número y el tamaño** de las entradas y la fecha de modificación de los ficheros.
- **Rollback:** un atacante que restaure una copia antigua completa del directorio no es detectado.
- La contraseña debe manejarse como `char[]` y borrarse tras derivar la clave (reducción de exposición, sin garantías en la JVM).
- Cambiar la contraseña exige reescribir los ficheros (derivar nueva `masterKey` y hacer un merge).
