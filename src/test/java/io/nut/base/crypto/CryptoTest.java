/*
 *  CryptoTest.java
 *
 *  Copyright (C) 2018-2024 francitoshi@gmail.com
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

import io.nut.base.util.CharSets;
import static io.nut.base.util.CharSets.UTF8;
import io.nut.base.util.Utils;
import io.nut.base.encoding.Hex;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class CryptoTest
{
    static volatile Crypto instance;
    
    static final byte[] BYTES32_256 = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};
    
    public CryptoTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
        instance = new Crypto();
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    static final byte[] IV16 = Utils.sequence(new byte[16], (byte)0, (byte)1);
    static final int KEY_BITS = Crypto.SYMETRIC_SAFE_KEY_BITS;    
 
    /**
     * Test of asSecretKey method, of class Crypto.
     */
    @Test
    public void testAsSecretKey()
    {
        Crypto.SymmetricAlgorithm algorithm = Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding;
        byte[] secretKey = BYTES32_256;
        SecretKey result = instance.asSecretKey(algorithm, secretKey);
        assertNotNull(result);
    }
    
////////////////////////////////////////////////////////////////////////////////    
////////////////////////////////////////////////////////////////////////////////    
////////////////////////////////////////////////////////////////////////////////    

    /**
     * Test of newMessageDigest method, of class Crypto.
     */
    @Test
    public void testNewMessageDigest() throws Exception
    {
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
            MessageDigest sha224 = instance.newMessageDigest(Crypto.MessageDigestAlgorithm.SHA224);
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
            MessageDigest sha512 = instance.newMessageDigest(Crypto.MessageDigestAlgorithm.SHA512);
            byte[] a = sha512.digest("".getBytes(UTF8));
            assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e", Hex.encode(a));
        }        
    }

    @Test
    public void testSymmetricEncryptDecrypt() throws Exception
    {
        byte[] plain = "ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvxyz0123456789".getBytes(CharSets.UTF8);
        Crypto.SymmetricAlgorithm[] algorithm = 
        {
            Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding,
            Crypto.SymmetricAlgorithm.DES_CBC_PKCS5Padding,
            Crypto.SymmetricAlgorithm.DesEde_Cbc_Pkcs5Padding,
        };
        Crypto.KeyGeneratorAlgorithm[] generator = 
        {
            Crypto.KeyGeneratorAlgorithm.AES,
            Crypto.KeyGeneratorAlgorithm.DES,
            Crypto.KeyGeneratorAlgorithm.DESede,
        };
        int[][] keyBits = 
        {
            {128,192,256},
            {64},
            {128,192},
        };
        int[][] ivBits = 
        {
            {128,128,128},
            {64},
            {64,64},
        };
        
        for(int i=0;i<algorithm.length;i++)
        {
            for(int j=0;j<keyBits[i].length;j++)
            {
                System.out.println("algorithm="+algorithm[i]+" "+generator[i]+" "+keyBits[i][j]+" "+ivBits[i][j]);
                System.out.flush();

                byte[] sk = Arrays.copyOf(BYTES32_256, keyBits[i][j]/8);
                byte[] iv = Arrays.copyOf(IV16, ivBits[i][j]/8);

                KeyGenerator kg = instance.newKeyGenerator(generator[i], keyBits[i][j]);

    //            SecretKey secretKey = bouncy.asSecretKey(algorithm[i], BYTES32_256);
                SecretKey secretKey = kg.generateKey();
                IvParameterSpec ivp = instance.asIvParameter(iv);

                secretKey = Crypto.resizeSecretKey(secretKey, keyBits[i][j]);

                Cipher encode0 = instance.newEncryptCipher(algorithm[i], secretKey, ivp);
                byte[] coded0 = encode0.doFinal(plain);

                Cipher encode1 = instance.newEncryptCipher(algorithm[i], sk, iv);
                byte[] coded1 = encode1.doFinal(plain);

                Cipher decode0 = instance.newDecryptCipher(algorithm[i], secretKey, ivp);
                byte[] plain0 = decode0.doFinal(coded0);

                Cipher decode1 = instance.newDecryptCipher(algorithm[i], sk, iv);
                byte[] plain1 = decode1.doFinal(coded1);

                assertArrayEquals(plain, plain0);
                assertArrayEquals(plain, plain1);
            }
        }
    }

    @Test
    public void testAsymmetricEncryptDecrypt() throws Exception
    {
        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);
        Crypto.AsymmetricAlgorithm[] algorithm = 
        {
            Crypto.AsymmetricAlgorithm.RSA_ECB_PKCS1Padding,
            Crypto.AsymmetricAlgorithm.RSA_ECB_OAEPWithSHA1AndMGF1Padding,
            Crypto.AsymmetricAlgorithm.RSA_ECB_OAEPWithSHA256AndMGF1Padding,
        };
        Crypto.KeyPairAlgorithm[] pair = 
        {
            Crypto.KeyPairAlgorithm.RSA,
            Crypto.KeyPairAlgorithm.RSA,
            Crypto.KeyPairAlgorithm.RSA,
        };
        int[][] keyBits = 
        {
            {1024,2048,4098},
            {1024,2048,4098},
            {1024,2048,4098},
        };
        int[][] ivBits = 
        {
            {0,0,0},
            {0,0,0},
            {256,256,256},
        };
        
        for(int i=0;i<algorithm.length;i++)
        {
            for(int j=0;j<keyBits[i].length;j++)
            {
                System.out.println("asym="+algorithm[i]+" "+pair[i]+" "+keyBits[i][j]+" "+ivBits[i][j]);
                System.out.flush();

                KeyPairGenerator alice = instance.newKeyPairGenerator(pair[i], keyBits[i][j]);
                KeyPair aliceKeyPair = alice.generateKeyPair();

                PrivateKey alicePrivateKey = aliceKeyPair.getPrivate();
                PublicKey alicePublicKey   = aliceKeyPair.getPublic();

                Cipher encode0 = instance.newEncryptCipher(algorithm[i], alicePublicKey);
                byte[] coded0 = encode0.doFinal(plain);
    
                Cipher decode0 = instance.newDecryptCipher(algorithm[i], alicePrivateKey);
                byte[] plain0 = decode0.doFinal(coded0);
                
                assertArrayEquals(plain, plain0);
                
                //mixed symmetric and asymmetric algorithms
                SecretKey sk = instance.newKeyGenerator(Crypto.KeyGeneratorAlgorithm.AES, KEY_BITS).generateKey();
                byte[] coded1 = instance.encrypt(algorithm[i], alicePublicKey,  Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, sk, IV16, plain);
                byte[] plain1 = instance.decrypt(algorithm[i], alicePrivateKey, Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, IV16, coded1);
                assertArrayEquals(plain, plain1);
                
                byte[][] plain2 = new byte[][]{ plain, coded1};
                byte[][] coded2 = instance.encrypt(algorithm[i], alicePublicKey, Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, sk, IV16, plain2);
                byte[][] clear2 = instance.decrypt(algorithm[i], alicePrivateKey, Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, IV16, coded2);
                assertArrayEquals(plain2, clear2);
            }
        }
    }

    @Test
    public void testKeyAgreement() throws Exception
    {
        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);

        Crypto.KeyPairAlgorithm[] pair = 
        {
            Crypto.KeyPairAlgorithm.EC,
            Crypto.KeyPairAlgorithm.DiffieHellman
        };
        Crypto.KeyAgreementAlgorithm[] agreement = 
        {
            Crypto.KeyAgreementAlgorithm.ECDH,
            Crypto.KeyAgreementAlgorithm.DiffieHellman
        };
        int[] keyBits = 
        {
            256, 
            512
        };
        int aesBits = 256;
        
        instance = Crypto.getInstanceBouncyCastle();
        
        for(int i=0;i<pair.length;i++)
        {        
            //Alice
            KeyPair alice = instance.newKeyPairGenerator(pair[i], keyBits[i]).generateKeyPair();
            byte[] a = alice.getPrivate().getEncoded();
            byte[] A = alice.getPublic().getEncoded();

            //Bob
            KeyPair bob = instance.newKeyPairGenerator(pair[i], keyBits[i]).generateKeyPair();
            byte[] b = bob.getPrivate().getEncoded();
            byte[] B = bob.getPublic().getEncoded();

            //Alice
            SecretKey aliceSecret = instance.makeAgreement(pair[i], agreement[i], a, B);

            //Bob
            SecretKey bobSecret = instance.makeAgreement(pair[i], agreement[i], b, A);

            assertEquals(aliceSecret, bobSecret);
            
            aliceSecret = Crypto.resizeSecretKey(aliceSecret, aesBits);
            Cipher aliceCipher = instance.newEncryptCipher(Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, aliceSecret.getEncoded(), IV16);
            byte[] encoded = instance.encrypt(aliceCipher, plain);
            
            bobSecret = Crypto.resizeSecretKey(bobSecret, aesBits);
            Cipher bobCipher   = instance.newDecryptCipher(Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, bobSecret.getEncoded(), IV16);
            byte[] restored = instance.decrypt(bobCipher, encoded);

            System.out.println(new String(plain)+" === "+new String(restored));
            System.out.flush();
            
            assertArrayEquals(plain, restored);            
        }
    }
    
    @Test
    public void testSignature() throws Exception
    {
        byte[] plain = "abcdefghijklmnopqrstuvxyz".getBytes(CharSets.UTF8);
        byte[] plain2 = Utils.join("abcdefghijklmnopqrstuvxyz",".").getBytes(CharSets.UTF8);

        Crypto.SignatureAlgorithm[] signAlgo = 
        {
            //NONEwith... doesn't use a digest so modifications beyond the used data are not detected
            Crypto.SignatureAlgorithm.MD5withRSA, 
            Crypto.SignatureAlgorithm.SHA1withRSA, 
            Crypto.SignatureAlgorithm.SHA256withRSA, 
            Crypto.SignatureAlgorithm.SHA1withDSA, 
            Crypto.SignatureAlgorithm.SHA256withDSA, 
            Crypto.SignatureAlgorithm.SHA1withECDSA, 
            Crypto.SignatureAlgorithm.SHA256withECDSA, 
        };
        
        final KeyPair rsa = instance.newKeyPairGenerator(Crypto.KeyPairAlgorithm.RSA, 1024).generateKeyPair();
        final KeyPair dsa = instance.newKeyPairGenerator(Crypto.KeyPairAlgorithm.DSA, 1024).generateKeyPair();
        final KeyPair ec = instance.newKeyPairGenerator(Crypto.KeyPairAlgorithm.EC, 256).generateKeyPair();
        
        for(int i=0;i<signAlgo.length;i++)
        {        
            System.out.println("-----");
            System.out.println(signAlgo[i]);
            System.out.flush();

            byte[] sign0;
            byte[] sign1;
            
            String algo = signAlgo[i].name().toLowerCase();
            KeyPair rsaDsa = algo.endsWith("rsa") ? rsa : (algo.endsWith("ecdsa") ? ec : dsa);
            {
                Signature aliceSignature = instance.newSignature(signAlgo[i]);
            
                aliceSignature.initSign(rsaDsa.getPrivate());
                aliceSignature.update(plain);
                sign0 = aliceSignature.sign();
                sign1 = instance.sign(signAlgo[i], rsaDsa.getPrivate(), plain);
            }
            {
                Signature bobSignature = instance.newSignature(signAlgo[i]);

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
     * Test of getInstance method, of class Crypto.
     */
    @Test
    public void testGetInstance_0args()
    {
        Crypto result = Crypto.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of getInstance method, of class Crypto.
     */
    @Test
    public void testGetInstance_boolean()
    {
        Crypto result = Crypto.getInstance(true);
        assertNotNull(result);
    }

    /**
     * Test of getInstanceBouncyCastle method, of class Crypto.
     */
    @Test
    public void testGetInstanceBouncyCastle()
    {
        Crypto result = Crypto.getInstanceBouncyCastle();
        assertNotNull(result);
    }

    /**
     * Test of getInstanceBouncyCastle method, of class Crypto.
     */
    @Test
    public void testExample1() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidParameterSpecException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException
    {
        Crypto crypto = Crypto.getInstanceBouncyCastle();
        final int keyBits = 256;
        
        String helloFromAlice = "hello, I'm Alice.";
        String helloFromBob = "hello, I'm Bob.";
                
        //generate a private key for Alice
        //Alice
        KeyPair alice = crypto.newKeyPairGenerator(Crypto.KeyPairAlgorithm.EC, keyBits).generateKeyPair();
        byte[] a = alice.getPrivate().getEncoded();
        byte[] A = alice.getPublic().getEncoded();

        //Bob
        KeyPair bob = crypto.newKeyPairGenerator(Crypto.KeyPairAlgorithm.EC, keyBits).generateKeyPair();
        byte[] b = bob.getPrivate().getEncoded();
        byte[] B = bob.getPublic().getEncoded();

        //Alice
        SecretKey aliceSecret = crypto.makeAgreement(Crypto.KeyPairAlgorithm.EC, Crypto.KeyAgreementAlgorithm.ECDH, a, B);

        //Bob
        SecretKey bobSecret = crypto.makeAgreement(Crypto.KeyPairAlgorithm.EC, Crypto.KeyAgreementAlgorithm.ECDH, b, A);

        assertEquals(aliceSecret, bobSecret);

        {
            aliceSecret = Crypto.resizeSecretKey(aliceSecret, keyBits);
            Cipher aliceEncryptCipher = crypto.newEncryptCipher(Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, aliceSecret.getEncoded(), IV16);
            byte[] encodedHelloFromAlice = crypto.encrypt(aliceEncryptCipher, helloFromAlice.getBytes());

            bobSecret = Crypto.resizeSecretKey(bobSecret, keyBits);
            Cipher bobDecryptCipher   = crypto.newDecryptCipher(Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, bobSecret.getEncoded(), IV16);
            byte[] restoredHelloFromAlice = crypto.decrypt(bobDecryptCipher, encodedHelloFromAlice);

            assertEquals(helloFromAlice, new String(restoredHelloFromAlice));            
        }
        {
        
            Cipher bobEncryptCipher = crypto.newEncryptCipher(Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, bobSecret.getEncoded(), IV16);
            byte[] encodedHelloFromBob = crypto.encrypt(bobEncryptCipher, helloFromBob.getBytes());

            Cipher aliceDecryptCipher   = crypto.newDecryptCipher(Crypto.SymmetricAlgorithm.AES_CBC_PKCS5Padding, aliceSecret.getEncoded(), IV16);
            byte[] restoredHelloFromBob = crypto.decrypt(aliceDecryptCipher, encodedHelloFromBob);
        
            assertEquals(helloFromBob, new String(restoredHelloFromBob));            
        }
        
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_booleanArr()
    {
        boolean[] dataNull = null;
        boolean[] dataEmpty = new boolean[0];

        assertNull(Crypto.random(dataNull));
        assertEquals(0, Crypto.random(dataEmpty).length);

        boolean[] result = Crypto.random(new boolean[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_byteArr()
    {
        byte[] dataNull = null;
        byte[] dataEmpty = new byte[0];

        assertNull(Crypto.random(dataNull));
        assertEquals(0, Crypto.random(dataEmpty).length);

        byte[] result = Crypto.random(new byte[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_intArr()
    {
        int[] dataNull = null;
        int[] dataEmpty = new int[0];

        assertNull(Crypto.random(dataNull));
        assertEquals(0, Crypto.random(dataEmpty).length);

        int[] result = Crypto.random(new int[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_longArr()
    {
        long[] dataNull = null;
        long[] dataEmpty = new long[0];

        assertNull(Crypto.random(dataNull));
        assertEquals(0, Crypto.random(dataEmpty).length);

        long[] result = Crypto.random(new long[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_floatArr()
    {
        float[] dataNull = null;
        float[] dataEmpty = new float[0];

        assertNull(Crypto.random(dataNull));
        assertEquals(0, Crypto.random(dataEmpty).length);

        float[] result = Crypto.random(new float[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_doubleArr()
    {
        double[] dataNull = null;
        double[] dataEmpty = new double[0];

        assertNull(Crypto.random(dataNull));
        assertEquals(0, Crypto.random(dataEmpty).length);

        double[] result = Crypto.random(new double[10]);
        assertEquals(10, result.length);
    }

    /**
     * Test of random method, of class Crypto.
     */
    @Test
    public void testRandom_BigIntegerArr_int()
    {
        BigInteger[] dataNull = null;
        BigInteger[] dataEmpty = new BigInteger[0];

        assertNull(Crypto.random(dataNull, 256));
        assertEquals(0, Crypto.random(dataEmpty, 256).length);

        BigInteger[] result = Crypto.random(new BigInteger[10], 256);
        assertEquals(10, result.length);
    }

    /**
     * Test of randomInt method, of class Crypto.
     */
    @Test
    public void testRandomInt_int()
    {
        int result = Crypto.randomInt(10);
        assertTrue(result<10);
        assertTrue(result>=0);
    }

}
