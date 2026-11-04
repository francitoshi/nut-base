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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link FixedRatiosScaleDimension}.
 */
class FixedRatiosScaleDimensionTest
{

@Test
@DisplayName("picks the largest preset ratio that fits the bounds")
void picksLargestFittingRatio()
{
    FixedRatiosScaleDimension scale = new FixedRatiosScaleDimension(500, 100);
    Dimension result = scale.convert(new Dimension(1000, 800));

    assertEquals(500, result.width);
    assertEquals(400, result.height);
}

@Test
@DisplayName("returns the original size when no preset ratio reaches the minimum")
void returnsOriginalWhenBelowMinimum()
{
    FixedRatiosScaleDimension scale = new FixedRatiosScaleDimension(500, 200);
    Dimension size = new Dimension(100, 100);

    assertSame(size, scale.convert(size));
}

@Test
@DisplayName("returns the supplied instance when the original size already fits")
void returnsOriginalWhenAlreadyFits()
{
    FixedRatiosScaleDimension scale = new FixedRatiosScaleDimension(1000, 0);
    Dimension size = new Dimension(800, 600);

    assertSame(size, scale.convert(size));
}

@Test
@DisplayName("convert(size, ratio) applies the ratio and keeps the aspect ratio")
void appliesExplicitRatio()
{
    FixedRatiosScaleDimension scale = new FixedRatiosScaleDimension(500, 100);
    Dimension result = scale.convert(new Dimension(1000, 800), 0.2);

    assertEquals(200, result.width);
    assertEquals(160, result.height);
}

@Test
@DisplayName("convert(size, ratio) returns null when the result misses the bounds")
void returnsNullWhenResultMissesBounds()
{
    FixedRatiosScaleDimension scale = new FixedRatiosScaleDimension(500, 100);

    assertNull(scale.convert(new Dimension(10000, 8000), 0.01));
    assertNull(scale.convert(new Dimension(100, 100), 0.5));
}
}
