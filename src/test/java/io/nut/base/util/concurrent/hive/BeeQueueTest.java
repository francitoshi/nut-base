/*
 *  BeeQueueTest.java
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

import io.nut.base.util.Utils;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

/**
 *
 * @author franci
 */
public class BeeQueueTest
{

    // =========================================================================
    // of(Consumer)
    // =========================================================================
    @Nested
    @DisplayName("of(Consumer)")
    class OfConsumer
    {

        @Test
        @DisplayName("returns a non-null BeeQueue instance")
        void returnsNonNull()
        {
            BeeQueue<String> q = BeeQueue.of((Consumer<String>) m ->
            {
            });
            assertNotNull(q);
        }

        @Test
        @DisplayName("returned instance implements Queue")
        void implementsQueue()
        {
            BeeQueue<String> q = BeeQueue.of((Consumer<String>) m ->
            {
            });
            assertInstanceOf(Queue.class, q);
        }

        @Test
        @DisplayName("offer() invokes the consumer with the offered element")
        void offer_invokesConsumer()
        {
            List<String> received = new CopyOnWriteArrayList<>();

            Queue<String> q = BeeQueue.of(received::add);
            q.offer("hello");

            assertEquals(Utils.listOf("hello"), received);
        }

        @Test
        @DisplayName("add() invokes the consumer via offer()")
        void add_invokesConsumer()
        {
            List<String> received = new CopyOnWriteArrayList<>();

            Queue<String> q = BeeQueue.of(received::add);
            q.add("world");

            assertEquals(Utils.listOf("world"), received);
        }

        @Test
        @DisplayName("addAll() invokes the consumer for every element in order")
        void addAll_invokesConsumerForAllElements()
        {
            List<String> received = new CopyOnWriteArrayList<>();
            List<String> batch = Arrays.asList("a", "b", "c");

            Queue<String> q = BeeQueue.of(received::add);
            q.addAll(batch);

            assertEquals(batch, received);
        }

        @Test
        @DisplayName("consumer is called once per offer()")
        void consumer_calledExactlyOncePerOffer()
        {
            AtomicInteger count = new AtomicInteger(0);

            Queue<String> q = BeeQueue.of((Consumer<String>) m -> count.incrementAndGet());
            q.offer("x");
            q.offer("y");
            q.offer("z");

            assertEquals(3, count.get());
        }

        @Test
        @DisplayName("each of(Consumer) call produces an independent instance")
        void eachCall_producesIndependentInstance()
        {
            List<String> received1 = new CopyOnWriteArrayList<>();
            List<String> received2 = new CopyOnWriteArrayList<>();

            Queue<String> q1 = BeeQueue.of(received1::add);
            Queue<String> q2 = BeeQueue.of(received2::add);

            q1.offer("for-1");
            q2.offer("for-2");

            assertEquals(Utils.listOf("for-1"), received1);
            assertEquals(Utils.listOf("for-2"), received2);
        }

        @Test
        @DisplayName("queue is permanently empty after offer()")
        void queueRemainsEmpty()
        {
            Queue<String> q = BeeQueue.of((Consumer<String>) m ->
            {
            });
            q.offer("x");

            assertTrue(q.isEmpty());
            assertEquals(0, q.size());
            assertNull(q.poll());
            assertNull(q.peek());
        }

        @Test
        @DisplayName("offer() returns false after shutdown()")
        void offer_afterShutdown_returnsFalse()
        {
            List<String> received = new CopyOnWriteArrayList<>();
            BeeQueue<String> q = BeeQueue.of(received::add);

            q.offer("before");
            q.shutdown();
            boolean accepted = q.offer("after");

            assertFalse(accepted);
            assertEquals(Utils.listOf("before"), received);
        }

        @Test
        @Timeout(10)
        @DisplayName("concurrent offer() calls all reach the consumer")
        void concurrentOffers_allReachConsumer() throws InterruptedException
        {
            final int threadCount = 8;
            final int messagesPerThread = 100;
            final int total = threadCount * messagesPerThread;

            AtomicInteger count = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(total);

            Hive hive = new Hive(4);
            BeeQueue<Integer> q = BeeQueue.of((Consumer<Integer>) m ->
            {
                count.incrementAndGet();
                latch.countDown();
            });

            List<Thread> threads = new java.util.ArrayList<>();
            for (int t = 0; t < threadCount; t++)
            {
                final int base = t * messagesPerThread;
                threads.add(new Thread(() ->
                {
                    for (int i = 0; i < messagesPerThread; i++)
                    {
                        q.offer(base + i);
                    }
                }));
            }
            threads.forEach(Thread::start);
            for (Thread t : threads)
            {
                t.join();
            }

            boolean completed = latch.await(8, TimeUnit.SECONDS);
            Bee.shutdownAndAwaitTermination(q);

            assertTrue(completed, "All messages should be processed within the timeout");
            assertEquals(total, count.get());
        }
    }

