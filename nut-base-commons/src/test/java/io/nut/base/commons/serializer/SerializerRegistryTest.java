/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.serializer;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SerializerRegistryTest
{

    private static final Serializer<String> STRING_SERIALIZER = new Serializer<String>()
    {
        @Override
        public byte[] serialize(String object)
        {
            return object.getBytes();
        }

        @Override
        public String deserialize(byte[] data)
        {
            return new String(data);
        }
    };

    private static final Serializer<Integer> INTEGER_SERIALIZER = new Serializer<Integer>()
    {
        @Override
        public byte[] serialize(Integer object)
        {
            return new byte[] { object.byteValue() };
        }

        @Override
        public Integer deserialize(byte[] data)
        {
            return (int) data[0];
        }
    };

    private static final Serializer<Shape> SHAPE_SERIALIZER = new Serializer<Shape>()
    {
        @Override
        public byte[] serialize(Shape object)
        {
            return new byte[0];
        }

        @Override
        public Shape deserialize(byte[] data)
        {
            return new Circle();
        }
    };

    @Test
    public void testRegisterAndLookupByType()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        assertTrue(registry.isRegistered(String.class));
        assertTrue(registry.isRegistered(1));
        assertSame(STRING_SERIALIZER, registry.get(String.class));
        assertEquals(1, registry.getId(String.class));
        assertEquals(1, registry.size());
    }

    @Test
    public void testRegisterAndLookupById()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        assertSame(STRING_SERIALIZER, registry.getById(1));
        SerializerRegistry.Item<?> item = registry.getItemById(1);
        assertEquals(String.class, item.getType());
        assertEquals(1, item.getId());
        assertSame(STRING_SERIALIZER, item.getSerializer());
    }

    @Test
    public void testItemKeepsTypeIdAndSerializer()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 7, STRING_SERIALIZER);
        SerializerRegistry.Item<String> item = registry.getItem(String.class);
        assertEquals(String.class, item.getType());
        assertEquals(7, item.getId());
        assertSame(STRING_SERIALIZER, item.getSerializer());
    }

    @Test
    public void testTypeCannotBeRegisteredTwice()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(String.class, 2, STRING_SERIALIZER));
        assertEquals(1, registry.size());
        assertEquals(1, registry.getId(String.class));
    }

    @Test
    public void testIdCannotBeRegisteredTwice()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(Integer.class, 1, INTEGER_SERIALIZER));
        assertEquals(1, registry.size());
        assertFalse(registry.isRegistered(Integer.class));
    }

    @Test
    public void testRegisterIfAbsent()
    {
        SerializerRegistry registry = new SerializerRegistry();
        assertTrue(registry.registerIfAbsent(String.class, 1, STRING_SERIALIZER));
        assertFalse(registry.registerIfAbsent(String.class, 2, STRING_SERIALIZER));
        assertFalse(registry.registerIfAbsent(Integer.class, 1, INTEGER_SERIALIZER));
        assertEquals(1, registry.size());
        assertEquals(1, registry.getId(String.class));
        assertFalse(registry.isRegistered(Integer.class));
        assertFalse(registry.isRegistered(2));
    }

    @Test
    public void testUnregisterByTypeRemovesBothMaps()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.unregister(String.class);
        assertFalse(registry.isRegistered(String.class));
        assertFalse(registry.isRegistered(1));
        assertEquals(0, registry.size());
    }

    @Test
    public void testUnregisterByIdRemovesBothMaps()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.unregister(1);
        assertFalse(registry.isRegistered(String.class));
        assertFalse(registry.isRegistered(1));
        assertEquals(0, registry.size());
    }

    @Test
    public void testIdCanBeReusedAfterUnregister()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.unregister(String.class);
        assertTrue(registry.registerIfAbsent(Integer.class, 1, INTEGER_SERIALIZER));
        assertEquals(1, registry.getId(Integer.class));
    }

    @Test
    public void testNotRegisteredLookups()
    {
        SerializerRegistry registry = new SerializerRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.get(String.class));
        assertThrows(IllegalArgumentException.class, () -> registry.getById(1));
        assertThrows(IllegalArgumentException.class, () -> registry.getItem(String.class));
        assertThrows(IllegalArgumentException.class, () -> registry.getItemById(1));
        assertThrows(IllegalArgumentException.class, () -> registry.getId(String.class));
    }

    @Test
    public void testGetForInstanceOf()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(Shape.class, 1, SHAPE_SERIALIZER);
        assertNotNull(registry.getForInstanceOf(Shape.class));
        assertNotNull(registry.getForInstanceOf(Circle.class));
        assertNotNull(registry.findItemForInstanceOf(Circle.class));
        assertEquals(Shape.class, registry.findItemForInstanceOf(Circle.class).getType());
    }

    @Test
    public void testItemsTypesAndIds()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.register(Integer.class, 2, INTEGER_SERIALIZER);
        assertEquals(2, registry.items().size());
        assertEquals(2, registry.types().size());
        assertEquals(2, registry.ids().size());
        assertTrue(registry.ids().contains(1));
        assertTrue(registry.ids().contains(2));
        List<SerializerRegistry.Item<?>> items = new ArrayList<>(registry.items());
        assertEquals(2, items.size());
    }

    @Test
    public void testClear()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.clear();
        assertEquals(0, registry.size());
        assertTrue(registry.ids().isEmpty());
        assertTrue(registry.types().isEmpty());
    }

    @Test
    public void testNullArguments()
    {
        SerializerRegistry registry = new SerializerRegistry();
        assertThrows(NullPointerException.class, () -> registry.register(null, 1, STRING_SERIALIZER));
        assertThrows(NullPointerException.class, () -> registry.register(String.class, 1, null));
        assertThrows(NullPointerException.class, () -> registry.registerIfAbsent(null, 1, STRING_SERIALIZER));
        assertThrows(NullPointerException.class, () -> registry.registerIfAbsent(String.class, 1, null));
    }

    @Test
    public void testSerializeAndDeserializeObject()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.register(Integer.class, 2, INTEGER_SERIALIZER);

        byte[] data = registry.serializeObject("hola");
        assertEquals(1, data[0]);
        assertEquals(4, data[1]);
        assertEquals("hola", registry.deserializeObject(data));

        data = registry.serializeObject(7);
        assertEquals(2, data[0]);
        assertEquals(1, data[1]);
        assertEquals(7, registry.deserializeObject(data));
    }

    @Test
    public void testObjectSerializerUsesTheRegistry()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        Serializer<Object> object = registry.object();

        byte[] data = object.serialize("hola");
        assertEquals(1, data[0]);
        assertEquals("hola", object.deserialize(data));
    }

    @Test
    public void testObjectSerializerWithoutSerializerForTheType()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        Serializer<Object> object = registry.object();

        assertThrows(IllegalArgumentException.class, () -> object.serialize(new Circle()));
        assertThrows(IllegalArgumentException.class, () -> registry.serializeObject(new Circle()));
    }

    @Test
    public void testObjectSerializerDoesNotRecurse()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);
        registry.register(Object.class, 2, registry.object());

        assertThrows(IllegalArgumentException.class, () -> registry.serializeObject(new Circle()));
    }

    @Test
    public void testObjectSerializerWithInvalidData()
    {
        SerializerRegistry registry = new SerializerRegistry();
        registry.register(String.class, 1, STRING_SERIALIZER);

        assertThrows(IllegalArgumentException.class, () -> registry.deserializeObject(new byte[] { 1, 2 }));
        assertThrows(IllegalArgumentException.class, () -> registry.deserializeObject(new byte[] { 9 }));
        assertThrows(NullPointerException.class, () -> registry.serializeObject(null));
        assertThrows(NullPointerException.class, () -> registry.deserializeObject(null));
    }

    private interface Shape
    {
    }

    private static class Circle implements Shape
    {
    }
}
