/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

import io.github.francitoshi.nut.base.commons.jca.Kr;
import io.github.francitoshi.nut.base.commons.jca.Rand;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Authenticated encryption of a single record with AES-256-GCM.
 * <p>
 * Every message is encrypted with a fresh, cryptographically random 96-bit
 * initialization vector, as recommended by NIST SP 800-38D for GCM. The
 * 128-bit authentication tag is appended to the ciphertext by the JCE
 * provider. The resulting blob is
 * {@code [IV (12 bytes)] + [ciphertext + authentication tag]}.
 * <p>
 * The initialization vector is never reused within a store: a new one is
 * drawn for every record, and it travels with the record when the journal is
 * compacted. As for any random-nonce AEAD, the number of encryptions
 * performed with the same key must stay well below 2^32.
 * <p>
 * The associated data is not encrypted but is covered by the authentication
 * tag, so it cannot be changed without detecting the tampering. The store uses
 * it to bind every ciphertext to its record type and to the format version.
 *
 * @see Kr.SecretKeyTransformation#AES_GCM_NoPadding
 */
final class AeadRecordCipher
{
    /** Length of the GCM initialization vector, in bytes (96 bits). */
    static final int IV_BYTES = 12;

    /** Length of the GCM authentication tag, in bits. */
    static final int TAG_BITS = 128;

    /** Length of the GCM authentication tag, in bytes. */
    static final int TAG_BYTES = TAG_BITS / 8;

    /** Minimum size of a valid blob: the initialization vector plus the tag. */
    static final int MIN_BLOB_BYTES = IV_BYTES + TAG_BYTES;

    private static final Rand RAND = Rand.getInstance();
    private static final Kr KRIPTO = Kr.getInstance();

    private final SecretKey key;

    AeadRecordCipher(SecretKey key)
    {
        this.key = Objects.requireNonNull(key, "key");
    }

    /**
     * Encrypts a record body.
     *
     * @param aad  the associated data, authenticated but not encrypted
     * @param body the plaintext
     * @return {@code [IV] + [ciphertext + authentication tag]}
     * @throws IllegalStateException if the encryption fails
     */
    byte[] encrypt(byte[] aad, byte[] body)
    {
        byte[] iv = RAND.nextBytes(new byte[IV_BYTES]);
        try
        {
            GCMParameterSpec spec = KRIPTO.getIvGCM(iv, TAG_BITS);
            Cipher cipher = KRIPTO.getCipher(key, Kr.SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.ENCRYPT_MODE);
            cipher.updateAAD(aad);
            byte[] ciphertext = cipher.doFinal(body);
            byte[] blob = new byte[IV_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, blob, 0, IV_BYTES);
            System.arraycopy(ciphertext, 0, blob, IV_BYTES, ciphertext.length);
            return blob;
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("cannot encrypt the record", e);
        }
    }

    /**
     * Decrypts and authenticates a record body.
     *
     * @param aad  the associated data used when the record was encrypted
     * @param blob {@code [IV] + [ciphertext + authentication tag]}
     * @return the plaintext
     * @throws GeneralSecurityException if the blob is malformed, the
     *                                   authentication tag does not match
     *                                   (wrong key or tampered data) or the
     *                                   decryption fails
     */
    byte[] decrypt(byte[] aad, byte[] blob) throws GeneralSecurityException
    {
        if (blob == null || blob.length < MIN_BLOB_BYTES)
        {
            throw new AEADBadTagException("malformed encrypted record");
        }
        byte[] iv = Arrays.copyOfRange(blob, 0, IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(blob, IV_BYTES, blob.length);
        GCMParameterSpec spec = KRIPTO.getIvGCM(iv, TAG_BITS);
        Cipher cipher = KRIPTO.getCipher(key, Kr.SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.DECRYPT_MODE);
        cipher.updateAAD(aad);
        return cipher.doFinal(ciphertext);
    }
}
