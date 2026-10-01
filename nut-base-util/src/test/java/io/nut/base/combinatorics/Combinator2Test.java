/*
 * Combinator2Test.java
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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class Combinator2Test
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
    static final Comparator<String[]> CMPSTR = new Comparator<String[]>()
    {
        @Override
        public int compare(String[] a, String[] b)
        {
            for(int i=0;i<a.length && i<b.length;i++)
            {
                int cmp = a[i].compareTo(b[i]);
                if(cmp!=0)
                {
                    return cmp;
                }
            }
            return Integer.compare(a.length, b.length);
        }
    };

    /**
     * Test of run method, of class Combinator2.
     */
    @Test
    public void testRun()
    {
        {
            Integer[][] elements = {{ 1 }};
            Combinator2<Integer> a = new Combinator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = {{ 1 }};

            assertArrayEquals(exp, ret);
        }
    
        {
            Integer[][] elements = {{ 1, 2 }};
            Combinator2<Integer> a = new Combinator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = {{1},{2}};

            Arrays.sort(ret, CMP);

            assertArrayEquals(exp, ret);
        }
        
        {
            Integer[][] elements = {{ 1, 2, 3 }};
            Combinator2<Integer> a = new Combinator2<>(elements);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1},{2},{3}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[][] elements = {{1}, {2}, {3}};
            Combinator2<Integer> a = new Combinator2<>(elements, 2);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1,2}, {1,3}, {2,3}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[][] elements = {{ 1, 2, 3 }};
            Combinator2<Integer> a = new Combinator2<>(elements, 1);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1}, {2}, {3}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }
        
        {
            Integer[][] elements = {{ 1, 2, 3, 4 }};
            Combinator2<Integer> a = new Combinator2<>(elements, 1);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = { {1}, {2}, {3}, {4}};

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[][] elements = {{1,11},{2,22},{3,33},{4,44}};
            Combinator2<Integer> a = new Combinator2<>(elements, 2);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = 
            { 
                {1,2}, {1,3}, {1,4}, {1,22}, {1,33}, {1,44}, {2,3}, {2,4}, {2,33}, {2,44},
                {3,4}, {3,44}, {11,2}, {11,3}, {11,4}, {11,22}, {11,33}, {11,44}, {22,3}, {22,4}, 
                {22,33}, {22,44}, {33,4}, {33,44} ,
            };

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[][] elements = {{1,11},{2,22},{3,33},{4,44}};
            Combinator2<Integer> a = new Combinator2<>(elements, 3);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = 
            { 
                {1,2,3}, {1,2,4}, {1,2,33}, {1,2,44}, {1,3,4}, {1,3,44}, {1,22,3}, {1,22,4}, {1,22,33}, {1,22,44}, 
                {1,33,4}, {1,33,44}, {2,3,4}, {2,3,44}, {2,33,4}, {2,33,44}, {11,2,3}, {11,2,4}, {11,2,33}, {11,2,44}, 
                {11,3,4}, {11,3,44}, {11,22,3}, {11,22,4}, {11,22,33}, {11,22,44}, {11,33,4}, {11,33,44}, {22,3,4}, {22,3,44}, 
                {22,33,4}, {22,33,44}
            };

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }

        {
            Integer[][] elements = {{1,2,3},{11,22,33},{111,222,333}};
            Combinator2<Integer> a = new Combinator2<>(elements, 3);

            Integer[][] ret = a.toArray(new Integer[0][]);
            Integer[][] exp = 
            { 
                {1,11,111}, {1,11,222}, {1,11,333}, {1,22,111}, {1,22,222}, {1,22,333}, {1,33,111}, {1,33,222}, {1,33,333}, 
                {2,11,111}, {2,11,222}, {2,11,333}, {2,22,111}, {2,22,222}, {2,22,333}, {2,33,111}, {2,33,222}, {2,33,333}, 
                {3,11,111}, {3,11,222}, {3,11,333}, {3,22,111}, {3,22,222}, {3,22,333}, {3,33,111}, {3,33,222}, {3,33,333}
            };

            Arrays.sort(ret, CMP);
            
            assertArrayEquals(exp, ret);
        }
        {
            String[][] elements = {{"1","2","3"},{"11","22","33"},{"111","222","333"}};
            Combinator2<String> a = new Combinator2<>(elements, 3);

            String[][] ret = a.toArray(new String[0][]);
            String[][] exp = 
            { 
                {"1","11","111"}, {"1","11","222"}, {"1","11","333"}, {"1","22","111"}, {"1","22","222"}, {"1","22","333"}, {"1","33","111"}, {"1","33","222"}, {"1","33","333"},
                {"2","11","111"}, {"2","11","222"}, {"2","11","333"}, {"2","22","111"}, {"2","22","222"}, {"2","22","333"}, {"2","33","111"}, {"2","33","222"}, {"2","33","333"},
                {"3","11","111"}, {"3","11","222"}, {"3","11","333"}, {"3","22","111"}, {"3","22","222"}, {"3","22","333"}, {"3","33","111"}, {"3","33","222"}, {"3","33","333"}
            };

            Arrays.sort(ret, CMPSTR);
            
            assertArrayEquals(exp, ret);
        }

        {
            String[][] elements = {{"a","b","c"},{"1","2"}};
            Combinator2<String> a = new Combinator2<>(elements, 2);

            String[][] ret = a.toArray(new String[0][]);
            String[][] exp = 
            { 
                {"a","1"}, {"a","2"},
                {"b","1"}, {"b","2"},
                {"c","1"}, {"c","2"},
            };

            Arrays.sort(ret, CMPSTR);
            
            assertArrayEquals(exp, ret);
        }

    }
    
}
