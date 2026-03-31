/*
 * DenseMatrix2DTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
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
