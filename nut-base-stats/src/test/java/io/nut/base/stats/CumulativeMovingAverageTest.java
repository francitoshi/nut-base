/*
 *  CumulativeMovingAverageTest.java
 *
 *  Copyright (c) 2024-2025 francitoshi@gmail.com
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
package io.nut.base.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author franci
 */
public class CumulativeMovingAverageTest
{

    /**
     * Test of next method, of class CumulativeMovingAverage.
     */
    @Test
    public void testNext()
    {
        CumulativeMovingAverage cma = MovingAverage.createCMA();
        for (int i = 1; i < 100; i++)
        {
            SimpleMovingAverage sma = MovingAverage.createSMA(i);

            double expected = 0;
            for (int j = 1; j <= i; j++)
            {
                expected = sma.next(j);
            }
            double result = cma.next(i);
            assertEquals(expected, result);
        }
    }

    private CumulativeMovingAverage cma;
    // a small delta is required when comparing doubles for equality
    private final double DELTA = 0.000001;

    
    @BeforeEach
    void setUp() throws Exception
    {
        cma = new CumulativeMovingAverage();
    }

    @Test
    void testInitialState()
    {
        // Depending on your implementation, this might be 0.0 or NaN.
        // The original code provided returns 0.0.
        assertEquals(0.0, cma.average(), DELTA, "Initial average should be 0.0");
    }

    @Test
    void testStandardBehavior()
    {
        // 1. Input: 10 -> Avg: 10
        assertEquals(10.0, cma.next(10), DELTA);

        // 2. Input: 20 -> Avg: 15 ((10+20)/2)
        assertEquals(15.0, cma.next(20), DELTA);

        // 3. Input: 30 -> Avg: 20 ((10+20+30)/3)
        assertEquals(20.0, cma.next(30), DELTA);

        // Check separate call to average()
        assertEquals(20.0, cma.average(), DELTA);
    }

    @Test
    void testNegativeNumbers()
    {
        cma.next(-10);
        assertEquals(-10.0, cma.average(), DELTA);

        cma.next(-20);
        // (-10 + -20) / 2 = -15
        assertEquals(-15.0, cma.average(), DELTA);

        cma.next(30);
        // (-10 + -20 + 30) / 3 = 0
        assertEquals(0.0, cma.average(), DELTA);
    }

    @Test
    void testZeroValues()
    {
        cma.next(0);
        assertEquals(0.0, cma.average(), DELTA);

        cma.next(0);
        assertEquals(0.0, cma.average(), DELTA);

        cma.next(3);
        // (0 + 0 + 3) / 3 = 1
        assertEquals(1.0, cma.average(), DELTA);
    }

    @Test
    void testFloatingPointValues()
    {
        cma.next(1.5);
        cma.next(2.5);
        // (1.5 + 2.5) / 2 = 2.0
        assertEquals(2.0, cma.average(), DELTA);

        cma.next(3.5);
        // (1.5 + 2.5 + 3.5) / 3 = 2.5
        assertEquals(2.5, cma.average(), DELTA);
    }

    /**
     * This test checks if the logic holds up over a slightly larger set. If you
     * fixed the code to use the recurrence formula: cma = cma + (val - cma) / n
     * This test ensures the math is still logically equivalent to sum / n.
     */
    @Test
    void testSequenceConsistency()
    {
        double[] inputs = { 5, 15, 25, 100, -50 };
        double sum = 0;
        int count = 0;

        for (double val : inputs)
        {
            sum += val;
            count++;
            double expected = sum / count;
            assertEquals(expected, cma.next(val), DELTA, "Failed at index " + (count - 1));
        }
    }

}
