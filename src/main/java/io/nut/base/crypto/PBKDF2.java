/*
 *  PBKDF2.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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

import io.nut.base.crypto.Kripto.Pbkdf2;
import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.encoding.Ascii85;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PBKDF2
{
    private final Kripto kripto;
    private final Pbkdf2 algorithm;

    public PBKDF2(Kripto kripto, Pbkdf2 algorithm)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.algorithm = algorithm;
    }

    public byte[] deriveSecretKeyEncoded(char[] password, byte[] salt, int rounds, int keyBits)
    {
        try
        {
            KeySpec spec = new PBEKeySpec(password, salt, rounds, keyBits);
            SecretKeyFactory factory = this.kripto.getSecretKeyFactory(this.algorithm.name());
            return factory.generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException ex)
        {
            Logger.getLogger(PBKDF2.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }


    public SecretKey deriveSecretKey(char[] password, byte[] salt, int rounds, int keyBits, Kripto.SecretKeyAlgorithm keyAlgorithm)
    {
        byte[] bytes = deriveSecretKeyEncoded(password, salt, rounds, keyBits);
        try
        {
            return new SecretKeySpec(bytes, keyAlgorithm.name());
        }
        finally
        {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    public SecretKey deriveSecretKeyAES(char[] password, byte[] salt, int rounds, int keyBits) 
    {
        return deriveSecretKey(password, salt, rounds, keyBits, SecretKeyAlgorithm.AES);
    }
    
    public char[] deriveSecretKeyAscii85(char[] password, byte[] salt, int rounds, int keyBits) throws InvalidKeySpecException
    {
        byte[] bytes = deriveSecretKeyEncoded(password, salt, rounds, keyBits);
        try
        {
            return Ascii85.encode(bytes);
        }
        finally
        {
            // Securely clear the temporary raw key from memory.
            Arrays.fill(bytes, (byte) 0);
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
     * @param targetMillis The desired execution time in milliseconds (e.g., 500).
     * @return The recommended number of rounds.
     * @throws InvalidKeySpecException if the key spec is invalid during
     * derivation.
     */
    public int calibrateRounds(long targetMillis) throws InvalidKeySpecException
    {
        int minRounds = 2048;
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
        
        long targetNanos = TimeUnit.MILLISECONDS.toNanos(targetMillis);

        int calculatedRounds = (int) (targetNanos / costPerRoundNanos);
        // It's good practice to round to a "clean" number, e.g., the nearest thousand.
        calculatedRounds = (calculatedRounds / 1000) * 1000;
        
        // 4. Verification Phase: Run with the calculated rounds to confirm the timing.
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++)
        {
            t0 = System.nanoTime();
            this.deriveSecretKeyEncoded(DUMMY_PASSWORD, salt, calculatedRounds, KEY_BITS);
            t1 = System.nanoTime();
            long millis = TimeUnit.NANOSECONDS.toMillis(t1-t0);
            if(millis<targetMillis)
            {
                return calculatedRounds;
            }
            calculatedRounds = (calculatedRounds * 1100) / 1000;
        }

        return calculatedRounds;
    }
}
