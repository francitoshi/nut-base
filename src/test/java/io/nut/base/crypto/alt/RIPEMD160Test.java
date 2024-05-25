/*
 *  RIPEMD160Test.java
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
package io.nut.base.crypto.alt;

import io.nut.base.encoding.Hex;
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

