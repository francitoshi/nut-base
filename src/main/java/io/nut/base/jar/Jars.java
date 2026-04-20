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
    
    public static Instant getClassBuildInstant(Class<?> clss) throws IOException
    {
        FileTime ft = getClassBuildFileTime(clss);
        return ft!=null ? ft.toInstant() : null;
    }
    
    public static LocalDate getClassBuildLocalDate(Class<?> clss) throws IOException
    {
        FileTime ft = getClassBuildFileTime(clss);
        return ft!=null ? JavaTime.asLocalDate(ft.toInstant()) : null;
    }
    
    public static LocalDateTime getClassBuildLocalDateTime(Class<?> clss) throws IOException
    {
        FileTime ft = getClassBuildFileTime(clss);
        return ft!=null ? JavaTime.asLocalDateTime(ft.toInstant()) : null;
    }
}
