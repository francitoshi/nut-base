/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link SimpleScaleDimension}.
 */
class SimpleScaleDimensionTest
{

@Test
@DisplayName("grows the image up to the minimum bounds preserving the aspect ratio")
void growToMinimumBounds()
{
    SimpleScaleDimension scale = new SimpleScaleDimension(400, 400);
    Dimension result = scale.convert(new Dimension(100, 100));

    assertEquals(400, result.width);
    assertEquals(400, result.height);
}

@Test
@DisplayName("grows both axes to the bounds without skewing a short image")
void growWithoutSkewing()
{
    SimpleScaleDimension scale = new SimpleScaleDimension(200, 200);
    Dimension result = scale.convert(new Dimension(100, 50));

    assertEquals(200, result.width);
    assertEquals(100, result.height);
}

@Test
@DisplayName("shrinks an oversized image to the largest size that fits the bounds")
void shrinkToMaximumBounds()
{
    SimpleScaleDimension scale = new SimpleScaleDimension(1200, 1200);
    Dimension result = scale.convert(new Dimension(2000, 1000));

    assertEquals(1200, result.width);
    assertEquals(600, result.height);
}

@Test
@DisplayName("fits a non square image into square bounds without skewing")
void fitIntoSquareBounds()
{
    SimpleScaleDimension scale = new SimpleScaleDimension(100, 100, 1200, 1200, 1.0);
    Dimension result = scale.convert(new Dimension(2000, 1000));

    assertEquals(1200, result.width);
    assertEquals(600, result.height);
}

@Test
@DisplayName("returns the supplied instance when the size is within the bounds")
void noScalingWithinBounds()
{
    SimpleScaleDimension scale = new SimpleScaleDimension(100, 200, 400, 800, 1.0);
    Dimension size = new Dimension(200, 200);

    assertSame(size, scale.convert(size));
}

@Test
@DisplayName("applies the supplied initial ratio when it is between the bounds")
void appliesInitialRatio()
{
    SimpleScaleDimension scale = new SimpleScaleDimension(0, 0, 10000, 10000, 0.5);
    Dimension result = scale.convert(new Dimension(1000, 1000));

    assertEquals(500, result.width);
    assertEquals(500, result.height);
}
}
