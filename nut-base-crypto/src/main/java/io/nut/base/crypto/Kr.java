/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.encoding.Base64;
import io.nut.base.encoding.Base64DecoderException;
import io.nut.base.encoding.Hex;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptography facade with a single, safe by design API, modelled after the
 * {@code std.crypto} standard library of the Zig language.
 * <p>
 * {@code Kr} is abstract and implements everything that can be shared by a
 * Java Cryptography Architecture (JCA/JCE) backend: parameter validation,
 * composite constructions such as HKDF or PBKDF2, AEAD envelopes, key
 * encoding and every algorithm name. Subclasses only bind those operations to
 * a concrete backend:
 * <ul>
 * <li>{@link KrJdk} uses the providers of the running JDK. This is the
 * default and the only mandatory dependency.</li>
 * <li>{@link KrBC} uses the Bouncy Castle provider, which adds the
 * algorithms that the JDK does not implement (on Java 8 for instance).</li>
 * </ul>
 * Instances are obtained with {@link #getInstance(boolean)}:
 * <pre>{@code
 * Kr kr = Kr.getInstance(true);   // Bouncy Castle when available, the JDK otherwise
 * byte[] hash = kr.hash.sha256("the quick brown fox");
 * }</pre>
 * The same code therefore runs unchanged on Java 8 up to Java 26, with and
 * without Bouncy Castle on the classpath. Anything the backend cannot do
 * fails with {@link UnsupportedAlgorithmException}, and
 * {@link #supports(HashAlgorithm)} and friends tell in advance what a backend
 * is able to do.
 * <p>
 * <b>Safety rules baked into this API:</b>
 * <ul>
 * <li>Only algorithms considered secure are exposed. There is no MD5, no
 * SHA-1, no DES, no ECB mode, no RSA with PKCS#1 v1.5 encryption.</li>
 * <li>Authenticated encryption always generates a fresh random nonce and
 * prepends it to the result, so it is impossible to reuse a nonce by
 * accident.</li>
 * <li>Key derivation takes the key size from the target algorithm instead of
 * from the caller, so keys cannot come out too short.</li>
 * <li>PBKDF2 refuses weak parameters, AEAD failures are reported as
 * {@link BadTagException} and tag or digest comparisons are constant
 * time.</li>
 * <li>Ciphertext, signature and authentication tag checks are never
 * skipped: there is no method that decrypts without verifying.</li>
 * </ul>
 * <b>Thread safety:</b> instances are immutable, hold no per operation state
 * and every call creates its own JCA object, so a single instance can be
 * shared by all the threads of an application. {@link SecureRandom} is
 * thread safe by contract.
 *
 * @author franci
 */
public abstract class Kr
{
    /**
     * Creates an instance. Backends call it, applications get one
     * with {@link #getInstance()} or {@link #getInstance(boolean)}.
     */
    protected Kr()
    {
    }

    /**
     * Name of the Bouncy Castle JCA provider.
     */
    public static final String BOUNCY_CASTLE_PROVIDER = "BC";

    /**
     * Name of the Bouncy Castle provider class. It is referenced by name so
     * that {@link Kr} can detect Bouncy Castle without loading {@link KrBC}.
     */
    static final String BOUNCY_CASTLE_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    /**
     * The version of the library inside the description of the Bouncy Castle
     * provider, which reads "Bouncy Castle Security Provider v1.84".
     */
    private static final Pattern BOUNCY_CASTLE_VERSION = Pattern.compile("v(\\d+(?:\\.\\d+)+)");

    /**
     * Splits the modular crypt encoded form of a password hash, whose fields
     * are separated by a dollar that has to be written as a regular expression
     * escape, which a constant keeps out of the code.
     */
    private static final String DOLLAR = "\\$";

    /**
     * Minimum RSA modulus size in bits. 2048 bits is the smallest size that is
     * still considered breakable-free, 3072 bits is the recommended default.
     */
    public static final int MINIMUM_RSA_BITS = 2048;

    /**
     * Length in bytes of the raw encoding of an Ed25519 or X25519 key, as
     * defined by RFC 7748 and RFC 8410.
     */
    private static final int RAW_KEY_BYTES = 32;

    /**
     * Digests: {@link HashAlgorithm}.
     */
    public final Hash hash = new Hash();

    /**
     * Message authentication codes: {@link HmacAlgorithm}.
     */
    public final Hmac hmac = new Hmac();

    /**
     * The Poly1305 message authentication code of RFC 8439, which is not a
     * HMAC and has no algorithm to choose.
     */
    public final Poly1305 poly1305 = new Poly1305();

    /**
     * Authenticated encryption: {@link AeadAlgorithm}.
     */
    public final Aeads aeads = new Aeads();

    /**
     * Key derivation: HKDF, PBKDF2 and ECDH.
     */
    public final Kdf kdf = new Kdf();

    /**
     * Stream ciphers: {@link StreamAlgorithm}.
     */
    public final Streams streams = new Streams();

    /**
     * Key wrapping: {@link KeyWrapAlgorithm}.
     */
    public final Wraps wraps = new Wraps();

    /**
     * Block ciphers: {@link CipherAlgorithm}.
     */
    public final Ciphers ciphers = new Ciphers();

    /**
     * Digital signatures: {@link SignatureAlgorithm}.
     */
    public final Sign sign = new Sign();

    /**
     * Key generation, encoding and decoding: {@link KeyAlgorithm} and
     * {@link SecretKeyAlgorithm}.
     */
    public final Keys keys = new Keys();

    /**
     * Cryptographically strong random data.
     */
    public final Random random = new Random();

    /**
     * Small helpers that complete the primitives, like constant time
     * comparison or wiping secrets.
     */
    public final Utils utils = new Utils();

    /**
     * Password hashing: {@link PasswordAlgorithm}.
     */
    public final Password password = new Password();

    private final Map<String, Boolean> capabilities = new ConcurrentHashMap<String, Boolean>();

    ////////////////////////////////////////////////////////////////////////////
    ///// Instances ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the shared instance that uses the providers of the running JDK.
     *
     * @return a {@link KrJdk} instance, never null
     */
    public static Kr getInstance()
    {
        return KrJdk.instance();
    }

    /**
     * Returns the shared instance that best fits the requested backend.
     *
     * @param bouncyCastle true to use Bouncy Castle, false to use the JDK
     * @return a {@link KrBC} instance when Bouncy Castle is available and
     * bouncyCastle is true, a {@link KrJdk} instance otherwise, never null
     */
    public static Kr getInstance(boolean bouncyCastle)
    {
        return bouncyCastle && isBouncyCastleAvailable() ? bouncyCastle() : getInstance();
    }

    private static Kr bouncyCastle()
    {
        return KrBC.instance();
    }

    /**
     * Returns whether Bouncy Castle is present on the classpath.
     * <p>
     * The provider class is only looked up, never loaded nor registered, so
     * calling this method has no side effect.
     *
     * @return true if Bouncy Castle can be used, false otherwise
     */
    public static boolean isBouncyCastleAvailable()
    {
        return bouncyCastleProviderClass() != null;
    }

    /**
     * Returns whether Bouncy Castle is present on the classpath of the given
     * class loader, and only there.
     * <p>
     * Unlike {@link #isBouncyCastleAvailable()}, no other class loader is
     * asked, because the point of this method is to find out what a single
     * class loader finds, like the one of a plugin or the one an application
     * server sets for a deployment, which is what decides whether Bouncy
     * Castle can be made an optional dependency there.
     *
     * @param classLoader the class loader to look in, null always gives false
     * @return true if Bouncy Castle can be used from that class loader, false
     * otherwise
     */
    public static boolean isBouncyCastleAvailable(ClassLoader classLoader)
    {
        if (classLoader == null)
        {
            return false;
        }
        try
        {
            return Class.forName(BOUNCY_CASTLE_PROVIDER_CLASS, false, classLoader) != null;
        }
        catch (ClassNotFoundException | LinkageError ex)
        {
            return false;
        }
    }

    /**
     * Returns the version of the Bouncy Castle implementation found on the
     * classpath.
     *
     * @return the version, or null when Bouncy Castle is not available or does
     * not report it
     */
    @SuppressWarnings("deprecation")
    public static String bouncyCastleVersion()
    {
        if (!isBouncyCastleAvailable())
        {
            return null;
        }
        Provider provider = bouncyCastleProvider();
        if (provider == null)
        {
            return null;
        }
        //Bouncy Castle names itself "Bouncy Castle Security Provider v1.84",
        //which is where the version of the library is, its jar does not always
        //have an implementation version in the manifest
        Matcher matcher = BOUNCY_CASTLE_VERSION.matcher(provider.getInfo() == null ? "" : provider.getInfo());
        if (matcher.find())
        {
            return matcher.group(1);
        }
        //the manifest of the jar, when the build that packaged it was kind
        Package pack = provider.getClass().getPackage();
        String version = pack == null ? null : pack.getImplementationVersion();
        if (version != null)
        {
            return version;
        }
        //and this is the last resort, where the provider reports the version
        //of the JCA interface it was built for, which is not the version of the
        //library: Provider#getVersion returns a double on Java 8, while the
        //nicer Provider#getVersionStr only exists since Java 9 and this module
        //has to run on Java 8
        return provider.getVersion() > 0 ? String.valueOf(provider.getVersion()) : null;
    }

    private static Provider bouncyCastleProvider()
    {
        Provider provider = Security.getProvider(BOUNCY_CASTLE_PROVIDER);
        if (provider != null)
        {
            return provider;
        }
        //not registered yet, but the version is worth reporting anyway, so the
        //provider is created without being added to the application
        Class<?> providerClass = bouncyCastleProviderClass();
        if (providerClass == null)
        {
            return null;
        }
        try
        {
            return (Provider) providerClass.getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException | RuntimeException ex)
        {
            return null;
        }
    }

    private static Class<?> bouncyCastleProviderClass()
    {
        //the context class loader comes first, Bouncy Castle is frequently loaded
        //by a container while this class comes from a shared library
        ClassLoader[] loaders = new ClassLoader[]
        {
            Thread.currentThread().getContextClassLoader(),
            Kr.class.getClassLoader()
        };
        for (ClassLoader loader : loaders)
        {
            if (loader == null)
            {
                continue;
            }
            try
            {
                return Class.forName(BOUNCY_CASTLE_PROVIDER_CLASS, false, loader);
            }
            catch (ClassNotFoundException | LinkageError ex)
            {
                //try the next class loader
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Identity ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the short name of this backend.
     *
     * @return the backend name, for example "jdk" or "BC"
     */
    public abstract String name();

    /**
     * Returns the JCA provider this backend is pinned to.
     *
     * @return the provider, or null when the default provider order is used
     */
    public Provider provider()
    {
        return null;
    }

    /**
     * Returns the name of the JCA provider this backend is pinned to.
     *
     * @return the provider name, or null when the default provider order is
     * used
     */
    public String providerName()
    {
        Provider provider = provider();
        return provider == null ? null : provider.getName();
    }

    @Override
    public String toString()
    {
        return name();
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Capabilities ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns whether this backend can compute this kind of hash.
     * <p>
     * SHA-3 needs Java 9 or later and BLAKE2 is only available with Bouncy
     * Castle, so this is the method to ask before relying on a given digest.
     *
     * @param algorithm the hash algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(HashAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("MessageDigest." + algorithm, () -> messageDigest(algorithm.jcaName()));
    }

    /**
     * Returns whether this backend can compute this kind of HMAC.
     *
     * @param algorithm the hmac algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(HmacAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("Mac." + algorithm, () -> mac(algorithm.jcaName()));
    }

    /**
     * Returns whether this backend can do authenticated encryption with this
     * algorithm.
     *
     * @param algorithm the aead algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(AeadAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("Cipher." + algorithm, () -> cipher(algorithm.transformation()));
    }

    /**
     * Returns whether this backend can do this block cipher.
     *
     * @param algorithm the cipher algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(CipherAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("Cipher." + algorithm, () -> cipher(algorithm.transformation()));
    }

    /**
     * Returns whether this backend can do this key wrap.
     *
     * @param algorithm the key wrap algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(KeyWrapAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        //RSA key wrap is arithmetic on the key, so no provider is needed
        return algorithm.usesRsaKey() || supports("Cipher." + algorithm, () -> cipher(algorithm.transformation()));
    }

    /**
     * Returns whether this backend can produce this kind of signature.
     *
     * @param algorithm the signature algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(SignatureAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("Signature." + algorithm, () -> signature(algorithm.jcaName()));
    }

    /**
     * Returns whether this backend can generate keys of this kind.
     *
     * @param algorithm the key algorithm, not null
     * @return true when the algorithm can be used, false otherwise
     */
    public boolean supports(KeyAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("KeyPairGenerator." + algorithm, () -> keyPairGenerator(algorithm.jcaName()));
    }

    /**
     * Returns whether this backend can generate keys on this elliptic curve.
     * <p>
     * The check is definitive: a throwaway key pair is generated the first
     * time and the result is cached.
     *
     * @param curve the curve, not null
     * @return true when the curve can be used, false otherwise
     */
    public boolean supports(EcCurve curve)
    {
        require(curve, "curve");
        return supports("Curve." + curve, () -> keys.generateEc(curve));
    }

    /**
     * Returns whether the backend has this Argon2 variant.
     *
     * @param algorithm the variant, not null
     * @return true when passwords can be hashed with it
     */
    public boolean supports(Argon2Algorithm algorithm)
    {
        require(algorithm, "algorithm");
        for (PasswordAlgorithm candidate : PasswordAlgorithm.values())
        {
            if (candidate.argon2Algorithm() == algorithm)
            {
                return supports(candidate);
            }
        }
        return false;
    }

    /**
     * Returns whether password hashing with the given algorithm is available.
     * <p>
     * None of these algorithms is part of the JDK, so this is false for
     * {@link KrJdk} and true for {@link KrBC}.
     *
     * @param algorithm the password algorithm, not null
     * @return true when passwords can be hashed with it, false otherwise
     */
    public boolean supports(PasswordAlgorithm algorithm)
    {
        require(algorithm, "algorithm");
        return supports("Password." + algorithm, () -> derive(algorithm));
    }

    private byte[] derive(PasswordAlgorithm algorithm)
    {
        byte[] password = utf8(CharBuffer.wrap("password"));
        byte[] salt = random.bytes(16);
        switch (algorithm)
        {
            case ARGON2D:
            case ARGON2I:
            case ARGON2ID:
                return deriveArgon2(algorithm.argon2Algorithm(), password, salt, 1024, 1, 1, 32);
            case SCRYPT:
                return deriveScrypt(password, salt, 16, 8, 1, 32);
            case BCRYPT:
                return utf8(CharBuffer.wrap(deriveBcrypt("password".toCharArray(), Password.MINIMUM_BCRYPT_COST)));
            default:
                throw new UnsupportedAlgorithmException("unknown password algorithm " + algorithm);
        }
    }

    private boolean supports(String capability, Probe probe)
    {
        Boolean supported = capabilities.get(capability);
        if (supported == null)
        {
            boolean available;
            try
            {
                probe.probe();
                available = true;
            }
            catch (UnsupportedAlgorithmException ex)
            {
                available = false;
            }
            capabilities.put(capability, available);
            return available;
        }
        return supported;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Backend hooks, implemented by KrJdk and KrBC /////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a new message digest of the given JCA algorithm.
     *
     * @param jcaName the JCA algorithm name
     * @return a new message digest, never null
     * @throws UnsupportedAlgorithmException when the algorithm is not
     * available
     */
    protected abstract MessageDigest messageDigest(String jcaName);

    /**
     * Returns a new, not yet initialized, message authentication code of the
     * given JCA algorithm.
     *
     * @param jcaName the JCA algorithm name
     * @return a new Mac, never null
     * @throws UnsupportedAlgorithmException when the algorithm is not
     * available
     */
    protected abstract Mac mac(String jcaName);

    /**
     * Returns a new, not yet initialized, cipher of the given JCA
     * transformation.
     *
     * @param transformation the JCA transformation
     * @return a new cipher, never null
     * @throws UnsupportedAlgorithmException when the transformation is not
     * available
     */
    protected abstract Cipher cipher(String transformation);

    /**
     * Returns a new, not yet initialized, key pair generator of the given JCA
     * algorithm.
     *
     * @param jcaName the JCA algorithm name
     * @return a new key pair generator, never null
     * @throws UnsupportedAlgorithmException when the algorithm is not
     * available
     */
    protected abstract KeyPairGenerator keyPairGenerator(String jcaName);

    /**
     * Returns a new key factory of the given JCA algorithm, able to encode and
     * decode keys in X.509 and PKCS#8 format.
     *
     * @param jcaName the JCA algorithm name
     * @return a new key factory, never null
     * @throws UnsupportedAlgorithmException when the algorithm is not
     * available
     */
    protected abstract KeyFactory keyFactory(String jcaName);

    /**
     * Returns a new, not yet initialized, signature of the given JCA
     * algorithm.
     *
     * @param jcaName the JCA algorithm name
     * @return a new signature, never null
     * @throws UnsupportedAlgorithmException when the algorithm is not
     * available
     */
    protected abstract Signature signature(String jcaName);

    /**
     * Returns a new, not yet initialized, key agreement of the given JCA
     * algorithm.
     *
     * @param jcaName the JCA algorithm name
     * @return a new key agreement, never null
     * @throws UnsupportedAlgorithmException when the algorithm is not
     * available
     */
    protected abstract KeyAgreement keyAgreement(String jcaName);

    /**
     * Returns a cryptographically strong random generator, ready to be used.
     *
     * @return a secure random generator, never null
     */
    protected abstract SecureRandom secureRandom();

    /**
     * Returns the domain parameters of an elliptic curve, as the backend
     * understands them.
     *
     * @param curve the curve, not null
     * @return the parameters to initialize a key pair generator with
     * @throws UnsupportedAlgorithmException when the curve is not available
     */
    protected abstract AlgorithmParameterSpec ecParameters(EcCurve curve);

    /**
     * Derives the bytes of an Argon2 hash with the given parameters, which the
     * backends that have it implement.
     *
     * @param algorithm the Argon2 variant, not null
     * @param password the password bytes, not null nor empty
     * @param salt the salt, not null
     * @param memoryKb how many kibibytes of memory to use
     * @param iterations how many passes over that memory
     * @param parallelism how many lanes
     * @param outputBytes how many bytes to return
     * @return the hash bytes
     * @throws UnsupportedAlgorithmException when the backend has no Argon2
     */
    protected abstract byte[] deriveArgon2(Argon2Algorithm algorithm, byte[] password, byte[] salt,
            int memoryKb, int iterations, int parallelism, int outputBytes);

    /**
     * Derives the bytes of a scrypt hash with the given parameters, which the
     * backends that have it implement.
     *
     * @param password the password bytes, not null nor empty
     * @param salt the salt, not null
     * @param cost the CPU and memory cost N, a power of two
     * @param blockSize the block size r
     * @param parallelism the parallelization factor p
     * @param outputBytes how many bytes to return
     * @return the hash bytes
     * @throws UnsupportedAlgorithmException when the backend has no scrypt
     */
    protected abstract byte[] deriveScrypt(byte[] password, byte[] salt, int cost, int blockSize,
            int parallelism, int outputBytes);

    /**
     * Hashes a password with bcrypt and returns the modular crypt encoded form
     * that crypt(3) and every implementation in the wild understand.
     *
     * @param password the password, not null nor empty
     * @param cost the cost factor, from 4 to 31
     * @return the encoded hash, like "$2a$10$..."
     * @throws UnsupportedAlgorithmException when the backend has no bcrypt
     */
    protected abstract String deriveBcrypt(char[] password, int cost);

    /**
     * Returns whether a password produces a bcrypt hash equal to the given
     * encoded one, comparing the bytes in constant time.
     *
     * @param encoded the encoded hash, not null
     * @param password the password, not null nor empty
     * @return true when the password is the one that was hashed
     * @throws UnsupportedAlgorithmException when the backend has no bcrypt
     */
    protected abstract boolean verifyBcryptHash(String encoded, char[] password);

    /**
     * Builds the exception used when an algorithm is missing, so subclasses
     * can add advice about how to get it.
     *
     * @param algorithm the JCA algorithm or transformation name
     * @return a new exception, ready to be thrown
     */
    protected final UnsupportedAlgorithmException unsupported(String algorithm)
    {
        return unsupported(algorithm, null);
    }

    /**
     * Builds the exception used when an algorithm is missing, as
     * {@link #unsupported(String)} with the cause of the failure.
     *
     * @param algorithm the JCA algorithm or transformation name
     * @param cause the cause of the failure, may be null
     * @return a new exception, ready to be thrown
     */
    protected final UnsupportedAlgorithmException unsupported(String algorithm, Throwable cause)
    {
        String advice = unavailableAdvice();
        return new UnsupportedAlgorithmException(algorithm + " is not available in the " + name()
                + " backend" + (advice.isEmpty() ? "" : ". " + advice), cause);
    }

    /**
     * Returns a hint appended to {@link UnsupportedAlgorithmException}
     * messages, telling how this algorithm could become available.
     *
     * @return the advice, empty when there is nothing to suggest
     */
    protected String unavailableAdvice()
    {
        return "";
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Algorithms //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Hash algorithms, all of them collision resistant and free of practical
     * attacks. Weak hashes such as MD5 or SHA-1 are deliberately absent.
     */
    public enum HashAlgorithm
    {
        /** SHA-224, a 28 bytes digest. */
        SHA224("SHA-224", 28),
        /** SHA-256, the usual default. */
        SHA256("SHA-256", 32),
        /** SHA-384, a 48 bytes digest. */
        SHA384("SHA-384", 48),
        /** SHA-512, a 64 bytes digest. */
        SHA512("SHA-512", 64),
        /** SHA3-224, the SHA-224 size of the SHA-3 family. */
        SHA3_224("SHA3-224", 28),
        /** SHA3-256, the SHA-256 size of the SHA-3 family. */
        SHA3_256("SHA3-256", 32),
        /** SHA3-384, the SHA-384 size of the SHA-3 family. */
        SHA3_384("SHA3-384", 48),
        /** SHA3-512, the SHA-512 size of the SHA-3 family. */
        SHA3_512("SHA3-512", 64),
        /** BLAKE2b with a 64 bytes digest, only available with Bouncy Castle. */
        BLAKE2B_512("BLAKE2B-512", 64),
        /** BLAKE2s with a 32 bytes digest, only available with Bouncy Castle. */
        BLAKE2S_256("BLAKE2S-256", 32);

        private final String jcaName;
        private final int digestBytes;

        HashAlgorithm(String jcaName, int digestBytes)
        {
            this.jcaName = jcaName;
            this.digestBytes = digestBytes;
        }

        /**
         * Returns the JCA algorithm name.
         *
         * @return the JCA name
         */
        public String jcaName()
        {
            return jcaName;
        }

        /**
         * Returns the size of the digest in bytes.
         *
         * @return the digest size
         */
        public int digestBytes()
        {
            return digestBytes;
        }
    }

    /**
     * Keyed hashing algorithms. HMAC-SHA256 is the usual default, larger
     * outputs are only needed when there is a reason for them.
     * <p>
     * The JCA names are the ones of the JDK provider instead of the usual
     * "HMAC-SHA256" spelling, which some providers only accept as an alias.
     */
    public enum HmacAlgorithm
    {
        /** HMAC-SHA256, the usual default. */
        SHA256("HmacSHA256", 32, HashAlgorithm.SHA256),
        /** HMAC-SHA384. */
        SHA384("HmacSHA384", 48, HashAlgorithm.SHA384),
        /** HMAC-SHA512. */
        SHA512("HmacSHA512", 64, HashAlgorithm.SHA512);

        private final String jcaName;
        private final int digestBytes;
        private final HashAlgorithm hashAlgorithm;

        HmacAlgorithm(String jcaName, int digestBytes, HashAlgorithm hashAlgorithm)
        {
            this.jcaName = jcaName;
            this.digestBytes = digestBytes;
            this.hashAlgorithm = hashAlgorithm;
        }

        /**
         * Returns the JCA algorithm name.
         *
         * @return the JCA name
         */
        public String jcaName()
        {
            return jcaName;
        }

        /**
         * Returns the size of the authentication tag in bytes.
         *
         * @return the tag size
         */
        public int digestBytes()
        {
            return digestBytes;
        }

        /**
         * Returns the equivalent unkeyed hash algorithm.
         *
         * @return the hash algorithm
         */
        public HashAlgorithm hashAlgorithm()
        {
            return hashAlgorithm;
        }
    }

    /**
     * Authenticated encryption algorithms, all of them with a 96 bits nonce
     * and a 128 bits tag, the sizes recommended by their respective standards
     * and the ones with the best support and speed.
     */
    public enum AeadAlgorithm
    {
        /** AES with a 128 bits key in GCM mode. */
        AES_128_GCM(SecretKeyAlgorithm.AES, 128, "AES/GCM/NoPadding", 12),
        /** AES with a 192 bits key in GCM mode. */
        AES_192_GCM(SecretKeyAlgorithm.AES, 192, "AES/GCM/NoPadding", 12),
        /** AES with a 256 bits key in GCM mode. */
        AES_256_GCM(SecretKeyAlgorithm.AES, 256, "AES/GCM/NoPadding", 12),
        /** ChaCha20 with a 256 bits key and the Poly1305 tag. */
        CHACHA20_POLY1305(SecretKeyAlgorithm.CHACHA20, 256, "ChaCha20-Poly1305", 12),
        /**
         * XChaCha20 with the Poly1305 tag, which takes a nonce of 24 bytes so
         * that random nonces are safe to use, described in
         * <a href="https://datatracker.ietf.org/doc/html/draft-irtf-cfrg-xchacha-03">draft-irtf-cfrg-xchacha</a>.
         */
        XCHACHA20_POLY1305(SecretKeyAlgorithm.CHACHA20, 256, "ChaCha20-Poly1305", 24);

        /**
         * Size of the nonce in bytes, which is what the 96 bits of RFC 8439
         * ask for.
         */
        public static final int NONCE_BYTES = 12;
        /**
         * Size of the nonce of XChaCha20-Poly1305 in bytes, 192 bits, which is
         * long enough to pick at random without ever repeating one.
         */
        public static final int XCHACHA_NONCE_BYTES = 24;
        /**
         * Size of the authentication tag in bytes.
         */
        public static final int TAG_BYTES = 16;

        private final SecretKeyAlgorithm secretKeyAlgorithm;
        private final int keyBits;
        private final String transformation;
        private final int nonceBytes;

        AeadAlgorithm(SecretKeyAlgorithm secretKeyAlgorithm, int keyBits, String transformation, int nonceBytes)
        {
            this.secretKeyAlgorithm = secretKeyAlgorithm;
            this.keyBits = keyBits;
            this.transformation = transformation;
            this.nonceBytes = nonceBytes;
        }

        /**
         * Returns the algorithm of the secret key this cipher needs.
         *
         * @return the secret key algorithm
         */
        public SecretKeyAlgorithm secretKeyAlgorithm()
        {
            return secretKeyAlgorithm;
        }

        /**
         * Returns the size of the key in bits.
         *
         * @return the key size
         */
        public int keyBits()
        {
            return keyBits;
        }

        /**
         * Returns the size of the key in bytes.
         *
         * @return the key size
         */
        public int keyBytes()
        {
            return keyBits / Byte.SIZE;
        }

        /**
         * Returns the JCA transformation name.
         *
         * @return the JCA transformation
         */
        public String transformation()
        {
            return transformation;
        }

        /**
         * Returns the size of the nonce in bytes, which is
         * {@link #XCHACHA_NONCE_BYTES} for {@link #XCHACHA20_POLY1305}.
         *
         * @return the nonce size
         */
        public int nonceBytes()
        {
            return nonceBytes;
        }

        /**
         * Returns whether the key and the nonce go through HChaCha20 before
         * the cipher sees them, which is how XChaCha20 makes a 24 byte nonce
         * out of the 12 byte one that ChaCha20-Poly1305 takes.
         *
         * @return true when this algorithm derives its key and nonce
         */
        public boolean usesHChaCha20()
        {
            return this == XCHACHA20_POLY1305;
        }

        /**
         * Returns the size of the authentication tag in bytes.
         *
         * @return the tag size
         */
        public int tagBytes()
        {
            return TAG_BYTES;
        }
    }

    /**
     * Stream cipher algorithms, each of them with a 256 bits key and its own
     * nonce size.
     * <p>
     * They are implemented here instead of being asked to the backend because
     * neither Java nor Bouncy Castle provides all of them, or provides them
     * with the same nonce and counter, and because a stream cipher needs
     * neither a provider nor a parameter object.
     */
    public enum StreamAlgorithm
    {
        /**
         * ChaCha20 of
         * <a href="https://datatracker.ietf.org/doc/html/rfc8439">RFC 8439</a>,
         * with the 96 bits nonce it asks for.
         */
        CHACHA20(SecretKeyAlgorithm.CHACHA20, 12),
        /**
         * XChaCha20 of
         * <a href="https://datatracker.ietf.org/doc/html/draft-irtf-cfrg-xchacha-03">draft-irtf-cfrg-xchacha</a>,
         * whose 24 bytes nonce is long enough to be picked at random.
         */
        XCHACHA20(SecretKeyAlgorithm.CHACHA20, 24),
        /** Salsa20, with the 64 bits nonce it was defined with. */
        SALSA20(SecretKeyAlgorithm.SALSA20, 8),
        /** XSalsa20, with a 24 bytes nonce. */
        XSALSA20(SecretKeyAlgorithm.SALSA20, 24);

        private final SecretKeyAlgorithm secretKeyAlgorithm;
        private final int nonceBytes;

        StreamAlgorithm(SecretKeyAlgorithm secretKeyAlgorithm, int nonceBytes)
        {
            this.secretKeyAlgorithm = secretKeyAlgorithm;
            this.nonceBytes = nonceBytes;
        }

        /**
         * Returns the algorithm of the secret key this cipher needs.
         *
         * @return the secret key algorithm
         */
        public SecretKeyAlgorithm secretKeyAlgorithm()
        {
            return secretKeyAlgorithm;
        }

        /**
         * Returns the size of the key in bits, which is 256 for all of them.
         *
         * @return the key size
         */
        public int keyBits()
        {
            return 256;
        }

        /**
         * Returns the size of the key in bytes, which is 32 for all of them.
         *
         * @return the key size
         */
        public int keyBytes()
        {
            return keyBits() / Byte.SIZE;
        }

        /**
         * Returns the size of the nonce in bytes.
         *
         * @return the nonce size
         */
        public int nonceBytes()
        {
            return nonceBytes;
        }

        /**
         * Returns whether the key and the nonce go through HChaCha20 before
         * the cipher sees them, which is how XChaCha20 makes a 24 byte nonce
         * out of the 12 byte one that ChaCha20 takes.
         *
         * @return true when this algorithm derives its key and nonce
         */
        public boolean usesHChaCha20()
        {
            return this == XCHACHA20;
        }

        /**
         * Returns whether the block counter is 32 bits, which is the case of
         * ChaCha20 and therefore limits a single stream to 256 GB.
         *
         * @return true when the counter is 32 bits
         */
        public boolean has32BitsCounter()
        {
            return this == CHACHA20 || this == XCHACHA20;
        }
    }

    /**
     * Block cipher algorithms, which encrypt without authenticating, so
     * {@link AeadAlgorithm} is the better choice whenever the data may be
     * tampered with.
     * <p>
     * CTR turns a block cipher into a stream one, and it does not need the
     * data to be a whole number of blocks. CBC is chained with the previous
     * block and pads the last one with PKCS#5.
     */
    public enum CipherAlgorithm
    {
        /** AES with a 128 bits key in CTR mode. */
        AES_128_CTR(SecretKeyAlgorithm.AES, 128, "AES/CTR/NoPadding"),
        /** AES with a 192 bits key in CTR mode. */
        AES_192_CTR(SecretKeyAlgorithm.AES, 192, "AES/CTR/NoPadding"),
        /** AES with a 256 bits key in CTR mode. */
        AES_256_CTR(SecretKeyAlgorithm.AES, 256, "AES/CTR/NoPadding"),
        /** AES with a 128 bits key in CBC mode, with PKCS#5 padding. */
        AES_128_CBC(SecretKeyAlgorithm.AES, 128, "AES/CBC/PKCS5Padding"),
        /** AES with a 192 bits key in CBC mode, with PKCS#5 padding. */
        AES_192_CBC(SecretKeyAlgorithm.AES, 192, "AES/CBC/PKCS5Padding"),
        /** AES with a 256 bits key in CBC mode, with PKCS#5 padding. */
        AES_256_CBC(SecretKeyAlgorithm.AES, 256, "AES/CBC/PKCS5Padding");

        /**
         * Size of the block of AES in bytes, which is also the size of the
         * initialization vector of both modes.
         */
        public static final int BLOCK_BYTES = 16;

        private final SecretKeyAlgorithm secretKeyAlgorithm;
        private final int keyBits;
        private final String transformation;

        CipherAlgorithm(SecretKeyAlgorithm secretKeyAlgorithm, int keyBits, String transformation)
        {
            this.secretKeyAlgorithm = secretKeyAlgorithm;
            this.keyBits = keyBits;
            this.transformation = transformation;
        }

        /**
         * Returns the algorithm of the secret key this cipher needs.
         *
         * @return the secret key algorithm
         */
        public SecretKeyAlgorithm secretKeyAlgorithm()
        {
            return secretKeyAlgorithm;
        }

        /**
         * Returns the size of the key in bits.
         *
         * @return the key size
         */
        public int keyBits()
        {
            return keyBits;
        }

        /**
         * Returns the size of the key in bytes.
         *
         * @return the key size
         */
        public int keyBytes()
        {
            return keyBits / Byte.SIZE;
        }

        /**
         * Returns the JCA transformation name.
         *
         * @return the JCA transformation
         */
        public String transformation()
        {
            return transformation;
        }

        /**
         * Returns the size of the initialization vector in bytes, which is
         * the size of the AES block.
         *
         * @return the initialization vector size
         */
        public int ivBytes()
        {
            return BLOCK_BYTES;
        }

        /**
         * Returns whether the cipher pads the data to a whole number of
         * blocks with PKCS#5, which CBC needs and CTR does not.
         *
         * @return true when the data is padded
         */
        public boolean isPadded()
        {
            return this == AES_128_CBC || this == AES_192_CBC || this == AES_256_CBC;
        }

        /**
         * Returns the CTR mode of this key size, with the given
         * transformation.
         *
         * @param keyBits the size of the key in bits, 128, 192 or 256
         * @return the CTR cipher of that key size
         */
        public static CipherAlgorithm aesCtr(int keyBits)
        {
            switch (keyBits)
            {
                case 128:
                    return AES_128_CTR;
                case 192:
                    return AES_192_CTR;
                case 256:
                    return AES_256_CTR;
                default:
                    throw new IllegalArgumentException("an AES key must be 128, 192 or 256 bits long, " + keyBits + " given");
            }
        }

        /**
         * Returns the CBC mode of this key size, with PKCS#5 padding.
         *
         * @param keyBits the size of the key in bits, 128, 192 or 256
         * @return the CBC cipher of that key size
         */
        public static CipherAlgorithm aesCbc(int keyBits)
        {
            switch (keyBits)
            {
                case 128:
                    return AES_128_CBC;
                case 192:
                    return AES_192_CBC;
                case 256:
                    return AES_256_CBC;
                default:
                    throw new IllegalArgumentException("an AES key must be 128, 192 or 256 bits long, " + keyBits + " given");
            }
        }
    }

    /**
     * Key wrapping algorithms, which encrypt key data in a way that is
     * detected when it is unwrapped, described in
     * <a href="https://datatracker.ietf.org/doc/html/rfc3394">RFC 3394</a>.
     * <p>
     * The AES ones wrap with a key encryption key of 128, 192 or 256 bits.
     * The RSA one wraps with the public key of an RSA key pair and unwraps
     * with its private key.
     */
    public enum KeyWrapAlgorithm
    {
        /** AES key wrap with a 128 bits key encryption key. */
        AES_128_KW(128),
        /** AES key wrap with a 192 bits key encryption key. */
        AES_192_KW(192),
        /** AES key wrap with a 256 bits key encryption key. */
        AES_256_KW(256),
        /** RSA key wrap with an RSA key pair. */
        RSA_KW(0);

        private final int keyBits;

        KeyWrapAlgorithm(int keyBits)
        {
            this.keyBits = keyBits;
        }

        /**
         * Returns the size of the key encryption key in bits, which is zero
         * for {@link #RSA_KW}, that wraps with a key pair instead.
         *
         * @return the key encryption key size
         */
        public int keyBits()
        {
            return keyBits;
        }

        /**
         * Returns the size of the key encryption key in bytes, which is zero
         * for {@link #RSA_KW}.
         *
         * @return the key encryption key size
         */
        public int keyBytes()
        {
            return keyBits / Byte.SIZE;
        }

        /**
         * Returns the JCA transformation name, which is null for
         * {@link #RSA_KW}, that no provider implements.
         *
         * @return the JCA transformation, may be null
         */
        public String transformation()
        {
            return usesRsaKey() ? null : "AESWrap";
        }

        /**
         * Returns the algorithm of the key encryption key, which is null for
         * {@link #RSA_KW}, that wraps with a key pair instead.
         *
         * @return the secret key algorithm, may be null
         */
        public SecretKeyAlgorithm secretKeyAlgorithm()
        {
            return usesRsaKey() ? null : SecretKeyAlgorithm.AES;
        }

        /**
         * Returns whether the key data is wrapped with the public key of an
         * RSA key pair instead of with a key encryption key.
         *
         * @return true when this algorithm needs an RSA key pair
         */
        public boolean usesRsaKey()
        {
            return this == RSA_KW;
        }

        /**
         * Returns the AES key wrap of the given key encryption key size.
         *
         * @param keyBits the size of the key encryption key in bits, 128,
         * 192 or 256
         * @return the AES key wrap of that key size
         */
        public static KeyWrapAlgorithm aesKw(int keyBits)
        {
            switch (keyBits)
            {
                case 128:
                    return AES_128_KW;
                case 192:
                    return AES_192_KW;
                case 256:
                    return AES_256_KW;
                default:
                    throw new IllegalArgumentException("an AES key encryption key must be 128, 192 or 256 bits long, "
                            + keyBits + " given");
            }
        }
    }

    /**
     * Algorithms for HMAC based key derivation, see
     * <a href="https://datatracker.ietf.org/doc/html/rfc5869">RFC 5869</a>.
     */
    public enum HkdfAlgorithm
    {
        /** HKDF with HMAC-SHA256, the usual default. */
        SHA256(HmacAlgorithm.SHA256),
        /** HKDF with HMAC-SHA384. */
        SHA384(HmacAlgorithm.SHA384),
        /** HKDF with HMAC-SHA512. */
        SHA512(HmacAlgorithm.SHA512);

        private final HmacAlgorithm hmacAlgorithm;

        HkdfAlgorithm(HmacAlgorithm hmacAlgorithm)
        {
            this.hmacAlgorithm = hmacAlgorithm;
        }

        /**
         * Returns the HMAC algorithm used to derive keys.
         *
         * @return the hmac algorithm
         */
        public HmacAlgorithm hmacAlgorithm()
        {
            return hmacAlgorithm;
        }

        /**
         * Returns the size of the hash in bytes.
         *
         * @return the hash size
         */
        public int hashBytes()
        {
            return hmacAlgorithm.digestBytes();
        }
    }

    /**
     * Algorithms for password based key derivation, see
     * <a href="https://datatracker.ietf.org/doc/html/rfc8018">RFC 8018</a>.
     * <p>
     * Passwords must be stretched, therefore {@link #minimumIterations()} is
     * the lowest number of iterations accepted by {@link Kdf#pbkdf2}.
     */
    public enum Pbkdf2Algorithm
    {
        /** PBKDF2 with HMAC-SHA256, 600000 iterations. */
        HMAC_SHA256(HmacAlgorithm.SHA256, 600000),
        /** PBKDF2 with HMAC-SHA512, 210000 iterations. */
        HMAC_SHA512(HmacAlgorithm.SHA512, 210000);

        /**
         * Minimum size of the salt in bytes, the size recommended for
         * PBKDF2.
         */
        public static final int MINIMUM_SALT_BYTES = 16;

        private final HmacAlgorithm hmacAlgorithm;
        private final int minimumIterations;

        Pbkdf2Algorithm(HmacAlgorithm hmacAlgorithm, int minimumIterations)
        {
            this.hmacAlgorithm = hmacAlgorithm;
            this.minimumIterations = minimumIterations;
        }

        /**
         * Returns the HMAC algorithm used to derive keys.
         *
         * @return the hmac algorithm
         */
        public HmacAlgorithm hmacAlgorithm()
        {
            return hmacAlgorithm;
        }

        /**
         * Returns the minimum number of iterations that is still considered
         * strong for this algorithm.
         *
         * @return the minimum number of iterations
         */
        public int minimumIterations()
        {
            return minimumIterations;
        }
    }

    /**
     * Signature algorithms. RSA and ECDSA are combined here with the hash they
     * sign, because that is how JCA names them; Ed25519 signs the message
     * itself and cannot be combined with a hash.
     */
    public enum SignatureAlgorithm
    {
        /** RSA with SHA-256. */
        SHA256_WITH_RSA("SHA256withRSA", KeyAlgorithm.RSA),
        /** RSA with SHA-384. */
        SHA384_WITH_RSA("SHA384withRSA", KeyAlgorithm.RSA),
        /** RSA with SHA-512. */
        SHA512_WITH_RSA("SHA512withRSA", KeyAlgorithm.RSA),
        /** ECDSA with SHA-256. */
        SHA256_WITH_ECDSA("SHA256withECDSA", KeyAlgorithm.EC),
        /** ECDSA with SHA-384. */
        SHA384_WITH_ECDSA("SHA384withECDSA", KeyAlgorithm.EC),
        /** ECDSA with SHA-512. */
        SHA512_WITH_ECDSA("SHA512withECDSA", KeyAlgorithm.EC),
        /** Ed25519, which signs the message itself, so it takes no hash. */
        ED25519("Ed25519", KeyAlgorithm.ED25519);

        private final String jcaName;
        private final KeyAlgorithm keyAlgorithm;

        SignatureAlgorithm(String jcaName, KeyAlgorithm keyAlgorithm)
        {
            this.jcaName = jcaName;
            this.keyAlgorithm = keyAlgorithm;
        }

        /**
         * Returns the JCA algorithm name.
         *
         * @return the JCA name
         */
        public String jcaName()
        {
            return jcaName;
        }

        /**
         * Returns the kind of key this signature needs.
         *
         * @return the key algorithm
         */
        public KeyAlgorithm keyAlgorithm()
        {
            return keyAlgorithm;
        }
    }

    /**
     * Variants of Argon2, the password hashing function of the Password
     * Hashing Competition, described in
     * <a href="https://datatracker.ietf.org/doc/html/rfc9106">RFC 9106</a>.
     * <p>
     * {@link #ID} is the one to use unless there is a reason not to: it
     * resists both side channel attacks on the memory access pattern and
     * attacks on the time memory trade off.
     */
    public enum Argon2Algorithm
    {
        /** Data depending version, the strongest and the slowest. */
        D("argon2d"),
        /** Data independent version, the safest against side channels. */
        I("argon2i"),
        /** Hybrid of {@link #D} and {@link #I}, the recommended one. */
        ID("argon2id");

        private final String jcaName;

        Argon2Algorithm(String jcaName)
        {
            this.jcaName = jcaName;
        }

        /**
         * Returns the name used in the encoded form of a hash.
         *
         * @return the name, like "argon2id"
         */
        public String jcaName()
        {
            return jcaName;
        }
    }

    /**
     * Password hashing algorithms, the ones meant to store a password and
     * check it later instead of stretching it into a key.
     * <p>
     * These are memory hard, which is the point: unlike {@link Kdf#pbkdf2},
     * making them slower costs memory as well, so a fast GPU or FPGA cannot
     * guess many passwords at once.
     * <p>
     * All of them need Bouncy Castle, the JDK has none of them.
     */
    public enum PasswordAlgorithm
    {
        /** Argon2d, see {@link Argon2Algorithm#D}. */
        ARGON2D(Argon2Algorithm.D, "argon2d"),
        /** Argon2i, see {@link Argon2Algorithm#I}. */
        ARGON2I(Argon2Algorithm.I, "argon2i"),
        /** Argon2id, see {@link Argon2Algorithm#ID} and the recommended one. */
        ARGON2ID(Argon2Algorithm.ID, "argon2id"),
        /** scrypt, described in <a href="https://datatracker.ietf.org/doc/html/rfc7914">RFC 7914</a>. */
        SCRYPT(null, "scrypt"),
        /** bcrypt, the oldest one here and the one many systems still store. */
        BCRYPT(null, "bcrypt");

        private final Argon2Algorithm argon2Algorithm;
        private final String jcaName;

        PasswordAlgorithm(Argon2Algorithm argon2Algorithm, String jcaName)
        {
            this.argon2Algorithm = argon2Algorithm;
            this.jcaName = jcaName;
        }

        /**
         * Returns the name used in the encoded form of a hash.
         *
         * @return the name, like "argon2id"
         */
        public String jcaName()
        {
            return jcaName;
        }

        /**
         * Returns the Argon2 variant of this algorithm.
         *
         * @return the variant, or null when this is not an Argon2 algorithm
         */
        public Argon2Algorithm argon2Algorithm()
        {
            return argon2Algorithm;
        }
    }

    /**
     * Public key algorithms.
     */
    public enum KeyAlgorithm
    {
        /** RSA with a 3072 bits key by default. */
        RSA("RSA", 3072),
        /** An elliptic curve key pair, see {@link EcCurve}. */
        EC("EC", 256),
        /** Ed25519, which signs and takes no hash. */
        ED25519("Ed25519", 256),
        /** X25519, which does key agreement and no signatures. */
        X25519("X25519", 256);

        private final String jcaName;
        private final int defaultKeyBits;

        KeyAlgorithm(String jcaName, int defaultKeyBits)
        {
            this.jcaName = jcaName;
            this.defaultKeyBits = defaultKeyBits;
        }

        /**
         * Returns the JCA algorithm name.
         *
         * @return the JCA name
         */
        public String jcaName()
        {
            return jcaName;
        }

        /**
         * Returns the recommended key size in bits, used when the caller does
         * not ask for a specific one.
         *
         * @return the default key size
         */
        public int defaultKeyBits()
        {
            return defaultKeyBits;
        }
    }

    /**
     * Elliptic curves supported for key pairs.
     */
    public enum EcCurve
    {
        /** NIST P-256, also known as prime256v1. */
        SECP256R1("secp256r1", 256),
        /** NIST P-384. */
        SECP384R1("secp384r1", 384),
        /** NIST P-521. */
        SECP521R1("secp521r1", 521),
        /**
         * Only available with Bouncy Castle, it is not a NIST curve and it
         * must not be used for signatures, only for compatibility with
         * existing systems.
         */
        SECP256K1("secp256k1", 256);

        private final String jcaName;
        private final int keyBits;

        EcCurve(String jcaName, int keyBits)
        {
            this.jcaName = jcaName;
            this.keyBits = keyBits;
        }

        /**
         * Returns the curve name understood by JCA.
         *
         * @return the curve name
         */
        public String jcaName()
        {
            return jcaName;
        }

        /**
         * Returns the size of the key in bits.
         *
         * @return the key size
         */
        public int keyBits()
        {
            return keyBits;
        }
    }

    /**
     * Symmetric key algorithms.
     */
    public enum SecretKeyAlgorithm
    {
        /** AES with a 128, 192 or 256 bits key. */
        AES("AES"),
        /** ChaCha20 with a 256 bits key. */
        CHACHA20("ChaCha20"),
        /** Salsa20 with a 256 bits key. */
        SALSA20("Salsa20");

        private final String jcaName;

        SecretKeyAlgorithm(String jcaName)
        {
            this.jcaName = jcaName;
        }

        /**
         * Returns the JCA algorithm name.
         *
         * @return the JCA name
         */
        public String jcaName()
        {
            return jcaName;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Exceptions //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Unchecked exception for every failure of a cryptographic operation. It
     * replaces the long list of checked exceptions of the JCA, which are
     * almost never recoverable.
     */
    public static class CryptoException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new exception.
         *
         * @param message the message
         */
        public CryptoException(String message)
        {
            super(message);
        }

        /**
         * Creates a new exception.
         *
         * @param message the message
         * @param cause the cause
         */
        public CryptoException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Thrown when the requested algorithm is not available in this backend,
     * either because the Java runtime is too old or because Bouncy Castle is
     * missing.
     * <p>
     * Use {@link #supports(HashAlgorithm)} and friends to know in advance.
     */
    public static class UnsupportedAlgorithmException extends CryptoException
    {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new exception.
         *
         * @param message the message
         */
        public UnsupportedAlgorithmException(String message)
        {
            super(message);
        }

        /**
         * Creates a new exception.
         *
         * @param message the message
         * @param cause the cause
         */
        public UnsupportedAlgorithmException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Thrown when the authentication tag of an authenticated encryption does
     * not match: the ciphertext, the associated data, the nonce or the key are
     * wrong, or the data has been tampered with.
     * <p>
     * It is deliberately a different exception, because the right reaction is
     * to discard the data and to treat it as hostile, not to retry.
     */
    public static class BadTagException extends CryptoException
    {
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new exception.
         *
         * @param message the message
         */
        public BadTagException(String message)
        {
            super(message);
        }

        /**
         * Creates a new exception.
         *
         * @param message the message
         * @param cause the cause
         */
        public BadTagException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Hash ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Message digests, the hash namespace of {@link Kr}.
     */
    public final class Hash
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Hash()
        {
        }

        /**
         * Computes the digest of the concatenation of the given chunks.
         *
         * @param algorithm the hash algorithm, not null
         * @param data the chunks of data to hash, neither null nor containing
         * nulls
         * @return the digest, whose length is
         * {@link HashAlgorithm#digestBytes()}
         * @throws UnsupportedAlgorithmException when the algorithm is not
         * available
         * @throws CryptoException when the algorithm fails
         */
        public byte[] digest(HashAlgorithm algorithm, byte[]... data)
        {
            require(algorithm, "algorithm");
            MessageDigest digest = messageDigest(algorithm.jcaName());
            update(digest::update, data);
            return digest.digest();
        }

        /**
         * Computes the digest of a slice of an array.
         *
         * @param algorithm the hash algorithm, not null
         * @param data the data to hash, not null
         * @param offset the first byte to hash
         * @param length the number of bytes to hash
         * @return the digest
         */
        public byte[] digest(HashAlgorithm algorithm, byte[] data, int offset, int length)
        {
            require(algorithm, "algorithm");
            require(data, "data");
            MessageDigest digest = messageDigest(algorithm.jcaName());
            digest.update(data, offset, length);
            return digest.digest();
        }

        /**
         * Computes the digest of the UTF-8 bytes of a text.
         *
         * @param algorithm the hash algorithm, not null
         * @param text the text to hash, not null
         * @return the digest
         */
        public byte[] digest(HashAlgorithm algorithm, CharSequence text)
        {
            require(text, "text");
            return digest(algorithm, utf8(text));
        }

        /**
         * Computes the digest of the concatenation of the given chunks and
         * returns it as a lower case hexadecimal string.
         *
         * @param algorithm the hash algorithm, not null
         * @param data the chunks of data to hash, neither null nor containing
         * nulls
         * @return the digest as hexadecimal
         */
        public String hex(HashAlgorithm algorithm, byte[]... data)
        {
            return Hex.encode(digest(algorithm, data));
        }

        /**
         * Computes the digest of the UTF-8 bytes of a text and returns it as a
         * lower case hexadecimal string.
         *
         * @param algorithm the hash algorithm, not null
         * @param text the text to hash, not null
         * @return the digest as hexadecimal
         */
        public String hex(HashAlgorithm algorithm, CharSequence text)
        {
            require(text, "text");
            return hex(algorithm, utf8(text));
        }

        /**
         * Computes the SHA-256 digest of the concatenation of the given
         * chunks.
         *
         * @param data the chunks of data to hash
         * @return the 32 bytes digest
         */
        public byte[] sha256(byte[]... data)
        {
            return digest(HashAlgorithm.SHA256, data);
        }

        /**
         * Computes the SHA-256 digest of the UTF-8 bytes of a text.
         *
         * @param text the text to hash, not null
         * @return the 32 bytes digest
         */
        public byte[] sha256(CharSequence text)
        {
            return digest(HashAlgorithm.SHA256, text);
        }

        /**
         * Computes the SHA-384 digest of the concatenation of the given
         * chunks.
         *
         * @param data the chunks of data to hash
         * @return the 48 bytes digest
         */
        public byte[] sha384(byte[]... data)
        {
            return digest(HashAlgorithm.SHA384, data);
        }

        /**
         * Computes the SHA-384 digest of the UTF-8 bytes of a text.
         *
         * @param text the text to hash, not null
         * @return the 48 bytes digest
         */
        public byte[] sha384(CharSequence text)
        {
            return digest(HashAlgorithm.SHA384, text);
        }

        /**
         * Computes the SHA-512 digest of the concatenation of the given
         * chunks.
         *
         * @param data the chunks of data to hash
         * @return the 64 bytes digest
         */
        public byte[] sha512(byte[]... data)
        {
            return digest(HashAlgorithm.SHA512, data);
        }

        /**
         * Computes the SHA-512 digest of the UTF-8 bytes of a text.
         *
         * @param text the text to hash, not null
         * @return the 64 bytes digest
         */
        public byte[] sha512(CharSequence text)
        {
            return digest(HashAlgorithm.SHA512, text);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// HMAC ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Keyed hashing, the auth namespace of {@link Kr}.
     */
    public final class Hmac
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Hmac()
        {
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks.
         *
         * @param algorithm the hmac algorithm, not null
         * @param key the key bytes, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the tag, whose length is
         * {@link HmacAlgorithm#digestBytes()}
         * @throws UnsupportedAlgorithmException when the algorithm is not
         * available
         */
        public byte[] hmac(HmacAlgorithm algorithm, byte[] key, byte[]... data)
        {
            require(key, "key");
            return hmac(algorithm, new SecretKeySpec(key.clone(), algorithm.jcaName()), data);
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks.
         *
         * @param algorithm the hmac algorithm, not null
         * @param key the key, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the tag
         */
        public byte[] hmac(HmacAlgorithm algorithm, SecretKey key, byte[]... data)
        {
            Mac mac = mac(algorithm.jcaName());
            try
            {
                mac.init(require(key, "key"));
            }
            catch (InvalidKeyException ex)
            {
                throw new CryptoException("the key cannot be used with " + algorithm, ex);
            }
            update(mac::update, data);
            return mac.doFinal();
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks and returns it in hexadecimal, handy for logs and for
         * comparisons against a value that comes from somewhere else.
         *
         * @param algorithm the hmac algorithm, not null
         * @param key the key bytes, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the tag in lowercase hexadecimal
         */
        public String hex(HmacAlgorithm algorithm, byte[] key, byte[]... data)
        {
            return Hex.encode(hmac(algorithm, key, data));
        }

        /**
         * Computes the HMAC-SHA256 tag of the concatenation of the given
         * chunks.
         *
         * @param key the key bytes, not null
         * @param data the chunks of data to authenticate
         * @return the 32 bytes tag
         */
        public byte[] sha256(byte[] key, byte[]... data)
        {
            return hmac(HmacAlgorithm.SHA256, key, data);
        }

        /**
         * Computes the HMAC-SHA384 tag of the concatenation of the given
         * chunks.
         *
         * @param key the key bytes, not null
         * @param data the chunks of data to authenticate
         * @return the 48 bytes tag
         */
        public byte[] sha384(byte[] key, byte[]... data)
        {
            return hmac(HmacAlgorithm.SHA384, key, data);
        }

        /**
         * Computes the HMAC-SHA512 tag of the concatenation of the given
         * chunks.
         *
         * @param key the key bytes, not null
         * @param data the chunks of data to authenticate
         * @return the 64 bytes tag
         */
        public byte[] sha512(byte[] key, byte[]... data)
        {
            return hmac(HmacAlgorithm.SHA512, key, data);
        }

        /**
         * Verifies an expected HMAC tag comparing it in constant time.
         *
         * @param expectedTag the tag to compare against, not null
         * @param algorithm the hmac algorithm, not null
         * @param key the key bytes, not null
         * @param data the chunks of data to authenticate
         * @return true when the tag matches, false otherwise
         */
        public boolean verify(byte[] expectedTag, HmacAlgorithm algorithm, byte[] key, byte[]... data)
        {
            require(expectedTag, "expectedTag");
            return utils.timingSafeEql(expectedTag, hmac(algorithm, key, data));
        }

        /**
         * Verifies an expected HMAC tag comparing it in constant time.
         *
         * @param expectedTag the tag to compare against, not null
         * @param algorithm the hmac algorithm, not null
         * @param key the key, not null
         * @param data the chunks of data to authenticate
         * @return true when the tag matches, false otherwise
         */
        public boolean verify(byte[] expectedTag, HmacAlgorithm algorithm, SecretKey key, byte[]... data)
        {
            require(expectedTag, "expectedTag");
            return utils.timingSafeEql(expectedTag, hmac(algorithm, key, data));
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Poly1305 ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * The Poly1305 message authentication code of RFC 8439, the poly1305
     * namespace of {@link Kr}.
     * <p>
     * This is the one time authenticator of the ChaCha20-Poly1305 AEAD taken
     * on its own. It is not a key derivation function and it is not safe to
     * use the same key twice: the key is the pair <code>(r, s)</code> of the
     * key generation in section 2.6 of RFC 8439, where <code>r</code> is not
     * secret and is what makes recovering <code>s</code> from the tag easy.
     *
     * @see Hmac
     */
    public final class Poly1305
    {
        /**
         * The key is <code>r</code> followed by <code>s</code>.
         */
        private static final int KEY_BYTES = 32;

        /**
         * The number of bytes of a block of the message.
         */
        private static final int BLOCK_BYTES = 16;

        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Poly1305()
        {
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks.
         *
         * @param key the one time key bytes, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the 16 bytes tag
         * @throws IllegalArgumentException when the key is not 32 bytes long
         */
        public byte[] poly1305(byte[] key, byte[]... data)
        {
            require(key, "key");
            return poly1305(new SecretKeySpec(key.clone(), "Poly1305"), data);
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks.
         *
         * @param key the one time key, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the 16 bytes tag
         * @throws IllegalArgumentException when the key is not 32 bytes long
         */
        public byte[] poly1305(SecretKey key, byte[]... data)
        {
            byte[] keyBytes = encodedKey(require(key, "key"));
            if (keyBytes.length != KEY_BYTES)
            {
                throw new IllegalArgumentException("a Poly1305 key is exactly " + KEY_BYTES + " bytes long, "
                        + keyBytes.length + " given");
            }
            //the first half of the key is r, which gets clamped, the second half is s
            BigInteger r = littleEndian(keyBytes, 0, BLOCK_BYTES).and(POLY1305_CLAMP);
            BigInteger s = littleEndian(keyBytes, BLOCK_BYTES, BLOCK_BYTES);
            require(data, "data");
            BigInteger accumulator = BigInteger.ZERO;
            byte[] message = utils.concat(data);
            //every block that is complete gets a 0x01 byte after its highest bit
            int complete = message.length / BLOCK_BYTES * BLOCK_BYTES;
            for (int offset = 0; offset < complete; offset += BLOCK_BYTES)
            {
                accumulator = multiply(accumulator, r, littleEndian(message, offset, BLOCK_BYTES).add(POLY1305_TAG_MODULUS));
            }
            //and the last block, which may be shorter, gets that byte where its data ends
            if (complete < message.length)
            {
                int length = message.length - complete;
                accumulator = multiply(accumulator, r, littleEndian(message, complete, length)
                        .add(BigInteger.ONE.shiftLeft(length * Byte.SIZE)));
            }
            //the tag is what is left after adding s, which wraps around
            return littleEndian(accumulator.add(s).mod(POLY1305_TAG_MODULUS), BLOCK_BYTES);
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks and returns it in hexadecimal, handy for logs and for
         * comparisons against a value that comes from somewhere else.
         *
         * @param key the one time key bytes, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the tag in hexadecimal
         */
        public String hex(byte[] key, byte[]... data)
        {
            return Hex.encode(poly1305(key, data));
        }

        /**
         * Computes the authentication tag of the concatenation of the given
         * chunks and returns it in hexadecimal, handy for logs and for
         * comparisons against a value that comes from somewhere else.
         *
         * @param key the one time key, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return the tag in hexadecimal
         */
        public String hex(SecretKey key, byte[]... data)
        {
            return Hex.encode(poly1305(key, data));
        }

        /**
         * Verifies an expected Poly1305 tag comparing it in constant time.
         *
         * @param expectedTag the tag to compare against, not null
         * @param key the one time key bytes, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return true when the tag matches, false otherwise
         */
        public boolean verify(byte[] expectedTag, byte[] key, byte[]... data)
        {
            require(expectedTag, "expectedTag");
            return utils.timingSafeEql(expectedTag, poly1305(key, data));
        }

        /**
         * Verifies an expected Poly1305 tag comparing it in constant time.
         *
         * @param expectedTag the tag to compare against, not null
         * @param key the one time key, not null
         * @param data the chunks of data to authenticate, neither null nor
         * containing nulls
         * @return true when the tag matches, false otherwise
         */
        public boolean verify(byte[] expectedTag, SecretKey key, byte[]... data)
        {
            require(expectedTag, "expectedTag");
            return utils.timingSafeEql(expectedTag, poly1305(key, data));
        }

        /**
         * The accumulator is multiplied by <code>r</code> modulo the prime of
         * the field.
         */
        private BigInteger multiply(BigInteger accumulator, BigInteger r, BigInteger block)
        {
            return accumulator.add(block).multiply(r).mod(POLY1305_PRIME);
        }

        /**
         * Reads a number out of bytes that are in the little-endian order
         * Poly1305 uses.
         */
        private BigInteger littleEndian(byte[] bytes, int offset, int length)
        {
            byte[] reversed = new byte[length];
            for (int i = 0; i < length; i++)
            {
                reversed[i] = bytes[offset + length - 1 - i];
            }
            return new BigInteger(1, reversed);
        }

        /**
         * Writes a number as the fixed length little-endian value Poly1305
         * serializes.
         */
        private byte[] littleEndian(BigInteger value, int length)
        {
            byte[] bytes = value.toByteArray();
            byte[] serialized = new byte[length];
            //the value can take one byte more than the length when its highest bit is set
            for (int i = 0; i < serialized.length && i < bytes.length; i++)
            {
                serialized[i] = bytes[bytes.length - 1 - i];
            }
            return serialized;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// AEAD ///////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Factory of authenticated encryption ciphers, the aead namespace of
     * {@link Kr}.
     */
    public final class Aeads
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Aeads()
        {
        }

        /**
         * Returns an AES-GCM cipher for the given key, choosing 128, 192 or
         * 256 bits from its length.
         *
         * @param key the key, not null
         * @return the cipher, ready to seal and open
         */
        public Aead aesGcm(SecretKey key)
        {
            require(key, "key");
            switch (encodedKey(key).length)
            {
                case 16:
                    return of(AeadAlgorithm.AES_128_GCM, key);
                case 24:
                    return of(AeadAlgorithm.AES_192_GCM, key);
                case 32:
                    return of(AeadAlgorithm.AES_256_GCM, key);
                default:
                    throw new IllegalArgumentException("an AES-GCM key must be 16, 24 or 32 bytes long, " + encodedKey(key).length + " given");
            }
        }

        /**
         * Returns an AES-GCM cipher for the given key bytes, choosing 128, 192
         * or 256 bits from their length.
         *
         * @param keyBytes the key bytes, not null
         * @return the cipher, ready to seal and open
         */
        public Aead aesGcm(byte[] keyBytes)
        {
            require(keyBytes, "keyBytes");
            return of(aesGcmAlgorithm(keyBytes.length), new SecretKeySpec(keyBytes.clone(), SecretKeyAlgorithm.AES.jcaName()));
        }

        /**
         * Returns a ChaCha20-Poly1305 cipher for the given key.
         *
         * @param key the key, not null
         * @return the cipher, ready to seal and open
         */
        public Aead chacha20Poly1305(SecretKey key)
        {
            return of(AeadAlgorithm.CHACHA20_POLY1305, key);
        }

        /**
         * Returns a ChaCha20-Poly1305 cipher for the given key bytes.
         *
         * @param keyBytes the 32 key bytes, not null
         * @return the cipher, ready to seal and open
         */
        public Aead chacha20Poly1305(byte[] keyBytes)
        {
            require(keyBytes, "keyBytes");
            return of(AeadAlgorithm.CHACHA20_POLY1305, new SecretKeySpec(keyBytes.clone(), SecretKeyAlgorithm.CHACHA20.jcaName()));
        }

        /**
         * Returns an XChaCha20-Poly1305 cipher for the given key, whose 24
         * bytes nonce is long enough to be picked at random.
         *
         * @param key the key, not null
         * @return the cipher, ready to seal and open
         */
        public Aead xchacha20Poly1305(SecretKey key)
        {
            return of(AeadAlgorithm.XCHACHA20_POLY1305, key);
        }

        /**
         * Returns an XChaCha20-Poly1305 cipher for the given key bytes.
         *
         * @param keyBytes the 32 key bytes, not null
         * @return the cipher, ready to seal and open
         */
        public Aead xchacha20Poly1305(byte[] keyBytes)
        {
            require(keyBytes, "keyBytes");
            return of(AeadAlgorithm.XCHACHA20_POLY1305,
                    new SecretKeySpec(keyBytes.clone(), SecretKeyAlgorithm.CHACHA20.jcaName()));
        }

        /**
         * Returns a cipher for the given algorithm, checking that the key has
         * exactly the expected size and that the backend supports the
         * algorithm.
         *
         * @param algorithm the aead algorithm, not null
         * @param key the key, not null
         * @return the cipher, ready to seal and open
         * @throws UnsupportedAlgorithmException when the algorithm is not
         * available in this backend
         */
        public Aead of(AeadAlgorithm algorithm, SecretKey key)
        {
            require(algorithm, "algorithm");
            require(key, "key");
            byte[] keyBytes = encodedKey(key);
            if (keyBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("a " + algorithm + " key must be exactly " + algorithm.keyBytes() + " bytes long, " + keyBytes.length + " given");
            }
            String keyAlgorithm = key.getAlgorithm();
            if (keyAlgorithm != null && !algorithm.secretKeyAlgorithm().jcaName().equalsIgnoreCase(keyAlgorithm))
            {
                throw new IllegalArgumentException("a " + algorithm + " cipher needs a " + algorithm.secretKeyAlgorithm().jcaName()
                        + " key, a " + keyAlgorithm + " key was given");
            }
            if (!supports(algorithm))
            {
                throw unsupported(algorithm.transformation(), null);
            }
            return new Aead(algorithm, key);
        }

        private AeadAlgorithm aesGcmAlgorithm(int keyBytes)
        {
            switch (keyBytes)
            {
                case 16:
                    return AeadAlgorithm.AES_128_GCM;
                case 24:
                    return AeadAlgorithm.AES_192_GCM;
                case 32:
                    return AeadAlgorithm.AES_256_GCM;
                default:
                    throw new IllegalArgumentException("an AES-GCM key must be 16, 24 or 32 bytes long, " + keyBytes + " given");
            }
        }

    }

    /**
     * An authenticated encryption cipher bound to a key.
     * <p>
     * {@link #seal(byte[])} and {@link #open(byte[])} are the methods to use:
     * they take care of the nonce, which is generated, prepended to the
     * result and never reused. The methods with an explicit nonce are only
     * for the rare cases with a protocol that dictates how nonces are
     * generated, such as a counter shared with the other party or a
     * deterministic derivation.
     */
    public final class Aead
    {
        private final AeadAlgorithm algorithm;
        private final SecretKey key;

        Aead(AeadAlgorithm algorithm, SecretKey key)
        {
            this.algorithm = algorithm;
            this.key = key;
        }

        /**
         * Returns the algorithm of this cipher.
         *
         * @return the aead algorithm
         */
        public AeadAlgorithm algorithm()
        {
            return algorithm;
        }

        /**
         * Returns the size of the nonce this cipher needs, which is
         * {@link AeadAlgorithm#XCHACHA_NONCE_BYTES} for
         * {@link AeadAlgorithm#XCHACHA20_POLY1305} and
         * {@link AeadAlgorithm#NONCE_BYTES} for the rest.
         *
         * @return the nonce size in bytes
         */
        public int nonceBytes()
        {
            return algorithm.nonceBytes();
        }

        /**
         * Returns the size of the authentication tag this cipher adds.
         *
         * @return the tag size in bytes
         */
        public int tagBytes()
        {
            return algorithm.tagBytes();
        }

        /**
         * Encrypts and authenticates the plaintext with a fresh random nonce.
         *
         * @param plaintext the data to encrypt, not null
         * @return the nonce, the ciphertext and the authentication tag
         */
        public byte[] seal(byte[] plaintext)
        {
            return seal(plaintext, null);
        }

        /**
         * Encrypts and authenticates the plaintext with a fresh random nonce,
         * also authenticating data that is not encrypted.
         *
         * @param plaintext the data to encrypt, not null
         * @param aad additional authenticated data, not encrypted and not
         * needed to decrypt, may be null
         * @return the nonce, the ciphertext and the authentication tag
         */
        public byte[] seal(byte[] plaintext, byte[] aad)
        {
            byte[] nonce = random.bytes(nonceBytes());
            byte[] ciphertextAndTag = sealWithNonce(nonce, plaintext, aad);
            return utils.concat(nonce, ciphertextAndTag);
        }

        /**
         * Encrypts and authenticates the plaintext with the given nonce.
         *
         * @param nonce the nonce, {@link #nonceBytes()} bytes, not null
         * @param plaintext the data to encrypt, not null
         * @return the ciphertext and the authentication tag, without the nonce
         */
        public byte[] sealWithNonce(byte[] nonce, byte[] plaintext)
        {
            return sealWithNonce(nonce, plaintext, null);
        }

        /**
         * Encrypts and authenticates the plaintext with the given nonce.
         *
         * @param nonce the nonce, {@link #nonceBytes()} bytes, not null
         * @param plaintext the data to encrypt, not null
         * @param aad additional authenticated data, may be null
         * @return the ciphertext and the authentication tag, without the nonce
         */
        public byte[] sealWithNonce(byte[] nonce, byte[] plaintext, byte[] aad)
        {
            require(nonce, "nonce");
            require(plaintext, "plaintext");
            checkNonce(nonce);
            Cipher cipher = cipher(algorithm.transformation());
            try
            {
                cipher.init(Cipher.ENCRYPT_MODE, cipherKey(nonce), nonceSpec(algorithm, cipherNonce(nonce)));
                if (aad != null && aad.length > 0)
                {
                    cipher.updateAAD(aad);
                }
                return cipher.doFinal(plaintext);
            }
            catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
            {
                throw new CryptoException("the data cannot be encrypted with " + algorithm, ex);
            }
        }

        /**
         * Decrypts and verifies the data sealed by {@link #seal(byte[])}.
         *
         * @param sealed the nonce, the ciphertext and the tag, not null
         * @return the plaintext
         * @throws BadTagException when the data has been tampered with or the
         * key is not the right one
         */
        public byte[] open(byte[] sealed)
        {
            return open(sealed, null);
        }

        /**
         * Decrypts and verifies the data sealed by {@link #seal(byte[], byte[])}.
         *
         * @param sealed the nonce, the ciphertext and the tag, not null
         * @param aad the additional authenticated data used to seal, may be
         * null
         * @return the plaintext
         * @throws BadTagException when the data has been tampered with or the
         * key is not the right one
         */
        public byte[] open(byte[] sealed, byte[] aad)
        {
            require(sealed, "sealed");
            if (sealed.length < nonceBytes() + tagBytes())
            {
                throw new CryptoException("sealed data is too short to be a valid " + algorithm + " envelope");
            }
            return openWithNonce(Arrays.copyOfRange(sealed, 0, nonceBytes()),
                    Arrays.copyOfRange(sealed, nonceBytes(), sealed.length), aad);
        }

        /**
         * Decrypts and verifies data sealed with an explicit nonce.
         *
         * @param nonce the nonce, {@link #nonceBytes()} bytes, not null
         * @param ciphertextAndTag the ciphertext and the tag, not null
         * @return the plaintext
         * @throws BadTagException when the data has been tampered with or the
         * key is not the right one
         */
        public byte[] openWithNonce(byte[] nonce, byte[] ciphertextAndTag)
        {
            return openWithNonce(nonce, ciphertextAndTag, null);
        }

        /**
         * Decrypts and verifies data sealed with an explicit nonce.
         *
         * @param nonce the nonce, {@link #nonceBytes()} bytes, not null
         * @param ciphertextAndTag the ciphertext and the tag, not null
         * @param aad the additional authenticated data used to seal, may be
         * null
         * @return the plaintext
         * @throws BadTagException when the data has been tampered with or the
         * key is not the right one
         */
        public byte[] openWithNonce(byte[] nonce, byte[] ciphertextAndTag, byte[] aad)
        {
            require(nonce, "nonce");
            require(ciphertextAndTag, "ciphertextAndTag");
            checkNonce(nonce);
            if (ciphertextAndTag.length < tagBytes())
            {
                throw new CryptoException("the ciphertext is too short to hold a " + tagBytes() + " bytes authentication tag");
            }
            Cipher cipher = cipher(algorithm.transformation());
            try
            {
                cipher.init(Cipher.DECRYPT_MODE, cipherKey(nonce), nonceSpec(algorithm, cipherNonce(nonce)));
                if (aad != null && aad.length > 0)
                {
                    cipher.updateAAD(aad);
                }
                return cipher.doFinal(ciphertextAndTag);
            }
            catch (BadPaddingException | IllegalBlockSizeException ex)
            {
                //any of these means the same thing here: the tag does not match
                throw new BadTagException("the authentication tag does not match: the data is corrupted, has been tampered with, or the key is wrong", ex);
            }
            catch (InvalidKeyException | InvalidAlgorithmParameterException ex)
            {
                throw new CryptoException("the data cannot be decrypted with " + algorithm, ex);
            }
        }

        private void checkNonce(byte[] nonce)
        {
            if (nonce.length != nonceBytes())
            {
                throw new IllegalArgumentException("the nonce of " + algorithm + " must be exactly " + nonceBytes()
                        + " bytes long, " + nonce.length + " given");
            }
        }

        private AlgorithmParameterSpec nonceSpec(AeadAlgorithm algorithm, byte[] nonce)
        {
            return algorithm.secretKeyAlgorithm() == SecretKeyAlgorithm.AES
                    ? new GCMParameterSpec(AeadAlgorithm.TAG_BYTES * Byte.SIZE, nonce)
                    : new IvParameterSpec(nonce);
        }

        /**
         * Returns the key the cipher sees, which for XChaCha20-Poly1305 is
         * HChaCha20 of the key and the first 16 bytes of the nonce.
         */
        private SecretKey cipherKey(byte[] nonce)
        {
            if (!algorithm.usesHChaCha20())
            {
                return key;
            }
            return new SecretKeySpec(hChaCha20(encodedKey(key), Arrays.copyOfRange(nonce, 0, 16)),
                    algorithm.secretKeyAlgorithm().jcaName());
        }

        /**
         * Returns the nonce the cipher sees, which for XChaCha20-Poly1305 is 4
         * zero bytes followed by the last 8 bytes of the nonce.
         */
        private byte[] cipherNonce(byte[] nonce)
        {
            return algorithm.usesHChaCha20()
                    ? utils.concat(new byte[4], Arrays.copyOfRange(nonce, 16, nonce.length))
                    : nonce;
        }

    }

    /**
     * The HChaCha20 function of draft-irtf-cfrg-xchacha, which is the ChaCha20
     * permutation of 20 rounds without the block counter and without adding
     * the original state, the way XChaCha20 turns a 24 bytes nonce into the
     * 12 bytes one that ChaCha20-Poly1305 takes.
     *
     * @param key the 32 key bytes, not null
     * @param nonce the 16 nonce bytes, not null
     * @return the 32 bytes of the derived key
     */
    private static byte[] hChaCha20(byte[] key, byte[] nonce)
    {
        if (key.length != 32)
        {
            throw new IllegalArgumentException("HChaCha20 needs a 32 bytes key, " + key.length + " given");
        }
        if (nonce.length != 16)
        {
            throw new IllegalArgumentException("HChaCha20 needs a 16 bytes nonce, " + nonce.length + " given");
        }
        //the state of ChaCha20, which starts with "expand 32-byte k"
        int[] state = new int[]{0x61707865, 0x3320646e, 0x79622d32, 0x6b206574,
            littleEndian(key, 0), littleEndian(key, 4), littleEndian(key, 8), littleEndian(key, 12),
            littleEndian(key, 16), littleEndian(key, 20), littleEndian(key, 24), littleEndian(key, 28),
            littleEndian(nonce, 0), littleEndian(nonce, 4), littleEndian(nonce, 8), littleEndian(nonce, 12)};
        chacha20Rounds(state);
        //the first and the last four words of the state, the words in between
        //are the block counter that HChaCha20 does not need
        byte[] derived = new byte[32];
        for (int word = 0; word < 4; word++)
        {
            littleEndian(derived, word * 4, state[word]);
            littleEndian(derived, 16 + word * 4, state[word + 12]);
        }
        return derived;
    }

    private static int littleEndian(byte[] bytes, int from)
    {
        return (bytes[from] & 0xff) | ((bytes[from + 1] & 0xff) << 8)
                | ((bytes[from + 2] & 0xff) << 16) | (bytes[from + 3] << 24);
    }

    private static void littleEndian(byte[] bytes, int from, int value)
    {
        bytes[from] = (byte) value;
        bytes[from + 1] = (byte) (value >>> 8);
        bytes[from + 2] = (byte) (value >>> 16);
        bytes[from + 3] = (byte) (value >>> 24);
    }

    private static void quarterRound(int[] state, int a, int b, int c, int d)
    {
        state[a] += state[b];
        state[d] = Integer.rotateLeft(state[d] ^ state[a], 16);
        state[c] += state[d];
        state[b] = Integer.rotateLeft(state[b] ^ state[c], 12);
        state[a] += state[b];
        state[d] = Integer.rotateLeft(state[d] ^ state[a], 8);
        state[c] += state[d];
        state[b] = Integer.rotateLeft(state[b] ^ state[c], 7);
    }

    private static void salsaQuarterRound(int[] state, int a, int b, int c, int d)
    {
        state[b] ^= Integer.rotateLeft(state[a] + state[d], 7);
        state[c] ^= Integer.rotateLeft(state[b] + state[a], 9);
        state[d] ^= Integer.rotateLeft(state[c] + state[b], 13);
        state[a] ^= Integer.rotateLeft(state[d] + state[c], 18);
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Streams /////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Stream ciphers, the streams namespace of {@link Kr}.
     * <p>
     * A stream cipher is symmetric, which means that
     * {@link #xor(StreamAlgorithm, SecretKey, byte[], long, byte[])} with the
     * same key and nonce is its own inverse, and that
     * {@link #keystream(StreamAlgorithm, SecretKey, byte[], long, int)} is
     * what the cipher encrypts a block of zeroes into. The nonce of an
     * algorithm has the size that {@link StreamAlgorithm#nonceBytes()} gives
     * and it must never be used twice with the same key: these ciphers only
     * provide confidentiality, so a repeated nonce leaks the plaintext of
     * both messages.
     * <p>
     * The methods with no block counter start at the block zero, which is
     * what almost every caller wants. The block counter is the way to encrypt
     * a message of more than 256 GB, up to what
     * {@link StreamAlgorithm#has32BitsCounter()} allows.
     */
    public final class Streams
    {
        /**
         * Size of the block that all these ciphers work with, in bytes.
         */
        private static final int BLOCK_BYTES = 64;

        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Streams()
        {
        }

        /**
         * Encrypts the data with the stream that starts at the block zero of
         * the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param key the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param data the data to encrypt, not null
         * @return the encrypted data, the same length as the given one
         */
        public byte[] xor(StreamAlgorithm algorithm, SecretKey key, byte[] nonce, byte[] data)
        {
            return xor(algorithm, encodedKey(algorithm, key), nonce, 0, data);
        }

        /**
         * Encrypts the data with the stream that starts at the given block
         * counter of the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param key the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param counter the number of the first block of the keystream
         * @param data the data to encrypt, not null
         * @return the encrypted data, the same length as the given one
         */
        public byte[] xor(StreamAlgorithm algorithm, SecretKey key, byte[] nonce, long counter, byte[] data)
        {
            return xor(algorithm, encodedKey(algorithm, key), nonce, counter, data);
        }

        /**
         * Encrypts the data with the stream that starts at the block zero of
         * the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param keyBytes the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param data the data to encrypt, not null
         * @return the encrypted data, the same length as the given one
         */
        public byte[] xor(StreamAlgorithm algorithm, byte[] keyBytes, byte[] nonce, byte[] data)
        {
            return xor(algorithm, keyBytes, nonce, 0, data);
        }

        /**
         * Encrypts the data with the stream that starts at the given block
         * counter of the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param keyBytes the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param counter the number of the first block of the keystream
         * @param data the data to encrypt, not null
         * @return the encrypted data, the same length as the given one
         */
        public byte[] xor(StreamAlgorithm algorithm, byte[] keyBytes, byte[] nonce, long counter, byte[] data)
        {
            require(algorithm, "algorithm");
            require(keyBytes, "keyBytes");
            require(nonce, "nonce");
            require(data, "data");
            check(algorithm, keyBytes, nonce, counter, data.length);
            byte[] cipherKey = cipherKey(algorithm, keyBytes, nonce);
            byte[] cipherNonce = cipherNonce(algorithm, nonce);
            byte[] encrypted = new byte[data.length];
            byte[] block = new byte[BLOCK_BYTES];
            for (int from = 0; from < data.length; from += BLOCK_BYTES)
            {
                int length = Math.min(BLOCK_BYTES, data.length - from);
                keystreamBlock(algorithm, cipherKey, cipherNonce, counter + from / BLOCK_BYTES, block, 0);
                for (int i = 0; i < length; i++)
                {
                    encrypted[from + i] = (byte) (data[from + i] ^ block[i]);
                }
            }
            Arrays.fill(block, (byte) 0);
            return encrypted;
        }

        /**
         * Returns the first bytes of the keystream that starts at the block
         * zero of the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param key the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param bytes how many bytes of the keystream to return
         * @return the keystream bytes
         */
        public byte[] keystream(StreamAlgorithm algorithm, SecretKey key, byte[] nonce, int bytes)
        {
            return keystream(algorithm, encodedKey(algorithm, key), nonce, 0, bytes);
        }

        /**
         * Returns the given bytes of the keystream that starts at the given
         * block counter of the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param key the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param counter the number of the first block of the keystream
         * @param bytes how many bytes of the keystream to return
         * @return the keystream bytes
         */
        public byte[] keystream(StreamAlgorithm algorithm, SecretKey key, byte[] nonce, long counter, int bytes)
        {
            return keystream(algorithm, encodedKey(algorithm, key), nonce, counter, bytes);
        }

        /**
         * Returns the first bytes of the keystream that starts at the block
         * zero of the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param keyBytes the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param bytes how many bytes of the keystream to return
         * @return the keystream bytes
         */
        public byte[] keystream(StreamAlgorithm algorithm, byte[] keyBytes, byte[] nonce, int bytes)
        {
            return keystream(algorithm, keyBytes, nonce, 0, bytes);
        }

        /**
         * Returns the given bytes of the keystream that starts at the given
         * block counter of the given key and nonce.
         *
         * @param algorithm the stream cipher, not null
         * @param keyBytes the 32 key bytes, not null
         * @param nonce the nonce, not null
         * @param counter the number of the first block of the keystream
         * @param bytes how many bytes of the keystream to return
         * @return the keystream bytes
         */
        public byte[] keystream(StreamAlgorithm algorithm, byte[] keyBytes, byte[] nonce, long counter, int bytes)
        {
            require(algorithm, "algorithm");
            require(keyBytes, "keyBytes");
            require(nonce, "nonce");
            if (bytes < 0)
            {
                throw new IllegalArgumentException("the number of keystream bytes cannot be negative, " + bytes + " given");
            }
            check(algorithm, keyBytes, nonce, counter, bytes);
            byte[] cipherKey = cipherKey(algorithm, keyBytes, nonce);
            byte[] cipherNonce = cipherNonce(algorithm, nonce);
            byte[] keystream = new byte[bytes];
            byte[] block = new byte[BLOCK_BYTES];
            for (int from = 0; from < bytes; from += BLOCK_BYTES)
            {
                int length = Math.min(BLOCK_BYTES, bytes - from);
                keystreamBlock(algorithm, cipherKey, cipherNonce, counter + from / BLOCK_BYTES, block, 0);
                System.arraycopy(block, 0, keystream, from, length);
            }
            Arrays.fill(block, (byte) 0);
            return keystream;
        }

        private byte[] encodedKey(StreamAlgorithm algorithm, SecretKey key)
        {
            require(key, "key");
            String keyAlgorithm = key.getAlgorithm();
            if (keyAlgorithm != null && keyAlgorithm.length() > 0
                    && !algorithm.secretKeyAlgorithm().jcaName().equalsIgnoreCase(keyAlgorithm))
            {
                throw new IllegalArgumentException("a " + algorithm + " cipher needs a " + algorithm.secretKeyAlgorithm().jcaName()
                        + " key, a " + keyAlgorithm + " key was given");
            }
            return Kr.encodedKey(key);
        }

        private void check(StreamAlgorithm algorithm, byte[] keyBytes, byte[] nonce, long counter, int bytes)
        {
            if (keyBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("the key of " + algorithm + " must be exactly " + algorithm.keyBytes()
                        + " bytes long, " + keyBytes.length + " given");
            }
            if (nonce.length != algorithm.nonceBytes())
            {
                throw new IllegalArgumentException("the nonce of " + algorithm + " must be exactly " + algorithm.nonceBytes()
                        + " bytes long, " + nonce.length + " given");
            }
            if (counter < 0)
            {
                throw new IllegalArgumentException("the block counter of " + algorithm + " cannot be negative, " + counter + " given");
            }
            if (algorithm.has32BitsCounter() && counter + ((long) bytes + BLOCK_BYTES - 1) / BLOCK_BYTES > 1L << 32)
            {
                throw new CryptoException("the block counter of " + algorithm + " has 32 bits, so it cannot encrypt past the "
                        + (1L << 32) + " block, " + counter + " already used");
            }
        }

        private byte[] cipherKey(StreamAlgorithm algorithm, byte[] keyBytes, byte[] nonce)
        {
            return algorithm.usesHChaCha20() ? hChaCha20(keyBytes, Arrays.copyOfRange(nonce, 0, 16)) : keyBytes;
        }

        private byte[] cipherNonce(StreamAlgorithm algorithm, byte[] nonce)
        {
            if (!algorithm.usesHChaCha20())
            {
                return nonce;
            }
            //the 12 bytes nonce of ChaCha20 is 4 bytes of zero counter followed
            //by the 8 bytes of the 24 bytes nonce that HChaCha20 did not touch
            byte[] cipherNonce = new byte[12];
            System.arraycopy(nonce, 16, cipherNonce, 4, 8);
            return cipherNonce;
        }

        private void keystreamBlock(StreamAlgorithm algorithm, byte[] key, byte[] nonce, long counter, byte[] keystream, int from)
        {
            switch (algorithm)
            {
            case CHACHA20:
            case XCHACHA20:
                chacha20Block(key, nonce, (int) counter, keystream, from);
                break;
            case SALSA20:
                salsa20Block(key, nonce, counter, keystream, from);
                break;
            case XSALSA20:
                salsa20Block(hSalsa20(key, nonce), Arrays.copyOfRange(nonce, 16, 24), counter, keystream, from);
                break;
            default:
                throw new CryptoException(algorithm + " is not a stream cipher");
            }
        }
    }

    /**
     * The 64 bytes block of the ChaCha20 keystream that starts at the given
     * counter, as defined by
     * <a href="https://datatracker.ietf.org/doc/html/rfc8439">RFC 8439</a>.
     *
     * @param key the 32 key bytes, not null
     * @param nonce the 12 nonce bytes, not null
     * @param counter the number of the block, from 0 to 2^32-1
     * @param keystream where to write the 64 bytes, not null
     * @param from where in the keystream to start writing
     */
    private static void chacha20Block(byte[] key, byte[] nonce, int counter, byte[] keystream, int from)
    {
        if (key.length != 32)
        {
            throw new IllegalArgumentException("ChaCha20 needs a 32 bytes key, " + key.length + " given");
        }
        if (nonce.length != 12)
        {
            throw new IllegalArgumentException("ChaCha20 needs a 12 bytes nonce, " + nonce.length + " given");
        }
        int[] state = new int[]{0x61707865, 0x3320646e, 0x79622d32, 0x6b206574,
            littleEndian(key, 0), littleEndian(key, 4), littleEndian(key, 8), littleEndian(key, 12),
            littleEndian(key, 16), littleEndian(key, 20), littleEndian(key, 24), littleEndian(key, 28),
            counter,
            littleEndian(nonce, 0), littleEndian(nonce, 4), littleEndian(nonce, 8)};
        int[] mixed = state.clone();
        chacha20Rounds(mixed);
        //the keystream is the state after the rounds plus the initial state
        for (int word = 0; word < 16; word++)
        {
            littleEndian(keystream, from + word * 4, state[word] + mixed[word]);
        }
    }

    /**
     * The 20 rounds that both ChaCha20 and HChaCha20 apply to a state, which
     * is the four "expand 32-byte k" words, eight key words and four words
     * that are the block counter for ChaCha20 and the nonce for HChaCha20.
     *
     * @param state the 16 words, not null, changed in place
     */
    private static void chacha20Rounds(int[] state)
    {
        for (int round = 0; round < 10; round++)
        {
            quarterRound(state, 0, 4, 8, 12);
            quarterRound(state, 1, 5, 9, 13);
            quarterRound(state, 2, 6, 10, 14);
            quarterRound(state, 3, 7, 11, 15);
            quarterRound(state, 0, 5, 10, 15);
            quarterRound(state, 1, 6, 11, 12);
            quarterRound(state, 2, 7, 8, 13);
            quarterRound(state, 3, 4, 9, 14);
        }
    }

    /**
     * The 64 bytes block of the Salsa20 keystream that starts at the given
     * counter. Its state has the four "expand 32-byte k" words at the words
     * 0, 5, 10 and 15, the eight key words at the ones in between, the 8
     * nonce bytes at the words 6 and 7 and the 64 bits counter at the words
     * 8 and 9, which is the layout that the Salsa20 rounds work on.
     *
     * @param key the 32 key bytes, not null
     * @param nonce the 8 nonce bytes, not null
     * @param counter the number of the block
     * @param keystream where to write the 64 bytes, not null
     * @param from where in the keystream to start writing
     */
    private static void salsa20Block(byte[] key, byte[] nonce, long counter, byte[] keystream, int from)
    {
        if (key.length != 32)
        {
            throw new IllegalArgumentException("Salsa20 needs a 32 bytes key, " + key.length + " given");
        }
        if (nonce.length != 8)
        {
            throw new IllegalArgumentException("Salsa20 needs an 8 bytes nonce, " + nonce.length + " given");
        }
        int[] state = new int[]{0x61707865, littleEndian(key, 0), littleEndian(key, 4), littleEndian(key, 8),
            littleEndian(key, 12), 0x3320646e, littleEndian(nonce, 0), littleEndian(nonce, 4),
            (int) counter, (int) (counter >>> 32), 0x79622d32, littleEndian(key, 16),
            littleEndian(key, 20), littleEndian(key, 24), littleEndian(key, 28), 0x6b206574};
        int[] mixed = state.clone();
        salsa20Rounds(mixed);
        //the keystream is the state after the rounds plus the initial state
        for (int word = 0; word < 16; word++)
        {
            littleEndian(keystream, from + word * 4, state[word] + mixed[word]);
        }
    }

    /**
     * The 20 rounds that both Salsa20 and HSalsa20 apply to a state, which is
     * the four "expand 32-byte k" words, eight key words and four words that
     * are the nonce for Salsa20 and the 16 bytes input for HSalsa20.
     *
     * @param state the 16 words, not null, changed in place
     */
    private static void salsa20Rounds(int[] state)
    {
        for (int round = 0; round < 10; round++)
        {
            salsaQuarterRound(state, 0, 4, 8, 12);
            salsaQuarterRound(state, 5, 9, 13, 1);
            salsaQuarterRound(state, 10, 14, 2, 6);
            salsaQuarterRound(state, 15, 3, 7, 11);
            salsaQuarterRound(state, 0, 1, 2, 3);
            salsaQuarterRound(state, 5, 6, 7, 4);
            salsaQuarterRound(state, 10, 11, 8, 9);
            salsaQuarterRound(state, 15, 12, 13, 14);
        }
    }

    /**
     * The 32 bytes subkey that HSalsa20 derives from a key and the first 16
     * bytes of a 24 bytes nonce, which is what XSalsa20 then runs Salsa20
     * with.
     *
     * @param key the 32 key bytes, not null
     * @param nonce the 24 nonce bytes, not null
     * @return the 32 bytes of the derived key
     */
    private static byte[] hSalsa20(byte[] key, byte[] nonce)
    {
        if (key.length != 32)
        {
            throw new IllegalArgumentException("HSalsa20 needs a 32 bytes key, " + key.length + " given");
        }
        if (nonce.length != 24)
        {
            throw new IllegalArgumentException("HSalsa20 needs a 24 bytes nonce, " + nonce.length + " given");
        }
        int[] state = new int[]{0x61707865, littleEndian(key, 0), littleEndian(key, 4), littleEndian(key, 8),
            littleEndian(key, 12), 0x3320646e, littleEndian(nonce, 0), littleEndian(nonce, 4),
            littleEndian(nonce, 8), littleEndian(nonce, 12), 0x79622d32, littleEndian(key, 16),
            littleEndian(key, 20), littleEndian(key, 24), littleEndian(key, 28), 0x6b206574};
        salsa20Rounds(state);
        //HSalsa20 has no block counter, so unlike Salsa20 it does not add the
        //initial state to the final one, and it returns the same eight words
        //that HChaCha20 does, in the order that its core expects
        byte[] derived = new byte[32];
        littleEndian(derived, 0, state[0]);
        littleEndian(derived, 4, state[5]);
        littleEndian(derived, 8, state[10]);
        littleEndian(derived, 12, state[15]);
        littleEndian(derived, 16, state[6]);
        littleEndian(derived, 20, state[7]);
        littleEndian(derived, 24, state[8]);
        littleEndian(derived, 28, state[9]);
        return derived;
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Block ciphers ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Block ciphers, the ciphers namespace of {@link Kr}.
     * <p>
     * These encrypt without authenticating, so {@link Aeads} is the better
     * choice whenever the data may be tampered with on its way.
     */
    public final class Ciphers
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Ciphers()
        {
        }

        /**
         * Returns a cipher for the given algorithm and key, checking that
         * the key is of the kind and size the algorithm needs.
         *
         * @param algorithm the cipher, not null
         * @param key the key, not null
         * @return the cipher, ready to encrypt and decrypt
         */
        public BlockCipher of(CipherAlgorithm algorithm, SecretKey key)
        {
            require(algorithm, "algorithm");
            require(key, "key");
            byte[] keyBytes = encodedKey(key);
            if (keyBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("a " + algorithm + " key must be exactly " + algorithm.keyBytes()
                        + " bytes long, " + keyBytes.length + " given");
            }
            String keyAlgorithm = key.getAlgorithm();
            if (keyAlgorithm != null && !algorithm.secretKeyAlgorithm().jcaName().equalsIgnoreCase(keyAlgorithm))
            {
                throw new IllegalArgumentException("a " + algorithm + " cipher needs a "
                        + algorithm.secretKeyAlgorithm().jcaName() + " key, a " + keyAlgorithm + " key was given");
            }
            if (!supports(algorithm))
            {
                throw new UnsupportedAlgorithmException("this backend cannot do " + algorithm);
            }
            return new BlockCipher(algorithm, key);
        }

        /**
         * Returns a cipher for the given algorithm and key bytes.
         *
         * @param algorithm the cipher, not null
         * @param keyBytes the key bytes, not null
         * @return the cipher, ready to encrypt and decrypt
         */
        public BlockCipher of(CipherAlgorithm algorithm, byte[] keyBytes)
        {
            return of(algorithm, keys.secretKey(algorithm, keyBytes));
        }

        /**
         * Returns an AES-CTR cipher for the given key, choosing 128, 192 or
         * 256 bits from its length.
         *
         * @param key the key, not null
         * @return the cipher, ready to encrypt and decrypt
         */
        public BlockCipher aesCtr(SecretKey key)
        {
            require(key, "key");
            return of(CipherAlgorithm.aesCtr(encodedKey(key).length * Byte.SIZE), key);
        }

        /**
         * Returns an AES-CTR cipher for the given key bytes, choosing 128,
         * 192 or 256 bits from their length.
         *
         * @param keyBytes the key bytes, not null
         * @return the cipher, ready to encrypt and decrypt
         */
        public BlockCipher aesCtr(byte[] keyBytes)
        {
            require(keyBytes, "keyBytes");
            return of(CipherAlgorithm.aesCtr(keyBytes.length * Byte.SIZE), keyBytes);
        }

        /**
         * Returns an AES-CBC cipher for the given key, choosing 128, 192 or
         * 256 bits from its length.
         *
         * @param key the key, not null
         * @return the cipher, ready to encrypt and decrypt
         */
        public BlockCipher aesCbc(SecretKey key)
        {
            require(key, "key");
            return of(CipherAlgorithm.aesCbc(encodedKey(key).length * Byte.SIZE), key);
        }

        /**
         * Returns an AES-CBC cipher for the given key bytes, choosing 128,
         * 192 or 256 bits from their length.
         *
         * @param keyBytes the key bytes, not null
         * @return the cipher, ready to encrypt and decrypt
         */
        public BlockCipher aesCbc(byte[] keyBytes)
        {
            require(keyBytes, "keyBytes");
            return of(CipherAlgorithm.aesCbc(keyBytes.length * Byte.SIZE), keyBytes);
        }
    }

    /**
     * A block cipher for one key, which encrypts and decrypts without
     * authenticating, so the ciphertext of {@link #encrypt(byte[])} can be
     * changed without notice.
     * <p>
     * Every message needs an initialization vector that is not repeated with
     * the same key, which is why it travels in front of the ciphertext.
     */
    public final class BlockCipher
    {
        private final CipherAlgorithm algorithm;
        private final SecretKey key;

        BlockCipher(CipherAlgorithm algorithm, SecretKey key)
        {
            this.algorithm = algorithm;
            this.key = key;
        }

        /**
         * Returns the algorithm of this cipher.
         *
         * @return the cipher algorithm
         */
        public CipherAlgorithm algorithm()
        {
            return algorithm;
        }

        /**
         * Returns the size of the initialization vector this cipher needs,
         * which is the size of the AES block.
         *
         * @return the initialization vector size in bytes
         */
        public int ivBytes()
        {
            return algorithm.ivBytes();
        }

        /**
         * Encrypts the plaintext with a fresh random initialization vector,
         * which is the one of {@link Aead#seal(byte[])} as well, random
         * because it must never be repeated with this key.
         *
         * @param plaintext the data to encrypt, not null
         * @return the initialization vector and the ciphertext
         */
        public byte[] encrypt(byte[] plaintext)
        {
            return encryptWithIv(random.bytes(ivBytes()), plaintext);
        }

        /**
         * Encrypts the plaintext with the given initialization vector, which
         * the caller must never repeat with this key.
         *
         * @param iv the initialization vector, {@link #ivBytes()} bytes, not null
         * @param plaintext the data to encrypt, not null
         * @return the initialization vector and the ciphertext
         */
        public byte[] encryptWithIv(byte[] iv, byte[] plaintext)
        {
            require(iv, "iv");
            require(plaintext, "plaintext");
            checkIv(iv);
            byte[] ciphertext = doFinal(Cipher.ENCRYPT_MODE, iv, plaintext);
            return utils.concat(iv, ciphertext);
        }

        /**
         * Decrypts the data encrypted by {@link #encrypt(byte[])}, taking the
         * initialization vector from the data itself.
         *
         * @param encrypted the initialization vector and the ciphertext, not null
         * @return the plaintext
         */
        public byte[] decrypt(byte[] encrypted)
        {
            require(encrypted, "encrypted");
            if (encrypted.length < ivBytes())
            {
                throw new CryptoException("encrypted data is too short to be a valid " + algorithm + " envelope");
            }
            return doFinal(Cipher.DECRYPT_MODE, Arrays.copyOfRange(encrypted, 0, ivBytes()),
                    Arrays.copyOfRange(encrypted, ivBytes(), encrypted.length));
        }

        private byte[] doFinal(int mode, byte[] iv, byte[] data)
        {
            checkIv(iv);
            try
            {
                Cipher cipher = cipher(algorithm.transformation());
                cipher.init(mode, key, new IvParameterSpec(iv));
                return cipher.doFinal(data);
            }
            catch (BadPaddingException ex)
            {
                throw new CryptoException("the data cannot be decrypted with " + algorithm
                        + ": the padding is not valid, so the data is corrupted or the key is wrong", ex);
            }
            catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException ex)
            {
                throw new CryptoException("the data cannot be processed with " + algorithm, ex);
            }
        }

        private void checkIv(byte[] iv)
        {
            if (iv.length != ivBytes())
            {
                throw new IllegalArgumentException("the initialization vector of " + algorithm + " must be exactly "
                        + ivBytes() + " bytes long, " + iv.length + " given");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Key wrapping ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Key wrapping, the wraps namespace of {@link Kr}.
     * <p>
     * Wrapping is for keys what encryption is for data: the wrapped key
     * carries its own integrity check, so a wrong key encryption key or a
     * wrapped key that has been tampered with is detected instead of giving
     * back wrong bytes.
     */
    public final class Wraps
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Wraps()
        {
        }

        /**
         * Wraps key data with a key encryption key, which is AES key wrap
         * and has no nonce, so the same input always gives the same output.
         *
         * @param algorithm the key wrap, not null
         * @param kek the key encryption key, not null
         * @param keyBytes the key data to wrap, not null
         * @return the wrapped key data
         */
        public byte[] wrap(KeyWrapAlgorithm algorithm, SecretKey kek, byte[] keyBytes)
        {
            require(algorithm, "algorithm");
            require(kek, "kek");
            require(keyBytes, "keyBytes");
            if (algorithm.usesRsaKey())
            {
                throw new IllegalArgumentException(algorithm + " wraps with an RSA public key, not with a key encryption key");
            }
            checkAesKek(algorithm, kek);
            checkKeyData(algorithm, keyBytes);
            if (!supports(algorithm))
            {
                throw new UnsupportedAlgorithmException("this backend cannot do " + algorithm);
            }
            try
            {
                Cipher cipher = cipher(algorithm.transformation());
                cipher.init(Cipher.WRAP_MODE, kek);
                return cipher.wrap(new SecretKeySpec(keyBytes.clone(), algorithm.secretKeyAlgorithm().jcaName()));
            }
            catch (GeneralSecurityException ex)
            {
                throw new CryptoException("the key data cannot be wrapped with " + algorithm, ex);
            }
        }

        /**
         * Unwraps key data with a key encryption key, checking the integrity
         * of the wrapped key data.
         *
         * @param algorithm the key wrap, not null
         * @param kek the key encryption key, not null
         * @param wrapped the wrapped key data, not null
         * @return the key data
         * @throws CryptoException when the wrapped key data has been tampered
         * with or the key encryption key is not the right one
         */
        public byte[] unwrap(KeyWrapAlgorithm algorithm, SecretKey kek, byte[] wrapped)
        {
            require(algorithm, "algorithm");
            require(kek, "kek");
            require(wrapped, "wrapped");
            if (algorithm.usesRsaKey())
            {
                throw new IllegalArgumentException(algorithm + " unwraps with an RSA private key, not with a key encryption key");
            }
            checkAesKek(algorithm, kek);
            checkWrapped(algorithm, wrapped);
            if (!supports(algorithm))
            {
                throw new UnsupportedAlgorithmException("this backend cannot do " + algorithm);
            }
            try
            {
                Cipher cipher = cipher(algorithm.transformation());
                cipher.init(Cipher.UNWRAP_MODE, kek);
                return encodedKey((SecretKey) cipher.unwrap(wrapped, algorithm.secretKeyAlgorithm().jcaName(), Cipher.SECRET_KEY));
            }
            catch (GeneralSecurityException ex)
            {
                throw new CryptoException("the wrapped key data does not match: it is corrupted, has been tampered with, "
                        + "or the key encryption key is wrong", ex);
            }
        }

        /**
         * Wraps key data with the public key of an RSA key pair, which is the
         * RSA key wrap of
         * <a href="https://datatracker.ietf.org/doc/html/rfc3394">RFC 3394</a>:
         * the initial value, the length of the key data and the key data go
         * through RSAES-PKCS1-v1_5 encryption, whose padding is random, so the
         * same input does not always give the same output.
         *
         * @param key the RSA public key, not null
         * @param keyBytes the key data to wrap, not null
         * @return the wrapped key data, as long as the modulus
         */
        public byte[] wrap(KeyWrapAlgorithm algorithm, PublicKey key, byte[] keyBytes)
        {
            require(algorithm, "algorithm");
            require(key, "key");
            require(keyBytes, "keyBytes");
            if (!algorithm.usesRsaKey())
            {
                throw new IllegalArgumentException(algorithm + " wraps with a key encryption key, not with an RSA public key");
            }
            RSAPublicKey rsa = rsaPublicKey(key);
            int modulusBytes = (rsa.getModulus().bitLength() + 7) / Byte.SIZE;
            //A = A6A6A6A6A6A6A6A6 followed by the length of the key data
            byte[] header = utils.concat(rfc3394InitialValue(), lengthBytes(keyBytes.length));
            byte[] data = utils.concat(header, keyBytes);
            //the data goes in the RSAES-PKCS1-v1_5 encrypted block, which also
            //takes three bytes of header and at least eight of padding
            if (data.length > modulusBytes - 11)
            {
                throw new IllegalArgumentException("a key of " + keyBytes.length + " bytes does not fit in a "
                        + modulusBytes + " bytes RSA key wrap");
            }
            byte[] block = utils.concat(new byte[] { 0, 2 }, nonZeroBytes(modulusBytes - data.length - 3),
                    new byte[] { 0 }, data);
            BigInteger value = new BigInteger(1, block);
            if (value.compareTo(rsa.getModulus()) >= 0)
            {
                throw new CryptoException("this RSA key cannot wrap " + keyBytes.length + " bytes of key data");
            }
            return leftPad(value.modPow(rsa.getPublicExponent(), rsa.getModulus()).toByteArray(), modulusBytes);
        }

        /**
         * Unwraps key data with the private key of an RSA key pair, checking
         * the integrity of the wrapped key data.
         *
         * @param key the RSA private key, not null
         * @param wrapped the wrapped key data, not null
         * @return the key data
         * @throws CryptoException when the wrapped key data has been tampered
         * with or the private key is not the right one
         */
        public byte[] unwrap(KeyWrapAlgorithm algorithm, PrivateKey key, byte[] wrapped)
        {
            require(algorithm, "algorithm");
            require(key, "key");
            require(wrapped, "wrapped");
            if (!algorithm.usesRsaKey())
            {
                throw new IllegalArgumentException(algorithm + " unwraps with a key encryption key, not with an RSA private key");
            }
            RSAPrivateKey rsa = rsaPrivateKey(key);
            int modulusBytes = (rsa.getModulus().bitLength() + 7) / Byte.SIZE;
            if (wrapped.length != modulusBytes)
            {
                throw new CryptoException("an RSA key wrap of a " + modulusBytes + " bytes key is exactly "
                        + modulusBytes + " bytes long, " + wrapped.length + " given");
            }
            byte[] block = leftPad(new BigInteger(1, wrapped).modPow(rsa.getPrivateExponent(), rsa.getModulus()).toByteArray(),
                    modulusBytes);
            //the padding of RSAES-PKCS1-v1_5: 0x00, 0x02, at least eight random non zero bytes,
            //0x00 and the data
            int separator = 2;
            while (separator < block.length && block[separator] != 0)
            {
                separator++;
            }
            if (block[0] != 0 || block[1] != 2 || separator == block.length || separator == block.length - 1
                    || separator < 10)
            {
                throw new CryptoException("the wrapped key data does not match: it is corrupted, has been tampered with, "
                        + "or the private key is wrong");
            }
            byte[] data = Arrays.copyOfRange(block, separator + 1, block.length);
            byte[] initialValue = rfc3394InitialValue();
            if (data.length < initialValue.length + 4 || !Arrays.equals(initialValue,
                    Arrays.copyOfRange(data, 0, initialValue.length))
                    || dataLength(data, initialValue.length) != data.length - initialValue.length - 4)
            {
                throw new CryptoException("the wrapped key data does not match: it is corrupted, has been tampered with, "
                        + "or the private key is wrong");
            }
            return Arrays.copyOfRange(data, initialValue.length + 4, data.length);
        }

        /**
         * Left pads a big integer with zeroes to the size of the modulus,
         * which is what the modular arithmetic leaves out.
         */
        private byte[] leftPad(byte[] bytes, int length)
        {
            if (bytes.length > length)
            {
                return bytes.length == length + 1 && bytes[0] == 0 ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
            }
            return utils.concat(new byte[length - bytes.length], bytes);
        }

        /**
         * The initial value of the key wrap algorithm of RFC 3394, which is
         * also the integrity check of the key data.
         */
        private byte[] rfc3394InitialValue()
        {
            return new byte[] { (byte) 0xA6, (byte) 0xA6, (byte) 0xA6, (byte) 0xA6,
                (byte) 0xA6, (byte) 0xA6, (byte) 0xA6, (byte) 0xA6 };
        }

        /**
         * The length of the key data as four bytes, most significant byte first.
         */
        private byte[] lengthBytes(int length)
        {
            return new byte[] { (byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8), (byte) length };
        }

        /**
         * The padding of RSAES-PKCS1-v1_5 is at least eight non zero bytes.
         */
        private byte[] nonZeroBytes(int length)
        {
            byte[] bytes = random.bytes(length);
            for (int i = 0; i < bytes.length; i++)
            {
                while (bytes[i] == 0)
                {
                    bytes[i] = (byte) (1 + random.nextInt(255));
                }
            }
            return bytes;
        }

        private void checkAesKek(KeyWrapAlgorithm algorithm, SecretKey kek)
        {
            byte[] kekBytes = encodedKey(kek);
            if (kekBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("the key encryption key of " + algorithm + " must be exactly "
                        + algorithm.keyBytes() + " bytes long, " + kekBytes.length + " given");
            }
        }

        private void checkKeyData(KeyWrapAlgorithm algorithm, byte[] keyBytes)
        {
            if (keyBytes.length < 16 || keyBytes.length % 8 != 0)
            {
                throw new IllegalArgumentException("the key data of " + algorithm + " must be at least 16 bytes long and "
                        + "a whole number of 8 bytes blocks, " + keyBytes.length + " given");
            }
        }

        private void checkWrapped(KeyWrapAlgorithm algorithm, byte[] wrapped)
        {
            if (wrapped.length < 24 || wrapped.length % 8 != 0)
            {
                throw new IllegalArgumentException("the wrapped key data of " + algorithm + " must be at least 24 bytes long and "
                        + "a whole number of 8 bytes blocks, " + wrapped.length + " given");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// KDF ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Key derivation, the kdf namespace of {@link Kr}.
     * <p>
     * HKDF for keys that come from another key, PBKDF2 for keys that come
     * from a password and ECDH plus HKDF for keys shared by two parties.
     */
    public final class Kdf
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Kdf()
        {
        }

        /**
         * Maximum number of blocks of output of HKDF, fixed by
         * <a href="https://datatracker.ietf.org/doc/html/rfc5869">RFC 5869</a>.
         */
        public static final int HKDF_MAX_BLOCKS = 255;

        /**
         * Derives key material with HKDF, as defined by
         * <a href="https://datatracker.ietf.org/doc/html/rfc5869">RFC 5869</a>.
         *
         * @param algorithm the hkdf algorithm, not null
         * @param inputKeyMaterial the secret to derive from, not null
         * @param salt the salt, recommended and may be null, which means a
         * block of zeros
         * @param info context and application specific information, may be
         * null
         * @param outputBytes how many bytes to derive, at most
         * {@link #HKDF_MAX_BLOCKS} times the hash size
         * @return the derived key material
         */
        public byte[] hkdf(HkdfAlgorithm algorithm, byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputBytes)
        {
            require(algorithm, "algorithm");
            require(inputKeyMaterial, "inputKeyMaterial");
            int maxOutputBytes = algorithm.hashBytes() * HKDF_MAX_BLOCKS;
            if (outputBytes <= 0 || outputBytes > maxOutputBytes)
            {
                throw new IllegalArgumentException("outputBytes must be between 1 and " + maxOutputBytes + " for " + algorithm + ", " + outputBytes + " given");
            }
            //RFC 5869: an absent salt is a block of zeros of hash length
            byte[] zeros = salt == null || salt.length == 0 ? new byte[algorithm.hashBytes()] : null;
            byte[] prk;
            try
            {
                prk = hmac.hmac(algorithm.hmacAlgorithm(), zeros == null ? salt : zeros, inputKeyMaterial);
            }
            finally
            {
                if (zeros != null)
                {
                    Arrays.fill(zeros, (byte) 0);
                }
            }
            try
            {
                return hkdfExpand(algorithm, prk, info, outputBytes);
            }
            finally
            {
                Arrays.fill(prk, (byte) 0);
            }
        }

        /**
         * Derives key material with HKDF without salt nor context.
         *
         * @param algorithm the hkdf algorithm, not null
         * @param inputKeyMaterial the secret to derive from, not null
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] hkdf(HkdfAlgorithm algorithm, byte[] inputKeyMaterial, int outputBytes)
        {
            return hkdf(algorithm, inputKeyMaterial, null, null, outputBytes);
        }

        /**
         * Derives a key with the size of the given cipher, so the key cannot
         * come out too short.
         *
         * @param aeadAlgorithm the cipher the key is for, not null
         * @param inputKeyMaterial the secret to derive from, not null
         * @param salt the salt, may be null
         * @param info context and application specific information, may be
         * null
         * @return the derived key
         */
        public SecretKey hkdfKey(AeadAlgorithm aeadAlgorithm, byte[] inputKeyMaterial, byte[] salt, byte[] info)
        {
            require(aeadAlgorithm, "aeadAlgorithm");
            return keys.secretKey(aeadAlgorithm, hkdf(HkdfAlgorithm.SHA256, inputKeyMaterial, salt, info, aeadAlgorithm.keyBytes()));
        }

        /**
         * Derives 32 bytes of key material with HKDF-SHA256.
         *
         * @param inputKeyMaterial the secret to derive from, not null
         * @param salt the salt, may be null
         * @param info context and application specific information, may be
         * null
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] hkdf256(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputBytes)
        {
            return hkdf(HkdfAlgorithm.SHA256, inputKeyMaterial, salt, info, outputBytes);
        }

        /**
         * Derives 48 bytes of key material with HKDF-SHA384.
         *
         * @param inputKeyMaterial the secret to derive from, not null
         * @param salt the salt, may be null
         * @param info context and application specific information, may be
         * null
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] hkdf384(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputBytes)
        {
            return hkdf(HkdfAlgorithm.SHA384, inputKeyMaterial, salt, info, outputBytes);
        }

        /**
         * Derives 64 bytes of key material with HKDF-SHA512.
         *
         * @param inputKeyMaterial the secret to derive from, not null
         * @param salt the salt, may be null
         * @param info context and application specific information, may be
         * null
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] hkdf512(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputBytes)
        {
            return hkdf(HkdfAlgorithm.SHA512, inputKeyMaterial, salt, info, outputBytes);
        }

        private byte[] hkdfExpand(HkdfAlgorithm algorithm, byte[] prk, byte[] info, int outputBytes)
        {
            int hashBytes = algorithm.hashBytes();
            byte[] output = new byte[outputBytes];
            byte[] block = null;
            int offset = 0;
            try
            {
                for (int i = 1; offset < outputBytes; i++)
                {
                    block = hmac.hmac(algorithm.hmacAlgorithm(), prk,
                            block == null ? new byte[0] : block,
                            info == null ? new byte[0] : info,
                            new byte[]{(byte) i});
                    int length = Math.min(hashBytes, outputBytes - offset);
                    System.arraycopy(block, 0, output, offset, length);
                    offset += length;
                }
                return output;
            }
            finally
            {
                if (block != null)
                {
                    Arrays.fill(block, (byte) 0);
                }
            }
        }

        /**
         * Derives key material from a password with PBKDF2, as defined by
         * <a href="https://datatracker.ietf.org/doc/html/rfc8018">RFC 8018</a>.
         * <p>
         * The password is encoded as UTF-8. The salt and the number of
         * iterations must be stored together with the derived key, otherwise
         * the password cannot be checked again.
         *
         * @param algorithm the pbkdf2 algorithm, not null
         * @param password the password, not null
         * @param salt the salt, at least {@link Pbkdf2Algorithm#MINIMUM_SALT_BYTES}
         * bytes, not null
         * @param iterations the number of iterations, at least
         * {@link Pbkdf2Algorithm#minimumIterations()}
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         * @throws IllegalArgumentException when the salt or the iterations are
         * too weak, in which case
         * {@link #pbkdf2ForLegacyInterop(Pbkdf2Algorithm, char[], byte[], int, int)}
         * can only be used to read existing data
         */
        public byte[] pbkdf2(Pbkdf2Algorithm algorithm, char[] password, byte[] salt, int iterations, int outputBytes)
        {
            require(algorithm, "algorithm");
            require(password, "password");
            checkPbkdf2Parameters(algorithm, salt, iterations, outputBytes);
            byte[] passwordBytes = utf8(CharBuffer.wrap(password));
            try
            {
                return pbkdf2Unchecked(algorithm, passwordBytes, salt, iterations, outputBytes);
            }
            finally
            {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }

        /**
         * Derives key material from a password with PBKDF2.
         *
         * @param algorithm the pbkdf2 algorithm, not null
         * @param password the UTF-8 bytes of the password, not null
         * @param salt the salt, at least {@link Pbkdf2Algorithm#MINIMUM_SALT_BYTES}
         * bytes, not null
         * @param iterations the number of iterations, at least
         * {@link Pbkdf2Algorithm#minimumIterations()}
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] pbkdf2(Pbkdf2Algorithm algorithm, byte[] password, byte[] salt, int iterations, int outputBytes)
        {
            require(algorithm, "algorithm");
            require(password, "password");
            checkPbkdf2Parameters(algorithm, salt, iterations, outputBytes);
            return pbkdf2Unchecked(algorithm, password, salt, iterations, outputBytes);
        }

        /**
         * Derives a key with the size of the given cipher from a password, so
         * the key cannot come out too short.
         *
         * @param aeadAlgorithm the cipher the key is for, not null
         * @param password the password, not null
         * @param salt the salt, at least {@link Pbkdf2Algorithm#MINIMUM_SALT_BYTES}
         * bytes, not null
         * @param iterations the number of iterations, at least
         * {@link Pbkdf2Algorithm#minimumIterations()}
         * @return the derived key
         */
        public SecretKey pbkdf2Key(AeadAlgorithm aeadAlgorithm, char[] password, byte[] salt, int iterations)
        {
            require(aeadAlgorithm, "aeadAlgorithm");
            return keys.secretKey(aeadAlgorithm, pbkdf2(Pbkdf2Algorithm.HMAC_SHA256, password, salt, iterations, aeadAlgorithm.keyBytes()));
        }

        /**
         * Derives key material from a password with PBKDF2 without checking
         * the strength of the parameters.
         * <p>
         * Only useful to read data that was derived with weaker parameters, for
         * instance to migrate it to
         * {@link #pbkdf2(Pbkdf2Algorithm, char[], byte[], int, int)} or to
         * Argon2. Never use it to protect new data.
         *
         * @param algorithm the pbkdf2 algorithm, not null
         * @param password the password, not null
         * @param salt the salt, not null
         * @param iterations the number of iterations
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] pbkdf2ForLegacyInterop(Pbkdf2Algorithm algorithm, char[] password, byte[] salt, int iterations, int outputBytes)
        {
            require(algorithm, "algorithm");
            require(salt, "salt");
            byte[] passwordBytes = utf8(CharBuffer.wrap(require(password, "password")));
            try
            {
                return pbkdf2Unchecked(algorithm, passwordBytes, salt, iterations, outputBytes);
            }
            finally
            {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }

        /**
         * Derives key material from a password with PBKDF2 without checking
         * the strength of the parameters.
         *
         * @param algorithm the pbkdf2 algorithm, not null
         * @param password the UTF-8 bytes of the password, not null
         * @param salt the salt, not null
         * @param iterations the number of iterations
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] pbkdf2ForLegacyInterop(Pbkdf2Algorithm algorithm, byte[] password, byte[] salt, int iterations, int outputBytes)
        {
            require(algorithm, "algorithm");
            require(salt, "salt");
            return pbkdf2Unchecked(algorithm, require(password, "password"), salt, iterations, outputBytes);
        }

        private void checkPbkdf2Parameters(Pbkdf2Algorithm algorithm, byte[] salt, int iterations, int outputBytes)
        {
            require(algorithm, "algorithm");
            require(salt, "salt");
            if (iterations < algorithm.minimumIterations())
            {
                throw new IllegalArgumentException("at least " + algorithm.minimumIterations() + " iterations are needed for " + algorithm
                        + ", " + iterations + " given, use pbkdf2ForLegacyInterop only to read data derived with weaker parameters");
            }
            if (salt.length < Pbkdf2Algorithm.MINIMUM_SALT_BYTES)
            {
                throw new IllegalArgumentException("the salt must be at least " + Pbkdf2Algorithm.MINIMUM_SALT_BYTES + " bytes long, " + salt.length + " given");
            }
            if (outputBytes <= 0)
            {
                throw new IllegalArgumentException("outputBytes must be greater than zero, " + outputBytes + " given");
            }
        }

        private byte[] pbkdf2Unchecked(Pbkdf2Algorithm algorithm, byte[] password, byte[] salt, int iterations, int outputBytes)
        {
            if (iterations <= 0)
            {
                throw new IllegalArgumentException("iterations must be greater than zero, " + iterations + " given");
            }
            if (outputBytes <= 0)
            {
                throw new IllegalArgumentException("outputBytes must be greater than zero, " + outputBytes + " given");
            }
            HmacAlgorithm hmacAlgorithm = algorithm.hmacAlgorithm();
            int hashBytes = hmacAlgorithm.digestBytes();
            byte[] output = new byte[outputBytes];
            byte[] u = null;
            byte[] block = null;
            int offset = 0;
            try
            {
                for (int i = 1; offset < outputBytes; i++)
                {
                    block = utils.concat(salt, int32Bytes(i));
                    u = hmac.hmac(hmacAlgorithm, password, block);
                    byte[] accumulated = u.clone();
                    for (int iteration = 1; iteration < iterations; iteration++)
                    {
                        u = hmac.hmac(hmacAlgorithm, password, u);
                        for (int j = 0; j < hashBytes; j++)
                        {
                            accumulated[j] ^= u[j];
                        }
                    }
                    int length = Math.min(hashBytes, outputBytes - offset);
                    System.arraycopy(accumulated, 0, output, offset, length);
                    Arrays.fill(accumulated, (byte) 0);
                    offset += length;
                }
                return output;
            }
            finally
            {
                if (u != null)
                {
                    Arrays.fill(u, (byte) 0);
                }
                if (block != null)
                {
                    Arrays.fill(block, (byte) 0);
                }
            }
        }

        private byte[] int32Bytes(int value)
        {
            return new byte[]{
                (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value
            };
        }

        /**
         * Performs an elliptic curve Diffie-Hellman key agreement and returns
         * the shared secret, which is the x coordinate of the resulting point.
         * <p>
         * This raw secret must not be used as a key: feed it to
         * {@link #ecdhHkdf(HkdfAlgorithm, PrivateKey, PublicKey, byte[], byte[], int)}
         * instead.
         *
         * @param privateKey our private key, not null
         * @param publicKey their public key, not null
         * @return the shared secret
         */
        public byte[] ecdh(PrivateKey privateKey, PublicKey publicKey)
        {
            require(privateKey, "privateKey");
            require(publicKey, "publicKey");
            KeyAlgorithm privateKeyAlgorithm = keyAlgorithmOf(privateKey);
            KeyAlgorithm publicKeyAlgorithm = keyAlgorithmOf(publicKey);
            if (privateKeyAlgorithm != publicKeyAlgorithm)
            {
                throw new IllegalArgumentException("a " + privateKeyAlgorithm + " private key cannot agree with a " + publicKeyAlgorithm + " public key");
            }
            String agreement = keyAgreementName(publicKeyAlgorithm);
            KeyAgreement keyAgreement = keyAgreement(agreement);
            try
            {
                keyAgreement.init(privateKey);
                keyAgreement.doPhase(publicKey, true);
                return keyAgreement.generateSecret();
            }
            catch (InvalidKeyException | IllegalStateException ex)
            {
                throw new CryptoException("the keys cannot agree, they may not belong to the same curve", ex);
            }
        }

        /**
         * Derives shared key material with an elliptic curve Diffie-Hellman key
         * agreement followed by HKDF, the usual way to turn an ephemeral key
         * exchange into a key.
         *
         * @param algorithm the hkdf algorithm, not null
         * @param privateKey our private key, not null
         * @param publicKey their public key, not null
         * @param salt the salt, may be null
         * @param info context and application specific information, may be
         * null
         * @param outputBytes how many bytes to derive
         * @return the derived key material
         */
        public byte[] ecdhHkdf(HkdfAlgorithm algorithm, PrivateKey privateKey, PublicKey publicKey, byte[] salt, byte[] info, int outputBytes)
        {
            return hkdf(algorithm, ecdh(privateKey, publicKey), salt, info, outputBytes);
        }

        /**
         * Derives a key with the size of the given cipher with an elliptic curve
         * Diffie-Hellman key agreement followed by HKDF.
         *
         * @param aeadAlgorithm the cipher the key is for, not null
         * @param privateKey our private key, not null
         * @param publicKey their public key, not null
         * @param salt the salt, may be null
         * @param info context and application specific information, may be
         * null
         * @return the derived key
         */
        public SecretKey ecdhHkdfKey(AeadAlgorithm aeadAlgorithm, PrivateKey privateKey, PublicKey publicKey, byte[] salt, byte[] info)
        {
            require(aeadAlgorithm, "aeadAlgorithm");
            return keys.secretKey(aeadAlgorithm, ecdhHkdf(HkdfAlgorithm.SHA256, privateKey, publicKey, salt, info, aeadAlgorithm.keyBytes()));
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Sign ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Digital signatures, the sign namespace of {@link Kr}.
     */
    public final class Sign
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Sign()
        {
        }

        /**
         * Signs the concatenation of the given chunks.
         *
         * @param algorithm the signature algorithm, not null
         * @param privateKey the private key, whose type must match the
         * algorithm, not null
         * @param data the chunks to sign, neither null nor containing nulls
         * @return the signature
         * @throws UnsupportedAlgorithmException when the algorithm is not
         * available
         * @throws CryptoException when the key or the data are invalid
         */
        public byte[] sign(SignatureAlgorithm algorithm, PrivateKey privateKey, byte[]... data)
        {
            Signature signature = signature(algorithm.jcaName());
            try
            {
                signature.initSign(require(privateKey, "privateKey"));
            }
            catch (InvalidKeyException ex)
            {
                throw new CryptoException("the private key cannot be used with " + algorithm, ex);
            }
            updateSignature(signature, data);
            try
            {
                return signature.sign();
            }
            catch (SignatureException ex)
            {
                throw new CryptoException("the data cannot be signed with " + algorithm, ex);
            }
        }

        /**
         * Verifies a signature.
         *
         * @param algorithm the signature algorithm, not null
         * @param publicKey the public key, whose type must match the
         * algorithm, not null
         * @param signatureBytes the signature to verify, not null
         * @param data the chunks that were signed, neither null nor containing
         * nulls
         * @return true when the signature is correct, false otherwise
         * @throws CryptoException when the key or the signature are malformed
         */
        public boolean verify(SignatureAlgorithm algorithm, PublicKey publicKey, byte[] signatureBytes, byte[]... data)
        {
            Signature signature = signature(algorithm.jcaName());
            try
            {
                signature.initVerify(require(publicKey, "publicKey"));
            }
            catch (InvalidKeyException ex)
            {
                throw new CryptoException("the public key cannot be used with " + algorithm, ex);
            }
            updateSignature(signature, data);
            try
            {
                return signature.verify(require(signatureBytes, "signatureBytes"));
            }
            catch (SignatureException ex)
            {
                throw new CryptoException("the signature is malformed", ex);
            }
        }

        /**
         * Signs with Ed25519, which hashes the message itself.
         *
         * @param privateKey the Ed25519 private key, not null
         * @param data the chunks to sign, neither null nor containing nulls
         * @return the 64 bytes signature
         */
        public byte[] signEd25519(PrivateKey privateKey, byte[]... data)
        {
            return sign(SignatureAlgorithm.ED25519, privateKey, data);
        }

        /**
         * Verifies an Ed25519 signature.
         *
         * @param publicKey the Ed25519 public key, not null
         * @param signatureBytes the signature to verify, not null
         * @param data the chunks that were signed, neither null nor containing
         * nulls
         * @return true when the signature is correct, false otherwise
         */
        public boolean verifyEd25519(PublicKey publicKey, byte[] signatureBytes, byte[]... data)
        {
            return verify(SignatureAlgorithm.ED25519, publicKey, signatureBytes, data);
        }

        /**
         * Signs with ECDSA.
         *
         * @param privateKey the EC private key, not null
         * @param data the chunks to sign, neither null nor containing nulls
         * @return the signature
         */
        public byte[] signEcdsaSha256(PrivateKey privateKey, byte[]... data)
        {
            return sign(SignatureAlgorithm.SHA256_WITH_ECDSA, privateKey, data);
        }

        /**
         * Verifies an ECDSA signature.
         *
         * @param publicKey the EC public key, not null
         * @param signatureBytes the signature to verify, not null
         * @param data the chunks that were signed, neither null nor containing
         * nulls
         * @return true when the signature is correct, false otherwise
         */
        public boolean verifyEcdsaSha256(PublicKey publicKey, byte[] signatureBytes, byte[]... data)
        {
            return verify(SignatureAlgorithm.SHA256_WITH_ECDSA, publicKey, signatureBytes, data);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Keys ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Key generation, encoding and decoding, the keys namespace of {@link Kr}.
     * <p>
     * Public keys are encoded as X.509 SubjectPublicKeyInfo and private keys as
     * PKCS#8, the two encodings understood by every other library.
     */
    public final class Keys
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Keys()
        {
        }

        /**
         * Generates a key pair with the recommended strength of the given
         * algorithm: 3072 bits for RSA, the NIST P-256 curve for EC, Ed25519 or
         * X25519.
         *
         * @param algorithm the key algorithm, not null
         * @return a new key pair
         */
        public KeyPair generate(KeyAlgorithm algorithm)
        {
            require(algorithm, "algorithm");
            switch (algorithm)
            {
                case EC:
                    return generateEc(EcCurve.SECP256R1);
                case RSA:
                    return generateRsa(algorithm.defaultKeyBits());
                default:
                    return keyPairGenerator(algorithm.jcaName()).generateKeyPair();
            }
        }

        /**
         * Generates a key pair of the given size.
         *
         * @param algorithm the key algorithm, not null
         * @param keyBits the size in bits: any size of at least
         * {@link #MINIMUM_RSA_BITS} for RSA, the size of a curve for EC, and
         * the fixed size of the algorithm for Ed25519 or X25519
         * @return a new key pair
         */
        public KeyPair generate(KeyAlgorithm algorithm, int keyBits)
        {
            require(algorithm, "algorithm");
            switch (algorithm)
            {
                case RSA:
                    return generateRsa(keyBits);
                case EC:
                    for (EcCurve curve : EcCurve.values())
                    {
                        if (curve.keyBits() == keyBits)
                        {
                            return generateEc(curve);
                        }
                    }
                    throw new IllegalArgumentException("there is no EC curve with " + keyBits + " bits");
                default:
                    if (keyBits != algorithm.defaultKeyBits())
                    {
                        throw new IllegalArgumentException(algorithm + " keys always have " + algorithm.defaultKeyBits() + " bits, " + keyBits + " given");
                    }
                    return generate(algorithm);
            }
        }

        private KeyPair generateRsa(int keyBits)
        {
            if (keyBits < MINIMUM_RSA_BITS)
            {
                throw new IllegalArgumentException("RSA keys must have at least " + MINIMUM_RSA_BITS + " bits, " + keyBits + " given");
            }
            KeyPairGenerator generator = keyPairGenerator(KeyAlgorithm.RSA.jcaName());
            generator.initialize(keyBits);
            return generator.generateKeyPair();
        }

        /**
         * Generates a key pair on the given elliptic curve.
         *
         * @param curve the curve, not null
         * @return a new key pair
         * @throws UnsupportedAlgorithmException when the backend does not know
         * the curve
         */
        public KeyPair generateEc(EcCurve curve)
        {
            require(curve, "curve");
            KeyPairGenerator generator = keyPairGenerator(KeyAlgorithm.EC.jcaName());
            AlgorithmParameterSpec parameters = ecParameters(curve);
            try
            {
                generator.initialize(parameters);
                return generator.generateKeyPair();
            }
            catch (InvalidAlgorithmParameterException | IllegalStateException ex)
            {
                throw unsupported(curve.jcaName(), ex);
            }
        }

        /**
         * Generates an Ed25519 key pair.
         *
         * @return a new key pair
         */
        public KeyPair generateEd25519()
        {
            return generate(KeyAlgorithm.ED25519);
        }

        /**
         * Generates an X25519 key pair for Diffie-Hellman key agreement.
         *
         * @return a new key pair
         */
        public KeyPair generateX25519()
        {
            return generate(KeyAlgorithm.X25519);
        }

        /**
         * Builds a secret key for the given algorithm, checking that the bytes
         * have exactly the expected length.
         *
         * @param algorithm the cipher the key is for, not null
         * @param keyBytes the key bytes, not null
         * @return the secret key
         */
        public SecretKey secretKey(AeadAlgorithm algorithm, byte[] keyBytes)
        {
            require(algorithm, "algorithm");
            require(keyBytes, "keyBytes");
            if (keyBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("a " + algorithm + " key must be exactly " + algorithm.keyBytes() + " bytes long, " + keyBytes.length + " given");
            }
            return new SecretKeySpec(keyBytes.clone(), algorithm.secretKeyAlgorithm().jcaName());
        }

        /**
         * Generates a random secret key of the size the given cipher needs.
         *
         * @param algorithm the cipher the key is for, not null
         * @return the secret key
         */
        public SecretKey generateSecretKey(AeadAlgorithm algorithm)
        {
            require(algorithm, "algorithm");
            return secretKey(algorithm, random.bytes(algorithm.keyBytes()));
        }

        /**
         * Returns the secret key that the given stream cipher takes.
         *
         * @param algorithm the stream cipher the key is for, not null
         * @param keyBytes the key bytes, not null
         * @return the secret key
         */
        public SecretKey secretKey(StreamAlgorithm algorithm, byte[] keyBytes)
        {
            require(algorithm, "algorithm");
            require(keyBytes, "keyBytes");
            if (keyBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("a " + algorithm + " key must be exactly " + algorithm.keyBytes()
                        + " bytes long, " + keyBytes.length + " given");
            }
            return new SecretKeySpec(keyBytes.clone(), algorithm.secretKeyAlgorithm().jcaName());
        }

        /**
         * Generates a random secret key of the size the given stream cipher
         * needs.
         *
         * @param algorithm the stream cipher the key is for, not null
         * @return the secret key
         */
        public SecretKey generateSecretKey(StreamAlgorithm algorithm)
        {
            require(algorithm, "algorithm");
            return secretKey(algorithm, random.bytes(algorithm.keyBytes()));
        }

        /**
         * Returns the secret key that the given block cipher takes.
         *
         * @param algorithm the block cipher the key is for, not null
         * @param keyBytes the key bytes, not null
         * @return the secret key
         */
        public SecretKey secretKey(CipherAlgorithm algorithm, byte[] keyBytes)
        {
            require(algorithm, "algorithm");
            require(keyBytes, "keyBytes");
            if (keyBytes.length != algorithm.keyBytes())
            {
                throw new IllegalArgumentException("a " + algorithm + " key must be exactly " + algorithm.keyBytes()
                        + " bytes long, " + keyBytes.length + " given");
            }
            return new SecretKeySpec(keyBytes.clone(), algorithm.secretKeyAlgorithm().jcaName());
        }

        /**
         * Generates a random secret key of the size the given block cipher
         * needs.
         *
         * @param algorithm the block cipher the key is for, not null
         * @return the secret key
         */
        public SecretKey generateSecretKey(CipherAlgorithm algorithm)
        {
            require(algorithm, "algorithm");
            return secretKey(algorithm, random.bytes(algorithm.keyBytes()));
        }

        /**
         * Encodes a public key in X.509 SubjectPublicKeyInfo format.
         *
         * @param key the key to encode, not null
         * @return the encoded key
         */
        public byte[] encode(PublicKey key)
        {
            return encodeKey(key, "public");
        }

        /**
         * Encodes a private key in PKCS#8 format.
         *
         * @param key the key to encode, not null
         * @return the encoded key
         */
        public byte[] encode(PrivateKey key)
        {
            return encodeKey(key, "private");
        }

        /**
         * Decodes a public key in X.509 SubjectPublicKeyInfo format.
         *
         * @param algorithm the kind of key, not null
         * @param encodedKey the bytes returned by {@link #encode(PublicKey)},
         * not null
         * @return the public key
         * @throws CryptoException when the bytes are not a valid key of this
         * kind
         */
        public PublicKey decodePublic(KeyAlgorithm algorithm, byte[] encodedKey)
        {
            require(algorithm, "algorithm");
            require(encodedKey, "encodedKey");
            try
            {
                return keyFactory(algorithm.jcaName()).generatePublic(new X509EncodedKeySpec(encodedKey));
            }
            catch (InvalidKeySpecException ex)
            {
                throw new CryptoException("the bytes are not a valid X.509 " + algorithm + " public key", ex);
            }
        }

        /**
         * Decodes a private key in PKCS#8 format.
         *
         * @param algorithm the kind of key, not null
         * @param encodedKey the bytes returned by {@link #encode(PrivateKey)},
         * not null
         * @return the private key
         * @throws CryptoException when the bytes are not a valid key of this
         * kind
         */
        public PrivateKey decodePrivate(KeyAlgorithm algorithm, byte[] encodedKey)
        {
            require(algorithm, "algorithm");
            require(encodedKey, "encodedKey");
            try
            {
                return keyFactory(algorithm.jcaName()).generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
            }
            catch (InvalidKeySpecException ex)
            {
                throw new CryptoException("the bytes are not a valid PKCS#8 " + algorithm + " private key", ex);
            }
        }

        /**
         * Decodes a key pair, both keys at once, which is how key pairs travel
         * most of the time.
         *
         * @param algorithm the kind of key, not null
         * @param encodedPrivateKey the bytes returned by
         * {@link #encode(PrivateKey)}, not null
         * @param encodedPublicKey the bytes returned by {@link #encode(PublicKey)},
         * not null
         * @return the key pair
         * @throws CryptoException when the bytes are not a valid key pair of
         * this kind
         */
        public KeyPair decodePair(KeyAlgorithm algorithm, byte[] encodedPrivateKey, byte[] encodedPublicKey)
        {
            return new KeyPair(decodePublic(algorithm, encodedPublicKey), decodePrivate(algorithm, encodedPrivateKey));
        }

        /**
         * Encodes an Ed25519 or X25519 public key as the 32 raw bytes that RFC
         * 7748 defines, without the X.509 header, which is how these keys
         * travel in protocols like SSH, WireGuard or age.
         * <p>
         * There is no method to do the same with a private key on purpose: the
         * JDK and Bouncy Castle do not encode the private key of these
         * algorithms the same way, so the raw bytes cannot always be read back.
         * Private keys travel in PKCS#8, which {@link #decodePair(KeyAlgorithm,
         * byte[], byte[])} reads everywhere.
         *
         * @param key an Ed25519 or X25519 public key, not null
         * @return the raw key bytes
         * @throws IllegalArgumentException when the key is of another kind
         */
        public byte[] encodeRaw(PublicKey key)
        {
            require(key, "key");
            if (rawHeader(keyAlgorithmOf(key), false) == null)
            {
                throw new IllegalArgumentException("only Ed25519 and X25519 public keys have a raw encoding, " + key.getAlgorithm() + " does not");
            }
            //the raw key is the tail of the X.509 encoding, which has no other content
            byte[] encoded = encodeKey(key, "raw");
            if (encoded.length <= RAW_KEY_BYTES)
            {
                throw new CryptoException("this " + key.getAlgorithm() + " public key encoding is too short to hold a " + RAW_KEY_BYTES + " bytes key");
            }
            return Arrays.copyOfRange(encoded, encoded.length - RAW_KEY_BYTES, encoded.length);
        }

        /**
         * Decodes an Ed25519 or X25519 public key from the 32 raw bytes that
         * RFC 7748 and RFC 8410 define.
         *
         * @param algorithm ED25519 or X25519, not null
         * @param rawKey the 32 raw key bytes, not null
         * @return the public key
         * @throws IllegalArgumentException when the algorithm does not have a
         * raw encoding or the bytes are not 32 bytes long
         */
        public PublicKey decodeRawPublic(KeyAlgorithm algorithm, byte[] rawKey)
        {
            require(algorithm, "algorithm");
            byte[] header = rawHeader(algorithm, false);
            return decodePublic(algorithm, rawKeyBytes(rawKey, header, algorithm));
        }

        /**
         * Decodes an Ed25519 or X25519 private key from the 32 raw bytes that
         * RFC 7748 and RFC 8410 define.
         *
         * @param algorithm ED25519 or X25519, not null
         * @param rawKey the 32 raw key bytes, not null
         * @return the private key
         * @throws IllegalArgumentException when the algorithm does not have a
         * raw encoding or the bytes are not 32 bytes long
         */
        public PrivateKey decodeRawPrivate(KeyAlgorithm algorithm, byte[] rawKey)
        {
            require(algorithm, "algorithm");
            byte[] header = rawHeader(algorithm, true);
            return decodePrivate(algorithm, rawKeyBytes(rawKey, header, algorithm));
        }

        /**
         * Decodes an Ed25519 or X25519 key pair from the 32 raw bytes that RFC
         * 7748 and RFC 8410 define.
         *
         * @param algorithm ED25519 or X25519, not null
         * @param rawPrivateKey the 32 raw private key bytes, not null
         * @param rawPublicKey the 32 raw public key bytes, not null
         * @return the key pair
         * @throws IllegalArgumentException when the algorithm does not have a
         * raw encoding or the bytes are not 32 bytes long
         */
        public KeyPair decodeRawPair(KeyAlgorithm algorithm, byte[] rawPrivateKey, byte[] rawPublicKey)
        {
            return new KeyPair(decodeRawPublic(algorithm, rawPublicKey), decodeRawPrivate(algorithm, rawPrivateKey));
        }

        private byte[] rawKeyBytes(byte[] rawKey, byte[] header, KeyAlgorithm algorithm)
        {
            require(rawKey, "rawKey");
            if (header == null)
            {
                throw new IllegalArgumentException("only Ed25519 and X25519 keys have a raw encoding, " + algorithm + " does not");
            }
            if (rawKey.length != RAW_KEY_BYTES)
            {
                throw new IllegalArgumentException("the raw bytes of a " + algorithm + " key are " + RAW_KEY_BYTES
                        + " bytes long, " + rawKey.length + " given");
            }
            byte[] encoded = new byte[header.length + rawKey.length];
            System.arraycopy(header, 0, encoded, 0, header.length);
            System.arraycopy(rawKey, 0, encoded, header.length, rawKey.length);
            return encoded;
        }

        /**
         * The fixed ASN.1 header that wraps a raw Ed25519 or X25519 key, which
         * is what lets the same code work on every Java version and provider:
         * X.509 for public keys and PKCS#8 for private keys.
         *
         * @param algorithm the key algorithm
         * @param privateKey true for the PKCS#8 header, false for the X.509 one
         * @return the header, or null when the algorithm has no raw encoding
         */
        private byte[] rawHeader(KeyAlgorithm algorithm, boolean privateKey)
        {
            switch (algorithm)
            {
                case ED25519:
                    return privateKey
                            ? new byte[]{0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20}
                            : new byte[]{0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00};
                case X25519:
                    return privateKey
                            ? new byte[]{0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x04, 0x22, 0x04, 0x20}
                            : new byte[]{0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00};
                default:
                    return null;
            }
        }

        private byte[] encodeKey(Key key, String kind)
        {
            require(key, "key");
            byte[] encoded = key.getEncoded();
            if (encoded == null)
            {
                throw new CryptoException("this " + kind + " key cannot be encoded, keys that live in a HSM or in a PKCS#11 keystore have no bytes to export");
            }
            return encoded;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Random //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Cryptographically strong random data, the random namespace of
     * {@link Kr}.
     */
    public final class Random
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Random()
        {
        }

        /**
         * Returns random bytes.
         *
         * @param length how many bytes, zero or more
         * @return the random bytes, a new array on every call
         */
        public byte[] bytes(int length)
        {
            if (length < 0)
            {
                throw new IllegalArgumentException("length must not be negative, " + length + " given");
            }
            byte[] bytes = new byte[length];
            secureRandom().nextBytes(bytes);
            return bytes;
        }

        /**
         * Fills an array with random bytes.
         *
         * @param destination the array to fill, not null
         */
        public void fill(byte[] destination)
        {
            require(destination, "destination");
            secureRandom().nextBytes(destination);
        }

        /**
         * Fills a slice of an array with random bytes.
         *
         * @param destination the array to fill, not null
         * @param offset the first byte to fill
         * @param length how many bytes to fill
         */
        public void fill(byte[] destination, int offset, int length)
        {
            require(destination, "destination");
            if (offset < 0 || length < 0 || offset > destination.length - length)
            {
                throw new IndexOutOfBoundsException("offset " + offset + " and length " + length + " do not fit in an array of " + destination.length + " bytes");
            }
            byte[] bytes = bytes(length);
            System.arraycopy(bytes, 0, destination, offset, length);
            Arrays.fill(bytes, (byte) 0);
        }

        /**
         * Returns a random number uniformly distributed between zero
         * (included) and the given bound (excluded), without the bias that the
         * modulo of a random number would introduce.
         *
         * @param bound the exclusive upper bound, greater than zero
         * @return a random number in [0, bound)
         */
        public int nextInt(int bound)
        {
            if (bound <= 0)
            {
                throw new IllegalArgumentException("bound must be greater than zero, " + bound + " given");
            }
            SecureRandom random = secureRandom();
            //keep the lowest bits that fit the bound and reject the values out of range
            int mask = (1 << (Integer.SIZE - Integer.numberOfLeadingZeros(bound - 1))) - 1;
            int value;
            do
            {
                value = random.nextInt() & mask;
            }
            while (value >= bound);
            return value;
        }

        /**
         * Returns 8 random bytes as a long.
         *
         * @return a random long
         */
        public long nextLong()
        {
            byte[] bytes = bytes(Long.BYTES);
            try
            {
                return ByteBuffer.wrap(bytes).getLong();
            }
            finally
            {
                Arrays.fill(bytes, (byte) 0);
            }
        }

        /**
         * Returns the underlying generator, to be used with the JCA directly
         * when something is missing here.
         *
         * @return the secure random generator of this backend, never null
         */
        public SecureRandom secureRandom()
        {
            return Kr.this.secureRandom();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Utils ///////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Small helpers that complete the cryptographic primitives, the utils
     * namespace of {@link Kr}.
     */
    public final class Utils
    {
        /**
         * Only {@link Kr} creates this namespace, which lives as long as
         * the instance that holds it.
         */
        Utils()
        {
        }

        /**
         * Compares two arrays in constant time, so the comparison does not
         * reveal how many leading bytes are equal.
         * <p>
         * Arrays of different length are not equal, which is a leak of no
         * consequence because key and digest sizes are not secret.
         *
         * @param a the first array, not null
         * @param b the second array, not null
         * @return true when both arrays have the same length and the same
         * content
         */
        public boolean timingSafeEql(byte[] a, byte[] b)
        {
            require(a, "a");
            require(b, "b");
            if (a.length != b.length)
            {
                return false;
            }
            int diff = 0;
            for (int i = 0; i < a.length; i++)
            {
                diff |= a[i] ^ b[i];
            }
            return diff == 0;
        }

        /**
         * Overwrites an array with zeroes. Java does not guarantee that the
         * virtual machine does not keep a copy of the bytes, so this reduces
         * but does not eliminate the time they stay in memory.
         *
         * @param bytes the array to wipe, not null
         */
        public void wipe(byte[] bytes)
        {
            Arrays.fill(require(bytes, "bytes"), (byte) 0);
        }

        /**
         * Returns an array of zeroes.
         *
         * @param length how many bytes
         * @return a new array of zeroes
         */
        public byte[] zeroes(int length)
        {
            if (length < 0)
            {
                throw new IllegalArgumentException("length must not be negative, " + length + " given");
            }
            return new byte[length];
        }

        /**
         * Concatenates arrays into a new one.
         *
         * @param parts the arrays to concatenate, neither null nor containing
         * nulls
         * @return the concatenation of all of them
         */
        public byte[] concat(byte[]... parts)
        {
            require(parts, "parts");
            int length = 0;
            for (byte[] part : parts)
            {
                length += require(part, "parts").length;
            }
            byte[] result = new byte[length];
            int offset = 0;
            for (byte[] part : parts)
            {
                System.arraycopy(part, 0, result, offset, part.length);
                offset += part.length;
            }
            return result;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Internal helpers ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private static KeyAlgorithm keyAlgorithmOf(Key key)
    {
        String algorithm = key.getAlgorithm();
        if ("RSA".equalsIgnoreCase(algorithm))
        {
            return KeyAlgorithm.RSA;
        }
        if ("EC".equalsIgnoreCase(algorithm) || "ECDSA".equalsIgnoreCase(algorithm))
        {
            return KeyAlgorithm.EC;
        }
        if ("Ed25519".equalsIgnoreCase(algorithm) || "EdDSA".equalsIgnoreCase(algorithm))
        {
            return KeyAlgorithm.ED25519;
        }
        if ("X25519".equalsIgnoreCase(algorithm) || "XDH".equalsIgnoreCase(algorithm))
        {
            return KeyAlgorithm.X25519;
        }
        throw new IllegalArgumentException("unsupported key algorithm: " + algorithm + ", supported ones are "
                + Arrays.toString(KeyAlgorithm.values()));
    }

    private static String keyAgreementName(KeyAlgorithm algorithm)
    {
        switch (algorithm)
        {
            case EC:
                return "ECDH";
            case X25519:
                return "XDH";
            default:
                throw new IllegalArgumentException(algorithm + " keys cannot be used for key agreement, X25519 or EC keys are needed");
        }
    }

    private static byte[] utf8(CharSequence text)
    {
        return require(text, "text").toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the argument if it is not null and throws a null pointer
     * exception naming the parameter otherwise.
     *
     * @param <T> the type of the value
     * @param value the value to check
     * @param name the name of the parameter, used in the message
     * @return the value
     */
    /**
     * The prime of the field Poly1305 computes in, 2^130 - 5.
     */
    private static final BigInteger POLY1305_PRIME = BigInteger.ONE.shiftLeft(130).subtract(BigInteger.valueOf(5));

    /**
     * The bits of the key that <code>r</code> does not use, which is what the
     * clamping of section 2.5.1 of RFC 8439 clears.
     */
    private static final BigInteger POLY1305_CLAMP = new BigInteger("0ffffffc0ffffffc0ffffffc0fffffff", 16);

    /**
     * One more than the biggest value a Poly1305 tag can hold, so the last sum
     * of section 2.5.2 of RFC 8439 wraps around it.
     */
    private static final BigInteger POLY1305_TAG_MODULUS = BigInteger.ONE.shiftLeft(128);

    /**
     * The length of the key data, which the initial value of the key wrap is
     * followed by, most significant byte first.
     */
    private static int dataLength(byte[] data, int offset)
    {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    private static RSAPublicKey rsaPublicKey(PublicKey key)
    {
        if (key instanceof RSAPublicKey)
        {
            return (RSAPublicKey) key;
        }
        throw new IllegalArgumentException("an RSA key wrap needs an RSA public key, a " + key.getAlgorithm()
                + " key was given");
    }

    private static RSAPrivateKey rsaPrivateKey(PrivateKey key)
    {
        if (key instanceof RSAPrivateKey)
        {
            return (RSAPrivateKey) key;
        }
        throw new IllegalArgumentException("an RSA key wrap needs an RSA private key, a " + key.getAlgorithm()
                + " key was given");
    }

    private static byte[] encodedKey(SecretKey key)
    {
        byte[] encoded = key.getEncoded();
        if (encoded == null)
        {
            throw new CryptoException("this key does not expose its bytes, keys that live in a HSM or in a PKCS#11 keystore cannot be used by this API");
        }
        return encoded;
    }

    protected static <T> T require(T value, String name)
    {
        if (value == null)
        {
            throw new NullPointerException(name + " must not be null");
        }
        return value;
    }

    private static void update(Updater updater, byte[]... data)
    {
        require(data, "data");
        for (byte[] item : data)
        {
            updater.update(require(item, "data"));
        }
    }

    private static void updateSignature(Signature signature, byte[]... data)
    {
        require(data, "data");
        for (byte[] item : data)
        {
            try
            {
                signature.update(require(item, "data"));
            }
            catch (SignatureException ex)
            {
                throw new CryptoException("the data cannot be signed nor verified", ex);
            }
        }
    }

    @FunctionalInterface
    private interface Updater
    {
        void update(byte[] data);
    }

    @FunctionalInterface
    private interface Probe
    {
        Object probe();
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Password hashing ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Password hashing, the namespace of {@link Kr}.
     * <p>
     * A password hash is not a key and must never be used as one: it is slow
     * on purpose and only verifies a password, it cannot decrypt anything.
     * Hash it with {@link #argon2Encoded} and keep the encoded string, then
     * check the password with {@link #verify}.
     * <p>
     * None of these algorithms is part of the JDK, they all need the
     * {@link KrBC} backend.
     */
    public final class Password
    {
        /**
         * Minimum size of the salt in bytes, which is the floor every
         * implementation here accepts. The salt of a new hash should be
         * {@link #RECOMMENDED_SALT_BYTES} long, this minimum only exists so
         * that a hash that already exists can be verified.
         */
        public static final int MINIMUM_SALT_BYTES = 8;

        /**
         * Size of the salt in bytes recommended for a new hash, which is what
         * Argon2 and scrypt ask for and more than enough to break rainbow
         * tables.
         */
        public static final int RECOMMENDED_SALT_BYTES = 16;

        /**
         * Minimum number of lanes that Argon2 can use.
         */
        public static final int MINIMUM_PARALLELISM = 1;

        /**
         * Number of bytes of the smallest Argon2 hash that Argon2 itself
         * accepts.
         */
        public static final int MINIMUM_HASH_BYTES = 4;

        /**
         * Minimum cost factor bcrypt accepts.
         */
        public static final int MINIMUM_BCRYPT_COST = 4;

        /**
         * Maximum cost factor bcrypt accepts.
         */
        public static final int MAXIMUM_BCRYPT_COST = 31;

        /**
         * Only this namespace creates it.
         */
        Password()
        {
        }

        /**
         * Hashes a password with Argon2, the recommended password hashing
         * function.
         *
         * @param password the password, not null nor empty
         * @param salt the salt, at least {@link #MINIMUM_SALT_BYTES} bytes and
         * preferably {@link #RECOMMENDED_SALT_BYTES} long, not null
         * @param algorithm the Argon2 variant, not null
         * @param memoryKb the memory to use in kibibytes, at least 8 times the
         * parallelism, 65536 for an ordinary user
         * @param iterations how many passes over that memory, 3 for an ordinary
         * user
         * @param parallelism how many lanes
         * @param outputBytes how many bytes the hash has
         * @return the hash bytes
         * @throws UnsupportedAlgorithmException when the backend has no Argon2
         */
        public byte[] argon2(char[] password, byte[] salt, Argon2Algorithm algorithm, int memoryKb,
                int iterations, int parallelism, int outputBytes)
        {
            require(algorithm, "algorithm");
            requireSalt(salt);
            if (iterations < 1)
            {
                throw new IllegalArgumentException("Argon2 needs at least one iteration, " + iterations + " given");
            }
            if (parallelism < MINIMUM_PARALLELISM)
            {
                throw new IllegalArgumentException("Argon2 needs at least one lane, " + parallelism + " given");
            }
            if (memoryKb < 8 * parallelism)
            {
                throw new IllegalArgumentException("Argon2 needs at least 8 kibibytes per lane, "
                        + (8 * parallelism) + " given " + memoryKb);
            }
            if (outputBytes < MINIMUM_HASH_BYTES)
            {
                throw new IllegalArgumentException("an Argon2 hash is at least " + MINIMUM_HASH_BYTES
                        + " bytes long, " + outputBytes + " given");
            }
            byte[] passwordBytes = utf8(CharBuffer.wrap(requirePassword(password)));
            try
            {
                return deriveArgon2(algorithm, passwordBytes, salt, memoryKb, iterations, parallelism, outputBytes);
            }
            finally
            {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }

        /**
         * Hashes a password with Argon2 and returns the encoded form of the
         * hash, which carries its own salt and parameters, so nothing else has
         * to be stored next to it.
         *
         * @param password the password, not null nor empty
         * @param salt the salt, at least {@link #MINIMUM_SALT_BYTES} bytes and
         * preferably {@link #RECOMMENDED_SALT_BYTES} long, not null
         * @param algorithm the Argon2 variant, not null
         * @param memoryKb the memory to use in kibibytes
         * @param iterations how many passes over that memory
         * @param parallelism how many lanes
         * @param outputBytes how many bytes the hash has
         * @return the encoded hash, like
         * "$argon2id$v=19$m=65536,t=3,p=4$c2FsdA$dGFzaA"
         * @throws UnsupportedAlgorithmException when the backend has no Argon2
         */
        public String argon2Encoded(char[] password, byte[] salt, Argon2Algorithm algorithm, int memoryKb,
                int iterations, int parallelism, int outputBytes)
        {
            return encodeArgon2(argon2(password, salt, algorithm, memoryKb, iterations, parallelism, outputBytes),
                    algorithm, salt, memoryKb, iterations, parallelism);
        }

        /**
         * Hashes a password with scrypt.
         *
         * @param password the password, not null nor empty
         * @param salt the salt, at least {@link #MINIMUM_SALT_BYTES} bytes and
         * preferably {@link #RECOMMENDED_SALT_BYTES} long, not null
         * @param cost the CPU and memory cost, which is the N of RFC 7914, a
         * power of two greater than one, like 16384
         * @param blockSize the block size, 8
         * @param parallelism how many threads can run in parallel
         * @param outputBytes how many bytes the hash has
         * @return the hash bytes
         * @throws UnsupportedAlgorithmException when the backend has no scrypt
         */
        public byte[] scrypt(char[] password, byte[] salt, int cost, int blockSize, int parallelism, int outputBytes)
        {
            requireSalt(salt);
            if (cost < 2 || (cost & (cost - 1)) != 0)
            {
                throw new IllegalArgumentException("the scrypt cost N must be a power of two greater than one, "
                        + cost + " given");
            }
            if (blockSize < 1)
            {
                throw new IllegalArgumentException("the scrypt block size must be at least one, " + blockSize + " given");
            }
            if (parallelism < MINIMUM_PARALLELISM)
            {
                throw new IllegalArgumentException("scrypt needs at least one thread, " + parallelism + " given");
            }
            if (outputBytes < MINIMUM_HASH_BYTES)
            {
                throw new IllegalArgumentException("a scrypt hash is at least " + MINIMUM_HASH_BYTES
                        + " bytes long, " + outputBytes + " given");
            }
            byte[] passwordBytes = utf8(CharBuffer.wrap(requirePassword(password)));
            try
            {
                return deriveScrypt(passwordBytes, salt, cost, blockSize, parallelism, outputBytes);
            }
            finally
            {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }

        /**
         * Hashes a password with scrypt and returns the encoded form of the
         * hash, which carries its own salt and parameters.
         *
         * @param password the password, not null nor empty
         * @param salt the salt, at least {@link #MINIMUM_SALT_BYTES} bytes and
         * preferably {@link #RECOMMENDED_SALT_BYTES} long, not null
         * @param cost the CPU and memory cost, a power of two greater than one
         * @param blockSize the block size
         * @param parallelism how many threads can run in parallel
         * @param outputBytes how many bytes the hash has
         * @return the encoded hash, like "$scrypt$ln=16,r=8,p=1$c2FsdA$dGFzaA"
         * @throws UnsupportedAlgorithmException when the backend has no scrypt
         */
        public String scryptEncoded(char[] password, byte[] salt, int cost, int blockSize,
                int parallelism, int outputBytes)
        {
            //the modular crypt format writes ln, the base 2 logarithm of N
            return "$scrypt$ln=" + (31 - Integer.numberOfLeadingZeros(cost)) + ",r=" + blockSize + ",p="
                    + parallelism + "$" + b64(salt) + "$"
                    + b64(scrypt(password, salt, cost, blockSize, parallelism, outputBytes));
        }

        /**
         * Hashes a password with bcrypt, the algorithm that most systems
         * already store.
         * <p>
         * bcrypt only looks at the first 72 bytes of the password and it
         * ignores anything after a null character.
         *
         * @param password the password, not null nor empty
         * @param cost the cost factor, from {@link #MINIMUM_BCRYPT_COST} to
         * {@link #MAXIMUM_BCRYPT_COST}
         * @return the encoded hash, like "$2a$10$..." ready to be stored
         * @throws UnsupportedAlgorithmException when the backend has no bcrypt
         */
        public String bcrypt(char[] password, int cost)
        {
            requirePassword(password);
            if (cost < MINIMUM_BCRYPT_COST || cost > MAXIMUM_BCRYPT_COST)
            {
                throw new IllegalArgumentException("the bcrypt cost goes from " + MINIMUM_BCRYPT_COST
                        + " to " + MAXIMUM_BCRYPT_COST + ", " + cost + " given");
            }
            return deriveBcrypt(password, cost);
        }

        /**
         * Returns whether a password is the one that produced a hash in the
         * encoded form of {@link #argon2Encoded}, {@link #scryptEncoded} or
         * {@link #bcrypt}, comparing the bytes in constant time.
         *
         * @param encoded the encoded hash, not null
         * @param password the password, not null
         * @return true when the password matches the hash
         * @throws IllegalArgumentException when the encoded string is not a
         * password hash this class understands
         * @throws UnsupportedAlgorithmException when the backend has no
         * algorithm for that kind of hash
         */
        public boolean verify(String encoded, char[] password)
        {
            require(encoded, "encoded");
            require(password, "password");
            if (encoded.startsWith("$2"))
            {
                requireBcryptHash(encoded);
                return verifyBcryptHash(encoded, password);
            }
            byte[] passwordBytes = utf8(CharBuffer.wrap(password));
            try
            {
                if (encoded.startsWith("$argon2"))
                {
                    return verifyArgon2(encoded, passwordBytes);
                }
                if (encoded.startsWith("$scrypt$"))
                {
                    return verifyScrypt(encoded, passwordBytes);
                }
                throw new IllegalArgumentException("this is not a password hash I understand: " + encoded);
            }
            finally
            {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }

        /**
         * Returns whether a password is the one that produced a hash in the
         * encoded form, as {@link #verify(String, char[])} with a string.
         *
         * @param encoded the encoded hash, not null
         * @param password the password, not null
         * @return true when the password matches the hash
         */
        public boolean verify(String encoded, String password)
        {
            require(password, "password");
            return verify(encoded, password.toCharArray());
        }

        private boolean verifyArgon2(String encoded, byte[] passwordBytes)
        {
            String[] parts = encoded.split(DOLLAR);
            //the first is empty because the string starts with a dollar
            if (parts.length != 6)
            {
                throw new IllegalArgumentException("this Argon2 hash is not well formed: " + encoded);
            }
            PasswordAlgorithm algorithm = argon2Algorithm(parts[1], encoded);
            if (!parts[2].startsWith("v="))
            {
                throw new IllegalArgumentException("this Argon2 hash does not say its version: " + encoded);
            }
            int version = number(parts[2].substring(2), encoded);
            if (version != 19)
            {
                throw new IllegalArgumentException("Argon2 version " + version
                        + " is not the one I know, which is 19, in " + encoded);
            }
            String[] parameters = parts[3].split(",");
            if (parameters.length != 3)
            {
                throw new IllegalArgumentException("this Argon2 hash does not have 3 parameters: " + encoded);
            }
            int memoryKb = parameter(parameters[0], "m=", encoded);
            int iterations = parameter(parameters[1], "t=", encoded);
            int parallelism = parameter(parameters[2], "p=", encoded);
            byte[] salt = b64decode(parts[4], encoded);
            byte[] expected = b64decode(parts[5], encoded);
            byte[] actual = deriveArgon2(algorithm.argon2Algorithm(), passwordBytes, salt, memoryKb,
                    iterations, parallelism, expected.length);
            return utils.timingSafeEql(expected, actual);
        }

        private boolean verifyScrypt(String encoded, byte[] passwordBytes)
        {
            String[] parts = encoded.split(DOLLAR);
            //the first is empty because the string starts with a dollar
            if (parts.length != 5)
            {
                throw new IllegalArgumentException("this scrypt hash is not well formed: " + encoded);
            }
            String[] parameters = parts[2].split(",");
            if (parameters.length != 3)
            {
                throw new IllegalArgumentException("this scrypt hash does not have 3 parameters: " + encoded);
            }
            int exponent = parameter(parameters[0], "ln=", encoded);
            if (exponent < 1 || exponent > 30)
            {
                throw new IllegalArgumentException("the scrypt cost of this hash is out of range in " + encoded);
            }
            int cost = 1 << exponent;
            int blockSize = parameter(parameters[1], "r=", encoded);
            int parallelism = parameter(parameters[2], "p=", encoded);
            byte[] salt = b64decode(parts[3], encoded);
            byte[] expected = b64decode(parts[4], encoded);
            byte[] actual = deriveScrypt(passwordBytes, salt, cost, blockSize, parallelism, expected.length);
            return utils.timingSafeEql(expected, actual);
        }

        private String encodeArgon2(byte[] hash, Argon2Algorithm algorithm, byte[] salt, int memoryKb,
                int iterations, int parallelism)
        {
            return "$" + algorithm.jcaName() + "$v=19$m=" + memoryKb + ",t=" + iterations + ",p=" + parallelism
                    + "$" + b64(salt) + "$" + b64(hash);
        }

        private PasswordAlgorithm argon2Algorithm(String name, String encoded)
        {
            for (PasswordAlgorithm algorithm : new PasswordAlgorithm[]{
                PasswordAlgorithm.ARGON2D, PasswordAlgorithm.ARGON2I, PasswordAlgorithm.ARGON2ID})
            {
                if (algorithm.jcaName().equals(name))
                {
                    return algorithm;
                }
            }
            throw new IllegalArgumentException("this is not an Argon2 variant I know in " + encoded);
        }

        private int parameter(String parameter, String prefix, String encoded)
        {
            if (!parameter.startsWith(prefix))
            {
                throw new IllegalArgumentException("expected " + prefix + " in " + encoded);
            }
            return number(parameter.substring(prefix.length()), encoded);
        }

        private int number(String value, String encoded)
        {
            try
            {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException ex)
            {
                throw new IllegalArgumentException("this is not a number in " + encoded + ": " + value, ex);
            }
        }

        private void requireBcryptHash(String encoded)
        {
            //"$2b$" then 2 digits of cost, "$", 22 characters of salt and 31 of
            //hash, all of them in the radix 64 of OpenBSD
            if (encoded.length() != 60 || !"$abxy".contains(String.valueOf(encoded.charAt(2)))
                    || encoded.charAt(3) != '$' || encoded.charAt(6) != '$'
                    || !isNumber(encoded, 4, 2) || !isRadix64(encoded, 7, 53))
            {
                throw new IllegalArgumentException("this is not a bcrypt hash I understand: " + encoded);
            }
        }

        private boolean isNumber(String value, int from, int length)
        {
            for (int i = from; i < from + length; i++)
            {
                if (!Character.isDigit(value.charAt(i)))
                {
                    return false;
                }
            }
            return true;
        }

        private boolean isRadix64(String value, int from, int length)
        {
            for (int i = from; i < from + length; i++)
            {
                if (!BCRYPT_RADIX64.contains(String.valueOf(value.charAt(i))))
                {
                    return false;
                }
            }
            return true;
        }

        private void requireSalt(byte[] salt)
        {
            require(salt, "salt");
            if (salt.length < MINIMUM_SALT_BYTES)
            {
                throw new IllegalArgumentException("the salt of a password hash is at least " + MINIMUM_SALT_BYTES
                        + " bytes, " + salt.length + " given");
            }
        }

        private char[] requirePassword(char[] password)
        {
            require(password, "password");
            if (password.length == 0)
            {
                throw new IllegalArgumentException("the password must not be empty");
            }
            return password;
        }

        /**
         * The alphabet of bcrypt, which is its own radix 64 and is not base64.
         */
        private static final String BCRYPT_RADIX64 =
                "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        /**
         * The base64 of the modular crypt format, which has no padding.
         */
        private String b64(byte[] bytes)
        {
            String encoded = Base64.encode(bytes);
            int end = encoded.length();
            while (end > 0 && encoded.charAt(end - 1) == '=')
            {
                end--;
            }
            return encoded.substring(0, end);
        }

        private byte[] b64decode(String encoded, String whole)
        {
            //the modular crypt format writes base64 without the padding that
            //makes it a multiple of 4 characters
            StringBuilder padded = new StringBuilder(encoded);
            while (padded.length() % 4 != 0)
            {
                padded.append('=');
            }
            try
            {
                return Base64.decode(padded.toString());
            }
            catch (Base64DecoderException ex)
            {
                throw new IllegalArgumentException("this is not base64 in " + whole + ": " + encoded, ex);
            }
        }
    }

}
