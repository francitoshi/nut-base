/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import java.util.Objects;

/**
 * An immutable container that holds three related values: a key, a
 * value, and an attribute.
 * <p>
 * {@code Trio} extends the two-element idea behind {@link Pair} to a
 * triple, with positional accessors ({@link #get1st()}, {@link #get2nd()},
 * {@link #get3rd()}) provided as aliases of the named accessors
 * ({@link #getKey()}, {@link #getVal()}, {@link #getAtt()}).
 * <p>
 * Instances are immutable: once created, the key, value and attribute
 * references cannot be changed.
 *
 * @param <K> the Key
 * @param <V> the Value
 * @param <A> the Attribute
 */
public class Trio<K,V,A>
{
    private final K key;
    private final V val;
    private final A att;

    /**
     * Creates a new trio holding the given key, value and attribute.
     *
     * @param key the key (first element) to store
     * @param val the value (second element) to store
     * @param att the attribute (third element) to store
     */
    public Trio(K key, V val, A att)
    {
        this.key = key;
        this.val = val;
        this.att = att;
    }

    /**
     * Returns the key (first element) of this trio.
     *
     * @return the key
     */
    public K getKey()
    {
        return key;
    }

    /**
     * Returns the value (second element) of this trio.
     *
     * @return the value
     */
    public V getVal()
    {
        return val;
    }

    /**
     * Returns the attribute (third element) of this trio.
     *
     * @return the attribute
     */
    public A getAtt()
    {
        return att;
    }

    /**
     * Returns a hash code value for this trio, based on the hash codes
     * of its key, value and attribute.
     *
     * @return a hash code value for this trio
     */
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.key);
        hash = 17 * hash + Objects.hashCode(this.val);
        hash = 17 * hash + Objects.hashCode(this.att);
        return hash;
    }

    /**
     * Compares this trio to the specified object. The result is {@code true}
     * if and only if the argument is not {@code null}, is exactly the same
     * runtime class as this trio, and has equal key, value and attribute
     * as determined by {@link Objects#equals(Object, Object)}.
     *
     * @param obj the object to compare with
     * @return {@code true} if the given object is equal to this trio
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
        final Trio<?, ?, ?> other = (Trio<?, ?, ?>) obj;
        if (!Objects.equals(this.key, other.key))
        {
            return false;
        }
        if (!Objects.equals(this.val, other.val))
        {
            return false;
        }
        return Objects.equals(this.att, other.att);
    }   

    /**
     * Returns the key of the given trio, or {@code null} if the trio
     * itself is {@code null}.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param <A> the type of the attribute
     * @param trio the trio to read from, may be {@code null}
     * @return the key of {@code trio}, or {@code null} if {@code trio} is {@code null}
     */
    public static <K,V,A> K getKey(Trio<K,V,A> trio)
    {
        return trio!=null ? trio.getKey() : null;
    }
    /**
     * Returns the value of the given trio, or {@code null} if the trio
     * itself is {@code null}.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param <A> the type of the attribute
     * @param trio the trio to read from, may be {@code null}
     * @return the value of {@code trio}, or {@code null} if {@code trio} is {@code null}
     */
    public static <K,V,A> V getVal(Trio<K,V,A> trio)
    {
        return trio!=null ? trio.getVal() : null;
    }
    /**
     * Returns the value of the given trio, or {@code null} if the trio
     * itself is {@code null}.
     * <p>
     * <b>Note:</b> despite its name, this method currently delegates to
     * {@link Trio#getVal()} rather than {@link Trio#getAtt()}; it does
     * not return the trio's attribute. This is preserved here as-is to
     * document the existing behavior.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @param <A> the type of the attribute
     * @param trio the trio to read from, may be {@code null}
     * @return the value of {@code trio} (not its attribute), or {@code null} if {@code trio} is {@code null}
     */
    public static <K,V,A> V getAtt(Trio<K,V,A> trio)
    {
        return trio!=null ? trio.getVal() : null;
    }
    
    /**
     * Returns the first element of this trio. This is an alias for
     * {@link #getKey()}.
     *
     * @return the first element
     */
    public K get1st() 
    {
        return key;
    }
    
    /**
     * Returns the second element of this trio. This is an alias for
     * {@link #getVal()}.
     *
     * @return the second element
     */
    public V get2nd() 
    {
        return val;
    }
    
    /**
     * Returns the third element of this trio. This is an alias for
     * {@link #getAtt()}.
     *
     * @return the third element
     */
    public A get3rd() 
    {
        return att;
    }
}
