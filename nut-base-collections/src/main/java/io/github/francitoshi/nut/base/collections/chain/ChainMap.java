/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A stacked view over an ordered sequence of {@link Map}s.
 * <p>
 * The maps are consulted from first to last: every lookup
 * ({@link #get(Object)}, {@link #containsKey(Object)},
 * {@link #containsValue(Object)}, {@link #size()}, ...) considers the maps in
 * order and stops at the first one that contains the key, so the maps earlier
 * in the chain <em>shadow</em> the ones after them. The chain keeps the given
 * map instances by reference, therefore changes made to those maps are
 * immediately visible through this view (and vice versa).
 * <p>
 * Operations that <em>insert</em> a mapping are always applied to the
 * <strong>first</strong> map: {@link #put(Object, Object)},
 * {@link #putAll(Map)} and the Java 8 default methods built on top of them,
 * such as {@link #putIfAbsent(Object, Object)},
 * {@link #computeIfAbsent(Object, java.util.function.Function)},
 * {@link #compute(Object, java.util.function.BiFunction)} and
 * {@link #merge(Object, Object, java.util.function.BiFunction)}. A new entry
 * therefore always shadows the entries of the lower maps.
 * <p>
 * {@link #remove(Object)} removes the <em>effective</em> mapping, i.e. the very
 * mapping returned by {@link #get(Object)}, from the first map that contains
 * the key. If a lower map also contains that key, the lower mapping becomes
 * visible afterwards. {@link #clear()} clears every map of the chain, so the
 * view becomes empty.
 * <p>
 * <strong>The mutating methods act on their target map only, not on the chain
 * as a whole.</strong> {@link #put(Object, Object)} and
 * {@link #putAll(Map)} always write to the first map, and their return values
 * come from that map too: {@code put} returns the value previously stored in
 * the <em>first</em> map, which may be {@code null} even when
 * {@link #get(Object)} returns a non-null value from a lower map.
 * {@link #remove(Object)} returns the effective mapping even when the key is
 * still present in a lower map afterwards, so it may return a non-null value
 * while {@link #containsKey(Object)} keeps returning {@code true}. The same
 * applies to the {@code remove} operations of the collections returned by
 * {@link #keySet()}, {@link #values()} and {@link #entrySet()}: when a key
 * exists at several levels of the chain, a single removal only peels off the
 * topmost mapping, so it may take more than one call to remove the key from
 * every level. {@link ChainSet} follows the same convention for its
 * elements.
 * <p>
 * {@link #keySet()}, {@link #values()} and {@link #entrySet()} are live views:
 * they are rebuilt on every operation and honour mutations through
 * {@link #remove(Object)}; their iterators walk a snapshot of the keys taken
 * when the iterator is created, and support {@link Iterator#remove()}.
 * <p>
 * Like {@link java.util.HashMap}, this class is not synchronized and permits
 * {@code null} keys and values if the backing maps do.
 *
 * @param <E> the type of both the keys and the values of this chain
 */
public class ChainMap<E> implements Map<E,E>
{
    private final List<Map<E,E>> maps;

    /**
     * Constructs a new ChainMap over the given maps, from the highest (first
     * looked up, first written to) to the lowest.
     *
     * @param maps at least one map; must not be {@code null} nor contain
     *             {@code null} elements
     * @throws NullPointerException     if {@code maps} or one of its elements
     *                                  is {@code null}
     * @throws IllegalArgumentException if no map is given
     */
    @SafeVarargs
    public ChainMap(Map<E,E>... maps)
    {
        this(toCollection(maps));
    }
    /**
     * Constructs a new ChainMap over the given maps, from the highest (first
     * looked up, first written to) to the lowest.
     *
     * @param maps at least one map; must not be {@code null} nor contain
     *             {@code null} elements
     * @throws NullPointerException     if {@code maps} or one of its elements
     *                                  is {@code null}
     * @throws IllegalArgumentException if no map is given
     */
    public ChainMap(Collection<? extends Map<E,E>> maps)
    {
        Objects.requireNonNull(maps, "maps must not be null");
        if(maps.isEmpty())
        {
            throw new IllegalArgumentException("at least one map is required");
        }
        ArrayList<Map<E,E>> copy = new ArrayList<>(maps.size());
        for(Map<E,E> m : maps)
        {
            copy.add(Objects.requireNonNull(m, "map must not be null"));
        }
        this.maps = Collections.unmodifiableList(copy);
    }

    private static <E> Collection<? extends Map<E,E>> toCollection(Map<E,E>[] maps)
    {
        Objects.requireNonNull(maps, "maps must not be null");
        return Arrays.asList(maps);
    }

    /**
     * Returns the maps of this chain as an unmodifiable list, from the highest
     * to the lowest.
     *
     * @return the maps of this chain; never {@code null}
     */
    public List<Map<E,E>> maps()
    {
        return this.maps;
    }

    /**
     * Returns the first map of the chain, the target of every insertion.
     *
     * @return the first map; never {@code null}
     */
    public Map<E,E> first()
    {
        return this.maps.get(0);
    }

    /**
     * Returns the first map of the chain that contains the given key.
     *
     * @param key the key to locate
     * @return the map containing the key, or {@code null} if none does
     */
    private Map<E,E> locate(Object key)
    {
        for(Map<E,E> m : this.maps)
        {
            if(m.containsKey(key))
            {
                return m;
            }
        }
        return null;
    }

    /**
     * Builds the effective mapping of this chain: for every key the value of
     * the first map that contains it, in chain order.
     *
     * @return a fresh snapshot; never {@code null}
     */
    private LinkedHashMap<E,E> snapshot()
    {
        LinkedHashMap<E,E> snap = new LinkedHashMap<>();
        for(Map<E,E> m : this.maps)
        {
            for(Entry<E,E> e : m.entrySet())
            {
                if(!snap.containsKey(e.getKey()))
                {
                    snap.put(e.getKey(), e.getValue());
                }
            }
        }
        return snap;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the value of the first map of the chain that contains the key.
     */
    @Override
    public E get(Object key)
    {
        for(Map<E,E> m : this.maps)
        {
            if(m.containsKey(key))
            {
                return m.get(key);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E getOrDefault(Object key, E defaultValue)
    {
        for(Map<E,E> m : this.maps)
        {
            if(m.containsKey(key))
            {
                return m.get(key);
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key)
    {
        for(Map<E,E> m : this.maps)
        {
            if(m.containsKey(key))
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
    public boolean containsValue(Object value)
    {
        for(Map<E,E> m : this.maps)
        {
            if(m.containsValue(value))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The size is the number of <em>distinct</em> keys of all the maps of the
     * chain: a key present in several maps is counted once.
     */
    @Override
    public int size()
    {
        HashSet<E> keys = new HashSet<>();
        for(Map<E,E> m : this.maps)
        {
            keys.addAll(m.keySet());
        }
        return keys.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        for(Map<E,E> m : this.maps)
        {
            if(!m.isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The entry is stored in the first map of the chain, shadowing any entry
     * with the same key in the lower maps. The returned value is the value
     * previously stored in the <em>first</em> map: it may be {@code null}
     * even when {@link #get(Object)} returns a non-null value from a lower
     * map.
     */
    @Override
    public E put(E key, E value)
    {
        return first().put(key, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * All the entries are stored in the first map of the chain.
     */
    @Override
    public void putAll(Map<? extends E,? extends E> m)
    {
        first().putAll(m);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the effective mapping, that is, the one returned by
     * {@link #get(Object)}, from the first map that contains the key. If a
     * lower map of the chain also contains the key, that mapping becomes
     * visible after this call, so this method may return a non-null value
     * while {@link #containsKey(Object)} still returns {@code true} for that
     * key.
     */
    @Override
    public E remove(Object key)
    {
        Map<E,E> m = locate(key);
        if(m==null)
        {
            return null;
        }
        return m.remove(key);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Every map of the chain is cleared, so that the view becomes empty.
     */
    @Override
    public void clear()
    {
        for(Map<E,E> m : this.maps)
        {
            m.clear();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned set is a live view of the distinct keys of the chain, in
     * chain order. Removing an element from it removes the effective mapping.
     */
    @Override
    public Set<E> keySet()
    {
        return new KeySetView();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned collection is a live view of the effective values of the
     * chain, in chain order. Values bound to different keys may appear more
     * than once. Removing an element from it removes the first mapping, in
     * chain order, whose value is equal to it.
     */
    @Override
    public Collection<E> values()
    {
        return new ValuesView();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned set is a live view of the effective mappings of the chain,
     * in chain order. Its entries are write-through: {@link Entry#setValue}
     * updates the map of the chain that currently supplies the key, or the
     * first map if the key is no longer present.
     */
    @Override
    public Set<Entry<E,E>> entrySet()
    {
        return new EntrySetView();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares the effective mappings of this chain with those of the given
     * map, as specified by {@link Map}.
     */
    @Override
    public boolean equals(Object o)
    {
        if(this==o)
        {
            return true;
        }
        if(!(o instanceof Map))
        {
            return false;
        }
        Map<?,?> other = (Map<?,?>) o;
        if(other.size()!=size())
        {
            return false;
        }
        return entrySet().equals(other.entrySet());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The hash code is the sum of the hash codes of the effective entries, as
     * specified by {@link Map}.
     */
    @Override
    public int hashCode()
    {
        int h = 0;
        for(Entry<E,E> e : entrySet())
        {
            h += e.hashCode();
        }
        return h;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string of the form {@code {key=value, key=value}} holding the
     * effective mappings of the chain.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean firstEntry = true;
        for(Entry<E,E> e : entrySet())
        {
            if(!firstEntry)
            {
                sb.append(", ");
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
            firstEntry = false;
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * A write-through entry of the chain: its value is always resolved against
     * the maps of the chain.
     */
    private final class ChainEntry implements Entry<E,E>
    {
        private final E key;

        ChainEntry(E key)
        {
            this.key = key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public E getKey()
        {
            return this.key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public E getValue()
        {
            return ChainMap.this.get(this.key);
        }

        /**
         * {@inheritDoc}
         * <p>
         * The value is set in the first map of the chain that contains the
         * key, or in the first map if the key is no longer present.
         */
        @Override
        public E setValue(E value)
        {
            Map<E,E> m = locate(this.key);
            if(m==null)
            {
                return first().put(this.key, value);
            }
            return m.put(this.key, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o)
        {
            if(!(o instanceof Entry))
            {
                return false;
            }
            Entry<?,?> other = (Entry<?,?>) o;
            Object otherKey = other.getKey();
            Object otherValue = other.getValue();
            return Objects.equals(this.key, otherKey) && Objects.equals(getValue(), otherValue);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return Objects.hashCode(this.key) ^ Objects.hashCode(getValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return this.key + "=" + getValue();
        }
    }

    /**
     * A set view of the distinct keys of the chain.
     */
    private final class KeySetView extends AbstractSet<E>
    {
        /**
         * {@inheritDoc}
         * <p>
         * The iterator walks a snapshot of the keys and removes through the
         * chain.
         */
        @Override
        public Iterator<E> iterator()
        {
            Iterator<E> base = new ArrayList<>(snapshot().keySet()).iterator();
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
                    ChainMap.this.remove(this.last);
                    this.removable = false;
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
        {
            return ChainMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
        {
            return ChainMap.this.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object o)
        {
            return ChainMap.this.containsKey(o);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(Object o)
        {
            if(!ChainMap.this.containsKey(o))
            {
                return false;
            }
            ChainMap.this.remove(o);
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
        {
            ChainMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return snapshot().keySet().toString();
        }
    }

    /**
     * A collection view of the effective values of the chain.
     */
    private final class ValuesView extends AbstractCollection<E>
    {
        /**
         * {@inheritDoc}
         * <p>
         * The iterator walks a snapshot of the entries and removes through the
         * chain.
         */
        @Override
        public Iterator<E> iterator()
        {
            final Iterator<Entry<E,E>> base = new ArrayList<>(snapshot().entrySet()).iterator();
            return new Iterator<E>()
            {
                private Entry<E,E> last;
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
                    return this.last.getValue();
                }
                @Override
                public void remove()
                {
                    if(!this.removable)
                    {
                        throw new IllegalStateException();
                    }
                    ChainMap.this.remove(this.last.getKey());
                    this.removable = false;
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
        {
            return ChainMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
        {
            return ChainMap.this.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object o)
        {
            for(Entry<E,E> e : snapshot().entrySet())
            {
                if(Objects.equals(e.getValue(), o))
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Removes the first mapping, in chain order, whose value is equal to
         * {@code o}.
         */
        @Override
        public boolean remove(Object o)
        {
            for(Entry<E,E> e : snapshot().entrySet())
            {
                if(Objects.equals(e.getValue(), o))
                {
                    ChainMap.this.remove(e.getKey());
                    return true;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
        {
            ChainMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return snapshot().values().toString();
        }
    }

    /**
     * A set view of the effective mappings of the chain.
     */
    private final class EntrySetView extends AbstractSet<Entry<E,E>>
    {
        /**
         * {@inheritDoc}
         * <p>
         * The iterator walks a snapshot of the keys and yields write-through
         * entries; removal goes through the chain.
         */
        @Override
        public Iterator<Entry<E,E>> iterator()
        {
            final Iterator<E> base = new ArrayList<>(snapshot().keySet()).iterator();
            return new Iterator<Entry<E,E>>()
            {
                private E last;
                private boolean removable;

                @Override
                public boolean hasNext()
                {
                    return base.hasNext();
                }
                @Override
                public Entry<E,E> next()
                {
                    if(!base.hasNext())
                    {
                        throw new NoSuchElementException();
                    }
                    this.last = base.next();
                    this.removable = true;
                    return new ChainEntry(this.last);
                }
                @Override
                public void remove()
                {
                    if(!this.removable)
                    {
                        throw new IllegalStateException();
                    }
                    ChainMap.this.remove(this.last);
                    this.removable = false;
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
        {
            return ChainMap.this.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
        {
            return ChainMap.this.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object o)
        {
            if(!(o instanceof Entry))
            {
                return false;
            }
            Entry<?,?> e = (Entry<?,?>) o;
            Object key = e.getKey();
            if(!ChainMap.this.containsKey(key))
            {
                return false;
            }
            return Objects.equals(ChainMap.this.get(key), e.getValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(Object o)
        {
            if(!contains(o))
            {
                return false;
            }
            ChainMap.this.remove(((Entry<?,?>) o).getKey());
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
        {
            ChainMap.this.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return snapshot().entrySet().toString();
        }
    }
}
