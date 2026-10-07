/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.jca.Digest;
import io.nut.base.crypto.shamir.ShamirSharedSecret;
import io.nut.base.crypto.kdf.HKDF;
import io.nut.base.crypto.kdf.HKDFBC;
import io.nut.base.crypto.kdf.PBKDF2;
import io.nut.base.crypto.stego.Steganography;
import io.nut.base.jca.Kr;
import io.nut.base.lang.Exceptions;
import io.nut.base.lang.Strings;
import io.nut.base.util.Comparators;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

/**
 * A utility class providing cryptographic operations including encryption,
 * decryption, key generation, digital signatures, and secure random number
 * generation.
 *
 * @author franci
 */
public class Kripto extends Kr
{
    ////////////////////////////////////////////////////////////////////////////
    ///// GOOD PRACTICES ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * AES transformation with GCM mode and no padding; DO NOT REPEAT IV, ALWAYS USE A RANDOM ONE.
     */
    public static final SecretKeyTransformation AES_GCM_NOPADDING = SecretKeyTransformation.AES_GCM_NoPadding;

    ////////////////////////////////////////////////////////////////////////////
    ///// Random data  /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public static Rand getRandStrong()
    {
        return new Rand(getSecureRandomStrong());
    }

    public static Rand getRandStrongFast()
    {
        return new Rand(getSecureRandomStrongFast());
    }

    public static Rand getRand()
    {
        return new Rand(getSecureRandom());
    }

    ////////////////////////////////////////////////////////////////////////////
    ///// Static Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private enum Holder
    {
        INSTANCE;
        Kripto kripto = new Kripto();
    }
    /**
     * Returns the default instance of {@link Kripto} with no specific provider.
     *
     * @return a new Kripto instance
     */
    public static Kripto getInstance()
    {
        return Holder.INSTANCE.kripto;
    }

    public static Kripto getInstance(String providerName)
    {
        return new Kripto(providerName);
    }

    /**
     * Returns an instance of {@link Kripto}, optionally preferring Bouncy
     * Castle provider.
     *
     * @param preferBouncyCastle true to prefer Bouncy Castle, false for the default instance
     * 
     * @return a Kripto instance
     */
    public static Kripto getInstance(boolean preferBouncyCastle)
    {
        Kripto instance = preferBouncyCastle ? getInstanceBouncyCastle() : getInstance();
        return instance != null ? instance : getInstance();
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
    ///// Instance Members /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    protected Kripto()
    {
        super();
    }

    protected Kripto(String providerName)
    {
        super(providerName);
    }

    protected Kripto(String providerName, boolean forceProvider)
    {
        super(providerName, forceProvider);
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

    /**
     * Computes RIPEMD160(SHA256(input)), the hash160 of a compressed public key.
     *
     * @deprecated the JDK does not implement RIPEMD160, so this only works when
     * Bouncy Castle is on the classpath and the instance was created with that
     * provider. Use {@link io.nut.base.crypto.alt.RIPEMD160} together with
     * {@link #sha256} instead, which needs no provider at all:
     * <pre>{@code new RIPEMD160().digest(kripto.sha256.digest(publicKey))}</pre>
     * @param input the data to hash
     * @return the 20 bytes of the double hash
     */
    @Deprecated
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

    /**
     * The RIPEMD160 digest of this provider.
     *
     * @deprecated the JDK does not implement RIPEMD160, so this digest can only
     * be used when Bouncy Castle is on the classpath and the instance was
     * created with that provider. Use {@link io.nut.base.crypto.alt.RIPEMD160},
     * which is implemented in pure java and always works, instead. See
     * {@link #getDigest(MessageDigestAlgorithm)}.
     */
    @Deprecated
    public final Digest ripemd160 = getDigest(MessageDigestAlgorithm.RIPEMD160);

    public final PBKDF2 pbkdf2WithSha256 = getPBKDF2(Pbkdf2.PBKDF2WithHmacSHA256);
    public final PBKDF2 pbkdf2WithSha512 = getPBKDF2(Pbkdf2.PBKDF2WithHmacSHA512);

    // This methods use Bouncy Castle
    public HKDF getHkdfWithSha256()
    {
        return getHKDF(Hkdf.HkdfWithSha256);
    }
    public final HKDF getHkdfWithSha384()
    {
        return getHKDF(Hkdf.HkdfWithSha384);
    }
    public final HKDF getHkdfWithSha512()
    {
        return getHKDF(Hkdf.HkdfWithSha512);
    }

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
        int cmp = Comparators.compare(ownFp, otherFp);

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
            throw new InvalidParameterException("q must be [q % 4 != 3]");
        }
        if(!p.isProbablePrime(128))
        {
            throw new InvalidParameterException("p must be prime");
        }
        if(!q.isProbablePrime(128))
        {
            throw new InvalidParameterException("q must be prime");
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