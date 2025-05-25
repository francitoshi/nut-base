/*
 *  IO.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.io;

import static io.nut.base.io.FileUtils.getParentFiles;
import static io.nut.base.io.FileUtils.isParentOf;
import io.nut.base.util.CharSets;
import io.nut.base.util.Sorts;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author franci
 */
public class IO
{
    private static final String TAG = IO.class.getSimpleName();

    public static final String UTF8 = CharSets.UTF8;
    
    public static final File ROOT = new File(File.separator);

    public static byte[] readAllBytes(File file) throws IOException
    {
        return Files.readAllBytes(file.toPath());
    }
    
    public static File copy(File source, File target, CopyOption... options) throws IOException
    {
        return Files.copy(source.toPath(), target.toPath(), options).toFile();
    }

    public static File move(File source, File target, CopyOption... options) throws IOException
    {
        return Files.move(source.toPath(), target.toPath(), options).toFile();
    }

    public static boolean delete(File file, boolean recursive)
    {
        if(file.isDirectory())
        {
            File[] childs = file.listFiles();
            if(recursive)
            {
                for (File item : childs)
                {
                    if (!delete(item, recursive))
                    {
                        return false;
                    }
                }
            }
            else if(childs.length>0)
            {
                return false;
            }
        }
        return file.delete();
    }
    
    public static void copy(InputStream in, OutputStream out) throws IOException
    {
        // Transfer bytes from in to out
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0)
        {
            out.write(buf, 0, r);
        }
    }
        
    public static String readInputStreamAsString(InputStream inputStream, String charsetName) throws IOException
    {
        try(BufferedInputStream bis = new BufferedInputStream(inputStream))
        {
            try(ByteArrayOutputStream bos = new ByteArrayOutputStream())
            {
                IO.copy(bis, bos);
                byte[] contents = bos.toByteArray();
                return new String(contents, charsetName);
            }
        }
    }
    public static String readInputStreamAsString(InputStream inputStream) throws IOException
    {
        return readInputStreamAsString(inputStream, UTF8);
    }    
    public static byte[] bytesFromFile(InputStream in, int readlimit) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buf[] = new byte[64 * 1024];
        int r;
        int count=0;
        while ((r = in.read(buf)) > 0 && count<readlimit)
        {
            baos.write(buf, 0, r);
            count+=r;
        }
        return baos.toByteArray();
    }
    public static byte[] bytesFromFile(InputStream in) throws IOException
    {
        return bytesFromFile(in, Integer.MAX_VALUE);
    }
    public static byte[] bytesFromFile(File file, int readlimit) throws IOException
    {
        return bytesFromFile(new FileInputStream(file), readlimit);
    }
    public static byte[] bytesFromFile(File file) throws IOException
    {
        return bytesFromFile(new FileInputStream(file), Integer.MAX_VALUE);
    }
    public static byte[] bytesFromFileGZ(InputStream in, int readlimit, boolean forceGZ) throws IOException
    {
        boolean gzip = forceGZ;
        if(!forceGZ)
        {
            in = in.markSupported()? in : new BufferedInputStream(in, 1024);
            in.mark(1024);
            try
            {
                byte[] data = new byte[1024];
                int r = in.read(data);
                GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(data, 0, r), 32);
                byte buf[] = new byte[16];
                gzis.read(buf);
                gzip = true;
            }
            catch(IOException ex)
            {
                gzip = false;
            }
            finally
            {
                in.reset();
            }
        }
        return bytesFromFile( gzip? new GZIPInputStream(in):in, readlimit);
    }
    public static byte[] bytesFromFileGZ(InputStream in, boolean detect) throws IOException
    {
        return bytesFromFileGZ(in, Integer.MAX_VALUE, detect);
    }
    public static byte[] bytesFromFileGZ(File file, int readlimit, boolean detect) throws IOException
    {
        return bytesFromFileGZ(new FileInputStream(file), readlimit, detect);
    }
    public static byte[] bytesFromFileGZ(File file, boolean detect) throws IOException
    {
        return bytesFromFileGZ(new FileInputStream(file), Integer.MAX_VALUE, detect);
    }

    public static boolean mkdirs(File dir)
    {
        if(!dir.exists())
        {
            dir.mkdirs();
            if(!dir.exists())
            {
                File parent = dir.getParentFile();
                mkdirs(parent);
                dir.mkdirs();
            }
            return true;
        }
        return false;
    }

    public static boolean isLink(String name) throws IOException
    {
        return isLink(new File(name));
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a link.
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean isLink(File file) throws IOException
    {
        return isLink(file, false);
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a link, or his path is in a link.
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean isLink(File file, boolean path) throws IOException
    {
        File c = file.getCanonicalFile();
        File a = file.getAbsoluteFile();
        boolean link = !c.equals(a);
        if (!link || path)
        {
            return link;
        }
        File ap = a.getParentFile();
        File cp = c.getParentFile();
        if (ap.equals(cp))
        {
            return true;
        }
        File apc = a.getParentFile().getCanonicalFile();
        String name = file.getName();
        File apcName = new File(apc, name);
        link = !apcName.equals(c);
        return link;
    }

    public static boolean isCyclicLink(File file) throws IOException
    {
        if (file.isDirectory() && isLink(file))
        {
            File canonical = file.getCanonicalFile();
            File absolute = file.getAbsoluteFile();
            File[] parents = getParentFiles(absolute, false);
            for (int i = 0; i < parents.length; i++)
            {
                if (canonical.equals(parents[i].getCanonicalFile()))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static String wildcard(String name)
    {
        return name.replace('\uFFFD', '?');
    }

    public static String normalize(String name)
    {
        return name.replace('\uFFFD', '.');
    }

    public static boolean isBugName(File file) throws IOException
    {
        String name = file.toString();
        String name2 = normalize(name);
        return !name.equals(name2);
    }

    private static boolean equals(File a, File b, boolean useCanonical) throws IOException
    {
        if (useCanonical)
        {
            a = a.getCanonicalFile();
            b = b.getCanonicalFile();
        }
        return a.equals(b);
    }
    
    public static File[] uniqueCopyOf(File[] list, boolean canonical, boolean mergeWithParent) throws IOException
    {
        // se eliminan los duplicados
        File[] unique = Sorts.uniqueCopyOf(list);
        for (int i = 0; i < unique.length; i++)
        {
            for (int j = i + 1; j < unique.length; j++)
            {
                if (equals(unique[j], unique[i], true))
                {
                    continue;
                }
                if (mergeWithParent)
                {
                    if (unique[j].isDirectory() && isParentOf(unique[j], unique[i], canonical))
                    {
                        unique[i] = unique[j];
                        continue;
                    }
                    if (unique[i].isDirectory() && isParentOf(unique[i], unique[j], canonical))
                    {
                        unique[j] = unique[i];
                        continue;
                    }
                }
            }
        }
        return Sorts.uniqueCopyOf(unique);
    }
    
}
