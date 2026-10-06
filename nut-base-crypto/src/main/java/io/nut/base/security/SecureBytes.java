/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Rand;
import io.nut.base.jca.Kr.SecretKeyTransformation;
import io.nut.base.lang.Empty;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.Destroyable;

/**
 * Holds a byte array securely in memory by encrypting it with AES-256-GCM
 * using an ephemeral key that lives only for the duration of this object's
 * lifetime.
 *
 * <p>The plaintext is encrypted immediately on construction and the original
 * array is zeroed out (also when encryption fails). The decrypted data is only
 * ever reconstructed transiently (inside {@link #getBytes()} or
 * {@link #consume(Consumer)}) and is zeroed again as soon as the caller is
 * done with it.</p>
 *
 * <p>The AES key is kept as a plain {@code byte[]} owned by this class. Every
 * cipher operation wraps it in a {@link SecureSecretKey} that is destroyed in a
 * {@code finally} block, so no {@code SecretKeySpec} copy is left on the heap
 * and no reflection (which fails on JDK 16+ without {@code --add-opens}) is
 * needed. The JCA provider may still keep its own internal copies (key
 * schedule); those cannot be wiped from here.</p>
 *
 * <p>Instances must be explicitly released by calling {@link #destroy()} or
 * by using a try-with-resources block. After destruction all internal state
 * (key, IV and ciphertext) is overwritten with zeroes and
 * {@link #getBytes()} / {@link #consume(Consumer)} throw
 * {@link IllegalStateException}.</p>
 *
 * <p>Thread safety: {@link #destroy()} and the decryption step of
 * {@link #getBytes()} are synchronized on this instance, so a concurrent
 * destroy can neither interleave with a decryption nor leave a half-wiped
 * state. The consumer passed to {@link #consume(Consumer)} runs outside the
 * lock.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * byte[] secret = obtainSecret();
 * try (SecureBytes sb = new SecureBytes(secret)) {
 *     // secret[] has already been zeroed by the constructor
 *     sb.consume(data -> process(data));
 *     // data[] is zeroed immediately after the lambda returns
 * }
 * }</pre>
 *
 * @see SecureChars
 */
public final class SecureBytes implements AutoCloseable, Destroyable
{
    /**
     * Lazy-initialized singleton holder for the shared {@link Kripto} instance.
     */
    private enum Holder
    {
        INSTANCE;
        final Kripto kripto = Kripto.getInstance();
    }

    /** Length of the AES-GCM initialization vector in bytes (96 bits). */
    private static final int IV_BYTES = 12; //96 bits

    /** Length of the AES-256 key in bytes. */
    private static final int KEY_BYTES = 32;

    /** Length of the AES-GCM authentication tag in bits. */
    private static final int TAG_BITS = 128;

    /** Algorithm name of the ephemeral key. */
    private static final String AES = "AES";

    /** Shared cryptographically-secure random generator. */
    private static final Rand RAND = Kripto.getRand();

    /** Cryptographic utilities used to create IVs and ciphers. */
    private final Kripto kripto;

    /** Random IV generated fresh for every instance. */
    private final byte[] iv;

    /** Ephemeral AES-256 key bytes owned by this object; wiped by {@link #destroy()}. */
    private final byte[] key;

    /** AES-256-GCM ciphertext of the original data, including the authentication tag. */
    private final byte[] encryptedData;

    /** {@code true} once {@link #destroy()} has been called (or for null/empty input). */
    private volatile boolean destroyed;

