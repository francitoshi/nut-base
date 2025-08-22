/*
 *  PassphraseHkdfTest.java
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
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PassphraseHkdfTest
{
    
    @Test
    public void testGetKey() throws Exception
    {
        byte[] key = "my-super-secret-password-123".getBytes(StandardCharsets.UTF_8);
        byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);
        Kripto kripto = Kripto.getInstance();

        try (PassphraserHkdf passphraser = kripto.getPassphraserHkdf(kripto.hkdfWithSha256, key, salt))
        {
            
            char[] pass1 = passphraser.chars("database-key");
            char[] pass2 = passphraser.chars("database-key");
            char[] apiKey = passphraser.chars("external-api-key");
            assertArrayEquals(pass1, pass2);
            assertFalse(Arrays.equals(pass1, apiKey));
        }
        try (PassphraserHkdf passphraser = kripto.getPassphraserHkdf(kripto.hkdfWithSha512, key, salt))
        {
            
            char[] pass1 = passphraser.chars("database-key");
            char[] pass2 = passphraser.chars("database-key");
            char[] apiKey = passphraser.chars("external-api-key");
            assertArrayEquals(pass1, pass2);
            assertFalse(Arrays.equals(pass1, apiKey));
        }
    }   
}
