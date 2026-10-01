/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Adaptive Replacement Cache (ARC) implementation.
 * ARC dynamically tunes between recency (LRU) and frequency (LFU) using
 * self-tuning parameters based on history hit feedback.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class ARCCache<K, V> extends AbstractCache<K, V> implements Cache<K, V>
{

    private final int capacity;
    private final long ttlNanos;
    private double p; // Target size for the recency list T1
    private final Map<K, Node<K, V>> map;

    private final DoublyLinkedList<K, V> t1;
    private final DoublyLinkedList<K, V> t2;
    private final DoublyLinkedList<K, V> b1;
    private final DoublyLinkedList<K, V> b2;

    public ARCCache(int capacity)
    {
        this(capacity, Long.MAX_VALUE);
    }

    public ARCCache(int capacity, long ttlNanos)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.ttlNanos = ttlNanos;
        this.p = 0.0;
        this.map = new HashMap<>((int) Math.ceil((2.0 * capacity) / 0.75f) + 1);
        this.t1 = new DoublyLinkedList<>();
        this.t2 = new DoublyLinkedList<>();
        this.b1 = new DoublyLinkedList<>();
        this.b2 = new DoublyLinkedList<>();
    }
    
    public ARCCache(int capacity, long ttl, TimeUnit timeUnit)
    {
        this(capacity, timeUnit.toNanos(ttl));
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> creator)
    {
        Node<K, V> node = map.get(key);
        long now = System.nanoTime();

        if (node != null)
        {
            if ((node.type == ListType.T1 || node.type == ListType.T2) && node.value != null && node.value.isExpired(now))
            {
                removeNodeCompletely(node);
                node = null;
            }
        }

        if (node != null)
        {
            if (node.type == ListType.T1 || node.type == ListType.T2)
            {
                // Case 1: Cache hit. Move node to the MRU (Most Recently Used) position of T2.
                moveToFront(t2, node, ListType.T2);
                return node.value != null ? node.value.v : null;
            }

            // Miss in the cache, but present in history lists B1 or B2 (ghost cache hit)
            if (creator == null)
            {
                return null;
            }

            // Case 2 & 3: Hit in B1 or B2. Adapt and readmit.
            V value = creator.apply(key);
            long exp = calculateExpiration(now, ttlNanos);
            AbstractCache.Item<V> item = new AbstractCache.Item<>(value, exp);

            if (node.type == ListType.B1)
            {
                double delta = b1.size() >= b2.size() ? 1.0 : (double) b2.size() / b1.size();
                p = Math.min(p + delta, capacity);
                replace(false, p);
            }
            else // B2
            {
                double delta = b2.size() >= b1.size() ? 1.0 : (double) b1.size() / b2.size();
                p = Math.max(p - delta, 0.0);
                replace(true, p);
            }
            node.value = item;
            moveToFront(t2, node, ListType.T2);
            return value;
        }

        // Case 4: Cache miss (completely new item)
        if (creator == null)
        {
            return null;
        }

        V value = creator.apply(key);
        long exp = calculateExpiration(now, ttlNanos);
        AbstractCache.Item<V> item = new AbstractCache.Item<>(value, exp);

        int l1Size = t1.size() + b1.size();
        int totalSize = l1Size + t2.size() + b2.size();

        if (l1Size == capacity)
        {
            if (t1.size() < capacity)
            {
                Node<K, V> lruB1 = b1.removeLast();
                if (lruB1 != null)
                {
                    map.remove(lruB1.key);
                }
                replace(false, p);
            }
            else
            {
                Node<K, V> lruT1 = t1.removeLast();
                if (lruT1 != null)
                {
                    map.remove(lruT1.key);
                }
            }
        }
        else if (l1Size < capacity)
        {
            if (totalSize >= capacity)
            {
                if (totalSize == 2 * capacity)
                {
                    Node<K, V> lruB2 = b2.removeLast();
                    if (lruB2 != null)
                    {
                        map.remove(lruB2.key);
                    }
                }
                replace(false, p);
            }
        }

        Node<K, V> newNode = new Node<>(key, item, ListType.T1);
        t1.addToFront(newNode);
        map.put(key, newNode);

        return value;
    }

    @Override
    public boolean containsKey(K key)
    {
        Node<K, V> node = map.get(key);
        if (node == null)
        {
            return false;
        }

        if (node.type == ListType.T1 || node.type == ListType.T2)
        {
            if (node.value != null && node.value.isExpired(System.nanoTime()))
            {
                removeNodeCompletely(node);
                return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public void put(K key, V value)
    {
        long now = System.nanoTime();
        long exp = calculateExpiration(now, ttlNanos);
        AbstractCache.Item<V> item = new AbstractCache.Item<>(value, exp);

        Node<K, V> node = map.get(key);
        if (node != null)
        {
            if ((node.type == ListType.T1 || node.type == ListType.T2) && node.value != null && node.value.isExpired(now))
            {
                removeNodeCompletely(node);
                node = null;
            }
        }

        if (node != null)
        {
            if (node.type == ListType.T1 || node.type == ListType.T2)
            {
                // Case 1: Cache hit (update value and move to MRU of T2)
                node.value = item;
                moveToFront(t2, node, ListType.T2);
                return;
            }

            if (node.type == ListType.B1)
            {
                // Case 2: Hit in B1 (ghost hit in recency history)
                // Adapt target p to favor recency: p = min(p + delta, c)
                // delta is 1 if |B1| >= |B2| else |B2| / |B1|
                double delta = b1.size() >= b2.size() ? 1.0 : (double) b2.size() / b1.size();
                p = Math.min(p + delta, capacity);
                replace(false, p);
                node.value = item;
                moveToFront(t2, node, ListType.T2);
                return;
            }
            else if (node.type == ListType.B2)
            {
                // Case 3: Hit in B2 (ghost hit in frequency history)
                // Adapt target p to favor frequency: p = max(p - delta, 0)
                // delta is 1 if |B2| >= |B1| else |B1| / |B2|
                double delta = b2.size() >= b1.size() ? 1.0 : (double) b1.size() / b2.size();
                p = Math.max(p - delta, 0.0);
                replace(true, p);
                node.value = item;
                moveToFront(t2, node, ListType.T2);
                return;
            }
        }

        // Case 4: Cache miss (completely new item)
        int l1Size = t1.size() + b1.size();
        int totalSize = l1Size + t2.size() + b2.size();

        if (l1Size == capacity)
        {
            if (t1.size() < capacity)
            {
                Node<K, V> lruB1 = b1.removeLast();
                if (lruB1 != null)
                {
                    map.remove(lruB1.key);
                }
                replace(false, p);
            }
            else
            {
                Node<K, V> lruT1 = t1.removeLast();
                if (lruT1 != null)
                {
                    map.remove(lruT1.key);
                }
            }
        }
        else if (l1Size < capacity)
        {
            if (totalSize >= capacity)
            {
                if (totalSize == 2 * capacity)
                {
                    Node<K, V> lruB2 = b2.removeLast();
                    if (lruB2 != null)
                    {
                        map.remove(lruB2.key);
                    }
                }
                replace(false, p);
            }
        }

        Node<K, V> newNode = new Node<>(key, item, ListType.T1);
        t1.addToFront(newNode);
        map.put(key, newNode);
    }

    private void replace(boolean isB2Hit, double targetP)
    {
        if (!t1.isEmpty() && ((double) t1.size() > targetP || (isB2Hit && (double) t1.size() == targetP)))
        {
            Node<K, V> lru = t1.removeLast();
            if (lru != null)
            {
                lru.value = null; // Evict value to free memory
                lru.type = ListType.B1;
                b1.addToFront(lru);
            }
        }
        else
        {
            Node<K, V> lru = t2.removeLast();
            if (lru != null)
            {
                lru.value = null; // Evict value to free memory
                lru.type = ListType.B2;
                b2.addToFront(lru);
            }
        }
    }

    private void removeNodeCompletely(Node<K, V> node)
    {
        switch (node.type)
        {
            case T1: t1.remove(node); break;
            case T2: t2.remove(node); break;
            case B1: b1.remove(node); break;
            case B2: b2.remove(node); break;
        }
        map.remove(node.key);
    }

    private void moveToFront(DoublyLinkedList<K, V> list, Node<K, V> node, ListType newType)
    {
        switch (node.type)
        {
            case T1: t1.remove(node); break;
            case T2: t2.remove(node); break;
            case B1: b1.remove(node); break;
            case B2: b2.remove(node); break;
        }
        node.type = newType;
        list.addToFront(node);
    }

    @Override
    public int size()
    {
        return t1.size() + t2.size();
    }

    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public void clear()
    {
        t1.clear();
        t2.clear();
        b1.clear();
        b2.clear();
        map.clear();
        p = 0.0;
    }

    @Override
    public void purgeExpired()
    {
        long now = System.nanoTime();
        purgeList(t1, now);
        purgeList(t2, now);
    }

    private void purgeList(DoublyLinkedList<K, V> list, long now)
    {
        Node<K, V> curr = list.head;
        while (curr != null)
        {
            Node<K, V> next = curr.next;
            if (curr.value != null && curr.value.isExpired(now))
            {
                removeNodeCompletely(curr);
            }
            curr = next;
        }
    }

    // Node state representation
    private enum ListType
    {
        T1, T2, B1, B2
    }

    private static class Node<K, V>
    {
        K key;
        AbstractCache.Item<V> value;
        ListType type;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, AbstractCache.Item<V> value, ListType type)
        {
            this.key = key;
            this.value = value;
            this.type = type;
        }
    }

    // O(1) Doubly Linked List implementation
    private static class DoublyLinkedList<K, V>
    {
        private Node<K, V> head;
        private Node<K, V> tail;
        private int size;

        int size()
        {
            return size;
        }

        boolean isEmpty()
        {
            return size == 0;
        }

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
            size++;
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
            size--;
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

        void clear()
        {
            head = null;
            tail = null;
            size = 0;
        }
    }
}
