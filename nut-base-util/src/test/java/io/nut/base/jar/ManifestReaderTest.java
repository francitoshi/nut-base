/*
 *  ManifestReaderTest.java
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ManifestReader")
class ManifestReaderTest
{

    // -------------------------------------------------------------------------
    // Scenario 1: class NOT loaded from a JAR
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("when class is NOT inside a JAR")
    class NotInsideJar
    {

        /**
         * During a normal test run the test class itself is on the exploded
         * classpath, so using {@code ManifestReaderTest.class} gives us a
         * reader that is not jar-backed.
         */
        private final ManifestReader reader = new ManifestReader(ManifestReaderTest.class);

        @Test
        @DisplayName("isJarInside() returns false")
        void isJarInsideReturnsFalse()
        {
            assertFalse(reader.isJarInside(),
                    "Should not report JAR presence when running from classpath directory");
        }

        @Test
        @DisplayName("getMainAttribute() returns null for any key")
        void getMainAttributeReturnsNull()
        {
            assertNull(reader.getMainAttribute("Implementation-Version"),
                    "No manifest is available; attribute lookup must return null");
        }

        @Test
        @DisplayName("getMainAttribute() returns null for BUILD_DATE constant")
        void getMainAttributeReturnNullForBuildDate()
        {
            assertNull(reader.getMainAttribute(ManifestReader.BUILD_DATE),
                    "BUILD_DATE attribute must be null outside a JAR");
        }

        @Test
        @DisplayName("getMainAttribute() returns null for null key without throwing")
        void getMainAttributeHandlesNullKey()
        {
            // mainAttributes is null in this branch, so getValue() is never called
            assertDoesNotThrow(() -> reader.getMainAttribute(null));
            assertNull(reader.getMainAttribute(null));
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 2 & 3: class loaded from a JAR
    // -------------------------------------------------------------------------

    /**
     * Helper that creates a JAR at {@code dest} containing:
     * <ul>
     *   <li>the provided {@link Manifest}</li>
     *   <li>a stub {@code .class} entry at the path expected by
     *       {@link ManifestReader} (i.e. {@code <simpleName>.class} in the
     *       root of the JAR)</li>
     * </ul>
     */
    static File buildJar(Path dir, String jarName, Manifest manifest) throws IOException
    {
        File jar = dir.resolve(jarName).toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar), manifest))
        {
            // Add a stub class entry so ManifestReader can form a jar: URL.
            // The bytes do not need to be valid bytecode — we never load the
            // class for real; we only need the resource URL to start with "jar:".
            jos.putNextEntry(new JarEntry("Stub.class"));
            jos.write(new byte[]{ (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
            jos.closeEntry();
        }
        return jar;
    }

    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("when class IS inside a JAR with a populated manifest")
    class InsideJarWithAttributes
    {

        @TempDir
        Path tempDir;

        private ManifestReader buildReader() throws Exception
        {
            Manifest mf = new Manifest();
            Attributes attrs = mf.getMainAttributes();
            attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attrs.put(new Attributes.Name("Implementation-Version"), "2.5.1");
            attrs.put(new Attributes.Name("Implementation-Title"),   "TestApp");
            attrs.put(new Attributes.Name("Implementation-Vendor"),  "Acme Corp");
            attrs.put(new Attributes.Name(ManifestReader.BUILD_DATE), "2024-07-01");

            File jar = buildJar(tempDir, "test-with-attrs.jar", mf);

            // Load a class whose simple name is "Stub" from the JAR
            try (java.net.URLClassLoader ucl =
                    new java.net.URLClassLoader(new java.net.URL[]{ jar.toURI().toURL() },
                            ClassLoader.getSystemClassLoader()))
            {
                // We cannot truly load the stub bytecode (it's fake), so we
                // verify the jar: URL path independently and construct the
                // reader with a real class that lives in this same JAR path.
                // Instead, use a subclass-friendly trick: supply a Class whose
                // getResource returns a jar: URL pointing to our temp JAR.
                //
                // Simplest correct approach: use URLClassLoader only to obtain
                // the resource URL, then reflectively build ManifestReader.
                Class<?> stubClass = buildFakeClass(ucl, jar);
                return new ManifestReader(stubClass);
            }
        }

        /**
         * Returns a {@link Class} object whose {@code getResource} call will
         * return a {@code jar:} URL that points into {@code jar}.
         *
         * <p>We use a Proxy / anonymous class trick: we make a subclass of
         * {@link ClassLoader} that reports the JAR URL for the resource named
         * {@code "Stub.class"}, then define a minimal Class via
         * {@code defineClass} using the bytes from the JAR entry.</p>
         *
         * <p>For test simplicity we take an even simpler approach: since
         * {@link ManifestReader} only calls
         * {@code clss.getResource(clss.getSimpleName() + ".class")}, we can
         * return any class whose simple name is "Stub" and whose class-loader
         * yields a JAR URL for that resource — which is exactly what a
         * {@link java.net.URLClassLoader} over our temp JAR does, provided we
         * use the resource-lookup path rather than trying to define the class.</p>
         */
        private Class<?> buildFakeClass(java.net.URLClassLoader ucl, File jar) throws Exception
        {
            // Use an anonymous URLClassLoader subclass that always returns a
            // jar: URL for "Stub.class" without actually defining the class.
            java.net.URL jarUrl = jar.toURI().toURL();
            java.net.URL resourceUrl = new java.net.URL("jar:" + jarUrl + "!/Stub.class");

            // Create a lightweight proxy Class using Mockito-free approach:
            // reflect on ClassLoader#defineClass to make a real Class<Stub>.
            // Since we only need getResource() to work, we can use a simple
            // URLClassLoader and load the resource URL directly.
            //
            // Actually, the simplest thing: return a real Class object from the
            // *test* JAR that happens to have simpleName "Stub" — we do this by
            // defining the class inline using ASM or by using a pre-compiled
            // minimal .class file embedded as a byte array.
            //
            // For zero-dependency correctness we use the well-known minimal
            // valid .class file (Java 8 bytecode, class "Stub", no members):
            byte[] minimalClass = buildMinimalClassBytes("Stub");

            java.lang.reflect.Method define = ClassLoader.class.getDeclaredMethod(
                    "defineClass", String.class, byte[].class, int.class, int.class);
            define.setAccessible(true);
            return (Class<?>) define.invoke(ucl, "Stub", minimalClass, 0, minimalClass.length);
        }

        /**
         * Builds a minimal valid Java class file for a public class with the
         * given simple name (no package, no superclass body, Java 8 major
         * version 52).
         */
        private byte[] buildMinimalClassBytes(String className)
        {
            // Constant pool entries for a class that extends java.lang.Object
            // with no fields, methods, or attributes.
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);
            try
            {
                dos.writeInt(0xCAFEBABE);          // magic
                dos.writeShort(0);                 // minor version
                dos.writeShort(52);                // major version (Java 8)
                // constant pool (count = 5, indices 1-4)
                dos.writeShort(5);
                // #1 Class -> #2
                dos.writeByte(7); dos.writeShort(2);
                // #2 Utf8 className
                dos.writeByte(1);
                byte[] nameBytes = className.getBytes("UTF-8");
                dos.writeShort(nameBytes.length); dos.write(nameBytes);
                // #3 Class -> #4 (java/lang/Object)
                dos.writeByte(7); dos.writeShort(4);
                // #4 Utf8 "java/lang/Object"
                dos.writeByte(1);
                byte[] objBytes = "java/lang/Object".getBytes("UTF-8");
                dos.writeShort(objBytes.length); dos.write(objBytes);

                dos.writeShort(0x0021);            // ACC_PUBLIC | ACC_SUPER
                dos.writeShort(1);                 // this class  -> #1
                dos.writeShort(3);                 // super class -> #3
                dos.writeShort(0);                 // interfaces count
                dos.writeShort(0);                 // fields count
                dos.writeShort(0);                 // methods count
                dos.writeShort(0);                 // attributes count
                dos.flush();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            return bos.toByteArray();
        }

        @Test
        @DisplayName("isJarInside() returns true")
        void isJarInsideReturnsTrue() throws Exception
        {
            assertTrue(buildReader().isJarInside());
        }

        @Test
        @DisplayName("getMainAttribute() returns correct Implementation-Version")
        void getImplementationVersion() throws Exception
        {
            assertEquals("2.5.1", buildReader().getMainAttribute("Implementation-Version"));
        }

        @Test
        @DisplayName("getMainAttribute() returns correct Implementation-Title")
        void getImplementationTitle() throws Exception
        {
            assertEquals("TestApp", buildReader().getMainAttribute("Implementation-Title"));
        }

        @Test
        @DisplayName("getMainAttribute() returns correct Implementation-Vendor")
        void getImplementationVendor() throws Exception
        {
            assertEquals("Acme Corp", buildReader().getMainAttribute("Implementation-Vendor"));
        }

        @Test
        @DisplayName("getMainAttribute() returns correct Build-Date via BUILD_DATE constant")
        void getBuildDate() throws Exception
        {
            assertEquals("2024-07-01", buildReader().getMainAttribute(ManifestReader.BUILD_DATE));
        }

        @Test
        @DisplayName("getMainAttribute() returns null for an absent attribute")
        void getAbsentAttributeReturnsNull() throws Exception
        {
            assertNull(buildReader().getMainAttribute("No-Such-Attribute"));
        }
    }

    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("when class IS inside a JAR with an empty manifest")
    class InsideJarWithEmptyManifest
    {

        @TempDir
        Path tempDir;

        private ManifestReader buildReader() throws Exception
        {
            // A manifest with only MANIFEST_VERSION (mandatory); no custom attrs
            Manifest mf = new Manifest();
            mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            File jar = buildJar(tempDir, "test-empty-manifest.jar", mf);

            try (java.net.URLClassLoader ucl =
                    new java.net.URLClassLoader(new java.net.URL[]{ jar.toURI().toURL() },
                            ClassLoader.getSystemClassLoader()))
            {
                // Reuse the same minimal-class trick
                InsideJarWithAttributes helper = new InsideJarWithAttributes();
                Class<?> stubClass = helper.buildFakeClass(ucl, jar);
                return new ManifestReader(stubClass);
            }
        }

        @Test
        @DisplayName("isJarInside() returns true even with empty manifest")
        void isJarInsideReturnsTrue() throws Exception
        {
            assertTrue(buildReader().isJarInside());
        }

        @Test
        @DisplayName("getMainAttribute() returns null when attribute is absent")
        void getMainAttributeReturnsNull() throws Exception
        {
            assertNull(buildReader().getMainAttribute("Implementation-Version"));
        }

        @Test
        @DisplayName("getMainAttribute() returns null for BUILD_DATE when absent")
        void getBuildDateReturnsNull() throws Exception
        {
            assertNull(buildReader().getMainAttribute(ManifestReader.BUILD_DATE));
        }
    }

    // -------------------------------------------------------------------------
    // Constant sanity check
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("BUILD_DATE constant")
    class BuildDateConstant
    {

        @Test
        @DisplayName("has expected value 'Build-Date'")
        void constantValue()
        {
            assertEquals("Build-Date", ManifestReader.BUILD_DATE);
        }
    }
}
