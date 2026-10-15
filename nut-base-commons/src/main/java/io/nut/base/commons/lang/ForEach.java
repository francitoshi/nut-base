/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class ForEach
{
    private static final Logger LOG = Logger.getLogger(ForEach.class.getName());

    public static <T> void foreach(Consumer<? super T> action, T... items)
    {
        foreach(1, action, items);
    }

    public static <T> void foreach(int threads, Consumer<? super T> action, T... items)
    {
        if (threads == 1)
        {
            for (T t : items)
            {
                action.accept(t);
            }
        }
        else
        {
            try
            {
                int nth = threads <= 0 ? Runtime.getRuntime().availableProcessors() * 2 : threads;
                ExecutorService executor = Executors.newFixedThreadPool​(Math.min(nth, items.length));
                for (T t : items)
                {
                    executor.execute(() -> action.accept(t));
                }
                executor.shutdown();
                executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            }
            catch (InterruptedException ex)
            {
                Exceptions.severe(LOG, ex);
            }
        }
    }

    
}
