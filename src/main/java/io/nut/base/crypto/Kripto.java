/*
 *  Kripto.java
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

import io.nut.base.encoding.Ascii85;
import io.nut.base.util.Byter;
import io.nut.base.util.Strings;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
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
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A utility class providing cryptographic operations including encryption, decryption,
 * key generation, digital signatures, and secure random number generation.
 *
 * @author franci
 */
public class Kripto
{
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Static Values ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
   
    /**
     * The UTF-8 charset name constant.
     */
    public static final String UTF8 = StandardCharsets.UTF_8.name();
    
    private static final String NOPADDING = "NoPadding";
    private static final String GCM = "GCM";
    
    /**
     * Constant for encryption mode, as defined in {@link Cipher#ENCRYPT_MODE}.
     */
    public static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
    
    /**
     * Constant for decryption mode, as defined in {@link Cipher#DECRYPT_MODE}.
     */
    public static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;
    
    /**
     * Constant for key wrapping mode, as defined in {@link Cipher#WRAP_MODE}.
     */
    public static final int WRAP_MODE    = Cipher.WRAP_MODE;
    
    /**
     * Constant for key unwrapping mode, as defined in {@link Cipher#UNWRAP_MODE}.
     */
    public static final int UNWRAP_MODE  = Cipher.UNWRAP_MODE;
    
    /**
     * Constant for public key type, as defined in {@link Cipher#PUBLIC_KEY}.
     */
    public static final int PUBLIC_KEY   = Cipher.PUBLIC_KEY;
    
    /**
     * Constant for private key type, as defined in {@link Cipher#PRIVATE_KEY}.
     */
    public static final int PRIVATE_KEY  = Cipher.PRIVATE_KEY;
    
    /**
     * Constant for secret key type, as defined in {@link Cipher#SECRET_KEY}.
     */
    public static final int SECRET_KEY   = Cipher.SECRET_KEY;

    ////////////////////////////////////////////////////////////////////////////
    ///// GOOD PRACTICES ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Recommended key size in bits for symmetric encryption (safe level).
     */
    public static final int SYMETRIC_SAFE_KEY_BITS = 128;
    
    /**
     * Intermediate key size in bits for symmetric encryption (half level).
     */
    public static final int SYMETRIC_HALF_KEY_BITS = 192;
    
    /**
     * High-security key size in bits for symmetric encryption (high level).
     */
    public static final int SYMETRIC_HIGH_KEY_BITS = 256;
    
    
    private static final int MINIMUM_PBKDF2_ROUNDS = 310000;
    
    /**
     * The AES algorithm for secret key operations.
     */
    public static final SecretKeyAlgorithm AES = SecretKeyAlgorithm.AES;
    
    /**
     * The RSA algorithm for key pair operations.
     */
    public static final KeyPairAlgorithm RSA = KeyPairAlgorithm.RSA;
        
    /**
     * AES transformation with CBC mode and PKCS5 padding.
     */
    public static final SecretKeyTransformation AES_CBC_PKCS5PADDING = SecretKeyTransformation.AES_CBC_PKCS5Padding;

    /**
     * AES transformation with GCM mode and no padding; DO NOT REPEAT IV, ALWAYS USE A RANDOM ONE.
     */
    public static final SecretKeyTransformation AES_GCM_NOPADDING = SecretKeyTransformation.AES_GCM_NoPadding;

    /**
     * RSA transformation with ECB mode and OAEP padding using SHA-256 and MGF1.
     */
    public static final KeyPairTransformation RSA_ECB_OAEPWITHSHA256ANDMGF1PADDING = KeyPairTransformation.RSA_ECB_OAEPWithSHA256AndMGF1Padding;

    /**
     * Signature algorithm using SHA-256 with ECDSA.
     */
    public static final SignatureAlgorithm SHA256WITHECDSA = SignatureAlgorithm.SHA256withECDSA;

    /**
     * Message digest algorithm using SHA-256.
     */
    public static final MessageDigestAlgorithm SHA256 = MessageDigestAlgorithm.SHA256;
    
    /**
     * Secret key derivation algorithm using PBKDF2 with HMAC SHA-256.
     */
    public static final SecretKeyDerivation PBKDF2WITHHMACSHA256 = SecretKeyDerivation.PBKDF2WithHmacSHA256;

