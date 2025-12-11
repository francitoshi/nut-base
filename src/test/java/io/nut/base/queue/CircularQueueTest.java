/*
 *  CircularQueueTest.java
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
package io.nut.base.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Claude Sonnet 4.5
class CircularQueueTest
{
    private CircularQueue<String> stringQueue;
    private CircularQueue<Integer> intQueue;
    private CircularQueue<Person> personQueue;

    // Clase auxiliar para tests
    static class Person
    {

        String name;
        int age;

        Person(String name, int age)
        {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            Person person = (Person) o;
            return age == person.age && name.equals(person.name);
        }

        @Override
        public String toString()
        {
            return name + "(" + age + ")";
        }
    }

    @BeforeEach
    void setUp()
    {
        stringQueue = new CircularQueue<>(5);
        intQueue = new CircularQueue<>(3);
        personQueue = new CircularQueue<>(4);
    }

    @Test
    void testConstructorInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<String>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<String>(-1));
    }

    @Test
    void testPushAndSize()
    {
        assertEquals(0, stringQueue.size());
        stringQueue.push("Hello");
        assertEquals(1, stringQueue.size());
        stringQueue.push("World");
        assertEquals(2, stringQueue.size());
    }

    @Test
    void testPushOverflow()
    {
        stringQueue.push("A");
        stringQueue.push("B");
        stringQueue.push("C");
        stringQueue.push("D");
        stringQueue.push("E");
        assertEquals(5, stringQueue.size());

        String removed = stringQueue.push("F");
        assertEquals("A", removed);
        assertEquals(5, stringQueue.size());
        assertEquals("B", stringQueue.get(0));
    }

    @Test
    void testPop()
    {
        stringQueue.push("First");
        stringQueue.push("Second");
        stringQueue.push("Third");

        assertEquals("First", stringQueue.pop());
        assertEquals(2, stringQueue.size());
        assertEquals("Second", stringQueue.pop());
        assertEquals(1, stringQueue.size());
        assertEquals("Third", stringQueue.pop());
        assertEquals(0, stringQueue.size());
    }

    @Test
    void testPopEmpty()
    {
        assertNull(stringQueue.pop());
    }

    @Test
    void testForeach()
    {
        stringQueue.push("One");
        stringQueue.push("Two");
        stringQueue.push("Three");

        List<String> values = new ArrayList<>();
        stringQueue.foreach(values::add);

        assertEquals(3, values.size());
        assertEquals("One", values.get(0));
        assertEquals("Two", values.get(1));
        assertEquals("Three", values.get(2));
    }

    @Test
    void testArray()
    {
        stringQueue.push("Alpha");
        stringQueue.push("Beta");
        stringQueue.push("Gamma");

        String[] arr = stringQueue.array(new String[0]);
        assertEquals(3, arr.length);
        assertEquals("Alpha", arr[0]);
        assertEquals("Beta", arr[1]);
        assertEquals("Gamma", arr[2]);
    }

    @Test
    void testMinMaxWithComparable()
    {
        intQueue.push(30);
        intQueue.push(10);
        intQueue.push(20);

        assertEquals(10, intQueue.min());
        assertEquals(30, intQueue.max());
    }

    @Test
    void testMinMaxWithComparator()
    {
        personQueue.push(new Person("Alice", 30));
        personQueue.push(new Person("Bob", 25));
        personQueue.push(new Person("Charlie", 35));

        Comparator<Person> ageComparator = Comparator.comparingInt(p -> p.age);

        Person youngest = personQueue.min(ageComparator);
        Person oldest = personQueue.max(ageComparator);

        assertEquals("Bob", youngest.name);
        assertEquals(25, youngest.age);
        assertEquals("Charlie", oldest.name);
        assertEquals(35, oldest.age);
    }

    @Test
    void testMinMaxEmpty()
    {
        assertNull(stringQueue.min());
        assertNull(stringQueue.max());
        assertNull(personQueue.min(Comparator.comparingInt(p -> p.age)));
        assertNull(personQueue.max(Comparator.comparingInt(p -> p.age)));
    }

    @Test
    void testGet()
    {
        stringQueue.push("A");
        stringQueue.push("B");
        stringQueue.push("C");

        assertEquals("A", stringQueue.get(0));
        assertEquals("B", stringQueue.get(1));
        assertEquals("C", stringQueue.get(2));
        assertNull(stringQueue.get(3));
        assertNull(stringQueue.get(-1));
    }

    @Test
    void testCircularBehavior()
    {
        intQueue.push(1);
        intQueue.push(2);
        intQueue.push(3);
        intQueue.push(4); // Elimina el 1
        intQueue.push(5); // Elimina el 2

        assertEquals(3, intQueue.size());
        assertEquals(3, intQueue.get(0));
        assertEquals(4, intQueue.get(1));
        assertEquals(5, intQueue.get(2));
    }

    @Test
    void testWithNullValues()
    {
        stringQueue.push("First");
        stringQueue.push(null);
        stringQueue.push("Third");

        assertEquals(3, stringQueue.size());
        assertEquals("First", stringQueue.get(0));
        assertNull(stringQueue.get(1));
        assertEquals("Third", stringQueue.get(2));
    }

    @Test
    void testStringComparison()
    {
        stringQueue.push("Zebra");
        stringQueue.push("Apple");
        stringQueue.push("Mango");

        assertEquals("Apple", stringQueue.min());
        assertEquals("Zebra", stringQueue.max());
    }

    @Test
    void testComplexObjectWithComparator()
    {
        personQueue.push(new Person("John", 40));
        personQueue.push(new Person("Jane", 35));
        personQueue.push(new Person("Jack", 28));

        // Comparador por longitud del nombre
        Comparator<Person> nameLength = Comparator.comparingInt(p -> p.name.length());

        Person shortest = personQueue.min(nameLength);
        Person longest = personQueue.max(nameLength);

        assertEquals(4, shortest.name.length()); // "John" o "Jane" o "Jack"
        assertEquals(4, longest.name.length());
    }

    @Test
    void testMultipleOverflows()
    {
        for (int i = 0; i < 20; i++)
        {
            intQueue.push(i);
        }

        assertEquals(3, intQueue.size());
        assertEquals(17, intQueue.get(0));
        assertEquals(18, intQueue.get(1));
        assertEquals(19, intQueue.get(2));
    }

    @Test
    void testForeachAfterOverflow()
    {
        stringQueue.push("A");
        stringQueue.push("B");
        stringQueue.push("C");
        stringQueue.push("D");
        stringQueue.push("E");
        stringQueue.push("F"); // Overflow
        stringQueue.push("G"); // Overflow

        List<String> result = new ArrayList<>();
        stringQueue.foreach(result::add);

        assertEquals(5, result.size());
        assertEquals("C", result.get(0));
        assertEquals("G", result.get(4));
    }
}
