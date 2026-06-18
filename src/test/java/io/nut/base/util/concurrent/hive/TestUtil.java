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

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Test-only polling helper. Some behaviors in this package are driven by
 * a real wall-clock timer (e.g. BatchBee's time-based flush) and cannot
 * be synchronized on with {@link Bee#shutdownAndAwaitTermination}, so
 * tests need to wait for a condition to become true without resorting
 * to a single arbitrary, possibly-too-short-or-too-long sleep.
 */
final class TestUtil
{
    private TestUtil()
    {
    }

    /**
     * Polls the given condition every 5ms until it becomes true or the
     * timeout elapses, returning the last observed value.
     */
    static boolean awaitTrue(BooleanSupplier condition, long timeoutMillis)
    {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline)
        {
            if (condition.getAsBoolean())
            {
                return true;
            }
            try
            {
                Thread.sleep(5);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return condition.getAsBoolean();
            }
        }
        return condition.getAsBoolean();
    }
}
