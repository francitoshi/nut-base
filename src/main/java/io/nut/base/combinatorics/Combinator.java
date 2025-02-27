/*
 * Combinator.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.combinatorics;

import io.nut.base.util.concurrent.Generator;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Combinator<E> extends Generator<E[]>
{
    private final E[] values;
    private final int k;
    private final E[] empty;

    public Combinator(E[] values, int k, int capacity)
    {
        super(capacity);
        this.values = values.clone();
        this.k = k;
        if (k > values.length || k < 0)
        {
            throw new InvalidParameterException("invalid value for k="+k);
        }
        this.empty = Arrays.copyOf(values, 0);
    }

    public Combinator(E[] values, int k)
    {
        this(values, k, 0);
    }
    
    public Combinator(E[] values)
    {
        this(values, values.length, 0);
    }
    @Override
    public void run()
    {
        if(k==values.length)
        {
            this.yield(values.clone());
        }
        else
        {
            combineK(values, 0, k, new ArrayList<>());
        }
    }
    
    private void combineK(E[] elements, int start, int k, List<E> current)
    {
        if (k == 0)
        {
            this.yield(current.toArray(this.empty));
            return;
        }

        for (int i = start; i < elements.length; i++)
        {
            current.add(elements[i]);
            combineK(elements, i + 1, k - 1, current);
            current.remove(current.size() - 1);
        }
    }

}
