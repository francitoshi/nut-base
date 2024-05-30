/*
 * TokenCalculatorTest.java
 *
 * Copyright (c) 2021-2023 francitoshi@gmail.com
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
package io.nut.base.crypto.otp;

import io.nut.base.encoding.Base32String;
import io.nut.base.encoding.Base64DecoderException;
import io.nut.base.time.JavaTime;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
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
public class TokenCalculatorTest
{
    
    public TokenCalculatorTest()
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
    static final String SECRET = "12345678901234567890";
    static String[][] TOTP_DATA =
    {
        //|  Time (sec) |   TOTP   |  Mode  |
        {"59",          "94287082", "SHA1"},
        {"59",          "46119246", "SHA256"},
        {"59",          "90693936", "SHA512"},
        {"1111111109",  "07081804", "SHA1"},
        {"1111111109",  "68084774", "SHA256"},
        {"1111111109",  "25091201", "SHA512"},
        {"1111111111",  "14050471", "SHA1"},
        {"1111111111",  "67062674", "SHA256"},
        {"1111111111",  "99943326", "SHA512"},
        {"1234567890",  "89005924", "SHA1"},
        {"1234567890",  "91819424", "SHA256"},
        {"1234567890",  "93441116", "SHA512"},
        {"2000000000",  "69279037", "SHA1"},
        {"2000000000",  "90698825", "SHA256"},
        {"2000000000",  "38618901", "SHA512"},
        {"20000000000", "65353130", "SHA1"},
        {"20000000000", "77737706", "SHA256"},
        {"20000000000", "47863826", "SHA512"},
    };
 
    /**
     * Test of TOTP_RFC6238 method, of class TokenCalculator.
     */
    @Test
    public void testTOTP_RFC6238_5args()
    {
        byte[] secret = "12345678901234567890".getBytes();
        for(int i=0;i<TOTP_DATA.length;i++)
        {
            long time = Long.parseLong(TOTP_DATA[i][0]);
            long expe = Long.parseLong(TOTP_DATA[i][1]);
            TokenCalculator.HashAlgorithm algorithm = TokenCalculator.HashAlgorithm.valueOf(TOTP_DATA[i][2]);
            
            int result = TokenCalculator.TOTP_RFC6238(secret, 30, time, 8, algorithm);
            assertEquals(expe, result, "i="+i);
        }
    }

    /**
     * Test of HOTP method, of class TokenCalculator.
     * https://tools.ietf.org/html/rfc4226
     */
    @Test
    public void testHOTP()
    {
        String[] HOTP = {"755224", "287082", "359152", "969429", "338314", "254676", "287922", "162583", "399871", "520489"};
        
        byte[] secret = "12345678901234567890".getBytes();
        for(int i=0;i<HOTP.length;i++)
        {
            String result = TokenCalculator.HOTP(secret, i, 6, TokenCalculator.HashAlgorithm.SHA1);
            assertEquals(HOTP[i], result);
        }
    }
    
    @Test
    public void howToUse() throws InvalidKeyException, Base32String.DecodingException, NoSuchAlgorithmException, GeneralSecurityException, Base64DecoderException, UnsupportedEncodingException
    {
        String sharedSecret = "JBSWY3DPEHPK3PXP";
        sharedSecret = sharedSecret.replace('0', 'O').replace('1', 'I');
        byte[] sharedSecretBytes = Base32String.decode(sharedSecret);
        final long counter = JavaTime.epochSecond()/TokenCalculator.TOTP_DEFAULT_PERIOD;
        
        String otpResult = TokenCalculator.HOTP(sharedSecretBytes, counter, TokenCalculator.TOTP_DEFAULT_DIGITS, TokenCalculator.HashAlgorithm.SHA1);
        
        System.out.println(otpResult);
        System.out.println(OTP.TOTP(sharedSecret, 30, JavaTime.epochSecond()));
    }
    
}
