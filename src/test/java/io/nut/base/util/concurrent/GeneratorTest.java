/*
 *  GeneratorTest.java
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
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class GeneratorTest
{

    public GeneratorTest()
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

    static Generator<Character> letters(int n, int capacity)
    {
        return new Generator.Safe<Character>()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < n; i++)
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
                for (int i = 0; i < n; i++)
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
            instance.shutdown();
        }
    }
}
