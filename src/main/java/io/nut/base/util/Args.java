/*
 *  Args.java
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
package io.nut.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author franci
 */
public final class Args
{
    private final List<String> items;

    public Args()
    {
        this(new ArrayList<>());
    }

    public Args(List<String> items)
    {
        this.items = items;
    }
    public Args(String... values)
    {
        this(new ArrayList<>());
        add(values);
    }

    public List<String> get()
    {
        return items;
    }

    public Args add(String... values)
    {
        items.addAll(Arrays.asList(values));
        return this;
    }
    
    public Args add(boolean include, String... values)
    {
        if(include)
        {
            add(values);
        }
        return this;
    }    
}
