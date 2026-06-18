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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Test-only Bee that records every message it receives, plus the
 * invocations of its {@code terminate()} and {@code exception(Exception)}
 * hooks, so unit tests can assert on what actually happened without
 * resorting to reflection or arbitrary sleeps. Not a test class itself;
 * used as a helper by several *Test classes in this package.
 */
class RecordingBee<M> extends Bee<M>
{
    final List<M> received = new CopyOnWriteArrayList<>();
    final AtomicBoolean terminated = new AtomicBoolean(false);
    final AtomicReference<Exception> lastException = new AtomicReference<>();
    private volatile Consumer<M> action;

    RecordingBee()
    {
        super();
    }

    RecordingBee(Hive hive)
    {
        super(hive);
    }

    RecordingBee(int threads, Hive hive)
    {
        super(threads, hive);
    }

    RecordingBee(int threads, Hive hive, int queueSize)
    {
        super(threads, hive, queueSize);
    }

    /**
     * Installs extra behavior run right after a message is recorded,
     * e.g. to make a particular message throw and exercise the
     * exception-handling path.
     */
    RecordingBee<M> withAction(Consumer<M> action)
    {
        this.action = action;
        return this;
    }

    @Override
    protected void receive(M m)
    {
        received.add(m);
        Consumer<M> a = this.action;
        if (a != null)
        {
            a.accept(m);
        }
    }

    @Override
    protected void terminate()
    {
        terminated.set(true);
    }

    @Override
    protected void exception(Exception ex)
    {
        lastException.set(ex);
    }
}
