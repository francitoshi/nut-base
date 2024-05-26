/*
 *  DERTest.java
 *
 *  Copyright (C) 2023 francitoshi@gmail.com
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
package io.nut.base.crypto.ec;

import io.nut.base.encoding.Hex;
import java.math.BigInteger;
import java.security.InvalidParameterException;
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
public class DERTest
{
    
    public DERTest()
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

    //der encoded (with SIGHASH):
    //type: 30
    //  length: 44
    //  type:   02
    //    length: 20
    //    r:      08f4f37e2d8f74e18c1b8fde2374d5f28402fb8ab7fd1cc5b786aa40851a70cb
    //  type:   02
    //    length: 20
    //    s:      1f40afd1627798ee8529095ca4b205498032315240ac322c9d8ff0f205a93a58
    //sighash: 01

    static final String DER_R0 = "08f4f37e2d8f74e18c1b8fde2374d5f28402fb8ab7fd1cc5b786aa40851a70cb";
    static final String DER_S0 = "1f40afd1627798ee8529095ca4b205498032315240ac322c9d8ff0f205a93a58";
    static final String DER_EXPECTED0 = "3044022008f4f37e2d8f74e18c1b8fde2374d5f28402fb8ab7fd1cc5b786aa40851a70cb02201f40afd1627798ee8529095ca4b205498032315240ac322c9d8ff0f205a93a5801";
    
    static final String DER_R1 = "3ff7162d6635246dbf59b7fa9e72e3023e959a73b1fbc51edbaaa5a8dbc6d2f7";
    static final String DER_S1 = "776e2fa5740df01cc0ac47bda713e87fc59044960122ba45abb11c949655c584";
    static final String DER_EXPECTED1 = "304402203ff7162d6635246dbf59b7fa9e72e3023e959a73b1fbc51edbaaa5a8dbc6d2f70220776e2fa5740df01cc0ac47bda713e87fc59044960122ba45abb11c949655c58401";
    
    
    /**
     * Test of der method, of class Sign.
     */
    @Test
    public void testEncode0()
    {
        BigInteger R = new BigInteger(DER_R0,16);
        BigInteger S = new BigInteger(DER_S0,16);

        //https://learnmeabitcoin.com/technical/ecdsa
        String result = Hex.encode(DER.encode(R, S));
        
        assertEquals(DER_EXPECTED0, result);

        byte[] r = Hex.decode(DER_R0);
        byte[] s = Hex.decode(DER_S0);

        String result2 = Hex.encode(DER.encode(r, s));
        
        assertEquals(DER_EXPECTED0, result2);
    }
    /**
     * Test of der method, of class Sign.
     */
    @Test
    public void testEncode1()
    {
        BigInteger R = new BigInteger(DER_R1,16);
        BigInteger S = new BigInteger(DER_S1,16);

        //https://learnmeabitcoin.com/technical/ecdsa
        String result = Hex.encode(DER.encode(R, S));
        
        assertEquals(DER_EXPECTED1, result);

        byte[] r = Hex.decode(DER_R1);
        byte[] s = Hex.decode(DER_S1);

        String result2 = Hex.encode(DER.encode(r, s));
        
        assertEquals(DER_EXPECTED1, result2);
    }

    /**
     * Test of der method, of class Sign.
     */
    @Test
    public void testDecode0() throws InvalidParameterException
    {
        assertNull(DER.decode(null));

        BigInteger[] rs = DER.decode(Hex.decode(DER_EXPECTED0));
        
        assertEquals(2, rs.length);
        
        String r = rs[0].toString(16);
        String s = rs[1].toString(16);
        
        while(r.length()< DER_R0.length())
        {
            r = '0'+r;
        }
        while(s.length()< DER_S0.length())
        {
            s = '0'+s;
        }
        
        assertEquals(DER_R0, r);
        assertEquals(DER_S0, s);
    }  
    /**
     * Test of der method, of class Sign.
     */
    @Test
    public void testDecode1() throws InvalidParameterException
    {
        assertNull(DER.decode(null));

        BigInteger[] rs = DER.decode(Hex.decode(DER_EXPECTED1));
        
        assertEquals(2, rs.length);
        
        String r = rs[0].toString(16);
        String s = rs[1].toString(16);
        
        while(r.length()< DER_R1.length())
        {
            r = '0'+r;
        }
        while(s.length()< DER_S1.length())
        {
            s = '0'+s;
        }
        
        assertEquals(DER_R1, r);
        assertEquals(DER_S1, s);
    }  
    
}
