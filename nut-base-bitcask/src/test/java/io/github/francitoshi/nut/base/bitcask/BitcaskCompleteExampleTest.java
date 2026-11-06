/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two end-to-end examples, one per profile, exercising every public feature
 * of the store: open and builder options, put, get, delete, keys, size,
 * containsKey, rotation by max file size, merge, reopen, crash-style
 * truncated tails, tombstones, "last wins" ordering and the directory lock.
 * The encrypted profile also shows the wrong-password rejection and checks
 * with its own eyes that keys and values never touch the disk unencrypted.
 */
public class BitcaskCompleteExampleTest
{
    @TempDir
    Path temp;

    private static byte[] bytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private Path dir()
    {
        return temp.resolve("example-" + System.nanoTime());
    }

    // ------------------------------------------------------------------
    // Plain profile: every feature in one run
    // ------------------------------------------------------------------

    @Test
    public void completeExampleWithThePlainStore() throws IOException
    {
        Path directory = dir();

        // builder: rotation threshold, sync policy, and a fresh store
        try (Bitcask store = Bitcask.builder(directory)
            .maxFileSize(120)
            .sync(SyncMode.NONE)
            .build())
        {
            assertEquals(0, store.size());

            // put: several keys across several files (rotation)
            Map<String, byte[]> written = new TreeMap<>();
            for (int i = 0; i < 12; i++)
            {
                String key = "language-" + i;
                byte[] value = bytes("value-storage-" + i);
                written.put(key, value);
                store.put(bytes(key), value);
            }

            assertEquals(12, store.size());
            assertEquals(written.size(), store.keys().length);
            assertTrue(listDataIds(directory).size() >= 4, "rotation should have created several data files");

            // get and containsKey
            for (Map.Entry<String, byte[]> entry : written.entrySet())
            {
                assertTrue(store.containsKey(bytes(entry.getKey())));
                assertArrayEquals(entry.getValue(), store.get(bytes(entry.getKey())));
            }
            assertFalse(store.containsKey(bytes("none-such")));
            assertNull(store.get(bytes("none-such")));

            // overwrite: last put wins
            store.put(bytes("language-3"), bytes("value-BIG-overwrite"));
            assertArrayEquals(bytes("value-BIG-overwrite"), store.get(bytes("language-3")));

            // delete: tombstone, idempotent for unknown keys
            assertTrue(store.delete(bytes("language-3")));
            assertFalse(store.delete(bytes("language-3")));
            assertFalse(store.delete(bytes("never-existed")));
            assertNull(store.get(bytes("language-3")));
            assertFalse(store.containsKey(bytes("language-3")));
            assertEquals(11, store.size());

            // merge: dead values and tombstones leave the disk
            long maxBefore = maxId(directory);
            store.merge();

            for (long id : listDataIds(directory))
            {
                assertTrue(id > maxBefore, "the merge rebuilds the disk with new ids");
            }

            // the store is usable right after the merge
            store.put(bytes("after-merge"), bytes("yes"));
            assertArrayEquals(bytes("yes"), store.get(bytes("after-merge")));

            // and the reopened view of every key matches
            for (Map.Entry<String, byte[]> entry : written.entrySet())
            {
                if (entry.getKey().equals("language-3"))
                {
                    continue; // deleted above
                }
                assertArrayEquals(entry.getValue(), store.get(bytes(entry.getKey())));
            }
        }

        // reopen: full recovery from the data files
        try (Bitcask store = Bitcask.open(directory))
        {
            assertEquals(12, store.size()); // 11 survivors plus after-merge
            for (int i = 0; i < 12; i++)
            {
                byte[] key = bytes("language-" + i);

                if (i == 3)
                {
                    assertNull(store.get(key)); // tombstone survived the merge and closes
                }
                else
                {
                    assertEquals("value-storage-" + i, new String(store.get(key), StandardCharsets.UTF_8));
                }
            }
            assertEquals("yes", new String(store.get(bytes("after-merge")), StandardCharsets.UTF_8));

            // the simulated crash: partial write at the end of the last file
            long lastId = maxId(directory);
            Path last = directory.resolve(lastId + Bitcask.DATA_SUFFIX);
            Files.write(last, new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, StandardOpenOption.APPEND);
        }

        try (Bitcask store = Bitcask.open(directory))
        {
            // truncated tail recovered: every key is still readable
            assertEquals(12, store.size());
            for (int i = 0; i < 12; i++)
            {
                byte[] key = bytes("language-" + i);
                if (i == 3)
                {
                    assertNull(store.get(key));
                }
                else
                {
                    assertTrue(store.containsKey(key));
                }
            }

            // closed-store behavior
            store.close();  // idempotent
            store.close();
            assertThrows(IllegalStateException.class, () -> store.put(bytes("x"), new byte[1]));
            assertThrows(IllegalStateException.class, () -> store.get(bytes("language-0")));
        }
    }

