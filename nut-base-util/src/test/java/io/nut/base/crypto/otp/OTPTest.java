/*
 * OTPTest.java
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
import io.nut.base.encoding.Hex;
import io.nut.base.time.JavaTime;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class OTPTest
{

    static final String SECRET20 = "12345678901234567890";
    static final String SECRET32 = "12345678901234567890123456789012";
    static final String SECRET64 = "1234567890123456789012345678901234567890123456789012345678901234";
    static final String[] SECRETS = {SECRET20, SECRET32, SECRET64};
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
     * Test of TOTP_RFC6238 method, of class OTP.
     */
    @Test
    public void testTOTP_RFC6238_5args() throws Base32String.DecodingException
    {
        for(int i=0;i<TOTP_DATA.length;i++)
        {
            byte[] secret = SECRETS[i%3].getBytes();
            long time = Long.parseLong(TOTP_DATA[i][0]);
            String exps = TOTP_DATA[i][1];
            String hash = TOTP_DATA[i][2];
            OTP.Hmac hmac = OTP.Hmac.valueOf("Hmac"+hash);
            
            OTP otp = new OTP(hmac, false);
            
            String res = otp.TOTP(secret, 30, time, 8);
            
            assertEquals(exps, res, "i2="+i);
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
        
        OTP otp = new OTP();
        
        byte[] secret = "12345678901234567890".getBytes();
        for(int i=0;i<HOTP.length;i++)
        {
            String result = otp.HOTP(secret, i, 6);
            assertEquals(HOTP[i], result);
        }
    }
    
    @Test
    public void howToUse() throws InvalidKeyException, Base32String.DecodingException, NoSuchAlgorithmException, GeneralSecurityException, Base64DecoderException, UnsupportedEncodingException
    {
        String sharedSecret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        int counter = (int) (JavaTime.epochSecond()/OTP.TOTP_DEFAULT_PERIOD);

        OTP otp = new OTP(OTP.Hmac.HmacSHA1, true);
        
        String otpResult = otp.HOTP(sharedSecret, counter);
        
        System.out.println(otpResult);
        System.out.println(otp.TOTP(sharedSecret, 30, JavaTime.epochSecond()));
    }
    
}
