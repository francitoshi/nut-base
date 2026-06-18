/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A BroadcastBee is a Bee&lt;T&gt; that fans out every message it receives
 * to a set of target {@code Sendable<T>} stages, e.g. several independent
 * {@code PipeBee}/{@code Bee} chains, so the same input feeds all of them
 * in parallel.
 * <p>
 * Targets can be supplied at construction time and/or added or removed
 * later with {@link #addTarget(Sendable)} / {@link #removeTarget(Sendable)};
 * the target list is backed by a {@link CopyOnWriteArrayList} so it is
 * safe to mutate it concurrently with message delivery.
 * <p>
 * The fan-in counterpart needs no dedicated class: any number of producers
 * can simply call {@link Sendable#send} on the very same downstream Bee.
 *
 * @param <T> the type of messages this BroadcastBee receives and
 *            forwards, unchanged, to every target
 */
public class BroadcastBee<T> extends Bee<T>
{
    protected final List<Sendable<T>> targets = new CopyOnWriteArrayList<>();

    @SafeVarargs
    public BroadcastBee(int threads, Hive hive, int queueSize, Sendable<T>... targets)
    {
        super(threads, hive, queueSize);
        addTargets(targets);
    }

    @SafeVarargs
    public BroadcastBee(int threads, Hive hive, Sendable<T>... targets)
    {
        super(threads, hive);
        addTargets(targets);
    }

    @SafeVarargs
    public BroadcastBee(Hive hive, Sendable<T>... targets)
    {
        super(hive);
        addTargets(targets);
    }

    @SafeVarargs
    public BroadcastBee(int threads, Sendable<T>... targets)
    {
        super(threads);
        addTargets(targets);
    }

    @SafeVarargs
    public BroadcastBee(Sendable<T>... targets)
    {
        super();
        addTargets(targets);
    }

    private void addTargets(Sendable<T>[] array)
    {
        for (Sendable<T> target : array)
        {
            addTarget(target);
        }
    }

    /**
     * Adds a new target that will receive every message from now on.
     *
     * @param target the Sendable&lt;T&gt; to add to the broadcast list
     * @return this BroadcastBee, to allow fluent chaining of additions
     */
    public BroadcastBee<T> addTarget(Sendable<T> target)
    {
        this.targets.add(Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    /**
     * Removes a previously added target so it stops receiving messages.
     *
     * @param target the Sendable&lt;T&gt; to remove
     * @return true if the target was present and has been removed
     */
    public boolean removeTarget(Sendable<T> target)
    {
        return this.targets.remove(target);
    }

    /**
     * @return an unmodifiable snapshot view of the current targets
     */
    public List<Sendable<T>> getTargets()
    {
        return Collections.unmodifiableList(targets);
    }

    @Override
    protected void receive(T m)
    {
        for (Sendable<T> target : targets)
        {
            target.send(m);
        }
    }

    @Override
    public BroadcastBee<T> shutdown(boolean onlyWhenEmpty)
    {
        return (BroadcastBee<T>) super.shutdown(onlyWhenEmpty);
    }

}
