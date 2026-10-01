/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.combinatorics;

import io.nut.base.concurrent.Generator;
import java.security.InvalidParameterException;

public class Permutator2<E> extends Generator<E[]>
{
    private final E[][] values;
    private final int k;

    public Permutator2(E[][] values, int k, int capacity)
    {
        super(capacity);
        this.values = values.clone();
        this.k = k;
        if (k > values.length || k < 0)
        {
            throw new InvalidParameterException("invalid value for k="+k);
        }
    }

    public Permutator2(E[][] values, int k)
    {
        this(values, k, 0);
    }
    
    public Permutator2(E[][] values)
    {
        this(values, values.length, 0);
    }

    @Override
    public void run()
    {
        Combinator2<E> combinator2 = new Combinator2<>(values,k);
        for(E[] c : combinator2)
        {
            Permutator<E> permutator = new Permutator<>(c,k);
            for(E[] p : permutator)
            {
                this.yield(p);
            }
        }
    }
    
}
