/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import io.nut.base.cache.CacheType;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MicroTemplate")
class MicroTemplateTest
{
    // Public nested classes to allow Reflection tests on public members 
    // of public types without requiring setAccessible(true) access.
    public static class PublicProfile
    {
        public String getRole() { return "Moderator"; }
    }

    public static class PublicPerson
    {
        public String getName() { return "Charlie"; }
        public PublicProfile getProfile() { return new PublicProfile(); }
        public boolean isActive() { return true; }
    }

    public static class PublicProduct
    {
        public String sku = "PROD-123";
    }

    public static class PrivateProduct
    {
        private String sku = "PROD-123";
    }

    public static class PrivateMethodProduct
    {
        private String getSku() { return "PROD-PRIVATE-GET"; }
    }

    @Nested
    @DisplayName("Basic Placeholder Rendering")
    class BasicRendering
    {
        @Test
        @DisplayName("renders template with a single variable")
        void renderSingleVariable()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("name", "World");
            String result = engine.resolve("Hello ${name}!", map);
            assertEquals("Hello World!", result);
        }

        @Test
        @DisplayName("renders template with multiple variables")
        void renderMultipleVariables()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("firstName", "John");
            map.put("lastName", "Doe");
            String result = engine.resolve("Name: ${lastName}, ${firstName}", map);
            assertEquals("Name: Doe, John", result);
        }

        @Test
        @DisplayName("replaces missing variables with empty string by default")
        void renderMissingVariable()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("known", "present");
            String result = engine.resolve("Values: [${known}] [${missing}]", map);
            assertEquals("Values: [present] []", result);
        }

        @Test
        @DisplayName("handles null inputs gracefully")
        void handleNullInputs()
        {
            MicroTemplate engine = new MicroTemplate();
            assertEquals("", engine.resolve(null, Collections.emptyMap()));
            assertEquals("static only", engine.resolve("static only", (Map<String, ?>) null));

            // Test Builder.build() with null
            MicroTemplate builtNull = MicroTemplate.builder().build();
            assertNotNull(builtNull);
            assertEquals("", builtNull.resolve(null, Collections.emptyMap()));
        }

        @Test
        @DisplayName("treats empty or blank placeholders as unresolved")
        void handlesEmptyPlaceholders()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("name", "Alice");
            assertEquals("Hello !", engine.resolve("Hello ${}!", map));
            assertEquals("Hello !", engine.resolve("Hello ${  }!", map));
            assertEquals("Hello !", engine.resolve("Hello ${.}!", map));
            assertEquals("Hello !", engine.resolve("Hello ${name..}!", map));
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues
    {
        @Test
        @DisplayName("uses default value if variable is missing")
        void useDefaultIfMissing()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            String result = engine.resolve("Hello ${name:Guest}!", map);
            assertEquals("Hello Guest!", result);
        }

        @Test
        @DisplayName("uses default value if variable resolves to null")
        void useDefaultIfNull()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("name", null);
            String result = engine.resolve("Hello ${name:Guest}!", map);
            assertEquals("Hello Guest!", result);
        }

        @Test
        @DisplayName("uses variable value if present, ignoring default")
        void useVariableValue()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("name", "Alice");
            String result = engine.resolve("Hello ${name:Guest}!", map);
            assertEquals("Hello Alice!", result);
        }

        @Test
        @DisplayName("handles default value with colon characters")
        void defaultValueWithColons()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            String result = engine.resolve("URL: ${endpoint:http://localhost:8080}", map);
            assertEquals("URL: http://localhost:8080", result);
        }
    }

    @Nested
    @DisplayName("Escaping")
    class Escaping
    {
        @Test
        @DisplayName("escapes placeholders when prefixed with backslash")
        void escapePlaceholder()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("name", "Alice");
            String result = engine.resolve("Hello \\${name}!", map);
            assertEquals("Hello ${name}!", result);
        }

        @Test
        @DisplayName("retains backslash when not preceding a prefix")
        void retainBackslash()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> map = new HashMap<>();
            map.put("name", "Alice");
            String result = engine.resolve("Hello \\\\${name}!", map);
            // First backslash escapes the second backslash, name gets resolved
            assertEquals("Hello \\Alice!", result);
        }
    }

    @Nested
    @DisplayName("Syntax Errors")
    class SyntaxErrors
    {
        @Test
        @DisplayName("throws IllegalArgumentException for unclosed placeholder")
        void throwsOnUnclosedPlaceholder()
        {
            MicroTemplate engine = new MicroTemplate();
            assertThrows(IllegalArgumentException.class, () -> engine.resolve("Hello ${name", new HashMap<>())
            );
        }
    }

    @Nested
    @DisplayName("Custom Delimiters")
    class CustomDelimiters
    {
        @Test
        @DisplayName("supports custom prefix and suffix delimiters")
        void customPrefixAndSuffix()
        {
            MicroTemplate template = MicroTemplate.builder().prefix("{{").suffix("}}").build();
            Map<String, Object> map = new HashMap<>();
            map.put("name", "Bob");
            assertEquals("Hello Bob!", template.resolve("Hello {{name:Guest}}!", map));
            
            Map<String, Object> emptyMap = new HashMap<>();
            assertEquals("Hello Guest!", template.resolve("Hello {{name:Guest}}!", emptyMap));
        }

        @Test
        @DisplayName("throws on empty or null custom delimiters")
        void throwsOnInvalidDelimiters()
        {
            assertThrows(IllegalArgumentException.class, () -> MicroTemplate.builder().prefix(null).build());
            assertThrows(IllegalArgumentException.class, () -> MicroTemplate.builder().suffix("").build());
        }
    }

    @Nested
    @DisplayName("Custom Resolver Functions")
    class CustomResolvers
    {
        @Test
        @DisplayName("uses functional resolver for dynamic values")
        void functionalResolver()
        {
            MicroTemplate engine = new MicroTemplate();
            Function<String, String> resolver = key -> "value_of_" + key;
            String result = engine.resolve("Get: ${sample}", resolver);
            assertEquals("Get: value_of_sample", result);
        }

        @Test
        @DisplayName("falls back to default value when resolver returns null")
        void resolverNullFallback()
        {
            MicroTemplate engine = new MicroTemplate();
            Function<String, Object> resolver = key -> null;
            String result = engine.resolve("Get: ${sample:default}", resolver);
            assertEquals("Get: default", result);
        }
    }

    @Nested
    @DisplayName("Nested Property Resolution")
    class NestedPropertyResolution
    {
        @Test
        @DisplayName("resolves properties in nested maps")
        void nestedMaps()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> address = new HashMap<>();
            address.put("city", "Barcelona");

            Map<String, Object> user = new HashMap<>();
            user.put("address", address);

            Map<String, Object> context = new HashMap<>();
            context.put("user", user);

            String result = engine.resolve("City: ${user.address.city}", context);
            assertEquals("City: Barcelona", result);
        }

        @Test
        @DisplayName("resolves properties from List indexes")
        void listIndexResolution()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> context = new HashMap<>();
            context.put("items", Arrays.asList("first", "second", "third"));

            String result = engine.resolve("Item: ${items.1}", context);
            assertEquals("Item: second", result);
        }

        @Test
        @DisplayName("resolves properties from array indexes")
        void arrayIndexResolution()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> context = new HashMap<>();
            context.put("items", new String[]{"first", "second", "third"});

            String result = engine.resolve("Item: ${items.0}", context);
            assertEquals("Item: first", result);
        }

        @Test
        @DisplayName("resolves properties via Reflection on POJOs")
        void reflectionResolution()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> context = new HashMap<>();
            context.put("person", new PublicPerson());

            String result = engine.resolve("Name: ${person.name}, Role: ${person.profile.role}, Active: ${person.active}", context);
            assertEquals("Name: Charlie, Role: Moderator, Active: true", result);
        }
        
        @Test
        @DisplayName("resolves public fields directly without getters")
        void publicFieldsResolution()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> context = new HashMap<>();
            context.put("product", new PublicProduct());

            String result = engine.resolve("SKU: ${product.sku}", context);
            assertEquals("SKU: PROD-123", result);
        }

        @Test
        @DisplayName("ignores private fields without getters")
        void ignoresPrivateFields()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> context = new HashMap<>();
            context.put("product", new PrivateProduct());

            String result = engine.resolve("SKU: ${product.sku}", context);
            assertEquals("SKU: ", result);
        }

        @Test
        @DisplayName("ignores private getter methods")
        void ignoresPrivateMethods()
        {
            MicroTemplate engine = new MicroTemplate();
            Map<String, Object> context = new HashMap<>();
            context.put("product", new PrivateMethodProduct());

            String result = engine.resolve("SKU: ${product.sku}", context);
            assertEquals("SKU: ", result);
        }
    }

    @Nested
    @DisplayName("Unresolved Property Handlers")
    class UnresolvedHandlers
    {
        @Test
        @DisplayName("KEEP retains the placeholder text")
        void keepUnresolved()
        {
            MicroTemplate template = MicroTemplate.builder()
                .keepUnresolved()
                .build();

            assertEquals("Hello ${name}!", template.resolve("Hello ${name}!", new HashMap<>()));
        }

        @Test
        @DisplayName("THROW throws NoSuchElementException")
        void throwOnUnresolved()
        {
            MicroTemplate template = MicroTemplate.builder()
                .throwOnUnresolved()
                .build();

            assertThrows(NoSuchElementException.class, () -> template.resolve("Hello ${name}!", new HashMap<>()));
        }

        @Test
        @DisplayName("custom UnresolvedHandler handles missing values")
        void customUnresolvedHandler()
        {
            MicroTemplate template = MicroTemplate.builder()
                .unresolvedHandler((var, placeholder) -> "[MISSING: " + var + "]")
                .build();

            assertEquals("Hello [MISSING: name]!", template.resolve("Hello ${name}!", new HashMap<>()));
        }
    }

    @Nested
    @DisplayName("Cache Configuration")
    class CacheConfiguration
    {
        @Test
        @DisplayName("supports configuring custom CacheType and capacity")
        void customCacheConfig()
        {
            MicroTemplate template = MicroTemplate.builder()
                .cache(CacheType.TINY_LFU, 50)
                .build();

            Map<String, Object> map = new HashMap<>();
            map.put("name", "Alice");
            assertEquals("Hello Alice!", template.resolve("Hello ${name}!", map));
        }

        @Test
        @DisplayName("validates cache parameters")
        void validateCacheParams()
        {
            assertThrows(IllegalArgumentException.class, () ->
                MicroTemplate.builder().cache(null, 100)
            );
            assertThrows(IllegalArgumentException.class, () ->
                MicroTemplate.builder().cache(CacheType.TINY_LFU, 0)
            );
            assertThrows(IllegalArgumentException.class, () ->
                MicroTemplate.builder().cache(CacheType.TINY_LFU, -10)
            );
        }
    }
}
