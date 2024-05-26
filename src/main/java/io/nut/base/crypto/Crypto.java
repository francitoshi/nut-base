/*
 *  Crypto.java
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

import io.nut.base.encoding.Base64DecoderException;
import io.nut.base.encoding.Encoding;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import io.nut.base.util.VarInt;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author franci
 */
public class Crypto
{
    private static final String UTF8 = StandardCharsets.UTF_8.name();
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Static Values ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA1";
    public static final String SHA256 = "SHA-256";
    
    public static final String NOPADDING = "NoPadding";
    
    public static SecureRandom getSecureRandomStrong()
    {
        try
        {
            return SecureRandom.getInstanceStrong();
        } 
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("there is no strong algorithm", ex);
        }
    }
    
    public static final int SYMETRIC_SAFE_KEY_BITS = 128;
    public static final int SYMETRIC_HALF_KEY_BITS = 192;
    public static final int SYMETRIC_HIGH_KEY_BITS = 256;

    ////////////////////////////////////////////////////////////////////////////
    ///// Enums /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    //https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyAgreement
        
    public enum SymmetricAlgorithm
    {
        //Symetric Algorithms
        AES_CBC_NoPadding("AES/CBC/NoPadding", true),               //(128,192,256)
        AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding", true),         //(128,192,256)
        AES_ECB_NoPadding("AES/ECB/NoPadding",  true),              //(128)
        AES_ECB_PKCS5Padding("AES/ECB/PKCS5Padding",  true),        //(128)

        AES_CFB8_NoPadding("AES/CFB8/NoPadding",  true),            //(128)

        DES_CBC_NoPadding("DES/CBC/NoPadding",  true),              //(56)
        DES_CBC_PKCS5Padding("DES/CBC/PKCS5Padding", true),         //(56)
        DES_ECB_NoPadding("DES/ECB/NoPadding", true),               //(56)
        DES_ECB_PKCS5Padding("DES/ECB/PKCS5Padding", true),         //(56)

        DESede_CBC_NoPadding("DESede/CBC/NoPadding", true),         //(168) [TripleDES]
        DesEde_Cbc_Pkcs5Padding("DESede/CBC/PKCS5Padding", true),   //(128) [TripleDES]
        DESede_ECB_NoPadding("DESede/ECB/NoPadding", true),         //(128) [TripleDES]
        DESede_ECB_PKCS5Padding("DESede/ECB/PKCS5Padding", false);  //(128) [TripleDES]

        public final String transformation;
        public final String algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;
        public final boolean iv;
        
        SymmetricAlgorithm(String transformation, boolean iv)
        {
            String[] items = transformation.split("/");
            this.algorithm = items[0];
            this.mode = items[1];
            this.padding = items[2];
            this.transformation = transformation;
            this.nopadding = NOPADDING.equalsIgnoreCase(padding);
            this.iv = iv;
        }
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm);
        }
    }
    public enum AsymmetricAlgorithm
    {
        RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding", false),                                  //(1024,2048)
        RSA_ECB_OAEPWithSHA1AndMGF1Padding("RSA/ECB/OAEPWithSHA-1AndMGF1Padding", false),     //(1024,2048)
        RSA_ECB_OAEPWithSHA256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", true);  //(1024, 2048)        

        public final String transformation;
        public final String algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;
        public final boolean iv;
        
        AsymmetricAlgorithm(String transformation, boolean iv)
        {
            String[] items = transformation.split("/");
            this.algorithm = items[0];
            this.mode = items[1];
            this.padding = items[2];
            this.transformation = transformation;
            this.nopadding = NOPADDING.equalsIgnoreCase(padding);
            this.iv = iv;
        }
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm);
        }
    }
    
    enum ParameterGeneratorAlgorithm
    {
        DiffieHellman,  // (1024)
        DSA             // (1024)
    }
    enum ParameterAlgorithm
    {
        AES, DES, DESede, DiffieHellman ,DSA
    }
    public enum KeyAgreementAlgorithm
    {
        DiffieHellman, ECDH, ECMQV
    }
    public enum KeyGeneratorAlgorithm
    {
        AES, DES, DESede, HmacSHA1, HmacSHA256, //mandatory
        ARCFOUR, Blowfish, HmacMD5, HmacSHA224, HmacSHA384, HmacSHA512, RC2 //optional
    }
    public enum KeyPairAlgorithm //KeyPair Algorithms, KeyFactory Algorithms
    {
        DiffieHellman, DSA, RSA, //mandatory DiffieHellman (1024), DSA (1024), RSA (1024, 2048)
        EC                      //optional   EC (192, 256)
    }
    public enum MessageDigestAlgorithm
    {
        MD2("MD2"), MD5("MD5"), SHA1("SHA1"), SHA224("SHA-224"), SHA256("SHA-256"), SHA384("SHA-384"), SHA512("SHA-512");
        MessageDigestAlgorithm(String code)
        {
            this.code = code;
        }
        final String code;
    }
    public enum SignatureAlgorithm
    {
        NONEwithRSA, MD2withRSA, MD5withRSA, SHA1withRSA, SHA224withRSA, SHA256withRSA, SHA384withRSA, SHA512withRSA,
        NONEwithDSA, SHA1withDSA, SHA224withDSA, SHA256withDSA, 
        NONEwithECDSA, SHA1withECDSA, SHA224withECDSA, SHA256withECDSA, SHA384withECDSA, SHA512withECDSA
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Class Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public static Crypto getInstance()
    {
        return new Crypto();
    }
    public static Crypto getInstance(boolean preferBouncyCastle)
    {
        Crypto instance = preferBouncyCastle ? getInstanceBouncyCastle() : null;
        return instance!=null ? instance : new Crypto();
    }
    
    private static volatile boolean registeredBouncyCastle;
    
    public static boolean registerBouncyCastle()
    {
        if(!registeredBouncyCastle)
        {
            try
            {
                Class<?> bcp = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                Security.addProvider((Provider) bcp.getDeclaredConstructor().newInstance()); 
                registeredBouncyCastle = true;
            }
            catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException ex)
            {
                Logger.getLogger(Crypto.class.getName()).log(Level.SEVERE, null, ex);
                registeredBouncyCastle = false;
            }
        }
        return registeredBouncyCastle;
    }
    
    public static Crypto getInstanceBouncyCastle()
    {
        return registerBouncyCastle() ? new Crypto("BC") : null;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Instance Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    protected final String providerName;
    
    public Crypto()
    {
        this.providerName = null;
    }
    protected Crypto(String providerName)
    {
        this.providerName = providerName;
    }
    ////////////////////////////////////////////////////////////////////////////
    ///// Converters and Tools /////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    public IvParameterSpec asIvParameter(byte[] iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        return new IvParameterSpec(iv);
    }
    public SecretKey asSecretKey(SymmetricAlgorithm algorithm, byte[] secretKey)
    {
        return new SecretKeySpec(secretKey, algorithm.algorithm);
    }
    public static SecretKey resizeSecretKey(SecretKey sk, int keyBits)
    {
        if(keyBits>0)
        {
            byte[] key = sk.getEncoded();
            int bits = 8*key.length;
            sk = keyBits<bits ? new SecretKeySpec(key, 0, keyBits/8, sk.getAlgorithm()) : sk;  
        }
        return sk;
    }
       
    ////////////////////////////////////////////////////////////////////////////
    ///// Message Diggest //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public MessageDigest newMessageDigest(MessageDigestAlgorithm algorithm) throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return this.providerName!=null ? MessageDigest.getInstance(algorithm.code, this.providerName) : MessageDigest.getInstance(algorithm.code);
    }
    public MessageDigest md5() throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return this.newMessageDigest(MessageDigestAlgorithm.MD5);
    }
    public MessageDigest sha1() throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return this.newMessageDigest(MessageDigestAlgorithm.SHA1);
    }
    public MessageDigest sha256() throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return this.newMessageDigest(MessageDigestAlgorithm.SHA256);
    }
    ////////////////////////////////////////////////////////////////////////////
    ///// Ciphers Private //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private Cipher newCipher(String algorithmTransformation) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        return this.providerName!=null ? Cipher.getInstance(algorithmTransformation, this.providerName) : Cipher.getInstance(algorithmTransformation);    
    }
    
    private Cipher newCipher(SymmetricAlgorithm algorithm, int mode, SecretKey secretKey, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = this.newCipher(algorithm.transformation);
        if(algorithm.iv)
        {
            cipher.init(mode, secretKey, iv);
        }
        else
        {
            cipher.init(mode, secretKey);
        }
        return cipher;
    }

    private Cipher newCipher(AsymmetricAlgorithm algorithm, int mode, Key key) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = this.newCipher(algorithm.transformation);
        cipher.init(mode, key);
        return cipher;
    }
    private Cipher newCipher(SymmetricAlgorithm algorithm, int mode, byte[] secretKey, byte[] iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, mode, this.asSecretKey(algorithm, secretKey), this.asIvParameter(iv));
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Ciphers Symetric /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public Cipher newEncryptCipher(SymmetricAlgorithm algorithm, SecretKey secretKey, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, Cipher.ENCRYPT_MODE, secretKey, iv);
    }
    public Cipher newDecryptCipher(SymmetricAlgorithm algorithm, SecretKey secretKey, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, Cipher.DECRYPT_MODE, secretKey, iv);
    }
    public Cipher newEncryptCipher(SymmetricAlgorithm algorithm, byte[] secretKey, byte[] iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, Cipher.ENCRYPT_MODE, this.asSecretKey(algorithm, secretKey), this.asIvParameter(iv));
    }
    public Cipher newDecryptCipher(SymmetricAlgorithm algorithm, byte[] secretKey, byte[] iv) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, Cipher.DECRYPT_MODE, this.asSecretKey(algorithm, secretKey), this.asIvParameter(iv));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Ciphers Asymetric /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public Cipher newCipher(AsymmetricAlgorithm algorithm, int mode, PublicKey key) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return newCipher(algorithm, mode, (Key)key);
    }
    public Cipher newCipher(AsymmetricAlgorithm algorithm, int mode, PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return newCipher(algorithm, mode, (Key)key);
    }
    public Cipher newEncryptCipher(AsymmetricAlgorithm algorithm, PublicKey key) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, Cipher.ENCRYPT_MODE, (Key)key);
    }
    public Cipher newDecryptCipher(AsymmetricAlgorithm algorithm, PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        return this.newCipher(algorithm, Cipher.DECRYPT_MODE, (Key)key);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Keys /////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    //AES, DES, DESede, HmacSHA1, HmacSHA256, //mandatory
    //ARCFOUR, Blowfish, HmacMD5, HmacSHA224, HmacSHA384, HmacSHA512, RC2 //optional
    public KeyGenerator newKeyGenerator(KeyGeneratorAlgorithm algorithm, int keyBits) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        KeyGenerator kg = this.providerName!=null ? KeyGenerator.getInstance(algorithm.name(), this.providerName) : KeyGenerator.getInstance(algorithm.name());
        kg.init(keyBits);
        return kg;
    }
    
    //DiffieHellman (1024), DSA (1024), RSA (1024, 2048), EC(192,256)
    public KeyPairGenerator newKeyPairGenerator(KeyPairAlgorithm algorithm, int keyBits) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidParameterSpecException, InvalidAlgorithmParameterException
    {
        KeyPairGenerator kpg = this.providerName!=null ? KeyPairGenerator.getInstance(algorithm.name(), this.providerName) : KeyPairGenerator.getInstance(algorithm.name());
        kpg.initialize(keyBits);
        return kpg;
    }

    public KeyFactory newKeyFactory(KeyPairAlgorithm algorithm) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        return this.providerName!=null ? KeyFactory.getInstance(algorithm.name(), this.providerName) : KeyFactory.getInstance(algorithm.name());
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Cipher facilities ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public static String encrypt(Cipher cipher, String plain, Encoding.Type encoding) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchProviderException
    {
        String coded=null;
        if(plain!=null)
        {
            byte[] data = cipher.doFinal(plain.getBytes(UTF8));
            coded = Encoding.encode(data, encoding);
        }
        return coded;
    }
    
    public static String decrypt(Cipher cipher, String coded, Encoding.Type mode) throws UnsupportedEncodingException, Base64DecoderException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        String plain=null;
        if(coded!=null)
        {
            byte[] data = Encoding.decode(coded, mode);
            data = cipher.doFinal(data);
            return new String(data, UTF8);
        }
        return plain;
    }

    public byte[] encrypt(Cipher cipher, byte[] plain) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, NoSuchProviderException
    {
        byte[] coded=null;
        if(plain!=null)
        {
            coded = cipher.doFinal(plain);
        }
        return coded;
    }

    public byte[] decrypt(Cipher cipher, byte[] coded) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        return decrypt(cipher, coded, 0, coded.length);
    }
    public byte[] decrypt(Cipher cipher, byte[] coded, int offset, int size) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        byte[] plain=null;
        if(coded!=null)
        {
            plain = cipher.doFinal(coded, offset, size);
        }
        return plain;
    }
    //wraps a secretKey using a publicKey and encrypts the data using the secretKey
    public byte[] encrypt(AsymmetricAlgorithm aa, PublicKey pk, SymmetricAlgorithm sa, SecretKey sk, byte[] iv, byte[] plain) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, NoSuchProviderException
    {
        byte[] wrap = this.wrapSecretKey(aa, pk, sk);
        Cipher cipher = this.newEncryptCipher(sa, sk, this.asIvParameter(iv));
        byte[] coded = this.encrypt(cipher, plain);
        byte[] wrapSize = new VarInt(wrap.length).encode();
        byte[] codedSize = new VarInt(coded.length).encode();
        return Utils.join(wrapSize,wrap, codedSize,coded);
    }
    //unwraps a secretKey using a privateKey and decrypts the data using the secretKey
    public byte[] decrypt(AsymmetricAlgorithm aa, PrivateKey pk, SymmetricAlgorithm sa, byte[] iv, byte[] coded) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, NoSuchProviderException
    {
        VarInt wrapVarInt = new VarInt(coded, 0);
        int wrapSize= wrapVarInt.intValue();
        int wrapSizeSize= wrapVarInt.getOriginalSizeInBytes();
        
        SecretKey sk = this.unwrapSecretKey(aa, pk, sa, Arrays.copyOfRange(coded, wrapSizeSize, wrapSizeSize+wrapSize));
        
        VarInt codedVarInt = new VarInt(coded, wrapSize+wrapSizeSize);
        int codedSize= codedVarInt.intValue();
        int codedSizeSize= codedVarInt.getOriginalSizeInBytes();

        Cipher cipher = this.newDecryptCipher(sa, sk, this.asIvParameter(iv));
        
        return this.decrypt(cipher, coded, wrapSize+wrapSizeSize+codedSizeSize, codedSize);
    }
    //wraps a secretKey using a publicKey and encrypts the array of data using the secretKey
    public byte[][] encrypt(AsymmetricAlgorithm aa, PublicKey pk, SymmetricAlgorithm sa, SecretKey sk, byte[] iv, byte[][] plain) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, NoSuchProviderException
    {
        byte[][] coded = new byte[plain.length+1][];
        coded[0] = this.wrapSecretKey(aa, pk, sk);
        Cipher cipher = this.newEncryptCipher(sa, sk, this.asIvParameter(iv));
        for(int i=0;i<plain.length;i++)
        {
            coded[i+1] = this.encrypt(cipher, plain[i]);
        }
        return coded;
    }
    //unwraps a secretKey using a privateKey and decrypts the array of data using the secretKey
    public byte[][] decrypt(AsymmetricAlgorithm aa, PrivateKey pk, SymmetricAlgorithm sa, byte[] iv, byte[][] coded) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, NoSuchProviderException
    {
        byte[][] plain = new byte[coded.length-1][];
        SecretKey sk = this.unwrapSecretKey(aa, pk, sa, coded[0]);
        Cipher cipher = this.newDecryptCipher(sa, sk, this.asIvParameter(iv));
        for(int i=1;i<coded.length;i++)
        {
            plain[i-1] = this.decrypt(cipher, coded[i]);
        }
        return plain;
    }
    
    public byte[] wrapSecretKey(AsymmetricAlgorithm algorithm, PublicKey publicKey, SecretKey secretKey) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException
    {        
        return this.newCipher(algorithm, Cipher.WRAP_MODE, publicKey).wrap(secretKey);
    }

    public SecretKey unwrapSecretKey(AsymmetricAlgorithm asymmetricAlgorithm, PrivateKey privateKey, SymmetricAlgorithm symmetricAlgorithm, byte[] secretKey) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        return (SecretKey) this.newCipher(asymmetricAlgorithm, Cipher.UNWRAP_MODE, privateKey).unwrap(secretKey, symmetricAlgorithm.algorithm, Cipher.SECRET_KEY);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// PairKey facilities ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public static PrivateKey decodePrivateKey(KeyFactory keyFac, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
        return keyFac.generatePrivate(keySpec);
    }

    public static PublicKey decodePublicKey(KeyFactory keyFac, byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
        return keyFac.generatePublic(spec);
    }

    public static byte[] encodePrivateKey(KeyFactory keyFac, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeySpecException 
    {
        PKCS8EncodedKeySpec spec = keyFac.getKeySpec(key, PKCS8EncodedKeySpec.class);
        return spec.getEncoded();
    }

    public static byte[] encodePublicKey(KeyFactory keyFac, PublicKey key) throws NoSuchAlgorithmException, InvalidKeySpecException 
    {
        X509EncodedKeySpec spec = keyFac.getKeySpec(key, X509EncodedKeySpec.class);
        return spec.getEncoded();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Shared Secrets n of m share the secret key ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public ShamirSharedSecret newShamirSharedSecret(int n, int m)
    {
        return new ShamirSharedSecret(n, m);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// KeyAgreement Algorithms ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    private KeyAgreement newKeyAgreement(KeyAgreementAlgorithm algorithm) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        return this.providerName!=null ? KeyAgreement.getInstance(algorithm.name(), this.providerName) : KeyAgreement.getInstance(algorithm.name());
    }
    //use the pair (EC,ECDH) or (DiffieHellman,DiffieHellman)
    public SecretKey makeAgreement(KeyPairAlgorithm kpa, KeyAgreementAlgorithm kaa, byte[] privateKeyBytes, byte[] foreignKeyBytes) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchProviderException, NoSuchPaddingException
    {
        KeyFactory keyFactory = this.newKeyFactory(kpa);
        KeyAgreement keyAgreement = this.newKeyAgreement(kaa);
        
        X509EncodedKeySpec foreignSpec = new X509EncodedKeySpec(foreignKeyBytes);
        PublicKey  foreignKey = keyFactory.generatePublic(foreignSpec);
        
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateSpec);
        
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(foreignKey, true);
        return keyAgreement.generateSecret(kpa.name());
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Signatures ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public Signature newSignature(SignatureAlgorithm algorithm) throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return this.providerName!=null ? Signature.getInstance(algorithm.name(), this.providerName) : Signature.getInstance(algorithm.name());
    }
    public byte[] sign(SignatureAlgorithm algorithm, PrivateKey privateKey, byte[]... data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException
    {
        Signature signature = this.newSignature(algorithm);
        signature.initSign(privateKey);
        for(int i=0;i<data.length;i++)
        {
            signature.update(data[i]);
        }
        return signature.sign();
    }       
    public boolean verify(SignatureAlgorithm algorithm, PublicKey publicKey, byte[] sign, byte[]... data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException
    {
        Signature signature = this.newSignature(algorithm);
        signature.initVerify(publicKey);
        for(int i=0;i<data.length;i++)
        {
            signature.update(data[i]);
        }
        return signature.verify(sign);
    }       
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Debug things /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public static void showProvidersInfo(PrintStream out)
    {
        String hr10 = Strings.repeat('-',10);
        String hr40 = Strings.repeat('-',40);
        
        for(Provider provider: Security.getProviders()) 
        {
            out.println(hr40);
            out.println(Strings.fill(hr10+' '+provider.getName()+' ','-',40));
            out.println(hr40);
            showProviderInfo(out, provider);
            out.println(hr40);
            out.println();
        }
    }
    public static void showProviderInfo(PrintStream out, Provider provider)
    {
        out.println(provider.getInfo());
        for (Service service: provider.getServices())
        {
            out.println("  "+service.toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Random data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    private static volatile SecureRandom secureRandom;

    private static SecureRandom getSecureRandom() throws RuntimeException
    {
        if(secureRandom==null)
        {
            secureRandom = getSecureRandomStrong();
        }
        return secureRandom;
    }
    
    public static boolean[] random(boolean[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = getSecureRandom().nextBoolean();
        }
        return data;
    }
    
    public static byte[] random(byte[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        getSecureRandom().nextBytes(data);
        return data;
    }
    
    public static int[] random(int[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = getSecureRandom().nextInt();
        }
        return data;
    }
    
    public static long[] random(long[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = getSecureRandom().nextLong();
        }
        return data;
    }
    
    public static float[] random(float[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = getSecureRandom().nextFloat();
        }
        return data;
    }
    
    public static double[] random(double[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = getSecureRandom().nextDouble();
        }
        return data;
    }
    
    public static BigInteger[] random(BigInteger[] data, int numBits)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = new BigInteger(numBits, getSecureRandom());
        }
        return data;
    }
    
    public static boolean randomBoolean()
    {
        return getSecureRandom().nextBoolean();
    }
    
    public static BigInteger randomBigInteger(int numBits)
    {
        return new BigInteger(numBits, getSecureRandom());
    }
    
    public static BigInteger randomBigInteger(BigInteger bound)
    {
        SecureRandom sr = getSecureRandom();
        BigInteger r;
        do 
        {
            r = new BigInteger(bound.bitLength(), sr);
        } 
        while (r.compareTo(bound) >= 0);
        return r;
    }
    
    public static int randomInt()
    {
        return getSecureRandom().nextInt();
    }

    public static int randomInt(int bound)
    {
        return getSecureRandom().nextInt(bound);
    }

    public static long randomLong()
    {
        return getSecureRandom().nextLong();
    }

    public static float randomFloat()
    {
        return getSecureRandom().nextFloat();
    }

    public static double randomDouble()
    {
        return getSecureRandom().nextDouble();
    }

    public static double randomGaussian()
    {
        return getSecureRandom().nextGaussian();
    }
        
}
