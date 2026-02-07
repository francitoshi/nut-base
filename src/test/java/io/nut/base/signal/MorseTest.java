/*
 * MorseTest.java
 *
 * Copyright (c) 2025-2026 francitoshi@gmail.com
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
package io.nut.base.signal;

import io.nut.base.math.Nums;
import static io.nut.base.signal.Morse.DAH;
import static io.nut.base.signal.Morse.DIT;
import io.nut.base.util.Strings;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

/**
 *
 * @author franci
 */
public class MorseTest
{
    static final String H = "....";
    static final String E = ".";
    static final String L = ".-..";
    static final String O = "---";
    static final String W = ".--";
    static final String R = ".-.";
    static final String D = "-..";

    static final String HELLO_WORLD = "Hello World";
    static final String[][] H_E_L_L_O_W_O_R_L_D = {{H,E,L,L,O},{W,O,R,L,D}};
    static final String HELLO_WORLD_MORSE = ".... . .-.. .-.. --- / .-- --- .-. .-.. -..";
    static final byte[][][] HELLO_WORLD_UNITS = {{{DIT,DIT,DIT,DIT}, {DIT}, {DIT,DAH,DIT,DIT}, {DIT,DAH,DIT,DIT}, {DAH,DAH,DAH}}, {{DIT,DAH,DAH}, {DAH,DAH,DAH}, {DIT,DAH,DIT}, {DIT,DAH,DIT,DIT}, {DAH,DIT,DIT}}};
    static final int[] HELLO_WORLD_PATTERN = {0,60,60,60,60,60,60,60,180,60,180,60,60,180,60,60,60,60,180,60,60,180,60,60,60,60,180,180,60,180,60,180,420,60,60,180,60,180,180,180,60,180,60,180,180,60,60,180,60,60,180,60,60,180,60,60,60,60,180,180,60,60,60,60};
    
    static final String ABC = "abcdefghijklmnopqrstuvwxyz.,0123456789";
    static final String ABC_MORSE = ".- -... -.-. -.. . ..-. --. .... .. .--- -.- .-.. -- -. --- .--. --.- .-. ... - ..- ...- .-- -..- -.-- --.. .-.-.- --..-- ----- .---- ..--- ...-- ....- ..... -.... --... ---.. ----.";
    
    static final String A_B_C = "a b c d e f g h i j k l m n o p q r s t u v w x y z . , 0 1 2 3 4 5 6 7 8 9";
    static final String A_B_C_MORSE = ".- / -... / -.-. / -.. / . / ..-. / --. / .... / .. / .--- / -.- / .-.. / -- / -. / --- / .--. / --.- / .-. / ... / - / ..- / ...- / .-- / -..- / -.-- / --.. / .-.-.- / --..-- / ----- / .---- / ..--- / ...-- / ....- / ..... / -.... / --... / ---.. / ----.";
    
    static final String PROSIGNS = "<CT> HELLO WORLD <SK>";
    static final String PROSIGNS_MORSE = "-.-.- / .... . .-.. .-.. --- / .-- --- .-. .-.. -.. / ...-.-";

    static final String SOS_PROSIGN = "<SOS>";
    static final String SOS_PROSIGN_MORSE = "...---...";
    static final byte[][][] SOS_PROSIGN_UNITS = {{{DIT,DIT,DIT,DAH,DAH,DAH,DIT,DIT,DIT}}};
    static final int[] SOS_PROSIGN_PATTERN = {0,60,60,60,60,60,60,180,60,180,60,180,60,60,60,60,60,60};
    
    /**
     * Test of encode method, of class Morse.
     */
    @Test
    public void testEncodeJoin()
    {
        Morse morse = new Morse(20, 20, 0, 0);
        
        assertTrue(Arrays.deepEquals(H_E_L_L_O_W_O_R_L_D, morse.encode(HELLO_WORLD)));
        assertEquals(HELLO_WORLD_MORSE, morse.join(morse.encode(HELLO_WORLD)));
        assertEquals(ABC_MORSE, morse.join(morse.encode(ABC)));
        assertEquals(A_B_C_MORSE, morse.join(morse.encode(A_B_C)));
        assertEquals(PROSIGNS_MORSE, morse.join(morse.encode(PROSIGNS)));
        
        assertEquals(SOS_PROSIGN_MORSE, morse.join(morse.encode(SOS_PROSIGN)));
    }

