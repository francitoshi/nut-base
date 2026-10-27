/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import io.github.francitoshi.nut.base.collections.chain.ChainMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ChainMapTest
{
    private static ChainMap<String> chain()
    {
        Map<String,String> high = new LinkedHashMap<>();
        high.put("a","1");
        high.put("b","2");
        Map<String,String> low = new LinkedHashMap<>();
        low.put("b","20");
        low.put("c","30");
        return new ChainMap<>(high, low);
    }

    @Test
    public void testConstructorEmpty()
    {
        assertThrows(IllegalArgumentException.class, () -> new ChainMap<String>());
    }

    @Test
    public void testConstructorNullElement()
    {
        Map<String,String> m = new HashMap<>();
        assertThrows(NullPointerException.class, () -> new ChainMap<>(m, null));
    }

    @Test
    public void testMaps()
    {
        ChainMap<String> instance = chain();

        assertEquals(2, instance.maps().size());
        assertEquals("1", instance.first().get("a"));
        assertThrows(UnsupportedOperationException.class, () -> instance.maps().clear());
    }

    @Test
    public void testGet()
    {
        ChainMap<String> instance = chain();

        assertEquals("1", instance.get("a"));
        assertEquals("2", instance.get("b"));
        assertEquals("30", instance.get("c"));
        assertNull(instance.get("z"));
        assertNull(instance.get(null));
    }

    @Test
    public void testGetOrDefault()
    {
        ChainMap<String> instance = chain();

        assertEquals("2", instance.getOrDefault("b","x"));
        assertEquals("x", instance.getOrDefault("z","x"));
    }

    @Test
    public void testContainsKey()
    {
        ChainMap<String> instance = chain();

        assertTrue(instance.containsKey("a"));
        assertTrue(instance.containsKey("b"));
        assertTrue(instance.containsKey("c"));
        assertFalse(instance.containsKey("z"));
        assertFalse(instance.containsKey(null));
    }

    @Test
    public void testContainsValue()
    {
        ChainMap<String> instance = chain();

        assertTrue(instance.containsValue("1"));
        assertTrue(instance.containsValue("30"));
        assertFalse(instance.containsValue("z"));
        assertFalse(instance.containsValue(null));
    }

    @Test
    public void testSize()
    {
        ChainMap<String> instance = chain();

        assertEquals(3, instance.size());
        assertFalse(instance.isEmpty());

        instance.clear();

        assertEquals(0, instance.size());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testPutGoesToFirstMap()
    {
        Map<String,String> high = new LinkedHashMap<>();
        Map<String,String> low = new LinkedHashMap<>();
        low.put("c","30");
        ChainMap<String> instance = new ChainMap<>(high, low);

        instance.put("c","300");

        assertEquals("300", instance.get("c"));
        assertEquals("300", high.get("c"));
        assertEquals("30", low.get("c"));
    }

    @Test
    public void testPutShadows()
    {
        Map<String,String> high = new LinkedHashMap<>();
        Map<String,String> low = new LinkedHashMap<>();
        low.put("b","20");
        ChainMap<String> instance = new ChainMap<>(high, low);

        instance.put("b","200");

        assertEquals("200", instance.get("b"));
        assertEquals("200", high.get("b"));
        assertEquals("20", low.get("b"));
    }

    @Test
    public void testPutAllGoesToFirstMap()
    {
        Map<String,String> high = new LinkedHashMap<>();
        Map<String,String> low = new LinkedHashMap<>();
        low.put("c","30");
        ChainMap<String> instance = new ChainMap<>(high, low);

        Map<String,String> extra = new LinkedHashMap<>();
        extra.put("c","300");
        extra.put("d","40");
        instance.putAll(extra);

        assertEquals("300", high.get("c"));
        assertEquals("40", high.get("d"));
        assertEquals("30", low.get("c"));
        assertFalse(low.containsKey("d"));
    }

    @Test
    public void testPutIfAbsent()
    {
        Map<String,String> high = new LinkedHashMap<>();
        Map<String,String> low = new LinkedHashMap<>();
        low.put("c","30");
        ChainMap<String> instance = new ChainMap<>(high, low);

        assertNull(instance.putIfAbsent("d","40"));
        assertEquals("40", high.get("d"));

        assertEquals("30", instance.putIfAbsent("c","300"));
        assertFalse(high.containsKey("c"));
        assertEquals("30", instance.get("c"));
    }

    @Test
    public void testComputeIfAbsent()
    {
        Map<String,String> high = new LinkedHashMap<>();
        Map<String,String> low = new LinkedHashMap<>();
        low.put("c","30");
        ChainMap<String> instance = new ChainMap<>(high, low);

        assertEquals("40", instance.computeIfAbsent("d", k -> "40"));
        assertEquals("40", high.get("d"));

        assertEquals("30", instance.computeIfAbsent("c", k -> "300"));
        assertFalse(high.containsKey("c"));
    }

    @Test
    public void testMerge()
    {
        Map<String,String> high = new LinkedHashMap<>();
        Map<String,String> low = new LinkedHashMap<>();
        low.put("b","20");
        ChainMap<String> instance = new ChainMap<>(high, low);

        assertEquals("20x", instance.merge("b","x", (v, s) -> v + s));

        assertEquals("20x", high.get("b"));
        assertEquals("20", low.get("b"));
        assertEquals("20x", instance.get("b"));
    }

    @Test
    public void testRemoveUnshadows()
    {
        ChainMap<String> instance = chain();

        assertEquals("2", instance.remove("b"));

        assertEquals("20", instance.get("b"));
        assertTrue(instance.containsKey("b"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testRemoveFromLowerMap()
    {
        ChainMap<String> instance = chain();

        assertEquals("30", instance.remove("c"));

        assertFalse(instance.containsKey("c"));
        assertNull(instance.get("c"));
        assertEquals(2, instance.size());
    }

    @Test
    public void testRemoveMissing()
    {
        ChainMap<String> instance = chain();

        assertNull(instance.remove("z"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testRemovePair()
    {
        ChainMap<String> instance = chain();

        assertTrue(instance.remove("b","2"));
        assertEquals("20", instance.get("b"));
        assertFalse(instance.first().containsKey("b"));
        assertFalse(instance.remove("b","2"));
        assertTrue(instance.remove("c","30"));
    }

    @Test
    public void testClear()
    {
        Map<String,String> high = new LinkedHashMap<>();
        high.put("a","1");
        Map<String,String> low = new LinkedHashMap<>();
        low.put("c","30");
        ChainMap<String> instance = new ChainMap<>(high, low);

        instance.clear();

        assertTrue(high.isEmpty());
        assertTrue(low.isEmpty());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testKeySet()
    {
        ChainMap<String> instance = chain();

        Set<String> keys = instance.keySet();

        assertEquals(3, keys.size());
        assertEquals("[a, b, c]", keys.toString());
        assertTrue(keys.contains("c"));
        assertFalse(keys.contains("z"));

        assertTrue(keys.remove("a"));
        assertFalse(instance.containsKey("a"));
        assertEquals(2, keys.size());

        keys.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testKeySetIteratorRemove()
    {
        ChainMap<String> instance = chain();

        Iterator<String> it = instance.keySet().iterator();
        assertEquals("a", it.next());
        it.remove();

        assertFalse(instance.containsKey("a"));
        assertEquals(2, instance.size());
    }

    @Test
    public void testKeySetRemoveAll()
    {
        ChainMap<String> instance = chain();

        Collection<String> toRemove = new ArrayList<>();
        toRemove.add("a");
        toRemove.add("c");

        assertTrue(instance.keySet().removeAll(toRemove));
        assertFalse(instance.containsKey("a"));
        assertFalse(instance.containsKey("c"));
        assertEquals("[b]", instance.keySet().toString());
    }

    @Test
    public void testValues()
    {
        ChainMap<String> instance = chain();

        Collection<String> values = instance.values();

        assertEquals(3, values.size());
        assertEquals("[1, 2, 30]", values.toString());
        assertTrue(values.contains("30"));
        assertFalse(values.contains("z"));

        assertTrue(values.remove("2"));
        assertEquals("20", instance.get("b"));

        values.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testValuesIteratorRemove()
    {
        ChainMap<String> instance = chain();

        Iterator<String> it = instance.values().iterator();
        assertEquals("1", it.next());
        it.remove();

        assertFalse(instance.containsKey("a"));
        assertEquals(2, instance.size());
    }

    @Test
    public void testEntrySet()
    {
        ChainMap<String> instance = chain();

        Set<Entry<String,String>> entries = instance.entrySet();

        assertEquals(3, entries.size());
        assertTrue(entries.contains(new ChainMapEntry("b","2")));
        assertTrue(entries.contains(new ChainMapEntry("c","30")));
        assertFalse(entries.contains(new ChainMapEntry("b","20")));
        assertFalse(entries.contains("z"));

        assertTrue(entries.remove(new ChainMapEntry("c","30")));
        assertFalse(instance.containsKey("c"));
        assertEquals(2, entries.size());

        entries.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testEntrySetIteratorRemove()
    {
        ChainMap<String> instance = chain();

        Iterator<Entry<String,String>> it = instance.entrySet().iterator();
        assertEquals("a", it.next().getKey());
        it.remove();

        assertFalse(instance.containsKey("a"));
        assertEquals(2, instance.size());
    }

    @Test
    public void testEntrySetValue()
    {
        Map<String,String> high = new LinkedHashMap<>();
        high.put("b","2");
        Map<String,String> low = new LinkedHashMap<>();
        low.put("b","20");
        low.put("c","30");
        ChainMap<String> instance = new ChainMap<>(high, low);

        Iterator<Entry<String,String>> it = instance.entrySet().iterator();
        while(it.hasNext())
        {
            Entry<String,String> e = it.next();
            if("b".equals(e.getKey()))
            {
                assertEquals("2", e.setValue("200"));
            }
        }

        for(Entry<String,String> e : instance.entrySet())
        {
            if("c".equals(e.getKey()))
            {
                assertEquals("30", e.setValue("300"));
            }
        }

        assertEquals("200", high.get("b"));
        assertEquals("20", low.get("b"));
        assertEquals("200", instance.get("b"));
        assertEquals("300", low.get("c"));
        assertEquals("300", instance.get("c"));
    }

    @Test
    public void testEntrySetIsLive()
    {
        ChainMap<String> instance = chain();

        Set<Entry<String,String>> entries = instance.entrySet();
        assertEquals(3, entries.size());

        instance.first().put("z","40");

        assertEquals(4, entries.size());
        assertTrue(entries.contains(new ChainMapEntry("z","40")));
    }

    @Test
    public void testForEachAndReplaceAll()
    {
        ChainMap<String> instance = chain();

        HashMap<String,String> seen = new HashMap<>();
        instance.forEach(seen::put);
        assertEquals(3, seen.size());
        assertEquals("2", seen.get("b"));

        instance.replaceAll((k, v) -> v + "!");
        assertEquals("1!", instance.get("a"));
        assertEquals("2!", instance.get("b"));
        assertEquals("30!", instance.get("c"));
    }

    @Test
    public void testEqualsHashCode()
    {
        ChainMap<String> instance = chain();

        Map<String,String> other = new HashMap<>();
        other.put("a","1");
        other.put("b","2");
        other.put("c","30");

        assertEquals(other, instance);
        assertEquals(instance, other);
        assertEquals(other.hashCode(), instance.hashCode());

        Map<String,String> different = new HashMap<>();
        different.put("a","1");
        assertNotEquals(different, instance);
        assertNotEquals(instance, different);
        assertNotEquals(instance, "not a map");
    }

    @Test
    public void testToString()
    {
        ChainMap<String> instance = chain();

        assertEquals("{a=1, b=2, c=30}", instance.toString());
    }

    /**
     * Minimal {@link Entry} used to compare against {@link ChainMap} entries.
     */
    private static final class ChainMapEntry implements Entry<String,String>
    {
        private final String key;
        private final String value;

        ChainMapEntry(String key, String value)
        {
            this.key = key;
            this.value = value;
        }
        @Override
        public String getKey()
        {
            return this.key;
        }
        @Override
        public String getValue()
        {
            return this.value;
        }
        @Override
        public String setValue(String value)
        {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean equals(Object o)
        {
            if(!(o instanceof Entry))
            {
                return false;
            }
            Entry<?,?> e = (Entry<?,?>) o;
            return java.util.Objects.equals(this.key, e.getKey()) && java.util.Objects.equals(this.value, e.getValue());
        }
        @Override
        public int hashCode()
        {
            return java.util.Objects.hashCode(this.key) ^ java.util.Objects.hashCode(this.value);
        }
        @Override
        public String toString()
        {
            return this.key + "=" + this.value;
        }
    }
}
