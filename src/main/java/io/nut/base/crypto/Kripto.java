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

import io.nut.base.crypto.stego.Steganography;
import io.nut.base.util.Byter;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
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
import java.util.function.UnaryOperator;
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
import javax.crypto.spec.SecretKeySpec;

/**
 * A utility class providing cryptographic operations including encryption,
 * decryption, key generation, digital signatures, and secure random number
 * generation.
 *
 * @author franci
 */
public class Kripto
{

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
        
    private static final String NOPADDING = "NoPadding";
    private static final String GCM = "GCM";

    /**
     * Constant for encryption mode, as defined in {@link Cipher#ENCRYPT_MODE}.
     */
    static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;

    /**
     * Constant for decryption mode, as defined in {@link Cipher#DECRYPT_MODE}.
     */
    private static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;

    /**
     * Constant for key wrapping mode, as defined in {@link Cipher#WRAP_MODE}.
     */
    private static final int WRAP_MODE = Cipher.WRAP_MODE;

    /**
     * Constant for key unwrapping mode, as defined in
     * {@link Cipher#UNWRAP_MODE}.
     */
    private static final int UNWRAP_MODE = Cipher.UNWRAP_MODE;

    /**
     * Constant for private key type, as defined in {@link Cipher#PRIVATE_KEY}.
     */
    private static final int PRIVATE_KEY = Cipher.PRIVATE_KEY;

    /**
     * Constant for secret key type, as defined in {@link Cipher#SECRET_KEY}.
     */
    private static final int SECRET_KEY = Cipher.SECRET_KEY;

    ////////////////////////////////////////////////////////////////////////////
    ///// GOOD PRACTICES ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * AES transformation with GCM mode and no padding; DO NOT REPEAT IV, ALWAYS USE A RANDOM ONE.
     */
    public static final SecretKeyTransformation AES_GCM_NOPADDING = SecretKeyTransformation.AES_GCM_NoPadding;

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

    /**
     * Returns a default instance of {@link Kripto} with no specific provider.
     *
     * @return a new Kripto instance
     */
    public static Kripto getInstance()
    {
        return new Kripto();
    }

    public static Kripto getInstance(String providerName)
    {
        return new Kripto(providerName);
    }

    /**
     * Returns an instance of {@link Kripto}, optionally preferring Bouncy
     * Castle provider.
     *
     * @param preferBouncyCastle true to prefer Bouncy Castle, false for default
     * provider
     * @return a new Kripto instance
     */
    public static Kripto getInstance(boolean preferBouncyCastle)
    {
        Kripto instance = preferBouncyCastle ? getInstanceBouncyCastle() : null;
        return instance != null ? instance : new Kripto();
    }

    private static volatile boolean registeredBouncyCastle;

