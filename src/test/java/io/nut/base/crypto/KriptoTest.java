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
import io.nut.base.crypto.Kripto.SecretKeyDerivation;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import io.nut.base.crypto.Kripto.SignatureAlgorithm;
import io.nut.base.encoding.Hex;
import io.nut.base.util.CharSets;
import static io.nut.base.util.CharSets.UTF8;
import io.nut.base.util.Utils;
import java.math.BigInteger;
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
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testDerive() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException
    {
        Kripto instance = Kripto.getInstance();

        String plainText = "this is the plaintext";
        char[] passphrase = "this is the key".toCharArray();
        
        byte[] salt = instance.deriveBytesSHA256("test"+"salt");
        byte[] iv32 = instance.deriveBytesSHA256("test"+"iv");

        SecretKey key = instance.deriveSecretKey(passphrase, salt, 2048, 256, SecretKeyDerivation.PBKDF2WithHmacSHA256, SecretKeyAlgorithm.AES);
        
        IvParameterSpec iv = instance.getIv(iv32,128);
        byte[] encryptedBytes = instance.encrypt(key, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, plainText.getBytes());

        byte[] restoredBytes = instance.decrypt(key, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, encryptedBytes);

        String restoredText = new String(restoredBytes);
        
        assertEquals(plainText, restoredText);

    }

    @Test
    public void testExampleAgrement() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto instance = Kripto.getInstance();
