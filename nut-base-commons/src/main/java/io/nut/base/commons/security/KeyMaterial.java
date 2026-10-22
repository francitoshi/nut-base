/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.security;

import io.nut.base.commons.io.IO;
import io.nut.base.commons.jca.Kr;
import io.nut.base.commons.jca.Rand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Helpers to create, derive, store and load symmetric key material.
 * <p>
 * The store never persists the key of the data it protects: the key is
 * always supplied by the caller, and this class only provides the building
 * blocks to obtain one. That separation keeps the trust decision (where does
 * the key come from?) outside of the code that uses it.
 * <p>
 * Three sources are supported:
 * <ul>
 * <li><b>Random key</b> ({@link #randomKey()}): 256 bits from
 * {@link java.security.SecureRandom}. Use it when the key is stored in a
 * keystore or in a protected file.</li>
 * <li><b>Derived key</b> ({@link #deriveKey(char[], byte[], int)}): PBKDF2
 * with HMAC-SHA256 over a random salt. Use it when the secret is a
 * human-memorable passphrase. Note that PBKDF2 is compute-bound but not
 * memory-hard, so it is much weaker against GPU/ASIC attacks than Argon2id or
 * scrypt, which are not available in the JDK this library targets.</li>
 * <li><b>Key file</b> ({@link #writeKeyFile(Path, byte[])} and
 * {@link #readKeyFile(Path)}): the raw key bytes in a file readable only by
 * its owner. The file is created with {@code 0600} permissions and replaced
 * atomically.</li>
 * </ul>
 * <p>
 * Key material is returned as plain {@code byte[]} arrays, so the caller owns
 * them and is responsible for wiping them with {@link Wiper#wipe(byte[])} once
 * they are no longer needed.
 *
 * @see SecureSecretKey
 * @see SecureBytes
 */
public final class KeyMaterial
{
    /** Size of the generated keys, in bits. */
    public static final int KEY_BITS = 256;

    /** Size of the generated keys, in bytes. */
    public static final int KEY_BYTES = KEY_BITS / 8;

    /** Size of the PBKDF2 salt, in bytes. */
    public static final int SALT_BYTES = 16;

    /**
     * Default number of PBKDF2 iterations. It is deliberately lower than the
     * 600.000 rounds recommended by OWASP for passwords, because this value
     * is paid on every open of the protected resource; raise it whenever the
     * secret is really a human password.
     */
    public static final int DEFAULT_PBKDF2_ITERATIONS = 100_000;

    private static final Rand RAND = Rand.getInstance();
    private static final Kr KRIPTO = Kr.getInstance();

    private KeyMaterial()
    {
    }

    /**
     * Generates a new random AES-256 key.
     *
     * @return 32 random bytes; the caller owns and must wipe the array
     */
    public static byte[] randomKey()
    {
        return RAND.nextBytes(new byte[KEY_BYTES]);
    }

    /**
     * Generates a new random salt for {@link #deriveKey(char[], byte[], int)}.
     *
     * @return {@value #SALT_BYTES} random bytes; the caller owns and must
     *         wipe the array
     */
    public static byte[] randomSalt()
    {
        return RAND.nextBytes(new byte[SALT_BYTES]);
    }

    /**
     * Derives an AES-256 key from a passphrase with PBKDF2-HMAC-SHA256.
     * <p>
     * The passphrase is never converted to a {@code String}, so it stays in
     * memory only as a {@code char[]} that the caller can wipe.
     *
     * @param passphrase the secret; the caller owns and should wipe it
     * @param salt       a random salt, at least 16 bytes; the caller owns and
     *                   should wipe it
     * @param iterations the number of iterations; must be positive
     * @return the derived 32-byte key; the caller owns and must wipe it
     * @throws IllegalArgumentException if the passphrase or the salt is
     *                                  empty or the iteration count is not
     *                                  positive
     */
    public static byte[] deriveKey(char[] passphrase, byte[] salt, int iterations)
    {
        Objects.requireNonNull(passphrase, "passphrase");
        Objects.requireNonNull(salt, "salt");
        if (passphrase.length == 0)
        {
            throw new IllegalArgumentException("passphrase cannot be empty");
        }
        if (salt.length == 0)
        {
            throw new IllegalArgumentException("salt cannot be empty");
        }
        if (iterations <= 0)
        {
            throw new IllegalArgumentException("iterations must be positive");
        }
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, KEY_BITS);
        try
        {
            SecretKeyFactory factory = KRIPTO.getSecretKeyFactory(Kr.Pbkdf2.PBKDF2WithHmacSHA256);
            return factory.generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            throw new IllegalStateException("cannot derive the key with PBKDF2WithHmacSHA256", e);
        }
        finally
        {
            spec.clearPassword();
        }
    }

    /**
     * Wraps raw key bytes into an AES {@link SecretKey}.
     *
     * @param key the raw key bytes; 16, 24 or 32 bytes are accepted
     * @return a new {@link SecretKey}
     * @throws IllegalArgumentException if the key is null, empty or has an
     *                                  invalid length
     */
    public static SecretKey aesKey(byte[] key)
    {
        Objects.requireNonNull(key, "key");
        if (key.length != 16 && key.length != 24 && key.length != 32)
        {
            throw new IllegalArgumentException("invalid AES key length: " + key.length);
        }
        return new SecureSecretKey(key, "AES");
    }

    /**
     * Writes raw key bytes to a file that only its owner can read.
     * <p>
     * The file is written to a temporary sibling and then moved into place,
     * so a crash never leaves a partially written key behind. On POSIX
     * filesystems the file is created with {@code 0600} permissions; on
     * filesystems without POSIX permissions the operation still succeeds but
     * the protection is the one of the directory.
     *
     * @param file the file to write
     * @param key  the raw key bytes
     * @throws IOException if the file cannot be written
     */
    public static void writeKeyFile(Path file, byte[] key) throws IOException
    {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(key, "key");
        if (key.length == 0)
        {
            throw new IllegalArgumentException("key cannot be empty");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null)
        {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        IO.writePrivate(tmp, key);
        IO.replaceAtomically(tmp, file);
    }

    /**
     * Reads raw key bytes from a file written by
     * {@link #writeKeyFile(Path, byte[])}.
     *
     * @param file the file to read
     * @return the raw key bytes; the caller owns and must wipe them
     * @throws IOException if the file cannot be read or does not hold a
     *                     valid key length
     */
    public static byte[] readKeyFile(Path file) throws IOException
    {
        Objects.requireNonNull(file, "file");
        byte[] key = Files.readAllBytes(file);
        if (key.length != 16 && key.length != 24 && key.length != 32)
        {
            Arrays.fill(key, (byte) 0);
            throw new IOException("invalid key file contents (length " + key.length + "): " + file);
        }
        return key;
    }
}
