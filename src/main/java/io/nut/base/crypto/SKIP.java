/*
 *  SKIP.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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

import io.nut.base.crypto.Kripto.Hmac;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.Base64;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

/**
 * <h1>Shared Key Identity Protocol (SKIP)</h1>
 * 
 * <p>SKIP provides a secure mechanism for encrypting and authenticating messages 
 * using a pre-shared secret. It utilizes AES-GCM for encryption and HMAC-SHA256 
 * for identity verification and message integrity.</p>
 * 
 * <p>Key features include:
 * <ul>
 *     <li>Key separation: Distinct keys for encryption and MAC derived via HKDF-style logic.</li>
 *     <li>Replay protection: Uses a Unix timestamp with a 7-day TTL (Time-To-Live).</li>
 *     <li>Memory safety: Defensive copying of secrets and explicit zeroing of sensitive arrays.</li>
 *     <li>Authentication: Constant-time HMAC comparison to prevent timing attacks.</li>
 * </ul>
 * </p>
 */
public class SKIP
{
    /** Salt used for the initial Key Derivation Function. */
    private static final byte[] SALT = "SKIP-v1".getBytes(StandardCharsets.UTF_8);
    
    /** Default expiration window for messages (7 days). */
    private static final long TTL_SECONDS = 7 * 24 * 3600;

    private final Kripto kripto;
    private final char[] sharedSecret;
    private final Function<char[], byte[]> kdf;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Constructs a SKIP instance with a custom Key Derivation Function.
     *
     * @param kripto       The cryptographic provider utility.
     * @param sharedSecret The secret shared between sender and receiver.
     * @param kdf          A function to derive the Initial Keying Material (IKM).
     */
    public SKIP(Kripto kripto, char[] sharedSecret, Function<char[], byte[]> kdf)
    {
        this.kripto = kripto != null ? kripto : Kripto.getInstance();
        // Defensive copy to ensure the original array isn't modified/held elsewhere
        this.sharedSecret = Arrays.copyOf(sharedSecret, sharedSecret.length);
        this.kdf = kdf;
    }

    /**
     * Convenience constructor that uses the default {@link #hkdfExpand(char[])} derivation.
     *
     * @param kripto     The cryptographic provider utility.
     * @param passphrase The secret shared between sender and receiver.
     */
    public SKIP(Kripto kripto, char[] passphrase)
    {
        this.kripto = kripto != null ? kripto : Kripto.getInstance();
        this.sharedSecret = Arrays.copyOf(passphrase, passphrase.length);
        this.kdf = this::hkdfExpand;
    }

    /**
     * Default KDF implementation.
     * <p>Converts the passphrase to UTF-8 bytes, derives an IKM using HMAC-SHA256 
     * with a fixed SALT, and clears temporary buffers immediately.</p>
     *
     * @param passphrase The secret characters to derive.
     * @return A 32-byte array containing the derived material.
     */
    public byte[] hkdfExpand(char[] passphrase)
    {
        byte[] ppBytes = toBytes(passphrase);
        try
        {
            SecretKey key = kripto.getSecretKey(SALT, Hmac.HmacSHA256);
            Mac mac = kripto.getMac(Hmac.HmacSHA256, key);
            return mac.doFinal(ppBytes);
        }
        catch (IllegalStateException ex)
        {
            throw new RuntimeException("hkdfExpand failed", ex);
        }
        finally
        {
            Arrays.fill(ppBytes, (byte) 0); // Clear sensitive bytes
        }
    }

    /**
     * Derives the encryption sub-key from the shared secret.
     *
     * @return 32 bytes to be used as an AES-256 key.
     */
    public byte[] deriveEncKey()
    {
        byte[] ikm = kdf.apply(sharedSecret);
        try
        {
            return expand(ikm, (byte) 0x01);
        }
        finally
        {
            Arrays.fill(ikm, (byte) 0);
        }
    }

    /**
     * Derives the MAC sub-key from the shared secret.
     *
     * @return 32 bytes to be used for HMAC.
     */
    public byte[] deriveMacKey()
    {
        byte[] ikm = kdf.apply(sharedSecret);
        try
        {
            return expand(ikm, (byte) 0x02);
        }
        finally
        {
            Arrays.fill(ikm, (byte) 0);
        }
    }

    /**
     * Internal HKDF-style expansion.
     * 
     * @param ikm  Initial Keying Material.
     * @param info Context byte to differentiate derived keys.
     * @return The expanded key bytes.
     */
    private byte[] expand(byte[] ikm, byte info)
    {
        try
        {
            SecretKey key = kripto.getSecretKey(SALT, Hmac.HmacSHA256);
            Mac mac = kripto.getMac(Hmac.HmacSHA256, key);

            byte[] input = new byte[ikm.length + 1];
            System.arraycopy(ikm, 0, input, 0, ikm.length);
            input[ikm.length] = info;
            return mac.doFinal(input);
        }
        catch (IllegalStateException e)
        {
            throw new RuntimeException("expand failed", e);
        }
    }

