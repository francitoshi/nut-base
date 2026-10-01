/*
 *  WiperTest.java
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
package io.nut.base.security;

import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WiperTest
{
    
    @Test
    public void testWipeSecretKeySpec()
    {
        byte[] zero = new byte[32];
        byte[] ones = new byte[32];

        Arrays.fill(ones, (byte)1);

        SecretKeySpec k0 = new SecretKeySpec(ones, "AES");
        assertTrue(Wiper.wipeSecretKeySpec(k0));
        assertArrayEquals(zero, k0.getEncoded());
    }

    @Test
    public void testWipeSecretKey()
    {
        byte[] zero = new byte[32];
        byte[] ones = new byte[32];

        Arrays.fill(ones, (byte)1);

        SecretKey k1 = new SecretKeySpec(ones, "AES");
        assertTrue(Wiper.wipeSecretKey(k1));
        assertArrayEquals(zero, k1.getEncoded());
    }
    
}
