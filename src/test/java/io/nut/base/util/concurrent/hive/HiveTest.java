/*
 *  HiveTest.java
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
package io.nut.base.util.concurrent.hive;

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
        
}
