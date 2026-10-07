/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.jca;

import io.nut.base.util.As;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.text.Normalizer;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provider aware JCA plumbing, base class of the crypto layer.
 *
 * <p>
 * This class holds the internals that were private in
 * io.nut.base.crypto.Kripto: the JCA factory methods with provider fallback,
 * the operation mode constants, the strong random state and the Bouncy Castle
 * registration state. They are protected here so subclasses can reuse them.
 *
 * <p>
 * This class depends only on the JDK and on nut-base-commons itself: it takes
 * no dependency on the classes or enums of io.nut.base.crypto, nor on the
 * nut-base-crypto module.
 *
 * @author franci
 */
public class Kr
{
    protected static final Logger LOG = Logger.getLogger(Kr.class.getName());

    ////////////////////////////////////////////////////////////////////////////
    ///// Static Values ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // The recommended IV size for GCM is 96 bits (12 bytes) for performance reasons.
    public static final int GCM_IV_BITS = 96;
    public static final int GCM_IV_BYTES = GCM_IV_BITS / 8;

    // The recommended TAG size for GCM is 128 bits (16 bytes) for security reasons.
    public static final int GCM_TAG_BITS = 128;
    public static final int GCM_TAG_BYTES = GCM_TAG_BITS / 8;

    public static final int CHACHA20_IV_BITS = 96;
    public static final int CHACHA20_IV_BYTES = CHACHA20_IV_BITS / 8;
    public static final int CHACHA20_TAG_BITS = 128;
    public static final int CHACHA20_TAG_BYTES = CHACHA20_TAG_BITS / 8;

    protected static final String NOPADDING = "NoPadding";
    protected static final String GCM = "GCM";

    /**
     * Constant for encryption mode, as defined in {@link Cipher#ENCRYPT_MODE}.
     */
    protected static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;

    /**
     * Constant for decryption mode, as defined in {@link Cipher#DECRYPT_MODE}.
     */
    protected static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;

    /**
     * Constant for key wrapping mode, as defined in {@link Cipher#WRAP_MODE}.
     */
    protected static final int WRAP_MODE = Cipher.WRAP_MODE;

    /**
     * Constant for key unwrapping mode, as defined in
     * {@link Cipher#UNWRAP_MODE}.
     */
    protected static final int UNWRAP_MODE = Cipher.UNWRAP_MODE;

    /**
     * Constant for private key type, as defined in {@link Cipher#PRIVATE_KEY}.
     */
    protected static final int PRIVATE_KEY = Cipher.PRIVATE_KEY;

    /**
     * Constant for secret key type, as defined in {@link Cipher#SECRET_KEY}.
     */
    protected static final int SECRET_KEY = Cipher.SECRET_KEY;

    ////////////////////////////////////////////////////////////////////////////
    ///// Random data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    protected static volatile boolean drbgUnavailable = false;

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

    protected static class StrongHolder
    {
        static final SecureRandom STRONG = getSecureRandomStrong();
    }

