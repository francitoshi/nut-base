/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.jca;

import io.nut.base.jca.Kr.Hmac;
import java.nio.charset.Charset;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A utility class for computing HMAC (Hash-based Message Authentication Code)
 * values.
 * <p>
 * This class wraps {@link javax.crypto.Mac} to provide a more convenient API.
 * It handles the creation of the {@code Mac} instance and of temporary
 * {@link SecretKeySpec} objects, so callers can obtain an authentication tag
 * with a single call.
 * <p>
 * Instances are configured for a specific {@link Kr.Hmac} algorithm
 * (e.g., {@code HmacSHA256}) and can be safely reused: every {@code digest}
 * call obtains a fresh {@link javax.crypto.Mac} from the underlying provider,
 * so no state is shared between operations.
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe, provided the
 * {@link Kr} instance it was built with is thread-safe (the default one from
 * {@link Kr#getInstance()} is).
 *
 * @see javax.crypto.Mac
 * @see Kr.Hmac
 */
public class HMAC
{

    /**
     * The underlying crypto provider factory.
     */
    final Kr kr;
    /**
     * The name of the HMAC algorithm for this instance.
     */
    final Hmac algorithm;

    /**
     * Creates a new HMAC instance for a specific algorithm.
     *
     * @param kr The crypto provider. If null, a default instance from
     * {@link Kr#getInstance()} will be used.
     * @param algorithm The {@link Kr.Hmac} enum constant representing the
     * desired algorithm, e.g. {@link Kr.Hmac#HmacSHA256}.
     */
    public HMAC(Kr kr, Hmac algorithm)
    {
        this.kr = kr==null ? Kr.getInstance() : kr;
        this.algorithm = algorithm;
    }

    /**
     * Gets a new {@link Mac} instance for the configured algorithm, initialized
     * with the given key.
     * <p>
     * A new instance is returned on each call, so callers own it and may feed
     * it with any number of updates before calling {@code doFinal()} on it.
     *
     * @param secretKey The secret key to initialize the MAC with.
     * @return A new initialized {@link Mac} instance.
     * @throws IllegalArgumentException if the algorithm is not supported by the
     * underlying provider.
     */
    public Mac get(SecretKey secretKey)
    {
        return kr.getMac(algorithm, secretKey);
    }

    /**
     * Computes the HMAC of the given byte array using a raw key.
     *
     * @param secretKey The raw key bytes, wrapped in a
     * {@link SecretKeySpec} for the configured algorithm.
     * @param bytes The data to authenticate.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(byte[] secretKey, byte[] bytes)
    {
        return digest(new SecretKeySpec(secretKey, algorithm.name()), bytes);
    }

    /**
     * Computes the HMAC of a sub-array of the given byte array using a raw key.
     *
     * @param secretKey The raw key bytes, wrapped in a
     * {@link SecretKeySpec} for the configured algorithm.
     * @param bytes The source byte array.
     * @param offset The starting offset in the array.
     * @param length The number of bytes to authenticate.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(byte[] secretKey, byte[] bytes, int offset, int length)
    {
        return digest(new SecretKeySpec(secretKey, algorithm.name()), bytes, offset, length);
    }

    /**
     * Computes the HMAC of the given byte array.
     *
     * @param secretKey The secret key.
     * @param bytes The data to authenticate.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(SecretKey secretKey, byte[] bytes)
    {
        return digest(secretKey, bytes, 0, bytes.length);
    }

    /**
     * Computes the HMAC of a sub-array of the given byte array.
     *
     * @param secretKey The secret key.
     * @param bytes The source byte array.
     * @param offset The starting offset in the array.
     * @param length The number of bytes to authenticate.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(SecretKey secretKey, byte[] bytes, int offset, int length)
    {
        Mac mac = get(secretKey);
        mac.update(bytes, offset, length);
        return mac.doFinal();
    }

    /**
     * Computes the HMAC by sequentially updating with multiple byte arrays.
     * This is equivalent to authenticating their concatenation, but avoids
     * copying them into a single array first.
     *
     * @param secretKey The secret key.
     * @param bytes A varargs array of byte arrays to authenticate in sequence.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(SecretKey secretKey, byte[]... bytes)
    {
        Mac mac = get(secretKey);
        for(byte[] item : bytes)
        {
            mac.update(item);
        }
        return mac.doFinal();
    }

    /**
     * Computes the HMAC of a string, converting it to bytes using the
     * platform's default charset.
     *
     * @param secretKey The secret key.
     * @param s The string to authenticate.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(SecretKey secretKey, String s)
    {
        return digest(secretKey, s.getBytes());
    }

    /**
     * Computes the HMAC of a string using the specified charset.
     *
     * @param secretKey The secret key.
     * @param s The string to authenticate.
     * @param charset The charset to use for converting the string to bytes.
     * @return The authentication tag as a byte array.
     */
    public byte[] digest(SecretKey secretKey, String s, Charset charset)
    {
        return digest(secretKey, s.getBytes(charset));
    }

}
