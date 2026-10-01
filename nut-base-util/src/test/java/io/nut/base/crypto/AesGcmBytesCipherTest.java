/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.kdf.PBKDF2;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class AesGcmBytesCipherTest
{
    
    @Test
    public void test() throws Exception
    {
        char[] passphrase = "passphrase".toCharArray();
        byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);
        
        Kripto kripto = Kripto.getInstance(true);
        PBKDF2 pbkdf2 = kripto.pbkdf2WithSha256;
        SecretKey key = pbkdf2.deriveSecretKey(passphrase, salt, 8, 256, SecretKeyAlgorithm.AES);
        
        AesGcmBytesCipher instance = new AesGcmBytesCipher(key);

        byte[] plaintext = "plaintext".getBytes(StandardCharsets.UTF_8);
        
        int num = 100;
        String[] s = new String[num];
        
        for(int i=0;i<num;i++)
        {
            byte[] ciphertext = instance.encrypt(plaintext);
            byte[] resulttext = instance.decrypt(ciphertext);
            assertArrayEquals(plaintext, resulttext);
            
            s[i] = Base64.getEncoder().encodeToString(ciphertext).substring(0, 8);
        }
        
        Arrays.sort(s);

        for(int i=0;i<num-1;i++)
        {
            assertNotEquals(s[i],s[i+1]);
        }        
        
    }

    
}
