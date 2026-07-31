/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An {@link Iterator} wrapper that allows looking at the next element without
 * consuming it, via {@link #peek()}.
 * <p>
 * This implementation correctly supports iterators that produce {@code null}
 * elements, since it uses an internal flag ({@code hasPeeked}) rather than a
 * null-check to track whether a value has been buffered.
 * <p>
 * This class is not thread-safe. If multiple threads access the same instance
 * concurrently, external synchronization is required.
 * <p>
 * {@link #remove()} is delegated to the underlying iterator. It follows the
 * standard {@link Iterator} contract: it removes the last element returned by
 * {@link #next()}, and throws {@link IllegalStateException} if {@code next()}
 * has not yet been called, or if a call to {@link #peek()} is currently pending
 * (since the "current" element in that case has not actually been consumed via
 * {@code next()} yet).
 *
 * @param <T> the type of elements returned by this iterator
 */
public class PeekableIterator<T> implements Iterator<T>
{

    private final Iterator<T> iterator;
    private T next;
    private boolean hasPeeked;

    /**
     * Creates a new PeekableIterator wrapping the given iterator.
     *
     * @param iterator the iterator to wrap, must not be {@code null}
     * @throws NullPointerException if {@code iterator} is {@code null}
     */
    public PeekableIterator(Iterator<T> iterator)
    {
        this.iterator = Objects.requireNonNull(iterator, "iterator");
    }

    /**
     * Returns the next element without advancing the iterator. Calling this
     * method repeatedly without an intervening call to {@link #next()} returns
     * the same element each time.
     *
     * @return the next element
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public T peek()
    {
        if (!hasPeeked)
        {
            next = iterator.next();
            hasPeeked = true;
        }
        return next;
    }

    /**
     * Returns the next element in the iteration, consuming it. If an element
     * was previously peeked via {@link #peek()}, that same element is returned
     * here without re-invoking the underlying iterator.
     *
     * @return the next element
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public T next()
    {
        if (!hasPeeked)
        {
            return iterator.next();
        }

        T result = next;
        hasPeeked = false;
        next = null;
        return result;
    }

    /**
     * Returns {@code true} if there is a peeked element pending, or if the
     * underlying iterator has more elements.
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext()
    {
        return hasPeeked || iterator.hasNext();
    }

    /**
     * Removes the last element returned by {@link #next()}, delegating to the
     * underlying iterator.
     *
     * @throws UnsupportedOperationException if the underlying iterator does not
     * support {@code remove()}
     * @throws IllegalStateException if {@code next()} has not yet been called,
     * or if a peeked element is currently pending (i.e. {@link #peek()} was
     * called but {@link #next()} was not called afterward to consume it)
     */
    @Override
    public void remove()
    {
        if (hasPeeked)
        {
            throw new IllegalStateException("cannot remove(): a peeked element is pending, call next() first");
        }
        iterator.remove();
    }
}
