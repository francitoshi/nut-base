/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package io.nut.base.crypto;

import java.nio.charset.StandardCharsets;
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
        Kripto kripto = Kripto.getInstance();
        PBKDF2 derive = kripto.pbkdf2WithSha256;
        
        int ms = 500;
        int rounds = derive.calibrateRounds(ms);
        
        System.out.printf("PBKDF2WithHmacSHA256 %d ms = %d rounds\n", ms, rounds);

        derive = kripto.pbkdf2WithSha512;
        rounds = derive.calibrateRounds(ms);
        System.out.printf("PBKDF2WithHmacSHA512 %d ms = %d rounds\n", ms, rounds);
        
        PBKDF2 derive2 = kripto.pbkdf2WithSha256;
        
        int ms2 = 500;
        int rounds2 = derive2.calibrateRounds(ms2);
        
        System.out.printf("PBKDF2WithHmacSHA256 %d ms = %d rounds\n", ms2, rounds2);

        derive2 = kripto.pbkdf2WithSha512;
        rounds2 = derive2.calibrateRounds(ms2);
        System.out.printf("PBKDF2WithHmacSHA512 %d ms = %d rounds\n", ms2, rounds2);
        
        
        
    }    
    
}
