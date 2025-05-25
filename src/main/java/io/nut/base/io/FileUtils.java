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
import io.nut.base.util.Sorts;
import io.nut.base.util.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author franci
 */
public class FileUtils extends IO
{
    private static final Random random = new Random();

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

    public static File createTempFile(File parent, String prefix, String suffix)
    {
        File file = new File(parent,prefix+random.nextInt(9999)+suffix);
        while(file.exists())
        {
            file = new File(parent,prefix+random.nextInt()+suffix);
        }
        return file;
    }
    public static File createTempFile(String prefix, String suffix)
    {
        return createTempFile(new File(Utils.getTmpDir()), prefix, suffix);
    }
    public static String createTempPath(String prefix, String suffix)
    {
        return createTempFile(prefix, suffix).toString();
    }

    public static File createNonExistentFile(File parent, String name, String ext)
    {
        File file = new File(parent, name+ext);
        for(int i=1;file.exists() && i<Integer.MAX_VALUE;i++)
        {
            file = new File(parent, name+"-"+i+ext);
        }
        return file;
    }

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

    public static String[] getParents(File file, boolean includeFile)
    {
        File[] items = getParentFiles(file, includeFile);
        String[] names = new String[items.length];
        for (int i = 0; i < items.length; i++)
        {
            names[i] = items[i].toString();
        }
        return names;
    }

    public static String[] getParents(File file)
    {
        return getParents(file, false);
    }

    public static File[] getParentFiles(File file, boolean includeFile)
    {
        File item = file;
        ArrayList<File> list = new ArrayList<>();
        if (includeFile)
        {
            list.add(item);
        }
        while ((item = item.getParentFile()) != null)
        {
            list.add(item);
        }
        return Sorts.reverseOf(list.toArray(new File[0]));
    }

    public static File[] getParentFiles(File file)
    {
        return getParentFiles(file, false);
    }

    public static String getCommonParent(File a, File b)
    {
        File parent = getCommonParentFile(a, b);
        if (parent != null)
        {
            return parent.toString();
        }
        return null;
    }

    public static File getCommonParentFile(File a, File b)
    {
        File[] listA = getParentFiles(a, true);
        File[] listB = getParentFiles(b, true);
        int max = Math.min(listA.length, listB.length);
        File common = null;
        for (int i = 0; i < max && listA[i].equals(listB[i]); i++)
        {
            common = listA[i];
        }
        return common;
    }
    public static String getCommonParent(File[] files)
    {
        File parent = getCommonParentFile(files);
        return parent==null?null:parent.toString();
    }
    public static File getCommonParentFile(File[] files)
    {
        if(files.length==0)
        {
            return null;
        }
        if(files.length==1)
        {
            return files[0];
        }
        File parent = files[0];
        for(int i=1;i<files.length;i++)
        {
            parent = getCommonParentFile(parent, files[i]);
        }
        return parent;
    }

    public static boolean haveCommonParent(File a, File b)
    {
        return (getCommonParentFile(a, b) != null);
    }

    public static boolean isParentOf(File parent, File child, boolean canonical) throws IOException
    {
        File pa = parent.getAbsoluteFile();
        File ca = child.getAbsoluteFile();
        if (pa.equals(getCommonParentFile(pa, ca)))
        {
            return true;
        }
        if (canonical)
        {
            File pc = parent.getCanonicalFile();
            File cc = child.getCanonicalFile();
            if (pc.equals(parent) && cc.equals(child))
            {
                return false;
            }
            return pc.equals(getCommonParentFile(pc, cc));
        }
        return false;
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
    
    public static File createTempDirectory(String prefix, String suffix) throws IOException
    {
        return createTempDirectory(prefix, suffix, null);
    }
    public static File createTempDirectory(String prefix, String suffix, File directory) throws IOException
    {
        prefix = (prefix!=null) ? prefix : "tmp-";
        suffix = (suffix!=null) ? suffix : ".tmp";
        directory = (directory!=null)?directory:new File(Utils.getTmpDir());
        File file=null;
	for(int i=0;i<Integer.MAX_VALUE;i++)
        {
            String medfix = String.format("%04x%04x",random.nextInt(0xffff),random.nextInt(0xffff));
            file=new File(directory, prefix+medfix+suffix);
            if(!file.exists())
            {
                file.mkdirs();
                break;
            }
	}
        return file;
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