    /**
     * Test of decode method, of class Morse.
     */
    @Test
    public void testDecode()
    {
        char[] EXTRA = "&'@)(,=!.-+\"?/Ñ".toCharArray();
                
        Morse morse = new Morse();

        StringBuilder sb0 = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        for(char i='A';i<'Z';i++)
        {
            sb0.append(i);
            sb1.append(i).append(' ');
        }
        for(char i='0';i<='9';i++)
        {
            sb0.append(i);
            sb1.append(i).append(' ');
        }
        for(int i=0;i<EXTRA.length;i++)
        {
            sb0.append(EXTRA[i]);
            sb1.append(EXTRA[i]).append(' ');
        }
        String s0 = sb0.toString();
        String s1 = sb1.toString().trim();
        
        String[][] m0 = morse.encode(s0);
        String[][] m1 = morse.encode(s1);
        
        String j0 = morse.join(m0);
        String j1 = morse.join(m1);        
        
        assertEquals(s0, morse.decode(j0));
        assertEquals(s1, morse.decode(j1));
        
    }

    /**
     * Test of encodeUnits method, of class Morse.
     */
    @Test
    public void testEncodeUnits()
    {
        Morse instance = new Morse();
        assertArrayEquals(HELLO_WORLD_UNITS, instance.encodeUnits(HELLO_WORLD));
        assertArrayEquals(SOS_PROSIGN_UNITS, instance.encodeUnits(SOS_PROSIGN));
    }

    /**
     * Test of encodePattern method, of class Morse.
     */
    @Test
    public void testEncodePattern()
    {
        Morse instance = new Morse(20,20, 0, 0);
        assertArrayEquals(HELLO_WORLD_PATTERN, instance.encodePattern(HELLO_WORLD));
        assertArrayEquals(SOS_PROSIGN_PATTERN, instance.encodePattern(SOS_PROSIGN));
    }
 
    static final String[] TEXTS =
    {
        "<SOS> is a Morse code distress signal", 
        "<CT> Start of transmission or Start of new message", 
        "<hh> error", 
        "<SK> end of transmision",
        ABC,
        A_B_C,
        PROSIGNS
    };
    /**
     * Test of decodeRobust method, of class MorseDecoder.
     */
    @Test
    public void testDecodePattern()
    {
        Morse morse = new Morse(20, 20, 0, 0);
        
        for(String item : TEXTS)
        {
            int[] msgPattern0 = morse.encodePattern(item);
            String result0 = morse.decodePattern(msgPattern0);
            assertEquals(item.toUpperCase(), result0);
        }
    }

