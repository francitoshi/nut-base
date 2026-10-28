/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto.gpg;

/**
 * Represents a GPG public-key entry ({@code pub} record) together with all of
 * its subkeys ({@code sub} records) and user IDs ({@code uid} records).
 *
 * <p>Instances are created during keyring parsing (see
 * {@link GPG#parseKeys(java.io.InputStream, java.util.List, java.util.List)})
 * whenever a {@code pub} record is encountered in the
 * {@code gpg --with-colons} output.</p>
 *
 * @author franci
 * @see MainKey
 * @see SecKey
 */
public class PubKey extends MainKey
{
    /**
     * Constructs a {@code PubKey} with the given primary subkey.
     *
     * @param main the primary {@link SubKey} parsed from a {@code pub} record;
     *             must not be {@code null}
     */
    public PubKey(SubKey main)
    {
        super(main);
    }
    
}