    public static SecureRandom getSecureRandomStrongFast()
    {
        if (!drbgUnavailable)
        {
            try
            {
                SecureRandom sr = SecureRandom.getInstance("DRBG"); // Java 9+
                sr.setSeed(StrongHolder.STRONG.generateSeed(64)); // 512 bits de entropía real
                return sr;
            }
            catch (NoSuchAlgorithmException e)
            {
                drbgUnavailable = true;
            }
        }

        return getSecureRandomStrong(); // Java 8
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Bouncy Castle registration ///////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    ///// Random data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public static SecureRandom getSecureRandom()
    {
        return new SecureRandom();
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Enums /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    //https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyAgreement

    public enum SecretKeyAlgorithm
    {
        AES, ChaCha20
    }
    
    public enum MessageDigestAlgorithm
    {
        @Deprecated
        MD5("MD5"),
        @Deprecated
        SHA1("SHA1"),
        @Deprecated
        SHA224("SHA-224"),
        SHA256("SHA-256"), //GOOD
        SHA384("SHA-384"), //GOOD
        SHA512("SHA-512"), //GOOD
        RIPEMD160("RIPEMD160");                                                 //GOOD

        MessageDigestAlgorithm(String code)
        {
            this.code = code;
        }
        public final String code;
    }
    
    public enum KeyPairAlgorithm //KeyPair Algorithms, KeyFactory Algorithms
    {
        DiffieHellman, DSA, RSA, //mandatory DiffieHellman (1024), DSA (1024), RSA (1024, 2048)
        EC                      //optional   EC (192, 256)
    }

    public enum Pbkdf2
    {
        PBKDF2WithHmacSHA256, PBKDF2WithHmacSHA512
    }
    
    public enum SecretKeyTransformation
    {
        //Symetric Algorithms
        AES_GCM_NoPadding("AES/GCM/NoPadding", 128, 96, 128), //(128,192,256) iv=96   GOOD
        AES_CTR_NoPadding("AES/CTR/NoPadding", 128, 128, 128),//(128,192,256) iv=128  GOOD
        AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding", 128, 128, 0), //(128,192,256) iv=128  GOOD
        AES_CFB8_NoPadding("AES/CFB8/NoPadding", 128, 128, 0),  //(128)         iv=128
        ChaCha20_Poly1305("ChaCha20-Poly1305", 512, 96, 128),  //(256)         iv=96
        ChaCha20("ChaCha20", 512, 96, 128);  //(256)         iv=96

        public final String transformation;
        public final SecretKeyAlgorithm algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;
        public final boolean gcm;
        public final int blockBits;
        public final int ivBits;
        public final int tagBits;

        SecretKeyTransformation(String transformation, int blockBits, int ivBits, int tagBits)
        {
            String[] items = transformation.split("[/-]");
            this.algorithm = SecretKeyAlgorithm.valueOf(items[0]);
            this.mode = items.length>1 ? items[1] : "";
            this.padding = items.length>2 ? items[2] : "";
            this.transformation = transformation;
            this.nopadding = NOPADDING.equalsIgnoreCase(padding) || padding.isEmpty();
            this.gcm = GCM.equalsIgnoreCase(mode);
            this.blockBits = blockBits;
            this.ivBits = ivBits;
            this.tagBits = tagBits;
        }

        /**
         * Returns the maximum allowed key length for this transformation's
         * algorithm.
         *
         * @return the maximum key length in bits
         * @throws NoSuchAlgorithmException if the algorithm is not available
         */
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm.name());
        }
    }

    public enum KeyPairTransformation
    {
        @Deprecated
        RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding"), //(1024,2048)
        @Deprecated
        RSA_ECB_OAEPWithSHA1AndMGF1Padding("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"), //(1024,2048)
        RSA_ECB_OAEPWithSHA256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");  //(1024, 2048)  GOOD        

        public final String transformation;
        public final KeyPairAlgorithm algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;

        KeyPairTransformation(String transformation)
        {
            String[] items = transformation.split("/");
            this.algorithm = KeyPairAlgorithm.valueOf(items[0]);
            this.mode = items.length>1 ? items[1] : "";
            this.padding = items.length>2 ? items[2] : "";
            this.transformation = transformation;
            this.nopadding = NOPADDING.equalsIgnoreCase(padding);
        }

