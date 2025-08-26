/*
 *  SecureWrapper.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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

import io.nut.base.crypto.Kripto.Hkdf;
import io.nut.base.crypto.Kripto.Pbkdf2;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Provides a high-level API for symmetric authenticated encryption, serializing
 * the output into a self-contained, versioned string format. This class is
 * designed to be instantiated with specific cryptographic parameters.
 * <p>
 * It supports two primary modes of operation for key derivation:
 * <ol>
 * <li><b>HKDF (HMAC-based Key Derivation Function):</b> For deriving encryption
 * keys from a high-entropy master key. This is suitable for machine-generated
 * keys. The resulting format is: {@code v1$salt$info$iv$ciphertext}</li>
 *
 * <li><b>PBKDF2 (Password-Based Key Derivation Function 2):</b> For deriving
 * encryption keys from a low-entropy, human-memorable password. The resulting
 * format is: {@code v1$salt$rounds$iv$ciphertext}</li>
 * </ol>
 * <p>
 * All encryption is performed using AES-GCM, which provides confidentiality,
 * integrity, and authenticity.
 * <p>
 * <b>Note on Thread Safety:</b> Instances of this class are NOT guaranteed to
 * be thread-safe. A new instance should be created for each task or used in a
 * thread-confined manner.
 */
public final class SecureWrapper
{
    // --- Cryptographic Constants ---
    private static final int GCM_IV_BYTES = 12; // 96 bits is recommended for GCM.
    private static final int KEY_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int SALT_BYTES = 16;
    private static final int PBKDF2_ROUNDS = 2048;

    // --- Format Constants ---
    private static final String FORMAT_VERSION = "v1";
    private static final String SEPARATOR = "$";
    private static final String SEPARATOR_REGEX = "\\$";

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    private final Kripto kripto;
    private final HKDF hkdf;
    private final PBKDF2 pbkdf2;
    public final int pbkdf2Rounds;
    private final int keyBits;
    private final int keyBytes;
    private final Rand rand;

    /**
     * Constructs a new SecureWrapper with specific cryptographic parameters.
     *
     * @param kripto The {@link Kripto} provider for cryptographic primitives.
     * If null, a default instance is used.
     * @param pbkdf2Rounds The number of iterations to use for PBKDF2 key
     * derivation. This is the work factor.
     * @param keyBits The desired key length in bits for AES (e.g., 128, 192,
     * 256).
     */
    public SecureWrapper(Kripto kripto, int pbkdf2Rounds, int keyBits)
    {
        this.kripto = kripto == null ? Kripto.getInstance(true) : kripto;
        this.rand = Kripto.getRand();
        this.hkdf = this.kripto.hkdfWithSha512;
        this.pbkdf2 = this.kripto.pbkdf2WithSha512;
        this.pbkdf2Rounds = pbkdf2Rounds;
        this.keyBits = keyBits;
        this.keyBytes = keyBits / 8;
    }

    /**
     * Constructor to create an instance with default parameters.
     */
    public SecureWrapper()
    {
        this(null, PBKDF2_ROUNDS, KEY_BITS);
    }

    /**
     * Wraps and encrypts plaintext using a high-entropy master key. It uses
     * HKDF to derive a unique encryption key for this operation. The provided
     * {@code info} parameter is embedded in the output and verified during
     * unwrap.
     *
     * @param plaintext The data to encrypt.
     * @param info The context/domain separation data for HKDF. This is a
     * critical security parameter.
     * @param key The high-entropy master key (IKM for HKDF).
     * @return A self-contained, encrypted string in the format
     * {@code v1$salt$info$iv$ciphertext}.
     * @throws GeneralSecurityException if any cryptographic operation fails.
     */
    public String wrap(byte[] plaintext, byte[] info, byte[] key) throws GeneralSecurityException
    {
        byte[] salt = this.rand.nextBytes(new byte[SALT_BYTES]);
        SecretKey secretKey = hkdf.deriveSecretKeyAES(key, salt, info, keyBytes);

        String encodedSalt = BASE64_ENCODER.encodeToString(salt);
        String encodedInfo = BASE64_ENCODER.encodeToString(info);
        String wrappedCiphertext = wrap(plaintext, secretKey);
        return String.join(SEPARATOR, FORMAT_VERSION, encodedSalt, encodedInfo, wrappedCiphertext);
    }

    /**
     * Wraps and encrypts plaintext using a low-entropy, human-memorable
     * password. It uses PBKDF2 to stretch the password into a secure encryption
     * key.
     *
     * @param plaintext The data to encrypt.
     * @param pass The password to use for key derivation. It is a
     * {@code char[]} for enhanced security.
     * @return A self-contained, encrypted string in the format
     * {@code v1$salt$rounds$iv$ciphertext}.
     * @throws GeneralSecurityException if any cryptographic operation fails.
     */
    public String wrap(byte[] plaintext, char[] pass) throws GeneralSecurityException
    {
        byte[] salt = this.rand.nextBytes(new byte[SALT_BYTES]);
        SecretKey secretKey = this.pbkdf2.deriveSecretKeyAES(pass, salt, pbkdf2Rounds, keyBits);

        String encodedSalt = BASE64_ENCODER.encodeToString(salt);
        String encodedRounds = Integer.toString(pbkdf2Rounds);
        String wrappedCiphertext = wrap(plaintext, secretKey);

        return String.join(SEPARATOR, FORMAT_VERSION, encodedSalt, encodedRounds, wrappedCiphertext);
    }

