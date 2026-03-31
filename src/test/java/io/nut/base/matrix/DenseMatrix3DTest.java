/*
 * DenseMatrix3DTest.java
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
