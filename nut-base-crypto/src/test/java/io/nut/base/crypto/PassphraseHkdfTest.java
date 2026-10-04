/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

        try (PassphraserHkdf passphraser = kripto.getPassphraserHkdf(kripto.getHkdfWithSha256(), key, salt))
        {
            
            char[] pass1 = passphraser.chars("database-key");
            char[] pass2 = passphraser.chars("database-key");
            char[] apiKey = passphraser.chars("external-api-key");
            assertArrayEquals(pass1, pass2);
            assertFalse(Arrays.equals(pass1, apiKey));
        }
        try (PassphraserHkdf passphraser = kripto.getPassphraserHkdf(kripto.getHkdfWithSha512(), key, salt))
        {
            
            char[] pass1 = passphraser.chars("database-key");
            char[] pass2 = passphraser.chars("database-key");
            char[] apiKey = passphraser.chars("external-api-key");
            assertArrayEquals(pass1, pass2);
            assertFalse(Arrays.equals(pass1, apiKey));
        }
    }   
}
