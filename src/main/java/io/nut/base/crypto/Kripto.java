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

import io.nut.base.util.Byter;
import io.nut.base.util.Strings;
import java.io.PrintStream;
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
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author franci
 */
public class Kripto
{
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Static Values ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
   
    public static final String UTF8 = StandardCharsets.UTF_8.name();
    
    private static final String NOPADDING = "NoPadding";
    private static final String GCM = "GCM";
    
    public static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
    public static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;
    public static final int WRAP_MODE    = Cipher.WRAP_MODE;
    public static final int UNWRAP_MODE  = Cipher.UNWRAP_MODE;
    public static final int PUBLIC_KEY   = Cipher.PUBLIC_KEY;
    public static final int PRIVATE_KEY  = Cipher.PRIVATE_KEY;
    public static final int SECRET_KEY   = Cipher.SECRET_KEY;

    ////////////////////////////////////////////////////////////////////////////
    ///// GOOD PRACTICES ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public static final int SYMETRIC_SAFE_KEY_BITS = 128;
    public static final int SYMETRIC_HALF_KEY_BITS = 192;
    public static final int SYMETRIC_HIGH_KEY_BITS = 256;
    
    public static final SecretKeyAlgorithm AES = SecretKeyAlgorithm.AES;
    public static final KeyPairAlgorithm RSA = KeyPairAlgorithm.RSA;
        
    public static final SecretKeyTransformation AES_CBC_PKCS5PADDING = SecretKeyTransformation.AES_CBC_PKCS5Padding;

    // DO NO REPEAT IV, USE ALWAYS A RANDOM ONE
    public static final SecretKeyTransformation AES_GCM_NOPADDING = SecretKeyTransformation.AES_GCM_NoPadding;

    public static final KeyPairTransformation RSA_ECB_OAEPWITHSHA256ANDMGF1PADDING = KeyPairTransformation.RSA_ECB_OAEPWithSHA256AndMGF1Padding;

    public static final SignatureAlgorithm SHA256WITHECDSA = SignatureAlgorithm.SHA256withECDSA;

    public static final MessageDigestAlgorithm SHA256 = MessageDigestAlgorithm.SHA256;
    
    public static final SecretKeyDerivation PBKDF2WITHHMACSHA256 = SecretKeyDerivation.PBKDF2WithHmacSHA256;

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
    
    public static Kripto getInstance()
    {
        return new Kripto();
    }
    static Kripto getInstance(String providerName)
    {
        return new Kripto(providerName);
    }
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
    
