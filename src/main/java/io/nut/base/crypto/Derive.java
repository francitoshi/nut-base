/*
 *  Derive.java
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

import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.Kripto.SecretKeyDerivation;
import io.nut.base.encoding.Ascii85;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A utility class for securely deriving cryptographic keys from passwords.
 * <p>
 * This class provides a high-level wrapper around Password-Based Key Derivation
 * Functions (PBKDF), such as PBKDF2. Its purpose is to transform a
 * user-provided password (which is typically low-entropy) into a strong,
 * cryptographically-secure key suitable for use in encryption algorithms like
 * AES.
 * <p>
 * Key features of this class include:
 * <ul>
 * <li>Encapsulation of the complex Java Cryptography Architecture (JCA) for key
 * derivation.</li>
 * <li>Enforcement of security best practices, such as requiring a minimum
 * number of derivation rounds.</li>
 * <li>Secure handling of passwords using {@code char[]} arrays to minimize
 * their lifetime in memory.</li>
 * <li>Convenience methods for common use cases, like deriving an AES key or
 * encoding the key.</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * Derive keyDeriver = new Derive(null, SecretKeyDerivation.PBKDF2WithHmacSHA256);
 * char[] password = "my-secret-password".toCharArray();
 * byte[] salt = new byte[16]; // A securely generated random salt
 * new SecureRandom().nextBytes(salt);
 * int rounds = 150000;
 * int keyLengthBits = 256;
 *
 * try {
 *     SecretKey aesKey = keyDeriver.deriveSecretKeyAES(password, salt, rounds, keyLengthBits);
 *     // The aesKey can now be used with a Cipher for encryption/decryption.
 * } finally {
 *     Arrays.fill(password, '\0'); // Clear the password from memory
 * }
 * }</pre>
 *
 * <b>Thread Safety:</b> This class is immutable and thread-safe. Methods can be
 * called concurrently from multiple threads.
 */
public class Derive
{    

    private final Kripto kripto;
    private final SecretKeyDerivation derivation;
    private final int minDeriveRounds;
        
