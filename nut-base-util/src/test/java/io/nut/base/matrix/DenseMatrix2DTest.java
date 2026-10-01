/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DenseMatrix2DTest
{

    private DenseMatrix2D<String> matrix;

    @BeforeEach
    void setUp()
    {
        // Initialize with a 3x3 array
        matrix = new DenseMatrix2D<>(new String[3][3]);
    }

    @Test
    void testDimensions()
    {
        assertEquals(2, matrix.dimensions());
    }

    @Test
    void testSetAndGet()
    {
        matrix.set("Hello", 1, 1);
        assertEquals("Hello", matrix.get(1, 1));
    }

    @Test
    void testGetEmptyCellReturnsNull()
    {
        assertNull(matrix.get(0, 0));
    }

    @Test
    void testRemove()
    {
        matrix.set("Value", 0, 0);
        String removed = matrix.remove(0, 0);

        assertEquals("Value", removed);
        assertNull(matrix.get(0, 0));
    }

    @Test
    void testInvalidDimensionsThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> matrix.get(1));
        assertThrows(IllegalArgumentException.class, () -> matrix.get(1, 1, 1));
    }

    @Test
    void testAnyMatch()
    {
        matrix.set("Target", 2, 2);
        assertTrue(matrix.anyMatch(val -> val.equals("Target")));
        assertFalse(matrix.anyMatch(val -> val.equals("NonExistent")));
    }

    @Test
    void testAllMatch()
    {
        matrix.set("A", 0, 0);
        matrix.set("A", 0, 1);
        assertTrue(matrix.allMatch(val -> val.equals("A")));

        matrix.set("B", 1, 1);
        assertFalse(matrix.allMatch(val -> val.equals("A")));
    }

    @Test
    void testFindFirst()
    {
        matrix.set("Apple", 1, 1);
        Optional<String> result = matrix.findFirst(val -> val.startsWith("Ap"));
        assertTrue(result.isPresent());
        assertEquals("Apple", result.get());
    }

    @Test
    void testForEach()
    {
        matrix.set("Data", 1, 2);
        AtomicInteger callCount = new AtomicInteger(0);

        matrix.forEach((indices, value) ->
        {
            callCount.incrementAndGet();
            assertEquals(1, indices[0]);
            assertEquals(2, indices[1]);
            assertEquals("Data", value);
        });

        assertEquals(1, callCount.get());
    }

    @Test
    void testClear()
    {
        matrix.set("A", 0, 0);
        matrix.set("B", 1, 1);
        matrix.clear();

        assertNull(matrix.get(0, 0));
        assertNull(matrix.get(1, 1));
    }
}
