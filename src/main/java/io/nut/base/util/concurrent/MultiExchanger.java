/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A synchronization point at which threads can pair and swap elements within
 * groups of {@code N} parties.
 *
 * <p>Each thread presents some object on entry to the {@link #exchange} method,
 * and receives a randomly assigned object from another participating thread in the group
 * upon return. A thread will never receive the exact same object it provided,
 * unless the exchange is not performed (e.g., due to interruption or timeout).</p>
 *
 * <p>Random pairing is done using {@link ThreadLocalRandom} by default, or
 * {@link SecureRandom} if constructed with the {@code secure} parameter set to {@code true}.</p>
 *
 * @param <V> the type of values that may be exchanged
 * @author franci
 * @since 1.8
 */
public final class MultiExchanger<V>
{
    private final ExchangerDelegate<V> delegate;

    /**
     * Creates a new MultiExchanger with the specified number of parties.
     * Randomness is generated using {@link ThreadLocalRandom#current()}.
     *
     * @param parties the number of threads that must participate in each exchange
     * @throws IllegalArgumentException if {@code parties < 2}
     */
    public MultiExchanger(int parties)
    {
        this(parties, false);
    }

    /**
     * Creates a new MultiExchanger with the specified number of parties and security setting.
     *
     * @param parties the number of threads that must participate in each exchange
     * @param secure if {@code true}, uses {@link SecureRandom} for randomizing the exchange;
     *               otherwise uses {@link ThreadLocalRandom#current()}
     * @throws IllegalArgumentException if {@code parties < 2}
     */
    public MultiExchanger(int parties, boolean secure)
    {
        if (parties < 2)
        {
            throw new IllegalArgumentException("Number of parties must be at least 2");
        }
        if (parties == 2)
        {
            this.delegate = new JdkExchangerDelegate<>();
        }
        else
        {
            this.delegate = new NPartyExchangerDelegate<>(parties, secure);
        }
    }

    /**
     * Waits for all {@code N} parties to arrive at this exchange point,
     * and then performs a random exchange of values.
     *
     * <p>If the current thread is interrupted while waiting, then {@link InterruptedException}
     * is thrown and the current thread's interrupted status is cleared.</p>
     *
     * @param x the value to exchange
     * @return the value provided by another thread in the group
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public V exchange(V x) throws InterruptedException
    {
        return delegate.exchange(x);
    }

    /**
     * Waits for all {@code N} parties to arrive at this exchange point (unless the
     * specified waiting time elapses), and then performs a random exchange of values.
     *
     * <p>If the current thread is interrupted while waiting, then {@link InterruptedException}
     * is thrown and the current thread's interrupted status is cleared.</p>
     *
     * <p>If the specified waiting time elapses, then {@link TimeoutException} is thrown.</p>
     *
     * @param x the value to exchange
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return the value provided by another thread in the group
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if the specified waiting time elapses before all parties arrive
     * @throws NullPointerException if {@code unit} is null
     */
    public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
    {
        if (unit == null)
        {
            throw new NullPointerException("TimeUnit must not be null");
        }
        return delegate.exchange(x, timeout, unit);
    }

    private interface ExchangerDelegate<V>
    {
        V exchange(V x) throws InterruptedException;
        V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
    }

    private static final class JdkExchangerDelegate<V> implements ExchangerDelegate<V>
    {
        private final java.util.concurrent.Exchanger<V> exchanger = new java.util.concurrent.Exchanger<>();

        @Override
        public V exchange(V x) throws InterruptedException
        {
            return exchanger.exchange(x);
        }

        @Override
        public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
        {
            return exchanger.exchange(x, timeout, unit);
        }
    }

    private static final class NPartyExchangerDelegate<V> implements ExchangerDelegate<V>
    {
        private final int parties;
        private final boolean secure;
        private final SecureRandom secureRandom;
        private final ReentrantLock lock = new ReentrantLock();
        private Generation<V> currentGeneration;

        NPartyExchangerDelegate(int parties, boolean secure)
        {
            this.parties = parties;
            this.secure = secure;
            this.secureRandom = secure ? new SecureRandom() : null;
            this.currentGeneration = new Generation<>(lock);
        }

        @Override
        public V exchange(V x) throws InterruptedException
        {
            try
            {
                return doExchange(x, false, 0L);
            }
            catch (TimeoutException e)
            {
                throw new AssertionError(e);
            }
        }

        @Override
        public V exchange(V x, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
        {
            return doExchange(x, true, unit.toNanos(timeout));
        }

        private V doExchange(V x, boolean timed, long nanos) throws InterruptedException, TimeoutException
        {
            final Thread currentThread = Thread.currentThread();
            final Node<V> node = new Node<>(currentThread, x);
            Generation<V> gen;

            lock.lock();
            try
            {
                gen = currentGeneration;
                gen.nodes.add(node);

                if (gen.nodes.size() == parties)
                {
                    performExchange(gen);
                    currentGeneration = new Generation<>(lock);
                    gen.completed = true;
                    gen.condition.signalAll();
                    return node.exchangedValue;
                }

                boolean interrupted = false;
                try
                {
                    while (true)
                    {
                        if (node.hasExchanged)
                        {
                            return node.exchangedValue;
                        }

                        if (Thread.interrupted())
                        {
                            interrupted = true;
                            if (node.hasExchanged)
                            {
                                break;
                            }
                            handleCancellation(gen, node);
                            throw new InterruptedException();
                        }

                        if (timed)
                        {
                            if (nanos <= 0L)
                            {
                                if (node.hasExchanged)
                                {
                                    break;
                                }
                                handleCancellation(gen, node);
                                throw new TimeoutException();
                            }
                            try
                            {
                                nanos = gen.condition.awaitNanos(nanos);
                            }
                            catch (InterruptedException e)
                            {
                                interrupted = true;
                                if (node.hasExchanged)
                                {
                                    break;
                                }
                                handleCancellation(gen, node);
                                throw e;
                            }
                        }
                        else
                        {
                            try
                            {
                                gen.condition.await();
                            }
                            catch (InterruptedException e)
                            {
                                interrupted = true;
                                if (node.hasExchanged)
                                {
                                    break;
                                }
                                handleCancellation(gen, node);
                                throw e;
                            }
                        }
                    }
                    return node.exchangedValue;
                }
                finally
                {
                    if (interrupted)
                    {
                        currentThread.interrupt();
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }

        private void handleCancellation(Generation<V> gen, Node<V> node)
        {
            if (!node.hasExchanged)
            {
                gen.nodes.remove(node);
                node.exchangedValue = node.offeredValue;
            }
        }

        private void performExchange(Generation<V> gen)
        {
            int n = gen.nodes.size();
            int[] indices = new int[n];
            for (int i = 0; i < n; i++)
            {
                indices[i] = i;
            }

            Random r = secure ? secureRandom : ThreadLocalRandom.current();

            boolean isDerangement = false;
            while (!isDerangement)
            {
                for (int i = n - 1; i > 0; i--)
                {
                    int index = r.nextInt(i + 1);
                    int temp = indices[i];
                    indices[i] = indices[index];
                    indices[index] = temp;
                }

                isDerangement = true;
                for (int i = 0; i < n; i++)
                {
                    if (indices[i] == i)
                    {
                        isDerangement = false;
                        break;
                    }
                }
            }

            List<V> offeredValues = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
            {
                offeredValues.add(gen.nodes.get(i).offeredValue);
            }

            for (int i = 0; i < n; i++)
            {
                Node<V> node = gen.nodes.get(i);
                node.exchangedValue = offeredValues.get(indices[i]);
                node.hasExchanged = true;
            }
        }
    }

    private static final class Node<V>
    {
        final Thread thread;
        final V offeredValue;
        V exchangedValue;
        boolean hasExchanged = false;

        Node(Thread thread, V offeredValue)
        {
            this.thread = thread;
            this.offeredValue = offeredValue;
            this.exchangedValue = offeredValue;
        }
    }

    private static final class Generation<V>
    {
        final List<Node<V>> nodes = new ArrayList<>();
        final Condition condition;
        boolean completed = false;

        Generation(ReentrantLock lock)
        {
            this.condition = lock.newCondition();
        }
    }
}
