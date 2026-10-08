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
     * the JDK does not implement RIPEMD160, so this digest can only
     * be used when Bouncy Castle is on the classpath and the instance was
     * created with that provider. Use {@link io.nut.base.crypto.alt.RIPEMD160},
     * which is implemented in pure java and always works, instead. See
     * {@link #getDigest(MessageDigestAlgorithm)}.
     */
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
}
