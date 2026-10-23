/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.jca;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AesXtsPlain64} (aes-xts-plain64, 512-bit key).
 *
 * <p>
 * The reference vectors were produced with an independent implementation
 * (Python "cryptography", AES-256-XTS, tweak = unit number as 64-bit
 * little-endian padded to 16 bytes) using:</p>
 * <ul>
 * <li>key[i] = (byte) (i * 7 + 3), for i in 0..63</li>
 * <li>plaintext[i] = (byte) (i * 31 + size)</li>
 * </ul>
 */
class AesXtsPlain64Test
{

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private static byte[] key()
    {
        byte[] k = new byte[64];
        for (int i = 0; i < k.length; i++)
        {
            k[i] = (byte) (i * 7 + 3);
        }
        return k;
    }

    private static byte[] pattern(int size)
    {
        byte[] p = new byte[size];
        for (int i = 0; i < size; i++)
        {
            p[i] = (byte) (i * 31 + size);
        }
        return p;
    }

    private static byte[] random(int size, long seed)
    {
        byte[] b = new byte[size];
        new Random(seed).nextBytes(b);
        return b;
    }

    private static String hex(byte[] b)
    {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b)
        {
            sb.append(String.format("%02x", x & 0xFF));
        }
        return sb.toString();
    }

    private static String sha256(byte[] b) throws Exception
    {
        return hex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    private static byte[] encrypt(AesXtsPlain64 x, byte[] plain, long unit) throws Exception
    {
        byte[] c = new byte[plain.length];
        x.encrypt(plain, c, unit);
        return c;
    }

    private static byte[] decrypt(AesXtsPlain64 x, byte[] cipher, long unit) throws Exception
    {
        byte[] p = new byte[cipher.length];
        x.decrypt(cipher, p, unit);
        return p;
    }

    /**
     * Straightforward block-by-block XTS used as an in-test reference.
     */
    private static byte[] referenceXts(byte[] key, byte[] in, long unit, boolean encrypt) throws Exception
    {
        SecretKeySpec k1 = new SecretKeySpec(Arrays.copyOfRange(key, 0, 32), "AES");
        SecretKeySpec k2 = new SecretKeySpec(Arrays.copyOfRange(key, 32, 64), "AES");

        Cipher data = Cipher.getInstance("AES/ECB/NoPadding");
        data.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, k1);
        Cipher tw = Cipher.getInstance("AES/ECB/NoPadding");
        tw.init(Cipher.ENCRYPT_MODE, k2);

        byte[] iv = new byte[16];
        for (int i = 0; i < 8; i++)
        {
            iv[i] = (byte) (unit >>> (8 * i));
        }
        byte[] t = tw.doFinal(iv);
        byte[] out = new byte[in.length];

        for (int off = 0; off < in.length; off += 16)
        {
            byte[] x = new byte[16];
            for (int i = 0; i < 16; i++)
            {
                x[i] = (byte) (in[off + i] ^ t[i]);
            }
            byte[] y = data.doFinal(x);
            for (int i = 0; i < 16; i++)
            {
                out[off + i] = (byte) (y[i] ^ t[i]);
            }
            int carry = 0;
            for (int i = 0; i < 16; i++)
            {
                int v = t[i] & 0xFF;
                int next = v >>> 7;
                t[i] = (byte) ((v << 1) | carry);
                carry = next;
            }
            if (carry != 0)
            {
                t[0] ^= (byte) 0x87;
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Known-answer tests (independent implementation)
    // ------------------------------------------------------------------
    @Test
    void knownAnswer_512_unit0_firstBlocks() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] c = encrypt(x, pattern(512), 0);

        String expectedPrefix
                = "C6DA2664A4FFB1E92729484791F56572"
                + "2EC767051BCC25A77B16D4C99934C653"
                + "823E1EDBC35AF909339B5D38900BBE2E"
                + "927CD8C6EE0ADD51687E50889E406E13";
        assertEquals(expectedPrefix.toLowerCase(), hex(Arrays.copyOf(c, 64)));
        assertEquals("554a27c8209e575140b7e4bc2f27a9ca4519511c3e322023cb5b479140423d1b", sha256(c));
    }

    @Test
    void knownAnswer_512_unit1() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        assertEquals("5c11d9845eb89e9157e0a3cfc611db54c4d3ec3b686994d129724c29a2d242e2",
                sha256(encrypt(x, pattern(512), 1)));
    }

    @Test
    void knownAnswer_4096_unit5000() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(4096, key());
        assertEquals("955825c0fa133293bf5996c8c52f8180bb0a219a8bde88993caf7729436dc28d",
                sha256(encrypt(x, pattern(4096), 5000)));
    }

    @Test
    void knownAnswer_wrapAroundUnitNumbers() throws Exception
    {
        // 0xFFFFFFFFFFFFFFF0 + size wraps modulo 2^64, as unsigned 64-bit.
        AesXtsPlain64 x512 = new AesXtsPlain64(512, key());
        assertEquals("8ca0629f0fc67b0a8191103a7cfcdbcd9a0635e4d7d380999e0b3d3715ce91a6",
                sha256(encrypt(x512, pattern(512), 0xFFFFFFFFFFFFFFF0L + 512)));

        AesXtsPlain64 x4096 = new AesXtsPlain64(4096, key());
        assertEquals("b1eea2108967588c1d8a80c876fccff10a2237244279f6aed24c3676d341c607",
                sha256(encrypt(x4096, pattern(4096), 0xFFFFFFFFFFFFFFF0L + 4096)));
    }

    @Test
    void knownAnswer_allEightBytesOfUnitNumberUsed() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(4096, key());
        assertEquals("6f295c3cbbc1e1d44c0b7c39086209c095820c6431af92b25043760e89ae4adc",
                sha256(encrypt(x, pattern(4096), 0x0102030405060708L)));
    }

    @Test
    void knownAnswer_highBitUnitNumberIsUnsigned() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(1024, key());
        assertEquals("c015e3c7d8c70992aedce16d5bd7c343288d93e4eea88f295b39b1e03814d72a",
                sha256(encrypt(x, pattern(1024), 0x8000000000000000L)));
    }

    @Test
    void knownAnswer_8192() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(8192, key());
        assertEquals("90843b47e21e3736ea21451234e8a448edc17fe87b114348143aaeaad617ee9a",
                sha256(encrypt(x, pattern(8192), 123456789L)));
    }

    // ------------------------------------------------------------------
    // Comparison with the in-test reference implementation
    // ------------------------------------------------------------------
    @Test
    void matchesReferenceImplementation_manySizesAndUnits() throws Exception
    {
        byte[] k = random(64, 1);
        long[] units =
        {
            0, 1, 2, 7, 255, 256, 4095, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE, -1L
        };
        for (int size : new int[]
        {
            512, 1024, 2048, 4096, 8192, 65536
        })
        {
            AesXtsPlain64 x = new AesXtsPlain64(size, k);
            for (long unit : units)
            {
                byte[] p = random(size, size ^ unit);
                byte[] expected = referenceXts(k, p, unit, true);
                byte[] c = encrypt(x, p, unit);
                assertArrayEquals(expected, c, "encrypt size=" + size + " unit=" + unit);
                assertArrayEquals(referenceXts(k, c, unit, false), decrypt(x, c, unit),
                        "decrypt size=" + size + " unit=" + unit);
            }
        }
    }

    @Test
    void tweakReductionIsExercised() throws Exception
    {
        // Over 4096/16 = 256 blocks the tweak overflows GF(2^128) many times.
        // A mismatch in the 0x87 reduction would show up against the reference.
        byte[] k = random(64, 99);
        AesXtsPlain64 x = new AesXtsPlain64(4096, k);
        for (long unit = 0; unit < 64; unit++)
        {
            byte[] p = new byte[4096];
            assertArrayEquals(referenceXts(k, p, unit, true), encrypt(x, p, unit));
        }
    }

    @Test
    void firstBlockMatchesDefinition() throws Exception
    {
        // C0 = AES_K1(P0 xor T0) xor T0, with T0 = AES_K2(IV)
        byte[] k = key();
        AesXtsPlain64 x = new AesXtsPlain64(512, k);
        byte[] p = pattern(512);
        long unit = 42;

        Cipher tw = Cipher.getInstance("AES/ECB/NoPadding");
        tw.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Arrays.copyOfRange(k, 32, 64), "AES"));
        byte[] iv = new byte[16];
        iv[0] = 42;
        byte[] t0 = tw.doFinal(iv);

        Cipher dc = Cipher.getInstance("AES/ECB/NoPadding");
        dc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Arrays.copyOfRange(k, 0, 32), "AES"));
        byte[] in = new byte[16];
        for (int i = 0; i < 16; i++)
        {
            in[i] = (byte) (p[i] ^ t0[i]);
        }
        byte[] expected = dc.doFinal(in);
        for (int i = 0; i < 16; i++)
        {
            expected[i] ^= t0[i];
        }

        assertArrayEquals(expected, Arrays.copyOf(encrypt(x, p, unit), 16));
    }

    @Test
    void keyIsSplitInHalves_firstHalfData_secondHalfTweak() throws Exception
    {
        byte[] k = random(64, 5);
        byte[] swapped = new byte[64];
        System.arraycopy(k, 32, swapped, 0, 32);
        System.arraycopy(k, 0, swapped, 32, 32);

        byte[] p = random(512, 6);
        byte[] c1 = encrypt(new AesXtsPlain64(512, k), p, 3);
        byte[] c2 = encrypt(new AesXtsPlain64(512, swapped), p, 3);
        assertFalse(Arrays.equals(c1, c2), "swapping halves must change the result");
        assertArrayEquals(referenceXts(k, p, 3, true), c1);
        assertArrayEquals(referenceXts(swapped, p, 3, true), c2);
    }

    // ------------------------------------------------------------------
    // Functional behaviour
    // ------------------------------------------------------------------
    @Test
    void roundTrip_allSupportedSizes() throws Exception
    {
        for (int size = 512; size <= 1 << 16; size <<= 1)
        {
            AesXtsPlain64 x = new AesXtsPlain64(size, key());
            byte[] p = random(size, size);
            byte[] c = encrypt(x, p, 17);
            assertFalse(Arrays.equals(p, c));
            assertArrayEquals(p, decrypt(x, c, 17));
        }
    }

    @Test
    void roundTrip_maxDataUnitSize() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(AesXtsPlain64.MAX_DATA_UNIT_SIZE, key());
        byte[] p = random(AesXtsPlain64.MAX_DATA_UNIT_SIZE, 7);
        byte[] c = encrypt(x, p, 1);
        assertArrayEquals(p, decrypt(x, c, 1));
    }

    @Test
    void deterministic_sameInputSameOutput() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 8);
        assertArrayEquals(encrypt(x, p, 9), encrypt(x, p, 9));
        assertArrayEquals(encrypt(x, p, 9), encrypt(new AesXtsPlain64(512, key()), p, 9));
    }

    @Test
    void differentUnitNumbers_giveDifferentCiphertext() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 10);
        assertFalse(Arrays.equals(encrypt(x, p, 0), encrypt(x, p, 1)));
    }

    @Test
    void decryptWithWrongUnit_doesNotRecoverPlaintext() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 11);
        byte[] c = encrypt(x, p, 5);
        assertFalse(Arrays.equals(p, decrypt(x, c, 6)));
    }

    @Test
    void decryptWithWrongKey_doesNotRecoverPlaintext() throws Exception
    {
        byte[] other = key();
        other[0] ^= 1;
        byte[] p = random(512, 12);
        byte[] c = encrypt(new AesXtsPlain64(512, key()), p, 5);
        assertFalse(Arrays.equals(p, decrypt(new AesXtsPlain64(512, other), c, 5)));
    }

    @Test
    void identicalPlaintextBlocks_produceDifferentCiphertextBlocks() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = new byte[512]; // 32 identical zero blocks
        byte[] c = encrypt(x, p, 0);
        List<String> seen = new ArrayList<>();
        for (int off = 0; off < 512; off += 16)
        {
            String b = hex(Arrays.copyOfRange(c, off, off + 16));
            assertFalse(seen.contains(b), "repeated ciphertext block at " + off);
            seen.add(b);
        }
    }

    @Test
    void changingOneBlock_onlyAffectsThatBlock() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 13);
        byte[] c1 = encrypt(x, p, 4);
        p[16 * 5 + 3] ^= 0x01;
        byte[] c2 = encrypt(x, p, 4);
        for (int blk = 0; blk < 32; blk++)
        {
            boolean same = Arrays.equals(
                    Arrays.copyOfRange(c1, blk * 16, blk * 16 + 16),
                    Arrays.copyOfRange(c2, blk * 16, blk * 16 + 16));
            assertEquals(blk != 5, same, "block " + blk);
        }
    }

    @Test
    void inputIsNotModified() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 14);
        byte[] copy = p.clone();
        byte[] c = encrypt(x, p, 1);
        assertArrayEquals(copy, p);
        byte[] cCopy = c.clone();
        decrypt(x, c, 1);
        assertArrayEquals(cCopy, c);
    }

    @Test
    void inPlace_encryptAndDecrypt() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(4096, key());
        byte[] p = random(4096, 15);
        byte[] expected = encrypt(x, p, 77);

        byte[] buf = p.clone();
        x.encrypt(buf, buf, 77);
        assertArrayEquals(expected, buf);
        x.decrypt(buf, buf, 77);
        assertArrayEquals(p, buf);
    }

    @Test
    void keyArrayMutationAfterConstruction_doesNotAffectInstance() throws Exception
    {
        byte[] k = key();
        AesXtsPlain64 x = new AesXtsPlain64(512, k);
        byte[] p = random(512, 16);
        byte[] before = encrypt(x, p, 0);
        Arrays.fill(k, (byte) 0);
        assertArrayEquals(before, encrypt(x, p, 0));
    }

    @Test
    void getDataUnitSize() throws Exception
    {
        assertEquals(4096, new AesXtsPlain64(4096, key()).getDataUnitSize());
    }

    // ------------------------------------------------------------------
    // Offset-based API
    // ------------------------------------------------------------------
    @Test
    void offsetApi_multipleUnitsInOneBuffer() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        int units = 6;
        byte[] plain = random(512 * units, 20);
        byte[] cipher = new byte[512 * units];

        for (int i = 0; i < units; i++)
        {
            x.encrypt(plain, i * 512, cipher, i * 512, 100 + i);
        }
        for (int i = 0; i < units; i++)
        {
            byte[] single = encrypt(x, Arrays.copyOfRange(plain, i * 512, (i + 1) * 512), 100 + i);
            assertArrayEquals(single, Arrays.copyOfRange(cipher, i * 512, (i + 1) * 512));
        }

        byte[] back = new byte[512 * units];
        for (int i = 0; i < units; i++)
        {
            x.decrypt(cipher, i * 512, back, i * 512, 100 + i);
        }
        assertArrayEquals(plain, back);
    }

    @Test
    void offsetApi_differentSourceAndDestinationOffsets() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 21);
        byte[] src = new byte[1024];
        System.arraycopy(p, 0, src, 100, 512);
        byte[] dst = new byte[2048];
        x.encrypt(src, 100, dst, 700, 3);
        assertArrayEquals(encrypt(x, p, 3), Arrays.copyOfRange(dst, 700, 1212));
        // bytes outside the destination region stay untouched
        assertEquals(0, dst[699]);
        assertEquals(0, dst[1212]);
    }

    @Test
    void offsetApi_inPlaceSameRegion() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] buf = random(1536, 22);
        byte[] original = buf.clone();
        x.encrypt(buf, 512, buf, 512, 8);
        assertArrayEquals(Arrays.copyOfRange(original, 0, 512), Arrays.copyOfRange(buf, 0, 512));
        assertArrayEquals(Arrays.copyOfRange(original, 1024, 1536), Arrays.copyOfRange(buf, 1024, 1536));
        x.decrypt(buf, 512, buf, 512, 8);
        assertArrayEquals(original, buf);
    }

    @Test
    void offsetApi_adjacentRegionsInSameArrayAreAllowed() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] buf = random(1024, 23);
        byte[] firstHalf = Arrays.copyOfRange(buf, 0, 512);
        x.encrypt(buf, 0, buf, 512, 2);
        assertArrayEquals(encrypt(x, firstHalf, 2), Arrays.copyOfRange(buf, 512, 1024));
    }

    // ------------------------------------------------------------------
    // Constructor errors
    // ------------------------------------------------------------------
    @Test
    void constructor_rejectsInvalidDataUnitSizes()
    {
        int[] invalid =
        {
            Integer.MIN_VALUE, -512, -1, 0, 1, 16, 256, 511, 513, 768, 1000, 1536, 4097,
            AesXtsPlain64.MAX_DATA_UNIT_SIZE + 1, AesXtsPlain64.MAX_DATA_UNIT_SIZE * 2, 1 << 30,
            (1 << 24) + 512
        };
        for (int size : invalid)
        {
            assertThrows(IllegalArgumentException.class, () -> new AesXtsPlain64(size, key()), "size " + size);
        }
    }

    @Test
    void constructor_acceptsAllPowersOfTwoInRange() throws Exception
    {
        for (int size = AesXtsPlain64.MIN_DATA_UNIT_SIZE; size <= AesXtsPlain64.MAX_DATA_UNIT_SIZE; size <<= 1)
        {
            assertEquals(size, new AesXtsPlain64(size, key()).getDataUnitSize());
        }
    }

    @Test
    void constructor_rejectsNullKey()
    {
        assertThrows(IllegalArgumentException.class, () -> new AesXtsPlain64(512, null));
    }

    @Test
    void constructor_rejectsWrongKeyLengths()
    {
        for (int len : new int[]
        {
            0, 1, 16, 32, 48, 63, 65, 128
        })
        {
            assertThrows(IllegalArgumentException.class,
                    () -> new AesXtsPlain64(512, random(len, len)), "key length " + len);
        }
    }

    @Test
    void constructor_rejectsEqualKeyHalves()
    {
        byte[] k = new byte[64]; // all zeros
        assertThrows(IllegalArgumentException.class, () -> new AesXtsPlain64(512, k));

        byte[] half = random(32, 30);
        byte[] equal = new byte[64];
        System.arraycopy(half, 0, equal, 0, 32);
        System.arraycopy(half, 0, equal, 32, 32);
        assertThrows(IllegalArgumentException.class, () -> new AesXtsPlain64(512, equal));
    }

    @Test
    void constructor_acceptsHalvesDifferingInOneBit() throws Exception
    {
        byte[] half = random(32, 31);
        byte[] k = new byte[64];
        System.arraycopy(half, 0, k, 0, 32);
        System.arraycopy(half, 0, k, 32, 32);
        k[63] ^= 1;
        assertNotNull(new AesXtsPlain64(512, k));
    }

    // ------------------------------------------------------------------
    // Operation errors
    // ------------------------------------------------------------------
    @Test
    void encryptDecrypt_rejectNullArguments() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] b = new byte[512];
        assertThrows(NullPointerException.class, () -> x.encrypt(null, b, 0));
        assertThrows(NullPointerException.class, () -> x.encrypt(b, null, 0));
        assertThrows(NullPointerException.class, () -> x.decrypt(null, b, 0));
        assertThrows(NullPointerException.class, () -> x.decrypt(b, null, 0));
        assertThrows(NullPointerException.class, () -> x.encrypt(null, 0, b, 0, 0));
        assertThrows(NullPointerException.class, () -> x.decrypt(b, 0, null, 0, 0));
    }

    @Test
    void encryptDecrypt_rejectWrongLengths() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] ok = new byte[512];
        for (int len : new int[]
        {
            0, 16, 511, 513, 1024
        })
        {
            byte[] bad = new byte[len];
            assertThrows(IllegalArgumentException.class, () -> x.encrypt(bad, ok, 0), "src len " + len);
            assertThrows(IllegalArgumentException.class, () -> x.encrypt(ok, bad, 0), "dst len " + len);
            assertThrows(IllegalArgumentException.class, () -> x.decrypt(bad, ok, 0), "src len " + len);
            assertThrows(IllegalArgumentException.class, () -> x.decrypt(ok, bad, 0), "dst len " + len);
        }
    }

    @Test
    void offsetApi_rejectsOutOfBoundsRanges() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] buf = new byte[1024];
        byte[] small = new byte[511];

        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, -1, buf, 512, 0));
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, 513, buf, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, 0, buf, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, 0, buf, 513, 0));
        assertThrows(IllegalArgumentException.class, () -> x.decrypt(small, 0, buf, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> x.decrypt(buf, 0, small, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, Integer.MAX_VALUE, buf, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, 0, buf, Integer.MIN_VALUE, 0));
    }

    @Test
    void offsetApi_rejectsPartialOverlap() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] buf = new byte[2048];
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, 0, buf, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(buf, 100, buf, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> x.decrypt(buf, 0, buf, 511, 0));
        assertThrows(IllegalArgumentException.class, () -> x.decrypt(buf, 511, buf, 0, 0));
    }

    @Test
    void failedCall_doesNotBreakInstance() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 40);
        byte[] expected = encrypt(x, p, 1);
        assertThrows(IllegalArgumentException.class, () -> x.encrypt(new byte[10], new byte[512], 1));
        assertArrayEquals(expected, encrypt(x, p, 1));
    }

    // ------------------------------------------------------------------
    // Concurrency
    // ------------------------------------------------------------------
    @Test
    void concurrentUse_isThreadSafe() throws Exception
    {
        final AesXtsPlain64 x = new AesXtsPlain64(4096, key());
        final int threads = 8;
        final int iterations = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try
        {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++)
            {
                final int id = t;
                futures.add(pool.submit((Callable<Boolean>) () ->
                {
                    for (int i = 0; i < iterations; i++)
                    {
                        long unit = id * 1000L + i;
                        byte[] p = random(4096, unit);
                        byte[] c = encrypt(x, p, unit);
                        if (!Arrays.equals(c, referenceXts(key(), p, unit, true)))
                        {
                            return false;
                        }
                        if (!Arrays.equals(p, decrypt(x, c, unit)))
                        {
                            return false;
                        }
                    }
                    return true;
                }));
            }
            for (Future<Boolean> f : futures)
            {
                assertTrue(f.get());
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    @Test
    void operationsWorkFromThreadsOtherThanTheCreator() throws Exception
    {
        AesXtsPlain64 x = new AesXtsPlain64(512, key());
        byte[] p = random(512, 50);
        byte[] expected = encrypt(x, p, 3);
        final byte[][] result = new byte[1][];
        final Throwable[] error = new Throwable[1];
        Thread th = new Thread(() ->
        {
            try
            {
                result[0] = encrypt(x, p, 3);
            }
            catch (Throwable e)
            {
                error[0] = e;
            }
        });
        th.start();
        th.join();
        assertNull(error[0]);
        assertArrayEquals(expected, result[0]);
    }
}