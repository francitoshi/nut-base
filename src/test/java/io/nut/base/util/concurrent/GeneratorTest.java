/*
 *  GeneratorTest.java
 *
 *  Copyright (C) 2024-2026 francitoshi@gmail.com
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
package io.nut.base.util.concurrent;

import io.nut.base.util.Utils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class GeneratorTest
{

    static Generator<Character> letters(int n, int capacity)
    {
        return new Generator.Safe<Character>()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < n && !isShutdown(); i++)
                {
                    this.yield((char) ('A' + i));
                }
            }
        };
    }

    static Generator<Integer> numbers(int n, int capacity)
    {
        return new Generator.Safe<Integer>()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < n && !isShutdown(); i++)
                {
                    this.yield(i);
                }
            }
        };
    }

    /**
     * Test of iterator method, of class Generator.
     */
    @Test
    public void testIterator()
    {
        for (Character ch : letters(26, 0))
        {
            for (int num : numbers(99, 0))
            {
                System.out.print(" " + ch + num);
            }
            System.out.println();
        }
        for (Character ch : letters(26, 5))
        {
            numbers(99, 10).forEach((num) -> System.out.print(" " + ch + num));
            System.out.println();
        }
        Generator<Integer> x = numbers(99, 0);
    }
    
    /**
     * Test of shutdown method, of class Generator.
     */
    @Test
    public void testShutdown()
    {
        Generator<Character> instance = letters(26, 0);
        for (Character ch : instance)
        {
            Utils.sleep(ch);
            instance.shutdown();
        }
        instance = letters(26, 10);
        for (Character ch : instance)
        {
            Utils.sleep(ch);
            instance.shutdown();
        }
        instance.reset();
        int count=0;
        //run instance twice it should work
        for (Character ch : instance)
        {
            count++;
        }
        instance.shutdown();
        assertTrue(count>0);
    }
    /**
     * Test of shutdown method, of class Generator.
     */
    @Test
    public void testShutdownNow()
    {
        Generator<Character> instance = letters(26, 20);
        int count = 0;
        for (Character ch : instance)
        {
            count++;
            Utils.sleep(100);
            instance.shutdownNow();
        }
        assertEquals(1,count);
        instance.reset();
        count=0;
        //run instance twice it should work
        for (Character ch : instance)
        {
            count++;
            instance.shutdownNow();
        }
        assertTrue(count>0);
    }
    /**
     * Test of nesting loops
     */
    @Test
    public void testNestedLoop()
    {
        Generator<Character> instance0 = letters(26, 0);
        int count = 0;
        char[] c = new char[26];
        for (Character ch : instance0)
        {
            c[count++] = ch;
            for (Character ch2 : instance0)
            {
                c[count++] = ch2;
                if(count%5==4) break;
            }
        }
        for(int i=0;i<c.length;i++)
        {
            assertEquals('A' + i, c[i]);
        }
        
        Generator<Character> instance1 = letters(26, 11);
        count = 0;
        c = new char[26];
        for (Character ch : instance1)
        {
            c[count++] = ch;
            for (Character ch2 : instance1)
            {
                c[count++] = ch2;
                if(count%5==4) break;
            }
        }
        for(int i=0;i<c.length;i++)
        {
            assertEquals('A' + i, c[i]);
        }
    }
}
