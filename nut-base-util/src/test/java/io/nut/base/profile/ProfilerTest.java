/*
 *  ProfilerTest.java
 *
 *  Copyright (C) 2023-2025 francitoshi@gmail.com
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
package io.nut.base.profile;

import io.nut.base.time.JavaTime;
import io.nut.base.util.Utils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ProfilerTest
{
    /**
     * Test of duration method, of class Profiler.
     */
    @Test
    public void testDuration()
    {
        assertEquals("0s", Profiler.duration(0));
        assertEquals("1m", Profiler.duration(60_000_000_000L));
        assertEquals("1m1s", Profiler.duration(61_000_000_000L));
        assertEquals("1m", Profiler.duration(60_999_999_999L));
        assertEquals("1m5s", Profiler.duration(65_100_200_300L));
        assertEquals("1m25s", Profiler.duration(85_000_000_000L));
        assertEquals("1m39s", Profiler.duration(99_900_900_000L));
    }

    @Test
    public void testExample1()
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS);
        
        Profiler.Task a = profiler.getTask("a");
        
        a.start();
        Utils.sleep(10);
        a.stop();
        a.count();

        a.start();
        Utils.sleep(5);
        a.stop();
        a.count();
        
        profiler.print();
        
        Profiler.Task b = profiler.getTask("b");
        
        b.start();
        Utils.sleep(15);
        b.stop();
        b.count();
        
        profiler.print();
    }

}
