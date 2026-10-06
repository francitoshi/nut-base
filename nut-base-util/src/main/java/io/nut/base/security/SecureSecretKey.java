/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.SecretKey;

/**
 * An immutable, self-wiping {@link SecretKey} that holds its key material as a
 * plain {@code byte[]} and never leaks a reference to it.
 *
 * <p>{@link javax.crypto.spec.SecretKeySpec} clones its input but offers no way
 * to erase the copy it keeps. Here {@link #destroy()} zeroes the array
 * immediately and drops the reference.</p>
 *
 * <p>Instances must be released by calling {@link #destroy()} or by using a
 * try-with-resources block. After destruction every accessor throws
 * {@link IllegalStateException}.</p>
 *
 * <p><b>Serialization is refused</b>, both ways, so raw key material is never
 * written to a stream and a destroyed key cannot be resurrected.</p>
 *
 * <p><b>Ownership.</b> The constructor copies {@code keyMaterial} but does
 * <em>not</em> zero the caller's array; the caller wipes its own copy. Note
 * that {@link #getEncoded()} also returns a copy that the caller must wipe, and
 * that a {@code Cipher} may keep internal copies that cannot be wiped.</p>
 *
 * <p><b>Thread safety.</b> All access to the key material goes through
 * methods synchronized on {@code this}; no method holds this lock while
 * calling into another key, so {@code a.equals(b)} and {@code b.equals(a)}
 * running concurrently cannot deadlock.</p>
 *
 * <p><b>Equality.</b> Only two {@code SecureSecretKey} instances can be equal,
 * which keeps {@code equals} consistent with {@link #hashCode()}: a
 * {@code SecretKeySpec} hashes its material while this class deliberately does
 * not, so mixing both types would break the {@code equals/hashCode} contract.
 * Compare against other key types with {@code MessageDigest.isEqual} on the
 * encoded bytes.</p>
 *
 * <pre>{@code
 * byte[] raw = loadKeyFromKeystore();
 * try (SecureSecretKey key = new SecureSecretKey(raw, "AES")) {
 *     Cipher cipher = kripto.getCipher(key, AES_GCM_NoPadding, spec, ENCRYPT_MODE);
 * }
 * Arrays.fill(raw, (byte) 0); // the caller still owns raw
 * }</pre>
 *
 * @see SecureBytes
 * @see Wiper
 */
public final class SecureSecretKey implements SecretKey, AutoCloseable
{
    private static final long serialVersionUID = 1L;

    private static final String FORMAT_RAW = "RAW";

    /** Raw key material, {@code null} once destroyed. Guarded by {@code this}. */
    private byte[] keyMaterial;

    /** Algorithm name as passed to the constructor, never {@code null}. */
    private final String algorithm;

    /** {@code true} once {@link #destroy()} has run. */
    private volatile boolean destroyed;

    /**
     * Creates a key from the given raw material (copied, not zeroed).
     *
     * @param keyMaterial the raw key bytes; must be non-null and non-empty
     * @param algorithm   the algorithm name, e.g. {@code "AES"}; non-null and non-blank
     * @throws IllegalArgumentException if either argument is null or empty
     */
    public SecureSecretKey(byte[] keyMaterial, String algorithm)
    {
        if(keyMaterial == null || keyMaterial.length == 0)
        {
            throw new IllegalArgumentException("Key material cannot be null or empty");
        }
        if(algorithm == null || algorithm.trim().isEmpty())
        {
            throw new IllegalArgumentException("Algorithm cannot be null or empty");
        }
        this.keyMaterial = keyMaterial.clone();
        this.algorithm = algorithm.trim();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this key has been destroyed
     */
    @Override
    public String getAlgorithm()
    {
        checkDestroyed();
        return this.algorithm;
    }

    /**
     * {@inheritDoc}
     *
     * @return always {@code "RAW"}
     * @throws IllegalStateException if this key has been destroyed
     */
    @Override
    public String getFormat()
    {
        checkDestroyed();
        return FORMAT_RAW;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a copy that the caller is responsible for wiping.</p>
     *
     * @throws IllegalStateException if this key has been destroyed
     */
    @Override
    public synchronized byte[] getEncoded()
    {
        checkDestroyed();
        return this.keyMaterial.clone();
    }

    /**
     * Zeroes the key material and releases the reference to it. Subsequent
     * calls are no-ops. Does not declare
     * {@link javax.security.auth.DestroyFailedException}, so {@link #close()}
     * needs no catch block.
     */
    @Override
    public synchronized void destroy()
    {
        if(!this.destroyed)
        {
            try
            {
                if(this.keyMaterial != null)
                {
                    Arrays.fill(this.keyMaterial, (byte) 0);
                    this.keyMaterial = null;
                }
            }
            finally
            {
                this.destroyed = true;
            }
        }
    }

    /** @return {@code true} after {@link #destroy()} or {@link #close()} has run */
    @Override
    public boolean isDestroyed()
    {
        return this.destroyed;
    }

    /** Delegates to {@link #destroy()}, enabling try-with-resources. */
    @Override
    public void close()
    {
        this.destroy();
    }

    /**
     * Compares this key with another {@code SecureSecretKey} by algorithm name
     * (case insensitive) and key material, in constant time with respect to the
     * material.
     *
     * <p>A key is always equal to itself (reflexivity), even if destroyed. Two
     * distinct keys are never equal if either has been destroyed.</p>
     *
     * @param other the object to compare with
     * @return {@code true} if both keys hold the same material for the same algorithm
     */
    @Override
    public boolean equals(Object other)
    {
        if(this == other)
        {
            return true;
        }
        if(!(other instanceof SecureSecretKey))
        {
            return false;
        }
        SecureSecretKey that = (SecureSecretKey) other;
        if(!this.algorithm.equalsIgnoreCase(that.algorithm))
        {
            return false;
        }
        // each copy is taken under that key's own lock, one after the other:
        // never two locks at once, so no lock-ordering deadlock is possible
        byte[] mine = this.copyMaterial();
        byte[] theirs = that.copyMaterial();
        try
        {
            return mine != null && theirs != null && MessageDigest.isEqual(mine, theirs);
        }
        finally
        {
            if(mine != null)
            {
                Arrays.fill(mine, (byte) 0);
            }
            if(theirs != null)
            {
                Arrays.fill(theirs, (byte) 0);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deliberately derived from the algorithm name only: a hash over the key
     * material would let anyone with a candidate key confirm it, and hash codes
     * often end up in logs. Keys sharing an algorithm therefore collide in a
     * {@link java.util.HashMap}; that is the intended trade-off. Works on
     * destroyed keys too, so destroyed keys remain removable from collections.</p>
     */
    @Override
    public int hashCode()
    {
        return this.algorithm.toUpperCase(Locale.ROOT).hashCode();
    }

    /** @return a copy of the material, or {@code null} if destroyed */
    private synchronized byte[] copyMaterial()
    {
        return this.keyMaterial == null ? null : this.keyMaterial.clone();
    }

    /**
     * Refuses to serialize this key.
     *
     * @throws NotSerializableException always
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        throw new NotSerializableException("SecureSecretKey holds raw key material and cannot be serialized");
    }

    /**
     * Refuses to deserialize a key.
     *
     * @throws NotSerializableException always
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        throw new NotSerializableException("SecureSecretKey holds raw key material and cannot be deserialized");
    }

    private void checkDestroyed()
    {
        if(this.destroyed)
        {
            throw new IllegalStateException("Key has been destroyed");
        }
    }
}