    /**
     * Core encryption logic using AES-GCM with a pre-derived key.
     *
     * @param plaintext The data to encrypt.
     * @param key The {@link SecretKey} to use for encryption.
     * @return An encrypted string containing the IV and the ciphertext,
     * separated by '$'.
     * @throws GeneralSecurityException if encryption fails.
     */
    private String wrap(byte[] plaintext, SecretKey key) throws GeneralSecurityException
    {
        byte[] iv = this.rand.nextBytes(new byte[GCM_IV_BYTES]);

        GCMParameterSpec ivGcm = this.kripto.getIvGCM(iv, GCM_TAG_BITS);
        Cipher cipher = this.kripto.getCipher(key, SecretKeyTransformation.AES_GCM_NoPadding, ivGcm, Cipher.ENCRYPT_MODE);
        byte[] ciphertext = cipher.doFinal(plaintext);

        String encodedIv = BASE64_ENCODER.encodeToString(iv);
        String encodedCiphertext = BASE64_ENCODER.encodeToString(ciphertext);

        return String.join(SEPARATOR, encodedIv, encodedCiphertext);
    }

    /**
     * Unwraps and decrypts a string that was protected with a master key. This
     * method re-derives the key using HKDF and verifies that the provided
     * {@code info} parameter matches the one embedded in the
     * {@code wrappedtext}. This check prevents context confusion attacks.
     *
     * @param wrappedtext The encrypted string to decrypt.
     * @param info The **expected** context/domain separation data.
     * @param key The master key that was originally used to wrap the data.
     * @return The original decrypted plaintext.
     * @throws GeneralSecurityException if decryption or authentication fails
     * (e.g., wrong key, tampered data).
     * @throws IllegalArgumentException if the wrapped string has an invalid
     * format or if the {@code info} context does not match.
     */
    public byte[] unwrap(String wrappedtext, byte[] info, byte[] key) throws GeneralSecurityException
    {
        String[] parts = wrappedtext.split(SEPARATOR_REGEX);
        if (parts.length != 5)
        {
            throw new IllegalArgumentException("Invalid wrapped format: incorrect number of parts.");
        }
        if (!FORMAT_VERSION.equals(parts[0]))
        {
            throw new IllegalArgumentException("Unsupported version: " + parts[0]);
        }
        byte[] salt = BASE64_DECODER.decode(parts[1]);
        byte[] parsedInfo = BASE64_DECODER.decode(parts[2]);
        if (!Arrays.equals(info, parsedInfo))
        {
            throw new IllegalArgumentException("Mismatched info context. Expected '" + new String(info) + "' but found '" + new String(parsedInfo) + "'.");
        }
        SecretKey secretKey = hkdf.deriveSecretKeyAES(key, salt, info, keyBytes);
        return unwrap(parts, secretKey);
    }

    /**
     * Unwraps and decrypts a string that was protected with a password. This
     * method re-derives the key using PBKDF2 with the parameters embedded in
     * the {@code wrappedtext}.
     *
     * @param wrappedtext The encrypted string to decrypt.
     * @param pass The password that was originally used to wrap the data.
     * @return The original decrypted plaintext.
     * @throws GeneralSecurityException if decryption or authentication fails
     * (e.g., wrong password, tampered data).
     * @throws IllegalArgumentException if the wrapped string has an invalid
     * format.
     */
    public byte[] unwrap(String wrappedtext, char[] pass) throws GeneralSecurityException
    {
        String[] parts = wrappedtext.split(SEPARATOR_REGEX);
        if (parts.length != 5)
        {
            throw new IllegalArgumentException("Invalid wrapped format: incorrect number of parts.");
        }
        if (!FORMAT_VERSION.equals(parts[0]))
        {
            throw new IllegalArgumentException("Unsupported version: " + parts[0]);
        }
        byte[] salt = BASE64_DECODER.decode(parts[1]);
        int rounds = Integer.parseUnsignedInt(parts[2]);
        SecretKey secretKey = pbkdf2.deriveSecretKeyAES(pass, salt, rounds, keyBits);
        return unwrap(parts, secretKey);
    }

    /**
     * Core decryption logic using a pre-derived key.
     *
     * @param parts The parts of the wrapped string, already split. The IV is at
     * index 3 and ciphertext at index 4.
     * @param key The {@link SecretKey} to use for decryption.
     * @return The original decrypted plaintext.
     * @throws GeneralSecurityException if decryption fails (e.g., the
     * authentication tag is invalid).
     */
    private byte[] unwrap(String[] parts, SecretKey key) throws GeneralSecurityException
    {
        byte[] iv = BASE64_DECODER.decode(parts[3]);
        byte[] wrappedCiphertext = BASE64_DECODER.decode(parts[4]);

        GCMParameterSpec ivGcm = kripto.getIvGCM(iv, GCM_TAG_BITS);
        Cipher cipher = kripto.getCipher(key, SecretKeyTransformation.AES_GCM_NoPadding, ivGcm, Cipher.DECRYPT_MODE);
        return cipher.doFinal(wrappedCiphertext);
    }

}
