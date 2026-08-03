/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import io.nut.base.cache.Cache;
import io.nut.base.cache.CacheFactory;
import io.nut.base.cache.CacheType;
import io.nut.base.logging.Log;

/**
 * A fast, lightweight, and thread-safe micro-templating utility.
 * Supports placeholder resolution from Maps, POJOs (using reflection), Lists, Arrays,
 * and custom Resolvers. Also supports default values and configurable unresolved property handling.
 * 
 * <p>Example usage:</p>
 * <pre>
 *   MicroTemplate engine = new MicroTemplate();
 *   Map&lt;String, Object&gt; context = new HashMap&lt;&gt;();
 *   context.put("name", "John");
 *   context.put("role", "Admin");
 *   
 *   String result = engine.resolve("Hello ${name}, role: ${role:Guest}", context);
 * </pre>
 *
 * @author franci
 */
public final class MicroTemplate
{
    @FunctionalInterface
    public interface UnresolvedHandler
    {
        /**
         * Invoked when a placeholder variable cannot be resolved.
         *
         * @param variableName the name/path of the variable
         * @param originalPlaceholder the original placeholder string (e.g. "${variableName}")
         * @return the replacement string, or null
         */
        String handle(String variableName, String originalPlaceholder);

        /** Replaces unresolved placeholders with an empty string. */
        UnresolvedHandler EMPTY = (var, placeholder) -> "";

        /** Keeps unresolved placeholders intact in the output. */
        UnresolvedHandler KEEP = (var, placeholder) -> placeholder;

        /** Throws a {@link NoSuchElementException} for unresolved placeholders. */
        UnresolvedHandler THROW = (var, placeholder) -> {
            throw new NoSuchElementException("Unresolved variable: " + var);
        };
    }

    private static final Log LOG = Log.of(MicroTemplate.class);

    private static final Member NOT_FOUND_SENTINEL = new Member()
    {
        @Override public Class<?> getDeclaringClass() { return null; }
        @Override public String getName() { return ""; }
        @Override public int getModifiers() { return 0; }
        @Override public boolean isSynthetic() { return false; }
    };

    private final String prefix;
    private final String suffix;
    private final UnresolvedHandler unresolvedHandler;
    private final Cache<String, Member> reflectionCache;
    private final Cache<String, List<Segment>> templateCache;

    /**
     * Constructs a new {@code MicroTemplate} engine with default settings:
     * prefix {@code "${"}, suffix {@code "}"}, empty unresolved handler,
     * and TinyLFU cache of capacity 2048.
     */
    public MicroTemplate()
    {
        this(CacheType.TINY_LFU, 2048);
    }

    /**
     * Constructs a new {@code MicroTemplate} engine with custom cache settings.
     *
     * @param cacheType the cache implementation to use
     * @param cacheCapacity the cache capacity
     */
    public MicroTemplate(CacheType cacheType, int cacheCapacity)
    {
        this("${", "}", UnresolvedHandler.EMPTY, cacheType, cacheCapacity);
    }

    private MicroTemplate(String prefix, String suffix, UnresolvedHandler unresolvedHandler, CacheType cacheType, int cacheCapacity)
    {
        this.prefix = prefix;
        this.suffix = suffix;
        this.unresolvedHandler = unresolvedHandler;
        this.reflectionCache = CacheFactory.<String, Member>getInstance(cacheType, cacheCapacity).synchronizedCache();
        this.templateCache = CacheFactory.<String, List<Segment>>getInstance(cacheType, cacheCapacity).synchronizedCache();
    }

    /**
     * Creates a new builder for configuring a {@link MicroTemplate} engine.
     *
     * @return a new builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Resolves the template string using the provided {@link Map} context.
     *
     * @param template the template string to resolve
     * @param variables the variables map
     * @return the resolved string
     */
    public String resolve(String template, Map<String, ?> variables)
    {
        return resolve(template, (Object) variables);
    }

    /**
     * Resolves the template string using the provided resolver function.
     *
     * @param template the template string to resolve
     * @param resolver the resolver function
     * @return the resolved string
     */
    public String resolve(String template, Function<String, ?> resolver)
    {
        return resolve(template, (Object) resolver);
    }

    /**
     * Resolves the template string using the provided context object (POJO, Map, or custom resolver).
     *
     * @param template the template string to resolve
     * @param context the context object
     * @return the resolved string
     */
    public String resolve(String template, Object context)
    {
        if (template == null)
        {
            return "";
        }

        List<Segment> segments = templateCache.get(template);
        if (segments == null)
        {
            segments = parse(template, prefix, suffix);
            templateCache.put(template, segments);
        }

        StringBuilder sb = new StringBuilder(template.length() + 32);
        for (Segment segment : segments)
        {
            segment.appendTo(sb, context, unresolvedHandler, reflectionCache);
        }
        return sb.toString();
    }