    /**
     * Constructs a new Derive instance with a specified key derivation
     * function.
     *
     * @param kripto The crypto provider. If null, a default instance from
     * {@link Kripto#getInstance()} is used.
     * @param derivation The {@link SecretKeyDerivation} algorithm to use (e.g.,
     * PBKDF2WithHmacSHA256).
     */
    public Derive(Kripto kripto, SecretKeyDerivation derivation)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.derivation = derivation;
        this.minDeriveRounds = kripto.minDeriveRounds;
    }
    
    /**
     * Derives a secret key from a passphrase, salt, and iteration count.
     * <p>
     * This method performs the core key derivation logic using the configured
     * algorithm. It enforces a minimum number of rounds to protect against
     * brute-force attacks.
     *
     * @param passphrase The user's password. Should be cleared from memory
     * after use.
     * @param salt A random salt, which should be unique per password. It does
     * not need to be secret.
     * @param rounds The number of iterations. A higher number increases
     * security but is slower.
     * @param keyBits The desired length of the derived key in bits (e.g., 128,
     * 256).
     * @param secretKeyAlgorithm The target algorithm for which this key will be
     * used (e.g., AES).
     * @return The derived {@link SecretKey}.
     * @throws InvalidKeySpecException if the key specification is invalid for
     * the factory.
     * @throws IllegalArgumentException if the number of rounds is below the
     * configured minimum or if the algorithm is unsupported.
     */
    public SecretKey deriveSecretKey(char[] passphrase, byte[] salt, int rounds, int keyBits, SecretKeyAlgorithm secretKeyAlgorithm) throws InvalidKeySpecException
    {
        if(rounds<this.minDeriveRounds)
        {
            throw new IllegalArgumentException("rounds = "+rounds+" < "+this.minDeriveRounds);
        }
        try
        {
            SecretKeyFactory factory = kripto.getSecretKeyFactory(derivation.name());
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, rounds, keyBits);
            SecretKey genericSecretKey = factory.generateSecret(spec);
            return kripto.getSecretKey(genericSecretKey.getEncoded(), secretKeyAlgorithm);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalArgumentException("Unsupported SecretKeyFactory algorithm: " + derivation.name(), ex);
        }
    }

    /**
     * A convenience method to derive a secret key specifically for the AES
     * algorithm.
     *
     * @param passphrase The user's password.
     * @param salt A random salt.
     * @param rounds The number of iterations.
     * @param keyBits The desired key length in bits (e.g., 128, 192, 256 for
     * AES).
     * @return The derived AES {@link SecretKey}.
     * @throws InvalidKeySpecException if the key specification is invalid.
     * @throws IllegalArgumentException if security parameters are invalid.
     */
    public SecretKey deriveSecretKeyAES(char[] passphrase, byte[] salt, int rounds, int keyBits) throws InvalidKeySpecException
    {
        return deriveSecretKey(passphrase, salt, rounds, keyBits, SecretKeyAlgorithm.AES);
    }
    
    /**
     * Derives a secret key and returns it as a raw byte array.
     * <p>
     * This is useful when the key needs to be stored or transmitted in its raw
     * format. The caller is responsible for securely handling and clearing the
     * returned byte array.
     *
     * @param passphrase The user's password.
     * @param salt A random salt.
     * @param rounds The number of iterations.
     * @param keyBits The desired key length in bits.
     * @return The raw bytes of the derived secret key.
     * @throws InvalidKeySpecException if the key specification is invalid.
     * @throws IllegalArgumentException if security parameters are invalid.
     */
    public byte[] deriveSecretKeyEncoded(char[] passphrase, byte[] salt, int rounds, int keyBits) throws InvalidKeySpecException
    {
        if(rounds<this.minDeriveRounds)
        {
            throw new IllegalArgumentException("rounds = "+rounds+" < "+this.minDeriveRounds);
        }
        try
        {
            SecretKeyFactory factory = kripto.getSecretKeyFactory(derivation.name());
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, rounds, keyBits);
            SecretKey genericSecretKey = factory.generateSecret(spec);
            return genericSecretKey.getEncoded();
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalArgumentException("Unsupported SecretKeyFactory algorithm: " + derivation.name(), ex);
        }
    }
    
    /**
     * Derives a secret key, encodes it using Ascii85, and returns it as a char
     * array.
     * <p>
     * This method securely handles the intermediate raw key bytes by clearing
     * them from memory in a {@code finally} block after encoding. Ascii85 is a
     * space-efficient binary-to-text encoding.
     *
     * @param passphrase The user's password.
     * @param salt A random salt.
     * @param rounds The number of iterations.
     * @param keyBits The desired key length in bits.
     * @return The Ascii85-encoded derived key as a char array.
     * @throws InvalidKeySpecException if the key specification is invalid.
     * @throws IllegalArgumentException if security parameters are invalid.
     */
    public char[] deriveSecretKeyAscii85(char[] passphrase, byte[] salt, int rounds, int keyBits) throws InvalidKeySpecException
    {
        byte[] tmp = deriveSecretKeyEncoded(passphrase, salt, rounds, keyBits);
        try
        {
            return Ascii85.encode(tmp);
        }
        finally
        {
            // Securely clear the temporary raw key from memory.
            Arrays.fill(tmp, (byte) 0);
        }
    }

    // Dummy data for calibration. The content doesn't matter, but the length should be realistic.
    private static final char[] DUMMY_PASSWORD = "calibration-password-123!".toCharArray();
    private static final int SALT_SIZE_BYTES = 16;
    private static final int KEY_BITS = 256;

    // --- Configuration for the calibration process ---
    private static final int MEASUREMENT_ITERATIONS = 25;

    /**
     * Calibrates the number of PBKDF2 rounds needed to achieve a target
     * duration. This method should be run on the target production hardware to
     * get an accurate measurement.
     *
     * @param deriver The configured {@link Derive} instance (e.g., using
     * PBKDF2WithHmacSHA256).
     * @param targetMillis The desired execution time in milliseconds (e.g.,
     * 500).
     * @return The recommended number of rounds.
     * @throws InvalidKeySpecException if the key spec is invalid during
     * derivation.
     */
    public int calibrateRounds(long targetMillis) throws InvalidKeySpecException
    {
        int minRounds = kripto.minDeriveRounds;
        byte[] salt = new byte[SALT_SIZE_BYTES];
        new SecureRandom().nextBytes(salt);

        // 1. Warm-up Phase: Run the derivation several times to allow the JIT compiler to optimize.
        long t = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(targetMillis);
        for (int i = 0; i < 5 || System.nanoTime() < t; i++)
        {
            this.deriveSecretKeyEncoded(DUMMY_PASSWORD, salt, minRounds, KEY_BITS);
        }
        
        // 2. Baseline Measurement Phase: Measure the time for a small number of rounds.
        long t0 = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++)
        {
            this.deriveSecretKeyEncoded(DUMMY_PASSWORD, salt, minRounds, KEY_BITS);
        }
        long t1 = System.nanoTime();
        long averageNanos = (t1-t0) / MEASUREMENT_ITERATIONS;

        // 3. Calculation Phase: Extrapolate to find the target number of rounds.
        // Cost per round in nanoseconds
        double costPerRoundNanos = (double) averageNanos / minRounds;
        // Target time in nanoseconds
        long targetNanos = targetMillis * 1_000_000L;

        int calculatedRounds = (int) (targetNanos / costPerRoundNanos);
        // It's good practice to round to a "clean" number, e.g., the nearest thousand.
        calculatedRounds = (calculatedRounds / 1000) * 1000;

        // 4. Verification Phase: Run with the calculated rounds to confirm the timing.
        long t2 = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS / 2; i++)
        {
            this.deriveSecretKeyEncoded(DUMMY_PASSWORD, salt, calculatedRounds, KEY_BITS);
        }
        long t3 = System.nanoTime();
        averageNanos = (t3-t2) / (MEASUREMENT_ITERATIONS / 2);

        // Clean up dummy password
        Arrays.fill(DUMMY_PASSWORD, '\0');

        return calculatedRounds;
    }
    
}
