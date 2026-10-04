/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
        byte[] key = new byte[32];
        new Random().nextBytes(key);
        SecureWrapper wrapper = new SecureWrapper(key);

        String ciphertext1 = wrapper.wrap(plaintext, info);
        String ciphertext2 = wrapper.wrap(plaintext, info);

        assertFalse(ciphertext1.equals(ciphertext2));
        
        byte[] result1 = wrapper.unwrap(ciphertext1, info);
        byte[] result2 = wrapper.unwrap(ciphertext2, info);
        
        assertArrayEquals(plaintext, result1);
        assertArrayEquals(plaintext, result2);

    }
    
}
