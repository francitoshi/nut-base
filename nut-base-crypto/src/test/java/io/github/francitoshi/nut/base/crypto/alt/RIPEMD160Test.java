/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto.alt;

import io.github.francitoshi.nut.base.commons.encoding.Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * JUnit test with example data for RIPEMD160.
 * * Test data comes from http://homes.esat.kuleuven.be/~bosselae/ripemd160.html
 */
public class RIPEMD160Test 
{
    @Test
    public void testWithLength0() 
    {
        assertEquals("9c1185a5c5e9fc54612808977ee8f548b2258d31", Hex.encode(new RIPEMD160().digest("".getBytes()) ));
        assertEquals("0bdc9d2d256b3ee9daae347be6f4dc835a467ffe", Hex.encode(new RIPEMD160().digest("a".getBytes()) ));
        assertEquals("8eb208f7e05d987a9b044a8e98c6b087f15a0bfc", Hex.encode(new RIPEMD160().digest("abc".getBytes()) ));
        assertEquals("5d0689ef49d2fae572b881b123a85ffa21595f36", Hex.encode(new RIPEMD160().digest("message digest".getBytes()) ));
        assertEquals("9b752e45573d4b39f4dbd3323cab82bf63326bfb", Hex.encode(new RIPEMD160().digest("12345678901234567890123456789012345678901234567890123456789012345678901234567890".getBytes()) ));
    }
}

