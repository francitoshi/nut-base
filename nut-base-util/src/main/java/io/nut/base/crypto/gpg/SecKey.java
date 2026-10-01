/*
 *  SecKey.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.crypto.gpg;

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
