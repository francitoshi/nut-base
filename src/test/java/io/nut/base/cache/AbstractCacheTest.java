/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class AbstractCacheTest
{
    /**
     * Test of synchronizedCache method, of class AbstractCache.
     */
    @Test
    public void testSynchronizedCache()
    {
        TinyLFUCache<String, String> instance = new TinyLFUCache<>(10);

        Cache<String,String> result1 = instance.synchronizedCache();
        assertNotNull(result1);
        Cache<String,String> result2 = result1.synchronizedCache();
        assertNotNull(result2);
    }
}
