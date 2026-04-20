/*
 *  I18n.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
 * Provides internationalization (i18n) support by wrapping a
 * {@link ResourceBundle} tied to a specific class and locale.
 *
 * <p>
 * Supports three loading strategies:
 * <ul>
 * <li><b>Direct</b> – reads a string value directly from the bundle
 * ({@link #getString}).</li>
 * <li><b>Resource</b> – loads a classpath resource by its literal path
 * ({@link #getResource}).</li>
 * <li><b>Indirect</b> – combines both: resolves a bundle key to a resource
 * path and then loads that resource ({@link #resolveResource}).</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * <pre>{@code
 * I18n i18n = I18n.of(MyClass.class, Locale.FRENCH);
 * String label   = i18n.getString("button.ok", "OK");
 * String content = i18n.getResource("/help/intro.txt");
 * String help    = i18n.resolveResource("help.file.key");
 * }</pre>
 */
public final class I18n
{
    /**
     * The class whose name is used to locate the {@link ResourceBundle} and
     * resources.
     */
    private final Class<?> clss;

    /**
     * The resource bundle resolved for the given class and locale.
     */
    private final ResourceBundle bundle;

    /**
     * Constructs an {@code I18n} instance for the given class and locale.
     *
     * @param clss the class used to locate the resource bundle and classpath
     * resources; must not be {@code null}
     * @param locale the locale to use when resolving the bundle, or
     * {@code null} to use the JVM default locale
     */
    private I18n(Class<?> clss, Locale locale)
    {
        this.clss = clss;
        this.bundle = locale == null ? ResourceBundle.getBundle(clss.getName())
                                     : ResourceBundle.getBundle(clss.getName(), locale);
    }

    /**
     * Creates an {@code I18n} instance for the given class using the JVM
     * default locale.
     *
     * @param clss the class used to locate the resource bundle; must not be
     * {@code null}
     * @return a new {@code I18n} instance bound to {@code clss} and the default
     * locale
     */
    public static I18n of(Class<?> clss)
    {
        return new I18n(clss, null);
    }

    /**
     * Creates an {@code I18n} instance for the given class and locale.
     *
     * @param clss the class used to locate the resource bundle; must not be
     * {@code null}
     * @param locale the locale to use when resolving the bundle; must not be
     * {@code null}
     * @return a new {@code I18n} instance bound to {@code clss} and
     * {@code locale}
     */
    public static I18n of(Class<?> clss, Locale locale)
    {
        return new I18n(clss, locale);
    }

    // -----------------------------------------------------------------------
    // Direct loading
    // -----------------------------------------------------------------------

    /**
     * Returns the string associated with {@code name} in the resource bundle,
     * or {@code defaultValue} if the key is not found or maps to {@code null}.
     *
     * @param name the bundle key to look up; must not be {@code null}
     * @param defaultValue the value to return when the key is absent or
     * {@code null}
     * @return the resolved string, or {@code defaultValue}
     */
    public String getString(String name, String defaultValue)
    {
        String value = bundle.getString(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the string associated with {@code name} in the resource bundle,
     * or {@code null} if the key is not found.
     *
     * @param name the bundle key to look up; must not be {@code null}
     * @return the resolved string, or {@code null}
     */
    public String getString(String name)
    {
        return getString(name, null);
    }

    // -----------------------------------------------------------------------
    // Resource loading
    // -----------------------------------------------------------------------

    /**
     * Loads the classpath resource at the given literal path and returns its
     * content as a string.
     *
     * <p>The resource is located via {@link Class#getResourceAsStream(String)}
     * relative to the bound class, so the same path-resolution rules apply:
     * a leading {@code /} makes the path absolute from the classpath root,
     * while a relative path is resolved against the package of the bound class.
     *
     * @param name the classpath-relative or absolute resource path;
     * must not be {@code null}
     * @param defaultValue the value to return when the resource is not found
     * or cannot be read
     * @return the resource content as a string, or {@code defaultValue} on
     * failure
     */
    public String getResource(String name, String defaultValue)
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
     * Loads the classpath resource at the given literal path and returns its
     * content as a string, or {@code null} if the resource is not found or
     * cannot be read.
     *
     * @param name the classpath-relative or absolute resource path;
     * must not be {@code null}
     * @return the resource content as a string, or {@code null} on failure
     * @see #getResource(String, String)
     */
    public String getResource(String name)
    {
        return getResource(name, null);
    }

    // -----------------------------------------------------------------------
    // Indirect loading
    // -----------------------------------------------------------------------

    /**
     * Resolves {@code unresolvedName} to a resource path via the bundle, then
     * loads and returns the content of that classpath resource as a string.
     *
     * <p>This is a two-step convenience that combines {@link #getString(String)}
     * and {@link #getResource(String, String)}:
     * <ol>
     * <li>The bundle is queried with {@code unresolvedName} to obtain a resource
     * path.</li>
     * <li>That path is passed to {@link #getResource(String, String)} to load
     * the file content.</li>
     * </ol>
     *
     * @param unresolvedName the bundle key whose value is a classpath resource
     * path; must not be {@code null}
     * @param defaultValue the value to return when the resource cannot be found
     * or read
     * @return the resource content as a string, or {@code defaultValue} on
     * failure
     */
    public String resolveResource(String unresolvedName, String defaultValue)
    {
        return getResource(getString(unresolvedName), defaultValue);
    }

    /**
     * Resolves {@code unresolvedName} to a resource path via the bundle, then
     * loads and returns the content of that classpath resource as a string, or
     * {@code null} if the resource cannot be found or read.
     *
     * @param unresolvedName the bundle key whose value is a classpath resource
     * path; must not be {@code null}
     * @return the resource content as a string, or {@code null} on failure
     * @see #resolveResource(String, String)
     */
    public String resolveResource(String unresolvedName)
    {
        return getResource(getString(unresolvedName), null);
    }

}