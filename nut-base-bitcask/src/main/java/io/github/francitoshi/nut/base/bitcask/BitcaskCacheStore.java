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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link CacheStore} backed by a Bitcask directory, designed as a durable
 * tier for {@link io.github.francitoshi.nut.base.commons.cache.persistent.PersistentCache}
 * that is faster than the single-file {@link io.github.francitoshi.nut.base.commons.cache.persistent.FileCacheStore}.
 * <p>
 * The store owns one {@link Bitcask} instance in the given directory, opened
 * with {@link SyncMode#NONE}: appends reach the page cache without fsync and
 * {@link #flush()} syncs on demand, exactly like the file-based store. The
 * expiration encoding and the rest of the store semantics are in
 * {@link AbstractBitcaskCacheStore}.
 * <p>
 * <h2>Why it is faster</h2>
 * <ul>
 * <li>Opening replays nothing: when hint files exist, the open reads only
 * hint headers plus keys and never the values. This store asks the
 * underlying Bitcask to merge on close and whenever dead data accumulates,
 * so a reopen almost always finds fresh hints.</li>
 * <li>{@link #containsKey}, {@link #size} and {@link #purgeExpired} run off
 * the in-memory keydir with no disk access.</li>
 * <li>Cross-process protection: the Bitcask directory lock rejects a second
 * open of the same cache directory instead of corrupting it.</li>
 * </ul>
 * <p>
 * Directories created by this store carry the {@link #MARKER_FILE} marker:
 * a data-file directory without the marker was created by someone else and
 * is refused, because the plain Bitcask profile has no per-file header and
 * an aggressive open could truncate foreign data.
 * <p>
 * Unlike the rest of the package this store <strong>is</strong> thread-safe:
 * the underlying Bitcask serializes writers and serves concurrent readers
 * with its own lock. The {@code CacheStore} thread contract is satisfied as
 * usual for callers that keep to the single-threaded discipline.
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public class BitcaskCacheStore<K,V> extends AbstractBitcaskCacheStore<K,V>
{
    /** Marker file that only this store creates; see the constructor. */
    public static final String MARKER_FILE = "bitcask.cache.store";

    private final Path directory;
    private final Bitcask store;

    /**
     * Creates a store in the given directory with the default Bitcask
     * settings (1 GiB rotation threshold, no fsync per write).
     *
     * @param directory the cache directory, created if missing
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     */
    public BitcaskCacheStore(Path directory, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        this(directory, keySerializer, valueSerializer, Bitcask.DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * Creates a store in the given directory.
     *
     * @param directory the cache directory, created if missing
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     * @param maxFileSize the Bitcask rotation threshold in bytes
     */
    public BitcaskCacheStore(Path directory, Serializer<K> keySerializer, Serializer<V> valueSerializer, long maxFileSize)
    {
        super(keySerializer, valueSerializer);
        this.directory = directory;

        try
        {
            this.store = buildStore(directory, maxFileSize);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(
                "cannot open the bitcask cache store (foreign directory or locked?): " + directory, e);
        }

        rebuildAccounting();
    }

    /**
     * Opens the underlying Bitcask, guarding the directory first: a
     * directory that holds data files but no marker was created by someone
     * else and is refused.
     *
     * @return the open Bitcask
     */
    private static Bitcask buildStore(Path directory, long maxFileSize) throws IOException
    {
        Files.createDirectories(directory);

        Path marker = directory.resolve(MARKER_FILE);

        if (Files.notExists(marker))
        {
            if (hasDataFiles(directory))
            {
                throw new IOException("directory holds data files without the " + MARKER_FILE
                    + " marker: not a BitcaskCacheStore directory: " + directory);
            }

            Files.write(marker, new byte[]{1});
        }

        return Bitcask.builder(directory)
            .maxFileSize(maxFileSize)
            .sync(SyncMode.NONE)
            .build();
    }

    private static boolean hasDataFiles(Path directory) throws IOException
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            return stream.iterator().hasNext();
        }
    }

    /**
     * Returns the directory of the underlying Bitcask store.
     *
     * @return the cache directory
     */
    public Path getDirectory()
    {
        return directory;
    }

    @Override
    protected KeyDirEntry storeLocate(byte[] key)
    {
        return store.locate(key);
    }

    @Override
    protected void storePut(byte[] key, byte[] value, int tstamp) throws IOException
    {
        store.put(key, value, tstamp);
    }

    @Override
    protected byte[] storeGet(byte[] key) throws IOException
    {
        return store.get(key);
    }

    @Override
    protected boolean storeRemove(byte[] key) throws IOException
    {
        return store.delete(key);
    }

    @Override
    protected void storeForEachEntry(Bitcask.EntryVisitor visitor)
    {
        store.forEachEntry(visitor);
    }

    /**
     * The tombstone of a plain-profile data file: the 14-byte entry header
     * plus the key, with no value.
     */
    @Override
    protected long tombstoneRecordSize(byte[] key)
    {
        return BitcaskFormat.HEADER_SIZE + key.length;
    }

    @Override
    protected long storeTotalBytes() throws IOException
    {
        return store.totalDataBytes();
    }

    @Override
    protected void storeFlush() throws IOException
    {
        store.flush();
    }

    @Override
    protected void storeMerge() throws IOException
    {
        store.merge();
    }

    @Override
    protected void storeClear() throws IOException
    {
        store.clear();
    }

    @Override
    protected void storeClose() throws IOException
    {
        store.close();
    }
}
