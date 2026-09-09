/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DenseMatrix3DTest
{

    private DenseMatrix3D<Integer> matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new DenseMatrix3D<>(new Integer[2][2][2]);
    }

    @Test
    void testDimensions()
    {
        assertEquals(3, matrix.dimensions());
    }

    @Test
    void testSetAndGet()
    {
        matrix.set(100, 0, 1, 0);
        assertEquals(100, matrix.get(0, 1, 0));
    }

    @Test
    void testRemove()
    {
        matrix.set(500, 1, 1, 1);
        Integer removed = matrix.remove(1, 1, 1);
        assertEquals(500, removed);
        assertNull(matrix.get(1, 1, 1));
    }

    @Test
    void testAnyMatch()
    {
        matrix.set(10, 0, 0, 0);
        matrix.set(20, 1, 1, 1);

        assertTrue(matrix.anyMatch(v -> v > 15));
        assertFalse(matrix.anyMatch(v -> v > 25));
    }

    @Test
    void testRemoveIf()
    {
        matrix.set(10, 0, 0, 0);
        matrix.set(20, 0, 0, 1);

        matrix.removeIf(v -> v == 10);

        assertNull(matrix.get(0, 0, 0));
        assertEquals(20, matrix.get(0, 0, 1));
    }

    @Test
    void testInvalidDimensionsThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> matrix.get(0, 0));
    }
}
