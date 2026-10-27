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
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A stacked view over an ordered sequence of {@link List}s, the {@link List}
 * counterpart of {@link ChainMap}.
 * <p>
 * The lists are concatenated in order: the element at index {@code i} is
 * taken from the first list that covers that index, so the chain behaves as a
 * single continuous sequence of {@link #size()} elements without copying
 * anything. The chain keeps the given list instances by reference and every
 * lookup only walks the list headers, never the elements, therefore changes
 * made to the underlying lists are immediately visible through this view (and
 * vice versa), including lists that are empty in the middle of the chain.
 * <p>
 * The mutating methods delegate to the underlying list that owns the affected
 * index: {@link #set(int, Object)} and {@link #remove(int)} update the list
 * where the element resides, {@link #add(int, Object)} and
 * {@link #addAll(int, Collection)} splice into the list that owns the
 * insertion position, and {@link #add(Object)} and
 * {@link #addAll(Collection)} append to the {@linkplain #last() last} list.
 * Note that, unlike {@link ChainMap} and {@link ChainSet}, whose write target
 * is the first container, the append target of a chain of lists is the last
 * one, because the {@link List} contract defines {@code add} as an append at
 * the end of the sequence. {@link #removeAll(Collection)},
 * {@link #retainAll(Collection)} and {@link #clear()} keep every underlying
 * list by reference and delegate to it, so no element is ever copied.
 * <p>
 * {@link #iterator()}, {@link #listIterator()} and the iterators returned by
 * them are <em>live cursors</em> over the chain: they resolve positions
 * against the underlying lists on every step instead of walking a snapshot
 * (a snapshot would be wrong here, because {@link ListIterator#remove()} and
 * {@link ListIterator#set(Object)} are positional). Structural changes made
 * to the underlying lists by other means while an iteration is in progress
 * are not detected: this class does not implement the fail-fast behaviour of
 * {@link java.util.ArrayList}.
 * <p>
 * {@link #subList(int, int)} returns another {@code ChainList} built over the
 * {@link List#subList(int, int)} views of the underlying lists, so it is a
 * live view too and copies no element.
 * <p>
 * Like {@link java.util.ArrayList}, this class is not synchronized and
 * permits {@code null} elements if the backing lists do.
 *
 * @param <E> the type of elements maintained by this chain
 */
public class ChainList<E> implements List<E>
{
    private final List<List<E>> lists;

    /**
     * Constructs a new ChainList over the given lists, from the first to the
     * last.
     *
     * @param lists at least one list; must not be {@code null} nor contain
     *              {@code null} elements
     * @throws NullPointerException     if {@code lists} or one of its elements
     *                                  is {@code null}
     * @throws IllegalArgumentException if no list is given
     */
    @SafeVarargs
    public ChainList(List<E>... lists)
    {
        this(toCollection(lists));
    }
    /**
     * Constructs a new ChainList over the given lists, from the first to the
     * last.
     *
     * @param lists at least one list; must not be {@code null} nor contain
     *              {@code null} elements
     * @throws NullPointerException     if {@code lists} or one of its elements
     *                                  is {@code null}
     * @throws IllegalArgumentException if no list is given
     */
    public ChainList(Collection<? extends List<E>> lists)
    {
        Objects.requireNonNull(lists, "lists must not be null");
        if(lists.isEmpty())
        {
            throw new IllegalArgumentException("at least one list is required");
        }
        ArrayList<List<E>> copy = new ArrayList<>(lists.size());
        for(List<E> l : lists)
        {
            copy.add(Objects.requireNonNull(l, "list must not be null"));
        }
        this.lists = Collections.unmodifiableList(copy);
    }

    private static <E> Collection<? extends List<E>> toCollection(List<E>[] lists)
    {
        Objects.requireNonNull(lists, "lists must not be null");
        return Arrays.asList(lists);
    }

    /**
     * Returns the lists of this chain as an unmodifiable list, from the first
     * to the last.
     *
     * @return the lists of this chain; never {@code null}
     */
    public List<List<E>> lists()
    {
        return this.lists;
    }

    /**
     * Returns the first list of the chain.
     *
     * @return the first list; never {@code null}
     */
    public List<E> first()
    {
        return this.lists.get(0);
    }

    /**
     * Returns the last list of the chain, the target of every append.
     *
     * @return the last list; never {@code null}
     */
    public List<E> last()
    {
        return this.lists.get(this.lists.size()-1);
    }

    /**
     * Returns the position of the list of the chain that owns the given
     * index.
     *
     * @param index a global index of the chain
     * @return the position of the owning list in {@link #lists()}
     * @throws IndexOutOfBoundsException if the index is negative or not less
     *                                   than {@link #size()}
     */
    private int owner(int index)
    {
        if(index<0)
        {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        int offset = 0;
        for(int i = 0, n = this.lists.size(); i<n; i++)
        {
            int size = this.lists.get(i).size();
            if(index<offset+size)
            {
                return i;
            }
            offset += size;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + offset);
    }

    /**
     * Returns the offset of the list at the given position of the chain, that
     * is, the number of elements of the preceding lists.
     *
     * @param position the position of a list in {@link #lists()}
     * @return the offset of that list within the chain
     */
    private int offsetOf(int position)
    {
        int offset = 0;
        for(int i = 0; i<position; i++)
        {
            offset += this.lists.get(i).size();
        }
        return offset;
    }

    /**
     * Builds a fresh copy of the content of this chain, in chain order.
     *
     * @return a fresh snapshot; never {@code null}
     */
    private ArrayList<E> snapshot()
    {
        ArrayList<E> snap = new ArrayList<>(size());
        for(List<E> l : this.lists)
        {
            snap.addAll(l);
        }
        return snap;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the sum of the sizes of the underlying lists, recomputed on
     * every call so that changes made to those lists are reflected at once.
     * The elements themselves are never visited.
     */
    @Override
    public int size()
    {
        int size = 0;
        for(List<E> l : this.lists)
        {
            size += l.size();
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        for(List<E> l : this.lists)
        {
            if(!l.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The index is resolved against the underlying lists in order: the index
     * of the first list is followed by the index of the second one and so
     * on, skipping empty lists.
     *
     * @throws IndexOutOfBoundsException if the index is negative or not less
     *                                   than {@link #size()}
     */
    @Override
    public E get(int index)
    {
        int owner = owner(index);
        return this.lists.get(owner).get(index-offsetOf(owner));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Replaces the element of the underlying list that owns the index.
     *
     * @throws IndexOutOfBoundsException if the index is negative or not less
     *                                   than {@link #size()}
     */
    @Override
    public E set(int index, E element)
    {
        int owner = owner(index);
        return this.lists.get(owner).set(index-offsetOf(owner), element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Inserts the element into the underlying list that owns the insertion
     * position, so an index equal to {@link #size()} appends to the
     * {@linkplain #last() last} list.
     *
     * @throws IndexOutOfBoundsException if the index is negative or greater
     *                                   than {@link #size()}
     */
    @Override
    public void add(int index, E element)
    {
        if(index==size())
        {
            last().add(element);
            return;
        }
        int owner = owner(index);
        this.lists.get(owner).add(index-offsetOf(owner), element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The element is appended to the {@linkplain #last() last} list of the
     * chain, keeping it at the end of the sequence.
     */
    @Override
    public boolean add(E element)
    {
        return last().add(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the element at the given index from the underlying list that
     * owns it, shifting the rest of that list, so the elements that follow
     * the index in the chain move one position forward.
     *
     * @throws IndexOutOfBoundsException if the index is negative or not less
     *                                   than {@link #size()}
     */
    @Override
    public E remove(int index)
    {
        int owner = owner(index);
        return this.lists.get(owner).remove(index-offsetOf(owner));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the first occurrence in chain order, that is, from the
     * underlying list that owns its index.
     */
    @Override
    public boolean remove(Object element)
    {
        int index = indexOf(element);
        if(index<0)
        {
            return false;
        }
        remove(index);
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the index of the first occurrence in chain order.
     */
    @Override
    public int indexOf(Object element)
    {
        int offset = 0;
        for(List<E> l : this.lists)
        {
            int i = l.indexOf(element);
            if(i>=0)
            {
                return offset+i;
            }
            offset += l.size();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the index of the last occurrence in chain order.
     */
    @Override
    public int lastIndexOf(Object element)
    {
        for(int i = this.lists.size()-1; i>=0; i--)
        {
            List<E> l = this.lists.get(i);
            int j = l.lastIndexOf(element);
            if(j>=0)
            {
                return offsetOf(i)+j;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object element)
    {
        for(List<E> l : this.lists)
        {
            if(l.contains(element))
            {
                return true;
            }
        }
        return false;
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
     * All the elements are appended to the {@linkplain #last() last} list of
     * the chain.
     */
    @Override
    public boolean addAll(Collection<? extends E> c)
    {
        return last().addAll(c);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The elements are spliced into the underlying list that owns the
     * insertion position, so an index equal to {@link #size()} appends to
     * the {@linkplain #last() last} list.
     *
     * @throws IndexOutOfBoundsException if the index is negative or greater
     *                                   than {@link #size()}
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        if(index==size())
        {
            return last().addAll(c);
        }
        int owner = owner(index);
        return this.lists.get(owner).addAll(index-offsetOf(owner), c);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The removal is delegated to every underlying list, so no element is
     * copied and the chain keeps only the elements of {@code c}.
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean modified = false;
        for(List<E> l : this.lists)
        {
            modified |= l.removeAll(c);
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The removal is delegated to every underlying list, so no element is
     * copied and the chain keeps only the elements of {@code c}.
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean modified = false;
        for(List<E> l : this.lists)
        {
            modified |= l.retainAll(c);
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Every list of the chain is cleared, so that the view becomes empty.
     */
    @Override
    public void clear()
    {
        for(List<E> l : this.lists)
        {
            l.clear();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a snapshot of the elements of the chain, in chain order.
     */
    @Override
    public Object[] toArray()
    {
        return snapshot().toArray();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a snapshot of the elements of the chain, in chain order.
     */
    @Override
    public <T> T[] toArray(T[] a)
    {
        return snapshot().toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned iterator is a live cursor over the chain; see the
     * {@link ChainList class documentation} for its behaviour with respect to
     * concurrent structural changes.
     */
    @Override
    public Iterator<E> iterator()
    {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned iterator is a live cursor over the chain; see the
     * {@link ChainList class documentation} for its behaviour with respect to
     * concurrent structural changes.
     */
    @Override
    public ListIterator<E> listIterator()
    {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned iterator is a live cursor over the chain; see the
     * {@link ChainList class documentation} for its behaviour with respect to
     * concurrent structural changes.
     *
     * @throws IndexOutOfBoundsException if the index is negative or greater
     *                                   than {@link #size()}
     */
    @Override
    public ListIterator<E> listIterator(int index)
    {
        if(index<0 || index>size())
        {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return new ChainListIterator(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned list is a {@code ChainList} over the
     * {@link List#subList(int, int)} views of the underlying lists: it is a
     * live view too and copies no element.
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative or
     *                                   {@code toIndex} is greater than
     *                                   {@link #size()}
     * @throws IllegalArgumentException  if {@code fromIndex} is greater than
     *                                   {@code toIndex}
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex)
    {
        if(fromIndex<0)
        {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if(toIndex>size())
        {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if(fromIndex>toIndex)
        {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        ArrayList<List<E>> slices = new ArrayList<>();
        int offset = 0;
        for(List<E> l : this.lists)
        {
            int start = Math.max(fromIndex-offset, 0);
            int end = Math.min(toIndex-offset, l.size());
            if(start<end)
            {
                slices.add(l.subList(start, end));
            }
            offset += l.size();
            if(offset>=toIndex)
            {
                break;
            }
        }
        if(slices.isEmpty())
        {
            int owner = fromIndex==size() ? this.lists.size()-1 : owner(fromIndex);
            List<E> l = this.lists.get(owner);
            int local = fromIndex-offsetOf(owner);
            slices.add(l.subList(local, local));
        }
        return new ChainList<>(slices);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares the elements of this chain with those of the given list, as
     * specified by {@link List}.
     */
    @Override
    public boolean equals(Object o)
    {
        if(this==o)
        {
            return true;
        }
        if(!(o instanceof List))
        {
            return false;
        }
        List<?> other = (List<?>) o;
        if(other.size()!=size())
        {
            return false;
        }
        Iterator<E> a = iterator();
        Iterator<?> b = other.iterator();
        while(a.hasNext() && b.hasNext())
        {
            if(!Objects.equals(a.next(), b.next()))
            {
                return false;
            }
        }
        return !a.hasNext() && !b.hasNext();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The hash code is the one defined by {@link List} for the sequence of
     * the elements of the chain.
     */
    @Override
    public int hashCode()
    {
        int h = 1;
        for(E e : this)
        {
            h = 31*h+Objects.hashCode(e);
        }
        return h;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string of the form {@code [e, e, e]} holding the elements of
     * the chain, in chain order. The string is built without materialising a
     * copy of the elements.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstElement = true;
        for(List<E> l : this.lists)
        {
            for(E e : l)
            {
                if(!firstElement)
                {
                    sb.append(", ");
                }
                sb.append(e);
                firstElement = false;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * A live cursor over the elements of the chain: every position is
     * resolved against the underlying lists when it is used.
     */
    private final class ChainListIterator implements ListIterator<E>
    {
        private int cursor;
        private int last = -1;
        private boolean removable;
        private boolean adjustable;

        ChainListIterator(int index)
        {
            this.cursor = index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            return this.cursor<ChainList.this.size();
        }
        /**
         * {@inheritDoc}
         *
         * @throws NoSuchElementException if the iteration has no more elements
         */
        @Override
        public E next()
        {
            if(!hasNext())
            {
                throw new NoSuchElementException();
            }
            this.last = this.cursor;
            this.cursor++;
            this.removable = true;
            this.adjustable = true;
            return ChainList.this.get(this.last);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasPrevious()
        {
            return this.cursor>0;
        }
        /**
         * {@inheritDoc}
         *
         * @throws NoSuchElementException if the iteration has no previous element
         */
        @Override
        public E previous()
        {
            if(!hasPrevious())
            {
                throw new NoSuchElementException();
            }
            this.cursor--;
            this.last = this.cursor;
            this.removable = true;
            this.adjustable = true;
            return ChainList.this.get(this.last);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int nextIndex()
        {
            return this.cursor;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int previousIndex()
        {
            return this.cursor-1;
        }
        /**
         * {@inheritDoc}
         * <p>
         * Removes the last returned element from the underlying list that
         * owns its index.
         *
         * @throws IllegalStateException if {@link #next()} or
         *                               {@link #previous()} has not been
         *                               called since the last removal
         */
        @Override
        public void remove()
        {
            if(!this.removable)
            {
                throw new IllegalStateException();
            }
            ChainList.this.remove(this.last);
            this.cursor = this.last;
            this.last = -1;
            this.removable = false;
            this.adjustable = false;
        }
        /**
         * {@inheritDoc}
         * <p>
         * Replaces the last returned element in the underlying list that
         * owns its index.
         *
         * @throws IllegalStateException if {@link #next()} or
         *                               {@link #previous()} has not been
         *                               called, or if {@link #remove()} or
         *                               {@link #add(Object)} has been called
         *                               since the last call to
         *                               {@link #next()} or
         *                               {@link #previous()}
         */
        @Override
        public void set(E element)
        {
            if(!this.adjustable)
            {
                throw new IllegalStateException();
            }
            ChainList.this.set(this.last, element);
        }
        /**
         * {@inheritDoc}
         * <p>
         * Inserts the element at the cursor position of the chain.
         */
        @Override
        public void add(E element)
        {
            ChainList.this.add(this.cursor, element);
            this.cursor++;
            this.last = -1;
            this.removable = false;
            this.adjustable = false;
        }
    }
}
