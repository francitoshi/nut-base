/*
 *  Jars.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
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

import io.nut.base.time.JavaTime;
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
