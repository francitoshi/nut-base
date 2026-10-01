/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import io.nut.base.util.MimeTypes;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public static String digestFileName(String s, String extension, String separator, int size, boolean dropExtensions, boolean allowTildes) throws UnsupportedEncodingException
    {
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