    // =========================================================================
    // of(Bee)
    // =========================================================================
    @Nested
    @DisplayName("of(Bee)")
    class OfBee
    {

        @Test
        @DisplayName("returns a non-null BeeQueue instance")
        void returnsNonNull()
        {
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                }
            };
            BeeQueue<String> q = BeeQueue.of(bee);
            assertNotNull(q);
        }

        @Test
        @DisplayName("returned instance implements Queue")
        void implementsQueue()
        {
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                }
            };
            assertInstanceOf(Queue.class, BeeQueue.of(bee));
        }

        @Test
        @DisplayName("offer() forwards the message to the delegate Bee via send()")
        void offer_forwardsMessageToBee()
        {
            List<String> received = new CopyOnWriteArrayList<>();
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    received.add(m);
                }
            };

            Queue<String> q = BeeQueue.of(bee);
            q.offer("hello");

            assertEquals(Utils.listOf("hello"), received);
        }

        @Test
        @DisplayName("add() forwards the message to the delegate Bee via offer()")
        void add_forwardsMessageToBee()
        {
            List<String> received = new CopyOnWriteArrayList<>();
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    received.add(m);
                }
            };

            BeeQueue.of(bee).add("world");

            assertEquals(Utils.listOf("world"), received);
        }

        @Test
        @DisplayName("addAll() forwards every element to the delegate Bee in order")
        void addAll_forwardsAllElementsToBee()
        {
            List<String> received = new CopyOnWriteArrayList<>();
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    received.add(m);
                }
            };
            List<String> batch = Arrays.asList("a", "b", "c");

            BeeQueue.of(bee).addAll(batch);

            assertEquals(batch, received);
        }

        @Test
        @DisplayName("delegate Bee receives exactly one message per offer()")
        void bee_receivedExactlyOncePerOffer()
        {
            AtomicInteger count = new AtomicInteger(0);
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    count.incrementAndGet();
                }
            };

            Queue<String> q = BeeQueue.of(bee);
            q.offer("x");
            q.offer("y");
            q.offer("z");

            assertEquals(3, count.get());
        }

        @Test
        @DisplayName("two queues wrapping different Bees dispatch to their own Bee independently")
        void twoQueues_dispatchToTheirOwnBee()
        {
            List<String> received1 = new CopyOnWriteArrayList<>();
            List<String> received2 = new CopyOnWriteArrayList<>();

            Bee<String> bee1 = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    received1.add(m);
                }
            };
            Bee<String> bee2 = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    received2.add(m);
                }
            };

            BeeQueue.of(bee1).offer("for-1");
            BeeQueue.of(bee2).offer("for-2");

            assertEquals(Utils.listOf("for-1"), received1);
            assertEquals(Utils.listOf("for-2"), received2);
        }

        @Test
        @DisplayName("queue is permanently empty after offer()")
        void queueRemainsEmpty()
        {
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                }
            };
            Queue<String> q = BeeQueue.of(bee);
            q.offer("x");

            assertTrue(q.isEmpty());
            assertEquals(0, q.size());
            assertNull(q.poll());
            assertNull(q.peek());
        }

        @Test
        @DisplayName("shutting down the BeeQueue does not shut down the delegate Bee")
        void shutdown_doesNotAffectDelegateBee()
        {
            List<String> received = new CopyOnWriteArrayList<>();
            Bee<String> bee = new Bee<String>()
            {
                @Override
                protected void receive(String m)
                {
                    received.add(m);
                }
            };

            BeeQueue<String> q = BeeQueue.of(bee);
            q.shutdown();

            // The delegate Bee is still alive and can receive messages directly
            assertFalse(bee.isShutdown(), "Delegate Bee should not be shut down");
            bee.send("direct");
            assertEquals(Utils.listOf("direct"), received);
        }

        @Test
        @Timeout(10)
        @DisplayName("concurrent offer() calls all reach the delegate Bee")
        void concurrentOffers_allReachDelegateBee() throws InterruptedException
        {
            final int threadCount = 8;
            final int messagesPerThread = 100;
            final int total = threadCount * messagesPerThread;

            AtomicInteger count = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(total);

            Bee<Integer> bee = new Bee<Integer>()
            {
                @Override
                protected void receive(Integer m)
                {
                    count.incrementAndGet();
                    latch.countDown();
                }
            };

            Queue<Integer> q = BeeQueue.of(bee);

            List<Thread> threads = new java.util.ArrayList<>();
            for (int t = 0; t < threadCount; t++)
            {
                final int base = t * messagesPerThread;
                threads.add(new Thread(() ->
                {
                    for (int i = 0; i < messagesPerThread; i++)
                    {
                        q.offer(base + i);
                    }
                }));
            }
            threads.forEach(Thread::start);
            for (Thread t : threads)
            {
                t.join();
            }

            boolean completed = latch.await(8, TimeUnit.SECONDS);
            Bee.shutdownAndAwaitTermination(bee);

            assertTrue(completed, "All messages should be processed within the timeout");
            assertEquals(total, count.get());
        }
    }
}
