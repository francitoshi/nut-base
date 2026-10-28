/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.combinatorics;

import static io.github.francitoshi.nut.base.combinatorics.Permutator2Test.CMP;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class Permutator2Test
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
     * Test of run method, of class Permutator2.
     */
    @Test
    public void testRun()
    {        
        {
            Integer[][] elements = {{ 1 }};
            Permutator2<Integer> a = new Permutator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = {{ 1 }};

            assertArrayEquals(exp, ret);
        }
    
        {
            Integer[][] elements = {{ 1, 2 }};
            Permutator2<Integer> a = new Permutator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = {{1},{2}};

            Arrays.sort(ret, CMP);

            assertArrayEquals(exp, ret);
        }
        
        {
            Integer[][] elements = {{ 1, 2, 3 }};
            Permutator2<Integer> a = new Permutator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1},{2},{3}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[][] elements = {{1},{2},{3}};
            Permutator2<Integer> a = new Permutator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1,2,3},{1,3,2},{2,1,3},{2,3,1},{3,1,2},{3,2,1}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }
        {
            Integer[][] elements = {{1,0},{2},{3,4}};
            Permutator2<Integer> a = new Permutator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = 
            { 
                {0,2,3},{0,2,4},{0,3,2},{0,4,2},
                {1,2,3},{1,2,4},{1,3,2},{1,4,2},
                {2,0,3},{2,0,4},{2,1,3},{2,1,4},{2,3,0},{2,3,1},{2,4,0},{2,4,1},
                {3,0,2},{3,1,2},{3,2,0},{3,2,1},
                {4,0,2},{4,1,2},{4,2,0},{4,2,1}
            };

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

    }
    
}
