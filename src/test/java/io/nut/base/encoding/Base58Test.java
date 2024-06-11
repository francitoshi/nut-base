/*
 * Base58Test.java
 *
 * Copyright (c) 2022-2024 francitoshi@gmail.com
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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
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
public class Base58Test
{

    public Base58Test()
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
     * Test of doEncode method, of class Base58.
     */
    @Test
    public void testEncode_byteArr()
    {
        // empty byte array
        assertEquals("", Base58.encode(new byte[0]));

        //zeroByteArrayLength1
        assertEquals("1", Base58.encode(new byte[1]));

        //zeroByteArrayLength2
        assertEquals("11", Base58.encode(new byte[2]));

        //zeroSecond
        assertEquals("5R", Base58.encode(new byte[]
        {
            1, 0
        }));

        //twoFiveFiveByteArrayLength1
        assertEquals("5Q", Base58.encode(new byte[]
        {
            (byte) 255
        }));

        //twoFiveFiveByteArrayLength2
        assertEquals("LUv", Base58.encode(new byte[]
        {
            (byte) 255, (byte) 255
        }));

        //allByteValues        
        {
            byte[] bytes = new byte[256];
            for (int i = 0; i < bytes.length; i++)
            {
                bytes[i] = (byte) i;
            }
            String expected = "1cWB5HCBdLjAuqGGReWE3R3CguuwSjw6RHn39s2yuDRTS5NsBgNiFpWgAnEx6VQi8csexkgYw3mdYrMHr8x9i7aEwP8kZ7vccXWqKDvGv3u1GxFKPuAkn8JCPPGDMf3vMMnbzm6Nh9zh1gcNsMvH3ZNLmP5fSG6DGbbi2tuwMWPthr4boWwCxf7ewSgNQeacyozhKDDQQ1qL5fQFUW52QKUZDZ5fw3KXNQJMcNTcaB723LchjeKun7MuGW5qyCBZYzA1KjofN1gYBV3NqyhQJ3Ns746GNuf9N2pQPmHz4xpnSrrfCvy6TVVz5d4PdrjeshsWQwpZsZGzvbdAdN8MKV5QsBDY";
            assertEquals(expected, Base58.encode(bytes));
        }

        //largeCase
        {
            byte[] bytes = new byte[2048];
            new Random(1234).nextBytes(bytes);
            String expected = "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";
            assertEquals(expected, Base58.encode(bytes));
        }

        byte[] testbytes = "Hello World".getBytes();
        assertEquals("JxF12TrwUP45BMd", Base58.encode(testbytes));

        BigInteger bi = BigInteger.valueOf(3471844090L);
        assertEquals("16Ho7Hs", Base58.encode(bi.toByteArray()));

        byte[] zeroBytes1 = new byte[1];
        assertEquals("1", Base58.encode(zeroBytes1));

        byte[] zeroBytes7 = new byte[7];
        assertEquals("1111111", Base58.encode(zeroBytes7));

        // test empty encode
        assertEquals("", Base58.encode(new byte[0]));

    }

    @Test
    public void testDecode() throws Exception
    {
        byte[] testbytes = "Hello World".getBytes();
        byte[] actualbytes = Base58.decode("JxF12TrwUP45BMd");
        assertTrue(Arrays.equals(testbytes, actualbytes), new String(actualbytes));

        assertTrue(Arrays.equals(Base58.decode("1"), new byte[1]), "1");
        assertTrue(Arrays.equals(Base58.decode("1111"), new byte[4]), "1111");

        // Test decode of empty String.
        assertEquals(0, Base58.decode("").length);
    }

    @Test
    public void testDecode_invalidBase58() throws Base58.FormatException
    {
        Exception exception = assertThrows(Base58.FormatException.class, () -> Base58.decode("This isn't valid base58"));
        
    }

    @Test
    public void testDecodeToBigInteger() throws Base58.FormatException
    {
        byte[] input = Base58.decode("129");
        assertEquals(new BigInteger(1, input), Base58.decodeToBigInteger("129"));
    }

    /**
     * Test of decodeChecked method, of class Base58.
     */
    @Test
    public void testDecodeChecked() throws Exception
    {
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", true);
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62j", false);
        assertBitcoin("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nK9", true);
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62X", false);
        assertBitcoin("1ANNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", false);
        assertBitcoin("1A Na15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", false);
        assertBitcoin("BZbvjr", false);
        assertBitcoin("i55j", false);
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62!", false);
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62iz", false);
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62izz", false);
        assertBitcoin("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nJ9", false);
        assertBitcoin("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62I", false);
    }
    
    private static void assertBitcoin(String address, boolean valid) 
    {
        try
        {
            Base58.decodeChecked(address);
            if(!valid)
            {
                throw new AssertionError(String.format("invalid address not detected %s",address));
            }
        }
        catch(Exception ex)
        {
            if(valid)
            {
                throw new AssertionError(String.format("valid address not detected %s",address));
            }
        }
    }
}
