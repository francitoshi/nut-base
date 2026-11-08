/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dense index in memory that associates each live key with the position of
 * its most recent entry (section 5 of bitcask-spec.md).
 * <p>
 * Keys are compared by content, never by array identity: the {@code byte[]}
 * of the key is never used directly as a map key. All concurrent access is
 * safe for readers; writers must be serialized externally (a single writer
 * thread per store, as enforced by the store's write lock).
 */
public class KeyDir
{
    private final ConcurrentMap<KeyBytes, KeyDirEntry> map;

    /**
     * Constructs an empty keydir backed by a
     * {@link ConcurrentHashMap}.
     */
    public KeyDir()
    {
        this.map = new ConcurrentHashMap<>();
    }

    /**
     * Returns the position recorded for the given key, or null if the key is
     * not live.
     *
     * @param key the key bytes; the array must not be modified afterwards
     * @return the entry, or null if the key is not in the keydir
     */
    public KeyDirEntry get(byte[] key)
    {
        return map.get(new KeyBytes(key));
    }

    /**
     * Records the position of the most recent entry for the given key,
     * replacing any previous position.
     *
     * @param key the key bytes; a defensive copy is stored
     * @param entry the position of the entry
     * @return the previous entry for the key, or null
     */
    public KeyDirEntry put(byte[] key, KeyDirEntry entry)
    {
        return map.put(new KeyBytes(key), entry);
    }

    /**
     * Removes a key from the keydir.
     *
     * @param key the key bytes
     * @return the previous entry for the key, or null if it was not present
     */
    public KeyDirEntry remove(byte[] key)
    {
        return map.remove(new KeyBytes(key));
    }

    /**
     * Returns true if the key is live.
     *
     * @param key the key bytes
     * @return true if the key is in the keydir
     */
    public boolean containsKey(byte[] key)
    {
        return map.containsKey(new KeyBytes(key));
    }

    /**
     * Returns the number of live keys.
     *
     * @return the number of entries in the keydir
     */
    public int size()
    {
        return map.size();
    }

    /**
     * Returns true if the keydir has no live keys.
     *
     * @return true if empty
     */
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    /**
     * Removes all keys.
     */
    public void clear()
    {
        map.clear();
    }

    /**
     * Returns a defensive-copy snapshot of all live keys, in no particular
     * order. Each returned array is a clone, safe to modify or retain.
     *
     * @return the live keys, unsorted
     */
    public byte[][] keys()
    {
        byte[][] keys = new byte[map.size()][];
        int i = 0;
        for (KeyBytes keyBytes : map.keySet())
        {
            keys[i++] = keyBytes.array.clone();
        }
        return keys;
    }

    /**
     * Performs the given action for every live key and its recorded entry,
     * in no particular order.
     *
     * @param action the action to perform
     */
    public void forEach(BiConsumer action)
    {
        for (java.util.Map.Entry<KeyBytes, KeyDirEntry> entry : map.entrySet())
        {
            action.accept(entry.getKey().array, entry.getValue());
        }
    }

    /**
     * A procedure over one live key and its recorded entry.
     */
    public interface BiConsumer
    {
        /**
         * Accepts one live key and its recorded entry.
         *
         * @param key the key bytes; the array must not be modified
         * @param entry the recorded position of the key's most recent entry
         */
        void accept(byte[] key, KeyDirEntry entry);
    }
}
