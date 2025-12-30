/*
 * MorseTest.java
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
package io.nut.base.morse;

import static io.nut.base.morse.Morse.DAH;
import static io.nut.base.morse.Morse.DIT;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    
    static final String ABC = "abcdefghijklmnopqrstuvwxyz.,0123456789";
    static final String ABC_MORSE = ".- -... -.-. -.. . ..-. --. .... .. .--- -.- .-.. -- -. --- .--. --.- .-. ... - ..- ...- .-- -..- -.-- --.. .-.-.- --..-- ----- .---- ..--- ...-- ....- ..... -.... --... ---.. ----.";
    
    static final String A_B_C = "a b c d e f g h i j k l m n o p q r s t u v w x y z . , 0 1 2 3 4 5 6 7 8 9";
    static final String A_B_C_MORSE = ".- / -... / -.-. / -.. / . / ..-. / --. / .... / .. / .--- / -.- / .-.. / -- / -. / --- / .--. / --.- / .-. / ... / - / ..- / ...- / .-- / -..- / -.-- / --.. / .-.-.- / --..-- / ----- / .---- / ..--- / ...-- / ....- / ..... / -.... / --... / ---.. / ----.";
    
    static final String PROSIGNS = "<CT> HELLO WORLD <SK>";
    static final String PROSIGNS_MORSE = "-.-.- / .... . .-.. .-.. --- / .-- --- .-. .-.. -.. / ...-.-";
    
    /**
     * Test of encode method, of class Morse.
     */
    @Test
    public void testEncodeJoin()
    {
        Morse morse = new Morse();
        
        assertTrue(Arrays.deepEquals(H_E_L_L_O_W_O_R_L_D, morse.encode(HELLO_WORLD)));
        assertEquals(HELLO_WORLD_MORSE, morse.join(morse.encode(HELLO_WORLD)));
        assertEquals(ABC_MORSE, morse.join(morse.encode(ABC)));
        assertEquals(A_B_C_MORSE, morse.join(morse.encode(A_B_C)));
        assertEquals(PROSIGNS_MORSE, morse.join(morse.encode(PROSIGNS)));
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
    }

}
