/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.callWithin;
import io.github.francitoshi.nut.base.commons.time.JavaTime;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A very negative timeout means "already expired". It used to overflow
 * ({@code deadline - nanoTime()} wrapped to ~{@code Long.MAX_VALUE}) and block
 * the caller for centuries.
 */
class ChannelTimeoutOverflowTest
{
    private static final long[] EXTREME_NEGATIVES = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -Long.MAX_VALUE / 2};

    @Test
    void toDeadlineOfNegativeTimeoutIsAlreadyExpired()
    {
        for (TimeUnit unit : TimeUnit.values())
        {
            for (long timeout : EXTREME_NEGATIVES)
            {
                long remaining = JavaTime.untilNanoTime(timeout, unit) - System.nanoTime();
                assertTrue(remaining <= 0, "remaining must be <= 0 for " + timeout + " " + unit + " but was " + remaining);
            }
        }
    }

    @Test
    void toDeadlineOfHugePositiveTimeoutDoesNotOverflow()
    {
        for (TimeUnit unit : TimeUnit.values())
        {
            long remaining = JavaTime.untilNanoTime(Long.MAX_VALUE, unit) - System.nanoTime();
            assertTrue(remaining > 0, "remaining must be > 0 for Long.MAX_VALUE " + unit + " but was " + remaining);
        }
    }

    @Test
    void getWithExtremelyNegativeTimeoutReturnsNullImmediately() throws Exception
    {
        List<Channel<String>> channels = new ArrayList<>();
        channels.add(new BufferedChannel<>(4));
        channels.add(new UnbufferedChannel<>());
        channels.add(new UnlimitedChannel<>());
        channels.add(new ConflatedChannel<>());
        channels.add(new CloseableBufferedChannel<>(4));
        channels.add(new CloseableUnbufferedChannel<>());
        channels.add(new CloseableUnlimitedChannel<>());
        channels.add(new CloseableConflatedChannel<>());

        for (Channel<String> ch : channels)
        {
            for (long timeout : EXTREME_NEGATIVES)
            {
                String what = ch.getClass().getSimpleName() + ".get(" + timeout + ", SECONDS)";
                String value = callWithin(what, () -> ch.get(timeout, TimeUnit.SECONDS));
                assertNull(value, what);
            }
        }
    }

    @Test
    void putWithExtremelyNegativeTimeoutOnFullChannelReturnsFalseImmediately() throws Exception
    {
        BufferedChannel<String> full = new BufferedChannel<>(1);
        full.put("x");
        CloseableBufferedChannel<String> closeableFull = new CloseableBufferedChannel<>(1);
        closeableFull.put("x");

        List<Channel<String>> channels = new ArrayList<>();
        channels.add(full);
        channels.add(closeableFull);
        channels.add(new UnbufferedChannel<>());          // nobody is reading
        channels.add(new CloseableUnbufferedChannel<>());  // nobody is reading

        for (Channel<String> ch : channels)
        {
            for (long timeout : EXTREME_NEGATIVES)
            {
                String what = ch.getClass().getSimpleName() + ".put(v, " + timeout + ", SECONDS)";
                boolean delivered = callWithin(what, () -> ch.put("y", timeout, TimeUnit.SECONDS));
                assertFalse(delivered, what);
            }
        }
    }

    @Test
    void selectGetWithExtremelyNegativeTimeoutReturnsImmediately() throws Exception
    {
        for (long timeout : EXTREME_NEGATIVES)
        {
            Object result = callWithin("selectGet(" + timeout + ", SECONDS)",
                    () -> Channel.selectGet(timeout, TimeUnit.SECONDS, new BufferedChannel<String>(1)));
            assertNull(result);
        }
    }
}
