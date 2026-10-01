/*
 *  TimingTest.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.debug;

import io.nut.base.util.Utils;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class TimingTest
{
    /**
     * Test of getInstance method, of class Timing.
     */
    @Test
    public void testGetSecondsInstance_int()
    {
        Timing timing = Timing.getSecondsInstance(2);
        long t1 = System.nanoTime();
        Utils.sleep(1);
        long t2 = System.nanoTime();
        Utils.sleep(1000);
        long t3 = System.nanoTime();
        
        timing.println("getSecondsInstance", t1, t2);
        timing.println("getSecondsInstance", t2, t3);
        
    }
    
    /**
     * Test of getInstance method, of class Timing.
     */
    @Test
    public void testGetMillisInstance_int()
    {
        Timing timing = Timing.getMillisInstance(2);
        long t1 = System.nanoTime();
        Utils.sleep(9);
        long t2 = System.nanoTime();
        Utils.sleep(99);
        long t3 = System.nanoTime();
        
        timing.println("getMillisInstance", t1, t2);
        timing.println("getMillisInstance", t2, t3);
        
    }
    
    /**
     * Test of getNanosInstance method, of class Timing.
     */
    @Test
    public void testGetNanosInstance_int()
    {
        Timing timing = Timing.getNanosInstance(2);
        long t1 = System.nanoTime();
        Utils.sleep(1);
        long t2 = System.nanoTime();
        Utils.sleep(1);
        long t3 = System.nanoTime();
        
        timing.println("getNanosInstance", t1, t2);
        timing.println("getNanosInstance", t2, t3);
    }

}
