/*
 *  SecureBytes.java
 *
 *  Copyright (C) 2025-2026 francitoshi@gmail.com
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
package io.nut.base.security;

import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import io.nut.base.crypto.Rand;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.Destroyable;

/**
 * Holds a byte array securely in memory by encrypting it with AES-256-GCM
 * using an ephemeral key that lives only for the duration of this object's
 * lifetime.
 *
 * <p>The plaintext is encrypted immediately on construction and the original
 * array is zeroed out. The decrypted data is only ever reconstructed
 * transiently (inside {@link #getBytes()} or {@link #consume(Consumer)}) and
 * is zeroed again as soon as the caller is done with it.</p>
 *
 * <p>Instances must be explicitly released by calling {@link #destroy()} or
 * by using a try-with-resources block (the class implements
 * {@link AutoCloseable}). After destruction all internal state (key, IV and
 * ciphertext) is overwritten with zeroes.</p>
 *
 * <p>Thread safety: {@code destroy()} is guarded by a {@code volatile} flag;
 * all other methods are <em>not</em> synchronized and should not be called
 * concurrently.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * byte[] secret = obtainSecret();
 * try (SecureBytes sb = new SecureBytes(secret)) {
 *     // secret[] has already been zeroed by the constructor
 *     sb.consume(data -> process(data));
 *     // data[] is zeroed immediately after the lambda returns
 * }
 * // sb is now destroyed; key, IV and ciphertext are all zeroed
 * }</pre>
 *
 * @see SecureChars
 */
public class SecureBytes implements AutoCloseable, Destroyable
{
    /**
     * Lazy-initialized singleton holder for the shared {@link Kripto} instance.
     * Using an enum guarantees thread-safe, exactly-once initialization without
     * explicit synchronization.
     */
    private enum Holder
    {
        INSTANCE;
        Kripto kripto = Kripto.getInstance();
    }

    /** Length of the AES-GCM initialization vector in bytes (96 bits). */
    private static final int IV_BYTES = 12; //96 bits

    /** Length of the AES-GCM authentication tag in bits. */
    private static final int TAG_BITS = 128;

    /** Shared cryptographically-secure random generator. */
    private static final Rand RAND = Kripto.getRand();

    /** Cryptographic utilities used to create keys, IVs and ciphers. */
    private final Kripto kripto;

    /** Random IV generated fresh for every instance. */
    private final byte[] iv;

    /** Ephemeral AES-256 key used solely for this object's encrypted payload. */
    private final SecretKey key;

    /** AES-256-GCM ciphertext of the original data, including the authentication tag. */
    private final byte[] encryptedData;

    /**
     * {@code true} once {@link #destroy()} has been called and all sensitive
     * material has been wiped. Declared {@code volatile} so that
     * {@link #isDestroyed()} is always visible across threads.
     */
    private volatile boolean destroyed;

    /**
     * Constructs a {@code SecureBytes} instance that encrypts {@code data}
     * with AES-256-GCM using the provided {@link Kripto} instance (or the
     * shared default if {@code null}).
     *
     * <p>The {@code data} array is zeroed immediately after encryption. Passing
     * {@code null} creates a destroyed instance that behaves as if it wraps a
     * {@code null} byte array. Passing an empty array creates a special-case
     * instance that returns an empty array from {@link #getBytes()} without
     * performing any cryptographic operation.</p>
     *
     * @param data   the plaintext byte array to protect; may be {@code null}
     *               or empty. The array is zeroed by this constructor.
     * @param kripto the {@link Kripto} instance to use for key generation and
     *               cipher operations; if {@code null} the shared singleton is used.
     * @throws RuntimeException wrapping any JCA exception that occurs during
     *                          key generation or encryption.
     */
    public SecureBytes(byte[] data, Kripto kripto)
    {
        try
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
                this.encryptedData = new byte[0];
                this.destroyed = true;
                return;
            }

            this.kripto = kripto==null ? Holder.INSTANCE.kripto : kripto;
            this.key = this.kripto.keyGenAes256.generateKey();
            this.iv = RAND.nextBytes(new byte[IV_BYTES]);
            