    /**
     * Constructs a {@code SecureBytes} instance that encrypts {@code data}
     * with AES-256-GCM using the provided {@link Kripto} instance (or the
     * shared default if {@code null}).
     *
     * <p>The {@code data} array is zeroed by this constructor, including when
     * encryption fails. Passing {@code null} creates an already destroyed
     * instance that behaves as if it wraps a {@code null} array. Passing an
     * empty array creates an already destroyed instance whose
     * {@link #getBytes()} returns an empty array without any cryptographic
     * operation.</p>
     *
     * @param data   the plaintext to protect; may be {@code null} or empty.
     * @param kripto the {@link Kripto} instance to use; if {@code null} the
     *               shared singleton is used.
     * @throws RuntimeException wrapping any JCA exception that occurs during
     *                          encryption.
     */
    public SecureBytes(byte[] data, Kripto kripto)
    {
        if(data == null)
        {
            this.kripto = null;
            this.iv = null;
            this.key = null;
            this.encryptedData = null;
            this.destroyed = true;
            return;
        }
        if(data.length == 0)
        {
            this.kripto = null;
            this.iv = null;
            this.key = null;
            this.encryptedData = Empty.BYTES;
            this.destroyed = true;
            return;
        }

        Kripto k = kripto == null ? Holder.INSTANCE.kripto : kripto;
        byte[] keyBytes = null;
        byte[] ivBytes = null;
        byte[] cipherText;
        boolean ok = false;
        try
        {
            // use the returned arrays: do not assume nextBytes fills in place
            keyBytes = RAND.nextBytes(new byte[KEY_BYTES]);
            ivBytes = RAND.nextBytes(new byte[IV_BYTES]);

            GCMParameterSpec spec = k.getIvGCM(ivBytes, TAG_BITS);
            try (SecureSecretKey sk = new SecureSecretKey(keyBytes, AES))
            {
                Cipher cipher = k.getCipher(sk, SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.ENCRYPT_MODE);
                cipherText = cipher.doFinal(data);
            }
            ok = true;
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            Arrays.fill(data, (byte) 0);
            if(!ok)
            {
                if(keyBytes != null)
                {
                    Arrays.fill(keyBytes, (byte) 0);
                }
                if(ivBytes != null)
                {
                    Arrays.fill(ivBytes, (byte) 0);
                }
            }
        }
        this.kripto = k;
        this.key = keyBytes;
        this.iv = ivBytes;
        this.encryptedData = cipherText;
    }

    /**
     * Constructs a {@code SecureBytes} instance using the shared default
     * {@link Kripto} instance. Equivalent to {@code new SecureBytes(data, null)}.
     *
     * @param data the plaintext to protect; may be {@code null} or empty.
     */
    public SecureBytes(byte[] data)
    {
        this(data, null);
    }

    /**
     * Decrypts and returns the protected byte array.
     *
     * <p>The returned array is a freshly allocated buffer. The caller must zero
     * it when finished (prefer {@link #consume(Consumer)}). Package-private on
     * purpose.</p>
     *
     * @return the plaintext, {@code null} if constructed with {@code null}, or
     *         an empty array if constructed with an empty array.
     * @throws IllegalStateException if this instance has been destroyed
     * @throws RuntimeException      wrapping any JCA exception during decryption
     */
    // keep private for outsiders
    synchronized byte[] getBytes()
    {
        if(this.encryptedData == null)
        {
            return null;
        }
        if(this.encryptedData.length == 0)
        {
            return this.encryptedData;
        }
        if(this.destroyed)
        {
            throw new IllegalStateException("SecureBytes has been destroyed");
        }
        try
        {
            GCMParameterSpec spec = this.kripto.getIvGCM(this.iv, TAG_BITS);
            try (SecureSecretKey sk = new SecureSecretKey(this.key, AES))
            {
                Cipher cipher = this.kripto.getCipher(sk, SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.DECRYPT_MODE);
                return cipher.doFinal(this.encryptedData);
            }
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Destroys this instance by zeroing all sensitive material (ciphertext,
     * IV and key) and marking it as destroyed. Subsequent calls are no-ops.
     */
    @Override
    public synchronized void destroy()
    {
        if(!this.destroyed)
        {
            try
            {
                if(this.encryptedData != null)
                {
                    Arrays.fill(this.encryptedData, (byte) 0);
                }
                if(this.iv != null)
                {
                    Arrays.fill(this.iv, (byte) 0);
                }
                if(this.key != null)
                {
                    Arrays.fill(this.key, (byte) 0);
                }
            }
            finally
            {
                this.destroyed = true;
            }
        }
    }

    /**
     * @return {@code true} after {@link #destroy()} or {@link #close()} has
     *         been called, and for instances built from {@code null} or an
     *         empty array.
     */
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
     * Decrypts the protected data, passes it to {@code consumer}, and zeros the
     * temporary plaintext before returning, even if the consumer throws.
     *
     * <p>If this instance was built from {@code null}, the consumer receives
     * {@code null}; if built from an empty array, it receives an empty
     * array.</p>
     *
     * @param consumer receives the temporary plaintext; must not retain it.
     * @throws IllegalStateException if this instance has been destroyed
     */
    public void consume(Consumer<byte[]> consumer)
    {
        byte[] tmp = getBytes();
        try
        {
            consumer.accept(tmp);
        }
        finally
        {
            if(tmp != null && tmp.length != 0)
            {
                Arrays.fill(tmp, (byte) 0);
            }
        }
    }
}
