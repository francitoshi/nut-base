/*
 *  SplitterTest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.util;

import io.nut.base.equalizer.Equalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Splitter")
class SplitterTest
{
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Sorts each inner array and then the outer array so comparisons are order-independent. */
    private static <T extends Comparable<T>> T[][] normalize(T[][] data)
    {
        for (T[] inner : data)
        {
            Arrays.sort(inner);
        }
        Arrays.sort(data, (a, b) ->
        {
            int len = Math.min(a.length, b.length);
            for (int i = 0; i < len; i++)
            {
                int c = a[i].compareTo(b[i]);
                if (c != 0) return c;
            }
            return Integer.compare(a.length, b.length);
        });
        return data;
    }

    // -----------------------------------------------------------------------
    // splitEquals(T[], Comparator<T>)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("splitEquals(T[], Comparator)")
    class SplitEqualsWithComparator
    {
        @Test
        @DisplayName("null comparator falls back to natural ordering")
        void nullComparatorUsesNaturalOrder()
        {
            Integer[] src = {3, 1, 2, 1, 3, 3};
            Integer[][] result = normalize(Splitter.splitEquals(src, (Comparator<Integer>) null));

            assertEquals(3, result.length);
            assertArrayEquals(new Integer[]{1, 1}, result[0]);
            assertArrayEquals(new Integer[]{2},    result[1]);
            assertArrayEquals(new Integer[]{3, 3, 3}, result[2]);
        }

        @Test
        @DisplayName("custom comparator groups by absolute value")
        void customComparatorGroupsByAbsoluteValue()
        {
            Integer[] src = {-2, 1, 2, -1, 3};
            Comparator<Integer> absCmp = Comparator.comparingInt(Math::abs);
            Integer[][] result = normalize(Splitter.splitEquals(src, absCmp));

            // groups: {1,-1}, {2,-2}, {3}
            assertEquals(3, result.length);
        }

        @Test
        @DisplayName("single-element array produces one group")
        void singleElement()
        {
            String[] src = {"hello"};
            String[][] result = Splitter.splitEquals(src, (Comparator<String>) null);

            assertEquals(1, result.length);
            assertArrayEquals(new String[]{"hello"}, result[0]);
        }

        @Test
        @DisplayName("all elements equal produces one group")
        void allElementsEqual()
        {
            String[] src = {"a", "a", "a"};
            String[][] result = Splitter.splitEquals(src, (Comparator<String>) null);

            assertEquals(1, result.length);
            assertEquals(3, result[0].length);
        }

        @Test
        @DisplayName("all elements distinct produces one group per element")
        void allElementsDistinct()
        {
            String[] src = {"x", "y", "z"};
            String[][] result = normalize(Splitter.splitEquals(src, (Comparator<String>) null));

            assertEquals(3, result.length);
            for (String[] group : result)
            {
                assertEquals(1, group.length);
            }
        }

        @Test
        @DisplayName("case-insensitive comparator groups mixed-case strings")
        void caseInsensitiveComparator()
        {
            String[] src = {"Apple", "apple", "Banana", "BANANA", "cherry"};
            String[][] result = normalize(Splitter.splitEquals(src, String.CASE_INSENSITIVE_ORDER));

            assertEquals(3, result.length);
            assertEquals(2, result[0].length); // apple / Apple
            assertEquals(2, result[1].length); // Banana / BANANA
            assertEquals(1, result[2].length); // cherry
        }
    }

    // -----------------------------------------------------------------------
    // splitEquals(T[])  — natural-ordering convenience overload
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("splitEquals(T[]) – natural ordering")
    class SplitEqualsNatural
    {
        @Test
        @DisplayName("typical mixed input")
        void typicalMixedInput()
        {
            String[] src = {"banana", "apple", "apple", "cherry", "banana"};
            String[][] result = normalize(Splitter.splitEquals(src));

            assertEquals(3, result.length);
            assertArrayEquals(new String[]{"apple", "apple"}, result[0]);
            assertArrayEquals(new String[]{"banana", "banana"}, result[1]);
            assertArrayEquals(new String[]{"cherry"}, result[2]);
        }

        @Test
        @DisplayName("empty array produces empty result")
        void emptyArray()
        {
            String[] src = {};
            String[][] result = Splitter.splitEquals(src);

            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("integers are grouped correctly")
        void integers()
        {
            Integer[] src = {5, 5, 3, 1, 3};
            Integer[][] result = normalize(Splitter.splitEquals(src));

            assertEquals(3, result.length);
            assertArrayEquals(new Integer[]{1},    result[0]);
            assertArrayEquals(new Integer[]{3, 3}, result[1]);
            assertArrayEquals(new Integer[]{5, 5}, result[2]);
        }
    }

    // -----------------------------------------------------------------------
    // splitEquals(T[], Equalizer<T>)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("splitEquals(T[], Equalizer)")
    class SplitEqualsWithEqualizer
    {
        @Test
        @DisplayName("equalizer that ignores case groups correctly")
        void equalizerIgnoresCase()
        {
            String[] src = {"Hello", "hello", "World", "WORLD"};
            // Equalizer: equal if strings match ignoring case
            String[][] result = Splitter.splitEquals(src, new Comparator<String>()
            {
                @Override
                public int compare(String a, String b)
                {
                    return a.compareToIgnoreCase(b);
                }
            });

            assertEquals(2, result.length);
        }

        @Test
        @DisplayName("null equalizer falls back to default equality")
        void nullEqualizerFallsBack()
        {
            Integer[] src = {1, 2, 1, 3};
            Integer[][] result = normalize(Splitter.splitEquals(src, (io.nut.base.equalizer.Equalizer<Integer>) null));

            assertEquals(3, result.length);
        }

        @Test
        @DisplayName("equalizer that treats all elements equal produces one group")
        void allEqualEqualizer()
        {
            Integer[] src = {1, 2, 3, 4};
//            Integer[][] result = Splitter.splitEquals(src, (a, b) -> true);
            Integer[][] result = Splitter.splitEquals(src, new Equalizer<Integer>()
            {
                @Override
                public boolean equals(Integer t1, Integer t2)
                {
                    return true;
                }
                @Override
                public int hashCode(Integer e)
                {
                    return 0;
                }
            });

            assertEquals(1, result.length);
            assertEquals(4, result[0].length);
        }

        @Test
        @DisplayName("single-element array produces one group")
        void singleElement()
        {
            String[] src = {"only"};
            String[][] result = Splitter.splitEquals(src, new Comparator<String>()
            {
                @Override
                public int compare(String a, String b)
                {
                    return a.compareTo(b);
                }
            });

            assertEquals(1, result.length);
            assertArrayEquals(new String[]{"only"}, result[0]);
        }
    }

    // -----------------------------------------------------------------------
    // splitAgainEquals(T[][], Comparator<T>)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("splitAgainEquals(T[][], Comparator)")
    class SplitAgainEqualsWithComparator
    {
        @Test
        @DisplayName("refines existing groups with a finer comparator")
        void refinesExistingGroups()
        {
            // Start with two pre-grouped arrays that need further splitting
            Integer[][] src = 
            {
                {1, -1, 2},   // first group: needs abs-value split → {1,-1} and {2}
                {3, -3}       // second group: same abs value → stays as one group
            };
            Comparator<Integer> absCmp = Comparator.comparingInt(Math::abs);
            Integer[][] result = Splitter.splitAgainEquals(src, absCmp);

            // 3 groups total: {1,-1}, {2}, {3,-3}
            assertEquals(3, result.length);
        }

        @Test
        @DisplayName("null comparator uses natural ordering")
        void nullComparatorNaturalOrder()
        {
            String[][] src = 
            {
                {"b", "a", "a"},
                {"c", "b"}
            };
            String[][] result = normalize(Splitter.splitAgainEquals(src, null));

            // groups: {a,a}, {b}, {b}, {c} → 4 groups
            assertEquals(4, result.length);
        }

        @Test
        @DisplayName("empty outer array produces empty result")
        void emptyOuterArray()
        {
            String[][] src = {};
            String[][] result = Splitter.splitAgainEquals(src, null);

            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("inner arrays of one element remain unchanged")
        void singletonInnerArrays()
        {
            Integer[][] src = {{1}, {2}, {3}};
            Integer[][] result = Splitter.splitAgainEquals(src, null);

            assertEquals(3, result.length);
        }
    }

    // -----------------------------------------------------------------------
    // splitAgainEquals(T[][])  — natural-ordering convenience overload
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("splitAgainEquals(T[][]) – natural ordering")
    class SplitAgainEqualsNatural
    {
        @Test
        @DisplayName("already homogeneous inner arrays produce same number of groups")
        void alreadyHomogeneous()
        {
            String[][] src = {{"a", "a"}, {"b", "b"}};
            String[][] result = Splitter.splitAgainEquals(src);

            assertEquals(2, result.length);
        }

        @Test
        @DisplayName("heterogeneous inner arrays are split further")
        void heterogeneousInnerArrays()
        {
            String[][] src = {{"a", "b", "a"}, {"c", "c", "d"}};
            String[][] result = normalize(Splitter.splitAgainEquals(src));

            // groups: {a,a}, {b}, {c,c}, {d} → 4 groups
            assertEquals(4, result.length);
            assertArrayEquals(new String[]{"a", "a"}, result[0]);
            assertArrayEquals(new String[]{"b"},      result[1]);
            assertArrayEquals(new String[]{"c", "c"}, result[2]);
            assertArrayEquals(new String[]{"d"},      result[3]);
        }

        @Test
        @DisplayName("empty inner arrays produce no output groups")
        void emptyInnerArrays()
        {
            String[][] src = {{}, {}};
            String[][] result = Splitter.splitAgainEquals(src);

            assertEquals(0, result.length);
        }
    }
}