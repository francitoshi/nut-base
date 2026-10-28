/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto.gpg;

/**
 * Represents a GPG secret-key entry ({@code sec} record) together with all of
 * its secret subkeys ({@code ssb} records) and user IDs ({@code uid} records).
 *
 * <p>Instances are created during keyring parsing (see
 * {@link GPG#parseKeys(java.io.InputStream, java.util.List, java.util.List)})
 * whenever a {@code sec} record is encountered in the
 * {@code gpg --with-colons} output.</p>
 *
 * @author franci
 * @see MainKey
 * @see PubKey
 */
public class SecKey extends MainKey
{
    /**
     * Constructs a {@code SecKey} with the given primary subkey.
     *
     * @param main the primary {@link SubKey} parsed from a {@code sec} record;
     *             must not be {@code null}
     */
    public SecKey(SubKey main)
    {
        super(main);
    }
}
