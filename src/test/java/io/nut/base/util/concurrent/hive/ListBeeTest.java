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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ListBee}: delegation to an underlying List,
 * List interface implementation, receive() add() semantics, and the
 * factory methods on {@link Hive}.
 */
class ListBeeTest
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
    void directSendAppendMessageToTheList()
    {
        List<String> underlying = new ArrayList<>();
        ListBee<String> lb = new ListBee<>(underlying);

        assertTrue(lb.send("hello"));

        assertEquals(1, lb.size());
        assertEquals("hello", lb.get(0));
    }

    @Test
    void implementsListInterface()
    {
        ListBee<Integer> lb = new ListBee<>(new ArrayList<>());

        assertTrue(lb instanceof List);
    }

    @Test
    void getIndexedAccess()
    {
        ListBee<String> lb = new ListBee<>(new ArrayList<>());

        lb.send("a");
        lb.send("b");
        lb.send("c");

        assertEquals("a", lb.get(0));
        assertEquals("b", lb.get(1));
        assertEquals("c", lb.get(2));
    }

    @Test
    void setIndexedElement()
    {
        ListBee<String> lb = new ListBee<>(new ArrayList<>());

        lb.send("a");
        lb.send("b");

        assertEquals("a", lb.set(0, "x"));
        assertEquals("x", lb.get(0));
    }

    @Test
    void addAtIndexInsertsElement()
    {
        ListBee<String> lb = new ListBee<>(new ArrayList<>());

        lb.send("a");
        lb.send("c");
        lb.add(1, "b");

        assertEquals(Arrays.asList("a", "b", "c"), lb);
    }

    @Test
    void removeAtIndexRemovesElement()
    {
        ListBee<String> lb = new ListBee<>(new ArrayList<>());

        lb.addAll(Arrays.asList("a", "b", "c"));

        assertEquals("b", lb.remove(1));
        assertEquals(Arrays.asList("a", "c"), lb);
    }

    @Test
    void indexOfAndLastIndexOf()
    {
        ListBee<String> lb = new ListBee<>(new ArrayList<>());

        lb.addAll(Arrays.asList("a", "b", "a"));

        assertEquals(0, lb.indexOf("a"));
        assertEquals(2, lb.lastIndexOf("a"));
        assertEquals(1, lb.indexOf("b"));
        assertEquals(-1, lb.indexOf("x"));
    }

    @Test
    void listIteratorAndSubList()
    {
        ListBee<String> lb = new ListBee<>(new ArrayList<>());

        lb.addAll(Arrays.asList("a", "b", "c", "d"));

        assertEquals(Arrays.asList("b", "c"), lb.subList(1, 3));

        java.util.ListIterator<String> it = lb.listIterator(1);
        assertTrue(it.hasNext());
        assertEquals("b", it.next());
    }

    @Test
    void sizeIsEmptyContainsDelegateToo()
    {
        ListBee<Integer> lb = new ListBee<>(new ArrayList<>());

        assertTrue(lb.isEmpty());
        assertEquals(0, lb.size());
        assertFalse(lb.contains(1));

        lb.send(1);

        assertFalse(lb.isEmpty());
        assertEquals(1, lb.size());
        assertTrue(lb.contains(1));
    }

    @Test
    void collectionMethodsDelegate()
    {
        ListBee<Integer> lb = new ListBee<>(new ArrayList<>());

        lb.addAll(Arrays.asList(1, 2, 3));

        assertEquals(3, lb.size());
        assertTrue(lb.containsAll(Arrays.asList(1, 2)));

        lb.removeAll(Arrays.asList(2));

        assertEquals(Arrays.asList(1, 3), lb);
    }

    @Test
    void constructorRejectsNullList()
    {
        assertThrows(NullPointerException.class, () -> new ListBee<>(null));
        assertThrows(NullPointerException.class, () -> new ListBee<>(hive, null));
        assertThrows(NullPointerException.class, () -> new ListBee<>(2, hive, null));
    }

    @Test
    void hiveListFactoryCreatesBoundListBee() throws InterruptedException
    {
        ListBee<String> lb = hive.list(new ArrayList<>());

        lb.send("test");
        Hive.shutdownAndAwaitTermination(true, true, lb);

        assertEquals(1, lb.size());
        assertEquals("test", lb.get(0));
    }

    @Test
    void hiveListFactoryWithThreadsParameter() throws InterruptedException
    {
        ListBee<String> lb = hive.list(2, new ArrayList<>());

        assertTrue(lb.send("test"));
        Hive.shutdownAndAwaitTermination(true, true, lb);

        assertEquals(1, lb.size());
    }

    @Test
    void hiveListFactoryWithQueueSizeParameter() throws InterruptedException
    {
        ListBee<String> lb = hive.list(2, 10, new ArrayList<>());

        assertTrue(lb.send("test"));
        Hive.shutdownAndAwaitTermination(true, true, lb);

        assertEquals(1, lb.size());
    }

    @Test
    void hiveListFactoryRejectsNullList()
    {
        assertThrows(NullPointerException.class, () -> hive.list(null));
        assertThrows(NullPointerException.class, () -> hive.list(2, null));
        assertThrows(NullPointerException.class, () -> hive.list(2, 10, null));
    }
}
