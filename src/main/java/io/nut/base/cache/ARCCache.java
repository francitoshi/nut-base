/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.HashMap;
import java.util.Map;

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
    private double p; // Target size for the recency list T1
    private final Map<K, Node<K, V>> map;

    private final DoublyLinkedList<K, V> t1;
    private final DoublyLinkedList<K, V> t2;
    private final DoublyLinkedList<K, V> b1;
    private final DoublyLinkedList<K, V> b2;

    public ARCCache(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.p = 0.0;
        this.map = new HashMap<>((int) Math.ceil((2.0 * capacity) / 0.75f) + 1);
        this.t1 = new DoublyLinkedList<>();
        this.t2 = new DoublyLinkedList<>();
        this.b1 = new DoublyLinkedList<>();
        this.b2 = new DoublyLinkedList<>();
    }

    @Override
    public V get(K key)
    {
        Node<K, V> node = map.get(key);
        if (node == null)
        {
            return null;
        }

        if (node.type == ListType.T1 || node.type == ListType.T2)
        {
            // Case 1: Cache hit. Move node to the MRU (Most Recently Used) position of T2.
            moveToFront(t2, node, ListType.T2);
            return node.value;
        }

        // Miss in the cache, but present in history lists B1 or B2 (ghost cache hit)
        return null;
    }

    @Override
    public void put(K key, V value)
    {
        Node<K, V> node = map.get(key);
        if (node != null)
        {
            if (node.type == ListType.T1 || node.type == ListType.T2)
            {
                // Case 1: Cache hit (update value and move to MRU of T2)
                node.value = value;
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
                node.value = value;
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
                node.value = value;
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

        Node<K, V> newNode = new Node<>(key, value, ListType.T1);
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

    // Node state representation
    private enum ListType
    {
        T1, T2, B1, B2
    }

    private static class Node<K, V>
    {
        K key;
        V value;
        ListType type;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value, ListType type)
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
