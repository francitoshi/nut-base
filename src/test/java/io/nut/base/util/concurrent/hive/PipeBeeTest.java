/*
 *  PipeBeeTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import io.nut.base.util.As;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class PipeBeeTest
{
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Collects all messages it receives into a thread-safe list. */
    private static <T> Bee<T> sink(List<T> collector)
    {
        return Bee.bee(item -> collector.add(item));
    }

    /** Collects all messages it receives into a thread-safe list, backed by a Hive. */
    private static <T> Bee<T> sink(Hive hive, List<T> collector)
    {
        return Bee.bee(1, hive, item -> collector.add(item));
    }

    
    @Nested
    @DisplayName("PipeBee")
    class PipeBeeTests
    {
        @Test
        @DisplayName("process result is forwarded automatically")
        void processResultForwarded()
        {
            List<Integer> received = new ArrayList<>();
            PipeBee<String, Integer> pipe = new PipeBee<String, Integer>()
            {
                @Override
                protected Integer process(String s)
                {
                    return s.length();
                }
            };
            pipe.setOut(sink(received));

            pipe.send("hello");
            pipe.send("hi");
            pipe.shutdown();
            pipe.awaitTermination(2000);

            assertEquals(As.list(5, 2), received);
        }

        @Test
        @DisplayName("pipe(Function) factory creates a working PipeBee")
        void pipeFactoryNoHive()
        {
            List<String> received = new ArrayList<>();
            PipeBee<Integer, String> pipe = PipeBee.pipe(n -> "num:" + n);
            pipe.setOut(sink(received));

            pipe.send(1);
            pipe.send(42);
            pipe.shutdown();
            pipe.awaitTermination(2000);

            assertEquals(As.list("num:1", "num:42"), received);
        }

        @Test
        @DisplayName("pipe(Hive, Function) factory uses the supplied Hive")
        void pipeFactoryWithHive() throws InterruptedException
        {
            Hive hive = new Hive(2);
            List<String> received = new CopyOnWriteArrayList<>();

            PipeBee<Integer, String> pipe = PipeBee.pipe(hive, n -> "v" + n);
            Bee<String> downstream = sink(hive, received);
            pipe.setOut(downstream);

            for (int i = 0; i < 20; i++)
            {
                pipe.send(i);
            }
            Bee.shutdownAndAwaitTermination(pipe, downstream);

            assertEquals(20, received.size());
            assertTrue(received.contains("v0"));
            assertTrue(received.contains("v19"));
        }

        @Test
        @DisplayName("pipe(int, Hive, Function) factory respects thread count")
        void pipeFactoryFullParams() throws InterruptedException
        {
            Hive hive = new Hive(4);
            AtomicInteger maxConcurrent = new AtomicInteger(0);
            AtomicInteger current = new AtomicInteger(0);
            List<Integer> received = new CopyOnWriteArrayList<>();
            CountDownLatch startLatch = new CountDownLatch(1);

            PipeBee<Integer, Integer> pipe = PipeBee.pipe(2, hive, n ->
            {
                int c = current.incrementAndGet();
                maxConcurrent.getAndUpdate(prev -> Math.max(prev, c));
                try { startLatch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
                current.decrementAndGet();
                return n * 2;
            });

            Bee<Integer> downstream = sink(hive, received);
            pipe.setOut(downstream);

            // Send tasks while they'll block on the latch
            for (int i = 0; i < 6; i++) pipe.send(i);

            startLatch.countDown(); // release all workers

            Bee.shutdownAndAwaitTermination(pipe, downstream);

            assertEquals(6, received.size());
            assertTrue(maxConcurrent.get() <= 2,
                    "Thread count should not exceed 2, was " + maxConcurrent.get());
        }

        @Test
        @DisplayName("multi-stage pipeline: String → Integer → String")
        void multiStagePipeline()
        {
            List<String> finalOutput = new ArrayList<>();

            // Stage 1: String → length (Integer)
            PipeBee<String, Integer> stage1 = PipeBee.pipe(String::length);

            // Stage 2: Integer → formatted String
            PipeBee<Integer, String> stage2 = PipeBee.pipe(n -> "[" + n + "]");

            stage1.setOut(stage2);
            stage2.setOut(sink(finalOutput));

            stage1.send("hello");
            stage1.send("hi");
            stage1.send("hey there");

            Bee.shutdownAndAwaitTermination(stage1, stage2);

            assertEquals(As.list("[5]", "[2]", "[9]"), finalOutput);
        }

        @Test
        @DisplayName("process is called exactly once per message")
        void processCalledOncePerMessage()
        {
            AtomicInteger callCount = new AtomicInteger(0);
            List<String> received = new ArrayList<>();

            PipeBee<String, String> pipe = new PipeBee<String, String>()
            {
                @Override
                protected String process(String s)
                {
                    callCount.incrementAndGet();
                    return s.toUpperCase();
                }
            };
            pipe.setOut(sink(received));

            int n = 10;
            for (int i = 0; i < n; i++) pipe.send("msg" + i);
            pipe.shutdown();
            pipe.awaitTermination(2000);

            assertEquals(n, callCount.get(), "process should be called exactly once per message");
            assertEquals(n, received.size());
        }

        @Test
        @DisplayName("identity pipe forwards messages unchanged")
        void identityPipe()
        {
            List<String> received = new ArrayList<>();
            PipeBee<String, String> pipe = PipeBee.pipe(Function.identity());
            pipe.setOut(sink(received));

            pipe.send("alpha");
            pipe.send("beta");
            pipe.shutdown();
            pipe.awaitTermination(2000);

            assertEquals(As.list("alpha", "beta"), received);
        }

        @Test
        @DisplayName("async: large message volume processed without loss")
        void asyncLargeVolume() throws InterruptedException
        {
            Hive hive = new Hive(4);
            List<Integer> received = new CopyOnWriteArrayList<>();

            PipeBee<Integer, Integer> pipe = PipeBee.pipe(hive, n -> n * n);
            Bee<Integer> downstream = sink(hive, received);
            pipe.setOut(downstream);

            int total = 200;
            for (int i = 1; i <= total; i++) pipe.send(i);

            Bee.shutdownAndAwaitTermination(pipe, downstream);

            assertEquals(total, received.size(), "All messages must be processed");
            assertTrue(received.contains(1));      // 1²
            assertTrue(received.contains(10000));  // 100²
            assertTrue(received.contains(40000));  // 200²
        }
    }
}