            GCMParameterSpec spec = this.kripto.getIvGCM(iv, TAG_BITS);
            Cipher cipher = this.kripto.getCipher(this.key, SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.ENCRYPT_MODE);
            this.encryptedData = cipher.doFinal(data);
            
            Arrays.fill(data, (byte) 0);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Constructs a {@code SecureBytes} instance using the shared default
     * {@link Kripto} instance.
     *
     * <p>Equivalent to {@code new SecureBytes(data, null)}.</p>
     *
     * @param data the plaintext byte array to protect; may be {@code null}
     *             or empty. The array is zeroed by this constructor.
     * @throws RuntimeException wrapping any JCA exception that occurs during
     *                          key generation or encryption.
     */
    public SecureBytes(byte[] data)
    {
        this(data, Holder.INSTANCE.kripto);
    }

    /**
     * Decrypts and returns the protected byte array.
     *
     * <p>The returned array is a freshly allocated buffer containing the
     * original plaintext. The caller is responsible for zeroing it when
     * finished (prefer {@link #consume(Consumer)} which does this
     * automatically).</p>
     *
     * <p>Package-private visibility is intentional: external code must use
     * {@link #consume(Consumer)} to guarantee that the plaintext is wiped
     * after use.</p>
     *
     * @return the decrypted plaintext, or {@code null} if this instance was
     *         constructed with a {@code null} array, or an empty array if it
     *         was constructed with an empty array.
     * @throws RuntimeException wrapping any JCA exception that occurs during
     *                          decryption.
     */
    // keep private for outsiders
    byte[] getBytes()
    {
        if(this.encryptedData==null)
        {
            return null;
        }
        if(this.encryptedData.length==0)
        {
            return this.encryptedData;
        }
        try
        {
            GCMParameterSpec spec = this.kripto.getIvGCM(iv, TAG_BITS);
            Cipher cipher = this.kripto.getCipher(this.key, SecretKeyTransformation.AES_GCM_NoPadding, spec, Cipher.DECRYPT_MODE);
            return cipher.doFinal(this.encryptedData);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Destroys this instance by zeroing all sensitive material (ciphertext,
     * IV and secret key) and marking the object as destroyed.
     *
     * <p>Subsequent calls to this method are no-ops. After this method
     * returns, {@link #isDestroyed()} will return {@code true} and any call
     * to {@link #getBytes()} or {@link #consume(Consumer)} may fail or
     * produce undefined results.</p>
     */
    @Override
    public void destroy()
    {
        if(!this.destroyed)
        {
            Arrays.fill(this.encryptedData, (byte) 0);
            Arrays.fill(this.iv, (byte) 0);
            Wiper.wipeSecretKey(this.key);
            this.destroyed=true;
        }
    }

    /**
     * Returns {@code true} if this instance has been destroyed and all
     * sensitive material has been wiped from memory.
     *
     * @return {@code true} after {@link #destroy()} (or {@link #close()})
     *         has been called; {@code false} otherwise.
     */
    @Override
    public boolean isDestroyed()
    {
        return this.destroyed;
    }

    /**
     * Implements {@link AutoCloseable} by delegating to {@link #destroy()},
     * enabling use in try-with-resources statements.
     */
    @Override
    public void close()
    {
        this.destroy();
    }

    /**
     * Decrypts the protected data, passes it to {@code consumer}, and then
     * zeros the temporary plaintext buffer before returning — even if the
     * consumer throws an exception.
     *
     * <p>This is the preferred way to access the protected data because it
     * guarantees that the plaintext does not linger on the heap longer than
     * necessary.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * secureBytes.consume(data -> sendOverNetwork(data));
     * }</pre>
     *
     * @param consumer a {@link Consumer} that receives the temporary plaintext
     *                 array; must not retain a reference to it after returning.
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
            if(tmp!=null && tmp.length!=0)
            {
                Arrays.fill(tmp, (byte)0);
            }
        }
    }
    
}
