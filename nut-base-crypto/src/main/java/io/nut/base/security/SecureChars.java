/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import io.nut.base.jca.Kr;
import io.nut.base.lang.Chars;
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
 * <p>The source {@code char[]} is encoded, handed to {@link SecureBytes} for
 * AES-256-GCM encryption, and zeroed, also if encoding or encryption fails.
 * The charset is always explicit (UTF-8 by default), so results do not depend
 * on the platform default charset, which changed on Java 18 (JEP 400).</p>
 *
 * <p>Note: whether {@link Chars} leaves intermediate {@code ByteBuffer} /
 * {@code CharBuffer} copies on the heap depends on its implementation, and
 * unmappable characters (e.g. unpaired surrogates) are replaced by the
 * charset's replacement, so the recovered text may differ from the
 * original.</p>
 *
 * <pre>{@code
 * char[] password = readPasswordFromUI();
 * try (SecureChars sc = new SecureChars(password)) 
 * {
 *     sc.consume(chars -> authenticate(chars));
 * }
 * }</pre>
 *
 * @see SecureBytes
 */
public final class SecureChars implements AutoCloseable, Destroyable
{
    /** Underlying encrypted storage of the encoded characters. */
    private final SecureBytes secureBytes;

    /** Charset used to convert between {@code char[]} and {@code byte[]}. */
    private final Charset charset;

    /**
     * Protects {@code src} using the given charset and {@link Kr}.
     *
     * @param src     the characters to protect; may be {@code null} or empty.
     *                Zeroed by this constructor.
     * @param charset encoding to use; {@code null} means UTF-8.
     * @param kripto  crypto provider; {@code null} means the shared default.
     */
    public SecureChars(char[] src, Charset charset, Kr kripto)
    {
        Charset cs = charset == null ? StandardCharsets.UTF_8 : charset;
        SecureBytes sb;
        try
        {
            sb = new SecureBytes(Chars.bytes(src, cs), kripto);
        }
        finally
        {
            if(src != null && src.length > 0)
            {
                Arrays.fill(src, '\0');
            }
        }
        this.secureBytes = sb;
        this.charset = cs;
    }

    /**
     * Uses UTF-8 and the given {@link Kr}.
     *
     * @param kripto crypto provider; {@code null} means the shared default.
     * @param src    the characters to protect. Zeroed by this constructor.
     */
    public SecureChars(Kr kripto, char[] src)
    {
        this(src, StandardCharsets.UTF_8, kripto);
    }

    /**
     * Uses UTF-8 and the shared default {@link Kr}.
     *
     * @param src the characters to protect. Zeroed by this constructor.
     */
    public SecureChars(char[] src)
    {
        this(src, StandardCharsets.UTF_8, null);
    }

    /**
     * Decrypts and decodes the protected data. The intermediate byte buffer is
     * zeroed, also if decoding fails. The caller must zero the result
     * (prefer {@link #consume(Consumer)} or {@link #apply(Function)}).
     *
     * @return the plaintext, or {@code null} / an empty array if constructed
     *         from {@code null} / an empty source.
     * @throws IllegalStateException if this instance has been destroyed
     */
    // keep private for outsiders
    char[] getChars()
    {
        byte[] bytes = this.secureBytes.getBytes();
        try
        {
            return Chars.chars(bytes, this.charset);
        }
        finally
        {
            if(bytes != null && bytes.length > 0)
            {
                Arrays.fill(bytes, (byte) 0);
            }
        }
    }

    /** Destroys this instance by delegating to {@link SecureBytes#destroy()}. */
    @Override
    public void destroy()
    {
        this.secureBytes.destroy();
    }

    /** @return {@code true} after {@link #destroy()} or {@link #close()}, and for null/empty input */
    @Override
    public boolean isDestroyed()
    {
        return this.secureBytes.isDestroyed();
    }

    /** Delegates to {@link #destroy()}, enabling try-with-resources. */
    @Override
    public void close()
    {
        this.destroy();
    }

    /**
     * Decrypts the data, passes it to {@code consumer}, and zeros the temporary
     * buffer afterwards, even if the consumer throws. The consumer receives
     * {@code null} if this instance was built from {@code null}.
     *
     * @param consumer receives the temporary plaintext; must not retain it.
     * @throws IllegalStateException if this instance has been destroyed
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
            if(tmp != null && tmp.length > 0)
            {
                Arrays.fill(tmp, '\0');
            }
        }
    }

    /**
     * Decrypts the data, applies {@code function}, zeros the temporary buffer
     * and returns the result, even if the function throws. The function must
     * not return the array it receives, since it is zeroed afterwards.
     *
     * @param <T>      result type
     * @param function receives the temporary plaintext and produces a result.
     * @return the function's result
     * @throws IllegalStateException if this instance has been destroyed
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
