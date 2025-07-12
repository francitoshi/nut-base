/*
 *  DeriveTest.java
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class DeriveTest
{
    
    @Test
    public void testDerive() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
    {
        Kripto kripto = Kripto.getInstance().setMinDeriveRounds(2048);
        Derive derive = kripto.getDerivePBKDF2WithHmacSHA256();
        String plainText = "this is the plaintext";
        char[] passphrase = "this is the key".toCharArray();
        
        byte[] salt = kripto.deriveSaltSHA256("test"+"salt");
        byte[] iv32 = kripto.deriveSaltSHA256("test"+"iv");

        SecretKey key = derive.deriveSecretKeyAES(passphrase, salt, 2048, 256);
        
        IvParameterSpec iv = kripto.getIv(iv32,128);
        byte[] encryptedBytes = kripto.encrypt(key, Kripto.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, plainText.getBytes());

        byte[] restoredBytes = kripto.decrypt(key, Kripto.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, encryptedBytes);

        String restoredText = new String(restoredBytes);
        
        assertEquals(plainText, restoredText);

    }
    @Test
    public void testCalibrate() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
    {
        Kripto kripto = Kripto.getInstance().setMinDeriveRounds(1000);
        Derive derive = kripto.getDerivePBKDF2WithHmacSHA256();
        
        int ms = 500;
        int rounds = derive.calibrateRounds(ms);
        
        System.out.printf("PBKDF2WithHmacSHA256 %d ms = %d rounds\n", ms, rounds);

        derive = kripto.getDerivePBKDF2WithHmacSHA512();
        rounds = derive.calibrateRounds(ms);
        System.out.printf("PBKDF2WithHmacSHA512 %d ms = %d rounds\n", ms, rounds);
        
        Derive derive2 = kripto.getDerivePBKDF2WithHmacSHA256();
        
        int ms2 = 500;
        int rounds2 = derive2.calibrateRounds(ms2);
        
        System.out.printf("PBKDF2WithHmacSHA256 %d ms = %d rounds\n", ms2, rounds2);

        derive2 = kripto.getDerivePBKDF2WithHmacSHA512();
        rounds2 = derive2.calibrateRounds(ms2);
        System.out.printf("PBKDF2WithHmacSHA512 %d ms = %d rounds\n", ms2, rounds2);
        
        
        
    }    
}
