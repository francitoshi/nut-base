/*
 * Copyright (C) 2024 franctoshi@gmail.com
 * Copyright (C) 2017-2020 Jakob Nixdorf
 * Copyright (C) 2015 Bruno Bierbaumer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nut.base.crypto.otp;

//copied from
//https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/Utilities/TokenCalculator.java

import io.nut.base.encoding.Base32String;
import io.nut.base.encoding.Hex;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class OTP
{
    public static final int TOTP_DEFAULT_PERIOD = 30;
    public static final int TOTP_DEFAULT_DIGITS = 6;
    public static final int HOTP_INITIAL_COUNTER = 1;

    public enum Hmac
    {
        HmacSHA1(20), HmacSHA256(32), HmacSHA512(64);
        private Hmac(int bytes)
        {
            this.bytes = bytes;
        }
        final int bytes;
    }

    private final Hmac hmac;
    private final boolean lenient;

    public OTP(Hmac hmac, boolean lenient)
    {
        this.hmac = hmac;
        this.lenient = lenient;
    }

    public OTP(Hmac hmac)
    {
        this(hmac, false);
    }

    public OTP()
    {
        this(Hmac.HmacSHA1,false);
    }
    
    private byte[] generateHash(byte[] key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException
    {
        Mac mac = Mac.getInstance(hmac.name());
        mac.init(new SecretKeySpec(key, "RAW"));
        return mac.doFinal(data);
    }

    public static String formatTokenString(int token, int digits) 
    {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        numberFormat.setMinimumIntegerDigits(digits);
        numberFormat.setGroupingUsed(false);
        return numberFormat.format(token);
    }    
    public static byte[] decodeBase32(String sharedSecret) throws Base32String.DecodingException 
    {
        sharedSecret = sharedSecret.replace('0', 'O').replace('1', 'I');
        return Base32String.decode(sharedSecret);
    }
    
    private int token(byte[] key, long counter, int digits)
    {
        if(key.length<16)
        {
            throw new IllegalArgumentException("key size should be >16 bytes.");
        }
        
        if(!this.lenient && key.length!=this.hmac.bytes)
        {
            throw new IllegalArgumentException("key size should be "+this.hmac.bytes+" bytes.");
        }
        
        try
        {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            
            byte[] hash = generateHash(key, data);

            int offset = hash[hash.length - 1] & 0xF;

            int binary = (hash[offset] & 0x7F) << 0x18;
            binary |= (hash[offset + 1] & 0xFF) << 0x10;
            binary |= (hash[offset + 2] & 0xFF) << 0x08;
            binary |= (hash[offset + 3] & 0xFF);

            int div = (int) Math.pow(10, digits);
            return (binary % div);            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }
    public String HOTP(byte[] key, long counter, int digits)
    {
        return formatTokenString(token(key, counter, digits), digits);
    }    
    public String HOTP(byte[] key, long counter)
    {
        return HOTP(key, counter, TOTP_DEFAULT_DIGITS);
    }

    public String HOTP(String sharedSecret, long counter) throws Base32String.DecodingException
    {
        return HOTP(decodeBase32(sharedSecret), counter);
    }

    public String TOTP(byte[] key, int period, long time)
    {
        return HOTP(key, (time / period), TOTP_DEFAULT_DIGITS);
    }

    public String TOTP(byte[] key, int period, long time, int digits)
    {
        return HOTP(key, (time / period), digits);
    }

    public String TOTP(String sharedSecret, int period, long time) throws Base32String.DecodingException
    {
        return TOTP(decodeBase32(sharedSecret), period, time);
    }

    private static final int STEAM_DEFAULT_DIGITS = 5;
    
    private static final char[] STEAMCHARS = new char[]
    {
        '2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C',
        'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
        'R', 'T', 'V', 'W', 'X', 'Y'
    };

    public String SteamTOTP(byte[] secret, int period, long time, int digits)
    {
        int fullToken = token(secret, (time / period), digits);

        StringBuilder tokenBuilder = new StringBuilder();

        for (int i = 0; i < digits; i++)
        {
            tokenBuilder.append(STEAMCHARS[fullToken % STEAMCHARS.length]);
            fullToken /= STEAMCHARS.length;
        }

        return tokenBuilder.toString();
    }

    public static String MOTP(String PIN, String secret, long epoch, int offset)
    {
        String epochText = String.valueOf((epoch / 10) + offset);
        String hashText = epochText + secret + PIN;
        String otp = "";

        try
        {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(hashText.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            String hexString = new String(Hex.encode(messageDigest));
            otp = hexString.substring(0, 6);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        return otp;
    }
}
