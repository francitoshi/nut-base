/*
 * HKDFBCTest.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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

import io.nut.base.encoding.Hex;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class HKDFBCTest
{
    
    @Test
    public void testGenerateBytes()
    {
        HKDFBC hkdf256 = new HKDFBC(Kripto.Hkdf.HkdfWithSha256);
        {
            byte[] ikm = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
            byte[] salt = Hex.decode("000102030405060708090a0b0c");
            byte[] info = Hex.decode("f0f1f2f3f4f5f6f7f8f9");
            int keyLengthBytes = 42;
            byte[] expResult = Hex.decode("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865");
            byte[] result = hkdf256.deriveBytes(ikm, salt, info, keyLengthBytes);
            assertArrayEquals(expResult, result);
        }
        {
            byte[] ikm = Hex.decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f");
            byte[] salt = Hex.decode("606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeaf");
            byte[] info = Hex.decode("b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
            int keyLengthBytes = 82;
            byte[] expResult = Hex.decode("b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71cc30c58179ec3e87c14c01d5c1f3434f1d87");
            byte[] result = hkdf256.deriveBytes(ikm, salt, info, keyLengthBytes);
            assertArrayEquals(expResult, result);
        }
        {
            byte[] ikm = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
            byte[] salt = Hex.decode("");
            byte[] info = Hex.decode("");
            int keyLengthBytes = 42;
            byte[] expResult = Hex.decode("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8");
            byte[] result = hkdf256.deriveBytes(ikm, salt, info, keyLengthBytes);
            assertArrayEquals(expResult, result);
        }
        {
            byte[] ikm = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
            byte[] salt = Hex.decode("");
            byte[] info = Hex.decode("");
            int keyLengthBytes = 42;
            byte[] expResult = Hex.decode("8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8");
            byte[] result = hkdf256.deriveBytes(ikm, salt, info, keyLengthBytes);
            assertArrayEquals(expResult, result);
        }
        HKDFBC hkdf512 = new HKDFBC(Kripto.Hkdf.HkdfWithSha512);
        {
            byte[] ikm = Hex.decode("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b");
            byte[] salt = Hex.decode("000102030405060708090a0b0c");
            byte[] info = Hex.decode("f0f1f2f3f4f5f6f7f8f9");
            int keyLengthBytes = 42;
            byte[] expResult = Hex.decode("832390086cda71fb47625bb5ceb168e4c8e26a1a16ed34d9fc7fe92c1481579338da362cb8d9f925d7cb");
            byte[] result = hkdf512.deriveBytes(ikm, salt, info, keyLengthBytes);
            assertArrayEquals(expResult, result);
        }
    }

    
}