    public static Kripto getInstanceBouncyCastle()
    {
        return registerBouncyCastle() ? new Kripto("BC") : null;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Enums /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    //https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyAgreement

    public enum SecretKeyTransformation
    {
        //Symetric Algorithms
        AES_CBC_NoPadding("AES/CBC/NoPadding",              128, 128),  //(128,192,256) iv=128
        AES_GCM_NoPadding("AES/GCM/NoPadding",              128, 96),   //(128,192,256) iv=96   GOOD
        AES_CBC_PKCS5Padding("AES/CBC/PKCS5Padding",        128, 128),  //(128,192,256) iv=128  GOOD
        AES_ECB_NoPadding("AES/ECB/NoPadding",              128, 0),    //(128)         iv=0
        AES_ECB_PKCS5Padding("AES/ECB/PKCS5Padding",        128, 0),    //(128)         iv=0
        AES_CFB8_NoPadding("AES/CFB8/NoPadding",            128, 128),  //(128)         iv=128

        DES_CBC_NoPadding("DES/CBC/NoPadding",              64, 64),    //(56)          iv=64
        DES_CBC_PKCS5Padding("DES/CBC/PKCS5Padding",        64, 64),    //(56)          iv=64
        DES_ECB_NoPadding("DES/ECB/NoPadding",              64, 0),     //(56)          iv=0
        DES_ECB_PKCS5Padding("DES/ECB/PKCS5Padding",        64, 0),     //(56)          iv=0

        DESede_CBC_NoPadding("DESede/CBC/NoPadding",        64, 64),    //(168)         iv=64
        DesEde_Cbc_Pkcs5Padding("DESede/CBC/PKCS5Padding",  64, 64),    //(128)         iv=64
        DESede_ECB_NoPadding("DESede/ECB/NoPadding",        64, 0),     //(128)         iv=0
        DESede_ECB_PKCS5Padding("DESede/ECB/PKCS5Padding",  64, 0);     //(128)         iv=0
        
        public final String transformation;
        public final String algorithm;
        public final String mode;
        public final String padding;
        public final boolean nopadding;
        public final boolean gcm;
        
        public final int blockBits;
        public final int ivBits;
        
        SecretKeyTransformation(String transformation, int blockBits, int ivBits)
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
        }
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm);
        }
    }
    public enum KeyPairTransformation
    {
        RSA_ECB_PKCS1Padding("RSA/ECB/PKCS1Padding"),                                  //(1024,2048)
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
        public int getMaxAllowedKeyLength() throws NoSuchAlgorithmException
        {
            return Cipher.getMaxAllowedKeyLength(algorithm);
        }
    }
    public enum SecretKeyDerivation
    {
        PBKDF2WithHmacSHA256                                                    //GOOD
    }
    public enum SecretKeyAlgorithm
    {
        AES, DES, DESede
        //, HmacSHA1, HmacSHA256
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
        MD2("MD2"), MD5("MD5"), SHA1("SHA1"), SHA224("SHA-224"), 
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
        NONEwithRSA, MD2withRSA, MD5withRSA, SHA1withRSA, SHA224withRSA, 
        SHA256withRSA,                                                          //GOOD 
        SHA384withRSA, SHA512withRSA,
        NONEwithDSA, SHA1withDSA, SHA224withDSA, SHA256withDSA, 
        NONEwithECDSA, SHA1withECDSA, SHA224withECDSA, 
        SHA256withECDSA,                                                        //GOOD
        SHA384withECDSA, SHA512withECDSA
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
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Message Diggest //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public MessageDigest getMessageDigest(MessageDigestAlgorithm algorithm) throws NoSuchAlgorithmException
    {
        return getMessageDigest(algorithm.code);
    }
    public MessageDigest md5() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.MD5);
    }
    public MessageDigest sha1() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA1);
    }
    public MessageDigest sha224() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA224);
    }
    public MessageDigest sha256() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA256);
    }
    public MessageDigest sha384() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA384);
    }
    public MessageDigest sha512() throws NoSuchAlgorithmException
    {
        return this.getMessageDigest(MessageDigestAlgorithm.SHA512);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Keys /////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    public SecretKey getSecretKey(byte[] secretKey, SecretKeyAlgorithm algoritm)
    {
        return new SecretKeySpec(secretKey, algoritm.name());
    }

    public KeyGenerator getKeyGenerator(SecretKeyAlgorithm algorithm, int keyBits) throws NoSuchAlgorithmException 
    {
        return getKeyGenerator(algorithm.name(), keyBits);
    }
    
    public KeyPairGenerator getKeyPairGenerator(KeyPairAlgorithm algorithm, int keyBits) throws NoSuchAlgorithmException 
    {
        return getKeyPairGenerator(algorithm.name(), keyBits);
    }    
    
    public static SecretKey resizeSecretKey(SecretKey sk, int keyBits)
    {
        int keyBytes = keyBits/8;
        if(keyBytes>0)
        {
            byte[] key = sk.getEncoded();
            sk = keyBytes<key.length ? new SecretKeySpec(key, 0, keyBytes, sk.getAlgorithm()) : sk;  
        }
        return sk;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// IV ///////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public IvParameterSpec getIv(byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        return new IvParameterSpec(iv);
    }
    public IvParameterSpec getIv(byte[] iv, int bits) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        return new IvParameterSpec(iv, 0, bits/8);
    }
    
    public GCMParameterSpec getIvGCM(byte[] iv, int bits) throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        return new GCMParameterSpec(bits, iv);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Salt facilities ////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public static String normalizeNFKD(CharSequence cs)
    {
        return Normalizer.normalize(cs, Normalizer.Form.NFKD);
    }

    public byte[] deriveBytesSHA256(CharSequence src) throws NoSuchAlgorithmException
    {
        MessageDigest sha256 = this.sha256();
        sha256.update(normalizeNFKD(src).getBytes(StandardCharsets.UTF_8));
        return sha256.digest();
    }

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
    
    public SecretKey deriveSecretKey(char[] passphrase, byte[] salt, int rounds, int keyBits, SecretKeyDerivation derivation, SecretKeyAlgorithm secretKeyAlgorithm) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory factory = this.getSecretKeyFactory(derivation.name());
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, rounds, keyBits);
        SecretKey genericSecretKey = factory.generateSecret(spec);
        return this.getSecretKey(genericSecretKey.getEncoded(), secretKeyAlgorithm);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// KeyAgreement Algorithms ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    //use the pair (EC,ECDH) or (DiffieHellman,DiffieHellman)
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

    public Cipher getCipher(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = getCipher(transformation.transformation);
        cipher.init(opmode, secretKey, iv);
        return cipher;        
    }
    
    public byte[] encrypt(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(secretKey, transformation, iv, ENCRYPT_MODE).doFinal(data);        
    }
    
    public byte[] decrypt(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(secretKey, transformation, iv, DECRYPT_MODE).doFinal(data);        
    }
    
    public byte[] wrap(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(secretKey, transformation, iv, WRAP_MODE).wrap(key);        
    }
    
    public SecretKey unwrap(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] key, SecretKeyAlgorithm secretKeyAlgorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return (SecretKey) getCipher(secretKey, transformation, iv, UNWRAP_MODE).unwrap(key, secretKeyAlgorithm.name(), SECRET_KEY);        
    }
    public PrivateKey unwrap(SecretKey secretKey, SecretKeyTransformation transformation, AlgorithmParameterSpec iv, byte[] key, KeyPairAlgorithm keyPairAlgorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return (PrivateKey) getCipher(secretKey, transformation, iv, UNWRAP_MODE).unwrap(key, keyPairAlgorithm.name(), PRIVATE_KEY);        
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// KeyPair Ciphers //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public Cipher getCipher(PublicKey pubKey, KeyPairTransformation transformation, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = getCipher(transformation.transformation);
        cipher.init(opmode, pubKey);
        return cipher;        
    }
    
    public Cipher getCipher(PrivateKey prvKey, KeyPairTransformation transformation, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        Cipher cipher = getCipher(transformation.transformation);
        cipher.init(opmode, prvKey);
        return cipher;        
    }
    
    public byte[] encrypt(PublicKey pubKey, KeyPairTransformation transformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(pubKey, transformation, ENCRYPT_MODE).doFinal(data);
    }
    
    public byte[] decrypt(PrivateKey prvKey, KeyPairTransformation transformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(prvKey, transformation, DECRYPT_MODE).doFinal(data);
    }
    
    // DO NOT IMPLEMENT wrap and unwrap for PublicKey or PrivateKey because it will fail, RSA will not allow such a big key as data

    public byte[] wrap(PublicKey pubKey, KeyPairTransformation transformation, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return getCipher(pubKey, transformation, WRAP_MODE).wrap(key);
    }
    
    public SecretKey unwrap(PrivateKey prvKey, KeyPairTransformation transformation, byte[] key, SecretKeyAlgorithm secretKeyAlgorithm) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        return (SecretKey) getCipher(prvKey, transformation, UNWRAP_MODE).unwrap(key, secretKeyAlgorithm.name(), SECRET_KEY);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    ///// Signatures ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
        
    public Signature getSignature(SignatureAlgorithm algorithm) throws NoSuchAlgorithmException
    {
        return this.getSignature(algorithm.name());
    }
    
    public byte[] sign(SignatureAlgorithm algorithm, PrivateKey privateKey, byte[]... data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException
    {
        Signature signature = this.getSignature(algorithm);
        signature.initSign(privateKey);
        for(int i=0;i<data.length;i++)
        {
            signature.update(data[i]);
        }
        return signature.sign();
    }
    public boolean verify(SignatureAlgorithm algorithm, PublicKey publicKey, byte[] sign, byte[]... data) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException
    {
        Signature signature = this.getSignature(algorithm);
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
        for (Provider.Service service: provider.getServices())
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

    ////////////////////////////////////////////////////////////////////////////
    ///// Shared Secrets n of m share the secret key ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    public static ShamirSharedSecret getShamirSharedSecret(int n, int k)
    {
        return new ShamirSharedSecret(n, k);
    }
    
}
