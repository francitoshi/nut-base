/*
 * ResourceBundles.java
 *
 * Copyright (c) 2014-2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.resources;

import io.nut.base.io.IO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class for managing {@link ResourceBundle} instances and retrieving 
 * classpath resources as strings.
 * <p>
 * This class provides methods to resolve bundles based on a priority list of locales 
 * and handles resource loading with fallback defaults.
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
    public static ResourceBundle getBundle(Class clss, Locale... locales)
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
    public static ResourceBundle getBundleStrict(Class clss, Locale... locales)
    {
        return getBundleStrict(clss.getName(), locales);
    }
    
    /**
     * Reads a resource from the classpath relative to the given class and returns it as a String.
     *
     * @param clss         the class used to locate the resource
     * @param name         the name/path of the resource
     * @param defaultValue the value to return if the resource is not found or an error occurs
     * @return the resource content as a string, or {@code defaultValue} on failure
     */
    public static String getResourceAsString(Class clss, String name, String defaultValue)
    {
        try
        {
            InputStream stream = clss.getResourceAsStream(name);
            return stream != null ? IO.readInputStreamAsString(stream) : defaultValue;
        }
        catch (IOException ex)
        {
            return defaultValue;
        }
    }

    /**
     * Reads a resource from the classpath relative to the given class and returns it as a String.
     * Returns {@code null} if the resource cannot be found or read.
     *
     * @param clss the class used to locate the resource
     * @param name the name/path of the resource
     * @return the resource content as a string, or {@code null} if it doesn't exist
     */
    public static String getResourceAsString(Class clss, String name)
    {
        return getResourceAsString(clss, name, null);
    }
    
}
