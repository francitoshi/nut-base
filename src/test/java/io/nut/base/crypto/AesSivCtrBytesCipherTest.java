/*
 *  AesSivCtrBytesCipherTest.java
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

import io.nut.base.crypto.Kripto.HMAC;
import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.Kripto.SecretKeyDerivation;
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
public class AesSivCtrBytesCipherTest
{
    
    @Test
    public void test() throws Exception
    {
        char[] passphrase = "passphrase".toCharArray();
        byte[] macSalt = "macSalt".getBytes(StandardCharsets.UTF_8);
        byte[] keySalt = "keySalt".getBytes(StandardCharsets.UTF_8);
        
        Kripto kripto = Kripto.getInstance(true).setMinimumPbkdf2Rounds(8);
        SecretKey hmacKey = kripto.deriveSecretKey(passphrase, macSalt, 8, 256, SecretKeyDerivation.PBKDF2WithHmacSHA256, SecretKeyAlgorithm.AES);

        int num = 10;
        String[] s = new String[num];
        
        for(int i=0;i<num;i++)
        {
            SecretKey key = kripto.deriveSecretKey(passphrase, keySalt, 8, 256, SecretKeyDerivation.PBKDF2WithHmacSHA256, SecretKeyAlgorithm.AES);
            AesSivCtrBytesCipher instance = new AesSivCtrBytesCipher(HMAC.HmacSHA256, hmacKey, key);
            
            byte[] plaintext = ("plaintext"+i).getBytes(StandardCharsets.UTF_8);
            
            byte[] ciphertext = instance.encrypt(plaintext);
            byte[] resulttext = instance.decrypt(ciphertext);
            assertArrayEquals(plaintext, resulttext);
            s[i] = Base64.getEncoder().encodeToString(ciphertext).substring(0,8);
        }
        
        Arrays.sort(s);

        for(int i=0;i<num-1;i++)
        {
            assertNotEquals(s[i],s[i+1]);
        }        
        
    }

    
}
