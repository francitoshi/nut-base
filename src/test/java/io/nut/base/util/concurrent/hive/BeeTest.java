/*
 *  BeeTest.java
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

import io.nut.base.util.Utils;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BeeTest
{
    /**
     * Test of setHive method, of class Bee.
     */
    @Test
    public void testSetHive()
    {
        Bee<String> instance = new Bee<String>()
        {
            @Override
            protected void receive(String m)
            {
                System.out.println(m);
            }
        };
        instance.send("hello");

        Hive hive = new Hive();
        instance.setHive(hive);

        instance.send("world");
        
        instance.shutdown();
    }

    /**
     * Test of getException method, of class Bee.
     */
    @Test
    public void testGetException() 
    {
        //Test Bees with no Hive work synchronously
        Bee<String> instance = new Bee()
        {
            @Override
            protected void receive(Object m) 
            {
                throw new NullPointerException();
            }
        };
        instance.dryLogger();
        instance.send("hello");        
        assertNotNull(instance.getException());
    }

    /**
     * Test of send method, of class Bee.
     */
    @Test
    public void testSend() 
    {
        final AtomicBoolean eureka = new AtomicBoolean();
        final AtomicInteger concurrent = new AtomicInteger(0);
        final AtomicInteger count = new AtomicInteger(0);
        final Hive hive = new Hive(8,16,4,1000);
        //Test Bees with no Hive work synchronously
        Bee<String> instance = new Bee(8, hive)
        {
            @Override
            protected void receive(Object m) 
            {
                int n = concurrent.incrementAndGet();
                count.incrementAndGet();
                if(n>4)
                {
                    eureka.set(true);
                }
                else if(eureka.get()==false)
                {
                    Utils.sleep(100);
                }
                concurrent.decrementAndGet();
            }
        };
        for(int i=0;i<100;i++)
        {
            instance.send("hello");
        }
        Utils.sleep(500);
        assertEquals(100, count.get());
        assertTrue(eureka.get());
    }
   
}
