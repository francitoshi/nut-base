/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.combinatorics;

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