    @Test
    public void testEncodePattern2()
    {
        Morse morse = new Morse(12, 12, 0, 0);
        
        int[] e = new int[]{0, 100};
        int[] ee = new int[]{0, 100, 300, 100};
        int[] sos= new int[]{ 0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100};
        int[] hw = new int[]{ 0, 100, 100, 100, 100, 100, 100, 100, 300, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 700, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100};
        int[] v  = new int[]{ 0, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300};
        int[] abc= new int[]{ 0, 100, 100, 300, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 300, 100, 100, 700, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 100, 100, 100, 700, 100, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 700, 300, 100, 300, 300, 300, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 300, 100, 100, 100, 300};
        int[] txt= { 0, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 700, 100, 100, 300, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 300, 100, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 100, 100, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 300, 100, 100, 300, 100, 300, 100, 300, 300, 300, 100, 100, 100, 300, 300, 100, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 300, 300, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 100, 100, 300, 300, 100, 100, 300, 100, 100, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 300, 100, 300, 300, 300, 100, 100, 100, 100, 100, 300, 300, 300, 100, 100, 100, 300, 100, 300, 300, 300, 100, 300, 100, 100, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 700, 100, 100, 300, 100, 300, 100, 300, 100, 300, 700, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 700, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 100, 300, 100, 300, 700, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 700, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 300, 100, 100, 100, 100, 100, 100, 100, 100, 100, 700, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 300, 300, 100, 100, 100, 100, 100, 100, 100, 100, 700, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 100, 100, 100, 100, 100, 700, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 100, 100, 100, 100, 700, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100, 300, 300, 100, 300, 100, 300, 100, 300, 100, 100};
        
        assertArrayEquals(e, morse.encodePattern("e"));
        assertArrayEquals(e, morse.encodePattern(" e"));
        assertArrayEquals(e, morse.encodePattern("e•"));
        assertArrayEquals(ee, morse.encodePattern("EE"));
        assertArrayEquals(sos, morse.encodePattern("SOS"));
        assertArrayEquals(hw, morse.encodePattern("hello world"));
        assertArrayEquals(hw, morse.encodePattern("hello  world"));
        assertArrayEquals(hw, morse.encodePattern("hello\tworld"));
        assertArrayEquals(hw, morse.encodePattern("HELLO\tWORLD"));
        assertArrayEquals(v, morse.encodePattern("aeiou"));
        assertArrayEquals(abc, morse.encodePattern("abc xyz pq mn 0123456789."));
        assertArrayEquals(txt, morse.encodePattern("aeiou abcdefghijklmnopqrstuvwxyz 0123456789 1 22 333 4444 55555 666666 7777777 88888888 999999999"));
    }
    
    @Test
    public void testParis()
    {
        Morse morse = new Morse(20, 20, 0, 0);
        
        String paris20 = Strings.repeat("paris ", 20);
        int[] paris = morse.encodePattern(paris20);
        int total = 0;
        for(int i : paris)
        {
            total += i;
        }
        assertEquals(59580, total);
    }

    
    static class Item implements Comparable<Item>
    {
        final String s;
        final int units;
        public Item(String s, int units)
        {
            this.s = s;
            this.units = units;
        }

        @Override
        public int compareTo(Item other)
        {
            int ret = Integer.compare(this.units, other.units);
            if(ret==0)
            {
                ret = this.s.compareTo(other.s);
            }
            return ret;
        }

        @Override
        public String toString()
        {
            return s + "=" + units;
        }
        
    }

    @Test
    @Disabled("this test is still incomplete")
    public void testShortest()
    {
        Morse morse = new Morse(20, 20, 0, 0);
        String[] items = morse.allowed(true, true);
        TreeSet<Item> set = new TreeSet<>();
        int[] data = new int[4];
        
        rotate(0, data, items, set);
        set.forEach((x) -> System.out.println(x));
    }
    
    static final Morse MORSE = new Morse(20, 20, 0, 0);
    
    static void rotate(int index, int[] data, String[] items, Set<Item> set)
    {
        if(index>= data.length) return;
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<index;i++)
        {
            sb.append(items[data[i]]);
        }
        String s = sb.toString();
        int[] pattern = MORSE.join(MORSE.encodeUnits(s), false);
        int units = (int) Nums.sum(pattern);
        set.add(new Item(s, units));
        
        for(int i=0;i<items.length;i++)
        {
            data[index] = i;
            rotate(index+1, data, items, set);
        }
    }

    /**
     * Test of setStartGapMultiplier method, of class Morse.
     */
    @Test
    public void testUpdateStartGap()
    {
        
        Morse instance = new Morse(12, 12, 0, 0);

        int[] pattern = instance.encodePattern("A");
        assertEquals(0, pattern[0]);
        assertEquals(0, instance.startGapMillis);

        instance.updateStartGap(2);

        pattern = instance.encodePattern("A");
        assertEquals(instance.wordGapMillis * 2, pattern[0]);
        assertEquals(instance.wordGapMillis * 2, instance.startGapMillis);
    }

    
}
