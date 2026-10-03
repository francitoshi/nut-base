/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.gpg;

import io.nut.base.util.Parsers;

/**
 * Represents a GPG user ID ({@code uid}) record as returned by
 * {@code gpg --with-colons} output.
 *
 * <p>A user ID typically contains the owner's real name, an optional comment,
 * and an e-mail address in the form {@code "Real Name (Comment) <email@example.com>"}.</p>
 *
 * <p>Fields are parsed from the colon-delimited {@code uid} line as described in
 * the GnuPG
 * <a href="https://github.com/gpg/gnupg/blob/master/doc/DETAILS">DETAILS</a>
 * file.</p>
 *
 * @author franci
 * @see MainKey
 */
public class UserId
{
    /**
     * Creation date of this user ID, expressed as a Unix epoch timestamp
     * (seconds since 1970-01-01 00:00:00 UTC), or {@code 0} if not available.
     */
    public final long createdEpochSecond;

    /**
     * SHA-1 hash of the user ID string, used internally by GnuPG to
     * uniquely reference this user ID within a key.
     */
    public final String hash;

    /**
     * Full user ID string, typically in the form
     * {@code "Real Name (Comment) <email@example.com>"}.
     */
    public final String uid;

    /**
     * Constructs a {@code UserId} by parsing a colon-delimited field array
     * from a {@code uid} record produced by {@code gpg --with-colons}.
     *
     * <p>The relevant fields (0-based) are:</p>
     * <pre>
     *  5  creation date (Unix epoch, may be empty → 0)
     *  7  hash of the user ID
     *  9  user ID string
     * </pre>
     *
     * @param s colon-split field array from a single {@code uid} line
     */
    public UserId(String[] s)
    {
        this.createdEpochSecond = Parsers.safeParseLong(s[5],0);
        this.hash = s[7];
        this.uid = s[9];
    }
    
}