    private static List<Segment> parse(String template, String prefix, String suffix)
    {
        List<Segment> list = new ArrayList<>();
        if (template == null)
        {
            return list;
        }

        int len = template.length();
        int i = 0;
        StringBuilder textBuf = new StringBuilder();

        while (i < len)
        {
            // Handle escape characters
            if (template.charAt(i) == '\\')
            {
                if (i + 1 < len)
                {
                    if (template.charAt(i + 1) == '\\')
                    {
                        textBuf.append('\\');
                        i += 2;
                        continue;
                    }
                    else if (template.startsWith(prefix, i + 1))
                    {
                        textBuf.append(prefix);
                        i += 1 + prefix.length();
                        continue;
                    }
                }
                // Single backslash not followed by another backslash or the prefix
                textBuf.append('\\');
                i++;
                continue;
            }

            if (template.startsWith(prefix, i))
            {
                if (textBuf.length() > 0)
                {
                    list.add(new TextSegment(textBuf.toString()));
                    textBuf.setLength(0);
                }

                int startOfVar = i + prefix.length();
                int endOfVar = template.indexOf(suffix, startOfVar);
                if (endOfVar == -1)
                {
                    throw new IllegalArgumentException("Unclosed placeholder starting at index " + i);
                }

                String placeholderContent = template.substring(startOfVar, endOfVar);
                String varPath = "";
                String defaultValue = null;

                int colonIndex = placeholderContent.indexOf(':');
                if (colonIndex != -1)
                {
                    varPath = placeholderContent.substring(0, colonIndex).trim();
                    defaultValue = placeholderContent.substring(colonIndex + 1);
                }
                else
                {
                    varPath = placeholderContent.trim();
                }

                list.add(new VariableSegment(varPath, defaultValue, prefix + placeholderContent + suffix));
                i = endOfVar + suffix.length();
            }
            else
            {
                textBuf.append(template.charAt(i));
                i++;
            }
        }

        if (textBuf.length() > 0)
        {
            list.add(new TextSegment(textBuf.toString()));
        }

        return list;
    }

    private static Object resolvePath(Object context, String[] pathParts, Cache<String, Member> reflectionCache)
    {
        if (pathParts.length == 0)
        {
            return null;
        }
        Object current = context;
        for (String part : pathParts)
        {
            if (current == null || part.isEmpty())
            {
                return null;
            }
            current = resolveProperty(current, part, reflectionCache);
        }
        return current;
    }

