/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A cache that balances recency (LRU) and frequency (LFU). Items are
 * prioritized by access count, with LRU as a tiebreaker.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class LRULFUCache<K, V> extends AbstractCache<K,V> implements Cache<K,V>
{

    private final int capacity;
    private final long ttlNanos;
    private final Map<K, Node<K, V>> cache;
    private final Map<Integer, FrequencyBucket<K, V>> frequencyMap;
    private int minFrequency;

    public LRULFUCache(int capacity, long ttlNanos)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.ttlNanos = ttlNanos;
        this.cache = new HashMap<>(capacity);
        this.frequencyMap = new HashMap<>();
        this.minFrequency = 0;
    }

    public LRULFUCache(int capacity)
    {
        this(capacity, Long.MAX_VALUE);
    }

    public LRULFUCache(int capacity, long ttl, TimeUnit timeUnit)
    {
        this(capacity, timeUnit.toNanos(ttl));
    }
    
    /**
     * Retrieves a value from the cache. Increases the frequency count for the
     * key.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if not found
     */
    @Override
    public V get(K key, Function<? super K, ? extends V> creator)
    {
        Node<K, V> node = cache.get(key);
        long now = System.nanoTime();
        if (node != null)
        {
            if (node.value != null && node.value.isExpired(now))
            {
                removeNodeCompletely(node);
            }
            else
            {
                updateFrequency(node);
                return node.value != null ? node.value.v : null;
            }
        }

        if (creator == null)
        {
            return null;
        }

        V value = creator.apply(key);
        long exp = calculateExpiration(now, ttlNanos);
        AbstractCache.Item<V> item = new AbstractCache.Item<>(value, exp);

        if (cache.size() >= capacity)
        {
            evictLFU();
        }

        Node<K, V> newNode = new Node<>(key, item);
        cache.put(key, newNode);

        // Add to frequency 1 bucket
        FrequencyBucket<K, V> bucket = frequencyMap.computeIfAbsent(1, k -> new FrequencyBucket<>());
        bucket.addToFront(newNode);
        minFrequency = 1;

        return value;
    }

    @Override
    public boolean containsKey(K key)
    {
        Node<K, V> node = cache.get(key);
        if (node == null)
        {
            return false;
        }

        if (node.value != null && node.value.isExpired(System.nanoTime()))
        {
            removeNodeCompletely(node);
            return false;
        }

        return true;
    }

    /**
     * Adds or updates a key-value pair in the cache. If the cache is at
     * capacity, evicts the least frequently used item (with LRU as tiebreaker).
     *
     * @param key the key to store
     * @param value the value to store
     */
    @Override
    public void put(K key, V value)
    {
        long now = System.nanoTime();
        long exp = calculateExpiration(now, ttlNanos);
        AbstractCache.Item<V> item = new AbstractCache.Item<>(value, exp);

        Node<K, V> node = cache.get(key);

        if (node != null)
        {
            if (node.value != null && node.value.isExpired(now))
            {
                removeNodeCompletely(node);
                node = null;
            }
        }

        if (node != null)
        {
            // Update existing node
            node.value = item;
            updateFrequency(node);
        }
        else
        {
            // Add new node
            if (cache.size() >= capacity)
            {
                evictLFU();
            }

            Node<K, V> newNode = new Node<>(key, item);
            cache.put(key, newNode);

            // Add to frequency 1 bucket
            FrequencyBucket<K, V> bucket = frequencyMap.computeIfAbsent(1, k -> new FrequencyBucket<>());
            bucket.addToFront(newNode);
            minFrequency = 1;
        }
    }

    private void removeNodeCompletely(Node<K, V> node)
    {
        cache.remove(node.key);
        FrequencyBucket<K, V> bucket = frequencyMap.get(node.frequency);
        if (bucket != null)
        {
            bucket.remove(node);
            if (bucket.isEmpty())
            {
                frequencyMap.remove(node.frequency);
                if (node.frequency == minFrequency)
                {
                    if (cache.isEmpty())
                    {
                        minFrequency = 0;
                    }
                    else
                    {
                        int nextMin = minFrequency;
                        while (!frequencyMap.containsKey(nextMin))
                        {
                            nextMin++;
                        }
                        minFrequency = nextMin;
                    }
                }
            }
        }
    }

    private void updateFrequency(Node<K, V> node)
    {
        int oldFreq = node.frequency;
        int newFreq = oldFreq + 1;

        // Remove from old frequency bucket
        FrequencyBucket<K, V> oldBucket = frequencyMap.get(oldFreq);
        oldBucket.remove(node);
        if (oldBucket.isEmpty())
        {
            frequencyMap.remove(oldFreq);
            if (oldFreq == minFrequency)
            {
                minFrequency = newFreq;
            }
        }
        // Add to new frequency bucket
        node.frequency = newFreq;
        FrequencyBucket<K, V> newBucket = frequencyMap.computeIfAbsent(newFreq, k -> new FrequencyBucket<>());
        newBucket.addToFront(node);
    }

    private void evictLFU()
    {
        // Get the least frequently used bucket
        FrequencyBucket<K, V> bucket = frequencyMap.get(minFrequency);

        // Remove least recently used from that bucket (tail)
        Node<K, V> lfu = bucket.removeLast();
        cache.remove(lfu.key);

        if (bucket.isEmpty())
        {
            frequencyMap.remove(minFrequency);
        }
    }

    @Override
    public int size()
    {
        return cache.size();
    }

    @Override
    public void clear()
    {
        cache.clear();
        frequencyMap.clear();
        minFrequency = 0;
    }

    @Override
    public void purgeExpired()
    {
        long now = System.nanoTime();
        java.util.List<Node<K, V>> nodes = new java.util.ArrayList<>(cache.values());
        for (Node<K, V> node : nodes)
        {
            if (node.value != null && node.value.isExpired(now))
            {
                removeNodeCompletely(node);
            }
        }
    }

    @Override
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    // Node class
    private static class Node<K, V>
    {

        K key;
        AbstractCache.Item<V> value;
        int frequency = 1;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, AbstractCache.Item<V> value)
        {
            this.key = key;
            this.value = value;
        }
    }

    // Frequency bucket (doubly linked list)
    private static class FrequencyBucket<K, V>
    {

        private Node<K, V> head;
        private Node<K, V> tail;

        void addToFront(Node<K, V> node)
        {
            node.prev = null;
            node.next = head;
            if (head != null)
            {
                head.prev = node;
            }
            head = node;
            if (tail == null)
            {
                tail = node;
            }
        }

        void remove(Node<K, V> node)
        {
            if (node.prev != null)
            {
                node.prev.next = node.next;
            }
            else
            {
                head = node.next;
            }

            if (node.next != null)
            {
                node.next.prev = node.prev;
            }
            else
            {
                tail = node.prev;
            }
            node.prev = null;
            node.next = null;
        }

        Node<K, V> removeLast()
        {
            if (tail == null)
            {
                return null;
            }

            Node<K, V> last = tail;
            remove(last);
            return last;
        }

        boolean isEmpty()
        {
            return head == null;
        }
    }
}
