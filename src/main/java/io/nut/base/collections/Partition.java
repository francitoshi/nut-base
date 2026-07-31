/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A container representing the result of partitioning an {@link Iterable} into two lists:
 * one containing elements that match a predicate (accepted), and one containing elements
 * that do not match (rejected).
 *
 * @param <T> the type of elements in the partition
 * @author franci
 * @since 1.8
 */
public final class Partition<T>
{
    private final List<T> accepted;
    private final List<T> rejected;

    private Partition(List<T> accepted, List<T> rejected)
    {
        this.accepted = accepted;
        this.rejected = rejected;
    }

    /**
     * Partitions the elements of an {@link Iterable} based on the given predicate.
     *
     * @param iterable the elements to partition; must not be {@code null}
     * @param predicate the predicate to evaluate each element; must not be {@code null}
     * @param <T> the type of elements
     * @return a Partition containing the accepted and rejected lists; never {@code null}
     * @throws NullPointerException if iterable or predicate is null
     */
    public static <T> Partition<T> partition(Iterable<T> iterable, Predicate<? super T> predicate)
    {
        Objects.requireNonNull(iterable, "iterable must not be null");
        Objects.requireNonNull(predicate, "predicate must not be null");

        List<T> accepted = new ArrayList<>();
        List<T> rejected = new ArrayList<>();

        for (T element : iterable)
        {
            if (predicate.test(element))
            {
                accepted.add(element);
            }
            else
            {
                rejected.add(element);
            }
        }

        return new Partition<>(
            Collections.unmodifiableList(accepted),
            Collections.unmodifiableList(rejected)
        );
    }

    /**
     * Returns an unmodifiable list of elements that matched the predicate.
     *
     * @return the accepted elements; never {@code null}
     */
    public List<T> accepted()
    {
        return accepted;
    }

    /**
     * Returns an unmodifiable list of elements that did not match the predicate.
     *
     * @return the rejected elements; never {@code null}
     */
    public List<T> rejected()
    {
        return rejected;
    }
}
