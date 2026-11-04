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

/**
 * Unit tests for {@link FixedScaleDimension}.
 */
class FixedScaleDimensionTest
{

@Test
@DisplayName("always returns the configured width and height")
void returnsConfiguredSize()
{
    FixedScaleDimension scale = new FixedScaleDimension(200, 100);

    assertEquals(new Dimension(200, 100), scale.convert(new Dimension(2000, 1000)));
    assertEquals(new Dimension(200, 100), scale.convert(new Dimension(10, 10)));
}

@Test
@DisplayName("square constructor yields a square dimension")
void yieldsSquareSize()
{
    FixedScaleDimension scale = new FixedScaleDimension(50);

    assertEquals(new Dimension(50, 50), scale.convert(new Dimension(1000, 500)));
}
}
