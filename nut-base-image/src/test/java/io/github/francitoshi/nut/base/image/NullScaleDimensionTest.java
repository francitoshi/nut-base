/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link NullScaleDimension}.
 */
class NullScaleDimensionTest
{

@Test
@DisplayName("returns the same instance unchanged")
void returnsSameInstance()
{
    NullScaleDimension scale = new NullScaleDimension();
    Dimension size = new Dimension(1234, 567);

    assertSame(size, scale.convert(size));
}
}
