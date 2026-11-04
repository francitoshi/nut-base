/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.imageio;

import java.io.File;
import java.io.FileFilter;

/**
 * Maps image file extensions to the format names accepted by
 * {@link javax.imageio.ImageIO#write} and filters image files.
 *
 * @author franci
 */
public class ImageFormat
{
    /** Disallow instantiation; this class only exposes static members. */
    private ImageFormat()
    {
    }

    /** BMP image format identifier. */
    public static final String BMP = "bmp";
    /** PNG image format identifier. */
    public static final String PNG = "png";
    /** JPEG image format identifier. */
    public static final String JPEG = "jpeg";
    /** GIF image format identifier. */
    public static final String GIF = "gif";
    private static final String[][] EXT_FMT =
    {
        {".bmp", BMP},
        {".png", PNG},
        {".jpg", JPEG},
        {".jpeg", JPEG},
        {".gif", GIF},
    };

    /**
     * Returns the image format identifier for a file name based on its extension.
     *
     * @param fileName the file name to inspect
     * @return the format identifier ({@code "bmp"}, {@code "png"},
     *         {@code "jpeg"} or {@code "gif"}), or {@code null} when the
     *         extension is not recognized
     */
    public static String getFormat(String fileName)
    {
        String name = fileName.toLowerCase();
        for (String[] item : EXT_FMT)
        {
            if (name.endsWith(item[0]))
            {
                return item[1];
            }
        }
        return null;
    }

    /**
     * Returns the image format identifier for a file based on its name.
     *
     * @param file the file to inspect
     * @return the format identifier, or {@code null} when the extension is not
     *         recognized
     */
    public static String getFormat(File file)
    {
        return getFormat(file.getName());
    }
    /**
     * Returns a {@link FileFilter} that accepts files with a recognized image
     * extension.
     *
     * @return a filter accepting {@code .bmp}, {@code .png}, {@code .jpg},
     *         {@code .jpeg} and {@code .gif} files
     */
    public static FileFilter getImageFileFilter()
    {
        return new FileFilter()
        {
            public boolean accept(File pathname)
            {
                return (getFormat(pathname)!=null);
            }
        };
    }
}
