/*
 *  HiveTest.java
 *
 *  Copyright (C) 2024-2025 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.hive;

import io.nut.base.profile.Profiler;
import io.nut.base.util.Utils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class HiveTest
{
    final Hive hive = new Hive();
    final Bee<Byte> beeByte = new Bee<Byte>(1, hive) 
    {
        @Override
        public void receive(Byte m)
        {
            beeShort.send(m.shortValue());
        }
    };
    final Bee<Short> beeShort = new Bee<Short>(1, hive) 
    {
        @Override
        public void receive(Short m)
        {
            beeInteger.send(m.intValue());
        }
    };
    final Bee<Integer> beeInteger = new Bee<Integer>(1, hive) 
    {
        @Override
        public void receive(Integer m)
        {
            beeLong.send(m.longValue());
        }
    };
    final Bee<Long> beeLong = new Bee<Long>(1, hive) 
    {
        @Override
        public void receive(Long m)
        {
            beeString.send(m.toString());
            beeString.send(",");
        }
    };
    final Bee<String> beeString = new Bee<String>(1, hive) 
    {
        @Override
        public void receive(String m)
        {
            s += m;
        }
    };
    
    volatile String s = "";
    
    /**
     * Test of shutdown method, of class Hive.
     */
    @Test
    public void testSomeMethod1() throws InterruptedException
    {   
        hive.add(beeByte, beeShort, beeInteger, beeLong, beeString);
        
        hive.add(beeByte).add(beeShort).add(beeInteger).add(beeLong).add(beeString);
        
        for(int i=0;i<10;i++)
        {
            beeByte.send((byte)i);
        }
        
        hive.shutdownAndAwaitTermination(beeByte, beeShort, beeInteger, beeLong, beeString);
        
        assertEquals("0,1,2,3,4,5,6,7,8,9,", s);
        assertTrue(hive.isShutdown(), "Shutdown");
        assertTrue(hive.isTerminated(), "Terminated");
    }
        
    /**
     * Test of shutdown method, of class Hive.
     */
    @Test
    public void testWaitPolicy() throws InterruptedException
    {   
        final Runnable fastTask = new Runnable()
        {
            @Override
            public void run()
            {
                Utils.sleep(5);
            }
        };
        final Runnable slowTask = new Runnable()
        {
            @Override
            public void run()
            {
                Utils.sleep(5_000);
            }
        };
        int th = 5;
        int q = 5;
        int loops = 4000;
        int slow = 10;
        Profiler profiler = new Profiler();
        Profiler.Task profilerRun = profiler.getTask("run");
        Profiler.Task profilerWait = profiler.getTask("wait");
        {
            Hive hive1 = new Hive(th, th, q, 60_000, false);
            profilerRun.start();
            for(int i=0;i<loops;i++)
            {
                hive1.execute(i==slow ? slowTask : fastTask);
                profilerRun.count();
            }
            hive1.shutdownAndAwaitTermination();
            profilerRun.stop().count();
        }
        
        {
            Hive hive2 = new Hive(th, th, q, 60_000, true);
            profilerWait.start();
            for(int i=0;i<loops;i++)
            {
                hive2.execute(i==slow ? slowTask : fastTask);
                profilerWait.count();
            }
            hive2.shutdownAndAwaitTermination();
            profilerWait.stop().count();
        }
        profiler.print();
        
        assertTrue(profilerRun.nanos()>profilerWait.nanos());
    }
        
}
