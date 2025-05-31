/*
 *  As.java
 *
 *  Copyright (c) 2024-2025 francitoshi@gmail.com
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
package io.nut.base.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 * @author franci
 */
public class As
{
    public static <K,V> Map<K,V> map(K[] keys, V[] values)
    {
        assert keys.length>=values.length;

        HashMap<K,V> map = new HashMap<>();

        for(int i=0;i<values.length;i++)
        {
            map.put(keys[i], values[i]);
        }
        for(int i=values.length;i<keys.length;i++)
        {
            map.put(keys[i], null);
        }
        return map;
    }
    
    public static <K,V> Map<K,V> map(K k1, V v1)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        return map;
    }
    public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9)
    {
        Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);
        return map;
    }

    public static <T> List<T> list(T... items)
    {
        return Arrays.asList(items);
    }
    
    public static <T> Queue<T> queue(T... items)
    {
        return new ArrayDeque<>(Arrays.asList(items));
    }
    
    public static <T> Deque<T> deque(T... items)
    {
        return new ArrayDeque<>(Arrays.asList(items));
    }
    
    public static <T> BlockingQueue<T> blockingQueue(T... items)
    {
        return new ArrayBlockingQueue<>(items.length, true, Arrays.asList(items));
    }

    public static <T> BlockingDeque<T> blockingDeque(T... items)
    {
        return new LinkedBlockingDeque<>(Arrays.asList(items));
    }

    public static <T> Set<T> set(T... items)
    {
        return new HashSet<>(Arrays.asList(items));
    }

}
