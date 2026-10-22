/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.cache.persistent;

import io.nut.base.commons.cache.CacheType;
import io.nut.base.commons.security.KeyMaterial;
import io.nut.base.commons.serializer.BasicSerializers;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class EncryptedFileCacheStoreTest
{

    /** Size of the file header: 'N' 'U' 'T' 'C' + version. */
    private static final int HEADER_SIZE = 5;

    @TempDir
    Path dir;

    private Path file()
    {
        return dir.resolve("store.bin");
    }

    private SecretKey key()
    {
        return KeyMaterial.aesKey(KeyMaterial.randomKey());
    }

    private FileCacheStore<String, String> store(SecretKey key)
    {
        return new FileCacheStore<>(file(), BasicSerializers.STRING, BasicSerializers.STRING, key);
    }

    @Test
    public void testPutGetAndReopen()
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            assertTrue(store.isEmpty());
            store.put("one", "1", Long.MAX_VALUE);
            store.put("two", "2", Long.MAX_VALUE);
            assertEquals(2, store.size());
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
            assertNull(store.get("three"));
            assertTrue(store.containsKey("one"));
        }
        try (FileCacheStore<String, String> store = store(key))
        {
            assertEquals(2, store.size());
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
        }
    }

    @Test
    public void testVersion2Header() throws IOException
    {
        try (FileCacheStore<String, String> store = store(key()))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        byte[] bytes = Files.readAllBytes(file());
        assertEquals(HEADER_SIZE, bytes.length - (bytes.length - HEADER_SIZE));
        assertEquals('N', bytes[0]);
        assertEquals('U', bytes[1]);
        assertEquals('T', bytes[2]);
        assertEquals('C', bytes[3]);
        assertEquals(2, bytes[4]);
    }

    @Test
    public void testPlaintextIsNotOnDisk() throws IOException
    {
        String secretKey = "secret-key-42";
        String secretValue = "secret-value-42";
        try (FileCacheStore<String, String> store = store(key()))
        {
            store.put(secretKey, secretValue, Long.MAX_VALUE);
        }
        byte[] bytes = Files.readAllBytes(file());
        String content = new String(bytes, StandardCharsets.ISO_8859_1);
        assertFalse(content.contains(secretKey), "the key must not be readable in clear");
        assertFalse(content.contains(secretValue), "the value must not be readable in clear");
    }

    @Test
    public void testExpirationIsNotOnDisk() throws IOException
    {
        long expiresAt = 1_900_000_000_000L;
        try (FileCacheStore<String, String> store = store(key()))
        {
            store.put("a", "1", expiresAt);
        }
        byte[] bytes = Files.readAllBytes(file());
        // the expiry is stored as a big endian long inside the encrypted body
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (int i = 0; i + 8 <= bytes.length; i++)
        {
            if (buffer.getLong(i) == expiresAt)
            {
                fail("the expiration must not be readable in clear");
            }
        }
    }

    @Test
    public void testWrongKeyIsRejected() throws IOException
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        long size = Files.size(file());
        assertThrows(UncheckedIOException.class, () -> store(key()));
        assertEquals(size, Files.size(file()), "a rejected store must not modify the file");
    }

    @Test
    public void testMissingKeyIsRejected()
    {
        try (FileCacheStore<String, String> store = store(key()))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        assertThrows(UncheckedIOException.class, () -> new FileCacheStore<>(file(),
                BasicSerializers.STRING, BasicSerializers.STRING));
    }

    @Test
    public void testPlainFileWithKeyIsRejected()
    {
        try (FileCacheStore<String, String> store = new FileCacheStore<>(file(),
                BasicSerializers.STRING, BasicSerializers.STRING))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        assertThrows(UncheckedIOException.class, () -> store(key()));
    }

    @Test
    public void testTamperedCiphertextIsDetected() throws IOException
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        tamperWithCiphertext(file());
        assertThrows(UncheckedIOException.class, () -> store(key));
    }

    @Test
    public void testTruncatedTailIsDiscarded() throws IOException
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
        }
        long validLength = Files.size(file());
        try (RandomAccessFile raf = new RandomAccessFile(file().toFile(), "rw"))
        {
            raf.seek(validLength);
            raf.write(new byte[]{1, 0, 0});
        }
        assertTrue(Files.size(file()) > validLength);
        try (FileCacheStore<String, String> store = store(key))
        {
            assertEquals(2, store.size());
            assertEquals("1", store.get("a"));
            assertEquals("2", store.get("b"));
        }
        assertEquals(validLength, Files.size(file()));
    }

    @Test
    public void testCorruptCiphertextDiscardsTheTail() throws IOException
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
        }
        byte[] bytes = Files.readAllBytes(file());
        bytes[bytes.length - 6] ^= 0x7F;
        Files.write(file(), bytes);
        try (FileCacheStore<String, String> store = store(key))
        {
            assertEquals(1, store.size());
            assertEquals("1", store.get("a"));
            assertNull(store.get("b"));
        }
    }

    @Test
    public void testOverwrite()
    {
        try (FileCacheStore<String, String> store = store(key()))
        {
            store.put("k", "v1", Long.MAX_VALUE);
            store.put("k", "v2", Long.MAX_VALUE);
            assertEquals(1, store.size());
            assertEquals("v2", store.get("k"));
        }
    }

    @Test
    public void testNullValueAndNullKey()
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("k", null, Long.MAX_VALUE);
            assertTrue(store.containsKey("k"));
            assertNull(store.get("k"));
            assertEquals(1, store.size());

            store.put(null, "nullKey", Long.MAX_VALUE);
            assertTrue(store.containsKey(null));
            assertEquals("nullKey", store.get(null));
            assertEquals(2, store.size());
        }
        try (FileCacheStore<String, String> store = store(key))
        {
            assertTrue(store.containsKey("k"));
            assertNull(store.get("k"));
            assertEquals(2, store.size());
            assertTrue(store.containsKey(null));
            assertEquals("nullKey", store.get(null));
        }
    }

    @Test
    public void testExpiredOnReadAndPurge()
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            long past = System.currentTimeMillis() - 1000;
            store.put("expired", "v", past);
            store.put("live", "v", Long.MAX_VALUE);
            assertEquals(1, store.size());
            assertNull(store.get("expired"));
            assertFalse(store.containsKey("expired"));
            store.purgeExpired();
            assertEquals(1, store.size());
            assertEquals("v", store.get("live"));
        }
        try (FileCacheStore<String, String> store = store(key))
        {
            assertEquals(1, store.size());
            assertEquals("v", store.get("live"));
            assertFalse(store.containsKey("expired"));
        }
    }

    @Test
    public void testClear()
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
            store.clear();
            assertTrue(store.isEmpty());
            assertEquals(0, store.size());
            assertNull(store.get("a"));
        }
        try (FileCacheStore<String, String> store = store(key))
        {
            assertTrue(store.isEmpty());
            assertNull(store.get("a"));
        }
    }

    @Test
    public void testCompaction()
    {
        SecretKey key = key();
        String value = String.join("", java.util.Collections.nCopies(64, "x"));
        FileCacheStore<String, String> store = store(key);
        for (int i = 0; i < 400; i++)
        {
            store.put("k" + i, value, Long.MAX_VALUE);
        }
        store.close();
        long firstClose = file().toFile().length();

        store = store(key);
        for (int i = 0; i < 400; i++)
        {
            store.put("k" + i, value, Long.MAX_VALUE);
        }
        long peak = file().toFile().length();
        assertTrue(peak > firstClose * 3 / 2,
                "overwrites should leave dead records behind: " + firstClose + " -> " + peak);
        store.close();
        long compacted = file().toFile().length();
        assertTrue(compacted < peak,
                "compaction should shrink the file: " + peak + " -> " + compacted);

        try (FileCacheStore<String, String> reopened = store(key))
        {
            assertEquals(400, reopened.size());
            for (int i = 0; i < 400; i++)
            {
                assertEquals(value, reopened.get("k" + i));
            }
        }
    }

    @Test
    public void testEncryptionIsNotDeterministic() throws IOException
    {
        SecretKey key = key();
        Path other = dir.resolve("other.bin");
        try (FileCacheStore<String, String> store = store(key))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        try (FileCacheStore<String, String> store = new FileCacheStore<>(other,
                BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        assertFalse(java.util.Arrays.equals(Files.readAllBytes(file()), Files.readAllBytes(other)),
                "the same content must not produce the same file: the initialization vector must be random");
    }

    @Test
    public void testSingleSerializerConstructorAndFactory()
    {
        SecretKey key = key();
        Path single = dir.resolve("single.bin");
        try (FileCacheStore<String, String> store = new FileCacheStore<>(single, BasicSerializers.STRING, key))
        {
            store.put("a", "1", Long.MAX_VALUE);
            assertEquals("1", store.get("a"));
        }
        try (FileCacheStore<String, String> store = new FileCacheStore<>(single, BasicSerializers.STRING, key))
        {
            assertEquals("1", store.get("a"));
        }

        Path factory = dir.resolve("factory.bin");
        try (FileCacheStore<String, String> store = FileCacheStore.of(factory, BasicSerializers.STRING, key))
        {
            store.put("a", "1", Long.MAX_VALUE);
            assertEquals("1", store.get("a"));
        }
        try (FileCacheStore<String, String> store = FileCacheStore.of(factory, BasicSerializers.STRING, key))
        {
            assertEquals("1", store.get("a"));
        }
    }

    @Test
    public void testGetKey()
    {
        SecretKey key = key();
        try (FileCacheStore<String, String> store = store(key))
        {
            assertSame(key, store.getKey());
        }
        try (FileCacheStore<String, String> store = new FileCacheStore<>(dir.resolve("plain.bin"),
                BasicSerializers.STRING, BasicSerializers.STRING))
        {
            assertNull(store.getKey());
        }
    }

    @Test
    public void testPersistentCacheWithKey()
    {
        SecretKey key = key();
        Path path = dir.resolve("persistent.bin");
        try (PersistentCache<String, String> cache = PersistentCache.open(path, CacheType.LRU_LFU, 16,
                Long.MAX_VALUE, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            cache.put("a", "1");
            assertEquals("1", cache.get("a"));
            assertEquals("1", cache.get("a", k -> "2"));
        }
        try (PersistentCache<String, String> cache = PersistentCache.open(path, CacheType.LRU_LFU, 16,
                Long.MAX_VALUE, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            assertEquals("1", cache.get("a"));
        }
    }

    @Test
    public void testFileIsNotWorldReadable()
    {
        try (FileCacheStore<String, String> store = store(key()))
        {
            store.put("a", "1", Long.MAX_VALUE);
        }
        try
        {
            java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(file());
            assertEquals(java.util.EnumSet.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE), permissions);
        }
        catch (UnsupportedOperationException e)
        {
            // not a POSIX filesystem
        }
        catch (IOException e)
        {
            fail("cannot read the permissions: " + e);
        }
    }

    /**
     * Flips a bit of the ciphertext of the first record and repairs its CRC,
     * so that only the authentication tag can detect the tampering.
     */
    private static void tamperWithCiphertext(Path file) throws IOException
    {
        byte[] bytes = Files.readAllBytes(file);
        int offset = HEADER_SIZE;
        int cipherLen = ByteBuffer.wrap(bytes, offset + 1, 4).getInt();
        assertTrue(cipherLen > 12);
        bytes[offset + 5 + 12] ^= 0x01;
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, 5 + cipherLen);
        ByteBuffer.wrap(bytes, offset + 5 + cipherLen, 4).putInt((int) crc.getValue());
        Files.write(file, bytes);
    }
}
