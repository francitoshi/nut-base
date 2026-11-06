/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

/**
 * Configurable durability policy for writes, following section 9 of
 * bitcask-spec.md.
 * <ul>
 * <li>{@link #NONE}: no call to {@code force()}; durability is up to the
 * operating system.</li>
 * <li>{@link #EVERY_WRITE}: {@code channel.force(false)} after every put or
 * delete.</li>
 * <li>{@link #INTERVAL}: {@code force(false)} at most once per configurable
 * interval of milliseconds, checked lazily on the next write.</li>
 * </ul>
 * {@code force(true)} is always called on rotation and where the merge
 * requires it, regardless of the policy.
 */
public enum SyncMode
{
    /** Never force: data may sit in the OS page cache indefinitely. */
    NONE,

    /** Force every write: slow but the most durable between uses. */
    EVERY_WRITE,

    /** Force no more often than every configured interval of milliseconds. */
    INTERVAL
}
