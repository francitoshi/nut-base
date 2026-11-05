/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

import io.github.francitoshi.nut.base.commons.cache.CacheType;
import io.github.francitoshi.nut.base.commons.encoding.Hex;
import io.github.francitoshi.nut.base.commons.jca.Kr;
import io.github.francitoshi.nut.base.commons.security.KeyMaterial;
import io.github.francitoshi.nut.base.commons.serializer.BasicSerializers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class Sha256FileCacheStoreTest
{

    @TempDir
    Path dir;

    private static final int HEAD_SIZE = 13;

    private Sha256FileCacheStore<String, String> store()
    {
        return new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING);
    }

    private SecretKey key()
    {
        return KeyMaterial.aesKey(KeyMaterial.randomKey());
    }

    private Path entry(String key)
    {
        byte[] digest = Kr.getInstance().sha256.digest(key.getBytes(StandardCharsets.UTF_8));
        return dir.resolve(Hex.encode(digest));
    }

    private int entryFiles()
    {
        try
        {
            int count = 0;
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir))
            {
                for (Path file : files)
                {
                    count++;
                }
            }
            return count;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private SortedSet<String> entryNames() throws IOException
    {
        SortedSet<String> names = new TreeSet<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir))
        {
            for (Path file : files)
            {
                names.add(file.getFileName().toString());
            }
        }
        return names;
    }

    @Test
    public void testPutGet()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            assertTrue(store.isEmpty());
            assertEquals(0, store.size());

            store.put("one", "1", Long.MAX_VALUE);
            store.put("two", "2", Long.MAX_VALUE);
            assertEquals(2, store.size());
            assertFalse(store.isEmpty());
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
            assertNull(store.get("three"));
            assertTrue(store.containsKey("one"));
            assertTrue(store.containsKey("two"));
            assertFalse(store.containsKey("three"));
        }
    }

    @Test
    public void testOneFilePerEntryNamedAfterSha256OfKey()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("one", "1", Long.MAX_VALUE);
            assertTrue(Files.exists(entry("one")));
            assertFalse(Files.exists(entry("two")));
            store.put("two", "2", Long.MAX_VALUE);
            assertTrue(Files.exists(entry("two")));
            assertEquals(2, entryFiles());
        }
    }

    @Test
    public void testFileFormat()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("one", "1", Long.MAX_VALUE);
        }
        try
        {
            byte[] data = Files.readAllBytes(entry("one"));
            assertEquals('N', data[0]);
            assertEquals('U', data[1]);
            assertEquals('T', data[2]);
            assertEquals('E', data[3]);
            assertEquals(1, data[4]);
            // expiration comes in clear right after the header, so an expiry
            // check never reads the value
            assertTrue(data.length > HEAD_SIZE);
        }
        catch (IOException e)
        {
            fail(e);
        }
    }

    @Test
    public void testOverwrite()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "v1", Long.MAX_VALUE);
            store.put("k", "v2", Long.MAX_VALUE);
            assertEquals(1, store.size());
            assertEquals(1, entryFiles());
            assertEquals("v2", store.get("k"));
        }
    }

    @Test
    public void testNullValue()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("n", null, Long.MAX_VALUE);
            assertTrue(store.containsKey("n"));
            assertEquals(1, store.size());
            assertNull(store.get("n"));
        }
    }

    @Test
    public void testRemove()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);
            assertTrue(store.containsKey("k"));
            store.remove("k");
            assertFalse(store.containsKey("k"));
            assertNull(store.get("k"));
            assertEquals(0, store.size());
            assertEquals(0, entryFiles());
            // removing an absent key has no effect
            assertDoesNotThrow(() -> store.remove("absent"));
        }
    }

    @Test
    public void testExpiration()
    {
        long now = System.currentTimeMillis();
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("past", "v", now - 1000);
            store.put("future", "v", now + TimeUnit.HOURS.toMillis(1));
            assertFalse(store.containsKey("past"));
            assertTrue(store.containsKey("future"));
            assertNull(store.get("past"));
            assertEquals("v", store.get("future"));
            assertEquals(1, store.size());
            // the expired entry is lazily discarded
            assertFalse(Files.exists(entry("past")));
        }
    }

    @Test
    public void testPurgeExpired()
    {
        long now = System.currentTimeMillis();
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("a", "1", now - 1000);
            store.put("b", "2", now - 2000);
            store.put("c", "3", Long.MAX_VALUE);
            assertEquals(1, store.size());
            store.purgeExpired();
            assertFalse(store.containsKey("a"));
            assertFalse(store.containsKey("b"));
            assertTrue(store.containsKey("c"));
            assertEquals(1, store.size());
            assertEquals(1, entryFiles());
        }
    }

    @Test
    public void testClear()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
            store.put("c", "3", Long.MAX_VALUE);
            store.clear();
            assertEquals(0, store.size());
            assertTrue(store.isEmpty());
            assertEquals(0, entryFiles());
        }
    }

    @Test
    public void testReopen()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("a", "alpha", Long.MAX_VALUE);
            store.put("b", "beta", Long.MAX_VALUE);
            store.put("a", "alpha2", Long.MAX_VALUE);
        }
        try (Sha256FileCacheStore<String, String> store = store())
        {
            assertEquals(2, store.size());
            assertEquals("alpha2", store.get("a"));
            assertEquals("beta", store.get("b"));
        }
    }

    @Test
    public void testNonStringKeys()
    {
        try (Sha256FileCacheStore<Long, byte[]> store = new Sha256FileCacheStore<>(dir, BasicSerializers.LONG, BasicSerializers.BYTE_ARRAY_1D))
        {
            byte[] image = new byte[4096];
            for (int i = 0; i < image.length; i++)
            {
                image[i] = (byte) i;
            }
            store.put(123456789L, image, Long.MAX_VALUE);
            assertArrayEquals(image, store.get(123456789L));
            assertEquals(1, store.size());
        }
        try (Sha256FileCacheStore<Long, byte[]> store = new Sha256FileCacheStore<>(dir, BasicSerializers.LONG, BasicSerializers.BYTE_ARRAY_1D))
        {
            assertEquals(4096, store.get(123456789L).length);
        }
    }

    @Test
    public void testNullKeyRejected()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            assertThrows(NullPointerException.class, () -> store.put(null, "v", Long.MAX_VALUE));
            assertThrows(NullPointerException.class, () -> store.get(null));
        }
    }

    @Test
    public void testLeftoverTmpFilesRemovedOnOpen() throws IOException
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);
        }
        // simulate a crash mid-put: the entry is moved out of its final name
        // into the temporary name of the write in progress
        Path entry = entry("k");
        Path tmp = entry.resolveSibling(entry.getFileName() + ".tmp");
        Files.move(entry, tmp, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(tmp));
        assertFalse(Files.exists(entry));
        try (Sha256FileCacheStore<String, String> store = store())
        {
            // opening cleans the tmp leftovers; the entry was lost by the
            // simulated crash, so it reads as a miss
            assertFalse(store.containsKey("k"));
            assertEquals(0, entryFiles());
        }
    }

    @Test
    public void testCorruptEntryIsMissAndRemoved()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);
        }
        try
        {
            Files.write(entry("k"), new byte[]{'x', 'y', 'z'});
        }
        catch (IOException e)
        {
            fail(e);
        }
        try (Sha256FileCacheStore<String, String> store = store())
        {
            assertFalse(store.containsKey("k"));
            assertNull(store.get("k"));
            assertFalse(Files.exists(entry("k")));
            assertEquals(0, store.size());
        }
    }

    @Test
    public void testTruncatedTailIsMissAndRemoved()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);
        }
        try
        {
            byte[] head = Files.readAllBytes(entry("k"));
            // cut the record in the middle of the value: the head survives,
            // the crc does not match
            byte[] truncated = new byte[head.length - 1];
            System.arraycopy(head, 0, truncated, 0, truncated.length);
            Files.write(entry("k"), truncated);
        }
        catch (IOException e)
        {
            fail(e);
        }
        try (Sha256FileCacheStore<String, String> store = store())
        {
            assertNull(store.get("k"));
            assertFalse(Files.exists(entry("k")));
        }
    }

    @Test
    public void testUnsupportedVersionIsHardError()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);
        }
        try
        {
            byte[] data = Files.readAllBytes(entry("k"));
            data[4] = 9; // unknown version
            Files.write(entry("k"), data);
        }
        catch (IOException e)
        {
            fail(e);
        }
        try (Sha256FileCacheStore<String, String> store = store())
        {
            assertThrows(UncheckedIOException.class, () -> store.get("k"));
            // a hard error never removes the entry
            assertTrue(Files.exists(entry("k")));
        }
    }

    @Test
    public void testEncryptedPutGetAndReopen()
    {
        SecretKey key = key();
        try (Sha256FileCacheStore<String, String> store
                = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            store.put("one", "1", Long.MAX_VALUE);
            store.put("two", "2", Long.MAX_VALUE);
            store.put("secret", null, Long.MAX_VALUE);
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
            assertNull(store.get("secret"));
            assertEquals(3, store.size());
            assertTrue(store.containsKey("secret"));
        }
        try (Sha256FileCacheStore<String, String> store = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
            assertNull(store.get("secret"));
            assertTrue(store.containsKey("secret"));
            assertEquals(3, store.size());
        }
    }

    @Test
    public void testEncryptedFileFormat() throws IOException
    {
        SecretKey key = key();
        try (Sha256FileCacheStore<String, String> store = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            store.put("k", "wide-visible-image", Long.MAX_VALUE);
        }
        byte[] data = Files.readAllBytes(entry("k"));
        assertEquals('N', data[0]);
        assertEquals('U', data[1]);
        assertEquals('T', data[2]);
        assertEquals('E', data[3]);
        assertEquals(2, data[4]);
        // only the expiration and the cipher length stay in clear
        String all = new String(data, StandardCharsets.ISO_8859_1);
        assertFalse(all.contains("wide-visible-image"));
        assertTrue(data.length >= HEAD_SIZE + 4 + AeadRecordCipher.MIN_BLOB_BYTES);
    }

    @Test
    public void testWrongKeyIsHardError()
    {
        SecretKey key = key();
        SecretKey wrongKey = key();
        try (Sha256FileCacheStore<String, String> store = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            store.put("k", "v", Long.MAX_VALUE);
        }
        try (Sha256FileCacheStore<String, String> store = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING, wrongKey))
        {
            // the value only authenticates on a decryption: a wrong key is a
            // hard error whenever the value is read, never an empty cache
            assertThrows(UncheckedIOException.class, () -> store.get("k"));
            // the head stays in the clear, so presence and expiry are
            // decidable without the key
            assertTrue(store.containsKey("k"));
            assertEquals(1, store.size());
            assertTrue(Files.exists(entry("k")));
        }
    }

    @Test
    public void testPlaintextStoreRefusesEncryptedEntry() throws IOException
    {
        SecretKey key = key();
        try (Sha256FileCacheStore<String, String> store = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING, key))
        {
            store.put("k", "v", Long.MAX_VALUE);
        }
        try (Sha256FileCacheStore<String, String> store = new Sha256FileCacheStore<>(dir, BasicSerializers.STRING, BasicSerializers.STRING))
        {
            assertThrows(UncheckedIOException.class, () -> store.get("k"));
            assertThrows(UncheckedIOException.class, () -> store.containsKey("k"));
            assertTrue(Files.exists(entry("k")));
        }
    }

    @Test
    public void testPersistentCacheIntegration()
    {
        try (PersistentCache<String, String> cache = PersistentCache.openPerEntry(dir, CacheType.TINY_LFU, 64, 1, TimeUnit.HOURS, BasicSerializers.STRING, BasicSerializers.STRING))
        {
            assertTrue(cache.isEmpty());
            assertEquals("first", cache.get("one", k -> "first"));
            assertEquals("first", cache.get("one"));
            assertEquals("second", cache.get("other", k -> "second"));
            assertFalse(cache.isEmpty());
        }
        try (PersistentCache<String, String> cache = PersistentCache.openPerEntry(dir, CacheType.TINY_LFU, 64, 1, TimeUnit.HOURS, BasicSerializers.STRING, BasicSerializers.STRING))
        {
            assertFalse(cache.isEmpty());
            assertEquals("first", cache.get("one"));
            // the creator is not invoked again: the store has the entry
            assertEquals("first", cache.get("one", k -> "unexpected"));
            assertEquals("second", cache.get("other"));
        }
    }

    @Test
    public void testCrcCoversHeaderAndValue()
    {
        try (Sha256FileCacheStore<String, String> store = store())
        {
            store.put("k", "test-value-bytes", Long.MAX_VALUE);
        }
        try
        {
            byte[] data = Files.readAllBytes(entry("k"));
            CRC32 crc = new CRC32();
            crc.update(data, 0, data.length - 4);
            int storedCrc = ((data[data.length - 4] & 0xFF) << 24)
                    | ((data[data.length - 3] & 0xFF) << 16)
                    | ((data[data.length - 2] & 0xFF) << 8)
                    | (data[data.length - 1] & 0xFF);
            assertEquals((int) crc.getValue(), storedCrc);
        }
        catch (IOException e)
        {
            fail(e);
        }
    }
}
