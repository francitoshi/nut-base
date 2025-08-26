/*
 *  SecureWrapperTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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

import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SecureWrapperTest
{
 
    @Test
    public void testWrapUnwrap() throws Exception
    {
        byte[] plaintext = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] info = "info".getBytes(StandardCharsets.UTF_8);
        char[] pass = "pass".toCharArray();
        byte[] key = new byte[32];
        new Random().nextBytes(key);
        SecureWrapper wrapper = new SecureWrapper(Kripto.getInstanceBouncyCastle(), 128, 256);

        String ciphertext1 = wrapper.wrap(plaintext, pass);
        byte[] result1 = wrapper.unwrap(ciphertext1, pass);
        
        assertArrayEquals(plaintext, result1);
        
        String ciphertext2 = wrapper.wrap(plaintext, info, key);
        byte[] result2 = wrapper.unwrap(ciphertext2, info, key);
        
        assertArrayEquals(plaintext, result2);
    }
    
}
