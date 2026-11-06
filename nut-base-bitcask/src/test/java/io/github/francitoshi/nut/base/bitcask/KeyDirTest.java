/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the in-memory dense index of live keys.
 */
public class KeyDirTest
{
    @Test
    public void putThenGetResolveByContent()
    {
        KeyDir keydir = new KeyDir();
        KeyDirEntry entry = new KeyDirEntry(1, 0, 17, 100);

        keydir.put(bytes("alpha"), entry);

        // a different array instance with the same content must resolve
        assertSame(entry, keydir.get(bytes("alpha")));
        assertTrue(keydir.containsKey(bytes("alpha")));
        assertEquals(1, keydir.size());
        assertFalse(keydir.isEmpty());
    }

    @Test
    public void storedKeyIsDefensivelyCopied()
    {
        KeyDir keydir = new KeyDir();
        byte[] key = bytes("alpha");
        KeyDirEntry entry = new KeyDirEntry(1, 0, 17, 100);

        keydir.put(key, entry);
        key[0] ^= 0xFF; // the caller may go on modifying its array

        assertTrue(keydir.containsKey(bytes("alpha")));
        assertSame(entry, keydir.get(bytes("alpha")));
        assertEquals(1, keydir.size());
    }

    @Test
    public void putReplacesPreviousEntry()
    {
        KeyDir keydir = new KeyDir();
        KeyDirEntry first = new KeyDirEntry(1, 0, 17, 100);
        KeyDirEntry second = new KeyDirEntry(2, 100, 17, 200);

        assertNull(keydir.put(bytes("k"), first));
        assertSame(first, keydir.put(bytes("k"), second));
        assertSame(second, keydir.get(bytes("k")));
        assertEquals(1, keydir.size());
    }

    @Test
    public void removeDeletesKeyFromIndex()
    {
        KeyDir keydir = new KeyDir();
        KeyDirEntry entry = new KeyDirEntry(1, 0, 17, 100);

        keydir.put(bytes("k"), entry);
        assertSame(entry, keydir.remove(bytes("k")));
        assertNull(keydir.get(bytes("k")));
        assertNull(keydir.remove(bytes("k")));
        assertEquals(0, keydir.size());
        assertTrue(keydir.isEmpty());
    }

    @Test
    public void missingKeyYieldsNothing()
    {
        KeyDir keydir = new KeyDir();

        assertNull(keydir.get(bytes("missing")));
        assertFalse(keydir.containsKey(bytes("missing")));
        assertNull(keydir.remove(bytes("missing")));
        assertTrue(keydir.isEmpty());
    }

    @Test
    public void keysReturnsDefensiveClonesInNoOrder()
    {
        KeyDir keydir = new KeyDir();
        keydir.put(bytes("one"), new KeyDirEntry(1, 0, 10, 1));
        keydir.put(bytes("two"), new KeyDirEntry(1, 10, 10, 2));

        byte[][] keys = keydir.keys();
        assertEquals(2, keys.length);

        Arrays.sort(keys, (a, b) -> new String(a).compareTo(new String(b)));
        assertArrayEquals(bytes("one"), keys[0]);
        assertArrayEquals(bytes("two"), keys[1]);

        // clones: modifying a returned key must not affect the keydir
        keys[0] = new byte[0];
        assertTrue(keydir.containsKey(bytes("one")));
    }

    @Test
    public void forEachVisitsEveryLiveKey()
    {
        KeyDir keydir = new KeyDir();
        keydir.put(bytes("one"), new KeyDirEntry(1, 0, 10, 1));
        keydir.put(bytes("two"), new KeyDirEntry(1, 10, 10, 2));
        keydir.put(bytes("three"), new KeyDirEntry(1, 20, 10, 3));
        keydir.remove(bytes("two"));

        int[] count = {0};
        keydir.forEach((key, entry) ->
        {
            count[0]++;
            assertTrue(keydir.containsKey(key));
            assertTrue(new String(key).equals("one") || new String(key).equals("three"));
        });

        assertEquals(2, count[0]);
    }

    @Test
    public void entryEqualsAndOrder()
    {
        KeyDirEntry a = new KeyDirEntry(1, 100, 10, 1);
        KeyDirEntry b = new KeyDirEntry(2, 0, 10, 1);
        KeyDirEntry c = new KeyDirEntry(1, 0, 10, 1);
        KeyDirEntry sameAsA = new KeyDirEntry(1, 100, 10, 1); // fully identical

        assertTrue(a.compareTo(b) < 0);   // file 1 before file 2
        assertTrue(a.compareTo(c) > 0);   // offset 100 after offset 0
        assertTrue(a.hasSameLocation(sameAsA));
        assertTrue(a.equals(sameAsA));    // value equality
        assertFalse(a.equals(b));
        assertFalse(a.equals(null));
    }

    private static byte[] bytes(String s)
    {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
