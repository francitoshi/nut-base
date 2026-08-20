/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import io.nut.base.util.Empty;
import java.util.Collections;
import java.util.List;

/**
 * A tuple of 0 elements.
 *
 * @author franci
 * @since 1.8
 */
public final class Tuple0 implements Tuple, Comparable<Tuple0>
{
    private static final long serialVersionUID = 1L;
    private static final Tuple0 INSTANCE = new Tuple0();

    private Tuple0()
    {
    }

    /**
     * Returns the singleton instance of Tuple0.
     *
     * @return the singleton Tuple0
     */
    public static Tuple0 instance()
    {
        return INSTANCE;
    }

    @Override
    public int arity()
    {
        return 0;
    }

    @Override
    public Object[] toArray()
    {
        return Empty.OBJECTS;
    }

    @Override
    public List<Object> toList()
    {
        return Collections.emptyList();
    }

    @Override
    public int compareTo(Tuple0 o)
    {
        return 0;
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj == this || obj instanceof Tuple0;
    }

    @Override
    public String toString()
    {
        return "()";
    }
}
