/*
 * AtomicDoubleTest.java
 *
 * Copyright (c) 2018-2024 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.atomic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class AtomicDoubleTest
{
    
    public AtomicDoubleTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    /**
     * Test of get method, of class AtomicDouble.
     */
    @Test
    public void testGet()
    {
        AtomicDouble instance = new AtomicDouble(33.33);
        assertEquals(33.33, instance.get(), 0.0);
    }

    /**
     * Test of set method, of class AtomicDouble.
     */
    @Test
    public void testSet()
    {
        AtomicDouble instance = new AtomicDouble();
        instance.set(33.33);
    }

    /**
     * Test of lazySet method, of class AtomicDouble.
     */
    @Test
    public void testLazySet()
    {
        AtomicDouble instance = new AtomicDouble();
        instance.lazySet(33.33);
    }

    /**
     * Test of getAndSet method, of class AtomicDouble.
     */
    @Test
    public void testGetAndSet()
    {
        AtomicDouble instance = new AtomicDouble(33.33);
        assertEquals(33.33, instance.getAndSet(44.44), 0.0);
        assertEquals(44.44, instance.getAndSet(55.55), 0.0);
    }

    /**
     * Test of compareAndSet method, of class AtomicDouble.
     */
    @Test
    public void testCompareAndSet()
    {
        AtomicDouble instance = new AtomicDouble(33.33);
        assertFalse(instance.compareAndSet(44.44, 55.55));
        assertTrue(instance.compareAndSet(33.33, 55.55));
        assertTrue(instance.compareAndSet(55.55, 66.66));
    }

    /**
     * Test of weakCompareAndSet method, of class AtomicDouble.
     */
    @Test
    public void testWeakCompareAndSet()
    {
        AtomicDouble instance = new AtomicDouble(33.33);
        assertFalse(instance.weakCompareAndSet(44.44, 55.55));
        assertTrue(instance.weakCompareAndSet(33.33, 55.55));
        assertTrue(instance.weakCompareAndSet(55.55, 66.66));
    }

    /**
     * Test of getAndIncrement method, of class AtomicDouble.
     */
    @Test
    public void testGetAndIncrement()
    {
        AtomicDouble instance = new AtomicDouble(0);
        assertEquals(0, instance.getAndIncrement(), 0.0);
        assertEquals(1, instance.getAndIncrement(), 0.0);
    }

    /**
     * Test of getAndDecrement method, of class AtomicDouble.
     */
    @Test
    public void testGetAndDecrement()
    {
        AtomicDouble instance = new AtomicDouble(9);
        assertEquals(9, instance.getAndDecrement(), 0.0);
        assertEquals(8, instance.getAndDecrement(), 0.0);
    }

    /**
     * Test of getAndAdd method, of class AtomicDouble.
     */
    @Test
    public void testGetAndAdd()
    {
        AtomicDouble instance = new AtomicDouble(0);
        assertEquals(0, instance.getAndAdd(2), 0.0);
        assertEquals(2, instance.getAndAdd(2), 0.0);
        assertEquals(4, instance.getAndAdd(2), 0.0);
    }

    /**
     * Test of incrementAndGet method, of class AtomicDouble.
     */
    @Test
    public void testIncrementAndGet()
    {
        AtomicDouble instance = new AtomicDouble(0);
        assertEquals(1, instance.incrementAndGet(), 0.0);
        assertEquals(2, instance.incrementAndGet(), 0.0);
    }

    /**
     * Test of decrementAndGet method, of class AtomicDouble.
     */
    @Test
    public void testDecrementAndGet()
    {
        AtomicDouble instance = new AtomicDouble(9);
        assertEquals(8, instance.decrementAndGet(), 0.0);
        assertEquals(7, instance.decrementAndGet(), 0.0);
    }

    /**
     * Test of addAndGet method, of class AtomicDouble.
     */
    @Test
    public void testAddAndGet()
    {
        AtomicDouble instance = new AtomicDouble(0);
        assertEquals(2, instance.addAndGet(2), 0.0);
        assertEquals(4, instance.addAndGet(2), 0.0);
        assertEquals(6, instance.addAndGet(2), 0.0);
    }

    /**
     * Test of toString method, of class AtomicDouble.
     */
    @Test
    public void testToString()
    {
        AtomicDouble instance = new AtomicDouble(33.33);
        assertEquals("33.33", instance.toString());
    }

    /**
     * Test of intValue method, of class AtomicDouble.
     */
    @Test
    public void testIntValue()
    {
        int value = 3333;
        AtomicDouble instance = new AtomicDouble(value);
        assertEquals(value, instance.intValue());
    }

    /**
     * Test of longValue method, of class AtomicDouble.
     */
    @Test
    public void testLongValue()
    {
        long value = 3333;
        AtomicDouble instance = new AtomicDouble(value);
        assertEquals(value, instance.longValue());
    }

    /**
     * Test of floatValue method, of class AtomicDouble.
     */
    @Test
    public void testFloatValue()
    {
        float value = 33.33f;
        AtomicDouble instance = new AtomicDouble(value);
        assertEquals(value, instance.floatValue(), 0.0f);
    }

    /**
     * Test of doubleValue method, of class AtomicDouble.
     */
    @Test
    public void testDoubleValue()
    {
        AtomicDouble instance = new AtomicDouble(33.33);
        assertEquals(33.33, instance.doubleValue(), 0.0);
    }
    
}
