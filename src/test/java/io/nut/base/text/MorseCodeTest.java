/*
 * MorseCodeTest.java
 *
 * Copyright (c) 2013-2024 francitoshi@gmail.com
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
package io.nut.base.text;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MorseCodeTest
{
    
    public MorseCodeTest()
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

    /**
     * Test of _pattern method, of class MorseCode.
     */
    @Test
    public void test_pattern_String()
    {
        int[] e = {0, 100, 0};
        int[] ee = {0, 100, 300, 100, 0};
        int[] sos= { 0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 0 };
        int[] hw = { 0, 100, 100, 100, 100, 100, 100, 100, 300, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 700, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 0 };
        int[] v  = { 0, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 0 };
        int[] abc= { 0, 100, 100, 300, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 300, 100, 100, 700, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 100, 100, 100, 700, 100, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 700, 300, 100, 300, 300, 300, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 300, 100, 100, 100, 300, 0 };
        int[] txt= { 0, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 700, 100, 100, 300, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 300, 100, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 100, 100, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 300, 100, 100, 300, 100, 300, 100, 300, 300, 300, 100, 100, 100, 300, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 300, 300, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 300, 100, 300, 300, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 100, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 700, 100, 100, 300, 100, 300, 100, 300, 100, 300, 700, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 700, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 700, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 700, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 700, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 700, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 700, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 0 };
        
        MorseCode mc = new MorseCode(12, 12);
        
        assertArrayEquals(e, mc.pattern("e"));
        assertArrayEquals(e, mc.pattern(" e"));
        assertArrayEquals(e, mc.pattern("e•"));
        assertArrayEquals(ee, mc.pattern("EE"));
        assertArrayEquals(sos, mc.pattern("SOS"));
        assertArrayEquals(hw, mc.pattern("hello world"));
        assertArrayEquals(hw, mc.pattern("hello  world"));
        assertArrayEquals(hw, mc.pattern("hello\tworld"));
        assertArrayEquals(hw, mc.pattern("HELLO\tWORLD"));
        assertArrayEquals(v, mc.pattern("aeiou"));
        assertArrayEquals(abc, mc.pattern("abc xyz pq mn 0123456789."));
        assertArrayEquals(txt, mc.pattern("aeiou abcdefghijklmnopqrstuvwxyz 0123456789 1 22 333 4444 55555 666666 7777777 88888888 999999999"));

        ////////////////////////////
        e = pattern12to20(e);
        ee = pattern12to20(ee);
        sos = pattern12to20(sos);
        hw = pattern12to20(hw);
        v = pattern12to20(v);
        abc = pattern12to20(abc);
        txt = pattern12to20(txt);
        
        mc = new MorseCode(20, 20);
        
        assertArrayEquals(e, mc.pattern("e"));
        assertArrayEquals(ee, mc.pattern("EE"));
        assertArrayEquals(sos, mc.pattern("SOS"));
        assertArrayEquals(hw, mc.pattern("hello world"));
        assertArrayEquals(hw, mc.pattern("hello  world"));
        assertArrayEquals(hw, mc.pattern("hello\tworld"));
        assertArrayEquals(hw, mc.pattern("HELLO\tWORLD"));
        assertArrayEquals(v, mc.pattern("aeiou"));
        assertArrayEquals(abc, mc.pattern("abc xyz pq mn 0123456789."));
        assertArrayEquals(txt, mc.pattern("aeiou abcdefghijklmnopqrstuvwxyz 0123456789 1 22 333 4444 55555 666666 7777777 88888888 999999999"));
        
    }
    static int[] pattern12to20(int[] pattern12)
    {
        int[] pattern20 = new int[pattern12.length];
        for(int i=0;i<pattern12.length;i++)
        {
            pattern20[i] = (pattern12[i]*60)/100;
        }
        return pattern20;
    }

    /**
     * Test of prosign method, of class MorseCode.
     */
    @Test
    public void testProsign()
    {
        MorseCode instance = new MorseCode();
        
        //no word gap
        int[] AR = {0, 100, 100, 300, 100, 100, 100, 300, 100, 100, 0};
        int[] KA = {0, 300, 100, 100, 100, 300, 100, 100, 100, 300, 0};
        
        int[] ar = instance.prosign(MorseCode.AR_END_OF_MESSAGE, false);
        assertArrayEquals(AR, ar);
        
        int[] ka = instance.prosign(MorseCode.KA_BEGINNING_OF_MESSAGE, false);
        assertArrayEquals(KA, ka);
        
        //with word gap
        int[] AR2 = {0, 100, 100, 300, 100, 100, 100, 300, 100, 100, 700};
        int[] KA2 = {0, 300, 100, 100, 100, 300, 100, 100, 100, 300, 700};
                
        int[] ar2 = instance.prosign(MorseCode.AR_END_OF_MESSAGE, true);
        assertArrayEquals(AR2, ar2);
        
        int[] ka2 = instance.prosign(MorseCode.KA_BEGINNING_OF_MESSAGE, true);
        assertArrayEquals(KA2, ka2);
        
    }
}
