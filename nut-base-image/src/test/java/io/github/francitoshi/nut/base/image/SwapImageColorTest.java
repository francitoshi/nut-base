/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SwapImageColor}.
 */
class SwapImageColorTest
{

/** A source color with easily distinguishable channels. */
private static final int SRC_RGB = new Color(255, 128, 1).getRGB();

private static BufferedImage onePixelImage()
{
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, SRC_RGB);
    return image;
}

private static void assertPixel(int op, Color expected)
{
    BufferedImage src = onePixelImage();
    BufferedImage dst = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    new SwapImageColor(src, dst, op).run();
    assertEquals(expected.getRGB(), dst.getRGB(0, 0));
}

@Test
@DisplayName("RGB leaves the color channels untouched")
void rgbOperation()
{
    assertPixel(SwapImageColor.RGB, new Color(255, 128, 1));
}

@Test
@DisplayName("GBR rotates the color channels to green, blue, red")
void gbrOperation()
{
    assertPixel(SwapImageColor.GBR, new Color(128, 1, 255));
}

@Test
@DisplayName("BRG rotates the color channels to blue, red, green")
void brgOperation()
{
    assertPixel(SwapImageColor.BRG, new Color(1, 255, 128));
}

@Test
@DisplayName("RG swaps the red and green channels")
void rgOperation()
{
    assertPixel(SwapImageColor.RG, new Color(128, 255, 1));
}

@Test
@DisplayName("RB swaps the red and blue channels")
void rbOperation()
{
    assertPixel(SwapImageColor.RB, new Color(1, 128, 255));
}

@Test
@DisplayName("GB swaps the green and blue channels")
void gbOperation()
{
    assertPixel(SwapImageColor.GB, new Color(255, 1, 128));
}

@Test
@DisplayName("R isolates the red channel and attenuates the others")
void rOperation()
{
    assertPixel(SwapImageColor.R, new Color(255, (int)(128 * 0.53), (int)(1 * 0.53)));
}

@Test
@DisplayName("G isolates the green channel and attenuates the others")
void gOperation()
{
    assertPixel(SwapImageColor.G, new Color((int)(255 * 0.53), 128, (int)(1 * 0.53)));
}

@Test
@DisplayName("B isolates the blue channel and attenuates the others")
void bOperation()
{
    assertPixel(SwapImageColor.B, new Color((int)(255 * 0.53), (int)(128 * 0.53), 1));
}

@Test
@DisplayName("an out of range operation completes without modifying the destination")
void outOfRangeOperationIsANoOp()
{
    BufferedImage src = onePixelImage();
    BufferedImage dst = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    SwapImageColor task = new SwapImageColor(src, dst, SwapImageColor.MAX + 1);
    int untouched = dst.getRGB(0, 0);

    task.run();

    assertEquals(untouched, dst.getRGB(0, 0));
    assertNull(task.getRGB());
    assertTrue(task.done.get());
}

@Test
@DisplayName("getRGB returns the symbolic name of the operation")
void getRGBReturnsOperationName()
{
    assertEquals("RGB", new SwapImageColor(onePixelImage(), onePixelImage(), SwapImageColor.RGB).getRGB());
    assertEquals("BRG", new SwapImageColor(onePixelImage(), onePixelImage(), SwapImageColor.BRG).getRGB());
    assertEquals("G", new SwapImageColor(onePixelImage(), onePixelImage(), SwapImageColor.G).getRGB());
}

@Test
@DisplayName("run marks the task as done and does not modify the source")
void runDoesNotModifySource()
{
    BufferedImage src = onePixelImage();
    BufferedImage dst = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    SwapImageColor task = new SwapImageColor(src, dst, SwapImageColor.GBR);

    task.run();

    assertTrue(task.done.get());
    assertEquals(SRC_RGB, src.getRGB(0, 0));
}
}
