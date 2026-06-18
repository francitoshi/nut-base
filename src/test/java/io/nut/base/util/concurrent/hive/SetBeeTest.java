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

import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SetBee}: delegation to an underlying Set,
 * Set interface implementation, receive() add() semantics, duplicate
 * rejection, and the factory methods on {@link Hive}.
 */
class SetBeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        hive.shutdown();
        hive.awaitTermination(2000);
    }

    @Test
    void directSendAddsMessageToTheSet()
    {
        Set<String> underlying = new HashSet<>();
        SetBee<String> sb = new SetBee<>(underlying);

        assertTrue(sb.send("hello"));

        assertEquals(1, sb.size());
        assertTrue(sb.contains("hello"));
    }

    @Test
    void implementsSetInterface()
    {
        SetBee<Integer> sb = new SetBee<>(new HashSet<>());

        assertTrue(sb instanceof Set);
    }

    @Test
    void duplicatesAreRejectedByTheUnderlyingSet()
    {
        SetBee<String> sb = new SetBee<>(new HashSet<>());

        sb.send("a");
        sb.send("a");
        sb.send("b");

        assertEquals(2, sb.size());
        assertTrue(sb.contains("a"));
        assertTrue(sb.contains("b"));
    }

    @Test
    void iteratorTraversesAllElements()
    {
        SetBee<String> sb = new SetBee<>(new HashSet<>());

        sb.send("a");
        sb.send("b");
        sb.send("c");

        Set<String> traversed = new HashSet<>();
        for (String s : sb)
        {
            traversed.add(s);
        }
        assertEquals(sb, traversed);
    }

    @Test
    void sizeIsEmptyContainsDelegateToo()
    {
        SetBee<Integer> sb = new SetBee<>(new HashSet<>());

        assertTrue(sb.isEmpty());
        assertEquals(0, sb.size());
        assertFalse(sb.contains(1));

        sb.send(1);

        assertFalse(sb.isEmpty());
        assertEquals(1, sb.size());
        assertTrue(sb.contains(1));
    }

    @Test
    void collectionMethodsDelegate()
    {
        SetBee<Integer> sb = new SetBee<>(new HashSet<>());

        sb.addAll(Arrays.asList(1, 2, 3));

        assertEquals(3, sb.size());
        assertTrue(sb.containsAll(Arrays.asList(1, 2)));

        sb.remove(2);

        assertEquals(2, sb.size());
        assertFalse(sb.contains(2));
    }

    @Test
    void removeAllRetainAllDelegate()
    {
        SetBee<Integer> sb = new SetBee<>(new HashSet<>());

        sb.addAll(Arrays.asList(1, 2, 3, 4, 5));

        sb.removeAll(Arrays.asList(2, 4));

        assertEquals(3, sb.size());
        assertTrue(sb.containsAll(Arrays.asList(1, 3, 5)));

        sb.retainAll(Arrays.asList(1, 3));

        assertEquals(2, sb.size());
        assertTrue(sb.contains(1));
        assertTrue(sb.contains(3));
        assertFalse(sb.contains(5));
    }

    @Test
    void clearRemovesAll()
    {
        SetBee<String> sb = new SetBee<>(new HashSet<>());

        sb.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, sb.size());

        sb.clear();

        assertEquals(0, sb.size());
        assertTrue(sb.isEmpty());
    }

    @Test
    void toArrayDelegates()
    {
        SetBee<String> sb = new SetBee<>(new HashSet<>());

        sb.addAll(Arrays.asList("a", "b", "c"));

        Object[] arr = sb.toArray();
        assertEquals(3, arr.length);

        String[] typed = sb.toArray(new String[0]);
        assertEquals(3, typed.length);
    }

    @Test
    void constructorRejectsNullSet()
    {
        assertThrows(NullPointerException.class, () -> new SetBee<>(null));
        assertThrows(NullPointerException.class, () -> new SetBee<>(hive, null));
        assertThrows(NullPointerException.class, () -> new SetBee<>(2, hive, null));
    }

    @Test
    void hiveSetFactoryCreatesBoundSetBee() throws InterruptedException
    {
        SetBee<String> sb = hive.set(new HashSet<>());

        sb.send("test");
        Hive.shutdownAndAwaitTermination(true, true, sb);

        assertEquals(1, sb.size());
        assertTrue(sb.contains("test"));
    }

    @Test
    void hiveSetFactoryWithThreadsParameter() throws InterruptedException
    {
        SetBee<String> sb = hive.set(2, new HashSet<>());

        assertTrue(sb.send("test"));
        Hive.shutdownAndAwaitTermination(true, true, sb);

        assertEquals(1, sb.size());
    }

    @Test
    void hiveSetFactoryWithQueueSizeParameter() throws InterruptedException
    {
        SetBee<String> sb = hive.set(2, 10, new HashSet<>());

        assertTrue(sb.send("test"));
        Hive.shutdownAndAwaitTermination(true, true, sb);

        assertEquals(1, sb.size());
    }

    @Test
    void hiveSetFactoryRejectsNullSet()
    {
        assertThrows(NullPointerException.class, () -> hive.set(null));
        assertThrows(NullPointerException.class, () -> hive.set(2, null));
        assertThrows(NullPointerException.class, () -> hive.set(2, 10, null));
    }
}
