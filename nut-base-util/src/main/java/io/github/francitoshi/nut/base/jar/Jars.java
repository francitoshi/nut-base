/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.jar;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.io.File;
import java.util.jar.JarFile;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.security.CodeSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.jar.JarEntry;

public class Jars
{
    //THIS METHOD DOES NOT WORK IF REPRODUCIBLE BUILD IS USED
    public static FileTime getClassBuildFileTime(Class<?> clss) throws IOException
    {
        String classFile = clss.getName().replace('.', '/') + ".class";
        CodeSource src = clss.getProtectionDomain().getCodeSource();
        if (src == null)
        {
            return null;
        }

        try
        {
            File location = new File(src.getLocation().toURI());

            if (location.isDirectory())
            {
                // clases sueltas en un directorio (ej: target/classes en desarrollo)
                File classPath = new File(location, classFile);
                return classPath.exists() ? Files.getLastModifiedTime(classPath.toPath()) : null;
            }
            else
            {
                // dentro de un .jar
                try (JarFile jar = new JarFile(location))
                {
            JarEntry entry = jar.getJarEntry(classFile);
            return entry != null ? entry.getLastModifiedTime() : null;
        }
            }
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }    
    
    //THIS METHOD DOES NOT WORK IF REPRODUCIBLE BUILD IS USED
    public static Instant getClassBuildInstant(Class<?> clss) throws IOException
    {
        FileTime ft = getClassBuildFileTime(clss);
        return ft!=null ? ft.toInstant() : null;
    }
    
    //THIS METHOD DOES NOT WORK IF REPRODUCIBLE BUILD IS USED
    public static LocalDate getClassBuildLocalDate(Class<?> clss) throws IOException
    {
        FileTime ft = getClassBuildFileTime(clss);
        return ft!=null ? JavaTime.asLocalDate(ft.toInstant()) : null;
    }
    
    //THIS METHOD DOES NOT WORK IF REPRODUCIBLE BUILD IS USED
    public static LocalDateTime getClassBuildLocalDateTime(Class<?> clss) throws IOException
    {
        FileTime ft = getClassBuildFileTime(clss);
        return ft!=null ? JavaTime.asLocalDateTime(ft.toInstant()) : null;
    }
}
