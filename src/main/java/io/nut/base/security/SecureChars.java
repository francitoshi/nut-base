/*
 *  SecureChars.java
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
import io.nut.base.util.Byter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.security.auth.Destroyable;

/**
 * Holds a {@code char[]} securely in memory by converting it to bytes and
 * delegating all cryptographic protection to a {@link SecureBytes} instance.
 *
 * <p>This class is the character-oriented counterpart of {@link SecureBytes}.
 * It is designed to protect sensitive string-like data (passwords, passphrases,
 * PINs, etc.) that must not persist as plaintext on the heap. The source
 * {@code char[]} is encoded to a byte array, handed off to {@link SecureBytes}
 * for AES-256-GCM encryption, and then zeroed immediately.</p>
 *
 * <p>Instances must be released by calling {@link #destroy()} or by using a
 * try-with-resources block. After destruction the underlying
 * {@link SecureBytes} wipes its key, IV and ciphertext.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * char[] password = readPasswordFromUI();
 * try (SecureChars sc = new SecureChars(password)) {
 *     // password[] has already been zeroed by the constructor
 *     sc.consume(chars -> authenticate(chars));
 *     // chars[] is zeroed immediately after the lambda returns
 * }
 * }</pre>
 *
 * @see SecureBytes
 */
public class SecureChars implements AutoCloseable, Destroyable
{
    /**
     * The underlying byte-level secure storage that holds the encoded and
     * encrypted character data.
     */
    private final SecureBytes secureBytes;

    /**
     * The character encoding used to convert between {@code char[]} and
     * {@code byte[]}.
     */
    private final Charset charset;

    /**
     * Constructs a {@code SecureChars} instance that protects {@code src}
     * using the specified {@link Charset} and {@link Kripto} instance.
     *
     * <p>The source array is encoded to bytes, encrypted by a new
     * {@link SecureBytes}, and then zeroed. Passing {@code null} or an empty
     * array is handled gracefully and delegates the edge-case behaviour to
     * {@link SecureBytes}.</p>
     *
     * @param src     the plaintext character array to protect; may be
     *                {@code null} or empty. The array is zeroed by this
     *                constructor.
     * @param charset the {@link Charset} used to encode {@code src} to bytes.
     * @param kripto  the {@link Kripto} instance for cryptographic operations;
     *                if {@code null} the shared singleton inside
     *                {@link SecureBytes} is used.
     */
    public SecureChars(char[] src, Charset charset, Kripto kripto)
    {
        this.secureBytes = new SecureBytes(Byter.bytes(src, charset), kripto);
        this.charset = charset;
        if(src!=null && src.length>0)
        {
            Arrays.fill(src, '\0');
        }
    }

    /**
     * Constructs a {@code SecureChars} instance using UTF-8 encoding and the
     * provided {@link Kripto} instance.
     *
     * <p>Equivalent to {@code new SecureChars(src, StandardCharsets.UTF_8, kripto)}.</p>
     *
     * @param kripto the {@link Kripto} instance for cryptographic operations;
     *               if {@code null} the shared singleton is used.
     * @param src    the plaintext character array to protect. The array is
     *               zeroed by this constructor.
     */
    public SecureChars(Kripto kripto, char[] src)
    {
        this(src, StandardCharsets.UTF_8, kripto);
    }

    /**
     * Constructs a {@code SecureChars} instance using UTF-8 encoding and the
     * shared default {@link Kripto} instance.
     *
     * <p>Equivalent to {@code new SecureChars(src, StandardCharsets.UTF_8, null)}.</p>
     *
     * @param src the plaintext character array to protect. The array is zeroed
     *            by this constructor.
     */
    public SecureChars(char[] src)
    {
        this(src, StandardCharsets.UTF_8, null);
    }

    /**
     * Decrypts and decodes the protected data, returning it as a fresh
     * {@code char[]}.
     *
     * <p>The intermediate byte buffer is zeroed before this method returns.
     * The caller is responsible for zeroing the returned {@code char[]} when
     * finished (prefer {@link #consume(Consumer)} or {@link #apply(Function)}
     * which do this automatically).</p>
     *
     * <p>Package-private visibility is intentional: external code must use
     * {@link #consume(Consumer)} or {@link #apply(Function)} to guarantee
     * that the plaintext is wiped after use.</p>
     *
     * @return a freshly decoded plaintext {@code char[]}, or {@code null} /
     *         an empty array if this instance was constructed from a
     *         {@code null} or empty source.
     */
    // keep private for outsiders
    char[] getChars()
    {
        byte[] bytes = this.secureBytes.getBytes();
        char[] chars = Byter.chars(bytes, this.charset);
        if(bytes!=null && bytes.length>0)
        {
            Arrays.fill(bytes, (byte)0);
        }
        return chars;
    }

    /**
     * Destroys this instance by delegating to the underlying
     * {@link SecureBytes#destroy()}, which zeros the key, IV and ciphertext.
     *
     * <p>Subsequent calls are no-ops. After this method returns,
     * {@link #isDestroyed()} returns {@code true}.</p>
     */
    @Override
    public void destroy()
    {
        this.secureBytes.destroy();
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
        return this.secureBytes.isDestroyed();
    }

    /**
     * Implements {@link AutoCloseable} by delegating to {@link #destroy()},
     * enabling use in try-with-resources statements.
     *
     * @throws Exception if {@link #destroy()} throws (it does not in the
     *                   current implementation, but the signature follows the
     *                   contract of {@link AutoCloseable}).
     */
    @Override
    public void close() throws Exception
    {
        this.destroy();
    }

    /**
     * Decrypts the protected data, passes the resulting {@code char[]} to
     * {@code consumer}, and then zeros the temporary buffer before returning
     * — even if the consumer throws an exception.
     *
     * <p>This is the preferred way to access the protected character data
     * because it guarantees the plaintext does not linger on the heap longer
     * than necessary.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * secureChars.consume(chars -> verifyPassword(chars));
     * }</pre>
     *
     * @param consumer a {@link Consumer} that receives the temporary plaintext
     *                 array; must not retain a reference to it after returning.
     */
    public void consume(Consumer<char[]> consumer)
    {
        char[] tmp = getChars();
        try
        {
            consumer.accept(tmp);
        }
        finally
        {
            if(tmp!=null && tmp.length>0)
            {
                Arrays.fill(tmp, '\0');
            }
        }
    }

    /**
     * Decrypts the protected data, applies {@code function} to the resulting
     * {@code char[]}, zeros the temporary buffer, and returns the function's
     * result — even if the function throws an exception.
     *
     * <p>Use this variant when the caller needs a return value (e.g., to
     * derive a key from a password):</p>
     * <pre>{@code
     * byte[] key = secureChars.apply(chars -> deriveKey(chars));
     * }</pre>
     *
     * @param <T>      the type of the result produced by {@code function}.
     * @param function a {@link Function} that receives the temporary plaintext
     *                 array and produces a result; must not retain a reference
     *                 to the array after returning.
     * @return the value returned by {@code function}.
     */
    public <T> T apply(Function<char[], T> function)
    {
        char[] tmp = getChars();
        try
        {
            return function.apply(tmp);
        }
        finally
        {
            if(tmp != null && tmp.length > 0)
            {
                Arrays.fill(tmp, '\0');
            }
        }
    }
}
