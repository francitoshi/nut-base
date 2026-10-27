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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A stacked view over an ordered sequence of {@link Set}s, the {@link Set}
 * counterpart of {@link ChainMap}.
 * <p>
 * The sets are consulted from first to last: every lookup
 * ({@link #contains(Object)}, {@link #size()}, ...) considers the sets in
 * order and stops at the first one that contains the element, so the sets
 * earlier in the chain <em>shadow</em> the ones after them. The chain keeps
 * the given set instances by reference, therefore changes made to those sets
 * are immediately visible through this view (and vice versa).
 * <p>
 * Operations that <em>insert</em> an element are always applied to the
 * <strong>first</strong> set: {@link #add(Object)} and
 * {@link #addAll(Collection)}. The element therefore always shadows the same
 * element in the lower sets.
 * <p>
 * {@link #remove(Object)} removes the element from the first set of the chain
 * that contains it. If a lower set also contains that element, it stays
 * visible afterwards, because the element is still part of the chain.
 * {@link #clear()} clears every set of the chain, so the view becomes empty.
 * <p>
 * <strong>Deviation from the {@link Set} and {@link Collection} contracts,
 * mirroring {@link ChainMap}:</strong> the mutating methods act on the target
 * set only, not on the chain as a whole. {@link #add(Object)} and
 * {@link #addAll(Collection)} always write to the first set and therefore may
 * return {@code true} even when the visible content of the chain is unchanged
 * (the element was already present in a lower set). {@link #remove(Object)}
 * removes only the <em>effective</em> occurrence, the one that
 * {@link #contains(Object)} sees, so it may return {@code true} while
 * {@link #contains(Object)} keeps returning {@code true} for a lower set that
 * still holds the element. The bulk removals
 * ({@link #removeAll(Collection)}, {@link #retainAll(Collection)} and the
 * default {@link Collection#removeIf(java.util.function.Predicate)}) follow
 * the same rule: an element present at several levels of the chain may
 * survive a single call, so repeat the call to peel it off every level.
 * <p>
 * {@link #iterator()} walks a snapshot of the distinct elements taken when the
 * iterator is created, in chain order, and supports {@link Iterator#remove()}.
 * <p>
 * Like {@link java.util.HashSet}, this class is not synchronized and permits
 * {@code null} elements if the backing sets do.
 *
 * @param <E> the type of elements maintained by this chain
 */
public class ChainSet<E> implements Set<E>
{
    private final List<Set<E>> sets;

    /**
     * Constructs a new ChainSet over the given sets, from the highest (first
     * looked up, first written to) to the lowest.
     *
     * @param sets at least one set; must not be {@code null} nor contain
     *             {@code null} elements
     * @throws NullPointerException     if {@code sets} or one of its elements
     *                                  is {@code null}
     * @throws IllegalArgumentException if no set is given
     */
    @SafeVarargs
    public ChainSet(Set<E>... sets)
    {
        this(toCollection(sets));
    }
    /**
     * Constructs a new ChainSet over the given sets, from the highest (first
     * looked up, first written to) to the lowest.
     *
     * @param sets at least one set; must not be {@code null} nor contain
     *             {@code null} elements
     * @throws NullPointerException     if {@code sets} or one of its elements
     *                                  is {@code null}
     * @throws IllegalArgumentException if no set is given
     */
    public ChainSet(Collection<? extends Set<E>> sets)
    {
        Objects.requireNonNull(sets, "sets must not be null");
        if(sets.isEmpty())
        {
            throw new IllegalArgumentException("at least one set is required");
        }
        ArrayList<Set<E>> copy = new ArrayList<>(sets.size());
        for(Set<E> s : sets)
        {
            copy.add(Objects.requireNonNull(s, "set must not be null"));
        }
        this.sets = Collections.unmodifiableList(copy);
    }

    private static <E> Collection<? extends Set<E>> toCollection(Set<E>[] sets)
    {
        Objects.requireNonNull(sets, "sets must not be null");
        return Arrays.asList(sets);
    }

    /**
     * Returns the sets of this chain as an unmodifiable list, from the highest
     * to the lowest.
     *
     * @return the sets of this chain; never {@code null}
     */
    public List<Set<E>> sets()
    {
        return this.sets;
    }

    /**
     * Returns the first set of the chain, the target of every insertion.
     *
     * @return the first set; never {@code null}
     */
    public Set<E> first()
    {
        return this.sets.get(0);
    }

    /**
     * Returns the first set of the chain that contains the given element.
     *
     * @param element the element to locate
     * @return the set containing the element, or {@code null} if none does
     */
    private Set<E> locate(Object element)
    {
        for(Set<E> s : this.sets)
        {
            if(s.contains(element))
            {
                return s;
            }
        }
        return null;
    }

    /**
     * Builds the effective content of this chain: every element of the sets,
     * in chain order, keeping the first occurrence of each element.
     *
     * @return a fresh snapshot; never {@code null}
     */
    private LinkedHashSet<E> snapshot()
    {
        LinkedHashSet<E> snap = new LinkedHashSet<>();
        for(Set<E> s : this.sets)
        {
            snap.addAll(s);
        }
        return snap;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if any set of the chain contains the element.
     */
    @Override
    public boolean contains(Object element)
    {
        for(Set<E> s : this.sets)
        {
            if(s.contains(element))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The size is the number of <em>distinct</em> elements of all the sets of
     * the chain: an element present in several sets is counted once.
     */
    @Override
    public int size()
    {
        return snapshot().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        for(Set<E> s : this.sets)
        {
            if(!s.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The element is stored in the first set of the chain, shadowing the same
     * element in the lower sets. Mirroring {@link ChainMap#put}, the element
     * is inserted even when a lower set already contains it, so this method
     * may return {@code true} while the visible content of the chain stays
     * unchanged.
     */
    @Override
    public boolean add(E element)
    {
        return first().add(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * All the elements are stored in the first set of the chain, even the
     * ones already present in a lower set; the return value reflects the
     * first set only, not the chain as a whole.
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return first().addAll(c);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the element from the first set of the chain that contains it.
     * If a lower set of the chain also contains the element, it stays visible
     * after this call, so this method may return {@code true} while
     * {@link #contains(Object)} still returns {@code true} for that element.
     */
    @Override
    public boolean remove(Object element)
    {
        Set<E> s = locate(element);
        if(s==null)
        {
            return false;
        }
        return s.remove(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Every set of the chain is cleared, so that the view becomes empty.
     */
    @Override
    public void clear()
    {
        for(Set<E> s : this.sets)
        {
            s.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        for(Object o : c)
        {
            if(!contains(o))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Each element is removed from the set of the chain that supplies it, so
     * a lower set that also holds the element keeps it visible: an element
     * present at several levels of the chain may need more than one call to
     * be removed from every level.
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean modified = false;
        if(size()>c.size())
        {
            for(Object o : c)
            {
                modified |= remove(o);
            }
        }
        else
        {
            for(Object o : snapshot())
            {
                if(c.contains(o))
                {
                    modified |= remove(o);
                }
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Every element of the chain that is not in {@code c} is removed from the
     * set of the chain that supplies it; as with {@link #removeAll}, an
     * element present at several levels of the chain may survive a single
     * call, so repeat the call to remove it from every level.
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean modified = false;
        for(Object o : snapshot())
        {
            if(!c.contains(o))
            {
                modified |= remove(o);
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a snapshot of the distinct elements of the chain, in chain
     * order.
     */
    @Override
    public Object[] toArray()
    {
        return snapshot().toArray();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a snapshot of the distinct elements of the chain, in chain
     * order.
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return snapshot().toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The iterator walks a snapshot of the distinct elements and removes
     * through the chain, so each {@link Iterator#remove()} call peels off the
     * effective occurrence only: an element present at several levels of the
     * chain is still visible after the removal.
     */
    @Override
    public Iterator<E> iterator()
    {
        Iterator<E> base = snapshot().iterator();
        return new Iterator<E>()
        {
            private E last;
            private boolean removable;

            @Override
            public boolean hasNext()
            {
                return base.hasNext();
            }
            @Override
            public E next()
            {
                if(!base.hasNext())
                {
                    throw new NoSuchElementException();
                }
                this.last = base.next();
                this.removable = true;
                return this.last;
            }
            @Override
            public void remove()
            {
                if(!this.removable)
                {
                    throw new IllegalStateException();
                }
                ChainSet.this.remove(this.last);
                this.removable = false;
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares the effective content of this chain with that of the given
     * set, as specified by {@link Set}.
     */
    @Override
    public boolean equals(Object o)
    {
        if(this==o)
        {
            return true;
        }
        if(!(o instanceof Set))
        {
            return false;
        }
        Set<?> other = (Set<?>) o;
        if(other.size()!=size())
        {
            return false;
        }
        return containsAll(other);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The hash code is the sum of the hash codes of the distinct elements of
     * the chain, as specified by {@link Set}.
     */
    @Override
    public int hashCode()
    {
        int h = 0;
        for(E e : snapshot())
        {
            h += Objects.hashCode(e);
        }
        return h;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string of the form {@code [e, e, e]} holding the distinct
     * elements of the chain, in chain order.
     */
    @Override
    public String toString()
    {
        return snapshot().toString();
    }
}
