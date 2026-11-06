/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

/**
 * Immutable snapshot of the position and header data of a key's most recent
 * data-file entry, as recorded in the keydir (section 5 of bitcask-spec.md).
 * <p>
 * To read a value, the entry is read in full from the data file
 * (entrySize bytes at entryOffset), the CRC is verified and the value is
 * extracted from bytes [14 + ksz, entrySize).
 */
public class KeyDirEntry implements Comparable<KeyDirEntry>
{
    /** Id of the data file holding the entry. */
    public final long fileId;

    /** Position of the first byte of the entry (the CRC) within the file. */
    public final long entryOffset;

    /** Total size in bytes of the entry: header + key + value. */
    public final int entrySize;

    /** Timestamp in seconds since epoch of the entry; informational. */
    public final int tstamp;

    /**
     * Constructs an entry snapshot.
     *
     * @param fileId id of the data file holding the entry
     * @param entryOffset position of the first byte of the entry within the file
     * @param entrySize total size in bytes of the entry
     * @param tstamp timestamp in seconds since epoch
     */
    public KeyDirEntry(long fileId, long entryOffset, int entrySize, int tstamp)
    {
        this.fileId = fileId;
        this.entryOffset = entryOffset;
        this.entrySize = entrySize;
        this.tstamp = tstamp;
    }

    /**
     * Returns true if this and the given entry describe the same position in
     * the same data file.
     *
     * @param other the other entry
     * @return true if both refer to the same entry
     */
    public boolean hasSameLocation(KeyDirEntry other)
    {
        return fileId == other.fileId && entryOffset == other.entryOffset;
    }

    @Override
    public int compareTo(KeyDirEntry other)
    {
        return fileId != other.fileId
            ? Long.compare(fileId, other.fileId)
            : Long.compare(entryOffset, other.entryOffset);
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof KeyDirEntry
            && fileId == ((KeyDirEntry) obj).fileId
            && entryOffset == ((KeyDirEntry) obj).entryOffset
            && entrySize == ((KeyDirEntry) obj).entrySize
            && tstamp == ((KeyDirEntry) obj).tstamp;
    }

    @Override
    public int hashCode()
    {
        int result = (int) (fileId ^ (fileId >>> 32));
        result = 31 * result + (int) (entryOffset ^ (entryOffset >>> 32));
        result = 31 * result + entrySize;
        return 31 * result + tstamp;
    }

    @Override
    public String toString()
    {
        return "KeyDirEntry{fileId=" + fileId + ", entryOffset=" + entryOffset
            + ", entrySize=" + entrySize + ", tstamp=" + tstamp + '}';
    }
}
