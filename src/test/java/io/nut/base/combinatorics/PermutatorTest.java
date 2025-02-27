/*
 * PermutatorTest.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.combinatorics;

import java.util.Arrays;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class PermutatorTest
{
    
    static final Comparator<Integer[]> CMP = new Comparator<Integer[]>()
    {
        @Override
        public int compare(Integer[] a, Integer[] b)
        {
            for(int i=0;i<a.length && i<b.length;i++)
            {
                int cmp = Integer.compare(a[i], b[i]);
                if(cmp!=0)
                {
                    return cmp;
                }
            }
            return Integer.compare(a.length, b.length);
        }
    };

    /**
     * Test of run method, of class Permutator.
     */
    @Test
    public void testRun()
    {
        {
            Integer[] elements = { 1 };
            Permutator<Integer> a = new Permutator<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = {{ 1 }};

            assertArrayEquals(exp, ret);
        }
                
        {
            Integer[] elements = { 1, 2 };
            Permutator<Integer> a = new Permutator<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = {{ 1,2}, { 2, 1}};

            Arrays.sort(ret, CMP);

            assertArrayEquals(exp, ret);
        }
        
        {
            Integer[] elements = { 1, 2, 3 };
            Permutator<Integer> a = new Permutator<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1,2,3}, {1,3,2}, {2,1,3}, {2,3,1}, {3,1,2}, {3,2,1}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[] elements = { 1, 2, 3 };
            Permutator<Integer> a = new Permutator<>(elements, 2);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1,2}, {1,3}, {2,1}, {2,3}, {3,1}, {3,2}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[] elements = { 1, 2, 3 };
            Permutator<Integer> a = new Permutator<>(elements, 1);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1}, {2}, {3}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }
        
        {
            Integer[] elements = { 1, 2, 3, 4 };
            Permutator<Integer> a = new Permutator<>(elements, 3);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = 
            { 
                {1,2,3}, {1,2,4}, {1,3,2}, {1,3,4}, {1,4,2}, {1,4,3}, {2,1,3}, {2,1,4}, {2,3,1}, {2,3,4}, 
                {2,4,1}, {2,4,3}, {3,1,2}, {3,1,4}, {3,2,1}, {3,2,4}, {3,4,1}, {3,4,2}, {4,1,2}, {4,1,3}, 
                {4,2,1}, {4,2,3}, {4,3,1}, {4,3,2}
            };

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

    }
}
