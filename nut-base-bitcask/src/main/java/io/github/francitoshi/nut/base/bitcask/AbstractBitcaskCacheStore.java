/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import io.github.francitoshi.nut.base.commons.cache.persistent.CacheStore;
import io.github.francitoshi.nut.base.commons.serializer.Serializer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

/**
 * The store semantics shared by the Bitcask-backed {@link CacheStore}
 * implementations: expiration encoded in the entry timestamp, null keys and
 * values with a one-byte sentinel, compaction policy and the accounting the
 * policies need. The concrete subclasses only map the hooks onto the plain
 * {@link Bitcask} or the encrypted one.
 * <p>
 * The expiration encoding is the same for both profiles: seconds since
 * epoch, rounded up, with {@code 0} meaning "never expires" and expirations
 * beyond the u32 range rejected. Granularity is therefore whole seconds
 * (an entry serves up to one second after its expiry, never before) and the
 * expiry cannot be later than early 2106. A directory written by a subclass
 * must not be interpreted by a plain {@link Bitcask} reader because the
 * timestamp no longer means "written at".
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public abstract class AbstractBitcaskCacheStore<K,V> implements CacheStore<K,V>
{
    private static final int MIN_COMPACT_BYTES = 16 * 1024;

    /** Expiry seconds must clear the 0 sentinel and fit the u32 tstamp. */
    static final long MAX_TSTAMP = 0xFFFFFFFEL;

    /** The one-byte sentinel in front of serialized nulls. */
    static final byte NULL_SENTINEL = 0x01;
    static final byte PLAIN_SENTINEL = 0x00;

    protected final Serializer<K> keySerializer;
    protected final Serializer<V> valueSerializer;

    private long liveBytes;
    private long totalBytes;

    protected AbstractBitcaskCacheStore(Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    // ------------------------------------------------------------------ hooks

    /**
     * Returns the keydir entry for the encoded key, or null: a pure memory
     * lookup with no disk access.
     */
    protected abstract KeyDirEntry storeLocate(byte[] key);

    /**
     * Appends the entry with the given encoded timestamp.
     */
    protected abstract void storePut(byte[] key, byte[] value, int tstamp) throws IOException;

    /**
     * Returns the stored value bytes of the encoded key, or null if the key
     * vanished before the read.
     */
    protected abstract byte[] storeGet(byte[] key) throws IOException;

    /**
     * Writes the tombstone of the encoded key; true if there was an entry.
     */
    protected abstract boolean storeRemove(byte[] key) throws IOException;

    /**
     * Calls the visitor for every live key and keydir entry, in no
     * particular order.
     */
    protected abstract void storeForEachEntry(Bitcask.EntryVisitor visitor);

    /**
     * The tombstone record size of one encoded key as written by
     * {@link #storeRemove(byte[])}, for the dead-byte accounting.
     */
    protected abstract long tombstoneRecordSize(byte[] key);

    /**
     * The bytes the data files occupy on disk, live and dead.
     */
    protected abstract long storeTotalBytes() throws IOException;

    /**
     * Forces the active data file to the device.
     */
    protected abstract void storeFlush() throws IOException;

    /**
     * Rewrites the directory without the dead data.
     */
    protected abstract void storeMerge() throws IOException;

    /**
     * Removes every key and data file, leaving one empty new file.
     */
    protected abstract void storeClear() throws IOException;

    /**
     * Releases the underlying store resources.
     */
    protected abstract void storeClose() throws IOException;

    // ------------------------------------------------------------------ API

    @Override
    public long size()
    {
        long[] count = {0};

        storeForEachEntry((key, entry) ->
        {
            if (!isExpired(entry.tstamp))
            {
                count[0]++;
            }
        });

        return count[0];
    }

    @Override
    public void put(K key, V value, long expiresAtEpochMillis)
    {
        byte[] keyBytes = encodedKey(key);
        byte[] valueBytes = encodedValue(value);
        int tstamp = toTstamp(expiresAtEpochMillis);

        KeyDirEntry old = storeLocate(keyBytes);
        storePutChecked(keyBytes, valueBytes, tstamp);
        KeyDirEntry fresh = storeLocate(keyBytes);

        totalBytes += fresh.entrySize;

        liveBytes += fresh.entrySize;
        if (old != null)
        {
            liveBytes -= old.entrySize;
        }

        maybeCompact();
    }

    @Override
    public V get(K key)
    {
        byte[] keyBytes = encodedKey(key);

        KeyDirEntry entry = storeLocate(keyBytes);

        if (entry == null)
        {
            return null;
        }

        if (isExpired(entry.tstamp))
        {
            removeKey(keyBytes, entry); // lazy discard, like the file store
            return null;
        }

        byte[] stored = storeGetChecked(keyBytes);

        if (stored == null)
        {
            // a concurrent remove dropped the key between the locate and the read
            return null;
        }

        return decodedValue(stored);
    }

    @Override
    public void remove(K key)
    {
        byte[] keyBytes = encodedKey(key);

        KeyDirEntry entry = storeLocate(keyBytes);

        if (entry == null)
        {
            return;
        }

        removeKey(keyBytes, entry);
    }

    @Override
    public boolean containsKey(K key)
    {
        KeyDirEntry entry = storeLocate(encodedKey(key));
        return entry != null && !isExpired(entry.tstamp);
    }

    /**
     * Clears the cache without a merge: every key goes and every data file
     * is replaced by one new empty file.
     */
    @Override
    public void clear()
    {
        storeClearChecked();
        storeFlushChecked();

        liveBytes = 0;
        totalBytes = storeTotalChecked();
    }

    /**
     * Tombstones every expired entry and compacts if dead bytes accumulate
     * over the policy threshold.
     */
    @Override
    public void purgeExpired()
    {
        List<byte[]> expired = new java.util.ArrayList<>();

        storeForEachEntry((key, entry) ->
        {
            if (isExpired(entry.tstamp))
            {
                expired.add(key.clone()); // the loop hands out an internal array
            }
        });

        for (byte[] keyBytes : expired)
        {
            KeyDirEntry entry = storeLocate(keyBytes);

            if (entry != null && isExpired(entry.tstamp))
            {
                removeKey(keyBytes, entry);
            }
        }

        maybeCompact();
    }

    /**
     * Forces the active data file of the underlying store to the device.
     */
    @Override
    public void flush()
    {
        storeFlushChecked();
    }

    /**
     * Flushes, and when dead bytes remain merges the store, before closing
     * it. Idempotent.
     */
    @Override
    public void close()
    {
        storeFlushChecked();

        if (totalBytes > liveBytes)
        {
            mergeUnchecked();
        }

        storeCloseChecked();
    }

    /**
     * Returns the total number of bytes the data files occupy on disk, live
     * and dead. Advisory: live bytes plus the accounting of the merge and
     * rotation policies.
     *
     * @return the total bytes on disk in data files
     */
    public long getTotalBytes()
    {
        return storeTotalChecked();
    }

    /**
     * Returns the number of bytes that the live entries occupy: the total
     * minus the dead bytes that a merge would reclaim.
     *
     * @return the live bytes
     */
    public long getLiveBytes()
    {
        return liveBytes;
    }

    // ------------------------------------------------------------ internals

    /**
     * Recomputes the accounting from the disk state: live bytes come from
     * the keydir, total bytes from the data file sizes. Used after open and
     * after a merge rewrote the directory.
     */
    protected final void rebuildAccounting()
    {
        long[] live = {0};

        storeForEachEntry((key, entry) -> live[0] += entry.entrySize);

        liveBytes = live[0];
        totalBytes = storeTotalChecked();
    }

    /**
     * Writes the tombstone and adjusts the accounting.
     */
    private void removeKey(byte[] keyBytes, KeyDirEntry entry)
    {
        boolean removed;

        try
        {
            removed = storeRemove(keyBytes);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot delete from the bitcask cache store", e);
        }

        if (removed)
        {
            liveBytes -= entry.entrySize;
            totalBytes += tombstoneRecordSize(keyBytes);
        }

        maybeCompact();
    }

    /**
     * The compaction policy of the file-based store: dead bytes above half
     * of the total and a total above the minimum. The merge rewrites the
     * directory, so the accounting is recomputed from disk afterwards.
     */
    private void maybeCompact()
    {
        if (totalBytes > MIN_COMPACT_BYTES && (totalBytes - liveBytes) * 2 > totalBytes)
        {
            mergeUnchecked();
            rebuildAccounting();
        }
    }

    private void storePutChecked(byte[] key, byte[] value, int tstamp)
    {
        try
        {
            storePut(key, value, tstamp);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot write to the bitcask cache store", e);
        }
    }

    private byte[] storeGetChecked(byte[] key)
    {
        try
        {
            return storeGet(key);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read from the bitcask cache store", e);
        }
    }

    private void storeFlushChecked()
    {
        try
        {
            storeFlush();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot flush the bitcask cache store", e);
        }
    }

    private long storeTotalChecked()
    {
        try
        {
            return storeTotalBytes();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot size the bitcask cache store", e);
        }
    }

    private void storeClearChecked()
    {
        try
        {
            storeClear();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot clear the bitcask cache store", e);
        }
    }

    private void storeCloseChecked()
    {
        try
        {
            storeClose();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot close the bitcask cache store", e);
        }
    }

    private void mergeUnchecked()
    {
        try
        {
            storeMerge();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot merge the bitcask cache store", e);
        }
    }

    /**
     * Encodes the expiration inside the entry timestamp: "never expires" is
     * 0; anything else is seconds since epoch, rounded up so an entry can
     * never expire before its expiry instant.
     */
    static int toTstamp(long expiresAtEpochMillis)
    {
        if (expiresAtEpochMillis == Long.MAX_VALUE)
        {
            return 0;
        }

        long seconds = (expiresAtEpochMillis + 999) / 1000;

        if (seconds < 1 || seconds > MAX_TSTAMP)
        {
            throw new IllegalArgumentException("expiresAtEpochMillis out of the u32 seconds range: " + expiresAtEpochMillis);
        }

        return (int) seconds;
    }

    /**
     * True if the expiry encoded in the tstamp has passed. The rounding of
     * {@link #toTstamp} makes this at most one tick late, never early: the
     * entry lives through the whole second that contains the expiry.
     */
    static boolean isExpired(int tstamp)
    {
        return tstamp != 0 && tstamp * 1000L <= System.currentTimeMillis();
    }

    /**
     * Keys travel with a one-byte sentinel: {@code 0x00} for bytes from the
     * serializer, {@code 0x01} for a null key, which the store contract
     * allows and the Bitcask format cannot represent on its own.
     */
    protected byte[] encodedKey(K key)
    {
        if (key == null)
        {
            return new byte[]{NULL_SENTINEL};
        }

        return withSentinel(keySerializer.serialize(key));
    }

    /**
     * Values, like keys, carry the null sentinel in the first byte.
     */
    protected byte[] encodedValue(V value)
    {
        if (value == null)
        {
            return new byte[]{NULL_SENTINEL};
        }

        return withSentinel(valueSerializer.serialize(value));
    }

    private static byte[] withSentinel(byte[] payload)
    {
        byte[] encoded = new byte[payload.length + 1];
        encoded[0] = PLAIN_SENTINEL;
        System.arraycopy(payload, 0, encoded, 1, payload.length);
        return encoded;
    }

    /**
     * Decodes a stored value, honoring the null sentinel.
     */
    protected V decodedValue(byte[] stored)
    {
        if (stored[0] == NULL_SENTINEL)
        {
            return null;
        }

        return valueSerializer.deserialize(Arrays.copyOfRange(stored, 1, stored.length));
    }
}
