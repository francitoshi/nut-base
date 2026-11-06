/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import io.github.francitoshi.nut.base.commons.cache.persistent.CacheStore;
import io.github.francitoshi.nut.base.commons.cache.persistent.FileCacheStore;
import io.github.francitoshi.nut.base.commons.profile.Profiler;
import io.github.francitoshi.nut.base.commons.serializer.BasicSerializers;
import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Same-operation benchmark of the two durable cache tiers: the single-file
 * journal store and the Bitcask directory store. The interesting number is
 * the reopen: the file store replays the whole journal (reads every byte),
 * while the Bitcask store reads hint headers plus keys when hints exist.
 * <p>
 * Values and keys are the same for both stores, the put/get phases run on
 * identical workloads, and each phase is timed with the same Profiler.
 */
public class BitcaskCacheStoreBenchmarkTest
{
    @TempDir
    Path temp;

    @Test
    public void benchmark()
    {
        int entries = 20000;
        int valueBytes = 200;

        String[] keys = new String[entries];
        for (int i = 0; i < entries; i++)
        {
            keys[i] = "element-" + i;
        }
        String value = bigString(valueBytes);

        // warm-up round (not measured)
        runPutGetPhase(new BitcaskCacheStore<>(temp.resolve("warm-bitcask"),
            BasicSerializers.STRING, BasicSerializers.STRING, 1 << 20), keys, value, 50);

        Profiler profiler = new Profiler(JavaTime.Resolution.MS, true);

        Profiler.Task fileReopen = profiler.getTask("file-reopen");
        Profiler.Task bitcaskReopen = profiler.getTask("bitcask-reopen");
        Profiler.Task bitcaskReopenHints = profiler.getTask("bitcask-reopen+hints");

        // 1. puts + get-all, same workload for both stores (per-phase tables)
        runPutGetPhase(new FileCacheStore<>(temp.resolve("put.bin"),
            BasicSerializers.STRING, BasicSerializers.STRING), keys, value, entries);
        runPutGetPhase(new BitcaskCacheStore<>(temp.resolve("put-bitcask"),
            BasicSerializers.STRING, BasicSerializers.STRING, 1 << 20), keys, value, entries);

        // 2. reopen measurement: full journal replay vs full data scan.
        //    The task measures the whole open-plus-first-reads cycle
        Path fileReopenPath = temp.resolve("reopen.bin");
        Path bitcaskReopenPath = temp.resolve("reopen-bitcask");
        unmeasuredFill(fileReopenPath, keys, value, entries);
        unmeasuredFill(bitcaskReopenPath, keys, value, entries);

        fileReopen.start();
        FileCacheStore<String, String> fileReopened = new FileCacheStore<>(fileReopenPath,
            BasicSerializers.STRING, BasicSerializers.STRING);
        assertReopened(fileReopened, keys, value, entries);
        fileReopen.stop().count(1);
        fileReopened.close();

        bitcaskReopen.start();
        BitcaskCacheStore<String, String> bitcaskReopened = new BitcaskCacheStore<>(bitcaskReopenPath,
            BasicSerializers.STRING, BasicSerializers.STRING, 1 << 20);
        assertReopened(bitcaskReopened, keys, value, entries);
        bitcaskReopen.stop().count(1);
        bitcaskReopened.close();

        // 3. reopen with hints: overwrite a little so the close merge fires;
        //    the reopen then reads hint headers and keys only
        Path hintDir = temp.resolve("hint-bitcask");
        unmeasuredFill(hintDir, keys, value, entries);
        BitcaskCacheStore<String, String> hintStore = new BitcaskCacheStore<>(hintDir,
            BasicSerializers.STRING, BasicSerializers.STRING, 1 << 20);

        for (int i = 0; i < 10; i++)
        {
            hintStore.put(keys[i], value, Long.MAX_VALUE); // dead bytes force the close merge
        }
        hintStore.close();

        bitcaskReopenHints.start();
        BitcaskCacheStore<String, String> hintReopened = new BitcaskCacheStore<>(hintDir,
            BasicSerializers.STRING, BasicSerializers.STRING, 1 << 20);
        assertReopened(hintReopened, keys, value, entries);
        bitcaskReopenHints.stop().count(1);
        hintReopened.close();

        profiler.print();
    }

    private void assertReopened(CacheStore<String, String> store,
        String[] keys, String value, int entries)
    {
        long size = store.size();
        String sample = store.get(keys[entries - 1]);

        if (!value.equals(sample) || size != entries)
        {
            throw new IllegalStateException("corrupt reopen: " + size);
        }
    }

    private void runPutGetPhase(CacheStore<String, String> store,
        String[] keys, String value, int count)
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS, true);
        Profiler.Task put = profiler.getTask("put");
        Profiler.Task getAll = profiler.getTask("get-all");

        // warm the writers so the first measured append is representative
        for (int i = 0; i < 20; i++)
        {
            store.put("warm-" + i, value, Long.MAX_VALUE);
        }

        put.start();
        for (int i = 0; i < count; i++)
        {
            store.put(keys[i], value, Long.MAX_VALUE);
        }
        put.stop().count(count);

        getAll.start();
        for (int i = 0; i < count; i++)
        {
            store.get(keys[i]);
        }
        getAll.stop().count(count);

        store.close();
        profiler.print();
    }

    private void unmeasuredFill(Path path, String[] keys, String value, int entries)
    {
        io.github.francitoshi.nut.base.commons.cache.persistent.CacheStore<String, String> store =
            keys[0] != null && path.getFileName().toString().endsWith(".bin")
                ? new FileCacheStore<>(path, BasicSerializers.STRING, BasicSerializers.STRING)
                : new BitcaskCacheStore<>(path, BasicSerializers.STRING, BasicSerializers.STRING, 1 << 20);

        for (int i = 0; i < entries; i++)
        {
            store.put(keys[i], value, Long.MAX_VALUE);
        }
        store.close();
    }

    private String bigString(int bytes)
    {
        byte[] big = new byte[bytes];
        java.util.Arrays.fill(big, (byte) 'x');
        return new String(big, java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}
