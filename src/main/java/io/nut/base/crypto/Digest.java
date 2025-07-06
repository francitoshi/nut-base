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
    final String algorithm;

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
        this(kripto, algorithm.code);
    }

    /**
     * Creates a new Digest instance for a specific algorithm by name.
     *
     * @param kripto The crypto provider. If null, a default instance from
     * {@link Kripto#getInstance()} will be used.
     * @param algorithm The standard name of the algorithm (e.g., "SHA-256").
     */
    public Digest(Kripto kripto, String algorithm)
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
    
    // --- Lazily-initialized instances for common algorithms ---
    private volatile Digest md5Digest;
    private volatile Digest sha1Digest;
    private volatile Digest sha256Digest;
    private volatile Digest sha512Digest;
    private volatile Digest ripemd160Digest;
    
    /**
     * Returns a lazily-initialized, shared instance of a {@code Digest}
     * configured for MD5.
     * <p>
     * <b>Warning:</b> MD5 is considered cryptographically broken and should not
     * be used for security-sensitive applications like password hashing or
     * digital signatures.
     *
     * @return A shared {@code Digest} instance for MD5 hashing.
     */
    public Digest md5()
    {
        return md5Digest==null ? md5Digest=new Digest(kripto, MessageDigestAlgorithm.MD5) : md5Digest;
    }
    
    /**
     * A convenience method to compute the MD5 hash of a byte array.
     */
    public byte[] md5(byte[] bytes) 
    {
        return this.md5().digest(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the MD5 hash of a sub-array.
     */
    public byte[] md5(byte[] bytes, int offset, int length) 
    {
        return this.md5().digest(bytes, offset, length);
    }

    /**
     * A convenience method to compute the MD5 hash of multiple byte arrays.
     */
    public byte[] md5(byte[]... bytes)
    {
        return this.md5().digest(bytes);
    }

    /**
     * A convenience method to compute the double-MD5 hash.
     */
    public byte[] md5Twice(byte[] bytes)
    {
        return md5Twice(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the double-MD5 hash of a sub-array.
     */
    public byte[] md5Twice(byte[] bytes, int offset, int length) 
    {
        return this.md5().digestTwice(bytes, offset, length);
    }

    /**
     * A convenience method to compute the MD5 hash of a string.
     */
    public byte[] md5(String s) 
    {
        return md5(s.getBytes());
    }

    /**
     * A convenience method to compute the MD5 hash of a string with a specific
     * charset.
     */
    public byte[] md5(String s, Charset charset) 
    {
        return md5(s.getBytes(charset));
    }
    
    /**
     * Returns a lazily-initialized, shared instance of a {@code Digest}
     * configured for SHA-1.
     * <p>
     * <b>Warning:</b> SHA-1 is also considered weak against collision attacks
     * and is deprecated for most security purposes. Prefer SHA-256 or stronger
     * algorithms.
     *
     * @return A shared {@code Digest} instance for SHA-1 hashing.
     */
    public Digest sha1()
    {
        return sha1Digest==null ? sha1Digest=new Digest(kripto, MessageDigestAlgorithm.SHA1) : sha1Digest;
    }
    
    /**
     * A convenience method to compute the SHA-1 hash of a byte array.
     */
    public byte[] sha1(byte[] bytes) 
    {
        return this.sha1().digest(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the SHA-1 hash of a sub-array.
     */
    public byte[] sha1(byte[] bytes, int offset, int length) 
    {
        return this.sha1().digest(bytes, offset, length);
    }

    /**
     * A convenience method to compute the SHA-1 hash of multiple byte arrays.
     */
    public byte[] sha1(byte[]... bytes)
    {
        return this.sha1().digest(bytes);
    }

    /**
     * A convenience method to compute the double-SHA-1 hash.
     */
    public byte[] sha1Twice(byte[] bytes)
    {
        return sha1Twice(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the double-SHA-1 hash of a sub-array.
     */
    public byte[] sha1Twice(byte[] bytes, int offset, int length) 
    {
        return this.sha1().digestTwice(bytes, offset, length);
    }

    /**
     * A convenience method to compute the SHA-1 hash of a string.
     */
    public byte[] sha1(String s) 
    {
        return sha1(s.getBytes());
    }

    /**
     * A convenience method to compute the SHA-1 hash of a string with a
     * specific charset.
     */
    public byte[] sha1(String s, Charset charset) 
    {
        return sha1(s.getBytes(charset));
    }

    /**
     * Returns a lazily-initialized, shared instance of a {@code Digest}
     * configured for SHA-256.
     *
     * @return A shared {@code Digest} instance for SHA-256 hashing.
     */
    public Digest sha256()
    {
        return sha256Digest==null ? sha256Digest=new Digest(kripto, MessageDigestAlgorithm.SHA256) : sha256Digest;
    }

    /**
     * A convenience method to compute the SHA-256 hash of a byte array.
     */
    public byte[] sha256(byte[] bytes) 
    {
        return this.sha256().digest(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the SHA-256 hash of a sub-array.
     */
    public byte[] sha256(byte[] bytes, int offset, int length) 
    {
        return this.sha256().digest(bytes, offset, length);
    }

    /**
     * A convenience method to compute the SHA-256 hash of multiple byte arrays.
     */
    public byte[] sha256(byte[]... bytes)
    {
        return this.sha256().digest(bytes);
    }
    
    /**
     * A convenience method to compute the double-SHA-256 hash. This is a common
     * operation in Bitcoin for transaction IDs and block hashing.
     */
    public byte[] sha256Twice(byte[] bytes)
    {
        return sha256Twice(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the double-SHA-256 hash of a sub-array.
     */
    public byte[] sha256Twice(byte[] bytes, int offset, int length) 
    {
        return this.sha256().digestTwice(bytes, offset, length);
    }

    /**
     * A convenience method to compute the SHA-256 hash of a string.
     */
    public byte[] sha256(String s) 
    {
        return sha256(s.getBytes());
    }

    /**
     * A convenience method to compute the SHA-256 hash of a string with a
     * specific charset.
     */
    public byte[] sha256(String s, Charset charset) 
    {
        return sha256(s.getBytes(charset));
    }

    /**
     * Returns a lazily-initialized, shared instance of a {@code Digest}
     * configured for SHA-512.
     *
     * @return A shared {@code Digest} instance for SHA-512 hashing.
     */
    public Digest sha512()
    {
        return sha512Digest==null ? sha512Digest=new Digest(kripto, MessageDigestAlgorithm.SHA512) : sha512Digest;
    }

    /**
     * A convenience method to compute the SHA-512 hash of a byte array.
     */
    public byte[] sha512(byte[] bytes) 
    {
        return this.sha512().digest(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the SHA-512 hash of a sub-array.
     */
    public byte[] sha512(byte[] bytes, int offset, int length) 
    {
        return this.sha512().digest(bytes, offset, length);
    }

    /**
     * A convenience method to compute the SHA-512 hash of multiple byte arrays.
     */
    public byte[] sha512(byte[]... bytes)
    {
        return this.sha512().digest(bytes);
    }

    /**
     * A convenience method to compute the double-SHA-512 hash.
     */
    public byte[] sha512Twice(byte[] bytes)
    {
        return sha512Twice(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the double-SHA-512 hash of a sub-array.
     */
    public byte[] sha512Twice(byte[] bytes, int offset, int length) 
    {
        return this.sha512().digestTwice(bytes, offset, length);
    }

    /**
     * A convenience method to compute the SHA-512 hash of a string.
     */
    public byte[] sha512(String s) 
    {
        return sha512(s.getBytes());
    }

    /**
     * A convenience method to compute the SHA-512 hash of a string with a
     * specific charset.
     */
    public byte[] sha512(String s, Charset charset) 
    {
        return sha512(s.getBytes(charset));
    }

    /**
     * Returns a lazily-initialized, shared instance of a {@code Digest}
     * configured for RIPEMD-160.
     *
     * @return A shared {@code Digest} instance for RIPEMD-160 hashing.
     */
    public Digest ripemd160()
    {
        return ripemd160Digest==null ? ripemd160Digest=new Digest(kripto, MessageDigestAlgorithm.RIPEMD160) : ripemd160Digest;
    }

    /**
     * A convenience method to compute the RIPEMD-160 hash of a byte array.
     */
    public byte[] ripemd160(byte[] bytes) 
    {
        return this.ripemd160().digest(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the RIPEMD-160 hash of a sub-array.
     */
    public byte[] ripemd160(byte[] bytes, int offset, int length) 
    {
        return this.ripemd160().digest(bytes, offset, length);
    }

    /**
     * A convenience method to compute the RIPEMD-160 hash of multiple byte
     * arrays.
     */
    public byte[] ripemd160(byte[]... bytes)
    {
        return this.ripemd160().digest(bytes);
    }

    /**
     * A convenience method to compute the double-RIPEMD-160 hash.
     */
    public byte[] ripemd160Twice(byte[] bytes)
    {
        return ripemd160Twice(bytes, 0, bytes.length);
    }

    /**
     * A convenience method to compute the double-RIPEMD-160 hash of a
     * sub-array.
     */
    public byte[] ripemd160Twice(byte[] bytes, int offset, int length) 
    {
        return this.ripemd160().digestTwice(bytes, offset, length);
    }

    /**
     * A convenience method to compute the RIPEMD-160 hash of a string.
     */
    public byte[] ripemd160(String s) 
    {
        return ripemd160(s.getBytes());
    }

    /**
     * A convenience method to compute the RIPEMD-160 hash of a string with a
     * specific charset.
     */
    public byte[] ripemd160(String s, Charset charset) 
    {
        return ripemd160(s.getBytes(charset));
    }
    
    /**
     * Calculates {@code RIPEMD160(SHA256(input))}.
     * <p>
     * This specific combination of hashing algorithms is a fundamental step in
     * generating Bitcoin addresses from a public key.
     *
     * @param input The input data to be hashed, typically a public key.
     * @return The resulting 160-bit hash (20 bytes).
     */
    public byte[] sha256ripemd160(byte[] input) 
    {
        return ripemd160(sha256(input));
    }
}