    private static Object resolveProperty(Object obj, String key, Cache<String, Member> reflectionCache)
    {
        if (obj instanceof Map)
        {
            return ((Map<?, ?>) obj).get(key);
        }
        if (obj instanceof List)
        {
            try
            {
                int index = Integer.parseInt(key);
                List<?> list = (List<?>) obj;
                if (index >= 0 && index < list.size())
                {
                    return list.get(index);
                }
            }
            catch (NumberFormatException e)
            {
                // Fall back to reflection
            }
        }
        if (obj.getClass().isArray())
        {
            try
            {
                int index = Integer.parseInt(key);
                int length = java.lang.reflect.Array.getLength(obj);
                if (index >= 0 && index < length)
                {
                    return java.lang.reflect.Array.get(obj, index);
                }
            }
            catch (NumberFormatException e)
            {
                // Fall back to reflection
            }
        }

        Class<?> clazz = obj.getClass();
        String cacheKey = clazz.getName() + "#" + key;
        Member member = reflectionCache.get(cacheKey);

        if (member == null)
        {
            member = findMember(clazz, key);
            if (member != null)
            {
                reflectionCache.put(cacheKey, member);
            }
            else
            {
                reflectionCache.put(cacheKey, NOT_FOUND_SENTINEL);
            }
        }

        if (member == NOT_FOUND_SENTINEL)
        {
            return null;
        }

        try
        {
            if (member instanceof Method)
            {
                return ((Method) member).invoke(obj);
            }
            else if (member instanceof Field)
            {
                return ((Field) member).get(obj);
            }
        }
        catch (Exception e)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Failed to resolve property '" + key + "' on object of type " + clazz.getName(), e);
            }
        }

        return null;
    }

    private static Member findMember(Class<?> clazz, String key)
    {
        String capitalized = key.substring(0, 1).toUpperCase(Locale.ENGLISH) + key.substring(1);
        String getMethodName = "get" + capitalized;
        String isMethodName = "is" + capitalized;

        // 1. Try public getMethodName()
        try
        {
            return clazz.getMethod(getMethodName);
        }
        catch (NoSuchMethodException e)
        {
            // Ignore
        }

        // 2. Try public isMethodName()
        try
        {
            return clazz.getMethod(isMethodName);
        }
        catch (NoSuchMethodException e)
        {
            // Ignore
        }

        // 3. Try public key()
        try
        {
            return clazz.getMethod(key);
        }
        catch (NoSuchMethodException e)
        {
            // Ignore
        }

        // 4. Try public field
        try
        {
            return clazz.getField(key);
        }
        catch (NoSuchFieldException e)
        {
            // Ignore
        }

        return null;
    }

    private interface Segment
    {
        void appendTo(StringBuilder sb, Object context, UnresolvedHandler unresolvedHandler, Cache<String, Member> reflectionCache);
    }

    private static final class TextSegment implements Segment
    {
        private final String text;

        TextSegment(String text)
        {
            this.text = text;
        }

        @Override
        public void appendTo(StringBuilder sb, Object context, UnresolvedHandler unresolvedHandler, Cache<String, Member> reflectionCache)
        {
            sb.append(text);
        }
    }

    private static final class VariableSegment implements Segment
    {
        private final String varPath;
        private final String defaultValue;
        private final String originalPlaceholder;
        private final String[] pathParts;

        VariableSegment(String varPath, String defaultValue, String originalPlaceholder)
        {
            this.varPath = varPath;
            this.defaultValue = defaultValue;
            this.originalPlaceholder = originalPlaceholder;
            this.pathParts = varPath.split("\\.", -1);
        }

        @Override
        public void appendTo(StringBuilder sb, Object context, UnresolvedHandler unresolvedHandler, Cache<String, Member> reflectionCache)
        {
            Object resolved = null;
            if (context != null)
            {
                resolved = resolveValue(context, reflectionCache);
            }

            if (resolved != null)
            {
                sb.append(resolved.toString());
            }
            else if (defaultValue != null)
            {
                sb.append(defaultValue);
            }
            else
            {
                String fallback = unresolvedHandler.handle(varPath, originalPlaceholder);
                if (fallback != null)
                {
                    sb.append(fallback);
                }
            }
        }

        private Object resolveValue(Object context, Cache<String, Member> reflectionCache)
        {
            if (context instanceof Function)
            {
                @SuppressWarnings("unchecked")
                Function<String, ?> resolver = (Function<String, ?>) context;
                Object val = resolver.apply(varPath);
                if (val != null)
                {
                    return val;
                }

                // If path has dot, try extracting the first part and resolving remaining parts
                int dotIndex = varPath.indexOf('.');
                if (dotIndex != -1)
                {
                    String rootKey = varPath.substring(0, dotIndex);
                    Object rootVal = resolver.apply(rootKey);
                    if (rootVal != null)
                    {
                        String[] remainingPath = varPath.substring(dotIndex + 1).split("\\.", -1);
                        return resolvePath(rootVal, remainingPath, reflectionCache);
                    }
                }
                return null;
            }
            return resolvePath(context, pathParts, reflectionCache);
        }
    }

    public static final class Builder
    {
        private String prefix = "${";
        private String suffix = "}";
        private UnresolvedHandler unresolvedHandler = UnresolvedHandler.EMPTY;
        private CacheType cacheType = CacheType.TINY_LFU;
        private int cacheCapacity = 2048;

        private Builder() {}

        public Builder prefix(String prefix)
        {
            this.prefix = prefix;
            return this;
        }

        public Builder suffix(String suffix)
        {
            this.suffix = suffix;
            return this;
        }

        public Builder unresolvedHandler(UnresolvedHandler unresolvedHandler)
        {
            this.unresolvedHandler = unresolvedHandler;
            return this;
        }

        public Builder keepUnresolved()
        {
            this.unresolvedHandler = UnresolvedHandler.KEEP;
            return this;
        }

        public Builder throwOnUnresolved()
        {
            this.unresolvedHandler = UnresolvedHandler.THROW;
            return this;
        }

        public Builder cache(CacheType cacheType, int cacheCapacity)
        {
            if (cacheType == null)
            {
                throw new IllegalArgumentException("cacheType cannot be null");
            }
            if (cacheCapacity <= 0)
            {
                throw new IllegalArgumentException("cacheCapacity must be positive");
            }
            this.cacheType = cacheType;
            this.cacheCapacity = cacheCapacity;
            return this;
        }

        public MicroTemplate build()
        {
            if (prefix == null || prefix.isEmpty())
            {
                throw new IllegalArgumentException("prefix cannot be null or empty");
            }
            if (suffix == null || suffix.isEmpty())
            {
                throw new IllegalArgumentException("suffix cannot be null or empty");
            }
            return new MicroTemplate(prefix, suffix, unresolvedHandler, cacheType, cacheCapacity);
        }
    }
}
