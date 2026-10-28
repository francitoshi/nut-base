/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for generating 96-bit (12-byte) nonces.
 * <p>
 * Implementations of this class provide different strategies for generating
 * unique nonces, which are commonly required in authenticated encryption modes
 * such as AES-GCM or ChaCha20-Poly1305.
 * </p>
 */
public abstract class BytesNonce
{
    /**
     * Standard nonce size (96 bits, 12 bytes).
     */
    static final int NONCE_SIZE = 12;

    /**
     * Generates the next nonce.
     *
     * @return a new 12-byte nonce, unique according to the implementation
     * strategy
     */
    public abstract byte[] next();
    
    /**
     * Creates an instance of a nonce generator that combines a random 96-bit
     * value with a monotonically increasing counter.
     * <p>
     * Nonce = R ⊕ C
     * </p>
     *
     * @return a {@link RandomCounterBytesNonce} instance
     */
    public static BytesNonce getRandomCounterInstance()
    {
        return new RandomCounterBytesNonce();
    }

    /**
     * Creates an instance of a stateless nonce generator, always return a new
     * random 96-bit value.
     *
     * @return a {@link RandomBytesNonce} instance
     */
    public static BytesNonce getRandomInstance()
    {
        return new RandomBytesNonce();
    }
    
}

/**
 * Nonce generator that combines a random 96-bit value (R) with a monotonically
 * increasing counter (C) using XOR.
 * <p>
 * Each generated nonce is: {@code nonce = R ⊕ encode(C)}
 * </p>
 * <p>
 * This construction ensures uniqueness as long as the counter does not wrap.
 * </p>
 */
class RandomCounterBytesNonce extends BytesNonce
{
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private final byte[] r = new byte[NONCE_SIZE];
    private final AtomicLong counter = new AtomicLong(0);
    
    /**
     * Creates a new nonce generator with a random 96-bit constant R.
     */
    public RandomCounterBytesNonce()
    {
        RANDOM.nextBytes(r);
    }

    /**
     * Returns the next nonce by XORing the constant random value with the
     * encoded counter.
     *
     * @return a unique 12-byte nonce
     */
    @Override
    public byte[] next()
    {
        long c = counter.getAndIncrement();
        if(c<0)
        {
            throw new SecurityException("counter overflow");
        }
        byte[] nonce = new byte[NONCE_SIZE];

        // Store counter in the last 8 bytes of the nonce
        ByteBuffer.wrap(nonce).putLong(4, c);

        // XOR with the fixed random value R
        for (int i = 0; i < NONCE_SIZE; i++)
        {
            nonce[i] = (byte) (r[i] ^ nonce[i]);
        }
        return nonce;
    }
}

/**
 * Nonce generator that produces fully random 96-bit values.
 * <p>
 * Each call generates a new random nonce using {@link SecureRandom}.
 * </p>
 * <p>
 * While simple, this method has a (very low) probability of collisions, so it
 * is less recommended for very high-volume systems.
 * </p>
 */
class RandomBytesNonce extends BytesNonce
{
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public byte[] next()
    {
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(nonce);
        return nonce;
    }
}
