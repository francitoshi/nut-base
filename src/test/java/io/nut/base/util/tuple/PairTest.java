/*
 *  PairTest.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.util.tuple;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PairTest
{
    /**
     * Test of getKey method, of class Pair.
     */
    @Test
    public void testGet()
    {
        Pair<Integer,String> instance = new Pair<>(1, "1");
        assertEquals(1, instance.getKey());
        assertEquals(1, instance.get1st());
        assertEquals(1, instance.getLeft());

        assertEquals("1", instance.getVal());
        assertEquals("1", instance.get2nd());
        assertEquals("1", instance.getRight());
    }
    
}
