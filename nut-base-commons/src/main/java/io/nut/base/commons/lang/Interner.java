/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

/**
 * Returns canonical instances of a type.
 * <p>
 * An interner is useful to reduce memory usage when many equal instances are
 * created, and to make identity comparison possible for values that are
 * otherwise only {@link Object#equals(Object) equal}.
 * <p>
 * Implementations rely on {@link Object#equals(Object)} and
 * {@link Object#hashCode()}. The interned values should therefore be immutable,
 * or at least their equality state must not change once interned.
 *
 * @param <E> the type of interned values
 * @author franci
 */
@FunctionalInterface
public interface Interner<E>
{
    /**
     * Returns a canonical instance equal to the given sample.
     * <p>
     * If an equal instance is already interned, it is returned; otherwise the
     * sample itself is stored and returned. In both cases, interning the
     * returned value again yields the same instance.
     *
     * @param sample the value to intern; must not be {@code null}
     * @return the canonical instance for the given value
     * @throws NullPointerException if {@code sample} is {@code null}
     */
    E intern(E sample);
}
