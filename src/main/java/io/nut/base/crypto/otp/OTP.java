/*
 * OTP.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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

/**
 *
 * @author franci
 */
public class OTP
{
    public static String HOTP(String sharedSecret, long counter) throws Base32String.DecodingException
    {
        sharedSecret = sharedSecret.replace('0', 'O').replace('1', 'I');
        return HOTP(Base32String.decode(sharedSecret), counter);
    }
    public static String HOTP(byte[] sharedSecret, long counter)
    {
        return TokenCalculator.HOTP(sharedSecret, counter, TokenCalculator.TOTP_DEFAULT_DIGITS, TokenCalculator.HashAlgorithm.SHA1);
    }
    
    public static String TOTP(String sharedSecret, int period, long epochSecond) throws Base32String.DecodingException
    {
        sharedSecret = sharedSecret.replace('0', 'O').replace('1', 'I');
        return TOTP(Base32String.decode(sharedSecret), period, epochSecond);
    }
    public static String TOTP(byte[] sharedSecret, int period, long epochSecond)
    {
        long counter = epochSecond/period;
        return TokenCalculator.HOTP(sharedSecret, counter, TokenCalculator.TOTP_DEFAULT_DIGITS, TokenCalculator.HashAlgorithm.SHA1);
    }
}
