/*
 * AbstractGaugeTest.java
 *
 * Copyright (c) 2015-2025 francitoshi@gmail.com
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
package io.nut.base.gauge;

import io.nut.base.util.Utils;
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
public class AbstractGaugeTest
{
    
    public AbstractGaugeTest()
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
     * Test of getPrefix method, of class AbstractGauge.
     */
    @Test
    public void testGetPrefix()
    {
        AbstractGauge instance = new AbstractGaugeProgressImpl();
        instance.setPrefix("123");
        assertEquals("123", instance.getPrefix());
    }

    /**
     * Test of isStarted method, of class AbstractGauge.
     */
    @Test
    public void testIsStarted()
    {
        AbstractGauge instance = new AbstractGaugeProgressImpl();
        assertFalse(instance.isStarted());
        instance.start();
        assertTrue(instance.isStarted());
        instance.close();
        assertFalse(instance.isStarted());
    }

    /**
     * Test of pause method, of class AbstractGauge.
     */
    @Test
    public void testPause()
    {
        AbstractGaugeProgressImpl instance = new AbstractGaugeProgressImpl();
        instance.start();
        instance.setNanos(10);
        instance.setVal(1);
        instance.pause();
        instance.setNanos(20);
        instance.resume();
        instance.setNanos(30);
        instance.setVal(2);
        assertEquals(20, instance.getAccuNanos());
    }

    /**
     * Test of resume method, of class AbstractGauge.
     */
    @Test
    public void testResume()
    {
        AbstractGaugeProgressImpl instance = new AbstractGaugeProgressImpl();
        instance.start();
        instance.setNanos(10);
        instance.setVal(1);
        instance.pause();
        instance.setNanos(20);
        assertEquals(10, instance.getLastNanos());
        instance.resume();
        assertEquals(20, instance.getLastNanos());
    }

    public class AbstractGaugeProgressImpl extends AbstractGauge
    {
        long nanos=0;
        boolean started;
        int max;
        int val;
        String prefix="";
        double done=0;
        String msg="";
        public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
        {
            this.started = started;
            this.max = max;
            this.val = val;
            this.prefix = prefix;
            this.done = done;
            this.msg = msg;
        }

        public long getNanos()
        {
            return nanos;
        }

        public void setNanos(long nanos)
        {
            this.nanos = nanos;
        }

        @Override
        protected long nanoTime()
        {
            return nanos;
        }
        
    }

    /**
     * Test of setPrefix method, of class AbstractGauge.
     */
    @Test
    public void testSetPrefix()
    {
        String prefix = "prefix";
        AbstractGaugeProgressImpl instance = new AbstractGaugeProgressImpl();
        instance.setPrefix(prefix);
        instance.invalidate();
        assertEquals(prefix, instance.prefix);
    }

    /**
     * Test of paint method, of class AbstractGauge.
     */
    @Test
    public void testPaint()
    {
        long nanos = System.nanoTime();
        
        AbstractGaugeProgressImpl instance = new AbstractGaugeProgressImpl();
        instance.setShow(true, true, true);
        instance.setNanos(nanos);
        instance.start();
        instance.setNanos(nanos+Utils.NANOS_PER_MILLIS*1000);
        instance.setVal(1);
        instance.pause();
        instance.setNanos(nanos+Utils.NANOS_PER_MILLIS*2000);
        instance.resume();
        instance.setNanos(nanos+Utils.NANOS_PER_MILLIS*3000);
        instance.setVal(2);
        instance.invalidate();
        
        assertEquals(2, instance.val);
        assertEquals(0.02, instance.done, 0.000001);
//        assertEquals(" 02.00% (2s) [1m38s] <1m40s>", instance.msg);
    }
   
}
