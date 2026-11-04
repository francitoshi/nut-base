/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.imageio;

import io.github.francitoshi.nut.base.image.SimpleScaleDimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ScaleImage}.
 */
class ScaleImageTest
{

private static BufferedImage newImage(int width, int height)
{
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try
    {
        graphics.setColor(java.awt.Color.RED);
        graphics.fillRect(0, 0, width, height);
    }
    finally
    {
        graphics.dispose();
    }
    return image;
}

private static InputStream pngBytes(BufferedImage image) throws IOException
{
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return new ByteArrayInputStream(bytes.toByteArray());
}

@Test
@DisplayName("a null scale with color enabled is a no-op for streams")
void streamNoOpReturnsSameStream() throws IOException
{
    ScaleImage noop = new ScaleImage((io.github.francitoshi.nut.base.image.ScaleDimension)null);
    InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

    assertSame(in, noop.filter(in, "jpeg"));
}

@Test
@DisplayName("a null scale with color enabled is a no-op for images")
void imageNoOpReturnsSameImage()
{
    ScaleImage noop = new ScaleImage((io.github.francitoshi.nut.base.image.ScaleDimension)null, false);
    BufferedImage image = newImage(10, 10);

    assertSame(image, noop.filter(image));
}

@Test
@DisplayName("filter(File) copies the image verbatim when no-op and reports false")
void fileNoOpCopiesVerbatim(@TempDir Path tempDir) throws IOException
{
    Path inputPath = tempDir.resolve("in.png");
    ImageIO.write(newImage(40, 20), "png", inputPath.toFile());
    File input = inputPath.toFile();
    File output = tempDir.resolve("out.png").toFile();

    assertFalse(new ScaleImage((io.github.francitoshi.nut.base.image.ScaleDimension)null, false)
            .filter(input, output, "png"));

    BufferedImage copied = ImageIO.read(output);
    assertTrue(copied.getWidth() == 40 && copied.getHeight() == 20);
}

@Test
@DisplayName("filter(File) scales the image, writes decodable output and reports true")
void fileFilterScalesImage(@TempDir Path tempDir) throws IOException
{
    Path inputPath = tempDir.resolve("in.png");
    ImageIO.write(newImage(40, 20), "png", inputPath.toFile());
    File input = inputPath.toFile();
    File output = tempDir.resolve("out.png").toFile();

    assertTrue(new ScaleImage(20, 20).filter(input, output, "png"));

    BufferedImage scaled = ImageIO.read(output);
    assertEquals(20, scaled.getWidth());
    assertEquals(10, scaled.getHeight());
}

@Test
@DisplayName("filter(File) throws IOException when the input file does not exist")
void fileFilterThrowsOnMissingInput(@TempDir Path tempDir)
{
    File missing = tempDir.resolve("missing.png").toFile();
    File output = tempDir.resolve("out.png").toFile();

    assertThrows(IOException.class, () -> new ScaleImage(20, 20).filter(missing, output, "png"));
}

@Test
@DisplayName("filter(InputStream) reads, scales and re-encodes the image")
void streamFilterScalesImage() throws IOException
{
    InputStream in = pngBytes(newImage(100, 80));

    BufferedImage scaled = ImageIO.read(new ScaleImage(40, 40).filter(in, "png"));

    assertEquals(40, scaled.getWidth());
    assertEquals(32, scaled.getHeight());
}

@Test
@DisplayName("filter(InputStream) throws IOException when the stream is not decodable")
void streamFilterThrowsOnUndecodableData()
{
    InputStream in = new ByteArrayInputStream("<not an image>".getBytes());
    ScaleImage scaler = new ScaleImage(40, 40);

    assertThrows(IOException.class, () -> scaler.filter(in, "png"));
}

@Test
@DisplayName("filter(InputStream) throws IOException when no writer supports the format")
void streamFilterThrowsOnUnknownFormat() throws IOException
{
    InputStream in = pngBytes(newImage(100, 80));
    ScaleImage scaler = new ScaleImage(40, 40);

    assertThrows(IOException.class, () -> scaler.filter(in, "bogus"));
}

@Test
@DisplayName("filter(BufferedImage) scales without skewing to fit the bounds")
void imageFilterScalesWithoutSkewing()
{
    BufferedImage scaled = new ScaleImage(1200, 1200).filter(newImage(2000, 1000));

    assertEquals(1200, scaled.getWidth());
    assertEquals(600, scaled.getHeight());
}

@Test
@DisplayName("filter(BufferedImage) returns the same image when the size is within the bounds")
void imageFilterReturnsSameImageWithinBounds()
{
    BufferedImage image = newImage(200, 200);
    ScaleImage scaler = new ScaleImage(new SimpleScaleDimension(100, 200, 400, 800, 1.0), false);

    assertSame(image, scaler.filter(image));
}

@Test
@DisplayName("grayscale instances convert to TYPE_BYTE_GRAY keeping the size")
void imageFilterGrayscale()
{
    BufferedImage gray = new ScaleImage(true).filter(newImage(100, 80));

    assertEquals(BufferedImage.TYPE_BYTE_GRAY, gray.getType());
    assertEquals(100, gray.getWidth());
    assertEquals(80, gray.getHeight());
}
}