    /**
     * Secret key derivation algorithm using PBKDF2 with HMAC SHA-512.
     */
    public static final SecretKeyDerivation PBKDF2WITHHMACSHA512 = SecretKeyDerivation.PBKDF2WithHmacSHA512;

    ////////////////////////////////////////////////////////////////////////////
    ///// Static Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

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
        
    private volatile int minimumPbkdf2Rounds = MINIMUM_PBKDF2_ROUNDS;
    
    /**
     * Returns a default instance of {@link Kripto} with no specific provider.
     *
     * @return a new Kripto instance
     */
    public static Kripto getInstance()
    {
        return new Kripto();
    }
    
    static Kripto getInstance(String providerName)
    {
        return new Kripto(providerName);
    }
    
    /**
     * Returns an instance of {@link Kripto}, optionally preferring Bouncy Castle provider.
     *
     * @param preferBouncyCastle true to prefer Bouncy Castle, false for default provider
     * @return a new Kripto instance
     */
    public static Kripto getInstance(boolean preferBouncyCastle)
    {
        Kripto instance = preferBouncyCastle ? getInstanceBouncyCastle() : null;
        return instance!=null ? instance : new Kripto();
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
                Logger.getLogger(Kripto.class.getName()).log(Level.SEVERE, null, ex);
                registeredBouncyCastle = false;
            }
        }
        return registeredBouncyCastle;
    }
    
    /**
     * Returns an instance of {@link Kripto} using the Bouncy Castle provider if available.
     *
     * @return a new Kripto instance with Bouncy Castle provider, or null if unavailable
     */
    public static Kripto getInstanceBouncyCastle()
    {
        return registerBouncyCastle() ? new Kripto("BC") : null;
    }
    
    /**
     * Checks if the Bouncy Castle provider's main class is available on the classpath.
     *
     * @return true if Bouncy Castle is present, false otherwise.
     */
    public static boolean isBouncyCastleAvailable() 
    {
        try 
        {
            // Intentamos cargar la clase principal del proveedor de Bouncy Castle.
            // No necesitamos una instancia, solo verificar que la clase existe.
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            return true;
        } 
        catch (ClassNotFoundException ex) 
        {
            return false;
        }
    }    

    public Kripto setMinimumPbkdf2Rounds(int value)
    {
        this.minimumPbkdf2Rounds = value;
        return this;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Enums /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    //https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyAgreement

    public enum SecretKeyTransformation
    {
        //Symetric Algorithms
        AES_CBC_NoPadding("AES/CBC/NoPadding",              128, 128, 0),  //(128,192,256) iv=128
        AES_GCM_NoPadding("AES/GCM/NoPadding",              128, 96, 128), //(128,192,256) iv=96   GOOD
        AES_CTR_NoPadding("AES/CTR/NoPadding",              128, 128, 128),//(128,192,256) iv=128  GOOD
        AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding",        128, 128, 0),  //(128,192,256) iv=128  GOOD
        AES_CFB8_NoPadding("AES/CFB8/NoPadding",            128, 128, 0);  //(128)         iv=128
        
        public final String transformation;
        public final String algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;
        public final boolean gcm;
        public final int blockBits;
        public final int ivBits;
        public final int tagBits;
        
        SecretKeyTransformation(String transformation, int blockBits, int ivBits, int tagBits)
        {
            String[] items = transformation.split("/");
            this.algorithm = items[0];
            this.mode = items[1];
            this.padding = items[2];
            this.transformation = transformation;
            this.nopadding = NOPADDING.equalsIgnoreCase(padding);
            this.gcm = GCM.equalsIgnoreCase(mode);
            this.blockBits = blockBits;
            this.ivBits = ivBits;
            this.tagBits = tagBits;
        }
        
        /**
         * Returns the maximum allowed key length for this transformation's algorithm.
         *
         * @return the maximum key length in bits
         * @throws NoSuchAlgorithmException if the algorithm is not available
         */
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm);
        }
    }
    
    public enum KeyPairTransformation
    {
        @Deprecated
        RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding"),                                  //(1024,2048)
        @Deprecated
        RSA_ECB_OAEPWithSHA1AndMGF1Padding("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"),     //(1024,2048)
        RSA_ECB_OAEPWithSHA256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");  //(1024, 2048)  GOOD        

        public final String transformation;
        public final String algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;
        
        KeyPairTransformation(String transformation)
        {
            String[] items = transformation.split("/");
            this.algorithm = items[0];
            this.mode = items[1];
            this.padding = items[2];
            this.transformation = transformation;
            this.nopadding = NOPADDING.equalsIgnoreCase(padding);
        }
        
        /**
         * Returns the maximum allowed key length for this transformation's algorithm.
         *
         * @return the maximum key length in bits
         * @throws NoSuchAlgorithmException if the algorithm is not available
         */
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm);
        }
    }
    
    public enum SecretKeyDerivation
    {
        PBKDF2WithHmacSHA256, PBKDF2WithHmacSHA512        //GOOD
    }
    
    public enum SecretKeyAlgorithm
    {
        AES
    }
    public enum KeyPairAlgorithm //KeyPair Algorithms, KeyFactory Algorithms
    {
        DiffieHellman, DSA, RSA, //mandatory DiffieHellman (1024), DSA (1024), RSA (1024, 2048)
        EC                      //optional   EC (192, 256)
    }
    
    public enum KeyAgreementAlgorithm
    {
        DiffieHellman, ECDH, ECMQV
    }

    public enum MessageDigestAlgorithm
    {
        SHA224("SHA-224"), 
        SHA256("SHA-256"),                                                      //GOOD
        SHA384("SHA-384"),                                                      //GOOD
        SHA512("SHA-512");                                                      //GOOD

        MessageDigestAlgorithm(String code)
        {
            this.code = code;
        }
        final String code;
    }

    public enum SignatureAlgorithm
    {
        NONEwithRSA, SHA224withRSA, 
        SHA256withRSA,                                                          //GOOD 
        SHA384withRSA, SHA512withRSA,
        NONEwithDSA, SHA224withDSA, SHA256withDSA, 
        NONEwithECDSA, SHA224withECDSA, 
        SHA256withECDSA,                                                        //GOOD
        SHA384withECDSA, SHA512withECDSA
    }
    public enum HMAC
    { 
        HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Instance Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    protected final String providerName;
    protected final boolean forceProvider;
    
    protected Kripto()
    {
        this(null,false);
    }
    protected Kripto(String providerName)
    {
        this(providerName,false);
    }
    protected Kripto(String providerName, boolean forceProvider)
    {
        this.providerName = providerName;
        this.forceProvider = forceProvider;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// PRIVATE METHODS //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName==null ? MessageDigest.getInstance(algorithm) : MessageDigest.getInstance(algorithm, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return MessageDigest.getInstance(algorithm);
        }
    }

    private Cipher getCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        try
        {
            return this.providerName==null ? Cipher.getInstance(transformation) : Cipher.getInstance(transformation, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return Cipher.getInstance(transformation);
        }
    }

    private KeyGenerator getKeyGenerator(String algorithm, int keyBits) throws NoSuchAlgorithmException 
    {
        try
        {
            KeyGenerator keyGen = this.providerName==null ? KeyGenerator.getInstance(algorithm) : KeyGenerator.getInstance(algorithm, this.providerName);
            keyGen.init(keyBits);
            return keyGen;
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keyBits);
            return keyGen;
        }
    }    

    private KeyPairGenerator getKeyPairGenerator(String algorithm, int keyBits) throws NoSuchAlgorithmException 
    {
        try
        {
            KeyPairGenerator keyGen = this.providerName==null ? KeyPairGenerator.getInstance(algorithm) : KeyPairGenerator.getInstance(algorithm, this.providerName);
            keyGen.initialize(keyBits);
            return keyGen;
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
            keyGen.initialize(keyBits);
            return keyGen;
        }
    }    
    
    private SecretKeyFactory getSecretKeyFactory(String algoritm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName==null ? SecretKeyFactory.getInstance(algoritm) : SecretKeyFactory.getInstance(algoritm, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return SecretKeyFactory.getInstance(algoritm);
        }       
    }

    private KeyFactory getKeyFactory(String algoritm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName==null ? KeyFactory.getInstance(algoritm) : KeyFactory.getInstance(algoritm, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return KeyFactory.getInstance(algoritm);
        }        
    }
    
    private KeyAgreement getKeyAgreement(String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        try
        {
            return this.providerName == null ? KeyAgreement.getInstance(algorithm) : KeyAgreement.getInstance(algorithm, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return KeyAgreement.getInstance(algorithm);
        }        
    }
    private Signature getSignature(String algorithm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName==null ? Signature.getInstance(algorithm) : Signature.getInstance(algorithm, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
            {
                throw new ProviderException(ex.getMessage(), ex);
            }
            return Signature.getInstance(algorithm);
        }        
    }

    private Mac getMac(String algorithm, SecretKey key) throws NoSuchAlgorithmException
    {
        Mac mac;
        try
        {
            mac = this.providerName==null ? Mac.getInstance(algorithm) : Mac.getInstance(algorithm, this.providerName);
        }
        catch(NoSuchProviderException ex)
        {
            if(this.forceProvider)
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
    
    private byte[] hmac(String algorithm, SecretKey key, byte[] data)
    {
        try
        {
            Mac mac = getMac(algorithm, key);
            return mac.doFinal(data);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalArgumentException("Unsupported MAC algorithm: " + algorithm, ex);
        }
    }    
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Message Diggest //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Returns a {@link MessageDigest} instance for the specified algorithm.
     *
     * @param algorithm the message digest algorithm to use
     * @return a MessageDigest instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public MessageDigest getMessageDigest(MessageDigestAlgorithm algorithm) throws NoSuchAlgorithmException
    {
        return getMessageDigest(algorithm.code);
    }
    
    /**
     * Returns a SHA-224 {@link MessageDigest} instance.
     *
     * @return a SHA-224 MessageDigest instance
     * @throws NoSuchAlgorithmException if SHA-224 is not available
     */
    public MessageDigest sha224() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA224);
    }
    
    /**
     * Returns a SHA-256 {@link MessageDigest} instance.
     *
     * @return a SHA-256 MessageDigest instance
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public MessageDigest sha256() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA256);
    }
    
    /**
     * Returns a SHA-384 {@link MessageDigest} instance.
     *
     * @return a SHA-384 MessageDigest instance
     * @throws NoSuchAlgorithmException if SHA-384 is not available
     */
    public MessageDigest sha384() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA384);
    }
    
    /**
     * Returns a SHA-512 {@link MessageDigest} instance.
     *
     * @return a SHA-512 MessageDigest instance
     * @throws NoSuchAlgorithmException if SHA-512 is not available
     */
    public MessageDigest sha512() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA512);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// HMAC facilities //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Returns a HMAC
     *
     * @param hash
     * @param key
     * @param data
     * @return the HMAC facilities class.
     */
    public byte[] hmac(HMAC hash, SecretKey key, byte[] data) 
    {
        return hmac(hash.name(), key, data);
    }    
    
    public byte[] hmac(HMAC hash, byte[] key, byte[] data) 
    {
        return hmac(hash, new SecretKeySpec(key, hash.name()), data);
    }
    
    public byte[] hmacSHA224(byte[] key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA224, key, data);
    }
    
    public byte[] hmacSHA256(byte[] key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA256, key, data);
    }
    
    public byte[] hmacSHA384(byte[] key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA384, key, data);
    }
    
    public byte[] hmacSHA512(byte[] key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA512, key, data);
    }
    
    public byte[] hmacSHA224(SecretKey key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA224, key, data);
    }
    
    public byte[] hmacSHA256(SecretKey key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA256, key, data);
    }
    
    public byte[] hmacSHA384(SecretKey key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA384, key, data);
    }
    
    public byte[] hmacSHA512(SecretKey key, byte[] data) 
    {
        return hmac(HMAC.HmacSHA512, key, data);
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
     * Returns a {@link KeyGenerator} for the specified secret key algorithm and key size.
     *
     * @param algorithm the secret key algorithm
     * @param keyBits the key size in bits
     * @return a KeyGenerator instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    public KeyGenerator getKeyGenerator(SecretKeyAlgorithm algorithm, int keyBits) throws NoSuchAlgorithmException 
    {
        return getKeyGenerator(algorithm.name(), keyBits);
    }
    
    /**
     * Returns a {@link KeyPairGenerator} for the specified key pair algorithm and key size.
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
    ///// IV ///////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creates an {@link IvParameterSpec} from the provided IV bytes.
     *
     * @param iv the initialization vector bytes
     * @return an IvParameterSpec instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     */
    public IvParameterSpec getIv(byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        return new IvParameterSpec(iv);
    }
    
    /**
     * Creates an {@link IvParameterSpec} from the provided IV bytes with specified bit length.
     *
     * @param iv the initialization vector bytes
     * @param ivBits the number of bits to use from the IV
     * @return an IvParameterSpec instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     */
    public IvParameterSpec getIv(byte[] iv, int ivBits) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        return new IvParameterSpec(iv, 0, ivBits/8);
    }
    
    /**
     * Creates a {@link GCMParameterSpec} for GCM mode from the provided IV bytes and bit length.
     *
     * @param iv the initialization vector bytes
     * @param tagBits the tag length in bits
     * @return a GCMParameterSpec instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchPaddingException if the padding is not available
     */
    public GCMParameterSpec getIvGCM(byte[] iv, int tagBits) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        return new GCMParameterSpec(tagBits, iv);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Salt facilities ////////////////////////////////////////////
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

    /**
     * Derives bytes from a character sequence using SHA-256.
     *
     * @param src the input character sequence
     * @return the derived bytes
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public byte[] deriveBytesSHA256(CharSequence src) throws NoSuchAlgorithmException
    {
        MessageDigest sha256 = this.sha256();
        sha256.update(normalizeNFKD(src).getBytes(StandardCharsets.UTF_8));
        return sha256.digest();
    }

    /**
     * Derives bytes from multiple character arrays using SHA-256.
     *
     * @param src the character arrays to process
     * @return the derived bytes
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public byte[] deriveBytesSHA256(char[]... src) throws NoSuchAlgorithmException 
    {
        MessageDigest sha256 = this.sha256();
        for(char[] item : src)
        {
            sha256.update(Byter.bytesUTF8(item));
        }
        return sha256.digest();
    }

    public byte[] deriveBytesSHA256(byte[]... src) throws NoSuchAlgorithmException 
    {
        MessageDigest sha256 = this.sha256();
        for(byte[] item : src)
        {
            sha256.update(item);
        }
        return sha256.digest();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Key Derivation facilities ////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Derives a {@link SecretKey} from a passphrase using the specified 
     * derivation algorithm.
     *
     * @param passphrase the passphrase to derive from
     * @param salt the salt to use
     * @param rounds the number of iteration rounds
     * @param keyBits the desired key size in bits
     * @param derivation the key derivation algorithm
     * @param secretKeyAlgorithm the target secret key algorithm
     * @return the derived SecretKey
     * @throws InvalidKeySpecException if the key specification is invalid
     */
    public SecretKey deriveSecretKey(char[] passphrase, byte[] salt, int rounds, int keyBits, SecretKeyDerivation derivation, SecretKeyAlgorithm secretKeyAlgorithm) throws InvalidKeySpecException
    {
        if(rounds<this.minimumPbkdf2Rounds)
        {
            throw new IllegalArgumentException("rounds = "+rounds+" < "+this.minimumPbkdf2Rounds);
        }
        try
        {
            SecretKeyFactory factory = this.getSecretKeyFactory(derivation.name());
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, rounds, keyBits);
            SecretKey genericSecretKey = factory.generateSecret(spec);
            return this.getSecretKey(genericSecretKey.getEncoded(), secretKeyAlgorithm);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalArgumentException("Unsupported SecretKeyFactory algorithm: " + derivation.name(), ex);
        }
    }

    public byte[] deriveSecretKeyEncoded(char[] passphrase, byte[] salt, int rounds, int keyBits, SecretKeyDerivation derivation) throws InvalidKeySpecException
    {
        if(rounds<this.minimumPbkdf2Rounds)
        {
            throw new IllegalArgumentException("rounds = "+rounds+" < "+this.minimumPbkdf2Rounds);
        }
        try
        {
            SecretKeyFactory factory = this.getSecretKeyFactory(derivation.name());
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, rounds, keyBits);
            SecretKey genericSecretKey = factory.generateSecret(spec);
            return genericSecretKey.getEncoded();
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalArgumentException("Unsupported SecretKeyFactory algorithm: " + derivation.name(), ex);
        }
    }

    public char[] deriveSecretKeyAscii85(char[] passphrase, byte[] salt, int rounds, int keyBits, SecretKeyDerivation derivation) throws InvalidKeySpecException
    {
        byte[] tmp = deriveSecretKeyEncoded(passphrase, salt, rounds, keyBits, derivation);
        try
        {
            return Ascii85.encode(tmp);
        }
        finally
        {
            Arrays.fill(tmp, (byte)0);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// KeyAgreement Algorithms ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Performs a key agreement to generate a shared {@link SecretKey}.
     *
     * @param kpa the key pair algorithm
     * @param kaa the key agreement algorithm
     * @param privateKeyBytes the private key bytes
     * @param foreignKeyBytes the foreign public key bytes
     * @return the shared SecretKey
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException if the key is invalid
     * @throws InvalidAlgorithmParameterException if the parameters are invalid
     * @throws InvalidKeySpecException if the key specification is invalid
     * @throws NoSuchPaddingException if the padding is not available
    //use the pair (EC,ECDH) or (DiffieHellman,DiffieHellman)
     */
    public SecretKey makeAgreement(KeyPairAlgorithm kpa, KeyAgreementAlgorithm kaa, byte[] privateKeyBytes, byte[] foreignKeyBytes) throws NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchPaddingException
    {
        KeyFactory keyFactory = this.getKeyFactory(kpa.name());
        KeyAgreement keyAgreement = this.getKeyAgreement(kaa.name());
        
        X509EncodedKeySpec foreignSpec = new X509EncodedKeySpec(foreignKeyBytes);
        PublicKey  foreignKey = keyFactory.generatePublic(foreignSpec);
        
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privateSpec);
        
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(foreignKey, true);
        return keyAgreement.generateSecret(kpa.name());
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
     * @throws BadPaddingException if the padding is invalid
    // DO NOT IMPLEMENT wrap and unwrap for PublicKey or PrivateKey because it will fail, RSA will not allow such a big key as data
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
     * Verifies a signature using a public key and specified signature algorithm.
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
    ///// Debug things /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Displays information about all registered security providers.
     *
     * @param out the PrintStream to output the information
     */
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
    
    /**
     * Displays information about a specific security provider.
     *
     * @param out the PrintStream to output the information
     * @param provider the Provider to display information about
     */
    public static void showProviderInfo(PrintStream out, Provider provider)
    {
        out.println(provider.getInfo());
        for (Provider.Service service: provider.getServices())
        {
            out.println("  "+service.toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Random data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private enum StrongSecureRandomHolder
    {
        INSTANCE;
        final SecureRandom secureRandom = getSecureRandomStrong();
    }

    public static SecureRandom getSecureRandom(boolean strong)
    {
        return strong ? StrongSecureRandomHolder.INSTANCE.secureRandom : new SecureRandom();
    }
    
    public static SecureRandom getSecureRandom()
    {
        return new SecureRandom();
    }

    public static Rand getRand(boolean strong)
    {
        return new Rand(getSecureRandom(strong));
    }
    public static Rand getRand()
    {
        return new Rand(getSecureRandom());
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Shared Secrets n of m share the secret key ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creates a new {@link ShamirSharedSecret} instance for secret sharing.
     *
     * @param n the total number of shares
     * @param k the minimum number of shares required to reconstruct the secret
     * @return a new ShamirSharedSecret instance
     */
    public static ShamirSharedSecret getShamirSharedSecret(int n, int k)
    {
        return new ShamirSharedSecret(n, k);
    }
    
    public PassphraseDeriver getPassphraseDeriver(char[] masterPassphrase, int keyBits, int rounds, boolean cache) throws Exception
    {
        return new PassphraseDeriver(masterPassphrase, keyBits, rounds, cache, this);
    }

    
    public KeyStore getKeyStore(String type) throws KeyStoreException, NoSuchProviderException
    {
        return this.providerName==null ? KeyStore.getInstance(type) : KeyStore.getInstance(type, this.providerName);
    }
    
    private static final String PKCS12 = "PKCS12";
    
    public KeyStore getKeyStorePKCS12() throws KeyStoreException, NoSuchProviderException
    {
        return this.providerName==null ? KeyStore.getInstance(PKCS12) : KeyStore.getInstance(PKCS12, this.providerName);
    }

    public KeyStoreManager getKeyStoreManager() throws KeyStoreException, NoSuchProviderException, Exception
    {
        return new KeyStoreManager(this.getKeyStorePKCS12());
    }
}
