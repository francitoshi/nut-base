/*
 *  AesGcmSerializer.java
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

import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer; // Using ByteBuffer for cleaner array handling is a good practice, but System.arraycopy is also correct and efficient.

/**
 * Implements the {@link BytesCipher} interface using the AES/GCM/NoPadding
 * algorithm.
 * <p>
 * This implementation provides authenticated encryption (AEAD), which ensures
 * both confidentiality (data is unreadable) and integrity/authenticity (data
 * cannot be tampered with undetected).
 * <p>
 * The output ciphertext is structured as
 * {@code [IV (12 bytes)] + [Encrypted Data + Auth Tag]}. A new,
 * cryptographically random 96-bit (12-byte) Initialization Vector (IV) is
 * generated for each encryption operation to ensure semantic security (the same
 * plaintext will result in a different ciphertext each time it is encrypted).
 *
 * @see BytesCipher
 */
public class AesGcmBytesCipher implements BytesCipher
{
    protected static final Rand RAND = Kripto.getRand();
    
    protected final Kripto kripto;
    protected final SecretKey key;

    // The recommended IV size for GCM is 96 bits (12 bytes) for performance reasons.
    private static final int GCM_IV_BITS = 96;
    private static final int GCM_IV_LENGTH = GCM_IV_BITS / 8;

    /**
     * Constructs an AesGcmBytesCipher with a default Kripto instance.
     *
     * @param key The secret key to be used for AES encryption and decryption.
     */
    public AesGcmBytesCipher(SecretKey key)
    {
        this(key, null);
    }

    /**
     * Constructs an AesGcmBytesCipher with a provided Kripto instance.
     *
     * @param key The secret key to be used for AES encryption and decryption.
     * @param kripto An optional Kripto helper instance. If null, a default
     * instance is created.
     */
    public AesGcmBytesCipher(SecretKey key, Kripto kripto)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.key = key;
    }

    /**
     * Encrypts the given plaintext using AES-GCM. A new random 12-byte IV is
     * generated for each operation and prepended to the ciphertext.
     *
     * @param plaintext The byte array to be encrypted.
     * @return The authenticated ciphertext, formatted as [IV] + [Encrypted
     * Data].
     * @throws Exception if any cryptographic error occurs during encryption.
     */
    @Override
    public byte[] encrypt(byte[] plaintext) throws Exception
    {
        // 1. Generate a new, cryptographically random IV for each encryption. This is critical for GCM security.
        byte[] iv = RAND.nextBytes(new byte[GCM_IV_LENGTH]);

        // 2. Prepare GCM parameters.
        GCMParameterSpec ivGCM = kripto.getIvGCM(iv, GCM_IV_BITS);

        // 3. Perform encryption. The result includes the authentication tag.
        byte[] encryptedData = kripto.encrypt(key, SecretKeyTransformation.AES_GCM_NoPadding, ivGCM, plaintext);

        // 4. Prepend the IV to the ciphertext. The receiver will need it for decryption.
        // Using ByteBuffer is an alternative to System.arraycopy for cleaner code.
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        return byteBuffer.array();
    }

    /**
     * Decrypts the given ciphertext and verifies its integrity using the
     * authentication tag. It expects the input to be formatted as
     * {@code [IV] + [Encrypted Data]}.
     *
     * @param ciphertext The byte array to be decrypted, which must include the
     * IV prefix.
     * @return The original plaintext byte array.
     * @throws Exception if decryption fails or, critically, if the
     * authentication tag is invalid, which indicates that the data has been
     * tampered with.
     */
    @Override
    public byte[] decrypt(byte[] ciphertext) throws Exception
    {
        // Using ByteBuffer for safer and cleaner extraction of IV and ciphertext.
        ByteBuffer byteBuffer = ByteBuffer.wrap(ciphertext);

        // 1. Extract the IV from the beginning of the ciphertext.
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        // 2. Extract the actual encrypted data (which also contains the authentication tag).
        byte[] encryptedData = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedData);

        // 3. Prepare GCM parameters with the extracted IV.
        GCMParameterSpec ivGCM = kripto.getIvGCM(iv, GCM_IV_BITS);

        // 4. Perform decryption. The underlying JCE provider will automatically
        // verify the authentication tag. If verification fails, it will throw an exception.
        return kripto.decrypt(key, Kripto.SecretKeyTransformation.AES_GCM_NoPadding, ivGCM, encryptedData);
    }
}