        /**
         * Returns the maximum allowed key length for this transformation's
         * algorithm.
         *
         * @return the maximum key length in bits
         * @throws NoSuchAlgorithmException if the algorithm is not available
         */
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm.name());
        }
    }
    
    public enum SignatureAlgorithm
    {
        @Deprecated
        NONEwithRSA, 
        @Deprecated
        NONEwithDSA, 
        @Deprecated
        NONEwithECDSA, 
        @Deprecated
        SHA224withRSA,
        @Deprecated
        SHA224withDSA, 
        @Deprecated
        SHA224withECDSA,
        SHA256withRSA, SHA384withRSA, SHA512withRSA,
        SHA256withDSA,
        SHA256withECDSA, SHA384withECDSA, SHA512withECDSA,
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Enums /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////


    public enum KeyAgreementAlgorithm
    {
        DiffieHellman, ECDH, ECMQV
    }

    public enum Hmac
    {
        @Deprecated
        HmacSHA224, 
        HmacSHA256, HmacSHA384, HmacSHA512
    }
    
    public enum Hkdf
    {
        HkdfWithSha256, HkdfWithSha384, HkdfWithSha512
    }
    
    public enum KeyStoreType
    {
        @Deprecated
        JKS, 
        @Deprecated
        JCEKS, 
        PKCS12, BCFKS
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Instance Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    protected final String providerName;
    protected final boolean forceProvider;

    public Kr()
    {
        this(null, false);
    }

    protected Kr(String providerName)
    {
        this(providerName, false);
    }

    protected Kr(String providerName, boolean forceProvider)
    {
        this.providerName = providerName;
        this.forceProvider = forceProvider;
    }
    
    private enum Holder
    {
        INSTANCE;
        final Kr kr = new Kr();
    }
    
    public static Kr getInstance()
    {
        return Holder.INSTANCE.kr;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// PROTECTED METHODS ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link MessageDigest} instance for the specified algorithm.
     *
     * @param algorithm the message digest algorithm to use
     * @return a MessageDigest instance
     */
    public MessageDigest getMessageDigest(MessageDigestAlgorithm algorithm)
    {
        return getMessageDigest(algorithm.code);
    }

    /**
     * Returns a {@link MessageDigest} instance for the specified algorithm.
     *
     * @param algorithm the message digest algorithm to use
     * @return a MessageDigest instance
     */
    protected MessageDigest getMessageDigest(String algorithm)
    {
        try
        {
            return this.providerName == null ? MessageDigest.getInstance(algorithm) : MessageDigest.getInstance(algorithm, this.providerName);
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            try
            {
                if (this.providerName != null)
                {
                    return MessageDigest.getInstance(algorithm);
                }
                throw new RuntimeException(ex.getMessage(), ex);
            }
            catch (NoSuchAlgorithmException ex2)
            {
                throw new RuntimeException(ex2.getMessage(), ex2);
            }
        }
    }

    protected Cipher getCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        try
        {
            return this.providerName == null ? Cipher.getInstance(transformation) : Cipher.getInstance(transformation, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return Cipher.getInstance(transformation);
        }
    }

    protected KeyGenerator getKeyGenerator(String algorithm, int keyBits) throws NoSuchAlgorithmException
    {
        try
        {
            KeyGenerator keyGen = this.providerName == null ? KeyGenerator.getInstance(algorithm) : KeyGenerator.getInstance(algorithm, this.providerName);
            keyGen.init(keyBits);
            return keyGen;
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keyBits);
            return keyGen;
        }
    }

    protected KeyPairGenerator getKeyPairGenerator(String algorithm, int keyBits) throws NoSuchAlgorithmException
    {
        try
        {
            KeyPairGenerator keyGen = this.providerName == null ? KeyPairGenerator.getInstance(algorithm) : KeyPairGenerator.getInstance(algorithm, this.providerName);
            keyGen.initialize(keyBits);
            return keyGen;
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
            keyGen.initialize(keyBits);
            return keyGen;
        }
    }

    protected KeyFactory getKeyFactory(String algoritm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName == null ? KeyFactory.getInstance(algoritm) : KeyFactory.getInstance(algoritm, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return KeyFactory.getInstance(algoritm);
        }
    }

    protected KeyAgreement getKeyAgreement(String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        try
        {
            return this.providerName == null ? KeyAgreement.getInstance(algorithm) : KeyAgreement.getInstance(algorithm, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return KeyAgreement.getInstance(algorithm);
        }
    }

    protected Signature getSignature(String algorithm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName == null ? Signature.getInstance(algorithm) : Signature.getInstance(algorithm, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return Signature.getInstance(algorithm);
        }
    }

    protected KeyStore getKeyStore(String type) throws KeyStoreException
    {
        try
        {
            return this.providerName == null ? KeyStore.getInstance(type) : KeyStore.getInstance(type, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return KeyStore.getInstance(type);
        }
    }

    protected Mac getMac(String algorithm, SecretKey key) throws NoSuchAlgorithmException
    {
        Mac mac;
        try
        {
            mac = this.providerName == null ? Mac.getInstance(algorithm) : Mac.getInstance(algorithm, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            mac = Mac.getInstance(algorithm);
        }
        try
        {
            mac.init(key);
        }
        catch (InvalidKeyException e)
        {
            throw new IllegalArgumentException("Invalid MAC key", e);
        }
        return mac;
    }
        
    ////////////////////////////////////////////////////////////////////////////
    ///// MISC /////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Normalizes a character sequence using NFKD normalization form.
     *
     * @param cs the character sequence to normalize
     * @return the normalized string
     */
    public static String normalizeNFKD(CharSequence cs)
    {
        return Normalizer.normalize(cs, Normalizer.Form.NFKD);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// IV ///////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creates an {@link IvParameterSpec} from the provided IV bytes.
     *
     * @param iv the initialization vector bytes
     * @return an IvParameterSpec instance
     */
    public IvParameterSpec getIv(byte[] iv)
    {
        return new IvParameterSpec(iv);
    }

    /**
     * Creates an {@link IvParameterSpec} from the provided IV bytes with
     * specified bit length.
     *
     * @param iv the initialization vector bytes
     * @param ivBits the number of bits to use from the IV
     * @return an IvParameterSpec instance
     */
    public IvParameterSpec getIv(byte[] iv, int ivBits)
    {
        return new IvParameterSpec(iv, 0, ivBits / 8);
    }

    /**
     * Creates a {@link GCMParameterSpec} for GCM mode from the provided IV
     * bytes and bit length.
     *
     * @param iv the initialization vector bytes
     * @param tagBits the tag length in bits
     * @return a GCMParameterSpec instance
     */
    public GCMParameterSpec getIvGCM(byte[] iv, int tagBits) 
    {
        return new GCMParameterSpec(tagBits, iv);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Keys /////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a {@link SecretKey} from the provided byte array and algorithm.
     *
     * @param secretKey the key material
     * @param algoritm the secret key algorithm
     * @return a new SecretKey instance
     */
    public SecretKey getSecretKey(byte[] secretKey, SecretKeyAlgorithm algoritm)
    {
        return new SecretKeySpec(secretKey, algoritm.name());
    }

    /**
     * Returns a {@link KeyGenerator} for the specified secret key algorithm and
     * key size.
     *
     * @param algorithm the secret key algorithm
     * @param keyBits the key size in bits
     * @return a KeyGenerator instance
     */
    public KeyGenerator getKeyGenerator(SecretKeyAlgorithm algorithm, int keyBits)
    {
        try
        {
            return getKeyGenerator(algorithm.name(), keyBits);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns a {@link KeyPairGenerator} for the specified key pair algorithm
     * and key size.
     *
     * @param algorithm the key pair algorithm
     * @param keyBits the key size in bits
     * @return a KeyPairGenerator instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public KeyPairGenerator getKeyPairGenerator(KeyPairAlgorithm algorithm, int keyBits) throws NoSuchAlgorithmException
    {
        return getKeyPairGenerator(algorithm.name(), keyBits);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// SecretKey Ciphers ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a configured {@link Cipher} for secret key operations.
     *
     * @param secretKey the secret key to use
     * @param transformation the transformation to apply
     * @param iv the initialization vector parameters
     * @param opmode the operation mode (e.g., {@link #ENCRYPT_MODE})
     * @return a configured Cipher instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     */
    public Cipher getCipher(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = getCipher(transformation.transformation);
        cipher.init(opmode, secretKey, iv);
        return cipher;
    }
    
    /**
     * Encrypts data using a secret key and specified transformation.
     *
     * @param secretKey the secret key to use
     * @param transformation the transformation to apply
     * @param iv the initialization vector parameters
     * @param data the data to encrypt
     * @return the encrypted data
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public byte[] encrypt(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(secretKey, transformation, iv, ENCRYPT_MODE).doFinal(data);
    }

    /**
     * Decrypts data using a secret key and specified transformation.
     *
     * @param secretKey the secret key to use
     * @param transformation the transformation to apply
     * @param iv the initialization vector parameters
     * @param data the data to decrypt
     * @return the decrypted data
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public byte[] decrypt(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(secretKey, transformation, iv, DECRYPT_MODE).doFinal(data);
    }

    /**
     * Wraps a key using a secret key and specified transformation.
     *
     * @param secretKey the secret key to use
     * @param transformation the transformation to apply
     * @param iv the initialization vector parameters
     * @param key the key to wrap
     * @return the wrapped key bytes
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public byte[] wrap(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(secretKey, transformation, iv, WRAP_MODE).wrap(key);
    }


    /**
     * Unwraps a secret key using a secret key and specified transformation.
     *
     * @param secretKey the secret key to use
     * @param transformation the transformation to apply
     * @param iv the initialization vector parameters
     * @param key the wrapped key bytes
     * @param secretKeyAlgorithm the algorithm of the key to unwrap
     * @return the unwrapped SecretKey
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public SecretKey unwrap(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] key, SecretKeyAlgorithm secretKeyAlgorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return (SecretKey) getCipher(secretKey, transformation, iv, UNWRAP_MODE).unwrap(key, secretKeyAlgorithm.name(), SECRET_KEY);
    }

    /**
     * Unwraps a private key using a secret key and specified transformation.
     *
     * @param secretKey the secret key to use
     * @param transformation the transformation to apply
     * @param iv the initialization vector parameters
     * @param key the wrapped key bytes
     * @param keyPairAlgorithm the algorithm of the key to unwrap
     * @return the unwrapped PrivateKey
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public PrivateKey unwrap(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] key, KeyPairAlgorithm keyPairAlgorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return (PrivateKey) getCipher(secretKey, transformation, iv, UNWRAP_MODE).unwrap(key, keyPairAlgorithm.name(), PRIVATE_KEY);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// KeyPair Ciphers //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a configured {@link Cipher} for public key operations.
     *
     * @param pubKey the public key to use
     * @param transformation the transformation to apply
     * @param opmode the operation mode (e.g., {@link #ENCRYPT_MODE})
     * @return a configured Cipher instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     */
    public Cipher getCipher(PublicKey pubKey, KeyPairTransformation transformation, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = getCipher(transformation.transformation);
        cipher.init(opmode, pubKey);
        return cipher;
    }

    /**
     * Returns a configured {@link Cipher} for private key operations.
     *
     * @param prvKey the private key to use
     * @param transformation the transformation to apply
     * @param opmode the operation mode (e.g., {@link #DECRYPT_MODE})
     * @return a configured Cipher instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     */
    public Cipher getCipher(PrivateKey prvKey, KeyPairTransformation transformation, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = getCipher(transformation.transformation);
        cipher.init(opmode, prvKey);
        return cipher;
    }

    /**
     * Encrypts data using a public key and specified transformation.
     *
     * @param pubKey the public key to use
     * @param transformation the transformation to apply
     * @param data the data to encrypt
     * @return the encrypted data
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public byte[] encrypt(PublicKey pubKey, KeyPairTransformation transformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(pubKey, transformation, ENCRYPT_MODE).doFinal(data);
    }

    /**
     * Decrypts data using a private key and specified transformation.
     *
     * @param prvKey the private key to use
     * @param transformation the transformation to apply
     * @param data the data to decrypt
     * @return the decrypted data
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public byte[] decrypt(PrivateKey prvKey, KeyPairTransformation transformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(prvKey, transformation, DECRYPT_MODE).doFinal(data);
    }

    /**
     * Wraps a secret key using a public key and specified transformation.
     *
     * @param pubKey the public key to use
     * @param transformation the transformation to apply
     * @param key the secret key to wrap
     * @return the wrapped key bytes
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid // DO NOT IMPLEMENT
     * wrap and unwrap for PublicKey or PrivateKey because it will fail, RSA
     * will not allow such a big key as data
     */
    public byte[] wrap(PublicKey pubKey, KeyPairTransformation transformation, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(pubKey, transformation, WRAP_MODE).wrap(key);
    }

    /**
     * Unwraps a secret key using a private key and specified transformation.
     *
     * @param prvKey the private key to use
     * @param transformation the transformation to apply
     * @param key the wrapped key bytes
     * @param secretKeyAlgorithm the algorithm of the key to unwrap
     * @return the unwrapped SecretKey
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws IllegalBlockSizeException if the block size is invalid
     * @throws BadPaddingException if the padding is invalid
     */
    public SecretKey unwrap(PrivateKey prvKey, KeyPairTransformation transformation, byte[] key, SecretKeyAlgorithm secretKeyAlgorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return (SecretKey) getCipher(prvKey, transformation, UNWRAP_MODE).unwrap(key, secretKeyAlgorithm.name(), SECRET_KEY);
    }


    ////////////////////////////////////////////////////////////////////////////
    ///// Signatures ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
        
    /**
     * Returns a {@link Signature} instance for the specified algorithm.
     *
     * @param algorithm the signature algorithm to use
     * @return a Signature instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public Signature getSignature(SignatureAlgorithm algorithm) throws NoSuchAlgorithmException
    {
        return this.getSignature(algorithm.name());
    }

    /**
     * Signs data using a private key and specified signature algorithm.
     *
     * @param algorithm the signature algorithm to use
     * @param privateKey the private key for signing
     * @param data the data to sign (multiple arrays)
     * @return the signature bytes
     * @throws InvalidKeyException if the key is invalid
     * @throws SignatureException if the signature process fails
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public byte[] sign(SignatureAlgorithm algorithm, PrivateKey privateKey, byte[]... data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException
    {
        Signature signature = this.getSignature(algorithm);
        signature.initSign(privateKey);
        for (byte[] item : data)
        {
            signature.update(item);
        }
        return signature.sign();
    }

    /**
     * Verifies a signature using a public key and specified signature
     * algorithm.
     *
     * @param algorithm the signature algorithm to use
     * @param publicKey the public key for verification
     * @param sign the signature bytes to verify
     * @param data the data to verify (multiple arrays)
     * @return true if the signature is valid, false otherwise
     * @throws InvalidKeyException if the key is invalid
     * @throws SignatureException if the verification process fails
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public boolean verify(SignatureAlgorithm algorithm, PublicKey publicKey, byte[] sign, byte[]... data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException
    {
        Signature signature = this.getSignature(algorithm);
        signature.initVerify(publicKey);
        for (byte[] item : data)
        {
            signature.update(item);
        }
        return signature.verify(sign);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Digest data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public HMAC getHMAC(Hmac algorithm)
    {
        return new HMAC(this, algorithm);
    }

    public Digest getDigest(MessageDigestAlgorithm algorithm)
    {
        return new Digest(this, algorithm);
    }

    //useful instances
    public final Digest sha256 = getDigest(MessageDigestAlgorithm.SHA256);
    public final Digest sha384 = getDigest(MessageDigestAlgorithm.SHA384);
    public final Digest sha512 = getDigest(MessageDigestAlgorithm.SHA512);
    
    ////////////////////////////////////////////////////////////////////////////
    ///// HMAC facilities //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public Mac getMac(Hmac hash, SecretKey key)
    {
        try
        {
            return getMac(hash.name(), key);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalArgumentException("Unsupported MAC algorithm: " + hash.name(), ex);
        }
    }
    
    /**
     * Creates a {@link SecretKey} from the provided byte array and algorithm.
     *
     * @param secretKey the key material
     * @param hmac the Hmac algorithm used as SecretKey
     * @return a new SecretKey instance
     */
    public SecretKey getSecretKey(byte[] secretKey, Hmac hmac)
    {
        return new SecretKeySpec(secretKey, hmac.name());
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Salt facilities ////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Derives bytes from a character sequence using SHA-256.
     *
     * @param src the input character sequence
     * @return the derived bytes
     */
    public byte[] deriveSaltSHA256(CharSequence src)
    {
        MessageDigest md = this.sha256.get();
        md.update(normalizeNFKD(src).getBytes(StandardCharsets.UTF_8));
        return md.digest();
    }

    /**
     * Derives bytes from multiple character arrays using SHA-256.
     *
     * @param src the character arrays to process
     * @return the derived bytes
     */
    public byte[] deriveSaltSHA256(char[]... src)
    {
        MessageDigest md = this.sha256.get();
        for (char[] item : src)
        {
            md.update(As.bytesUTF8(item));
        }
        return md.digest();
    }

    /**
     * Derives bytes from multiple byte arrays using SHA-256.
     *
     * @param src the byte arrays to process
     * @return the derived bytes
     */
    public byte[] deriveSaltSHA256(byte[]... src)
    {
        MessageDigest md = this.sha256.get();
        for (byte[] item : src)
        {
            md.update(item);
        }
        return md.digest();
    }

}
