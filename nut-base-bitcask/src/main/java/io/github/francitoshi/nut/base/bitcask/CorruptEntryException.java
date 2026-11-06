/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

/**
 * Thrown when a data entry is corrupt (bad CRC, invalid field values) or
 * truncated (the entry extends beyond the end of the available data).
 * <p>
 * The distinction between a truncated tail, which may be recovered by
 * truncation, and real corruption matters for the recovery algorithm, so
 * subclasses {@link TruncatedEntryException} exists for the first case.
 */
public class CorruptEntryException extends RuntimeException
{
    /**
     * Constructs an exception with the given detail message.
     *
     * @param message the detail message
     */
    public CorruptEntryException(String message)
    {
        super(message);
    }

    /**
     * Constructs an exception with the given detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public CorruptEntryException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
