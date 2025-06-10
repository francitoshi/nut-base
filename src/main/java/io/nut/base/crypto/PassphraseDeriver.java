/*
 *  PassphraseDeriver.java
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

import static io.nut.base.crypto.Kripto.SecretKeyTransformation.AES_GCM_NoPadding;
import io.nut.base.util.Byter;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A class to derive deterministic keys from a master passphrase and a label.
 * The master passphrase itself is encrypted in memory for added security. It
 * uses PBKDF2 for key derivation and caches the derived keys in an encrypted
 * format. The class is thread-safe and implements AutoCloseable for secure
 * resource cleanup.
 */
public final class PassphraseDeriver implements AutoCloseable
{
    // --- Constants for cryptographic algorithms ---
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits), as recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final byte[] MASTER_SALT = "master".getBytes(StandardCharsets.UTF_8);

    public static final int AES_KEY_SIZE = 256; // bits
    
    /**
     * Inner record to hold all necessary data for a cached key. This allows
     * storing the encrypted key, the key used for encryption, and the IV in a
     * single map value.
     */
    private static class CachedKeyData
    {
        final byte[] iv;
        final byte[] encryptedKey;
        final SecretKey protectionKey;
        public CachedKeyData(byte[] iv, byte[] encryptedKey, SecretKey protectionKey)
        {
            this.iv = iv;
            this.encryptedKey = encryptedKey;
            this.protectionKey = protectionKey;
        }
    }

    // --- Instance fields ---
    private final int keybits;
    private final int rounds;
    private final CachedKeyData master;
    private final Map<String, CachedKeyData> cache;

    private final Kripto kripto;

    /**
     * Constructs a new DeterministicKeyDeriver. The provided master passphrase
     * is immediately encrypted and stored in memory only in its encrypted form.
     * The original plaintext passphrase is not retained.
     *
     * @param masterPassphrase The master passphrase to derive keys from. It
     * will be cleared from memory after use.
     * @param keyBits The desired output key length in bits.
     * @param rounds The number of iterations for the PBKDF2 algorithm.
     * @param cache If the instance will cache keys
     * @throws GeneralSecurityException if the initial encryption of the master
     * passphrase fails.
     */
    public PassphraseDeriver(char[] masterPassphrase, int keyBits, int rounds, boolean cache) throws GeneralSecurityException
    {
        this(masterPassphrase, keyBits, rounds, cache, Kripto.getInstance());
    }
    PassphraseDeriver(char[] masterPassphrase, int keyBits, int rounds, boolean cache, Kripto kripto) throws GeneralSecurityException
    {
        this.kripto = kripto;
        this.keybits = keyBits;
        this.rounds = rounds;
        this.cache = (cache ? new HashMap<>() : null);

        // Immediately encrypt the master passphrase for secure in-memory storage.
        SecretKey protectionKey = generateAesKey();
        byte[] iv = generateIv();
        byte[] passphraseBytes = kripto.derivePassphrase(masterPassphrase, MASTER_SALT, rounds, keyBits, Kripto.SecretKeyDerivation.PBKDF2WithHmacSHA256);
        Arrays.fill(passphraseBytes, (byte) 0);
        byte[] encryptedKey = encrypt(passphraseBytes, protectionKey, iv);

        this.master = new CachedKeyData(iv, encryptedKey, protectionKey);
    }

    /**
     * Gets a derived key for a given label. The method first checks a local
     * cache. If the key is present, it's decrypted and returned. If not, it
     * derives a new key using PBKDF2, encrypts it with a new ephemeral key,
     * stores it in the cache, and then returns the derived key. This method is
     * synchronized to ensure thread-safety when accessing the cache.
     *
     * @param label A unique string identifier (e.g., "database-password",
     * "api-key") used as salt.
     * @return The derived key as a byte array.
     * @throws GeneralSecurityException if any cryptographic error occurs.
     */
    public synchronized byte[] getKey(String label) throws GeneralSecurityException
    {
        Objects.requireNonNull(label, "Label cannot be null");

        // 1. Check if the key is already in the cache.
        if (cache!=null && cache.containsKey(label))
        {
            CachedKeyData cachedData = cache.get(label);
            return decrypt(cachedData.encryptedKey, cachedData.protectionKey, cachedData.iv);
        }

        // Derive the key, which requires decrypting the master passphrase temporarily.
        byte[] derivedKey = deriveKeyFromPassphrase(label);

        if(cache!=null)
        {
            // Encrypt and cache the newly derived key.
            SecretKey protectionKey = generateAesKey();
            byte[] iv = generateIv();
            byte[] encryptedDerivedKey = encrypt(derivedKey, protectionKey, iv);
            cache.put(label, new CachedKeyData(iv, encryptedDerivedKey, protectionKey));
        }
        return derivedKey;
    }

    /**
     * Derives a key using PBKDF2. This method temporarily decrypts the master
     * passphrase in memory, uses it for derivation, and immediately clears the
     * plaintext version.
     *
     * @param saltLabel The label to be used as salt.
     * @return The derived key.
     * @throws NoSuchAlgorithmException if the PBKDF2 algorithm is not
     * available.
     * @throws InvalidKeySpecException if the key spec is invalid.
     */
    private byte[] deriveKeyFromPassphrase(String label) throws GeneralSecurityException
    {
        byte[] masterBytes = decrypt(this.master.encryptedKey, master.protectionKey, this.master.iv);
        try
        {
            char[] masterChars = Byter.charsUTF8(masterBytes);
            try
            {
                byte[] salt = label.getBytes(StandardCharsets.UTF_8);
                return kripto.derivePassphrase(masterChars, salt, rounds, keybits, Kripto.SecretKeyDerivation.PBKDF2WithHmacSHA256);
            }
            finally
            {
                Arrays.fill(masterChars, '\0');
            }
        }        
        finally
        {
            Arrays.fill(masterBytes, (byte) 0);
        }            

    }

    /**
     * A generic encryption method using AES/GCM/NoPadding.
     *
     * @param plaintext The plaintext data.
     * @param key The key to use for encryption.
     * @param iv The initialization vector.
     * @return The encrypted data (ciphertext).
     * @throws GeneralSecurityException on encryption failure.
     */
    private byte[] encrypt(byte[] plaintext, SecretKey key, byte[] iv) throws GeneralSecurityException
    {
        GCMParameterSpec gcm = kripto.getIvGCM(iv, GCM_TAG_LENGTH);
        return kripto.encrypt(key, AES_GCM_NoPadding, gcm, plaintext);
    }

    /**
     * A generic decryption method using AES/GCM/NoPadding.
     *
     * @param codedtext The ciphertext to decrypt.
     * @param key The key to use for decryption.
     * @param iv The initialization vector.
     * @return The decrypted data (plaintext).
     * @throws GeneralSecurityException on decryption failure.
     */
    private byte[] decrypt(byte[] codedtext, SecretKey key, byte[] iv) throws GeneralSecurityException
    {
        GCMParameterSpec gcm = kripto.getIvGCM(iv, GCM_TAG_LENGTH);
        return kripto.decrypt(key, AES_GCM_NoPadding, gcm, codedtext);
    }

    private SecretKey generateAesKey() throws NoSuchAlgorithmException
    {
        return kripto.getKeyGenerator(Kripto.SecretKeyAlgorithm.AES, keybits).generateKey();
    }

    private byte[] generateIv()
    {
        return Kripto.random(new byte[GCM_IV_LENGTH]);
    }

    /**
     * Clears all sensitive information from memory. This includes the encrypted
     * master passphrase, its encryption key, and the derived key cache.
     */
    @Override
    public void close()
    {
        // Overwrite all sensitive byte arrays with zeros.
        Arrays.fill(this.master.encryptedKey, (byte) 0);

        // Clear the cache. The cached data will be garbage collected.
        if(cache!=null)
        {
            cache.clear();
        }
    }

}
