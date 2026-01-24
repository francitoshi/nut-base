/*
 *  DHontTest.java
 *
 *  Copyright (C) 2015-2026 Francisco Gómez Carrasco
 *
 *  Report bugs or new features to: flikxxi@gmail.com
 *
 */
package io.nut.base.stats.polls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DHontTest
{
    @Test
    public void testCompute()
    {
        {
            int[] votes = {340000, 280000, 160000, 60000, 15000};
            int[] expected = {3, 3, 1, 0, 0};

            DHont instance = new DHont(7);
            int[] result = instance.distribute(votes);
            assertArrayEquals(expected, result);
        }        
        {
            int[] votes = {391000, 311000, 184000,  73000, 27000, 12000, 2000};
            int[] expected = {9, 7, 4, 1, 0, 0, 0};

            DHont instance = new DHont(21);
            int[] result = instance.distribute(votes);
            assertArrayEquals(expected, result);
        }        
        {
            int[] votes = {100000, 80000, 30000, 20000};
            int[] expected = {4, 3, 1, 0};

            DHont instance = new DHont(8);
            int[] result = instance.distribute(votes);
            assertArrayEquals(expected, result);
        }        
        {
            int[] votes = {75000, 48000, 34000, 28000};
            int[] expected = {2, 1, 1, 1};

            DHont instance = new DHont(5);
            int[] result = instance.distribute(votes);
            assertArrayEquals(expected, result);
        }        
        {
            int[] votes = {9, 8, 7,  6, 5};
            int[] expected = {2, 1, 1, 1, 0};

            DHont instance = new DHont(5, 0.18);
            int[] result = instance.distribute(votes);
            assertArrayEquals(expected, result);
        }        
    }

}
