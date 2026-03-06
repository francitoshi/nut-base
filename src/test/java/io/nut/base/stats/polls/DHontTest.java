/*
 *  DHontTest.java
 *
 *  Copyright (C) 2015-2026 francitoshi@gmail.com
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
package io.nut.base.stats.polls;

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
