/*
 *  Args.java
 *
 *  Copyright (c) 2025-2026 francitoshi@gmail.com
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
 * A mutable, ordered collection of string arguments designed for building
 * command-line argument lists programmatically.
 *
 * <p>This class wraps a {@link List} of strings and provides a fluent API
 * for conditionally or unconditionally appending arguments. It is useful for
 * constructing argument arrays to pass to external processes or parsers.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Args args = new Args("--output", "file.txt")
 *     .add(verbose, "--verbose")
 *     .add("--format", "json");
 *
 * String[] result = args.get().toArray(new String[0]);
 * }</pre>
 *
 * @author franci
 */
public final class Args
{
    private final List<String> items;

    /**
     * Constructs an empty {@code Args} instance backed by a new
     * {@link ArrayList}.
     */
    public Args()
    {
        this(new ArrayList<>());
    }

    /**
     * Constructs an {@code Args} instance backed by the given list.
     *
     * <p>The provided list is used directly (not copied), so any external
     * modifications to it will be reflected in this instance.</p>
     *
     * @param items the backing list of argument strings; must not be
     *              {@code null}
     */
    public Args(List<String> items)
    {
        this.items = items;
    }

    /**
     * Constructs an {@code Args} instance pre-populated with the given values.
     *
     * @param values the initial argument strings to add; may be empty but must
     *               not be {@code null}
     */
    public Args(String... values)
    {
        this(new ArrayList<>());
        add(values);
    }

    /**
     * Returns the underlying list of argument strings.
     *
     * <p>The returned list is the live backing list; modifications to it will
     * affect this {@code Args} instance.</p>
     *
     * @return the list of argument strings; never {@code null}
     */
    public List<String> get()
    {
        return items;
    }

    /**
     * Returns the argument string at the specified index, or {@code null} if
     * the index is out of bounds.
     *
     * @param index the zero-based index of the argument to retrieve
     * @return the argument string at {@code index}, or {@code null} if
     *         {@code index >= size()}
     */
    public String get(int index)
    {
        return items.size()>index ? items.get(index) : null;
    }

    /**
     * Appends one or more argument strings to this instance.
     *
     * @param values the argument strings to append; must not be {@code null}
     * @return this {@code Args} instance, to allow method chaining
     */
    public Args add(String... values)
    {
        items.addAll(Arrays.asList(values));
        return this;
    }

    /**
     * Conditionally appends one or more argument strings to this instance.
     *
     * <p>The values are only added when {@code include} is {@code true};
     * otherwise this method is a no-op.</p>
     *
     * @param include {@code true} to append {@code values}, {@code false} to
     *                skip them
     * @param values  the argument strings to append when {@code include} is
     *                {@code true}; must not be {@code null}
     * @return this {@code Args} instance, to allow method chaining
     */
    public Args add(boolean include, String... values)
    {
        if(include)
        {
            add(values);
        }
        return this;
    }    
}
