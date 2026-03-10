/*
 *  ManifestReader.java
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
package io.nut.base.jar;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads manifest attributes from the JAR file that contains a given class.
 *
 * <p>This utility is useful for retrieving build-time metadata embedded in a
 * JAR's {@code META-INF/MANIFEST.MF}, such as version, vendor, or build date.
 * It works only when the class is loaded from inside a JAR; when running from
 * an exploded classpath (e.g. during development in an IDE) the manifest is
 * not accessible and all attribute lookups will return {@code null}.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * ManifestReader reader = new ManifestReader(MyApp.class);
 * if (reader.isJarInside()) {
 *     String version = reader.getMainAttribute("Implementation-Version");
 *     String buildDate = reader.getMainAttribute(ManifestReader.BUILD_DATE);
 * }
 * }</pre>
 *
 * <h2>Gradle setup</h2>
 * <p>Add the following block to your {@code build.gradle} to embed the required
 * attributes at build time:</p>
 * <pre>{@code
 * import java.text.SimpleDateFormat
 * import java.util.Date
 *
 * def buildDate = LocalDate.now()
 *
 * jar {
 *     manifest {
 *         attributes 'Main-Class'              : 'io.francitoshi.ditdah.Main'
 *         attributes 'Implementation-Version'  : project.version
 *         attributes 'Implementation-Title'    : project.name
 *         attributes 'Implementation-Vendor'   : 'vendor@gmail.com'
 *         attributes 'Build-Date'              : buildDate
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Setup notes:</strong></p>
 * <ol>
 *   <li>Add {@code import java.time.LocalDate} at the top of your
 *       {@code build.gradle} (the snippet above uses {@code LocalDate.now()},
 *       which requires this import).</li>
 *   <li>Replace {@code 'io.francitoshi.ditdah.Main'} with the fully-qualified
 *       name of your own main class.</li>
 *   <li>{@code project.version} is read from {@code gradle.properties} or a
 *       {@code version = '1.0.0'} declaration in {@code build.gradle}.</li>
 *   <li>After building ({@code ./gradlew jar}), verify the manifest with:
 *       <pre>{@code unzip -p build/libs/your-app.jar META-INF/MANIFEST.MF}</pre>
 *   </li>
 * </ol>
 */
public class ManifestReader
{
    
    /**
     * Manifest attribute key for the build date ({@value}).
     *
     * <p>Used as the attribute name both when writing the manifest in Gradle
     * and when reading it via {@link #getMainAttribute(String)}.</p>
     */
    public static final String BUILD_DATE = "Build-Date";
        
    /** {@code true} when the class was loaded from inside a JAR file. */
    private final boolean jarInside;

    /** The parsed {@link Manifest} from the enclosing JAR, or {@code null}. */
    private final Manifest manifest;

    /** The main attributes section of {@link #manifest}, or {@code null}. */
    private final Attributes mainAttributes;

    /**
     * Constructs a {@code ManifestReader} for the JAR that contains the
     * given class.
     *
     * <p>If the class is not loaded from a JAR (e.g. exploded classpath),
     * {@link #isJarInside()} will return {@code false} and
     * {@link #getMainAttribute(String)} will always return {@code null}.</p>
     *
     * @param clss the class whose enclosing JAR manifest should be read;
     *             must not be {@code null}
     */
    public ManifestReader(Class clss)
    {
        boolean jarIn = false;
        Manifest m = null;
        Attributes attr = null;
        try
        {
            URL classUrl = clss.getResource(clss.getSimpleName() + ".class");
            
            if (classUrl != null && classUrl.toString().startsWith("jar:"))
            {
                jarIn = true;
                // open the connection as JarURLConnection
                JarURLConnection connection = (JarURLConnection) classUrl.openConnection();
                m = connection.getManifest();
                attr = m!=null ? m.getMainAttributes() : null;
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(ManifestReader.class.getName()).log(Level.SEVERE, (String) null, ex);
        }
        this.jarInside = jarIn;
        this.manifest = m;
        this.mainAttributes = attr;
    }

    /**
     * Returns {@code true} if the class passed to the constructor was loaded
     * from inside a JAR file.
     *
     * <p>When this method returns {@code false}, no manifest data is available
     * and {@link #getMainAttribute(String)} will always return {@code null}.</p>
     *
     * @return {@code true} if running from a JAR, {@code false} otherwise
     */
    public boolean isJarInside()
    {
        return jarInside;
    }
            
    /**
     * Returns the value of the named main-section attribute from the JAR
     * manifest, or {@code null} if the attribute is absent or the manifest
     * could not be read.
     *
     * <p>Common attribute names are defined as constants in this class (e.g.
     * {@link #BUILD_DATE}) or in {@link java.util.jar.Attributes.Name}.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * String version = reader.getMainAttribute("Implementation-Version");
     * String date    = reader.getMainAttribute(ManifestReader.BUILD_DATE);
     * }</pre>
     *
     * @param name the manifest attribute name; must not be {@code null}
     * @return the attribute value, or {@code null} if not found
     */
    public String getMainAttribute(String name)
    {
        return this.mainAttributes!=null ? this.mainAttributes.getValue(name) : null;
    }    
}
