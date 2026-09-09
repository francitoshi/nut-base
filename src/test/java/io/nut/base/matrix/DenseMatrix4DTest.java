/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DenseMatrix4DTest
{

    private DenseMatrix4D<Double> matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new DenseMatrix4D<>(new Double[2][2][2][2]);
    }

    @Test
    void testDimensions()
    {
        // Note: The current implementation has a bug and returns 3. 
        // This test expects the correct behavior (4).
        assertEquals(4, matrix.dimensions());
    }

    @Test
    void testSetAndGet()
    {
        matrix.set(3.14, 1, 0, 1, 0);
        assertEquals(3.14, matrix.get(1, 0, 1, 0));
    }

    @Test
    void testSetAndGetVarargs()
    {
        int[] indices =
        {
            0, 1, 0, 1
        };
        matrix.set(2.71, indices);
        assertEquals(2.71, matrix.get(indices));
    }

    @Test
    void testRemove()
    {
        matrix.set(1.1, 0, 0, 0, 0);
        Double removed = matrix.remove(0, 0, 0, 0);
        assertEquals(1.1, removed);
        assertNull(matrix.get(0, 0, 0, 0));
    }

    @Test
    void testAllMatchVacuousTruth()
    {
        // An empty matrix should return true for allMatch
        assertTrue(matrix.allMatch(v -> v > 0));
    }

    @Test
    void testClear()
    {
        matrix.set(1.0, 0, 0, 0, 0);
        matrix.set(2.0, 1, 1, 1, 1);
        matrix.clear();

        assertNull(matrix.get(0, 0, 0, 0));
        assertNull(matrix.get(1, 1, 1, 1));
    }

    @Test
    void testOutOfBoundsReturnsNull()
    {
        // Testing the safety check in the get method
        assertNull(matrix.get(10, 10, 10, 10));
    }
}
