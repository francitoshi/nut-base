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
 *
 * @author franci
 */
public class MainKey
{
    final SubKey main;
    private final List<SubKey> keys = new ArrayList<>();
    private final List<UserId> uids = new ArrayList<>();

    public MainKey(SubKey main)
    {
        this.main = main;
        this.keys.add(main);
    }

    public SubKey getMain()
    {
        return main;
    }

    public void add(SubKey sub)
    {
        this.keys.add(sub);
    }

    public void add(UserId uid)
    {
        this.uids.add(uid);
    }

    public SubKey[] getKeys()
    {
        return this.keys.toArray(new SubKey[0]);
    }

    public UserId[] getUids()
    {
        return this.uids.toArray(new UserId[0]);
    }
    
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
