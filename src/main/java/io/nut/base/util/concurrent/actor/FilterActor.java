/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A pipeline stage that forwards only those messages that satisfy a
 * {@link Predicate}, discarding the rest without further processing.
 *
 * @param <T> the type of messages this stage receives, tests, and may forward
 */
public class FilterActor<T> extends LinkableActor<T,T>
{
    private final Predicate<T> predicate;

    public FilterActor(ActorHub actorHub, int threads, int queueSize, Predicate<T> predicate)
    {
        super(null);
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
        ActorHooks<T> hooks = new ActorHooks<T>()
        {
            @Override
            public void receive(T m, long seq)
            {
                if (predicate.test(m))
                {
                    FilterActor.this.forward(m);
                }
            }

            @Override
            public void terminate()
            {
            }

            @Override
            public void exception(Exception ex)
            {
                FilterActor.this.handleException(ex);
            }
        };
        this.inner = ActorFlavors.create(actorHub, threads, queueSize, hooks);
    }

    public FilterActor(ActorHub actorHub, Predicate<T> predicate)
    {
        this(actorHub, 0, 0, predicate);
    }

    public FilterActor(int threads, int queueSize, Predicate<T> predicate)
    {
        this(null, threads, queueSize, predicate);
    }

    public FilterActor(Predicate<T> predicate)
    {
        this(null, 1, 0, predicate);
    }

}
