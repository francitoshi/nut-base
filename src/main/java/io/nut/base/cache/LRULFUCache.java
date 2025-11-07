/*
 *  LRULFUCache.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.cache;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<K, Node<K, V>> cache;
    private final Map<Integer, FrequencyBucket<K, V>> frequencyMap;
    private int minFrequency;

    public LRULFUCache(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);
        this.frequencyMap = new HashMap<>();
        this.minFrequency = 0;
    }

    /**
     * Retrieves a value from the cache. Increases the frequency count for the
     * key.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if not found
     */
    @Override
    public V get(K key)
    {
        Node<K, V> node = cache.get(key);
        if (node == null)
        {
            return null;
        }

        // Increment frequency
        updateFrequency(node);
        return node.value;
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
        Node<K, V> node = cache.get(key);

        if (node != null)
        {
            // Update existing node
            node.value = value;
            updateFrequency(node);
        }
        else
        {
            // Add new node
            if (cache.size() >= capacity)
            {
                evictLFU();
            }

            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);

            // Add to frequency 1 bucket
            FrequencyBucket<K, V> bucket = frequencyMap.computeIfAbsent(1, k -> new FrequencyBucket<>());
            bucket.addToFront(newNode);
            minFrequency = 1;
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
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    // Node class
    private static class Node<K, V>
    {

        K key;
        V value;
        int frequency = 1;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value)
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
