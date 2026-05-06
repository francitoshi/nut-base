/*
 *  MainKey.java
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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GPG primary key together with all of its associated subkeys
 * and user IDs.
 *
 * <p>A GPG keyring entry consists of one primary (main) key, zero or more
 * subkeys, and one or more user IDs.  This class acts as the common base for
 * both {@link PubKey} (public-key entries) and {@link SecKey} (secret-key
 * entries).</p>
 *
 * @author franci
 * @see PubKey
 * @see SecKey
 * @see SubKey
 * @see UserId
 */
public class MainKey
{
    /** The primary subkey of this key entry. Always the first element of the key list. */
    final SubKey main;

    /** All subkeys belonging to this key entry, including the primary key at index 0. */
    private final List<SubKey> keys = new ArrayList<>();

    /** All user IDs associated with this key entry. */
    private final List<UserId> uids = new ArrayList<>();

    /**
     * Constructs a {@code MainKey} with the given primary subkey.
     *
     * @param main the primary {@link SubKey}; must not be {@code null}
     */
    public MainKey(SubKey main)
    {
        this.main = main;
        this.keys.add(main);
    }

    /**
     * Returns the primary subkey of this key entry.
     *
     * @return the primary {@link SubKey}
     */
    public SubKey getMain()
    {
        return main;
    }

    /**
     * Adds a subkey to this key entry.
     *
     * @param sub the {@link SubKey} to add; must not be {@code null}
     */
    public void add(SubKey sub)
    {
        this.keys.add(sub);
    }

    /**
     * Adds a user ID to this key entry.
     *
     * @param uid the {@link UserId} to add; must not be {@code null}
     */
    public void add(UserId uid)
    {
        this.uids.add(uid);
    }

    /**
     * Returns all subkeys belonging to this key entry, including the primary key.
     *
     * @return array of {@link SubKey} instances; never {@code null}
     */
    public SubKey[] getKeys()
    {
        return this.keys.toArray(new SubKey[0]);
    }

    /**
     * Returns all user IDs associated with this key entry.
     *
     * @return array of {@link UserId} instances; never {@code null}
     */
    public UserId[] getUids()
    {
        return this.uids.toArray(new UserId[0]);
    }

    /**
     * Finds all subkeys whose capabilities include every capability listed in
     * the {@code capabilities} filter string.
     *
     * <p>For example, passing {@code "ES"} returns only subkeys that have both
     * encrypt ({@code E}) and sign ({@code S}) capabilities.</p>
     *
     * @param capabilities a string of capability letters to match
     *                     ({@code E}=encrypt, {@code S}=sign, {@code C}=certify,
     *                     {@code A}=authenticate); case-insensitive
     * @return array of matching {@link SubKey} instances; never {@code null},
     *         may be empty
     */
    public SubKey[] findSecKeys(String capabilities)
    {
        ArrayList<SubKey> list = new ArrayList<>();
        for(SubKey item : this.keys)
        {
            if(isCompatible(item.capabilities, capabilities))
            {
                list.add(item);
            }
        }
        return list.toArray(new SubKey[0]);
    }

    /**
     * Checks whether the given {@code capabilities} string contains all of the
     * capability letters required by the {@code filter} string.
     *
     * <p>Both strings are compared case-insensitively.  A subkey is considered
     * compatible when every character present in {@code filter} also appears in
     * {@code capabilities}.</p>
     *
     * @param capabilities the capabilities advertised by the subkey
     * @param filter       the capabilities required by the caller
     * @return {@code true} if every character of {@code filter} is present in
     *         {@code capabilities}; {@code false} otherwise
     */
    static boolean isCompatible(String capabilities, String filter)
    {
        capabilities = capabilities.toUpperCase();
        filter = filter.toUpperCase();
        for(char c : filter.toCharArray())
        {
            if(!capabilities.contains(Character.toString(c)))
            {
                return false;
            }
        }
        return true;
    }
}
