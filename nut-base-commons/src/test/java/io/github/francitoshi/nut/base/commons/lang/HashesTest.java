/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashesTest
{
    private static byte[] bytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // canonical FNV-1a vectors (FNV reference test suite / Go hash/fnv)
    @Test
    void fnv1a32MatchesTheOfficialVectors()
    {
        assertEquals(0x811c9dc5, Hashes.fnv1a32(bytes("")));
        assertEquals(0xe40c292c, Hashes.fnv1a32(bytes("a")));
        assertEquals(0xbf9cf968, Hashes.fnv1a32(bytes("foobar")));
    }

    @Test
    void fnv1a64MatchesTheOfficialVectors()
    {
        assertEquals(0xcbf29ce484222325L, Hashes.fnv1a64(bytes("")));
        assertEquals(0xaf63dc4c8601ec8cL, Hashes.fnv1a64(bytes("a")));
        assertEquals(0x85944171f73967e8L, Hashes.fnv1a64(bytes("foobar")));
    }

    @Test
    void fnv1IsTheMultiplicationFirstVariant()
    {
        // for one byte FNV-1 and FNV-1a differ: multiply-then-xor vs xor-then-multiply
        assertNotEquals(Hashes.fnv132(bytes("a")), Hashes.fnv1a32(bytes("a")));
        assertNotEquals(Hashes.fnv164(bytes("a")), Hashes.fnv1a64(bytes("a")));
        // empty input returns the offset basis in both variants
        assertEquals(Hashes.FNV_OFFSET_BASIS_32, Hashes.fnv132(bytes("")));
        assertEquals(Hashes.FNV_OFFSET_BASIS_64, Hashes.fnv164(bytes("")));
    }

    @Test
    void slicesMatchWholeArraysAndByteBuffers()
    {
        byte[] data = bytes("The quick brown fox jumps over the lazy dog");
        int off = 10;
        int len = 15;

        assertEquals(Hashes.fnv1a32(data), Hashes.fnv1a32(data, 0, data.length));
        assertEquals(Hashes.fnv1a32(data, off, len), Hashes.fnv1a32(data, off, len));

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(off);
        buffer.limit(off + len);
        assertEquals(Hashes.fnv1a32(data, off, len), Hashes.fnv1a32(buffer));

        ByteBuffer buffer64 = ByteBuffer.wrap(data);
        buffer64.position(off);
        buffer64.limit(off + len);
        assertEquals(Hashes.fnv1a64(data, off, len), Hashes.fnv1a64(buffer64));
    }

    @Test
    void charSequencesHashTheirUtf8Bytes()
    {
        // multi-byte character: the hash must be over the UTF-8 bytes, so it
        // equals hashing the equivalent byte array
        String s = "ñandú-🎄";
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        assertEquals(Hashes.fnv1a64(utf8), Hashes.fnv1a64((CharSequence) s));
        assertEquals(Hashes.fnv1a32(utf8), Hashes.fnv1a32((CharSequence) s));
    }

    @Test
    void incrementalHashingMatchesSingleShot()
    {
        byte[] data = bytes("hello world");
        long incremental = Hashes.fnv1a64(Hashes.fnv1a64(Hashes.FNV_OFFSET_BASIS_64, data, 0, 5), data, 5, 6);
        assertEquals(Hashes.fnv1a64(data), incremental);
    }

    @Test
    void processSeedIsStableAndNonZero()
    {
        long seed = Hashes.processSeed();
        assertNotEquals(0L, seed);
        assertEquals(seed, Hashes.processSeed(), "the seed is generated once per JVM");
    }

    @Test
    void maphashIsSeededAndStableWithinTheProcess()
    {
        byte[] data = bytes("attacker-chosen-key");
        long seeded = Hashes.maphash64(data);
        long plain = Hashes.fnv1a64(data);

        assertNotEquals(plain, seeded, "the seeded hash must differ from plain FNV");
        assertEquals(seeded, Hashes.maphash64(data), "stable within the process");
        assertEquals(seeded, Hashes.maphash64(data, 0, data.length));
    }

    @Test
    void explicitSeedOverridesTheProcessSeed()
    {
        byte[] data = bytes("some data");
        long a = Hashes.maphash64(42L, data, 0, data.length);
        long b = Hashes.maphash64(43L, data, 0, data.length);
        assertNotEquals(a, b);
        // zero seed falls back to the process seed instead of degrading to plain FNV
        assertEquals(Hashes.maphash64(data), Hashes.maphash64(0L, data, 0, data.length));
        // the explicit seed is deterministic across calls and seeds
        assertEquals(a, Hashes.maphash64(42L, data, 0, data.length));
    }

    @Test
    void differentInputsHashDifferently()
    {
        assertNotEquals(Hashes.fnv1a64(bytes("key-1")), Hashes.fnv1a64(bytes("key-2")));
        assertFalse(Hashes.fnv1a64(bytes("key-1")) == 0L);
        assertTrue(Hashes.fnv1a64(Hashes.FNV_OFFSET_BASIS_64, bytes(""), 0, 0) == Hashes.FNV_OFFSET_BASIS_64);
    }

    @Test
    void rejectsNullInputs()
    {
        assertThrows(NullPointerException.class, () -> Hashes.fnv1a64((byte[]) null));
        assertThrows(NullPointerException.class, () -> Hashes.fnv1a64((CharSequence) null));
        assertThrows(NullPointerException.class, () -> Hashes.fnv1a32((ByteBuffer) null));
        assertThrows(NullPointerException.class, () -> Hashes.maphash64((byte[]) null));
    }
}
