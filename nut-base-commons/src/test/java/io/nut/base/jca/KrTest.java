/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.jca;

import io.nut.base.encoding.Hex;
import io.nut.base.lang.Joins;
import io.nut.base.util.CharSets;
import static io.nut.base.util.CharSets.UTF8;
import io.nut.base.util.Utils;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class KrTest
{
    
    /**
     * Test of getInstance method, of class Kripto.
     */
    @Test
    public void testGetInstance_0args()
    {
        Kr result = Kr.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of newMessageDigest method, of class Kripto.
     */
    @Test
    public void testMessageDigest() throws Exception
    {
        Kr instance = Kr.getInstance();
        {
            MessageDigest sha224 = instance.getMessageDigest(Kr.MessageDigestAlgorithm.SHA224);
            byte[] a = sha224.digest("The quick brown fox jumps over the lazy dog".getBytes(UTF8));
            byte[] b = sha224.digest("The quick brown fox jumps over the lazy dog.".getBytes(UTF8));
            byte[] c = sha224.digest("".getBytes(UTF8));
            assertEquals("730e109bd7a8a32b1cb9d9a09aa2325d2430587ddbc0c38bad911525", Hex.encode(a));
            assertEquals("619cba8e8e05826e9b8c519c0a5c68f4fb653e8a3d8aa04bb2c8cd4c", Hex.encode(b));
            assertEquals("d14a028c2a3a2bc9476102bb288234c415a2b01f828ea62ac5b3e42f", Hex.encode(c));
        }
        {
            MessageDigest sha256 = instance.sha256.get();
            byte[] a = sha256.digest("".getBytes(UTF8));
            assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hex.encode(a));
        }
        {
            MessageDigest sha512 = instance.sha512.get();
            byte[] a = sha512.digest("".getBytes(UTF8));
            assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e", Hex.encode(a));
        }
    }

    @Test
    public void testSignature() throws Exception
    {
        Kr instance = Kr.getInstance();

        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);
        byte[] plain2 = Joins.join("abcdefghijklmnopqrstuvxyz", ".").getBytes(CharSets.UTF8);

        Kr.SignatureAlgorithm[] signAlgo =
        {
            //NONEwith... doesn't use a digest so modifications beyond the used data are not detected
            Kr.SignatureAlgorithm.SHA256withRSA,
            Kr.SignatureAlgorithm.SHA256withDSA,
            Kr.SignatureAlgorithm.SHA256withECDSA,
        };

        final KeyPair rsa = instance.getKeyPairGenerator(Kr.KeyPairAlgorithm.RSA, 1024).generateKeyPair();
        final KeyPair dsa = instance.getKeyPairGenerator(Kr.KeyPairAlgorithm.DSA, 1024).generateKeyPair();
        final KeyPair ec = instance.getKeyPairGenerator(Kr.KeyPairAlgorithm.EC, 256).generateKeyPair();

        for (int i = 0; i < signAlgo.length; i++)
        {
            System.out.println("-----");
            System.out.println(signAlgo[i]);
            System.out.flush();

            byte[] sign0;
            byte[] sign1;

            String algo = signAlgo[i].name().toLowerCase();
            KeyPair rsaDsa = algo.endsWith("rsa") ? rsa : (algo.endsWith("ecdsa") ? ec : dsa);
            {
                Signature aliceSignature = instance.getSignature(signAlgo[i]);

                aliceSignature.initSign(rsaDsa.getPrivate());
                aliceSignature.update(plain);
                sign0 = aliceSignature.sign();
                sign1 = instance.sign(signAlgo[i], rsaDsa.getPrivate(), plain);
            }
            {
                Signature bobSignature = instance.getSignature(signAlgo[i]);

                bobSignature.initVerify(rsaDsa.getPublic());
                bobSignature.update(plain);
                boolean verified0 = bobSignature.verify(sign0);
                assertTrue(verified0);

                boolean verified1 = instance.verify(signAlgo[i], rsaDsa.getPublic(), sign1, plain);
                assertTrue(verified1);

                boolean verified2 = instance.verify(signAlgo[i], rsaDsa.getPublic(), sign1, plain2);
                assertFalse(verified2);
            }
        }
    }

    static final byte[] IV16 = Utils.sequence(new byte[16], (byte) 0, (byte) 1);

    @Test
    public void testExampleAES() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kr instance = Kr.getInstance();

        KeyGenerator keyGenerator = instance.getKeyGenerator(Kr.SecretKeyAlgorithm.AES, 192);
        SecretKey secretKey = keyGenerator.generateKey();

        String plainText = "¡Hello! This is a secret message.";
        System.out.println("original text: " + plainText);

        IvParameterSpec iv = instance.getIv(IV16, 128);

        Cipher encrypt = instance.getCipher(secretKey, Kr.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.ENCRYPT_MODE);
        byte[] encryptedBytes = encrypt.doFinal(plainText.getBytes());

        Cipher decrypt = instance.getCipher(secretKey, Kr.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.DECRYPT_MODE);
        byte[] decryptedBytes = decrypt.doFinal(encryptedBytes);
        String decryptedText = new String(decryptedBytes);

        assertEquals(plainText, decryptedText);
        
        byte[] enc = instance.encrypt(secretKey, Kr.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, plainText.getBytes());
        byte[] dec = instance.decrypt(secretKey, Kr.SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, enc);
        String decText = new String(dec);
        
        assertEquals(plainText, decText);
    }
}
