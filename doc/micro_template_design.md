# MicroTemplate Proposal and Implementation Design

I have designed and implemented an extremely fast, lightweight, and thread-safe templating engine called **`MicroTemplate`** in the `nut-base` library. It is compatible with **Java 8** and is entirely free of external dependencies.

The source code file is available at [MicroTemplate.java](file:///home/franci/git/nut/nut-base/src/main/java/io/nut/base/util/MicroTemplate.java) and the unit test suite at [MicroTemplateTest.java](file:///home/franci/git/nut/nut-base/src/test/java/io/nut/base/util/MicroTemplateTest.java).

---

## 🚀 Key Features

1. **Compilation and Segmentation (High Performance)**:
   - Instead of evaluating regular expressions or performing complex string slicing on every render call, `MicroTemplate` compiles the input string by dividing it into static (`TextSegment`) and dynamic (`VariableSegment`) segments.
   - Rendering simply iterates over these segments and appends them to a `StringBuilder`, minimizing object allocation and maximizing throughput.

2. **GC-Friendly Architecture (No Static Memory Overhead)**:
   - To prevent memory leaks and keep the lifecycle clean, **no long-lived global static caches are used**.
   - Both the reflection cache (`reflectionCache`) and the template cache (`templateCache`) are instance variables within each `MicroTemplate` engine object.
   - When a `MicroTemplate` instance is no longer referenced and gets collected by the Garbage Collector (GC), all of its internal caches are automatically freed and collected.
   - All static compilation/render methods have been removed.

3. **Flexible Cache Configuration via Builder**:
   - Using the Builder pattern, users can explicitly configure the cache type (`CacheType`) and the capacity (`capacity`) for storing pre-compiled templates and reflection metadata.
   - By default, it uses the TinyLFU cache with a capacity of 2048 elements, which can be modified via `.cache(CacheType type, int capacity)`.

4. **Dynamic and Nested Resolution (Dot Notation)**:
   - Supports nested property path traversal (e.g., `${user.address.street}`).
   - Resolves properties intelligently using a hierarchy of types:
     - **`Map`**: Bypasses reflection and queries via `.get(key)`.
     - **`List` and Arrays**: Accesses elements by index (e.g., `${items.0}`).
     - **POJOs via Reflection**: Checks for public getters `getProp()`, boolean check methods `isProp()`, matching name public methods `prop()`, or public fields (`fields`). For security reasons, package-private, protected, and private members are strictly ignored (never uses `setAccessible(true)`).

5. **Default Values**:
   - Supports defining fallbacks directly inside the placeholder using a colon `:`, for example: `${name:Guest}` or `${server.port:8080}`.

6. **Escape Support (`\`)**:
   - Allows using `\${name}` to prevent template parsing and render `${name}` literally. Handles double escape sequences correctly (e.g. `\\${name}` -> `\value`).

7. **Configurable Unresolved Handling**:
   - Allows defining a strategy via the Builder when a placeholder cannot be resolved (and lacks a default value):
     - `UnresolvedHandler.EMPTY` (default): Replaces with an empty string `""`.
     - `UnresolvedHandler.KEEP`: Retains the placeholder text intact (e.g. `${variable}`).
     - `UnresolvedHandler.THROW`: Throws a `NoSuchElementException`.
     - **Custom**: A functional interface allowing custom callback logic.

---

## 🛠️ Usage Examples

### 1. Basic Resolution (using Map)
```java
MicroTemplate engine = new MicroTemplate();
Map<String, Object> variables = new HashMap<>();
variables.put("name", "World");

String greeting = engine.resolve("Hello ${name}!", variables);
// Result: "Hello World!"
```

### 2. Default Values and Nested Properties (using POJO)
```java
MicroTemplate engine = new MicroTemplate();
User user = new User();
String output = engine.resolve("User ${name} is ${meta.role:Regular User} on port ${port:80}", user);
```

### 3. Advanced Configuration via `Builder` (with Custom Cache)
```java
MicroTemplate engine = MicroTemplate.builder()
    .prefix("{{")
    .suffix("}}")
    .throwOnUnresolved() 
    .cache(CacheType.TINY_LFU, 512) // Configures the cache capacity for this engine instance
    .build();

Map<String, Object> map = new HashMap<>();
map.put("app.name", "NutBase");

String config = engine.resolve("Config: {{app.name}} - Debug: {{debug:false}}", map);
// Result: "Config: NutBase - Debug: false"
```

### 4. Functional Resolver
```java
MicroTemplate engine = new MicroTemplate();
Function<String, String> envResolver = key -> System.getenv(key);
String path = engine.resolve("System PATH: ${PATH}", envResolver);
```

---

## ⚙️ Performance & Robustness
- **Template Caching**: Parsed template segments are stored in the instance's `templateCache`. Resolving the same template string repeatedly bypasses parsing entirely.
- **Reflection Caching**: Caches public methods and fields resolved via reflection using a sentry value to ensure fast failures on missing keys.
- **Exception Avoidance**: Bypasses trying/catching exceptions for normal flow control (like checking property existence).
- **Null Safety**: Treats `null` template input parameters consistently as empty templates, returning `""`.
- **Transparent Reflection Logging**: Any unexpected reflection exception is logged at `DEBUG` level using the `Log` facade, helping troubleshoot package-private proxy objects without interrupting execution.
- **Test Coverage**: All unit tests in `MicroTemplateTest.java` (including nested maps, lists, arrays, POJOs, delimiters, escaping, defaults, and cache configuration) pass successfully.
