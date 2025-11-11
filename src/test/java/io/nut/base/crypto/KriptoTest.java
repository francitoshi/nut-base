/*
 *  KriptoTest.java
 *
 *  Copyright (C) 2018-2025 francitoshi@gmail.com
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

import io.nut.base.crypto.Kripto.KeyAgreementAlgorithm;
import io.nut.base.crypto.Kripto.KeyPairAlgorithm;
import io.nut.base.crypto.Kripto.KeyPairTransformation;
import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import io.nut.base.crypto.Kripto.SignatureAlgorithm;
import io.nut.base.encoding.Hex;
import io.nut.base.util.CharSets;
import static io.nut.base.util.CharSets.UTF8;
import io.nut.base.util.Joins;
import io.nut.base.util.Sorts;
import io.nut.base.util.Utils;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

/**
 *
 * @author franci
 */
public class KriptoTest
{

    static final byte[] BYTES32_256 =
    {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
    };

    static final byte[] IV16 = Utils.sequence(new byte[16], (byte) 0, (byte) 1);

    @Test
    public void testExampleAES() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto instance = Kripto.getInstance();

        KeyGenerator keyGenerator = instance.getKeyGenerator(SecretKeyAlgorithm.AES, 192);
        SecretKey secretKey = keyGenerator.generateKey();

        String plainText = "¡Hello! This is a secret message.";
        System.out.println("original text: " + plainText);

        IvParameterSpec iv = instance.getIv(IV16, 128);

        Cipher encrypt = instance.getCipher(secretKey, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.ENCRYPT_MODE);
        byte[] encryptedBytes = encrypt.doFinal(plainText.getBytes());

        Cipher decrypt = instance.getCipher(secretKey, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.DECRYPT_MODE);
        byte[] decryptedBytes = decrypt.doFinal(encryptedBytes);
        String decryptedText = new String(decryptedBytes);

        assertEquals(plainText, decryptedText);
        
        byte[] enc = instance.encrypt(secretKey, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, plainText.getBytes());
        byte[] dec = instance.decrypt(secretKey, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, enc);
        String decText = new String(dec);
        
