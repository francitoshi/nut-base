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
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
    private final byte[] ikm;
    private final Rand rand;
    private final HKDF hkdf;

    public SecureWrapper(Kripto kripto, byte[] ikm, Hkdf hkdf)
    {
        this.kripto = kripto == null ? Kripto.getInstance(true) : kripto;
        this.ikm = ikm;
        this.rand = Kripto.getRand();
        this.hkdf = this.kripto.getHKDF(hkdf);
    }

    /**
     * Constructor to create an instance with default parameters.
     */
    public SecureWrapper(byte[] ikm)
    {
        this(null, ikm, Hkdf.HkdfWithSha512);
    }

    public String wrap(byte[] plaintext, byte[] info)
    {
        byte[] salt = this.rand.nextBytes(new byte[SALT_BYTES]);
        SecretKey secretKey = hkdf.deriveSecretKeyAES(ikm, salt, info, ikm.length);

        String encodedSalt = BASE64_ENCODER.encodeToString(salt);
        String encodedInfo = BASE64_ENCODER.encodeToString(info);
        String wrappedCiphertext = wrap(plaintext, secretKey);
        return String.join(SEPARATOR, FORMAT_VERSION, encodedSalt, encodedInfo, wrappedCiphertext);
    }
    
    public String wrap(byte[] plaintext, String info)
    {
        return wrap(plaintext, info.getBytes(StandardCharsets.UTF_8));
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
    private String wrap(byte[] plaintext, SecretKey key)
    {
        try
        {
            byte[] iv = this.rand.nextBytes(new byte[GCM_IV_BYTES]);
            
            GCMParameterSpec ivGcm = this.kripto.getIvGCM(iv, GCM_TAG_BITS);
            Cipher cipher = this.kripto.getCipher(key, SecretKeyTransformation.AES_GCM_NoPadding, ivGcm, Cipher.ENCRYPT_MODE);
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            String encodedIv = BASE64_ENCODER.encodeToString(iv);
            String encodedCiphertext = BASE64_ENCODER.encodeToString(ciphertext);
            
            return String.join(SEPARATOR, encodedIv, encodedCiphertext);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public byte[] unwrap(String wrappedtext, byte[] info)
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
        SecretKey secretKey = hkdf.deriveSecretKeyAES(ikm, salt, info, ikm.length);
        return unwrap(parts, secretKey);
    }
    
    public byte[] unwrap(String wrappedtext, String info)
    {
        return unwrap(wrappedtext, info.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] unwrap(String[] parts, SecretKey key)
    {
        try
        {
            byte[] iv = BASE64_DECODER.decode(parts[3]);
            byte[] wrappedCiphertext = BASE64_DECODER.decode(parts[4]);
            
            GCMParameterSpec ivGcm = kripto.getIvGCM(iv, GCM_TAG_BITS);
            Cipher cipher = kripto.getCipher(key, SecretKeyTransformation.AES_GCM_NoPadding, ivGcm, Cipher.DECRYPT_MODE);
            return cipher.doFinal(wrappedCiphertext);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

}
