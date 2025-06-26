/*
 *  Files.java
 *
 *  Copyright (C) 2024-2025 francitoshi@gmail.com
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

import io.nut.base.util.MimeTypes;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author franci
 */
public class FileUtils extends IO
{
            
    private static final String[][] escapeCharacters =
    {
        {
            "\\", "\\\\'"
        },
        {
            " ", "\\ "
        },
        {
            "\t", "\\\t"
        },
        {
            "\"", "\\\""
        },
        {
            "\'", "\\\'"
        },
        {
            "[", "\\["
        },
        {
            "]", "\\]"
        },
        {
            "(", "\\("
        },
        {
            ")", "\\)"
        },
        {
            "&", "\\&"
        },
    };

    public static String escape(String name)
    {
        for (String[] item : escapeCharacters)
        {
            if (!File.separator.equals(item[0]))
            {
                name = name.replace(item[0], item[1]);
            }
        }
        return name;
    }


    public static File getNoDotFile(File file) throws IOException
    {
        // "." => ""
        File absolute = file.getAbsoluteFile();
        File canonical = file.getCanonicalFile();
        if (absolute.equals(canonical))
        {
            return file;
        }

        // .../. => ...
        String name = file.getName();
        while (name.equals("..") || name.equals("."))
        {
            File parent = file.getParentFile();
            if (parent == null)
            {
                parent = file.getAbsoluteFile().getParentFile();
            }
            if (parent == null)
            {
                return canonical;
            }
            if (name.equals(".") && !canonical.equals(parent.getCanonicalFile()))
            {
                return canonical;
            }
            file = parent;
            name = file.getName();
        }
        String path = file.getPath();

        if (path.startsWith("." + File.separator) || path.startsWith(".." + File.separator))
        {
            return canonical;
        }
        if (path.contains(File.separator + "." + File.separator) || path.contains(File.separator + ".." + File.separator))
        {
            return canonical;
        }
        return file;
    }


    public static File[] getAbsoluteFile(File[] files)
    {
        File[] abs = new File[files.length];
        for (int i = 0; i < files.length; i++)
        {
            abs[i] = files[i].getAbsoluteFile();
        }
        return abs;
    }

    public static File[] toFileArray(String[] fileNames)
    {
        if (fileNames == null)
        {
            return null;
        }
        File[] files = new File[fileNames.length];
        for (int i = 0; i < files.length; i++)
        {
            files[i] = new File(fileNames[i]);
        }
        return files;
    }

    public static String[] briefPath(String[] fileNames, int max)
    {
//        comprobar si la longitud es menor del máximo, en ese caso no hacer nada
//                en caso contrario se sustituye la parte del medio por ...
        return fileNames;
    }

    public static String briefPath(String fileName, int max)
    {
        return fileName;
    }
    
    public static String getExtension(String fileName)
    {
        return getExtension(fileName, false);
    }
    public static String getExtension(String fileName, boolean dot)
    {
        int index = fileName.lastIndexOf('.');
        if(index>=0)
        {
            return fileName.substring(index+(dot?0:1));
        }
        return "";
    }
    public static String getBaseName(String fileName)
    {
        int index = fileName.lastIndexOf('.');
        if(index>=0)
        {
            return fileName.substring(0, index);
        }
        return fileName;
    }
    
    public static void writeFile(String data, File file) throws IOException
    {
        writeFile(data.getBytes(UTF8), file);
    }

    public static void writeFile(byte[] data, File file) throws IOException
    {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false)))
        {
            bos.write(data);
        }
    }

    public static String readInputStreamAsString(InputStream inputStream, String charsetName) throws IOException
    {
        try(BufferedInputStream bis = new BufferedInputStream(inputStream))
        {
            try(ByteArrayOutputStream bos = new ByteArrayOutputStream())
            {
                FileUtils.copy(bis, bos);
                byte[] contents = bos.toByteArray();
                return new String(contents, charsetName);
            }
        }
    }
    public static String readInputStreamAsString(InputStream inputStream) throws IOException
    {
        return readInputStreamAsString(inputStream, UTF8);
    }
    
    public static String readFileAsString(File file, String charsetName) throws IOException
    {
        return readInputStreamAsString(new FileInputStream(file), charsetName);
    }
    public static String readFileAsString(File file) throws IOException
    {
        return readInputStreamAsString(new FileInputStream(file), UTF8);
    }
    
    public static String digestFileName(String s, String extension, String separator, int size, boolean dropExtensions, boolean allowTildes) throws UnsupportedEncodingException
    {
        String a = "\u00E9";
        String b = "\u0065\u0301";
        System.out.println(a);
        System.out.println(b);
        System.out.println(a.equals(b));
        System.out.println(a.equalsIgnoreCase(b));
        System.out.println(a.contentEquals(b));
        System.out.println(a.codePointCount(0,1));
        System.out.println(b.codePointCount(0,2));
        
        if(dropExtensions)
        {
            String ext;
            while( (ext=MimeTypes.getMimeExtension(s))!=null)
            {
                s = s.substring(s.length()-ext.length());
            }
        }
        String[] words = s.toLowerCase().split("[^\\p{L}0-9]+");
        StringBuilder sb = new StringBuilder();
        String sep = "";
        int count = extension.length();
        for (String word : words)
        {
            count += sep.length() + word.length();
            if(count>size)
            {
                break;
            }
            sb.append(sep).append(word);
            sep = separator;
        }
        sb.append(extension);
        return sb.toString();
    }
    
}
