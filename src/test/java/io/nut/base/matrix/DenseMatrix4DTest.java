/*
 * DenseMatrix4DTest.java
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