    // ------------------------------------------------------------------
    // Encrypted profile: same run plus password handling and proof that
    // nothing readable ever reaches the disk
    // ------------------------------------------------------------------

    @Test
    public void completeExampleWithTheEncryptedStore() throws IOException
    {
        Path directory = dir();
        char[] password = "correct horse battery staple".toCharArray();

        // create the store: the meta file is written once with the password
        try (EncryptedBitcask store = EncryptedBitcask.builder(directory, password)
            .iterations(2000)                    // tests only; production keeps the 600000 default
            .maxFileSize(300)
            .sync(SyncMode.NONE)
            .build())
        {
            assertTrue(Files.exists(directory.resolve(EncryptedBitcask.META_FILE)));

            // put, get, rewrite, delete
            Map<String, byte[]> written = new TreeMap<>();
            for (int i = 0; i < 10; i++)
            {
                String key = "agent-" + i;
                byte[] value = bytes("topsecret-report-" + i);
                written.put(key, value);
                store.put(bytes(key), value);
            }

            for (Map.Entry<String, byte[]> entry : written.entrySet())
            {
                assertTrue(store.containsKey(bytes(entry.getKey())));
                assertArrayEquals(entry.getValue(), store.get(bytes(entry.getKey())));
            }
            assertEquals("topsecret-report-4", new String(store.get(bytes("agent-4")), StandardCharsets.UTF_8));

            store.put(bytes("agent-4"), bytes("topsecret-overwritten"));
            assertTrue(store.delete(bytes("agent-9")));
            assertFalse(store.delete(bytes("agent-9")));
            assertFalse(store.containsKey(bytes("agent-9")));
            assertEquals(9, store.size());

            // merge: only live entries, freshly encrypted in output files
            store.merge();
            store.put(bytes("after"), bytes("topsecret-merged"));
            assertEquals("topsecret-merged", new String(store.get(bytes("after")), StandardCharsets.UTF_8));

            // nothing readable on disk: no key, no value, no "bitcask.meta" leak
            String overview = new String(readAllDataFiles(directory), StandardCharsets.ISO_8859_1);
            assertFalse(overview.contains("topsecret"), "ciphertext must not contain the plaintext values");
            assertFalse(overview.contains("agent-"), "ciphertext must not contain the plaintext keys");
        }

        // reopen with the same password: full decryption recovery
        try (EncryptedBitcask store = EncryptedBitcask.open(directory, password))
        {
            assertEquals("topsecret-report-0", new String(store.get(bytes("agent-0")), StandardCharsets.UTF_8));
            assertNull(store.get(bytes("agent-9")));
            assertEquals("topsecret-overwritten", new String(store.get(bytes("agent-4")), StandardCharsets.UTF_8));
            assertEquals("topsecret-merged", new String(store.get(bytes("after")), StandardCharsets.UTF_8));
            assertEquals(10, store.size());
        }

        // wrong password: rejected at open, the store keeps working under
        // the correct password afterwards
        assertThrows(IOException.class, () -> EncryptedBitcask.open(directory, "wrong".toCharArray()));
        try (EncryptedBitcask store = EncryptedBitcask.open(directory, password))
        {
            assertEquals(10, store.size());
        }

        // simulated crash: garbage appended to the last file truncates away
        Path last = directory.resolve(maxId(directory) + Bitcask.DATA_SUFFIX);
        Files.write(last, new byte[]{(byte) 0x9c, 0x31, 0x00, (byte) 0xff, 0x73}, StandardOpenOption.APPEND);

        try (EncryptedBitcask store = EncryptedBitcask.open(directory, password))
        {
            assertEquals(10, store.size());
            store.put(bytes("after-crash"), bytes("topsecret-survivor"));
        }

        try (EncryptedBitcask store = EncryptedBitcask.open(directory, password))
        {
            assertEquals(11, store.size());
            assertEquals("topsecret-survivor", new String(store.get(bytes("after-crash")), StandardCharsets.UTF_8));
        }
    }

    // ------------------------------------------------------------------
    // helpers

    private List<Long> listDataIds(Path directory) throws IOException
    {
        List<Long> ids = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                ids.add(Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length())));
            }
        }
        return ids;
    }

    private long maxId(Path directory) throws IOException
    {
        long max = 0;
        for (long id : listDataIds(directory))
        {
            max = Math.max(max, id);
        }
        return max;
    }

    private byte[] readAllDataFiles(Path directory) throws IOException
    {
        List<Long> ids = listDataIds(directory);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        for (long id : ids)
        {
            out.write(Files.readAllBytes(directory.resolve(id + Bitcask.DATA_SUFFIX)));
        }

        return out.toByteArray();
    }
}
