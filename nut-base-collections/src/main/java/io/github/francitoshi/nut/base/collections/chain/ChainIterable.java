/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A stacked view over an ordered sequence of {@link Iterable}s, the
 * {@link Iterable} counterpart of {@link ChainMap}.
 * <p>
 * The iterables are traversed in order: the elements of the second one follow
 * the elements of the first one and so on, so the chain behaves as a single
 * continuous sequence without collecting anything in memory. Sources that are
 * empty are skipped. The chain keeps the given iterable instances by
 * reference, therefore changes made to those sources are seen by the next
 * traversal of the chain (and vice versa): every call to {@link #iterator()}
 * asks each source for a fresh iterator, so the chain is exactly as
 * re-iterable as its sources are.
 * <p>
 * The iterators are live cursors over the sources and skip the exhausted ones
 * on the fly. {@link Iterator#remove()} is delegated to the source iterator
 * that supplied the last element, so it removes the element from the source
 * that owns it and is supported exactly when that source iterator supports
 * it.
 * <p>
 * Unlike the other classes of this package, this class does not override
 * {@link Object#toString()}: rendering the elements would consume sources
 * that may be lazy or single-pass.
 * <p>
 * Like the rest of the package, this class is not synchronized and its
 * iterators must not be shared across threads without external
 * synchronization.
 *
 * @param <E> the type of elements in this chain
 */
public class ChainIterable<E> implements Iterable<E>
{
    private final List<Iterable<? extends E>> iterables;

    /**
     * Constructs a new ChainIterable over the given iterables, from the first
     * to the last.
     *
     * @param iterables at least one iterable; must not be {@code null} nor
     *                  contain {@code null} elements
     * @throws NullPointerException     if {@code iterables} or one of its
     *                                  elements is {@code null}
     * @throws IllegalArgumentException if no iterable is given
     */
    @SafeVarargs
    public ChainIterable(Iterable<? extends E>... iterables)
    {
        this(toCollection(iterables));
    }
    /**
     * Constructs a new ChainIterable over the given iterables, from the first
     * to the last.
     *
     * @param iterables at least one iterable; must not be {@code null} nor
     *                  contain {@code null} elements
     * @throws NullPointerException     if {@code iterables} or one of its
     *                                  elements is {@code null}
     * @throws IllegalArgumentException if no iterable is given
     */
    public ChainIterable(Collection<? extends Iterable<? extends E>> iterables)
    {
        Objects.requireNonNull(iterables, "iterables must not be null");
        if(iterables.isEmpty())
        {
            throw new IllegalArgumentException("at least one iterable is required");
        }
        ArrayList<Iterable<? extends E>> copy = new ArrayList<>(iterables.size());
        for(Iterable<? extends E> iterable : iterables)
        {
            copy.add(Objects.requireNonNull(iterable, "iterable must not be null"));
        }
        this.iterables = Collections.unmodifiableList(copy);
    }

    private static <E> Collection<? extends Iterable<? extends E>> toCollection(Iterable<? extends E>[] iterables)
    {
        Objects.requireNonNull(iterables, "iterables must not be null");
        return Arrays.asList(iterables);
    }

    /**
     * Returns the iterables of this chain as an unmodifiable list, from the
     * first to the last.
     *
     * @return the iterables of this chain; never {@code null}
     */
    public List<Iterable<? extends E>> iterables()
    {
        return this.iterables;
    }

    /**
     * Returns the first iterable of the chain, the first one traversed.
     *
     * @return the first iterable; never {@code null}
     */
    public Iterable<? extends E> first()
    {
        return this.iterables.get(0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned iterator walks the sources in order, skipping the empty
     * ones, and yields a fresh traversal of the chain: it is as re-iterable
     * as the sources themselves are.
     */
    @Override
    public Iterator<E> iterator()
    {
        return new ChainIterator();
    }

    /**
     * A live cursor that walks the sources of the chain in order and
     * delegates removal to the source that supplied the last element.
     */
    private final class ChainIterator implements Iterator<E>
    {
        private int nextSource = 0;
        private Iterator<? extends E> current;
        private Iterator<? extends E> last;

        /**
         * {@inheritDoc}
         * <p>
         * Sources that are exhausted (or empty) are skipped on the fly.
         */
        @Override
        public boolean hasNext()
        {
            while(this.current==null || !this.current.hasNext())
            {
                if(this.nextSource>=ChainIterable.this.iterables.size())
                {
                    return false;
                }
                this.current = ChainIterable.this.iterables.get(this.nextSource++).iterator();
            }
            return true;
        }
        /**
         * {@inheritDoc}
         *
         * @throws NoSuchElementException if all the sources of the chain are
         *                               exhausted
         */
        @Override
        public E next()
        {
            if(!hasNext())
            {
                throw new NoSuchElementException();
            }
            E element = this.current.next();
            this.last = this.current;
            return element;
        }
        /**
         * {@inheritDoc}
         * <p>
         * Removes the last returned element from the source iterator that
         * supplied it, so the element is removed from the source itself.
         *
         * @throws IllegalStateException    if {@link #next()} has not been
         *                                  called since the last removal
         * @throws UnsupportedOperationException if the source iterator does
         *                                  not support removal
         */
        @Override
        public void remove()
        {
            if(this.last==null)
            {
                throw new IllegalStateException();
            }
            this.last.remove();
            this.last = null;
        }
    }
}
