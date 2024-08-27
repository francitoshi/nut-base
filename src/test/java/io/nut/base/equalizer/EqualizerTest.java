/*
 * EqualizerTest.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.equalizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class EqualizerTest
{
    final Equalizer<String> byStringSize5 = new Equalizer<String>()
    {
        @Override
        public boolean equals(String t1, String t2)
        {
            return t1.length()==t2.length();
        }

        @Override
        public int hashCode(String e)
        {
            return e.length()%5;
        }
    };
    final Equalizer<String> byStringIgnoreCase = Equalizer.STRING_CASE_INSENSITIVE;
        
    /**
     * Test of equals method, of class Equalizer.
     */
    @Test
    public void testEquals()
    {
        String hw = "hello world";
        String HW = "HELLO WORLD";
        String hw1 = "hello world1";
        String HW2 = "HELLO WORLD2";

        assertTrue(byStringSize5.equals(hw, HW));
        assertTrue(byStringSize5.equals(hw1, HW2));
        assertFalse(byStringSize5.equals(hw, hw1));
        assertFalse(byStringSize5.equals(HW, HW2));

        assertTrue(byStringIgnoreCase.equals(hw, HW));
        assertFalse(byStringIgnoreCase.equals(hw1, HW2));
        
        //// Proxy
        EqualsProxy<String> size5hw = new EqualsProxy<>(byStringSize5,hw);
        EqualsProxy<String> size5HW = new EqualsProxy<>(byStringSize5,HW);
        EqualsProxy<String> size5hw1 = new EqualsProxy<>(byStringSize5,hw1);
        EqualsProxy<String> size5HW2 = new EqualsProxy<>(byStringSize5,HW2);
        
        assertTrue(size5hw.equals(size5HW));
        assertTrue(size5hw1.equals(size5HW2));
        assertFalse(size5hw.equals(size5hw1));
        
        EqualsProxy<String> ichw = new EqualsProxy<>(byStringIgnoreCase,hw);
        EqualsProxy<String> icHW = new EqualsProxy<>(byStringIgnoreCase,HW);
        EqualsProxy<String> ichw1 = new EqualsProxy<>(byStringIgnoreCase,hw1);
        EqualsProxy<String> icHW2 = new EqualsProxy<>(byStringIgnoreCase,HW2);
        
        assertTrue(ichw.equals(icHW));
        assertFalse(ichw1.equals(icHW2));
        assertFalse(ichw.equals(ichw1));
       
        StringBuilder sb0 = new StringBuilder("0");
        EqualsSame<StringBuilder> s01 = new EqualsSame<>(sb0);
        EqualsSame<StringBuilder> s02 = new EqualsSame<>(sb0);
        assertTrue(s01.equals(s01));
        assertTrue(s01.equals(s02));

        EqualsSame<StringBuilder> s1 = new EqualsSame<>(new StringBuilder("a"));
        EqualsSame<StringBuilder> s2 = new EqualsSame<>(new StringBuilder("a"));
        assertFalse(s1.equals(s2));
    }

    /**
     * Test of hashCode method, of class Equalizer.
     */
    @Test
    public void testHashCode()
    {

        String s0  = "";
        String s1  = "1";
        String s7 = "1234567";
        
        assertEquals(0, byStringSize5.hashCode(s0));
        assertEquals(1, byStringSize5.hashCode(s1));
        assertEquals(2, byStringSize5.hashCode(s7));

        String hw = "hello world";
        String HW = "HELLO WORLD";

        assertTrue(byStringIgnoreCase.hashCode(hw)==byStringIgnoreCase.hashCode(HW));

        EqualsProxy<String> ic0 = new EqualsProxy<>(byStringIgnoreCase,hw);
        EqualsProxy<String> ic1 = new EqualsProxy<>(byStringIgnoreCase,HW);
        
        assertTrue(ic0.hashCode()==ic1.hashCode());   
    }    
}