    /**
     * Encrypts and packages a payload into a SKIP message.
     *
     * @param epochSecond The creation timestamp (Unix epoch).
     * @param payload     The plaintext message string.
     * @return A Base64 encoded string containing [Timestamp | IV | Ciphertext | HMAC].
     * @throws Exception If encryption or key derivation fails.
     */
    public String buildMessage(long epochSecond, String payload) throws Exception
    {
        byte[] epochSecondBytes = ByteBuffer.allocate(8).putLong(epochSecond).array();

        byte[] kEnc = deriveEncKey();
        try
        {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            GCMParameterSpec ivGCM = kripto.getIvGCM(iv, 128);
            SecretKey secretKey = kripto.getSecretKey(kEnc, Kripto.SecretKeyAlgorithm.AES);
            Cipher cipher = kripto.getCipher(secretKey, SecretKeyTransformation.AES_GCM_NoPadding, ivGCM, Cipher.ENCRYPT_MODE);
            
            byte[] ciphertext = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            byte[] hmac = computeHmac(epochSecondBytes, iv, ciphertext);

            ByteBuffer buf = ByteBuffer.allocate(8 + 12 + ciphertext.length + 32);
            buf.put(epochSecondBytes).put(iv).put(ciphertext).put(hmac);

            return Base64.getEncoder().encodeToString(buf.array());
        }
        finally
        {
            Arrays.fill(kEnc, (byte) 0);
        }
    }

    /**
     * Receives, validates, and decrypts a SKIP message.
     * <p>Checks performed:
     * <ol>
     *     <li>Temporal validation: Message must not be in the future and not older than 7 days.</li>
     *     <li>Integrity validation: HMAC is verified using constant-time comparison.</li>
     * </ol>
     * </p>
     *
     * @param base64Msg The Base64 SKIP message string.
     * @return The decrypted plaintext.
     * @throws SecurityException If the message is expired, in the future, or the HMAC is invalid.
     * @throws Exception         If decryption or data parsing fails.
     */
    public String receiveMessage(long epochSecond, String base64Msg) throws Exception
    {
        return receiveMessage(epochSecond, base64Msg, TTL_SECONDS);
    }
    public String receiveMessage(long epochSecond, String base64Msg, long ttlSeconds) throws Exception
    {
        byte[] raw = Base64.getDecoder().decode(base64Msg);
        if (raw.length < 8 + 12 + 32)
        {
            throw new IllegalArgumentException("Message too short");
        }

        ByteBuffer buf = ByteBuffer.wrap(raw);

        byte[] t0Bytes = new byte[8];
        buf.get(t0Bytes);
        long t0 = ByteBuffer.wrap(t0Bytes).getLong();

        byte[] iv = new byte[12];
        buf.get(iv);

        int cipherLen = raw.length - 8 - 12 - 32;
        byte[] ciphertext = new byte[cipherLen];
        buf.get(ciphertext);

        byte[] hmacReceived = new byte[32];
        buf.get(hmacReceived);

        // 1. Time window validation
        if (t0 > epochSecond)
        {
            throw new SecurityException("Future timestamp detected");
        }
        if (epochSecond - t0 > ttlSeconds)
        {
            throw new SecurityException("Message expired (> "+ttlSeconds+"s)");
        }

        // 2. Verify HMAC in constant time
        byte[] hmacExpected = computeHmac(t0Bytes, iv, ciphertext);
        if (!MessageDigest.isEqual(hmacExpected, hmacReceived))
        {
            throw new SecurityException("Invalid HMAC: message tampered with or unauthorized");
        }

        // 3. Decrypt
        byte[] kEnc = deriveEncKey();
        try
        {
            GCMParameterSpec ivGCM = kripto.getIvGCM(iv, 128);
            SecretKey secretKey = kripto.getSecretKey(kEnc, Kripto.SecretKeyAlgorithm.AES);
            Cipher cipher = kripto.getCipher(secretKey, SecretKeyTransformation.AES_GCM_NoPadding, ivGCM, Cipher.DECRYPT_MODE);

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        }
        finally
        {
            Arrays.fill(kEnc, (byte) 0);
        }
    }

    /**
     * Explicitly clears the shared secret from memory.
     * Should be called when the SKIP instance is no longer needed.
     */
    public void destroy()
    {
        Arrays.fill(sharedSecret, '\0');
    }

    /**
     * Computes the HMAC-SHA256 for the message components.
     * 
     * @param epochSecondBytes The 8-byte timestamp.
     * @param iv               The 12-byte initialization vector.
     * @param ciphertext       The encrypted payload.
     * @return The 32-byte HMAC.
     */
    private byte[] computeHmac(byte[] epochSecondBytes, byte[] iv, byte[] ciphertext)
    {
        byte[] kMac = deriveMacKey();
        try
        {
            SecretKey key = kripto.getSecretKey(kMac, Hmac.HmacSHA256);
            Mac mac = kripto.getMac(Hmac.HmacSHA256, key);
            mac.update(epochSecondBytes);
            mac.update(iv);
            mac.update(ciphertext);
            return mac.doFinal();
        }
        catch (Exception e)
        {
            throw new RuntimeException("computeHmac failed", e);
        }
        finally
        {
            Arrays.fill(kMac, (byte) 0);
        }
    }

    /**
     * Safely converts a char array to a UTF-8 byte array.
     * <p>This avoids creating intermediate String objects that would reside in the
     * memory pool and cannot be cleared.</p>
     *
     * @param chars The sensitive character array.
     * @return The corresponding UTF-8 byte array.
     */
    static byte[] toBytes(char[] chars)
    {
        CharBuffer cb = CharBuffer.wrap(chars);
        ByteBuffer bb = StandardCharsets.UTF_8.encode(cb);
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        // Clear the internal array of the ByteBuffer if possible
        if (bb.hasArray())
        {
            Arrays.fill(bb.array(), (byte) 0);
        }
        return bytes;
    }
}
