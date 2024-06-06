/*
 *  DelayOutputStreamTest.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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
package io.nut.base.io;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class DelayOutputStreamTest
{
    /**
     * Test of main method, of class DelayOutputStream.
     */
    @Test
    public void testMain1() throws Exception
    {
        int ms = 100;
        int low = (int) (ms/2);
        int high = ms+low;
        
        PrintStream out = new PrintStream(new DelayOutputStream(System.out, 0, ms, 10));

        out.printf("start main test %d / %d / %d\n", low, ms, high);
        
        long t0 = System.nanoTime();

        for (int i = 0; i < 100; i++)
        {
            out.printf("%s\n",LocalDateTime.now());
            long t1 = System.nanoTime();
            long diff = TimeUnit.NANOSECONDS.toMillis(t1-t0);
            System.out.println("diff="+diff);
            assertTrue(diff>low," i="+i+" diff>75 diff="+diff);
            assertTrue(diff<high," i="+i+" diff>99 diff="+diff);
            t0=t1;
        }
        
    }
    /**
     * Test of main method, of class DelayOutputStream.
     */
    @Test
    public void testMain2() throws Exception
    {
        int ms = 10;
        int low = (int) (ms/2)*10;
        int high = (ms+low)*10;
        
        PrintStream out = new PrintStream(new DelayOutputStream(System.out, ms, 0, 10));

        System.out.printf("start main test %d / %d / %d\n", low, ms, high);
        
        long t0 = System.nanoTime();

        for (int i = 0; i < 100; i++)
        {
            out.println(".........");
            long t1 = System.nanoTime();
            long diff = TimeUnit.NANOSECONDS.toMillis(t1-t0);
            System.out.println("diff="+diff+" i="+i);
            assertTrue(diff>low," i="+i+" diff>75 diff="+diff);
            assertTrue(diff<high," i="+i+" diff>99 diff="+diff);
            t0=t1;
        }
        
    }
    
}
