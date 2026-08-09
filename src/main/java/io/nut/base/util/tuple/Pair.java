/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import java.util.Objects;

/**
 * An immutable container that holds two related values, a key and a value.
 * <p>
 * This class is intentionally generic about the semantics of its two
 * elements: depending on the use case, the same pair of values can be
 * viewed as a key/value entry, a left/right tuple, a 1st/2nd ordered
 * tuple, an in/out parameter, or a read/write pair. To support all of
 * these idioms without forcing callers into a single naming convention,
 * {@code Pair} exposes equivalent getters under several different names
 * ({@link #getKey()}/{@link #getVal()}, {@link #getLeft()}/{@link #getRight()},
 * {@link #get1st()}/{@link #get2nd()}, {@link #getIn()}/{@link #getOut()},
 * {@link #getRead()}/{@link #getWrite()}); all of them return the same
 * two underlying values.
 * <p>
 * Instances are immutable: once created, the key and value references
 * cannot be changed.
 *
 * @author franci
 * @param <K> the type of the key (first element)
 * @param <V> the type of the value (second element)
 */
public class Pair<K,V>
{
    final K key;
    final V val;

    /**
     * Creates a new pair holding the given key and value.
     *
     * @param key the key (first element) to store
     * @param val the value (second element) to store
     */
    public Pair(K key, V val)
    {
        this.key = key;
        this.val = val;
    }

    /**
     * Returns the key (first element) of this pair.
     *
     * @return the key
     */
    public K getKey()
    {
        return key;
    }

    /**
     * Returns the value (second element) of this pair.
     *
     * @return the value
     */
    public V getVal()
    {
        return val;
    }

    /**
     * Returns a string representation of this pair in the form
     * {@code key=val}.
     *
     * @return a string representation of this pair
     */
    @Override
    public String toString()
    {
        return key + "=" + val;
    }

    /**
     * Returns a hash code value for this pair, based on the hash codes
     * of its key and value.
     *
     * @return a hash code value for this pair
     */
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.key);
        hash = 97 * hash + Objects.hashCode(this.val);
        return hash;
    }

    /**
     * Compares this pair to the specified object. The result is {@code true}
     * if and only if the argument is not {@code null}, is exactly the same
     * runtime class as this pair, and has equal key and value as determined
     * by {@link Objects#equals(Object, Object)}.
     *
     * @param obj the object to compare with
     * @return {@code true} if the given object is equal to this pair
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (!Objects.equals(this.key, other.key))
        {
            return false;
        }
        return Objects.equals(this.val, other.val);
    }    

    /**
     * Returns the key of the given pair, or {@code null} if the pair
     * itself is {@code null}.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param pair the pair to read from, may be {@code null}
     * @return the key of {@code pair}, or {@code null} if {@code pair} is {@code null}
     */
    public static <K,V> K getKey(Pair<K,V> pair)
    {
        return pair!=null ? pair.getKey() : null;
    }
    /**
     * Returns the value of the given pair, or {@code null} if the pair
     * itself is {@code null}.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param pair the pair to read from, may be {@code null}
     * @return the value of {@code pair}, or {@code null} if {@code pair} is {@code null}
     */
    public static <K,V> V getVal(Pair<K,V> pair)
    {
        return pair!=null ? pair.getVal() : null;
    }
    
    /**
     * Creates a new pair from the given left and right values. This is a
     * convenience factory method equivalent to calling
     * {@link #Pair(Object, Object) new Pair<>(left, right)}.
     *
     * @param <K> the type of the left element (key)
     * @param <V> the type of the right element (value)
     * @param left the left element
     * @param right the right element
     * @return a new {@code Pair} containing {@code left} and {@code right}
     */
    public static <K,V> Pair<K, V> of(K left, V right) 
    {
        return new Pair<>(left, right);
    }
    /**
     * Returns the left element of this pair. This is an alias for
     * {@link #getKey()}.
     *
     * @return the left element
     */
    public K getLeft() 
    {
        return key;
    }

    /**
     * Returns the right element of this pair. This is an alias for
     * {@link #getVal()}.
     *
     * @return the right element
     */
    public V getRight() 
    {
        return val;
    }
    
    /**
     * Returns the first element of this pair. This is an alias for
     * {@link #getKey()}.
     *
     * @return the first element
     */
    public K get1st() 
    {
        return key;
    }
    
    /**
     * Returns the second element of this pair. This is an alias for
     * {@link #getVal()}.
     *
     * @return the second element
     */
    public V get2nd() 
    {
        return val;
    }

    /**
     * Returns the "in" element of this pair. This is an alias for
     * {@link #getKey()}, useful when the pair represents an input/output
     * relationship.
     *
     * @return the "in" element
     */
    public K getIn() 
    {
        return key;
    }
    
    /**
     * Returns the "out" element of this pair. This is an alias for
     * {@link #getVal()}, useful when the pair represents an input/output
     * relationship.
     *
     * @return the "out" element
     */
    public V getOut() 
    {
        return val;
    }

    /**
     * Returns the "read" element of this pair. This is an alias for
     * {@link #getKey()}, useful when the pair represents a read/write
     * relationship.
     *
     * @return the "read" element
     */
    public K getRead() 
    {
        return key;
    }
    
    /**
     * Returns the "write" element of this pair. This is an alias for
     * {@link #getVal()}, useful when the pair represents a read/write
     * relationship.
     *
     * @return the "write" element
     */
    public V getWrite() 
    {
        return val;
    }
    
    /**
     * Returns a new pair with the key and value swapped, i.e. the value
     * of this pair becomes the key of the returned pair and vice versa.
     * This pair is not modified.
     *
     * @return a new {@code Pair} with key and value swapped
     */
    public Pair<V,K> inverse()
    {
        return new Pair<>(val, key);
    }
    
}
