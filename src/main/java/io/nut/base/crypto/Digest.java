/*
 *  Digest.java
 *
 *  Copyright (c) 2014-2025 francitoshi@gmail.com
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

import io.nut.base.crypto.Kripto.MessageDigestAlgorithm;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * A utility class for simplifying cryptographic hashing operations (message
 * digests).
 * <p>
 * This class acts as a wrapper around {@link java.security.MessageDigest} to
 * provide a more convenient and fluent API. It handles exceptions, provides
 * shortcuts for common algorithms like SHA-256 and MD5, and includes
 * specialized methods used in cryptocurrency protocols.
 * <p>
 * Instances of this class are configured for a specific algorithm. For
 * convenience, it also provides lazily-initialized singleton instances for
 * common algorithms, making it easy to perform hashes without manually managing
 * {@code MessageDigest} objects.
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe. The core {@code digest}
 * methods create a new {@link MessageDigest} instance for each operation, as
 * those objects are not reusable. The lazily-initialized helper instances
 * (e.g., {@code md5Digest}) are declared as {@code volatile} to ensure
 * visibility across threads.
 *
 * @see java.security.MessageDigest
 * @see io.nut.base.crypto.Kripto
 */
public class Digest
{

    /**
     * The underlying crypto provider factory.
     */
    final Kripto kripto;
    /**
     * The name of the message digest algorithm for this instance.
     */
    final MessageDigestAlgorithm algorithm;

    /**
     * Creates a new Digest instance for a specific algorithm.
     *
     * @param kripto The crypto provider. If null, a default instance from
     * {@link Kripto#getInstance()} will be used.
     * @param algorithm The {@link MessageDigestAlgorithm} enum constant
     * representing the desired algorithm.
     */
    public Digest(Kripto kripto, MessageDigestAlgorithm algorithm)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
        this.algorithm = algorithm;
    }
    
    /**
     * Gets a new {@link MessageDigest} instance for the configured algorithm.
     * <p>
     * A new instance is returned on each call because {@code MessageDigest}
     * objects are reset after a {@code digest()} operation and are not
     * generally safe for concurrent use.
     *
     * @return A new {@link MessageDigest} instance.
     * @throws IllegalArgumentException if the algorithm is not supported by the
     * underlying provider.
     */
    public MessageDigest get()
    {
        return kripto.getMessageDigest(algorithm);
    }

    /**
     * Computes the hash of the given byte array.
     *
     * @param bytes The data to hash.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digest(byte[] bytes) 
    {
        return digest(bytes, 0, bytes.length);
    }

    /**
     * Computes the hash of a sub-array of the given byte array.
     *
     * @param bytes The source byte array.
     * @param offset The starting offset in the array.
     * @param length The number of bytes to use for hashing.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digest(byte[] bytes, int offset, int length) 
    {
        MessageDigest digest = get();
        digest.update(bytes, offset, length);
        return digest.digest();
    }

    /**
     * Computes the hash by sequentially updating with multiple byte arrays.
     * This is more efficient than concatenating the arrays into a single array
     * first.
     *
     * @param bytes A varargs array of byte arrays to hash in sequence.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digest(byte[]... bytes) 
    {
        MessageDigest digest = get();
        for(byte[] item : bytes)
        {
            digest.update(item);
        }
        return digest.digest();
    }

    /**
     * Computes a double-hash of the data (i.e., {@code hash(hash(data))}).
     *
     * @param bytes The data to hash twice.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digestTwice(byte[] bytes) 
    {
        return digestTwice(bytes, 0, bytes.length);
    }

    /**
     * Computes a double-hash of a sub-array of the given byte array.
     *
     * @param bytes The source byte array.
     * @param offset The starting offset in the array.
     * @param length The number of bytes to use for hashing.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digestTwice(byte[] bytes, int offset, int length) 
    {
        MessageDigest digest = get();
        digest.update(bytes, offset, length);
        digest.update(digest.digest()); // Hash the first hash
        return digest.digest();
    }
    
    /**
     * Computes the hash of a string, converting it to bytes using the
     * platform's default charset.
     *
     * @param s The string to hash.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digest(String s) 
    {
        return digest(s.getBytes());
    }

    /**
     * Computes the hash of a string using the specified charset.
     *
     * @param s The string to hash.
     * @param charset The charset to use for converting the string to bytes.
     * @return The resulting hash digest as a byte array.
     */
    public byte[] digest(String s, Charset charset) 
    {
        return digest(s.getBytes(charset));
    }
    
}
