/*
 *  CircularQueueBigIntegerTest.java
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

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigInteger;

// Claude Sonnet 4.5
class CircularQueueBigIntegerTest
{

    private CircularQueueBigInteger queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueBigInteger(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push(new BigInteger("100"));
        queue.push(new BigInteger("200"));
        assertEquals(new BigInteger("100"), queue.pop());
        assertEquals(new BigInteger("200"), queue.pop());
        assertNull(queue.pop());
    }

    @Test
    void testSum()
    {
        queue.push(new BigInteger("1000000000000"));
        queue.push(new BigInteger("2000000000000"));
        queue.push(new BigInteger("3000000000000"));
        assertEquals(new BigInteger("6000000000000"), queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(new BigInteger("30"));
        queue.push(new BigInteger("60"));
        queue.push(new BigInteger("90"));
        assertEquals(new BigDecimal("60"), queue.average());
    }

    @Test
    void testMinMax()
    {
        queue.push(new BigInteger("500"));
        queue.push(new BigInteger("100"));
        queue.push(new BigInteger("300"));
        assertEquals(new BigInteger("100"), queue.min());
        assertEquals(new BigInteger("500"), queue.max());
    }

    @Test
    void testPushOverflow()
    {
        queue.push(new BigInteger("10"));
        queue.push(new BigInteger("20"));
        queue.push(new BigInteger("30"));
        BigInteger removed = queue.push(new BigInteger("40"));
        assertEquals(new BigInteger("10"), removed);
    }
}
