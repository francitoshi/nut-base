/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.kdf;

import io.nut.base.crypto.Kripto;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PBKDF2Test
{
    
    @Test
    public void testDerive() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
    {
        Kripto kripto = Kripto.getInstance();
        PBKDF2 pbkdf2 = kripto.pbkdf2WithSha256;
        String plainText = "this is the plaintext";
        char[] passphrase = "this is the key".toCharArray();
        
        byte[] salt = kripto.deriveSaltSHA256("test"+"salt");
        byte[] iv32 = kripto.deriveSaltSHA256("test"+"iv");

        SecretKey key = pbkdf2.deriveSecretKeyAES(passphrase, salt, 2048, 256);
        
        IvParameterSpec iv = kripto.getIv(iv32,128);
        byte[] encryptedBytes = kripto.encrypt(key, Kripto.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, plainText.getBytes());

        byte[] restoredBytes = kripto.decrypt(key, Kripto.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, encryptedBytes);

        String restoredText = new String(restoredBytes);
        
        assertEquals(plainText, restoredText);

    }
    @Test
    public void testCalibrate() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
    {
        long secToLoop = 5;
        
        long untilNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(secToLoop);
        
        Kripto kripto = Kripto.getInstance();
        PBKDF2[] pbkdf2 = { kripto.pbkdf2WithSha256, kripto.pbkdf2WithSha512};
                
        for(int i=1;i<6 && System.nanoTime()<untilNanos;i++)
        {
            int ms = i*200;
            for(PBKDF2 derive : pbkdf2)
            {
                int rounds = kripto.pbkdf2WithSha256.calibrateRounds(ms);
                System.out.printf("%s %d ms = %d rounds\n", derive.algorithm.name(), ms, rounds);
            }
        }
        
    }    
    
}
