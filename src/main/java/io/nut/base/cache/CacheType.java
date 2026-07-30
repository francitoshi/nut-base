/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

/**
 * Supported underlying cache implementation types.
 */
public enum CacheType
{
    HASH_MAP,
    ARC,
    LRU_LFU,
    TINY_LFU
}