//
//        // Paso 1: Generar pares de claves DH para Alice y Bob
//        KeyPairGenerator keyPairGen = instance.get KeyPairGenerator.getInstance("DH");
//            keyPairGen.initialize(2048); // Tamaño de clave DH
//            
//            KeyPair aliceKeyPair = keyPairGen.generateKeyPair();
//            KeyPair bobKeyPair = keyPairGen.generateKeyPair();
//            
//            // Paso 2: Inicializar KeyAgreement para ambas partes
//            KeyAgreement aliceKeyAgreement = KeyAgreement.getInstance("DH");
//            aliceKeyAgreement.init(aliceKeyPair.getPrivate());
//            
//            KeyAgreement bobKeyAgreement = KeyAgreement.getInstance("DH");
//            bobKeyAgreement.init(bobKeyPair.getPrivate());
//            
//            // Paso 3: Intercambiar claves públicas y generar clave secreta compartida
//            // Alice usa la clave pública de Bob
//            aliceKeyAgreement.doPhase(bobKeyPair.getPublic(), true);
//            byte[] aliceSharedSecret = aliceKeyAgreement.generateSecret();
//            
//            // Bob usa la clave pública de Alice
//            bobKeyAgreement.doPhase(aliceKeyPair.getPublic(), true);
//            byte[] bobSharedSecret = bobKeyAgreement.generateSecret();
//            
//            // Verificar que ambos secretos sean iguales
//            System.out.println("¿Son iguales los secretos compartidos? " + 
//                MessageDigest.isEqual(aliceSharedSecret, bobSharedSecret));
//            
//            // Paso 4: Derivar una clave AES de 256 bits del secreto compartido
//            byte[] aesKeyBytes = new byte[32]; // 256 bits
//            System.arraycopy(aliceSharedSecret, 0, aesKeyBytes, 0, 
//                Math.min(aliceSharedSecret.length, aesKeyBytes.length));
//            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
//            
//            // Paso 5: Usar la clave AES para cifrar un mensaje
//            String originalMessage = "¡Hola, Bob! Este es un mensaje secreto.";
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            byte[] iv = new byte[16];
//            new SecureRandom().nextBytes(iv);
//            IvParameterSpec ivSpec = new IvParameterSpec(iv);
//            
//            // Cifrar
//            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
//            byte[] encryptedMessage = cipher.doFinal(originalMessage.getBytes(StandardCharsets.UTF_8));
//            String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedMessage);
//            
//            // Mostrar resultados
//            System.out.println("Mensaje original: " + originalMessage);
//            System.out.println("IV16 (Base64): " + Base64.getEncoder().encodeToString(iv));
//            System.out.println("Mensaje cifrado (Base64): " + encryptedBase64);
//            
//            // Descifrar (simulando que Bob lo hace)
//            Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
//            byte[] decryptedMessage = decryptCipher.doFinal(encryptedMessage);
//            System.out.println("Mensaje descifrado: " + new String(decryptedMessage, StandardCharsets.UTF_8));

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
            MessageDigest md5 = instance.md5();

            byte[] a = md5.digest("The quick brown fox jumps over the lazy dog".getBytes(UTF8));
            byte[] b = md5.digest("The quick brown fox jumps over the lazy dog.".getBytes(UTF8));
            byte[] c = md5.digest("".getBytes(UTF8));

            assertEquals("9e107d9d372bb6826bd81d3542a419d6", Hex.encode(a));
            assertEquals("e4d909c290d0fb1ca068ffaddf22cbd0", Hex.encode(b));
            assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hex.encode(c));
        }
        {
            MessageDigest sha1 = instance.sha1();

            byte[] a = sha1.digest("The quick brown fox jumps over the lazy dog".getBytes(UTF8));
            byte[] b = sha1.digest("The quick brown fox jumps over the lazy cog".getBytes(UTF8));
            byte[] c = sha1.digest("".getBytes(UTF8));

            assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", Hex.encode(a));
            assertEquals("de9f2c7fd25e1b3afad3e85a0bd17d9b100db4b3", Hex.encode(b));
            assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Hex.encode(c));
        }
        {
            MessageDigest sha224 = instance.sha224();
            byte[] a = sha224.digest("The quick brown fox jumps over the lazy dog".getBytes(UTF8));
            byte[] b = sha224.digest("The quick brown fox jumps over the lazy dog.".getBytes(UTF8));
            byte[] c = sha224.digest("".getBytes(UTF8));
            assertEquals("730e109bd7a8a32b1cb9d9a09aa2325d2430587ddbc0c38bad911525", Hex.encode(a));
            assertEquals("619cba8e8e05826e9b8c519c0a5c68f4fb653e8a3d8aa04bb2c8cd4c", Hex.encode(b));
            assertEquals("d14a028c2a3a2bc9476102bb288234c415a2b01f828ea62ac5b3e42f", Hex.encode(c));
        }
        {
            MessageDigest sha256 = instance.sha256();
            byte[] a = sha256.digest("".getBytes(UTF8));
            assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hex.encode(a));
        }
        {
            MessageDigest sha512 = instance.sha512();
            byte[] a = sha512.digest("".getBytes(UTF8));
            assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e", Hex.encode(a));
        }
    }

    @Test
    public void testExampleBouncyCastleAES() throws Exception
    {
        Kripto instance = Kripto.getInstanceBouncyCastle();

        byte[] plain = "ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvxyz0123456789".getBytes(CharSets.UTF8);
        SecretKeyTransformation[] secretKeyTransformations =
        {
            SecretKeyTransformation.AES_CBC_PKCS5Padding,
            SecretKeyTransformation.AES_GCM_NoPadding,
            SecretKeyTransformation.DesEde_Cbc_Pkcs5Padding,
            SecretKeyTransformation.DES_CBC_PKCS5Padding,
        };
        SecretKeyAlgorithm[] secretKeyAlgorithms =
        {
            SecretKeyAlgorithm.AES,
            SecretKeyAlgorithm.AES,
            SecretKeyAlgorithm.DESede,
            SecretKeyAlgorithm.DES,
        };
        int[][] keyBits =
        {
            { 128, 192, 256 },
            { 128 },
            { 128, 192 },
            { 64 },
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

                AlgorithmParameterSpec iv = (skt.gcm|skt.siv) ? instance.getIvGCM(ivDet, skt.tagBits) : instance.getIv(ivDet, skt.ivBits);

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

        KeyPairAlgorithm[] pair =
        {
            KeyPairAlgorithm.EC,
            KeyPairAlgorithm.DiffieHellman,
        };
        KeyAgreementAlgorithm[] agreement =
        {
            KeyAgreementAlgorithm.ECDH,
            KeyAgreementAlgorithm.DiffieHellman
        };
        int[] keyBits = { 256, 512 };
        int aesBits = 256;

        Kripto instance = Kripto.getInstanceBouncyCastle();

        for (int i = 0; i < pair.length; i++)
        {
            //Alice
            KeyPair alice = instance.getKeyPairGenerator(pair[i], keyBits[i]).generateKeyPair();
            byte[] a = alice.getPrivate().getEncoded();
            byte[] A = alice.getPublic().getEncoded();

            //Bob
            KeyPair bob = instance.getKeyPairGenerator(pair[i], keyBits[i]).generateKeyPair();
            byte[] b = bob.getPrivate().getEncoded();
            byte[] B = bob.getPublic().getEncoded();

            //Alice
            SecretKey aliceSecret = instance.makeAgreement(pair[i], agreement[i], a, B);

            //Bob
            SecretKey bobSecret = instance.makeAgreement(pair[i], agreement[i], b, A);

            assertEquals(aliceSecret, bobSecret);

            aliceSecret = Kripto.resizeSecretKey(aliceSecret, aesBits);
            IvParameterSpec iv = instance.getIv(B, 128);
            Cipher aliceCipher = instance.getCipher(aliceSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.ENCRYPT_MODE);
            byte[] encoded = aliceCipher.doFinal(plain);

            bobSecret = Kripto.resizeSecretKey(bobSecret, aesBits);
            Cipher bobCipher = instance.getCipher(bobSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.DECRYPT_MODE);
            byte[] restored = bobCipher.doFinal(encoded);

            System.out.println(new String(plain) + " === " + new String(restored));
            System.out.flush();

            assertArrayEquals(plain, restored);
        }
    }

    @Test
    public void testSignature() throws Exception
    {
        Kripto instance = Kripto.getInstance();

        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);
        byte[] plain2 = Utils.join("abcdefghijklmnopqrstuvxyz", ".").getBytes(CharSets.UTF8);

        SignatureAlgorithm[] signAlgo =
        {
            //NONEwith... doesn't use a digest so modifications beyond the used data are not detected
            SignatureAlgorithm.MD5withRSA,
            SignatureAlgorithm.SHA1withRSA,
            SignatureAlgorithm.SHA256withRSA,
            SignatureAlgorithm.SHA1withDSA,
            SignatureAlgorithm.SHA256withDSA,
            SignatureAlgorithm.SHA1withECDSA,
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
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_booleanArr()
    {
        boolean[] dataNull = null;
        boolean[] dataEmpty = new boolean[0];

        assertNull(Kripto.random(dataNull));
        assertEquals(0, Kripto.random(dataEmpty).length);

        boolean[] result = Kripto.random(new boolean[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_byteArr()
    {
        byte[] dataNull = null;
        byte[] dataEmpty = new byte[0];

        assertNull(Kripto.random(dataNull));
        assertEquals(0, Kripto.random(dataEmpty).length);

        byte[] result = Kripto.random(new byte[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_intArr()
    {
        int[] dataNull = null;
        int[] dataEmpty = new int[0];

        assertNull(Kripto.random(dataNull));
        assertEquals(0, Kripto.random(dataEmpty).length);

        int[] result = Kripto.random(new int[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_longArr()
    {
        long[] dataNull = null;
        long[] dataEmpty = new long[0];

        assertNull(Kripto.random(dataNull));
        assertEquals(0, Kripto.random(dataEmpty).length);

        long[] result = Kripto.random(new long[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_floatArr()
    {
        float[] dataNull = null;
        float[] dataEmpty = new float[0];

        assertNull(Kripto.random(dataNull));
        assertEquals(0, Kripto.random(dataEmpty).length);

        float[] result = Kripto.random(new float[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_doubleArr()
    {
        double[] dataNull = null;
        double[] dataEmpty = new double[0];

        assertNull(Kripto.random(dataNull));
        assertEquals(0, Kripto.random(dataEmpty).length);

        double[] result = Kripto.random(new double[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Kripto.
     */
    @Test
    public void testRandom_BigIntegerArr_int()
    {
        BigInteger[] dataNull = null;
        BigInteger[] dataEmpty = new BigInteger[0];

        assertNull(Kripto.random(dataNull, 256));
        assertEquals(0, Kripto.random(dataEmpty, 256).length);

        BigInteger[] result = Kripto.random(new BigInteger[10], 256);
        assertEquals(10, result.length);
    }

    /**
     * Test of randomNextInt method, of class Kripto.
     */
    @Test
    public void testRandomInt_int()
    {
        int result = Kripto.randomInt(10);
        assertTrue(result < 10);
        assertTrue(result >= 0);
    }

    /**
     * Test of randomNextInt method, of class Kripto.
     */
    @Test
    public void testNoProvier() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        Kripto instance = new Kripto("fake");
        assertNotNull(instance.sha256());

        KeyGenerator keyGenerator = instance.getKeyGenerator(SecretKeyAlgorithm.AES, 192);
        SecretKey secretKey = keyGenerator.generateKey();

        String plainText = "¡Hello! This is a secret message.";

        IvParameterSpec iv = instance.getIv(IV16, 128);
        Cipher encrypt = instance.getCipher(secretKey, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv, Cipher.ENCRYPT_MODE);

        KeyPairGenerator keyPairGenerator = instance.getKeyPairGenerator(KeyPairAlgorithm.RSA, 512);

        KeyPair kp = keyPairGenerator.genKeyPair();

        instance.getCipher(kp.getPublic(), KeyPairTransformation.RSA_ECB_PKCS1Padding, Cipher.ENCRYPT_MODE);

        Signature aliceSignature = instance.getSignature(SignatureAlgorithm.SHA1withECDSA);

        //kripto.deriveKey
        Kripto forced = new Kripto("fake", true);
        try
        {
            forced.sha256();
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
            aliceSecret = Kripto.resizeSecretKey(aliceSecret, keyBits);
            IvParameterSpec iv16 = instance.getIv(IV16, 128);
            Cipher aliceEncryptCipher = instance.getCipher(aliceSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv16, Cipher.ENCRYPT_MODE);
            byte[] encodedHelloFromAlice = aliceEncryptCipher.doFinal(helloFromAlice.getBytes());

            bobSecret = Kripto.resizeSecretKey(bobSecret, keyBits);
            Cipher bobDecryptCipher = instance.getCipher(bobSecret, SecretKeyTransformation.AES_CBC_PKCS5Padding, iv16, Cipher.DECRYPT_MODE);
            byte[] restoredHelloFromAlice = bobDecryptCipher.doFinal(encodedHelloFromAlice);

            assertEquals(helloFromAlice, new String(restoredHelloFromAlice));
        }
        {
            bobSecret = Kripto.resizeSecretKey(bobSecret, keyBits);
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

        SecretKeyAlgorithm aes = Kripto.AES;
        KeyPairAlgorithm rsa = Kripto.RSA;

        KeyGenerator keyGen = instance.getKeyGenerator(aes, 256);
        KeyPairGenerator keyPairGen = instance.getKeyPairGenerator(rsa, 2048);

        SecretKey aesWrapper = keyGen.generateKey();
        KeyPair rsaWrapper = keyPairGen.generateKeyPair();

        SecretKey secKeySrc = keyGen.generateKey();
        PrivateKey prvKeySrc = keyPairGen.generateKeyPair().getPrivate();

        SecretKeyTransformation aesCBC = Kripto.AES_CBC_PKCS5PADDING;
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

}
