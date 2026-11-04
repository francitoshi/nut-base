/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.imageio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImageFormat}.
 */
class ImageFormatTest
{

@Test
@DisplayName("getFormat maps known extensions to the format identifiers")
void mapsKnownExtensions()
{
    assertEquals(ImageFormat.BMP, ImageFormat.getFormat("image.bmp"));
    assertEquals(ImageFormat.PNG, ImageFormat.getFormat("image.png"));
    assertEquals(ImageFormat.JPEG, ImageFormat.getFormat("image.jpg"));
    assertEquals(ImageFormat.JPEG, ImageFormat.getFormat("image.jpeg"));
    assertEquals(ImageFormat.GIF, ImageFormat.getFormat("image.gif"));
}

@Test
@DisplayName("getFormat is case insensitive")
void isCaseInsensitive()
{
    assertEquals(ImageFormat.PNG, ImageFormat.getFormat("IMAGE.PNG"));
    assertEquals(ImageFormat.JPEG, ImageFormat.getFormat("photo.JPG"));
}

@Test
@DisplayName("getFormat returns null for unknown extensions")
void returnsNullForUnknownExtensions()
{
    assertNull(ImageFormat.getFormat("document.txt"));
    assertNull(ImageFormat.getFormat("archive.tar.gz"));
    assertNull(ImageFormat.getFormat("noextension"));
    assertNull(ImageFormat.getFormat(""));
}

@Test
@DisplayName("getFormat(File) infers the format from the file name")
void infersFormatFromFile()
{
    assertEquals(ImageFormat.PNG, ImageFormat.getFormat(new File("/tmp/picture.png")));
    assertNull(ImageFormat.getFormat(new File("/tmp/picture.svg")));
}

@Test
@DisplayName("getImageFileFilter accepts recognized image files only")
void filtersImageFiles()
{
    assertTrue(ImageFormat.getImageFileFilter().accept(new File("a.jpg")));
    assertTrue(ImageFormat.getImageFileFilter().accept(new File("a.GIF")));
    assertFalse(ImageFormat.getImageFileFilter().accept(new File("a.txt")));
    assertFalse(ImageFormat.getImageFileFilter().accept(new File("a")));
}
}
