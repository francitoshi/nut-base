/*
 *  AtbashTest.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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

import static io.nut.base.util.Assert.assertEquals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class AtbashTest
{
    
    public AtbashTest()
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
     * Test of getCodex method, of class BlaBlaBla.
     */
    @Test
    public void testGetCodex()
    {
        String ABC = "ABCDEFGHIJKLM NOPQRSTUVWXYZ 01234 56789";
        String ZYX = "ZYXWVUTSRQPON MLKJIHGFEDCBA 98765 43210";
         
        Atbash instance = new Atbash(0, false);
        assertEquals(ZYX, instance.get(ABC));
        assertEquals(ABC, instance.get(ZYX));

        instance = new Atbash(0, true);
        assertEquals(ZYX.toUpperCase(), instance.get(ABC.toLowerCase()));
        assertEquals(ABC.toUpperCase(), instance.get(ZYX.toLowerCase()));

        assertEquals(ZYX.toLowerCase(), instance.get(ABC.toUpperCase()));
        assertEquals(ABC.toLowerCase(), instance.get(ZYX.toUpperCase()));
        
        String abc = "AB CD EF GH IJ KL MN OP QR ST UV WX YZ 01 23 45 67 89";
        System.out.println();
        System.out.println("  "+abc);
        for(int i=0;i<10;i++)
        {
            Atbash bbb = new Atbash(i, false);
            System.out.printf("%d %s\n", i, bbb.get(abc).toUpperCase());
        }
        
    }

       
}
