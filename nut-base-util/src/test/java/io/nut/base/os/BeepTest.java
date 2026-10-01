/*
 *  BeepTest.java
 *
 *  Copyright (c) 2020-2025 francitoshi@gmail.com
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
package io.nut.base.os;

import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BeepTest
{
    
    /**
     * Test of beep method, of class Beep.
     */
    @Test
    public void testBeep()
    {
        Beep.beep();
        double[] f = {440, 220, 440, 220, 440, 220};
        int[] ms = {1000, 500, 1000, 500,1000, 500};
        
        Beep.beep(f,ms);
    }
    
}
