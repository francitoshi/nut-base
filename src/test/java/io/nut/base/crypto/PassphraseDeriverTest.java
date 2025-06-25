/*
 *  PassphraseDeriverTest.java
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

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PassphraseDeriverTest
{
    
    @Test
    public void testGetKey() throws Exception
    {
        char[] password = "my-super-secret-password-123".toCharArray();
        int keyBits = 256;
        int pbkdf2Iterations = 10;
        Kripto kripto = Kripto.getInstance().setMinimumPbkdf2Rounds(pbkdf2Iterations);

        try (PassphraseDeriver keyDeriver = new PassphraseDeriver(password, keyBits, pbkdf2Iterations, true, kripto))
        {
            
            byte[] dbKey1 = keyDeriver.getKey("database-key");
            byte[] dbKey2 = keyDeriver.getKey("database-key");
            byte[] apiKey = keyDeriver.getKey("external-api-key");
            assertArrayEquals(dbKey1, dbKey2);
            assertFalse(Arrays.equals(dbKey1, apiKey));
        }
        try (PassphraseDeriver keyDeriver = new PassphraseDeriver(password, keyBits, pbkdf2Iterations, false, kripto))
        {
            
            byte[] dbKey1 = keyDeriver.getKey("database-key");
            byte[] dbKey2 = keyDeriver.getKey("database-key");
            byte[] apiKey = keyDeriver.getKey("external-api-key");
            assertArrayEquals(dbKey1, dbKey2);
            assertFalse(Arrays.equals(dbKey1, apiKey));
        }
    }

    
}
