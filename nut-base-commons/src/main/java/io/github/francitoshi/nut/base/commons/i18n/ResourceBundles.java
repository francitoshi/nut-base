/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.i18n;

import io.github.francitoshi.nut.base.commons.io.IO;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class for managing {@link ResourceBundle} instances and reading classpath
 * resources whose paths are stored in resource bundle entries.
 * <p>
 * This class provides methods to resolve bundles based on a priority list of locales
 * and handles resource loading with fallback defaults. For direct reading of classpath
 * resources as strings, use {@link IO#readResourceString(Class, String, String)}.
 */
public class ResourceBundles
{
    /**
     * The root locale used as a fallback or comparison point for bundle resolution.
     */
    static final Locale ROOT = Locale.ROOT;

    /**
     * Searches for a resource bundle matching the provided locales in order of priority.
     * <p>
     * It iterates through the provided locales and returns the first bundle found that 
     * does not resolve to the {@link Locale#ROOT}. If all provided locales resolve to 
     * the root bundle, the last one retrieved is returned.
     *
     * @param baseName the base name of the resource bundle, including the fully qualified package name
     * @param locales  a varargs list of locales to check in order of preference
     * @return the resolved {@link ResourceBundle}
     */
    public static ResourceBundle getBundle(String baseName, Locale... locales)
    {
        ResourceBundle bundle = null;
        for (Locale item : locales)
        {
            bundle = ResourceBundle.getBundle(baseName, item);
            if(!ROOT.equals(bundle.getLocale()))
            {
                return bundle;
            }
        }
        return bundle;
    }
    
    /**
     * Searches for a resource bundle using the fully qualified name of the provided class.
     *
     * @param clss    the class whose name will be used as the base name
     * @param locales a varargs list of locales to check in order of preference
     * @return the resolved {@link ResourceBundle}
     * @see #getBundle(String, Locale...)
     */
    public static <T> ResourceBundle getBundle(Class<T> clss, Locale... locales)
    {
        return getBundle(clss.getName(), locales);
    }
    
    /**
     * Attempts to find a resource bundle that strictly matches one of the provided locales.
     * <p>
     * If a bundle's locale matches the requested locale exactly, it is returned immediately.
     * If no exact match is found for any of the locales, it falls back to the logic 
     * defined in {@link #getBundle(String, Locale...)}.
     *
     * @param baseName the base name of the resource bundle
     * @param locales  a varargs list of locales to check for an exact match
     * @return a {@link ResourceBundle} matching one of the locales exactly, or the result of standard resolution
     */
    public static ResourceBundle getBundleStrict(String baseName, Locale... locales)
    {
        for (Locale item : locales)
        {
            ResourceBundle bundle = ResourceBundle.getBundle(baseName, item);
            if(item.equals(bundle.getLocale()))
            {
                return bundle;
            }
        }
        return getBundle(baseName, locales);
    }
    
    /**
     * Attempts to find a resource bundle strictly matching the provided class name and locales.
     *
     * @param clss    the class whose name will be used as the base name
     * @param locales a varargs list of locales to check
     * @return the resolved {@link ResourceBundle}
     * @see #getBundleStrict(String, Locale...)
     */
    public static <T> ResourceBundle getBundleStrict(Class<T> clss, Locale... locales)
    {
        return getBundleStrict(clss.getName(), locales);
    }
    
    /**
     * Reads the classpath resource named by the given key of the provided bundle
     * and returns it as a String, using {@link IO#readResourceString(Class, String, String)}.
     *
     * @param clss         the class used to locate the resource
     * @param key         the key of the bundle entry holding the resource name/path
     * @param bundle       the bundle providing the resource name for the given key
     * @param defaultValue the value to return if the resource is not found or an error occurs
     * @return the resource content as a string, or {@code defaultValue} on failure
     * @throws MissingResourceException if no mapping for the given key is found in the bundle
     */
    public static <T> String getResourceStringIndirect(Class<T> clss, ResourceBundle bundle, String key, String defaultValue)
    {
        String name = bundle.getString(key);
        return IO.readResourceString(clss, name, defaultValue);
    }
    public static <T> String getResourceStringIndirect(Class<T> clss, ResourceBundle bundle, String key)
    {
        return getResourceStringIndirect(clss, bundle, key, null);
    }
}
