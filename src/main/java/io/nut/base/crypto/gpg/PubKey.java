/*
 *  PubKey.java
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