        assertEquals(plainText, decText);
    }

    @Test
    public void testExample1RSA() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto instance = Kripto.getInstance();

        KeyPairGenerator keyPairGenerator = instance.getKeyPairGenerator(Kripto.KeyPairAlgorithm.RSA, 512);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey prvKey = keyPair.getPrivate();

        String plainText = "¡Hello! This is a secret message.";
        System.out.println("original text: " + plainText);

        Cipher cipher = instance.getCipher(pubKey, KeyPairTransformation.RSA_ECB_PKCS1Padding, Cipher.ENCRYPT_MODE);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

        cipher = instance.getCipher(prvKey, KeyPairTransformation.RSA_ECB_PKCS1Padding, Cipher.DECRYPT_MODE);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedText = new String(decryptedBytes);

        assertEquals(plainText, decryptedText);

        byte[] enc = instance.encrypt(pubKey, KeyPairTransformation.RSA_ECB_PKCS1Padding, plainText.getBytes());
        byte[] dec = instance.decrypt(prvKey, KeyPairTransformation.RSA_ECB_PKCS1Padding, enc);
        String decText = new String(dec);
        
        assertEquals(plainText, decText);
        
    }

    /**
     * Test of asSecretKey method, of class Kripto.
     */
    @Test
    public void testGetSecretKey()
    {
        Kripto instance = Kripto.getInstance();
        byte[] secretKey = BYTES32_256;
        SecretKey result = instance.getSecretKey(secretKey, SecretKeyAlgorithm.AES);
        assertNotNull(result);
    }

    /**
     * Test of newMessageDigest method, of class Kripto.
     */
    @Test
    public void testMessageDigest() throws Exception
    {
        Kripto instance = Kripto.getInstance();
        {
            MessageDigest sha224 = instance.getMessageDigest(Kripto.MessageDigestAlgorithm.SHA224);
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
    public void testExampleBouncyCastle() throws Exception
    {
        Kripto instance = Kripto.getInstanceBouncyCastle();

        byte[] plain = "ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvxyz0123456789".getBytes(CharSets.UTF8);
        SecretKeyTransformation[] secretKeyTransformations =
        {
            SecretKeyTransformation.AES_CBC_PKCS5Padding,
            SecretKeyTransformation.AES_GCM_NoPadding,
            SecretKeyTransformation.ChaCha20_Poly1305,
        };
        SecretKeyAlgorithm[] secretKeyAlgorithms =
        {
            SecretKeyAlgorithm.AES,
            SecretKeyAlgorithm.AES,
            SecretKeyAlgorithm.ChaCha20,
        };
        int[][] keyBits =
        {
            { 128, 192, 256 },
            { 128 },
            { 256 },
        };

        for (int i = 0; i < secretKeyTransformations.length; i++)
        {
            for (int j = 0; j < keyBits[i].length; j++)
            {
                SecretKeyTransformation skt = secretKeyTransformations[i];
                System.out.println("algorithm=" + skt + " " + secretKeyAlgorithms[i] + " " + keyBits[i][j] + " ivBits=" + skt.ivBits+ " tagBits=" + skt.tagBits);
                System.out.flush();

                int bits = keyBits[i][j];

                byte[] skDet = Arrays.copyOf(BYTES32_256, bits / 8);
                byte[] ivDet = Arrays.copyOf(BYTES32_256, skt.ivBits / 8);

                KeyGenerator kg = instance.getKeyGenerator(secretKeyAlgorithms[i], bits);

                SecretKey secretKey0 = instance.getSecretKey(skDet, secretKeyAlgorithms[i]);
                SecretKey secretKey1 = kg.generateKey();

                AlgorithmParameterSpec iv = skt.gcm ? instance.getIvGCM(ivDet, skt.tagBits) : instance.getIv(ivDet, skt.ivBits);

                Cipher encode0 = instance.getCipher(secretKey0, secretKeyTransformations[i], iv, Cipher.ENCRYPT_MODE);
                byte[] coded0 = encode0.doFinal(plain);

                Cipher encode1 = instance.getCipher(secretKey1, secretKeyTransformations[i], iv, Cipher.ENCRYPT_MODE);
                byte[] coded1 = encode1.doFinal(plain);

                Cipher decode0 = instance.getCipher(secretKey0, secretKeyTransformations[i], iv, Cipher.DECRYPT_MODE);
                byte[] plain0 = decode0.doFinal(coded0);

                Cipher decode1 = instance.getCipher(secretKey1, secretKeyTransformations[i], iv, Cipher.DECRYPT_MODE);
                byte[] plain1 = decode1.doFinal(coded1);

                assertArrayEquals(plain, plain0);
                assertArrayEquals(plain, plain1);
            }
        }
    }

    @Test
    public void testExample2RSA() throws Exception
    {
        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);
        KeyPairTransformation[] keyPairTransformations =
        {
            KeyPairTransformation.RSA_ECB_PKCS1Padding,
            KeyPairTransformation.RSA_ECB_OAEPWithSHA1AndMGF1Padding,
            KeyPairTransformation.RSA_ECB_OAEPWithSHA256AndMGF1Padding,
        };
        KeyPairAlgorithm[] keyPairAlgorithms =
        {
            KeyPairAlgorithm.RSA,
            KeyPairAlgorithm.RSA,
            KeyPairAlgorithm.RSA,
        };
        int[][] keyBits =
        {
            { 1024, 2048, 4098 },
            { 1024, 2048, 4098 },
            { 1024, 2048, 4098 },
        };
        int[][] ivBits =
        {
            { 0, 0, 0 },
            { 0, 0, 0 },
            { 256, 256, 256 },
        };

        Kripto instance = Kripto.getInstance();

        for (int i = 0; i < keyPairTransformations.length; i++)
        {
            for (int j = 0; j < keyBits[i].length; j++)
            {
                System.out.println("asym=" + keyPairTransformations[i] + " " + keyPairAlgorithms[i] + " " + keyBits[i][j] + " " + ivBits[i][j]);
                System.out.flush();

                KeyPairGenerator alice = instance.getKeyPairGenerator(keyPairAlgorithms[i], keyBits[i][j]);
                KeyPair aliceKeyPair = alice.generateKeyPair();

                PrivateKey alicePrivateKey = aliceKeyPair.getPrivate();
                PublicKey alicePublicKey = aliceKeyPair.getPublic();

                Cipher encode0 = instance.getCipher(alicePublicKey, keyPairTransformations[i], Cipher.ENCRYPT_MODE);
                byte[] coded0 = encode0.doFinal(plain);

                Cipher decode0 = instance.getCipher(alicePrivateKey, keyPairTransformations[i], Cipher.DECRYPT_MODE);
                byte[] plain0 = decode0.doFinal(coded0);

                assertArrayEquals(plain, plain0);
            }
        }
    }

    @Test
    public void testKeyAgreement() throws Exception
    {
        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);

        KeyPairAlgorithm[] pairAlgo =
        {
            KeyPairAlgorithm.EC,
            KeyPairAlgorithm.DiffieHellman,
        };
        KeyAgreementAlgorithm[] agreeAlgo =
        {
            KeyAgreementAlgorithm.ECDH,
            KeyAgreementAlgorithm.DiffieHellman
        };
        int keyBits = 256;

        Kripto instance = Kripto.getInstanceBouncyCastle();

        for (int i = 0; i < pairAlgo.length; i++)
        {
            //Alice
            KeyPair alice = instance.getKeyPairGenerator(pairAlgo[i], keyBits).generateKeyPair();
            byte[] a = alice.getPrivate().getEncoded();
            byte[] A = alice.getPublic().getEncoded();

            //Bob
            KeyPair bob = instance.getKeyPairGenerator(pairAlgo[i], keyBits).generateKeyPair();
            byte[] b = bob.getPrivate().getEncoded();
            byte[] B = bob.getPublic().getEncoded();

            //Alice
            SecretKey aliceSecret = instance.makeAgreement(pairAlgo[i], agreeAlgo[i], a, B);

            //Bob
            SecretKey bobSecret = instance.makeAgreement(pairAlgo[i], agreeAlgo[i], b, A);

            assertEquals(aliceSecret, bobSecret);

            IvParameterSpec iv = instance.getIv(B, 128);
            Cipher aliceCipher = instance.getCipher(aliceSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.ENCRYPT_MODE);
            byte[] encoded = aliceCipher.doFinal(plain);

            Cipher bobCipher = instance.getCipher(bobSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.DECRYPT_MODE);
            byte[] restored = bobCipher.doFinal(encoded);

            System.out.println(new String(plain) + " === " + new String(restored));
            System.out.flush();

            assertArrayEquals(plain, restored);
        }
    }

    @Test
    public void testExampleAgrement() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto kripto = Kripto.getInstance();
        // Paso 1: Generar pares de claves DH para Alice y Bob
        KeyPairGenerator keyPairGen = kripto.getKeyPairGenerator(KeyPairAlgorithm.DiffieHellman, 1024);
        keyPairGen.initialize(2048); // Tamaño de clave DH
            
        KeyPair aliceKeyPair = keyPairGen.generateKeyPair();
        KeyPair bobKeyPair = keyPairGen.generateKeyPair();
            
        // Paso 2: Inicializar KeyAgreement para ambas partes
        KeyAgreement aliceKeyAgreement = KeyAgreement.getInstance("DH");
        aliceKeyAgreement.init(aliceKeyPair.getPrivate());

        KeyAgreement bobKeyAgreement = KeyAgreement.getInstance("DH");
        bobKeyAgreement.init(bobKeyPair.getPrivate());
            
        // Paso 3: Intercambiar claves públicas y generar clave secreta compartida
        // Alice usa la clave pública de Bob
        aliceKeyAgreement.doPhase(bobKeyPair.getPublic(), true);
        byte[] aliceSharedSecret = aliceKeyAgreement.generateSecret();

        // Bob usa la clave pública de Alice
        bobKeyAgreement.doPhase(aliceKeyPair.getPublic(), true);
        byte[] bobSharedSecret = bobKeyAgreement.generateSecret();

        // Verificar que ambos secretos sean iguales
        System.out.println("¿Son iguales los secretos compartidos? " + 
        MessageDigest.isEqual(aliceSharedSecret, bobSharedSecret));
            
        // Paso 4: Derivar una clave AES de 256 bits del secreto compartido
        byte[] aesKeyBytes = new byte[32]; // 256 bits
        System.arraycopy(aliceSharedSecret, 0, aesKeyBytes, 0, 
        Math.min(aliceSharedSecret.length, aesKeyBytes.length));
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Paso 5: Usar la clave AES para cifrar un mensaje
        String originalMessage = "¡Hola, Bob! Este es un mensaje secreto.";
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Cifrar
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
        byte[] encryptedMessage = cipher.doFinal(originalMessage.getBytes(StandardCharsets.UTF_8));
        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedMessage);
            
        // Mostrar resultados
        System.out.println("Mensaje original: " + originalMessage);
        System.out.println("IV16 (Base64): " + Base64.getEncoder().encodeToString(iv));
        System.out.println("Mensaje cifrado (Base64): " + encryptedBase64);
            
        // Descifrar (simulando que Bob lo hace)
        Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        byte[] decryptedMessage = decryptCipher.doFinal(encryptedMessage);
        System.out.println("Mensaje descifrado: " + new String(decryptedMessage, StandardCharsets.UTF_8));

    }
    
    @Test
    public void testSignature() throws Exception
    {
        Kripto instance = Kripto.getInstance();

        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);
        byte[] plain2 = Joins.join("abcdefghijklmnopqrstuvxyz", ".").getBytes(CharSets.UTF8);

        SignatureAlgorithm[] signAlgo =
        {
            //NONEwith... doesn't use a digest so modifications beyond the used data are not detected
            SignatureAlgorithm.SHA256withRSA,
            SignatureAlgorithm.SHA256withDSA,
            SignatureAlgorithm.SHA256withECDSA,
        };

        final KeyPair rsa = instance.getKeyPairGenerator(KeyPairAlgorithm.RSA, 1024).generateKeyPair();
        final KeyPair dsa = instance.getKeyPairGenerator(KeyPairAlgorithm.DSA, 1024).generateKeyPair();
        final KeyPair ec = instance.getKeyPairGenerator(KeyPairAlgorithm.EC, 256).generateKeyPair();

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

    /**
     * Test of getInstance method, of class Kripto.
     */
    @Test
    public void testGetInstance_0args()
    {
        Kripto result = Kripto.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of getInstance method, of class Kripto.
     */
    @Test
    public void testGetInstance_boolean()
    {
        Kripto result = Kripto.getInstance(true);
        assertNotNull(result);
    }

    /**
     * Test of getInstanceBouncyCastle method, of class Kripto.
     */
    @Test
    public void testGetInstanceBouncyCastle()
    {
        Kripto result = Kripto.getInstanceBouncyCastle();
        assertNotNull(result);
    }

    /**
     * Test of randomNextInt method, of class Kripto.
     */
    @Test
    public void testNoProvier() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto instance = new Kripto("fake");
        assertNotNull(instance.sha256.get());

        KeyGenerator keyGenerator = instance.getKeyGenerator(SecretKeyAlgorithm.AES, 192);
        SecretKey secretKey = keyGenerator.generateKey();

        String plainText = "¡Hello! This is a secret message.";

        IvParameterSpec iv = instance.getIv(IV16, 128);
        Cipher encrypt = instance.getCipher(secretKey, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.ENCRYPT_MODE);

        KeyPairGenerator keyPairGenerator = instance.getKeyPairGenerator(KeyPairAlgorithm.RSA, 512);

        KeyPair kp = keyPairGenerator.genKeyPair();

        instance.getCipher(kp.getPublic(), KeyPairTransformation.RSA_ECB_PKCS1Padding, Cipher.ENCRYPT_MODE);

        Signature aliceSignature = instance.getSignature(SignatureAlgorithm.SHA256withECDSA);

        //kripto.deriveKey
        Kripto forced = new Kripto("fake", true);
        try
        {
            forced.sha256.get();
            fail("must throw an exception like ProviderException");
        }
        catch (ProviderException ex)
        {
            //
        }

    }

    /**
     * Test of getInstanceBouncyCastle method, of class Kripto.
     */
    @Test
    public void testKeyAgreement2() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidParameterSpecException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto instance = Kripto.getInstanceBouncyCastle();
        final int keyBits = 256;

        String helloFromAlice = "hello, I'm Alice.";
        String helloFromBob = "hello, I'm Bob.";

        //generate a private key for Alice
        //Alice
        KeyPair alice = instance.getKeyPairGenerator(KeyPairAlgorithm.EC, keyBits).generateKeyPair();
        byte[] a = alice.getPrivate().getEncoded();
        byte[] A = alice.getPublic().getEncoded();

        //Bob
        KeyPair bob = instance.getKeyPairGenerator(KeyPairAlgorithm.EC, keyBits).generateKeyPair();
        byte[] b = bob.getPrivate().getEncoded();
        byte[] B = bob.getPublic().getEncoded();

        //Alice
        SecretKey aliceSecret = instance.makeAgreement(KeyPairAlgorithm.EC, KeyAgreementAlgorithm.ECDH, a, B);

        //Bob
        SecretKey bobSecret = instance.makeAgreement(KeyPairAlgorithm.EC, KeyAgreementAlgorithm.ECDH, b, A);

        assertEquals(aliceSecret, bobSecret);

        {
            IvParameterSpec iv16 = instance.getIv(IV16, 128);
            Cipher aliceEncryptCipher = instance.getCipher(aliceSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv16, Cipher.ENCRYPT_MODE);
            byte[] encodedHelloFromAlice = aliceEncryptCipher.doFinal(helloFromAlice.getBytes());

            Cipher bobDecryptCipher = instance.getCipher(bobSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv16, Cipher.DECRYPT_MODE);
            byte[] restoredHelloFromAlice = bobDecryptCipher.doFinal(encodedHelloFromAlice);

            assertEquals(helloFromAlice, new String(restoredHelloFromAlice));
        }
        {
            IvParameterSpec iv16 = instance.getIv(IV16, 128);

            Cipher bobEncryptCipher = instance.getCipher(bobSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv16, Cipher.ENCRYPT_MODE);
            byte[] encodedHelloFromBob = bobEncryptCipher.doFinal(helloFromBob.getBytes());

            Cipher aliceDecryptCipher = instance.getCipher(aliceSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv16, Cipher.DECRYPT_MODE);
            byte[] restoredHelloFromBob = aliceDecryptCipher.doFinal(encodedHelloFromBob);

            assertEquals(helloFromBob, new String(restoredHelloFromBob));
        }

    }

    @Test
    public void testWrapUnwrap() throws Exception
    {
        Kripto instance = Kripto.getInstanceBouncyCastle();

        SecretKeyAlgorithm aes = SecretKeyAlgorithm.AES;
        KeyPairAlgorithm rsa = KeyPairAlgorithm.RSA;

        KeyGenerator keyGen = instance.getKeyGenerator(aes, 256);
        KeyPairGenerator keyPairGen = instance.getKeyPairGenerator(rsa, 2048);

        SecretKey aesWrapper = keyGen.generateKey();
        KeyPair rsaWrapper = keyPairGen.generateKeyPair();

        SecretKey secKeySrc = keyGen.generateKey();
        PrivateKey prvKeySrc = keyPairGen.generateKeyPair().getPrivate();

        SecretKeyTransformation aesCBC = SecretKeyTransformation.AES_CBC_PKCS5Padding;
        SecretKeyTransformation aesGCM = Kripto.AES_GCM_NOPADDING;

        IvParameterSpec iv = instance.getIv(IV16, 128);
        GCMParameterSpec ivGCM = instance.getIvGCM(IV16, 96);

        {
            byte[] bytes = instance.wrap(aesWrapper, aesCBC, iv, secKeySrc);
            SecretKey dst = instance.unwrap(aesWrapper, aesCBC, iv, bytes, aes);
            assertEquals(secKeySrc, dst);
        }

        {
            byte[] bytes = instance.wrap(aesWrapper, aesGCM, ivGCM, secKeySrc);
            SecretKey dst = (SecretKey) instance.unwrap(aesWrapper, aesGCM, ivGCM, bytes, aes);
            assertEquals(secKeySrc, dst);
        }

        {
            byte[] bytes = instance.wrap(aesWrapper, aesCBC, iv, prvKeySrc);
            PrivateKey dst = instance.unwrap(aesWrapper, aesCBC, iv, bytes, rsa);
            assertEquals(prvKeySrc, dst);
        }

        KeyPairTransformation rsaOAEP = Kripto.KeyPairTransformation.RSA_ECB_OAEPWithSHA256AndMGF1Padding;

        {
            byte[] bytes = instance.wrap(rsaWrapper.getPublic(), rsaOAEP, secKeySrc);
            SecretKey dst = instance.unwrap(rsaWrapper.getPrivate(), rsaOAEP, bytes, aes);
            assertEquals(secKeySrc, dst);
        }

    }

    @Test
    public void testGetShamirSharedSecret()
    {
        Kripto instance = Kripto.getInstance();

        String secret = "this is a secret";
        byte[] secretBytes = secret.getBytes();
        
        ShamirSharedSecret shamir = Kripto.getShamirSharedSecret(3, 2);

        byte[][] parts012 = shamir.split(secretBytes);
        
        byte[][] parts01 = {parts012[0], parts012[1]};
        byte[][] parts02 = {parts012[0], parts012[2]};
        byte[][] parts12 = {parts012[1], parts012[2]};

        byte[][] parts10 = {parts012[1], parts012[0]};
        byte[][] parts20 = {parts012[2], parts012[0]};
        byte[][] parts21 = {parts012[2], parts012[1]};
        
        byte[][][] parts =
        {
            parts01, parts02, parts12,  //with order
            parts10, parts20, parts21   //with no order
        };
        for(byte[][] item : parts)
        {
            byte[] join = shamir.join(item);
            String result = new String(join);
            assertEquals(secret, result);
        }                
    }

    @Test
    public void testIsAvailable()
    {

        Kripto instance = Kripto.getInstanceBouncyCastle();
        
        assertTrue(instance.isAvailable(SecretKeyTransformation.AES_GCM_NoPadding));
        assertTrue(instance.isAvailable(SecretKeyTransformation.ChaCha20_Poly1305));
        
        assertTrue(instance.isAvailable(SecretKeyAlgorithm.AES));
        assertTrue(instance.isAvailable(SecretKeyAlgorithm.ChaCha20));
        
    }
    
    static final Kripto MITM_KRIPTO = Kripto.getInstance();

    @Test
    @DisplayName("Should successfully verify when both parties use correct fingerprints")
    void testSuccessfulAuthentication() throws NoSuchAlgorithmException
    {
        // Arrange: Simulate Alice and Bob with their real fingerprints
        byte[] aliceFingerprint = "ALICE_GPG_FINGERPRINT_1234567890".getBytes(StandardCharsets.UTF_8);
        byte[] bobFingerprint = "BOB_GPG_FINGERPRINT_0987654321".getBytes(StandardCharsets.UTF_8);
        byte[] sharedSecret = "SecureSecret123".getBytes(StandardCharsets.UTF_8);

        // Act: Alice generates her proof fragments
        byte[][] aliceProof = MITM_KRIPTO.deriveMutualAuthProof(aliceFingerprint, bobFingerprint, sharedSecret);
        byte[] aliceFragmentToSend = aliceProof[0];
        byte[] aliceFragmentToExpect = aliceProof[1];

        // Act: Bob generates his proof fragments
        byte[][] bobProof = MITM_KRIPTO.deriveMutualAuthProof(bobFingerprint, aliceFingerprint, sharedSecret);
        byte[] bobFragmentToSend = bobProof[0];
        byte[] bobFragmentToExpect = bobProof[1];

        // Assert: What Alice sends should match what Bob expects
        assertArrayEquals(aliceFragmentToSend, bobFragmentToExpect, "Alice's fragment should match what Bob expects to receive");

        // Assert: What Bob sends should match what Alice expects
        assertArrayEquals(bobFragmentToSend, aliceFragmentToExpect, "Bob's fragment should match what Alice expects to receive");

        // Assert: Fragments should be 16 bytes (half of SHA-256)
        assertEquals(16, aliceFragmentToSend.length, "Fragment should be 16 bytes");
        assertEquals(16, bobFragmentToSend.length, "Fragment should be 16 bytes");

        // Assert: The fragments they send should be different (complementary halves)
        assertFalse(Arrays.equals(aliceFragmentToSend, bobFragmentToSend), "Alice and Bob should send different fragments");
    }

    @Test
    @DisplayName("Should work with reversed fingerprint order")
    void testReversedFingerprintOrder() throws NoSuchAlgorithmException
    {
        // Arrange
        byte[] fingerprint1 = "AAA_FINGERPRINT".getBytes(StandardCharsets.UTF_8);
        byte[] fingerprint2 = "ZZZ_FINGERPRINT".getBytes(StandardCharsets.UTF_8);
        byte[] sharedSecret = "MySecret".getBytes(StandardCharsets.UTF_8);

        // Act: Generate proofs in both orders
        byte[][] proof1 = MITM_KRIPTO.deriveMutualAuthProof(fingerprint1, fingerprint2, sharedSecret);
        byte[][] proof2 = MITM_KRIPTO.deriveMutualAuthProof(fingerprint2, fingerprint1, sharedSecret);

        // Assert: Complementary fragments should match
        assertArrayEquals(proof1[0], proof2[1], "Fragment to send from first should match fragment to expect from second");
        assertArrayEquals(proof1[1], proof2[0], "Fragment to expect from first should match fragment to send from second");
    }

    @Test
    @DisplayName("Should produce different proofs with different shared secrets")
    void testDifferentSharedSecrets() throws NoSuchAlgorithmException
    {
        // Arrange
        byte[] aliceFingerprint = "ALICE_FP".getBytes(StandardCharsets.UTF_8);
        byte[] bobFingerprint = "BOB_FP".getBytes(StandardCharsets.UTF_8);
        byte[] secret1 = "Secret1".getBytes(StandardCharsets.UTF_8);
        byte[] secret2 = "Secret2".getBytes(StandardCharsets.UTF_8);

        // Act
        byte[][] proof1 = MITM_KRIPTO.deriveMutualAuthProof(aliceFingerprint, bobFingerprint, secret1);
        byte[][] proof2 = MITM_KRIPTO.deriveMutualAuthProof(aliceFingerprint, bobFingerprint, secret2);

        // Assert: Different secrets produce different proofs
        assertFalse(Arrays.equals(proof1[0], proof2[0]), "Different shared secrets should produce different fragments");
        assertFalse(Arrays.equals(proof1[1], proof2[1]), "Different shared secrets should produce different fragments");
    }

    @Test
    @DisplayName("Should detect MITM when attacker intercepts and replaces fingerprints")
    void testMitmAttackDetection() throws NoSuchAlgorithmException
    {
        // Arrange: Real fingerprints
        byte[] aliceRealFingerprint = "ALICE_REAL_FP_12345".getBytes(StandardCharsets.UTF_8);
        byte[] bobRealFingerprint = "BOB_REAL_FP_67890".getBytes(StandardCharsets.UTF_8);
        byte[] attackerFingerprint = "ATTACKER_FP_MITM".getBytes(StandardCharsets.UTF_8);
        byte[] sharedSecret = "SecureSecret123".getBytes(StandardCharsets.UTF_8);

        // Act: Alice thinks she's talking to Bob, but receives attacker's fingerprint
        byte[][] aliceProof = MITM_KRIPTO.deriveMutualAuthProof(
                aliceRealFingerprint,
                attackerFingerprint, // MITM replaced Bob's fingerprint
                sharedSecret
        );
        byte[] aliceFragmentToSend = aliceProof[0];
        byte[] aliceFragmentToExpect = aliceProof[1];

        // Act: Bob thinks he's talking to Alice, but receives attacker's fingerprint
        byte[][] bobProof = MITM_KRIPTO.deriveMutualAuthProof(
                bobRealFingerprint,
                attackerFingerprint, // MITM replaced Alice's fingerprint
                sharedSecret
        );
        byte[] bobFragmentToSend = bobProof[0];
        byte[] bobFragmentToExpect = bobProof[1];

        // Assert: Alice's verification should FAIL
        assertFalse(Arrays.equals(bobFragmentToSend, aliceFragmentToExpect), "Alice should NOT receive the fragment she expects (MITM detected)");

        // Assert: Bob's verification should FAIL
        assertFalse(Arrays.equals(aliceFragmentToSend, bobFragmentToExpect), "Bob should NOT receive the fragment he expects (MITM detected)");
    }

    @Test
    @DisplayName("Should detect MITM even if attacker relays fragments unchanged")
    void testMitmCannotRelayFragments() throws NoSuchAlgorithmException
    {
        // Arrange
        byte[] aliceFingerprint = "ALICE_FP".getBytes(StandardCharsets.UTF_8);
        byte[] bobFingerprint = "BOB_FP".getBytes(StandardCharsets.UTF_8);
        byte[] attackerFingerprint = "MITM_FP".getBytes(StandardCharsets.UTF_8);
        byte[] sharedSecret = "Secret".getBytes(StandardCharsets.UTF_8);

        // Simulate MITM scenario
        byte[][] aliceProof = MITM_KRIPTO.deriveMutualAuthProof(aliceFingerprint, attackerFingerprint, sharedSecret);
        byte[][] bobProof = MITM_KRIPTO.deriveMutualAuthProof(bobFingerprint, attackerFingerprint, sharedSecret);

        // Attacker tries to relay fragments without modification
        byte[] fragmentAliceSends = aliceProof[0];
        byte[] fragmentBobExpects = bobProof[1];

        // Assert: Even with relay, verification fails
        assertFalse(Arrays.equals(fragmentAliceSends, fragmentBobExpects), "MITM cannot succeed by simply relaying fragments");
    }

    @Test
    @DisplayName("Should detect MITM when attacker uses wrong shared secret")
    void testWrongSharedSecret() throws NoSuchAlgorithmException
    {
        // Arrange
        byte[] aliceFingerprint = "ALICE_FP".getBytes(StandardCharsets.UTF_8);
        byte[] bobFingerprint = "BOB_FP".getBytes(StandardCharsets.UTF_8);
        byte[] correctSecret = "CorrectSecret".getBytes(StandardCharsets.UTF_8);
        byte[] wrongSecret = "WrongSecret".getBytes(StandardCharsets.UTF_8);

        // Act: Alice uses correct secret
        byte[][] aliceProof = MITM_KRIPTO.deriveMutualAuthProof(aliceFingerprint, bobFingerprint, correctSecret);

        // Act: Bob uses wrong secret (simulating compromised secret or typo)
        byte[][] bobProof = MITM_KRIPTO.deriveMutualAuthProof(bobFingerprint, aliceFingerprint, wrongSecret);

        // Assert: Verification should fail
        assertFalse(Arrays.equals(aliceProof[0], bobProof[1]), "Verification should fail with different shared secrets");
        assertFalse(Arrays.equals(bobProof[0], aliceProof[1]), "Verification should fail with different shared secrets");
    }

    @Test
    @DisplayName("Should throw exception when fingerprints are identical")
    void testIdenticalFingerprints()
    {
        // Arrange
        byte[] sameFingerprint = "SAME_FP".getBytes(StandardCharsets.UTF_8);
        byte[] sharedSecret = "Secret".getBytes(StandardCharsets.UTF_8);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
        {
            MITM_KRIPTO.deriveMutualAuthProof(sameFingerprint, sameFingerprint, sharedSecret);
        }, "Should throw IllegalArgumentException when fingerprints are identical");
    }

    @Test
    @DisplayName("Should handle empty shared secret")
    void testEmptySharedSecret() throws NoSuchAlgorithmException
    {
        // Arrange
        byte[] fp1 = "FP1".getBytes(StandardCharsets.UTF_8);
        byte[] fp2 = "FP2".getBytes(StandardCharsets.UTF_8);
        byte[] emptySecret = new byte[0];

        // Act
        byte[][] proof = MITM_KRIPTO.deriveMutualAuthProof(fp1, fp2, emptySecret);

        // Assert: Should not throw exception, but produces valid output
        assertNotNull(proof);
        assertEquals(2, proof.length);
        assertEquals(16, proof[0].length);
        assertEquals(16, proof[1].length);
    }

    @Test
    @DisplayName("Should handle very long fingerprints")
    void testLongFingerprints() throws NoSuchAlgorithmException
    {
        // Arrange: Simulate long GPG fingerprints (typical GPG fingerprints are 40 hex chars)
        byte[] longFp1 = new byte[1024];
        byte[] longFp2 = new byte[1024];
        Arrays.fill(longFp1, (byte) 0xAA);
        Arrays.fill(longFp2, (byte) 0xBB);
        byte[] sharedSecret = "Secret".getBytes(StandardCharsets.UTF_8);

        // Act
        byte[][] proof = MITM_KRIPTO.deriveMutualAuthProof(longFp1, longFp2, sharedSecret);

        // Assert
        assertNotNull(proof);
        assertEquals(16, proof[0].length);
        assertEquals(16, proof[1].length);
    }

    @Test
    @DisplayName("using and strengthener")
    void testStrengthenerSharedSecret() throws NoSuchAlgorithmException
    {
        // Arrange: Simulate Alice and Bob with their real fingerprints
        byte[] aliceFingerprint = "ALICE_GPG_FINGERPRINT_1234567890".getBytes(StandardCharsets.UTF_8);
        byte[] bobFingerprint = "BOB_GPG_FINGERPRINT_0987654321".getBytes(StandardCharsets.UTF_8);
        byte[] sharedSecret = "SecureSecret123".getBytes(StandardCharsets.UTF_8);

        // Act: Alice generates her proof fragments
        byte[][] aliceProof = MITM_KRIPTO.deriveMutualAuthProof(aliceFingerprint, bobFingerprint, sharedSecret, x->strengthener(x));
        byte[] aliceFragmentToSend = aliceProof[0];
        byte[] aliceFragmentToExpect = aliceProof[1];

        // Act: Bob generates his proof fragments
        byte[][] bobProof = MITM_KRIPTO.deriveMutualAuthProof(bobFingerprint, aliceFingerprint, sharedSecret, x->strengthener(x));
        byte[] bobFragmentToSend = bobProof[0];
        byte[] bobFragmentToExpect = bobProof[1];

        // Assert: What Alice sends should match what Bob expects
        assertArrayEquals(aliceFragmentToSend, bobFragmentToExpect, "Alice's fragment should match what Bob expects to receive");

        // Assert: What Bob sends should match what Alice expects
        assertArrayEquals(bobFragmentToSend, aliceFragmentToExpect, "Bob's fragment should match what Alice expects to receive");
    }
    public static byte[] strengthener(byte[] data)
    {
        return Joins.join(data, data, data);
    }
}