    public static boolean registerBouncyCastle()
    {
        if (!registeredBouncyCastle)
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
     * Returns an instance of {@link Kripto} using the Bouncy Castle provider if
     * available.
     *
     * @return a new Kripto instance with Bouncy Castle provider, or null if
     * unavailable
     */
    public static Kripto getInstanceBouncyCastle()
    {
        return registerBouncyCastle() ? new Kripto("BC") : null;
    }

    /**
     * Checks if the Bouncy Castle provider's main class is available on the
     * classpath.
     *
     * @return true if Bouncy Castle is present, false otherwise.
     */
    public static boolean isBouncyCastleAvailable()
    {
        try
        {
            // We're trying to load the Bouncy Castle provider's main class.
            // We don't need an instance, just verify that the class exists.
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            return true;
        }
        catch (ClassNotFoundException ex)
        {
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Enums /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    //https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyAgreement

    public enum SecretKeyAlgorithm
    {
        AES, ChaCha20
    }

    public enum SecretKeyTransformation
    {
        //Symetric Algorithms
        AES_GCM_NoPadding("AES/GCM/NoPadding", 128, 96, 128), //(128,192,256) iv=96   GOOD
        AES_CTR_NoPadding("AES/CTR/NoPadding", 128, 128, 128),//(128,192,256) iv=128  GOOD
        AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding", 128, 128, 0), //(128,192,256) iv=128  GOOD
        AES_CFB8_NoPadding("AES/CFB8/NoPadding", 128, 128, 0),  //(128)         iv=128
        ChaCha20_Poly1305("ChaCha20-Poly1305", 512, 96, 128);  //(256)         iv=96

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

    public enum KeyPairAlgorithm //KeyPair Algorithms, KeyFactory Algorithms
    {
        DiffieHellman, DSA, RSA, //mandatory DiffieHellman (1024), DSA (1024), RSA (1024, 2048)
        EC                      //optional   EC (192, 256)
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

    public enum KeyAgreementAlgorithm
    {
        DiffieHellman, ECDH, ECMQV
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

    public enum Hmac
    {
        @Deprecated
        HmacSHA224, 
        HmacSHA256, HmacSHA384, HmacSHA512
    }
    
    public enum Pbkdf2
    {
        PBKDF2WithHmacSHA256, PBKDF2WithHmacSHA512
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

    protected Kripto()
    {
        this(null, false);
    }

    protected Kripto(String providerName)
    {
        this(providerName, false);
    }

    protected Kripto(String providerName, boolean forceProvider)
    {
        this.providerName = providerName;
        this.forceProvider = forceProvider;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// PRIVATE METHODS //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////


    /**
     * Returns a {@link MessageDigest} instance for the specified algorithm.
     *
     * @param algorithm the message digest algorithm to use
     * @return a MessageDigest instance
     */
    private MessageDigest getMessageDigest(String algorithm)
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
                Logger.getLogger(Kripto.class.getName()).log(Level.SEVERE, null, ex2);
                throw new RuntimeException(ex2.getMessage(), ex2);
            }
        }
    }

    private Cipher getCipher(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
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

    private KeyGenerator getKeyGenerator(String algorithm, int keyBits) throws NoSuchAlgorithmException
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

    private KeyPairGenerator getKeyPairGenerator(String algorithm, int keyBits) throws NoSuchAlgorithmException
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

    protected SecretKeyFactory getSecretKeyFactory(String algoritm) throws NoSuchAlgorithmException
    {
        try
        {
            return this.providerName == null ? SecretKeyFactory.getInstance(algoritm) : SecretKeyFactory.getInstance(algoritm, this.providerName);
        }
        catch (NoSuchProviderException ex)
        {
            if (this.forceProvider)
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

    private KeyAgreement getKeyAgreement(String algorithm) throws NoSuchAlgorithmException, NoSuchPaddingException
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

    private Signature getSignature(String algorithm) throws NoSuchAlgorithmException
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
    private KeyStore getKeyStore(String type) throws KeyStoreException
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

    private Mac getMac(String algorithm, SecretKey key) throws NoSuchAlgorithmException
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
    ///// Message Diggest //////////////////////////////////////////////////////
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
            md.update(Byter.bytesUTF8(item));
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
        PublicKey foreignKey = keyFactory.generatePublic(foreignSpec);

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
    ///// Debug things /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Displays information about all registered security providers.
     *
     * @param out the PrintStream to output the information
     */
    public static void showProvidersInfo(PrintStream out)
    {
        String hr10 = Strings.repeat('-', 10);
        String hr40 = Strings.repeat('-', 40);

        for (Provider provider : Security.getProviders())
        {
            out.println(hr40);
            out.println(Strings.fill(hr10 + ' ' + provider.getName() + ' ', '-', 40));
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
        for (Provider.Service service : provider.getServices())
        {
            out.println("  " + service.toString());
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

    public PassphraserHkdf getPassphraserHkdf(HKDF hkdf, byte[] ikm, byte[] salt)
    {
        return new PassphraserHkdf(hkdf, ikm, salt);
    }

    public KeyStore getKeyStore(KeyStoreType type)
    {
        try
        {
            return getKeyStore(type.name());
        }
        catch (KeyStoreException ex)
        {
            throw new IllegalArgumentException(type.name()+": "+ex.getMessage(), ex);
        }
    }

    public KeyStore getKeyStorePKCS12()
    {
        return getKeyStore(KeyStoreType.PKCS12);
    }

    public KeyStoreManager getKeyStoreManager(KeyStoreType type)
    {
        return new KeyStoreManager(this.getKeyStore(type));
    }

    public KeyStoreManager getKeyStoreManager(KeyStoreType type, Passphraser passphraser)
    {
        return new KeyStoreManager(this.getKeyStore(type), passphraser);
    }

    public KeyStoreManager getKeyStoreManagerPKCS12()
    {
        return getKeyStoreManager(KeyStoreType.PKCS12);
    }

    public KeyStoreManager getKeyStoreManagerPKCS12(Passphraser passphraser)
    {
        return getKeyStoreManager(KeyStoreType.PKCS12, passphraser);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Derive data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public PBKDF2 getPBKDF2(Pbkdf2 derivation)
    {
        return new PBKDF2(this, derivation);
    }

    public HKDF getHKDF(Hkdf algorithm)
    {
        return new HKDFBC(algorithm);
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

    public byte[] ripemd160_digest_sha256_digest(byte[] input)
    {
        return ripemd160.digest(sha256.digest(input));
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Steganography ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public Steganography getSteganography(int columns, boolean splitLines, boolean mergeLines, boolean deflate)
    {
        return new Steganography(this, columns, splitLines, mergeLines, deflate);
    }

    //useful instances
    public final Digest sha256 = getDigest(MessageDigestAlgorithm.SHA256);
    public final Digest sha384 = getDigest(MessageDigestAlgorithm.SHA384);
    public final Digest sha512 = getDigest(MessageDigestAlgorithm.SHA512);
    public final Digest ripemd160 = getDigest(MessageDigestAlgorithm.RIPEMD160);

    public final PBKDF2 pbkdf2WithSha256 = getPBKDF2(Pbkdf2.PBKDF2WithHmacSHA256);
    public final PBKDF2 pbkdf2WithSha512 = getPBKDF2(Pbkdf2.PBKDF2WithHmacSHA512);

    public final HKDF hkdfWithSha256 = getHKDF(Hkdf.HkdfWithSha256);
    public final HKDF hkdfWithSha384 = getHKDF(Hkdf.HkdfWithSha384);
    public final HKDF hkdfWithSha512 = getHKDF(Hkdf.HkdfWithSha512);

    public final KeyGenerator keyGenAes256 = getKeyGenerator(SecretKeyAlgorithm.AES, 256);
    
    public boolean isAvailable(SecretKeyTransformation secretKeyTransformation) 
    {
        try
        {
            getCipher(secretKeyTransformation.transformation);
            return true;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex)
        {
            return false;
        }
    }
    public boolean isAvailable(SecretKeyAlgorithm secretKeyAlgorithm) 
    {
        try
        {
            getCipher(secretKeyAlgorithm.name());
            return true;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex)
        {
            return false;
        }
    }
    
    /**
     * Derives a mutual authentication proof to detect Man-in-the-Middle (MITM)
     * attacks in a peer-to-peer communication scenario.
     *
     * <p>
     * This method implements a secure protocol where two parties can verify
     * they are communicating directly without an intermediary attacker, using a
     * pre-shared secret. The protocol works by:
     * <ol>
     * <li>Ordering both fingerprints alphabetically (lexicographically)</li>
     * <li>Concatenating: firstFingerprint + secondFingerprint +
     * sharedSecret</li>
     * <li>Applying SHA-256 twice (double hashing for additional security)</li>
     * <li>Splitting the resulting hash into two equal halves</li>
     * <li>Determining which half to send and which to expect based on
     * fingerprint order</li>
     * </ol>
     *
     * <p>
     * Each party generates the same hash but sends different halves. The party
     * whose fingerprint comes first alphabetically sends the first half and
     * expects to receive the second half. The other party does the opposite.
     * This asymmetry prevents a MITM attacker from simply relaying the
     * messages, as they would need to know the shared secret to generate valid
     * fragments.
     *
     * <p>
     * <strong>Security properties:</strong>
     * <ul>
     * <li>Resistant to active MITM attacks when combined with a shared
     * secret</li>
     * <li>Does not require a trusted third party or PKI infrastructure</li>
     * <li>Suitable for decentralized P2P communications</li>
     * <li>The shared secret should have sufficient entropy (recommended: 6+
     * alphanumeric characters or 4+ diceware words, especially when used with
     * key derivation functions like Argon2)</li>
     * </ul>
     *
     * <p>
     * <strong>Usage example:</strong>
     * <pre>{@code
     * byte[] myFingerprint = getMyGpgFingerprint();
     * byte[] theirFingerprint = getTheirGpgFingerprint();
     * byte[] sharedSecret = "k7Qm2pX".getBytes(StandardCharsets.UTF_8);
     *
     * byte[][] proof = deriveMutualAuthProof(myFingerprint, theirFingerprint, sharedSecret, null);
     * byte[] fragmentToSend = proof[0];
     * byte[] fragmentToExpect = proof[1];
     *
     * // Send fragmentToSend to peer
     * sendToPeer(fragmentToSend);
     *
     * // Receive fragment from peer
     * byte[] receivedFragment = receiveFromPeer();
     *
     * // Verify
     * if (Arrays.equals(receivedFragment, fragmentToExpect)) {
     *     System.out.println("Authentication successful - No MITM detected");
     * } else {
     *     System.out.println("Authentication failed - Possible MITM attack!");
     * }
     * }</pre>
     *
     * @param ownFp the fingerprint of the local party's public key
     * (e.g., GPG key fingerprint)
     * @param otherFp the fingerprint of the remote party's public key
     * received during key exchange
     * @param sharedSecret a pre-shared secret known only to both legitimate
     * parties; should not be transmitted over the communication channel
     * @param strengthener an strengthener for your shared secret or null
     * @return a two-element array where:
     * <ul>
     * <li>index 0: the fragment to send to the other party</li>
     * <li>index 1: the fragment expected to receive from the other party</li>
     * </ul>
     * Each fragment is 16 bytes long (half of the SHA-256 hash output)
     * @throws IllegalArgumentException if both fingerprints are identical
     * @see MessageDigest
     */
    public byte[][] deriveMutualAuthProof(byte[] ownFp, byte[] otherFp, byte[] sharedSecret, UnaryOperator<byte[]> strengthener)
    {
        // Determine alphabetical order by comparing the fingerprints
        int cmp = Utils.compare(ownFp, otherFp);

        byte[] f1stFp;
        byte[] s2ndFp;
        boolean mineF1st;

        if (cmp < 0)
        {
            // My fingerprint goes first alphabetically
            f1stFp = ownFp;
            s2ndFp = otherFp;
            mineF1st = true;
        }
        else if (cmp > 0)
        {
            // Their fingerprint goes first alphabetically
            f1stFp = otherFp;
            s2ndFp = ownFp;
            mineF1st = false;
        }
        else
        {
            // Fingerprints are identical (should not happen in practice)
            throw new IllegalArgumentException("Fingerprints are identical");
        }
        // Apply strengthener if provided        
        sharedSecret = strengthener!=null ? strengthener.apply(sharedSecret) : sharedSecret;

        byte[] hash = sha256.digest(sha256.digest(f1stFp,s2ndFp,sharedSecret));

        // Split the hash into two halves
        int half = hash.length / 2;
        byte[] firstHalf = Arrays.copyOfRange(hash, 0, half);
        byte[] secondHalf = Arrays.copyOfRange(hash, half, hash.length);

        // Determine which half to send and which to receive
        byte[][] result = new byte[2][];

        if (mineF1st)
        {
            result[0] = firstHalf;   // Send the first half
            result[1] = secondHalf;  // Receive the second half
        }
        else
        {
            result[0] = secondHalf;  // Send the second half
            result[1] = firstHalf;   // Receive the first half
        }

        return result;
    }
    
    /**
     * Derives a mutual authentication proof without a strengthener function.
     *
     * <p>
     * This is a convenience method that calls
     * {@link #deriveMutualAuthProof(byte[], byte[], byte[], UnaryOperator)}
     * with a null strengthener parameter.
     *
     * @param ownFp the fingerprint of the local party's public key
     * @param otherFp the fingerprint of the remote party's public key
     * @param sharedSecret a pre-shared secret known only to both legitimate
     * parties
     * @return a two-element array containing the fragment to send (index 0) and
     * the fragment to expect (index 1)
     * @throws IllegalArgumentException if both fingerprints are identical
     * @see #deriveMutualAuthProof(byte[], byte[], byte[], UnaryOperator)
     */
    public byte[][] deriveMutualAuthProof(byte[] ownFp, byte[] otherFp, byte[] sharedSecret)
    {
        return deriveMutualAuthProof(ownFp, otherFp, sharedSecret, null);
    }    

    /**
     * Implements the Blum Blum Shub (BBS) cryptographically secure pseudorandom
     * number generator.
     *
     * <p>
     * The Blum Blum Shub algorithm is a pseudorandom number generator based on
     * the difficulty of integer factorization. It generates a sequence of
     * random bits by repeatedly squaring a seed value modulo the product of two
     * large primes.
     *
     * <p>
     * The algorithm works as follows:
     * <ol>
     * <li>Compute n = p × q (where p and q are primes)</li>
     * <li>Start with an initial seed value x₀</li>
     * <li>For each iteration i: x<sub>i+1</sub> = x<sub>i</sub>² mod n</li>
     * <li>Return the final value after the specified number of iterations</li>
     * </ol>
     *
     * <p>
     * <strong>Security requirements:</strong>
     * <ul>
     * <li>Both p and q must be large prime numbers</li>
     * <li>Both p and q must be congruent to 3 (mod 4), i.e., p ≡ 3 (mod 4) and
     * q ≡ 3 (mod 4)</li>
     * <li>The seed must be coprime to n (gcd(seed, n) = 1)</li>
     * <li>p and q should be kept secret for cryptographic applications</li>
     * </ul>
     *
     * <p>
     * <strong>Example usage:</strong>
     * <pre>{@code
     * BigInteger p = new BigInteger("499");      // Prime, 499 % 4 = 3
     * BigInteger q = new BigInteger("547");      // Prime, 547 % 4 = 3
     * BigInteger seed = new BigInteger("159");   // Initial seed
     * int iterations = 1000;
     *
     * BigInteger result = blumBlumShub(p, q, seed, iterations);
     * System.out.println("Random value: " + result);
     * }</pre>
     *
     * <p>
     * <strong>Performance note:</strong> This implementation uses direct
     * multiplication and modulo operations (x × x mod n) instead of
     * {@link BigInteger#modPow(BigInteger, BigInteger)} for better performance,
     * as we're always squaring (exponent = 2).
     *
     * @param p the first prime number, must satisfy p ≡ 3 (mod 4) and be prime
     * @param q the second prime number, must satisfy q ≡ 3 (mod 4) and be prime
     * @param seed the initial seed value for the generator, should be coprime
     * to p×q
     * @param iterations the number of squaring iterations to perform, must be
     * non-negative
     * @return the pseudorandom value after the specified number of iterations
     * @throws InvalidParameterException if p % 4 ≠ 3
     * @throws InvalidParameterException if q % 4 ≠ 3
     * @throws InvalidParameterException if p is not prime (tested with 128-bit
     * certainty)
     * @throws InvalidParameterException if q is not prime (tested with 128-bit
     * certainty)
     * @see <a href="https://en.wikipedia.org/wiki/Blum_Blum_Shub">Blum Blum
     * Shub on Wikipedia</a>
     */
    public static BigInteger blumBlumShub(BigInteger p, BigInteger q, BigInteger seed, int iterations)
    {
        BigInteger t3th = BigInteger.valueOf(3);
        BigInteger f4th = BigInteger.valueOf(4);
        //assert p % 4 == 3
        if(!p.mod(f4th).equals(t3th))
        {
            throw new InvalidParameterException("p % 4 != 3");
        }
        //assert q % 4 == 3
        if(!q.mod(f4th).equals(t3th))
        {
            throw new InvalidParameterException("q % 4 != 3");
        }
        if(!p.isProbablePrime(128))
        {
            throw new InvalidParameterException("p not prime");
        }
        if(!q.isProbablePrime(128))
        {
            throw new InvalidParameterException("q not prime");
        }

        BigInteger n = p.multiply(q);
            
        BigInteger current = seed;
        for (int i = 0; i < iterations; i++) 
        {
            //this is faster than modPow
            current = current.multiply(current).mod(n);// x² mod n
        }
        return current;        
    }
}
