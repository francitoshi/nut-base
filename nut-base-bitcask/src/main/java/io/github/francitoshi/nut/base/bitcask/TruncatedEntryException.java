/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

/**
 * Thrown when a data entry appears truncated: the declared size of the entry
 * extends beyond the end of the available data. When this happens in the file
 * with the highest id it is treated as an interrupted write and the file is
 * truncated; in any other file it is treated as corruption.
 */
public class TruncatedEntryException extends CorruptEntryException
{
    /**
     * Constructs an exception with the given detail message.
     *
     * @param message the detail message
     */
    public TruncatedEntryException(String message)
    {
        super(message);
    }
}
