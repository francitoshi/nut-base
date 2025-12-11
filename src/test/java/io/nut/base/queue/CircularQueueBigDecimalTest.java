/*
 *  CircularQueueBigDecimalTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;

// Claude Sonnet 4.5
class CircularQueueBigDecimalTest
{
    private CircularQueueBigDecimal queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueBigDecimal(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push(new BigDecimal("10.5"));
        queue.push(new BigDecimal("20.7"));
        assertEquals(new BigDecimal("10.5"), queue.pop());
        assertEquals(new BigDecimal("20.7"), queue.pop());
        assertNull(queue.pop());
    }

    @Test
    void testSum()
    {
        queue.push(new BigDecimal("1.5"));
        queue.push(new BigDecimal("2.5"));
        queue.push(new BigDecimal("3.0"));
        assertEquals(new BigDecimal("7.0"), queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(new BigDecimal("10"));
        queue.push(new BigDecimal("20"));
        queue.push(new BigDecimal("30"));
        assertEquals(0, new BigDecimal("20").compareTo(queue.average()));
    }

    @Test
    void testMinMax()
    {
        queue.push(new BigDecimal("15.5"));
        queue.push(new BigDecimal("5.2"));
        queue.push(new BigDecimal("25.8"));
        assertEquals(new BigDecimal("5.2"), queue.min());
        assertEquals(new BigDecimal("25.8"), queue.max());
    }

    @Test
    void testGet()
    {
        queue.push(new BigDecimal("100"));
        queue.push(new BigDecimal("200"));
        assertEquals(new BigDecimal("100"), queue.get(0));
        assertEquals(new BigDecimal("200"), queue.get(1));
        assertNull(queue.get(5));
    }
}
