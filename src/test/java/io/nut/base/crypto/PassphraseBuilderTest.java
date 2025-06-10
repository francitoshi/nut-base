/*
 *  PassphraseBuilderTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PassphraseBuilderTest
{
    @Test
    public void testGenerate()
    {
        PassphraseBuilder instance = new PassphraseBuilder(0, 0, 0, 0);
        
        for(int i=1;i<100;i++)
        {
            char[] pass = instance.generate(i);
            assertEquals(i, pass.length);
        }
        
        for(int i=1;i<20;i++)
        {
            instance = new PassphraseBuilder(i, i, i, i);
            char[] pass = instance.generate(i*4);
            String s = new String(pass);
            
            assertEquals(pass.length-i, s.replaceAll("["+PassphraseBuilder.UPPERCASE+"]", "").length());
            assertEquals(pass.length-i, s.replaceAll("["+PassphraseBuilder.LOWERCASE+"]", "").length());
            assertEquals(pass.length-i, s.replaceAll("["+PassphraseBuilder.NUMBERS+"]", "").length());
            assertEquals(pass.length-i, s.replaceAll("["+PassphraseBuilder.SPECIAL+"]", "").length());
        }
    }

    
}
