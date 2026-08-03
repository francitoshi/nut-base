# MicroTemplate User Guide and Tutorial

`MicroTemplate` is an ultra-lightweight, high-performance, thread-safe template engine designed for **Java 8** with zero external dependencies. It parses and compiles text templates dynamically, storing the compiled representation in an instance-level cache to bypass subsequent parsing steps.

This guide walks you through everything from basic usage to advanced builder configurations.

---

## 📋 Table of Contents
1. [Basic Usage](#1-basic-usage)
2. [Default Values](#2-default-values)
3. [Complex Structures & Traversal (Dot Notation)](#3-complex-structures--traversal-dot-notation)
4. [Escape Handling (Backslash `\`)](#4-escape-handling-backslash-)
5. [Advanced Configuration via `Builder`](#5-advanced-configuration-via-builder)
6. [Performance Best Practices](#6-performance-best-practices)

---

## 1. Basic Usage

To use `MicroTemplate`, you must first instantiate the engine class. This instance acts as your configured rendering engine and manages its own internal caches (both for compiled template segments and for reflection metadata).

Once you have the engine instance, call the `resolve(...)` method by passing the template string and the context.

### Quick Example with a Map:
```java
import io.nut.base.util.MicroTemplate;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 1. Create the template engine instance
        MicroTemplate engine = new MicroTemplate();

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "World");

        // 2. Resolve the template string by passing it as an argument
        String result = engine.resolve("Hello ${name}!", variables);
        System.out.println(result); // Prints: Hello World!
    }
}
```

---

## 2. Default Values

If a variable is missing from the context or resolves to `null`, you can specify a fallback default value directly inside the placeholder using a colon (`:`).

### Syntax:
* `${variableName:defaultValue}`

```java
MicroTemplate engine = new MicroTemplate();
Map<String, Object> variables = new HashMap<>();
variables.put("username", "John");
// "port" is not defined in the map

String output = engine.resolve("Connecting to ${server:localhost} on port ${port:8080}...", variables);
System.out.println(output);
// Prints: Connecting to localhost on port 8080...
```

---

## 3. Complex Structures & Traversal (Dot Notation)

`MicroTemplate` dynamically navigates nested structures, maps, lists, and arrays using dot notation (`.`).

Properties are resolved in the following priority order:
1. **Maps (`Map`)**: Looks up keys directly using `.get(key)`.
2. **Lists and Arrays**: Accesses elements by index if the token is numeric (e.g., `0`, `1`).
3. **Java Objects (POJOs) via Reflection**:
   * Looks up a public getter method: `getProp()`.
   * Looks up a public boolean check method: `isProp()`.
   * Looks up a public method matching the property name: `prop()`.
   * Looks up a public field: `prop`.
   * **Strict Security**: For safety, package-private, protected, and private fields/methods are completely ignored (does not call `setAccessible(true)`).

### Nested Property Example:
```java
// Public classes for demonstration
public class Address {
    public String getCity() { return "Barcelona"; }
}

public class User {
    public String name = "Anna";
    public Address getAddress() { return new Address(); }
}

// Resolution:
MicroTemplate engine = new MicroTemplate();
User user = new User();
String result = engine.resolve("User ${name} lives in ${address.city}", user);
System.out.println(result); // Prints: User Anna lives in Barcelona
```

### List and Array Example:
```java
MicroTemplate engine = new MicroTemplate();
Map<String, Object> context = new HashMap<>();
context.put("fruits", Arrays.asList("Apple", "Banana", "Orange"));

String result = engine.resolve("My favorite fruit is ${fruits.1}", context);
System.out.println(result); // Prints: My favorite fruit is Banana
```

---

## 4. Escape Handling (Backslash `\`)

If you want to render the literal characters `${` in the output without them being resolved as variables, escape them by prefixing a backslash (`\`).

* `\${name}` resolves to `${name}`.
* `\\${name}` escapes the backslash itself, resolving the placeholder normally (e.g., `\John`).

```java
MicroTemplate engine = new MicroTemplate();
Map<String, Object> variables = new HashMap<>();
variables.put("price", "10");

String output = engine.resolve("The format is \\${price} and the value is ${price} USD", variables);
System.out.println(output);
// Prints: The format is ${price} and the value is 10 USD
```

---

## 5. Advanced Configuration via `Builder`

If you require custom placeholders or want to configure a strict failure strategy for missing values, use the `Builder` to customize the engine.

### Builder Options:
1. **Custom Delimiters**: Override prefix and suffix delimiters (e.g., using `{{` and `}}`).
2. **Strategy for Unresolved Variables**:
   * `keepUnresolved()`: Leaves the placeholder intact (e.g. `${variable}`).
   * `throwOnUnresolved()`: Throws a `NoSuchElementException` immediately.
   * `unresolvedHandler(UnresolvedHandler)`: Registers a custom callback implementation.
3. **Cache Fine-Tuning**:
   * Customize the caching policy and capacity for reflection and template parsing using `.cache(CacheType, capacity)`.

### Builder Example:
```java
import io.nut.base.cache.CacheType;
import io.nut.base.util.MicroTemplate;

// 1. Create a configured template engine instance
MicroTemplate engine = MicroTemplate.builder()
    .prefix("{{")
    .suffix("}}")
    .throwOnUnresolved() // Fails fast if any variable is missing and lacks a default value
    .cache(CacheType.TINY_LFU, 512) // Configures the cache capacity
    .build();

// 2. Reuse the configured engine to resolve templates
Map<String, Object> map = new HashMap<>();
map.put("user.name", "Carlos");

String greeting = engine.resolve("Hello {{user.name}}!", map);
String welcome = engine.resolve("Welcome to {{system:Web}}", map);
```

---

## 6. Performance Best Practices

To get maximum performance out of `MicroTemplate`:

1. **Reuse Engine Instances**:
   * Because both template compilation caches and reflection caches reside at the instance level, you should reuse the same `MicroTemplate` instance across your application.
   * Resolving a template string for a second time will lookup the parsed segments directly from the cache, completely bypassing string parsing.
2. **Avoid Re-instantiating Engines in Loops**:
   * Instantiating new engines repeatedly negates the performance benefits of caching.
3. **Leverage the GC Lifecycle**:
   * When you want to clear memory occupied by parsed templates and reflection metadata in long-running services, simply discard your reference to the `MicroTemplate` engine instance. The Garbage Collector will clean everything up without leaving static trace leaks.
