/*
 *  HexTest.java
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
package io.nut.base.encoding;

import java.security.NoSuchAlgorithmException;
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
public class HexTest
{
    
    public HexTest()
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

    static final byte[] DATA1 = {0x00,0x01,0x12,0x23,0x34,0x45,0x56,0x67,0x78,0x0a,0x0b,0x1c,0x2d,0x3e,0x4f};
    static final byte[] DATA2 = {0,1,1,2,3,5,8,0xD,0x15,0x22,0x37,0x59,0x1a,0x2b,0x3c,0x4d,0x5e,0x6f};
    /**
     * Test of encode method, of class Hex.
     */
    @Test
    public void testEncode_byteArr_boolean()
    {
        assertEquals("0001122334455667780a0b1c2d3e4f", Hex.encode(DATA1, false));
        assertEquals("0001122334455667780A0B1C2D3E4F", Hex.encode(DATA1, true));
    
        assertEquals("000101020305080d152237591a2b3c4d5e6f", Hex.encode(DATA2, false));
        assertEquals("000101020305080D152237591A2B3C4D5E6F", Hex.encode(DATA2, true));
    }

    /**
     * Test of encode method, of class Hex.
     */
    @Test
    public void testEncode_byteArr()
    {
        assertEquals("0001122334455667780a0b1c2d3e4f", Hex.encode(DATA1));
        assertEquals("000101020305080d152237591a2b3c4d5e6f", Hex.encode(DATA2));    
    }

    /**
     * Test of decode method, of class Hex.
     */
    @Test
    public void testDecode() throws NoSuchAlgorithmException
    {
        assertArrayEquals(DATA1, Hex.decode("0001122334455667780a0b1c2d3e4f"));
        assertArrayEquals(DATA1, Hex.decode("0001122334455667780A0B1C2D3E4F"));
        assertArrayEquals(DATA2, Hex.decode("000101020305080d152237591a2b3c4d5e6f"));
        assertArrayEquals(DATA2, Hex.decode("000101020305080D152237591A2B3C4D5E6F"));
    }
    
}
