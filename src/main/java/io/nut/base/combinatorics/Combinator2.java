/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.combinatorics;

import io.nut.base.concurrent.Generator;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Combinator2<E> extends Generator<E[]>
{
    private final E[][] values;
    private final int k;
    private final E[] empty;

    public Combinator2(E[][] values, int k, int capacity)
    {
        super(capacity);
        this.values = values.clone();
        this.k = k;
        if (k > values.length || k < 0)
        {
            throw new InvalidParameterException("invalid value for k="+k);
        }
        this.empty = Arrays.copyOf(values[0], 0);
    }

    public Combinator2(E[][] values, int k)
    {
        this(values, k, 0);
    }
    
    public Combinator2(E[][] values)
    {
        this(values, values.length, 0);
    }
    @Override
    public void run()
    {
        combine(0, k, new ArrayList<>());
    }
    
    private void combine(int start, int k, List<E> current)
    {
        if (k == 0)
        {
            this.yield(current.toArray(this.empty));
            return;
        }
        for (int i = start; i < values.length; i++)
        {
            for (int j = 0; j < values[i].length; j++)
            {
                current.add(values[i][j]);
                combine(i + 1, k - 1, current);
                current.remove(current.size() - 1);
            }
            //combine(i + 1, k, current);
        }
    }
}
