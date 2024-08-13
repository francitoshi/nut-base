/*
 *  QueueBeeTest.java
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

import io.nut.base.util.Utils;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class QueueBeeTest
{

    static final String POISON = "POISON";
    /**
     * Test of receive method, of class QueueBee.
     */
    @Test
    public void testSomeMethod1() throws InterruptedException
    {
        Hive hive = new Hive();
        
        QueueBee<Integer,String> instance = new QueueBee<Integer,String>(hive,1) 
        {
            @Override
            protected void receive(Integer m)
            {
                try
                {
                    beeLong.send(m.longValue());
                }
                catch (InterruptedException ex)
                {
                    Logger.getLogger(QueueBeeTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            protected void terminate()
            {
                super.terminate();
                beeLong.shutdown();
            }
            
            final Bee<Long> beeLong = new Bee<Long>(hive, 1)
            {
                @Override
                protected void receive(Long m)
                {
                    try
                    {
                        beeString.send(m.toString());
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(QueueBeeTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                @Override
                protected void terminate()
                {
                    super.terminate();
                    beeString.shutdown();
                }
            };
            final Bee<String> beeString = new Bee<String>(hive, 1)
            {
                @Override
                protected void receive(String m)
                {
                    getTail().add(m);
                }
                @Override
                protected void terminate()
                {
                    super.terminate();
                    getTail().add(POISON);
                }
            };
        };
        
        instance.send(1);
        instance.send(2);
        instance.send(3);        
        instance.shutdown();        
        
        assertEquals("1", instance.take());
        assertEquals("2", instance.take());
        assertEquals("3", instance.take());
        
        instance.awaitTermination(Integer.MAX_VALUE);
        
        hive.shutdown();
        hive.awaitTermination(1000);
    }
    /**
     * Test of receive method, of class QueueBee.
     */
    @Test
    public void testSomeMethod2() throws InterruptedException
    {
        final int waitMillis = 400;
        final int messages = 128;
        final int beeThreads = 64;
        Hive hive = new Hive(messages);
        
        QueueBee<Byte,String> instance = new QueueBee<Byte,String>(hive,beeThreads) 
        {
            @Override
            protected void receive(Byte m)
            {
                try
                {
                    Utils.sleep(waitMillis);
                    beeShort.send(m.shortValue());
                    System.out.println(m+" "+m.getClass().getSimpleName());
                }
                catch (InterruptedException ex)
                {
                    Logger.getLogger(QueueBeeTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            protected void terminate()
            {
                super.terminate();
                beeShort.shutdown();
            }
            
            final Bee<Short> beeShort = new Bee<Short>(hive, beeThreads)
            {
                @Override
                protected void receive(Short m)
                {
                    try
                    {
                        Utils.sleep(waitMillis);
                        beeInteger.send(m.intValue());
                    System.out.println(m+" "+m.getClass().getSimpleName());
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(QueueBeeTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                @Override
                protected void terminate()
                {
                    super.terminate();
                    beeInteger.shutdown();
                }
            };
            final Bee<Integer> beeInteger = new Bee<Integer>(hive, beeThreads)
            {
                @Override
                protected void receive(Integer m)
                {
                    try
                    {
                        Utils.sleep(waitMillis);
                        beeLong.send(m.longValue());
                        System.out.println(m+" "+m.getClass().getSimpleName());
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(QueueBeeTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                @Override
                protected void terminate()
                {
                    super.terminate();
                    beeLong.shutdown();
                }
            };
            final Bee<Long> beeLong = new Bee<Long>(hive, beeThreads)
            {
                @Override
                protected void receive(Long m)
                {
                    try
                    {
                        Utils.sleep(waitMillis);
                        beeString.send(m.toString());
                        System.out.println(m+" "+m.getClass().getSimpleName());
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(QueueBeeTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                @Override
                protected void terminate()
                {
                    super.terminate();
                    beeString.shutdown();
                }
            };
            final Bee<String> beeString = new Bee<String>(hive, beeThreads)
            {
                @Override
                protected void receive(String m)
                {
                    Utils.sleep(waitMillis);
                    getTail().add(m);
                    System.out.println(m+" "+m.getClass().getSimpleName());
                }
                @Override
                protected void terminate()
                {
                    super.terminate();
                    getTail().add(POISON);
                }
            };
        };
        
        instance.send((byte)0);
        for(int i=0;i<messages;i++)
        {
            instance.send((byte)i);
        }
        instance.shutdown();
        String value;
        
        for(int i=0;!POISON.equals(value=instance.take());i++)
        {
            System.out.println("loop "+value);
        }
        
        hive.shutdown();
        hive.awaitTermination(1000);
    }

}