/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fast, non-cryptographic hashes in the spirit of Go's {@code hash/fnv} and
 * {@code hash/maphash}.
 * <p>
 * The FNV-1 and FNV-1a functions are deterministic: the same input hashes to
 * the same value on every process and every machine, which makes them suitable
 * for anything that crosses process boundaries — persistent fingerprints,
 * consistent sharding between nodes, deduplication records. They are <strong>not
 * cryptographic</strong> and not collision-resistant against an adversary who
 * chooses the inputs: never use them for signatures, MACs or password storage
 * (see the {@code security} package and JCA for that).
 * <p>
 * For hash tables indexed with attacker-chosen keys, use the seeded variants
 * ({@link #maphash64(long, byte[], int, int)} and friends) with a per-process
 * seed, so
 * that collisions cannot be prepared in advance: with a seed unknown to the
 * attacker, crafted collision sets for one process do not apply to another.
 * {@link #processSeed()} generates one such seed, once per JVM.
 * <p>
 * Typical uses:
 * <ul>
 * <li><b>Consistent sharding / partitioning</b> across processes or nodes:
 *     {@code int bucket = Math.floorMod(Hashes.fnv1a64(key), buckets);}</li>
 * <li><b>Deduplication records and persistent fingerprints</b> of serialized
 *     data: store {@code fnv1a64(bytes)} alongside the payload as a cheap
 *     first check before an expensive equality or digest comparison.</li>
 * <li><b>Tables fed with untrusted keys</b>: index with
 *     {@code Math.floorMod(Hashes.maphash64(key), buckets)} so prepared
 *     collision sets do not transfer between processes.</li>
 * </ul>
 */
public final class Hashes
{
    private Hashes()
    {
    }

    /** FNV-1/FNV-1a 32-bit offset basis: {@code 0x811c9dc5}. */
    public static final int FNV_OFFSET_BASIS_32 = 0x811c9dc5;

    /** FNV-1/FNV-1a 32-bit prime: {@code 0x01000193}. */
    public static final int FNV_PRIME_32 = 0x01000193;

    /** FNV-1/FNV-1a 64-bit offset basis: {@code 0xcbf29ce484222325}. */
    public static final long FNV_OFFSET_BASIS_64 = 0xcbf29ce484222325L;

    /** FNV-1/FNV-1a 64-bit prime: {@code 0x100000001b3}. */
    public static final long FNV_PRIME_64 = 0x100000001b3L;

    // -----------------------------------------------------------------
    // FNV-1a (xor first: the recommended variant, better diffusion for
    // short inputs such as identifiers and text keys)
    // -----------------------------------------------------------------

    /** Returns the FNV-1a 32-bit hash of the given byte range. */
    public static int fnv1a32(byte[] data, int off, int len)
    {
        return fnv1a32(FNV_OFFSET_BASIS_32, data, off, len);
    }

    /** Returns the FNV-1a 32-bit hash of the whole array. */
    public static int fnv1a32(byte[] data)
    {
        return fnv1a32(FNV_OFFSET_BASIS_32, data, 0, data.length);
    }

    /** Returns the FNV-1a 32-bit hash of the remaining bytes (absolute positions). */
    public static int fnv1a32(ByteBuffer data)
    {
        return fnv1a32(FNV_OFFSET_BASIS_32, data);
    }

    /** Returns the FNV-1a 32-bit hash of the UTF-8 encoding of the sequence. */
    public static int fnv1a32(CharSequence data)
    {
        return fnv1a32(FNV_OFFSET_BASIS_32, data);
    }

    /**
     * Continues (or seeds) an FNV-1a 32-bit hash: the given value is used as
     * the starting hash, so callers can hash several fragments into one value
     * or supply their own seed.
     */
    public static int fnv1a32(int hash, byte[] data, int off, int len)
    {
        Objects.requireNonNull(data, "data must not be null");
        for (int i = off; i < off + len; i++)
        {
            hash ^= (data[i] & 0xFF);
            hash *= FNV_PRIME_32;
        }
        return hash;
    }

    private static int fnv1a32(int hash, ByteBuffer data)
    {
        Objects.requireNonNull(data, "data must not be null");
        for (int i = data.position(); i < data.limit(); i++)
        {
            hash ^= (data.get(i) & 0xFF);
            hash *= FNV_PRIME_32;
        }
        return hash;
    }

    private static int fnv1a32(int hash, CharSequence data)
    {
        Objects.requireNonNull(data, "data must not be null");
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        return fnv1a32(hash, bytes, 0, bytes.length);
    }

    // -----------------------------------------------------------------
    // FNV-1 (multiply first: kept for parity with Go's hash/fnv)
    // -----------------------------------------------------------------

    /** Returns the FNV-1 32-bit hash of the whole array. */
    public static int fnv132(byte[] data)
    {
        return fnv132(FNV_OFFSET_BASIS_32, data, 0, data.length);
    }

    /** Returns the FNV-1 64-bit hash of the whole array. */
    public static long fnv164(byte[] data)
    {
        return fnv164(FNV_OFFSET_BASIS_64, data, 0, data.length);
    }

    private static int fnv132(int hash, byte[] data, int off, int len)
    {
        for (int i = off; i < off + len; i++)
        {
            hash *= FNV_PRIME_32;
            hash ^= (data[i] & 0xFF);
        }
        return hash;
    }

    private static long fnv164(long hash, byte[] data, int off, int len)
    {
        for (int i = off; i < off + len; i++)
        {
            hash *= FNV_PRIME_64;
            hash ^= (data[i] & 0xFFL);
        }
        return hash;
    }

    // -----------------------------------------------------------------
    // FNV-1a 64
    // -----------------------------------------------------------------

    /** Returns the FNV-1a 64-bit hash of the given byte range. */
    public static long fnv1a64(byte[] data, int off, int len)
    {
        return fnv1a64(FNV_OFFSET_BASIS_64, data, off, len);
    }

    /** Returns the FNV-1a 64-bit hash of the whole array. */
    public static long fnv1a64(byte[] data)
    {
        return fnv1a64(FNV_OFFSET_BASIS_64, data, 0, data.length);
    }

    /** Returns the FNV-1a 64-bit hash of the remaining bytes (absolute positions). */
    public static long fnv1a64(ByteBuffer data)
    {
        return fnv1a64(FNV_OFFSET_BASIS_64, data);
    }

    /** Returns the FNV-1a 64-bit hash of the UTF-8 encoding of the sequence. */
    public static long fnv1a64(CharSequence data)
    {
        return fnv1a64(FNV_OFFSET_BASIS_64, data);
    }

    /**
     * Continues (or seeds) an FNV-1a 64-bit hash: the given value is used as
     * the starting hash, so callers can hash several fragments into one value
     * or supply their own seed.
     */
    public static long fnv1a64(long hash, byte[] data, int off, int len)
    {
        Objects.requireNonNull(data, "data must not be null");
        for (int i = off; i < off + len; i++)
        {
            hash ^= (data[i] & 0xFFL);
            hash *= FNV_PRIME_64;
        }
        return hash;
    }

    private static long fnv1a64(long hash, ByteBuffer data)
    {
        Objects.requireNonNull(data, "data must not be null");
        for (int i = data.position(); i < data.limit(); i++)
        {
            hash ^= (data.get(i) & 0xFFL);
            hash *= FNV_PRIME_64;
        }
        return hash;
    }

    private static long fnv1a64(long hash, CharSequence data)
    {
        Objects.requireNonNull(data, "data must not be null");
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        return fnv1a64(hash, bytes, 0, bytes.length);
    }

    // -----------------------------------------------------------------
    // maphash-style seeded hashing
    // -----------------------------------------------------------------

    /**
     * Returns the per-process seed used by the {@code maphash*} variants.
     * Generated once per JVM from a random source and forced non-zero (a zero
     * basis would make empty prefixes degenerate); stable within the lifetime
     * of the process, different across processes and runs.
     *
     * @return the per-process hash seed, never {@code 0}
     */
    public static long processSeed()
    {
        return Holder.SEED;
    }

    /**
     * Returns a seeded FNV-1a 64-bit hash of the whole array, equivalent to
     * {@code fnv1a64(processSeed(), data, 0, data.length)}: the value differs
     * between processes, so prepared collision sets do not transfer.
     */
    public static long maphash64(byte[] data)
    {
        return fnv1a64(processSeed(), data, 0, data.length);
    }

    /** Returns a seeded FNV-1a 64-bit hash of the given byte range. */
    public static long maphash64(byte[] data, int off, int len)
    {
        return fnv1a64(processSeed(), data, off, len);
    }

    /**
     * Returns a seeded FNV-1a 64-bit hash of the UTF-8 encoding of the
     * sequence, with the same per-process seed property as
     * {@link #maphash64(byte[])}.
     */
    public static long maphash64(CharSequence data)
    {
        Objects.requireNonNull(data, "data must not be null");
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        return fnv1a64(processSeed(), bytes, 0, bytes.length);
    }

    /**
     * Continues (or seeds) a seeded FNV-1a 64-bit hash: the caller-provided
     * {@code seed} replaces the per-process seed, so callers can key the hash
     * with their own secret or hash several fragments into one value.
     */
    public static long maphash64(long seed, byte[] data, int off, int len)
    {
        return fnv1a64(Holder.mix(seed), data, off, len);
    }

    private static final class Holder
    {
        static final long SEED = initSeed();

        private static long initSeed()
        {
            long seed = ThreadLocalRandom.current().nextLong();
            return seed == 0L ? FNV_OFFSET_BASIS_64 : seed;
        }

        /**
         * Normalizes a caller-provided seed: a zero seed falls back to the
         * per-process seed so that {@code maphash64(0, ...)} never degrades to
         * plain (predictable) FNV by accident.
         */
        private static long mix(long seed)
        {
            return seed == 0L ? SEED : seed;
        }
    }
}
