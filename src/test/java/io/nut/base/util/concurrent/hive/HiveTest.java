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

import java.util.logging.Level;
import java.util.logging.Logger;
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
    final Bee<Byte> beeByte = new Bee<Byte>(hive, 1) 
    {
        @Override
        public void receive(Byte m)
        {
            try
            {
                beeShort.send(m.shortValue());
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(HiveTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    final Bee<Short> beeShort = new Bee<Short>(hive, 1) 
    {
        @Override
        public void receive(Short m)
        {
            try
            {
                beeInteger.send(m.intValue());
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(HiveTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    final Bee<Integer> beeInteger = new Bee<Integer>(hive, 1) 
    {
        @Override
        public void receive(Integer m)
        {
            try
            {
                beeLong.send(m.longValue());
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(HiveTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    final Bee<Long> beeLong = new Bee<Long>(hive, 1) 
    {
        @Override
        public void receive(Long m)
        {
            try
            {
                beeString.send(m.toString());
                beeString.send(",");
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(HiveTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    final Bee<String> beeString = new Bee<String>(hive, 1) 
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
        for(int i=0;i<10;i++)
        {
            beeByte.send((byte)i);
        }
        
        beeByte.shutdown();
        beeByte.awaitTermination(Integer.MAX_VALUE);
        
        beeShort.shutdown();
        beeShort.awaitTermination(Integer.MAX_VALUE);
        
        beeInteger.shutdown();
        beeInteger.awaitTermination(Integer.MAX_VALUE);
        
        beeLong.shutdown();
        beeLong.awaitTermination(Integer.MAX_VALUE);
        
        beeString.shutdown();
        beeString.awaitTermination(Integer.MAX_VALUE);
        
        hive.shutdown();
        hive.awaitTermination(100);
        
        assertEquals("0,1,2,3,4,5,6,7,8,9,", s);
        assertTrue(hive.isShutdown(), "Shutdown");
        assertTrue(hive.isTerminated(), "Terminated");
    }
        
}
