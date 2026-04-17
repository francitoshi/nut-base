/*
 *  ResourceLoader.java
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

import java.util.ResourceBundle;

/**
 * High-level facade for loading classpath resources as strings.
 *
 * <p>A {@code ResourceLoader} binds a {@link Class} (used to locate resources
 * on the classpath) and an optional {@link ResourceBundle} (used to resolve
 * logical keys to physical resource names) into a single reusable object.
 * This avoids repeating those two context parameters on every call, keeping
 * call-sites concise and focused on what actually varies: the resource name or
 * key.
 *
 * <p>Two loading strategies are supported:
 * <ul>
 *   <li><b>Direct</b> — the caller supplies the exact resource path relative
 *       to the anchor class (see {@link #getString(String)} and
 *       {@link #getString(String, String)}).</li>
 *   <li><b>Key-based</b> — the caller supplies a logical key that is first
 *       resolved to a physical resource path via the bound
 *       {@link ResourceBundle} (see {@link #getStringByKey(String)} and
 *       {@link #getStringByKey(String, String)}).</li>
 * </ul>
 *
 * <p>Typical usage:
 * <pre>{@code
 * ResourceBundle bundle = ResourceBundles.getBundle(MyApp.class, Locale.getDefault());
 * ResourceLoader loader  = ResourceLoader.of(MyApp.class, bundle);
 *
 * // Direct load
 * String css = loader.getString("style.css", "");
 *
 * // Key-based load: bundle maps "help.file" -> "help_en.txt"
 * String help = loader.getStringByKey("help.file", "No help available.");
 * }</pre>
 *
 * <p>This class delegates all low-level I/O to {@link ResourceBundles} and is
 * itself not meant to be subclassed.
 */
public final class ResourceLoader
{
    /**
     * The anchor class used to locate classpath resources.
     * {@link Class#getResourceAsStream(String)} is called on this class.
     */
    private final Class<?> clss;

    /**
     * The resource bundle used to resolve logical keys to physical resource
     * paths. May be {@code null} if only direct loading is needed.
     */
    private final ResourceBundle bundle;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Private constructor — use the factory methods instead.
     *
     * @param clss   the anchor class; must not be {@code null}
     * @param bundle the resource bundle for key-based resolution; may be
     *               {@code null}
     */
    private ResourceLoader(Class<?> clss, ResourceBundle bundle)
    {
        this.clss   = clss;
        this.bundle = bundle;
    }

    /**
     * Creates a {@code ResourceLoader} bound to the given class and bundle.
     *
     * @param clss   the anchor class used to locate classpath resources;
     *               must not be {@code null}
     * @param bundle the {@link ResourceBundle} used to resolve logical keys to
     *               physical resource names; must not be {@code null} when
     *               key-based methods are used
     * @return a new {@code ResourceLoader} instance
     * @throws NullPointerException if {@code clss} is {@code null}
     */
    public static ResourceLoader of(Class<?> clss, ResourceBundle bundle)
    {
        return new ResourceLoader(clss, bundle);
    }

    /**
     * Creates a {@code ResourceLoader} bound only to the given class, with no
     * resource bundle.
     *
     * <p>Only direct-loading methods ({@link #getString(String)} and
     * {@link #getString(String, String)}) may be used on instances created
     * with this factory method; calling key-based methods will throw an
     * {@link IllegalStateException}.
     *
     * @param clss the anchor class used to locate classpath resources;
     *             must not be {@code null}
     * @return a new {@code ResourceLoader} instance with no bundle
     * @throws NullPointerException if {@code clss} is {@code null}
     */
    public static ResourceLoader of(Class<?> clss)
    {
        return new ResourceLoader(clss, null);
    }

    // -----------------------------------------------------------------------
    // Direct loading
    // -----------------------------------------------------------------------

    /**
     * Loads a classpath resource by its path relative to the anchor class and
     * returns its content as a string.
     *
     * <p>The resource is located using
     * {@link Class#getResourceAsStream(String)} on the bound class.
     *
     * @param name         the resource path relative to the anchor class
     *                     (e.g. {@code "help.txt"} or {@code "/com/example/data.json"})
     * @param defaultValue the value to return when the resource is not found
     *                     or cannot be read
     * @return the resource content as a string, or {@code defaultValue} on
     *         failure
     */
    public String getString(String name, String defaultValue)
    {
        return ResourceBundles.getResourceAsString(clss, name, defaultValue);
    }

    /**
     * Loads a classpath resource by its path relative to the anchor class and
     * returns its content as a string, or {@code null} if the resource cannot
     * be found or read.
     *
     * <p>Equivalent to {@link #getString(String, String) getString(name, null)}.
     *
     * @param name the resource path relative to the anchor class
     * @return the resource content as a string, or {@code null} on failure
     */
    public String getString(String name)
    {
        return getString(name, null);
    }

    // -----------------------------------------------------------------------
    // Key-based loading
    // -----------------------------------------------------------------------

    /**
     * Resolves a logical key to a physical resource path via the bound
     * {@link ResourceBundle} and then loads that resource as a string.
     *
     * <p>The resolution step calls {@link ResourceBundle#getString(String)}
     * with the supplied key. The returned value is then used as the resource
     * path for a direct load.
     *
     * @param key          a key defined in the bound {@link ResourceBundle}
     *                     whose value is the physical resource path
     * @param defaultValue the value to return when the key is not found in the
     *                     bundle, or when the resolved resource cannot be read
     * @return the resource content as a string, or {@code defaultValue} on
     *         failure
     * @throws IllegalStateException if this instance was created without a
     *                               {@link ResourceBundle}
     */
    public String getStringByKey(String key, String defaultValue)
    {
        if (bundle == null)
        {
            throw new IllegalStateException("No ResourceBundle bound to this ResourceLoader. Use ResourceLoader.of(Class, ResourceBundle) to enable key-based loading.");
        }
        String resolvedName = bundle.getString(key);
        return ResourceBundles.getResourceAsString(clss, resolvedName, defaultValue);
    }

    /**
     * Resolves a logical key to a physical resource path via the bound
     * {@link ResourceBundle} and then loads that resource as a string,
     * returning {@code null} if resolution or loading fails.
     *
     * <p>Equivalent to {@link #getStringByKey(String, String) getStringByKey(key, null)}.
     *
     * @param key a key defined in the bound {@link ResourceBundle} whose value
     *            is the physical resource path
     * @return the resource content as a string, or {@code null} on failure
     * @throws IllegalStateException if this instance was created without a
     *                               {@link ResourceBundle}
     */
    public String getStringByKey(String key)
    {
        return getStringByKey(key, null);
    }
}
